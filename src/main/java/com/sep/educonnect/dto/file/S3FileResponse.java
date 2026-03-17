package com.sep.educonnect.dto.file;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class S3FileResponse {

    String fileId;
    String url;
}
