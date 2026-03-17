package com.sep.educonnect.service.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sep.educonnect.entity.ClassEnrollment;
import com.sep.educonnect.entity.Course;
import com.sep.educonnect.entity.CourseProgress;
import com.sep.educonnect.entity.Lesson;
import com.sep.educonnect.entity.LessonProgress;
import com.sep.educonnect.entity.Module;
import com.sep.educonnect.entity.Syllabus;
import com.sep.educonnect.entity.TutorClass;
import com.sep.educonnect.enums.CourseProgressStatus;
import com.sep.educonnect.enums.LessonProgressStatus;
import com.sep.educonnect.repository.*;
import com.sep.educonnect.service.ProgressService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock private ClassEnrollmentRepository classEnrollmentRepository;

    @Mock private CourseProgressRepository courseProgressRepository;

    @Mock private LessonProgressRepository lessonProgressRepository;

    @Mock private ModuleRepository moduleRepository;

    @Mock private LessonRepository lessonRepository;

    @InjectMocks private ProgressService progressService;

    @Test
    @DisplayName("Should create course progress with lesson progress entries")
    void should_createCourseProgress() {
        Long enrollmentId = 1L;

        Syllabus syllabus = Syllabus.builder().syllabusId(10L).build();
        Course course = Course.builder().id(5L).syllabus(syllabus).build();
        TutorClass tutorClass = TutorClass.builder().id(3L).course(course).build();
        ClassEnrollment enrollment =
                ClassEnrollment.builder().id(enrollmentId).tutorClass(tutorClass).build();

        Module module = Module.builder().moduleId(20L).syllabusId(syllabus.getSyllabusId()).build();
        Lesson lesson =
                Lesson.builder()
                        .lessonId(30L)
                        .moduleId(module.getModuleId())
                        .title("Lesson 1")
                        .build();

        when(courseProgressRepository.existsByEnrollmentId(enrollmentId)).thenReturn(false);
        when(classEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(moduleRepository.findBySyllabusIdOrderByOrderNumberAsc(syllabus.getSyllabusId()))
                .thenReturn(List.of(module));
        when(lessonRepository.findByModuleIdOrderByOrderNumberAsc(module.getModuleId()))
                .thenReturn(List.of(lesson));
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CourseProgress result = progressService.createCourseProgress(enrollmentId);

        assertNotNull(result);
        assertEquals(enrollment, result.getEnrollment());
        assertEquals(1, result.getTotalLessons());
        assertEquals(CourseProgressStatus.NOT_STARTED, result.getStatus());
        assertEquals(1, result.getLessonProgresses().size());
        assertEquals(enrollment.getCourseProgress(), result);

        verify(courseProgressRepository).save(any(CourseProgress.class));
    }

    @Test
    @DisplayName("Should return existing course progress if already exists")
    void should_returnExistingCourseProgress_whenAlreadyExists() {
        // Given
        Long enrollmentId = 1L;
        CourseProgress existingProgress =
                CourseProgress.builder()
                        .id(100L)
                        .enrollment(ClassEnrollment.builder().id(enrollmentId).build())
                        .totalLessons(5)
                        .completedLessons(2)
                        .status(CourseProgressStatus.IN_PROGRESS)
                        .build();

        when(courseProgressRepository.existsByEnrollmentId(enrollmentId)).thenReturn(true);
        when(courseProgressRepository.findByEnrollmentId(enrollmentId))
                .thenReturn(Optional.of(existingProgress));

        // When
        CourseProgress result = progressService.createCourseProgress(enrollmentId);

        // Then
        assertNotNull(result);
        assertEquals(existingProgress.getId(), result.getId());
        assertEquals(5, result.getTotalLessons());
        assertEquals(2, result.getCompletedLessons());
        assertEquals(CourseProgressStatus.IN_PROGRESS, result.getStatus());

        verify(courseProgressRepository, never()).save(any(CourseProgress.class));
        verify(classEnrollmentRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should create course progress with no lessons")
    void should_createCourseProgress_withNoLessons() {
        // Given
        Long enrollmentId = 1L;
        Syllabus syllabus = Syllabus.builder().syllabusId(10L).build();
        Course course = Course.builder().id(5L).syllabus(syllabus).build();
        TutorClass tutorClass = TutorClass.builder().id(3L).course(course).build();
        ClassEnrollment enrollment =
                ClassEnrollment.builder().id(enrollmentId).tutorClass(tutorClass).build();

        when(courseProgressRepository.existsByEnrollmentId(enrollmentId)).thenReturn(false);
        when(classEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(moduleRepository.findBySyllabusIdOrderByOrderNumberAsc(syllabus.getSyllabusId()))
                .thenReturn(List.of());
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CourseProgress result = progressService.createCourseProgress(enrollmentId);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalLessons());
        assertEquals(CourseProgressStatus.NOT_STARTED, result.getStatus());
        assertTrue(result.getLessonProgresses().isEmpty());

        verify(courseProgressRepository).save(any(CourseProgress.class));
    }

    @Test
    @DisplayName("Should initialize with zero completed lessons")
    void should_initializeProgressPercentageToZero() {
        // Given
        Long enrollmentId = 1L;
        Syllabus syllabus = Syllabus.builder().syllabusId(10L).build();
        Course course = Course.builder().id(5L).syllabus(syllabus).build();
        TutorClass tutorClass = TutorClass.builder().id(3L).course(course).build();
        ClassEnrollment enrollment =
                ClassEnrollment.builder().id(enrollmentId).tutorClass(tutorClass).build();

        Module module = Module.builder().moduleId(20L).syllabusId(10L).build();
        Lesson lesson = Lesson.builder().lessonId(30L).moduleId(20L).title("Lesson 1").build();

        when(courseProgressRepository.existsByEnrollmentId(enrollmentId)).thenReturn(false);
        when(classEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(moduleRepository.findBySyllabusIdOrderByOrderNumberAsc(10L))
                .thenReturn(List.of(module));
        when(lessonRepository.findByModuleIdOrderByOrderNumberAsc(20L)).thenReturn(List.of(lesson));
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CourseProgress result = progressService.createCourseProgress(enrollmentId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalLessons());
        assertEquals(CourseProgressStatus.NOT_STARTED, result.getStatus());
        // Verify that the course progress is correctly initialized
        assertNotNull(result.getEnrollment());
        assertNotNull(result.getLessonProgresses());
    }

    @Test
    @DisplayName("Should get lesson progresses successfully")
    void should_getLessonProgresses_successfully() {
        // Given
        Long enrollmentId = 1L;
        Long courseProgressId = 100L;

        CourseProgress courseProgress =
                CourseProgress.builder()
                        .id(courseProgressId)
                        .enrollment(ClassEnrollment.builder().id(enrollmentId).build())
                        .totalLessons(3)
                        .status(CourseProgressStatus.IN_PROGRESS)
                        .build();

        LessonProgress lp1 =
                LessonProgress.builder()
                        .id(1L)
                        .courseProgress(courseProgress)
                        .lesson(Lesson.builder().lessonId(10L).title("Lesson 1").build())
                        .status(LessonProgressStatus.COMPLETED)
                        .build();

        LessonProgress lp2 =
                LessonProgress.builder()
                        .id(2L)
                        .courseProgress(courseProgress)
                        .lesson(Lesson.builder().lessonId(11L).title("Lesson 2").build())
                        .status(LessonProgressStatus.IN_PROGRESS)
                        .build();

        LessonProgress lp3 =
                LessonProgress.builder()
                        .id(3L)
                        .courseProgress(courseProgress)
                        .lesson(Lesson.builder().lessonId(12L).title("Lesson 3").build())
                        .status(LessonProgressStatus.NOT_STARTED)
                        .build();

        List<LessonProgress> expectedProgresses = List.of(lp1, lp2, lp3);

        when(courseProgressRepository.findByEnrollmentId(enrollmentId))
                .thenReturn(Optional.of(courseProgress));
        when(lessonProgressRepository.findByCourseProgressId(courseProgressId))
                .thenReturn(expectedProgresses);

        // When
        List<LessonProgress> result = progressService.getLessonProgresses(enrollmentId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(LessonProgressStatus.COMPLETED, result.get(0).getStatus());
        assertEquals(LessonProgressStatus.IN_PROGRESS, result.get(1).getStatus());
        assertEquals(LessonProgressStatus.NOT_STARTED, result.get(2).getStatus());
        verify(courseProgressRepository).findByEnrollmentId(enrollmentId);
        verify(lessonProgressRepository).findByCourseProgressId(courseProgressId);
    }


    @Test
    @DisplayName("Should return empty list when no lesson progresses exist")
    void should_returnEmptyList_whenNoLessonProgressesExist() {
        // Given
        Long enrollmentId = 1L;
        Long courseProgressId = 100L;

        CourseProgress courseProgress =
                CourseProgress.builder()
                        .id(courseProgressId)
                        .enrollment(ClassEnrollment.builder().id(enrollmentId).build())
                        .totalLessons(0)
                        .status(CourseProgressStatus.NOT_STARTED)
                        .build();

        when(courseProgressRepository.findByEnrollmentId(enrollmentId))
                .thenReturn(Optional.of(courseProgress));
        when(lessonProgressRepository.findByCourseProgressId(courseProgressId))
                .thenReturn(List.of());

        // When
        List<LessonProgress> result = progressService.getLessonProgresses(enrollmentId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(lessonProgressRepository).findByCourseProgressId(courseProgressId);
    }

    @Test
    @DisplayName("Should get lesson progresses with all COMPLETED status")
    void should_getLessonProgresses_withAllCompleted() {
        // Given
        Long enrollmentId = 1L;
        Long courseProgressId = 100L;

        CourseProgress courseProgress =
                CourseProgress.builder()
                        .id(courseProgressId)
                        .enrollment(ClassEnrollment.builder().id(enrollmentId).build())
                        .totalLessons(2)
                        .status(CourseProgressStatus.COMPLETED)
                        .build();

        java.time.LocalDateTime completedTime = java.time.LocalDateTime.now();

        LessonProgress lp1 =
                LessonProgress.builder()
                        .id(1L)
                        .courseProgress(courseProgress)
                        .lesson(Lesson.builder().lessonId(10L).build())
                        .status(LessonProgressStatus.COMPLETED)
                        .completedAt(completedTime)
                        .build();

        LessonProgress lp2 =
                LessonProgress.builder()
                        .id(2L)
                        .courseProgress(courseProgress)
                        .lesson(Lesson.builder().lessonId(11L).build())
                        .status(LessonProgressStatus.COMPLETED)
                        .completedAt(completedTime)
                        .build();

        when(courseProgressRepository.findByEnrollmentId(enrollmentId))
                .thenReturn(Optional.of(courseProgress));
        when(lessonProgressRepository.findByCourseProgressId(courseProgressId))
                .thenReturn(List.of(lp1, lp2));

        // When
        List<LessonProgress> result = progressService.getLessonProgresses(enrollmentId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(
                result.stream().allMatch(lp -> lp.getStatus() == LessonProgressStatus.COMPLETED));
        assertTrue(result.stream().allMatch(lp -> lp.getCompletedAt() != null));
    }


    @Test
    @DisplayName("Should get lesson progresses with single lesson")
    void should_getLessonProgresses_withSingleLesson() {
        // Given
        Long enrollmentId = 1L;
        Long courseProgressId = 100L;

        CourseProgress courseProgress =
                CourseProgress.builder()
                        .id(courseProgressId)
                        .enrollment(ClassEnrollment.builder().id(enrollmentId).build())
                        .totalLessons(1)
                        .build();

        LessonProgress lp =
                LessonProgress.builder()
                        .id(1L)
                        .courseProgress(courseProgress)
                        .lesson(Lesson.builder().lessonId(10L).title("Only Lesson").build())
                        .status(LessonProgressStatus.IN_PROGRESS)
                        .build();

        when(courseProgressRepository.findByEnrollmentId(enrollmentId))
                .thenReturn(Optional.of(courseProgress));
        when(lessonProgressRepository.findByCourseProgressId(courseProgressId))
                .thenReturn(List.of(lp));

        // When
        List<LessonProgress> result = progressService.getLessonProgresses(enrollmentId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Only Lesson", result.get(0).getLesson().getTitle());
    }

    @Test
    @DisplayName("Should load lessons from single module")
    void should_loadLessonsForCourse_singleModule() {
        // Given
        Long enrollmentId = 1L;
        Syllabus syllabus = Syllabus.builder().syllabusId(10L).build();
        Course course = Course.builder().id(5L).syllabus(syllabus).build();
        TutorClass tutorClass = TutorClass.builder().id(3L).course(course).build();
        ClassEnrollment enrollment =
                ClassEnrollment.builder().id(enrollmentId).tutorClass(tutorClass).build();

        Module module = Module.builder().moduleId(20L).syllabusId(10L).orderNumber(1).build();
        Lesson lesson1 = Lesson.builder().lessonId(30L).moduleId(20L).orderNumber(1).build();
        Lesson lesson2 = Lesson.builder().lessonId(31L).moduleId(20L).orderNumber(2).build();
        Lesson lesson3 = Lesson.builder().lessonId(32L).moduleId(20L).orderNumber(3).build();

        when(courseProgressRepository.existsByEnrollmentId(enrollmentId)).thenReturn(false);
        when(classEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(moduleRepository.findBySyllabusIdOrderByOrderNumberAsc(10L))
                .thenReturn(List.of(module));
        when(lessonRepository.findByModuleIdOrderByOrderNumberAsc(20L))
                .thenReturn(List.of(lesson1, lesson2, lesson3));
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CourseProgress result = progressService.createCourseProgress(enrollmentId);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalLessons());
        assertEquals(3, result.getLessonProgresses().size());
        verify(moduleRepository).findBySyllabusIdOrderByOrderNumberAsc(10L);
        verify(lessonRepository).findByModuleIdOrderByOrderNumberAsc(20L);
    }

    @Test
    @DisplayName("Should load lessons from multiple modules in order")
    void should_loadLessonsForCourse_multipleModulesInOrder() {
        // Given
        Long enrollmentId = 1L;
        Syllabus syllabus = Syllabus.builder().syllabusId(10L).build();
        Course course = Course.builder().id(5L).syllabus(syllabus).build();
        TutorClass tutorClass = TutorClass.builder().id(3L).course(course).build();
        ClassEnrollment enrollment =
                ClassEnrollment.builder().id(enrollmentId).tutorClass(tutorClass).build();

        Module module1 = Module.builder().moduleId(20L).syllabusId(10L).orderNumber(1).build();
        Module module2 = Module.builder().moduleId(21L).syllabusId(10L).orderNumber(2).build();
        Module module3 = Module.builder().moduleId(22L).syllabusId(10L).orderNumber(3).build();

        Lesson lesson1 = Lesson.builder().lessonId(30L).moduleId(20L).build();
        Lesson lesson2 = Lesson.builder().lessonId(31L).moduleId(20L).build();
        Lesson lesson3 = Lesson.builder().lessonId(32L).moduleId(21L).build();
        Lesson lesson4 = Lesson.builder().lessonId(33L).moduleId(22L).build();
        Lesson lesson5 = Lesson.builder().lessonId(34L).moduleId(22L).build();

        when(courseProgressRepository.existsByEnrollmentId(enrollmentId)).thenReturn(false);
        when(classEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(moduleRepository.findBySyllabusIdOrderByOrderNumberAsc(10L))
                .thenReturn(List.of(module1, module2, module3));
        when(lessonRepository.findByModuleIdOrderByOrderNumberAsc(20L))
                .thenReturn(List.of(lesson1, lesson2));
        when(lessonRepository.findByModuleIdOrderByOrderNumberAsc(21L))
                .thenReturn(List.of(lesson3));
        when(lessonRepository.findByModuleIdOrderByOrderNumberAsc(22L))
                .thenReturn(List.of(lesson4, lesson5));
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CourseProgress result = progressService.createCourseProgress(enrollmentId);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getTotalLessons());
        assertEquals(5, result.getLessonProgresses().size());
        verify(moduleRepository).findBySyllabusIdOrderByOrderNumberAsc(10L);
        verify(lessonRepository, times(3)).findByModuleIdOrderByOrderNumberAsc(anyLong());
    }

    @Test
    @DisplayName("Should load empty list when no modules exist")
    void should_loadLessonsForCourse_noModules() {
        // Given
        Long enrollmentId = 1L;
        Syllabus syllabus = Syllabus.builder().syllabusId(10L).build();
        Course course = Course.builder().id(5L).syllabus(syllabus).build();
        TutorClass tutorClass = TutorClass.builder().id(3L).course(course).build();
        ClassEnrollment enrollment =
                ClassEnrollment.builder().id(enrollmentId).tutorClass(tutorClass).build();

        when(courseProgressRepository.existsByEnrollmentId(enrollmentId)).thenReturn(false);
        when(classEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(moduleRepository.findBySyllabusIdOrderByOrderNumberAsc(10L)).thenReturn(List.of());
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CourseProgress result = progressService.createCourseProgress(enrollmentId);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalLessons());
        assertTrue(result.getLessonProgresses().isEmpty());
        verify(moduleRepository).findBySyllabusIdOrderByOrderNumberAsc(10L);
        verify(lessonRepository, never()).findByModuleIdOrderByOrderNumberAsc(anyLong());
    }

    @Test
    @DisplayName("Should load lessons when all modules are empty")
    void should_loadLessonsForCourse_allModulesEmpty() {
        // Given
        Long enrollmentId = 1L;
        Syllabus syllabus = Syllabus.builder().syllabusId(10L).build();
        Course course = Course.builder().id(5L).syllabus(syllabus).build();
        TutorClass tutorClass = TutorClass.builder().id(3L).course(course).build();
        ClassEnrollment enrollment =
                ClassEnrollment.builder().id(enrollmentId).tutorClass(tutorClass).build();

        Module module1 = Module.builder().moduleId(20L).syllabusId(10L).build();
        Module module2 = Module.builder().moduleId(21L).syllabusId(10L).build();

        when(courseProgressRepository.existsByEnrollmentId(enrollmentId)).thenReturn(false);
        when(classEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(moduleRepository.findBySyllabusIdOrderByOrderNumberAsc(10L))
                .thenReturn(List.of(module1, module2));
        when(lessonRepository.findByModuleIdOrderByOrderNumberAsc(20L)).thenReturn(List.of());
        when(lessonRepository.findByModuleIdOrderByOrderNumberAsc(21L)).thenReturn(List.of());
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CourseProgress result = progressService.createCourseProgress(enrollmentId);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalLessons());
        assertTrue(result.getLessonProgresses().isEmpty());
        verify(lessonRepository, times(2)).findByModuleIdOrderByOrderNumberAsc(anyLong());
    }


    @Test
    @DisplayName("Should load lessons maintaining module order")
    void should_loadLessonsForCourse_maintainModuleOrder() {
        // Given
        Long enrollmentId = 1L;
        Syllabus syllabus = Syllabus.builder().syllabusId(10L).build();
        Course course = Course.builder().id(5L).syllabus(syllabus).build();
        TutorClass tutorClass = TutorClass.builder().id(3L).course(course).build();
        ClassEnrollment enrollment =
                ClassEnrollment.builder().id(enrollmentId).tutorClass(tutorClass).build();

        // Modules with specific order numbers
        Module module1 = Module.builder().moduleId(20L).syllabusId(10L).orderNumber(1).build();
        Module module2 = Module.builder().moduleId(21L).syllabusId(10L).orderNumber(2).build();

        Lesson lesson1 =
                Lesson.builder().lessonId(30L).moduleId(20L).title("Module1-Lesson1").build();
        Lesson lesson2 =
                Lesson.builder().lessonId(31L).moduleId(21L).title("Module2-Lesson1").build();

        when(courseProgressRepository.existsByEnrollmentId(enrollmentId)).thenReturn(false);
        when(classEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(moduleRepository.findBySyllabusIdOrderByOrderNumberAsc(10L))
                .thenReturn(List.of(module1, module2));
        when(lessonRepository.findByModuleIdOrderByOrderNumberAsc(20L))
                .thenReturn(List.of(lesson1));
        when(lessonRepository.findByModuleIdOrderByOrderNumberAsc(21L))
                .thenReturn(List.of(lesson2));
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CourseProgress result = progressService.createCourseProgress(enrollmentId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalLessons());
        List<LessonProgress> progresses = new java.util.ArrayList<>(result.getLessonProgresses());
        assertEquals("Module1-Lesson1", progresses.get(0).getLesson().getTitle());
        assertEquals("Module2-Lesson1", progresses.get(1).getLesson().getTitle());
    }

    @Test
    @DisplayName("Should load single lesson from single module")
    void should_loadLessonsForCourse_singleLessonSingleModule() {
        // Given
        Long enrollmentId = 1L;
        Syllabus syllabus = Syllabus.builder().syllabusId(10L).build();
        Course course = Course.builder().id(5L).syllabus(syllabus).build();
        TutorClass tutorClass = TutorClass.builder().id(3L).course(course).build();
        ClassEnrollment enrollment =
                ClassEnrollment.builder().id(enrollmentId).tutorClass(tutorClass).build();

        Module module = Module.builder().moduleId(20L).syllabusId(10L).build();
        Lesson lesson = Lesson.builder().lessonId(30L).moduleId(20L).title("Only Lesson").build();

        when(courseProgressRepository.existsByEnrollmentId(enrollmentId)).thenReturn(false);
        when(classEnrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(moduleRepository.findBySyllabusIdOrderByOrderNumberAsc(10L))
                .thenReturn(List.of(module));
        when(lessonRepository.findByModuleIdOrderByOrderNumberAsc(20L)).thenReturn(List.of(lesson));
        when(courseProgressRepository.save(any(CourseProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CourseProgress result = progressService.createCourseProgress(enrollmentId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalLessons());
        assertEquals(1, result.getLessonProgresses().size());
        assertEquals("Only Lesson", result.getLessonProgresses().get(0).getLesson().getTitle());
    }
}
