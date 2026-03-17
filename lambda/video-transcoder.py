import json
import boto3
import os
import subprocess
import tempfile
import logging
from urllib.parse import unquote_plus

logger = logging.getLogger()
logger.setLevel(logging.INFO)

s3 = boto3.client('s3')

BUCKET_NAME = os.environ.get('S3_BUCKET_NAME')
CALLBACK_API_URL = os.environ.get('CALLBACK_API_URL')
OUTPUT_PREFIX = 'videos/lessons'

# FFmpeg path - check layer first, then system PATH
FFMPEG_PATH = '/opt/bin/ffmpeg' if os.path.exists('/opt/bin/ffmpeg') else 'ffmpeg'
logger.info(f"Using FFmpeg path: {FFMPEG_PATH}")


def lambda_handler(event, context):
    try:
        logger.info(f"Event: {json.dumps(event)}")

        # Verify FFmpeg
        try:
            result = subprocess.run([FFMPEG_PATH, '-version'],
                              capture_output=True,
                              timeout=5)
            if result.returncode == 0:
                logger.info("FFmpeg verified successfully")
            else:
                logger.error(f"FFmpeg check failed: {result.stderr.decode()}")
                return {"statusCode": 500, "body": "FFmpeg not working"}
        except FileNotFoundError:
            logger.error("FFmpeg not found")
            return {"statusCode": 500, "body": "FFmpeg not found. Please add FFmpeg layer."}
        except Exception as e:
            logger.error(f"FFmpeg check error: {str(e)}")
            return {"statusCode": 500, "body": f"FFmpeg error: {str(e)}"}

        for record in event.get('Records', []):
            bucket = record['s3']['bucket']['name']
            key = unquote_plus(record['s3']['object']['key'])

            parts = key.split('/')
            if len(parts) < 4 or parts[-1] != 'original.mp4':
                logger.warning(f"Skip key: {key}")
                continue

            video_lesson_id = parts[-2]
            ok, err = process_one(bucket, key, video_lesson_id)
            if ok:
                update_status(video_lesson_id, status='READY', manifest_key=f"{OUTPUT_PREFIX}/{video_lesson_id}/master.m3u8")
            else:
                update_status(video_lesson_id, status='FAILED', error_message=err)
    except Exception as e:
        logger.exception("Unhandled error")
        raise


def process_one(bucket, input_key, video_lesson_id):
    tmp_dir = tempfile.mkdtemp()
    try:
        input_path = os.path.join(tmp_dir, 'input.mp4')
        logger.info(f"Downloading {input_key} from S3...")
        s3.download_file(bucket, input_key, input_path)
        logger.info("Download complete")

        out_dir = os.path.join(tmp_dir, 'hls')
        os.makedirs(out_dir, exist_ok=True)

        # Use FFMPEG_PATH variable
        cmd = [
            FFMPEG_PATH, '-y', '-i', input_path,
            '-vf', 'scale=w=1280:h=720:force_original_aspect_ratio=decrease',
            '-c:v', 'libx264', '-b:v', '2500k',
            '-c:a', 'aac', '-b:a', '128k',
            '-hls_time', '6', '-hls_playlist_type', 'vod',
            '-hls_segment_filename', os.path.join(out_dir, '720p_%03d.ts'),
            os.path.join(out_dir, '720p.m3u8')
        ]
        logger.info('Running FFmpeg: ' + ' '.join(cmd))
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=870)
        if r.returncode != 0:
            logger.error(f"FFmpeg failed: {r.stderr}")
            return False, r.stderr

        logger.info("FFmpeg transcode completed successfully")

        # master playlist
        master = "#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720\n720p.m3u8\n"
        master_path = os.path.join(out_dir, 'master.m3u8')
        with open(master_path, 'w') as f:
            f.write(master)

        # upload
        base = f"{OUTPUT_PREFIX}/{video_lesson_id}"
        logger.info(f"Uploading HLS files to s3://{bucket}/{base}/...")
        s3.upload_file(master_path, bucket, f"{base}/master.m3u8")
        s3.upload_file(os.path.join(out_dir, '720p.m3u8'), bucket, f"{base}/720p.m3u8")

        ts_count = 0
        for name in os.listdir(out_dir):
            if name.endswith('.ts'):
                s3.upload_file(os.path.join(out_dir, name), bucket, f"{base}/{name}")
                ts_count += 1

        logger.info(f"Uploaded {ts_count} TS segments")
        return True, None
    except subprocess.TimeoutExpired:
        return False, 'FFmpeg timeout'
    except Exception as e:
        logger.exception('Process error')
        return False, str(e)
    finally:
        try:
            import shutil
            shutil.rmtree(tmp_dir, ignore_errors=True)
        except Exception:
            pass


def update_status(video_lesson_id, status, manifest_key=None, error_message=None):
    if not CALLBACK_API_URL:
        logger.info("No callback URL, skip status update")
        return
    try:
        from urllib.parse import urlencode
        from urllib.request import urlopen, Request
        from urllib.error import URLError, HTTPError

        params = {
            'videoLessonId': int(video_lesson_id),
            'status': status,
        }
        if manifest_key:
            params['hlsMasterPlaylistS3Key'] = manifest_key
        if error_message:
            params['errorMessage'] = error_message

        url = CALLBACK_API_URL.rstrip('/') + '/api/video-lessons/update-status'
        data = urlencode(params).encode('utf-8')
        
        logger.info(f"Calling callback API: {url}")
        logger.info(f"Payload: {params}")

        req = Request(url, data=data, method='POST')
        req.add_header('Content-Type', 'application/x-www-form-urlencoded')

        try:
            with urlopen(req, timeout=15) as response:
                status_code = response.getcode()
                result = response.read().decode('utf-8')
                
                if status_code == 200:
                    logger.info(f"Status updated successfully (HTTP {status_code}): {result}")
                else:
                    logger.warning(f"Callback returned non-200 status: {status_code}, response: {result}")
        except HTTPError as e:
            error_body = e.read().decode('utf-8') if e.fp else 'No response body'
            logger.error(f"HTTP error calling callback API: {e.code} {e.reason}, body: {error_body}")
        except URLError as e:
            logger.error(f"URL error calling callback API: {e.reason}")
            raise
    except Exception as e:
        logger.error(f"Callback error: {type(e).__name__}: {str(e)}")
        logger.exception('Callback error details')
        # Don't re-raise - we don't want Lambda to fail if callback fails