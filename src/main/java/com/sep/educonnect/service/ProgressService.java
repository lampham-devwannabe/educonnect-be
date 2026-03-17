package com.sep.educonnect.service;

import com.sep.educonnect.entity.*;
import com.sep.educonnect.enums.CourseProgressStatus;
import com.sep.educonnect.enums.LessonProgressStatus;
import com.sep.educonnect.exception.AppException;
import com.sep.educonnect.exception.ErrorCode;
import com.sep.educonnect.repository.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

  private final ClassEnrollmentRepository classEnrollmentRepository;
  private final CourseProgressRepository courseProgressRepository;
  private final LessonProgressRepository lessonProgressRepository;
  private final ModuleRepository moduleRepository;
  private final LessonRepository lessonRepository;

  @Transactional
  public CourseProgress createCourseProgress(Long enrollmentId) {
    if (courseProgressRepository.existsByEnrollmentId(enrollmentId)) {
      return courseProgressRepository
          .findByEnrollmentId(enrollmentId)
          .orElseThrow(() -> new AppException(ErrorCode.COURSE_PROGRESS_STATE_INVALID));
    }

    ClassEnrollment enrollment =
        classEnrollmentRepository
            .findById(enrollmentId)
            .orElseThrow(() -> new AppException(ErrorCode.CLASS_ENROLLMENT_NOT_FOUND));

    TutorClass tutorClass = enrollment.getTutorClass();
    if (tutorClass == null) {
      throw new AppException(ErrorCode.TUTOR_CLASS_NOT_ASSOCIATED);
    }

    Course course = tutorClass.getCourse();
    if (course == null) {
      throw new AppException(ErrorCode.COURSE_NOT_ASSOCIATED);
    }

    if (course.getSyllabus() == null) {
      throw new AppException(ErrorCode.SYLLABUS_NOT_DEFINED);
    }

    List<Lesson> lessons = loadLessonsForCourse(course);
    CourseProgress progress =
        CourseProgress.builder()
            .enrollment(enrollment)
            .totalLessons(lessons.size())
            .status(CourseProgressStatus.NOT_STARTED)
            .build();

    List<LessonProgress> lessonProgresses = new ArrayList<>();
    for (Lesson lesson : lessons) {
      LessonProgress lessonProgress =
          LessonProgress.builder()
              .courseProgress(progress)
              .lesson(lesson)
              .optionalLesson(Boolean.FALSE)
              .status(LessonProgressStatus.NOT_STARTED)
              .build();
      lessonProgresses.add(lessonProgress);
    }
    progress.setLessonProgresses(lessonProgresses);

    CourseProgress savedProgress = courseProgressRepository.save(progress);
    enrollment.setCourseProgress(savedProgress);

    log.debug(
        "Initialized course progress for enrollment {} with {} lessons",
        enrollmentId,
        lessons.size());
    return savedProgress;
  }

  @Transactional
  public CourseProgress getCourseProgress(Long enrollmentId) {
    CourseProgress courseProgress =
        courseProgressRepository
            .findByEnrollmentId(enrollmentId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_PROGRESS_NOT_FOUND));

    // Reconcile lessons: if new lessons were added to the syllabus after
    // the CourseProgress was created, create corresponding LessonProgress
    // entries so the returned CourseProgress reflects current syllabus.
    try {
      TutorClass tutorClass =
          courseProgress.getEnrollment() == null
              ? null
              : courseProgress.getEnrollment().getTutorClass();
      if (tutorClass != null
          && tutorClass.getCourse() != null
          && tutorClass.getCourse().getSyllabus() != null) {
        Course course = tutorClass.getCourse();
        List<Lesson> lessons = loadLessonsForCourse(course);

        Set<Long> existingLessonIds =
            courseProgress.getLessonProgresses() == null
                ? java.util.Collections.emptySet()
                : courseProgress.getLessonProgresses().stream()
                    .map(lp -> lp.getLesson().getLessonId())
                    .collect(Collectors.toSet());

        List<LessonProgress> toAdd = new ArrayList<>();
        for (Lesson lesson : lessons) {
          if (lesson == null || lesson.getLessonId() == null) continue;
          if (!existingLessonIds.contains(lesson.getLessonId())) {
            LessonProgress lp =
                LessonProgress.builder()
                    .courseProgress(courseProgress)
                    .lesson(lesson)
                    .optionalLesson(Boolean.FALSE)
                    .status(LessonProgressStatus.NOT_STARTED)
                    .build();
            toAdd.add(lp);
          }
        }

        if (!toAdd.isEmpty()) {
          // persist new lesson progress entries
          lessonProgressRepository.saveAll(toAdd);

          // add to existing collection to avoid orphan removal issues
          if (courseProgress.getLessonProgresses() == null) {
            courseProgress.setLessonProgresses(new ArrayList<>());
          }
          courseProgress.getLessonProgresses().addAll(toAdd);

          // update totals and persist courseProgress
          courseProgress.setTotalLessons(courseProgress.getLessonProgresses().size());
          courseProgressRepository.save(courseProgress);
        }

        // Recalculate status/progress values (completed, percentage, startedAt...)
        courseProgress = recalculateCourseProgressInternal(courseProgress);
      }
    } catch (Exception ex) {
      log.warn(
          "Failed to reconcile lessons for course progress {}: {}", enrollmentId, ex.getMessage());
    }

    return courseProgress;
  }

  @Transactional
  public LessonProgress updateLessonProgress(
      Long enrollmentId,
      Long lessonId,
      LessonProgressStatus status,
      String result,
      Boolean optionalSkip) {
    CourseProgress courseProgress =
        courseProgressRepository
            .findByEnrollmentId(enrollmentId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_PROGRESS_NOT_FOUND));

    LessonProgress lessonProgress =
        lessonProgressRepository
            .findByCourseProgressIdAndLesson_LessonId(courseProgress.getId(), lessonId)
            .orElseThrow(() -> new AppException(ErrorCode.LESSON_PROGRESS_NOT_FOUND));

    LocalDateTime now = LocalDateTime.now();
    lessonProgress.setStatus(status);
    lessonProgress.setLastAccessedAt(now);

    if (optionalSkip != null && lessonProgress.getOptionalLesson()) {
      if (optionalSkip) {
        lessonProgress.setStatus(LessonProgressStatus.SKIPPED);
      }
    }

    if (status == LessonProgressStatus.COMPLETED) {
      lessonProgress.setCompletedAt(now);
    } else if (status != LessonProgressStatus.COMPLETED) {
      lessonProgress.setCompletedAt(null);
    }

    lessonProgress.setResult(result);
    lessonProgressRepository.save(lessonProgress);

    recalculateCourseProgressInternal(courseProgress);
    return lessonProgress;
  }

  @Transactional(readOnly = true)
  public LessonProgress getLessonProgress(Long enrollmentId, Long lessonId) {
    CourseProgress courseProgress = getCourseProgress(enrollmentId);
    return lessonProgressRepository
        .findByCourseProgressIdAndLesson_LessonId(courseProgress.getId(), lessonId)
        .orElseThrow(() -> new AppException(ErrorCode.LESSON_PROGRESS_NOT_FOUND));
  }

  @Transactional
  public CourseProgress recalculateCourseProgress(Long enrollmentId) {
    CourseProgress courseProgress =
        courseProgressRepository
            .findByEnrollmentId(enrollmentId)
            .orElseThrow(() -> new AppException(ErrorCode.COURSE_PROGRESS_NOT_FOUND));
    return recalculateCourseProgressInternal(courseProgress);
  }

  @Transactional(readOnly = true)
  public List<LessonProgress> getLessonProgresses(Long enrollmentId) {
    CourseProgress courseProgress = getCourseProgress(enrollmentId);
    return lessonProgressRepository.findByCourseProgressId(courseProgress.getId());
  }

  private CourseProgress recalculateCourseProgressInternal(CourseProgress courseProgress) {
    List<LessonProgress> lessonProgresses =
        lessonProgressRepository.findByCourseProgressId(courseProgress.getId());
    int total = lessonProgresses.size();

    int completed =
        (int)
            lessonProgresses.stream()
                .filter(
                    lp ->
                        lp.getStatus() == LessonProgressStatus.COMPLETED
                            || (lp.getOptionalLesson()
                                && lp.getStatus() == LessonProgressStatus.SKIPPED))
                .count();

    boolean hasFailed =
        lessonProgresses.stream().anyMatch(lp -> lp.getStatus() == LessonProgressStatus.FAILED);

    boolean hasStarted =
        lessonProgresses.stream()
            .anyMatch(
                lp ->
                    lp.getStatus() == LessonProgressStatus.IN_PROGRESS
                        || lp.getStatus() == LessonProgressStatus.COMPLETED
                        || lp.getStatus() == LessonProgressStatus.FAILED
                        || lp.getStatus() == LessonProgressStatus.SKIPPED);

    courseProgress.setTotalLessons(total);
    courseProgress.setCompletedLessons(completed);

    int percentage = total == 0 ? 0 : (int) Math.round((completed * 100.0) / total);
    courseProgress.setProgressPercentage(percentage);

    if (total == 0) {
      courseProgress.setStatus(CourseProgressStatus.NOT_STARTED);
      courseProgress.setStartedAt(null);
      courseProgress.setCompletedAt(null);
    } else if (completed >= total && !hasFailed) {
      courseProgress.setStatus(CourseProgressStatus.COMPLETED);
      courseProgress.setCompletedAt(LocalDateTime.now());
    } else if (hasFailed) {
      courseProgress.setStatus(CourseProgressStatus.FAILED);
      courseProgress.setCompletedAt(null);
    } else if (hasStarted) {
      courseProgress.setStatus(CourseProgressStatus.IN_PROGRESS);
      if (courseProgress.getStartedAt() == null) {
        courseProgress.setStartedAt(LocalDateTime.now());
      }
      courseProgress.setCompletedAt(null);
    } else {
      courseProgress.setStatus(CourseProgressStatus.NOT_STARTED);
      courseProgress.setStartedAt(null);
      courseProgress.setCompletedAt(null);
    }

    return courseProgressRepository.save(courseProgress);
  }

  private List<Lesson> loadLessonsForCourse(Course course) {
    Long syllabusId = course.getSyllabus().getSyllabusId();
    List<Lesson> lessons =
        moduleRepository.findBySyllabusIdOrderByOrderNumberAsc(syllabusId).stream()
            .flatMap(
                module ->
                    lessonRepository
                        .findByModuleIdOrderByOrderNumberAsc(module.getModuleId())
                        .stream())
            .collect(Collectors.toList());

    // Deduplicate lessons if necessary
    return new ArrayList<>(lessons);
  }
}
