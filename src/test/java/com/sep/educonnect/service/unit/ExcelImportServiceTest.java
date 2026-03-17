package com.sep.educonnect.service.unit;

import com.sep.educonnect.dto.exam.ExcelImportResponse;
import com.sep.educonnect.dto.exam.QuizRequest;
import com.sep.educonnect.dto.exam.QuizResponse;
import com.sep.educonnect.entity.Quiz;
import com.sep.educonnect.repository.QuizRepository;
import com.sep.educonnect.service.ExamService;
import com.sep.educonnect.service.ExcelImportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExcelImportService Unit Tests")
class ExcelImportServiceTest {

    @Mock
    private ExamService examService;

    @Mock
    private QuizRepository quizRepository;

    @InjectMocks
    private ExcelImportService excelImportService;

    private MultipartFile createExcelFile(String filename, Workbook workbook) throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        when(file.getOriginalFilename()).thenReturn(filename);
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenReturn(inputStream);
        
        return file;
    }

    private Workbook createWorkbookWithHeader() {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Quizzes");
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Questions");
        headerRow.createCell(1).setCellValue("Option A");
        headerRow.createCell(2).setCellValue("Option B");
        headerRow.createCell(3).setCellValue("Option C");
        headerRow.createCell(4).setCellValue("Option D");
        headerRow.createCell(5).setCellValue("Quiz Type");
        headerRow.createCell(6).setCellValue("Correct Answer");
        headerRow.createCell(7).setCellValue("Explanation");
        headerRow.createCell(8).setCellValue("Order No");
        return workbook;
    }

    @Test
    @DisplayName("Should return error when file is null")
    void should_returnError_when_fileIsNull() {
        // Given
        Long examId = 1L;

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, null, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getErrorCount());
        assertEquals(0, response.getSuccessCount());
        assertFalse(response.getErrors().isEmpty());
        assertEquals("File is empty or null", response.getErrors().get(0).getMessage());
    }

    @Test
    @DisplayName("Should return error when file is empty")
    void should_returnError_when_fileIsEmpty() {
        // Given
        Long examId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getErrorCount());
        assertEquals(0, response.getSuccessCount());
    }

    @Test
    @DisplayName("Should return error when file format is invalid")
    void should_returnError_when_fileFormatInvalid() {
        // Given
        Long examId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.pdf");

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getErrorCount());
        assertTrue(response.getErrors().get(0).getMessage().contains("Invalid file format"));
    }

    @Test
    @DisplayName("Should import quizzes with APPEND mode successfully")
    void should_importQuizzes_withAppendMode_successfully() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(3).setCellValue("5");
        row1.createCell(4).setCellValue("6");
        row1.createCell(5).setCellValue("SINGLE_CHOICE");
        row1.createCell(6).setCellValue("B");
        row1.createCell(7).setCellValue("Basic math");
        row1.createCell(8).setCellValue(1);

        MultipartFile file = createExcelFile("test.xlsx", workbook);
        
        QuizResponse quizResponse = QuizResponse.builder()
                .quizId(1L)
                .examId(examId)
                .text("What is 2+2?")
                .build();

        when(examService.createQuiz(any(QuizRequest.class))).thenReturn(quizResponse);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        assertEquals(0, response.getErrorCount());
        assertEquals(1, response.getTotalRows());
        verify(examService).createQuiz(any(QuizRequest.class));
    }

    @Test
    @DisplayName("Should import quizzes with REPLACE mode and delete existing quizzes")
    void should_importQuizzes_withReplaceMode_andDeleteExisting() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(5).setCellValue("SINGLE_CHOICE");
        row1.createCell(6).setCellValue("B");

        MultipartFile file = createExcelFile("test.xlsx", workbook);
        
        Quiz existingQuiz = Quiz.builder()
                .quizId(10L)
                .examId(examId)
                .text("Old question")
                .build();

        QuizResponse quizResponse = QuizResponse.builder()
                .quizId(1L)
                .examId(examId)
                .text("What is 2+2?")
                .build();

        when(quizRepository.findByExamId(examId)).thenReturn(List.of(existingQuiz));
        doNothing().when(examService).deleteQuiz(10L);
        when(examService.createQuiz(any(QuizRequest.class))).thenReturn(quizResponse);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "REPLACE");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        verify(quizRepository).findByExamId(examId);
        verify(examService).deleteQuiz(10L);
        verify(examService).createQuiz(any(QuizRequest.class));
    }

    @Test
    @DisplayName("Should import quizzes with UPDATE_OR_CREATE mode - update existing")
    void should_importQuizzes_withUpdateOrCreateMode_updateExisting() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(5).setCellValue("SINGLE_CHOICE");
        row1.createCell(6).setCellValue("B");

        MultipartFile file = createExcelFile("test.xlsx", workbook);
        
        Quiz existingQuiz = Quiz.builder()
                .quizId(10L)
                .examId(examId)
                .text("What is 2+2?")
                .build();

        QuizResponse quizResponse = QuizResponse.builder()
                .quizId(10L)
                .examId(examId)
                .text("What is 2+2?")
                .build();

        when(quizRepository.findByExamIdAndText(examId, "What is 2+2?")).thenReturn(Optional.of(existingQuiz));
        when(examService.updateQuiz(eq(10L), any(QuizRequest.class))).thenReturn(quizResponse);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "UPDATE_OR_CREATE");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        verify(quizRepository).findByExamIdAndText(examId, "What is 2+2?");
        verify(examService).updateQuiz(eq(10L), any(QuizRequest.class));
        verify(examService, never()).createQuiz(any(QuizRequest.class));
    }

    @Test
    @DisplayName("Should import quizzes with UPDATE_OR_CREATE mode - create new")
    void should_importQuizzes_withUpdateOrCreateMode_createNew() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(5).setCellValue("SINGLE_CHOICE");
        row1.createCell(6).setCellValue("B");

        MultipartFile file = createExcelFile("test.xlsx", workbook);
        
        QuizResponse quizResponse = QuizResponse.builder()
                .quizId(1L)
                .examId(examId)
                .text("What is 2+2?")
                .build();

        when(quizRepository.findByExamIdAndText(examId, "What is 2+2?")).thenReturn(Optional.empty());
        when(examService.createQuiz(any(QuizRequest.class))).thenReturn(quizResponse);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "UPDATE_OR_CREATE");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        verify(quizRepository).findByExamIdAndText(examId, "What is 2+2?");
        verify(examService).createQuiz(any(QuizRequest.class));
        verify(examService, never()).updateQuiz(anyLong(), any(QuizRequest.class));
    }

    @Test
    @DisplayName("Should import quizzes with SKIP_DUPLICATE mode - skip existing")
    void should_importQuizzes_withSkipDuplicateMode_skipExisting() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(5).setCellValue("SINGLE_CHOICE");
        row1.createCell(6).setCellValue("B");

        MultipartFile file = createExcelFile("test.xlsx", workbook);
        
        Quiz existingQuiz = Quiz.builder()
                .quizId(10L)
                .examId(examId)
                .text("What is 2+2?")
                .build();

        when(quizRepository.findByExamIdAndText(examId, "What is 2+2?")).thenReturn(Optional.of(existingQuiz));

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "SKIP_DUPLICATE");

        // Then
        assertNotNull(response);
        assertEquals(0, response.getSuccessCount());
        assertEquals(1, response.getErrorCount());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("already exists, skipped")));
        verify(quizRepository).findByExamIdAndText(examId, "What is 2+2?");
        verify(examService, never()).createQuiz(any(QuizRequest.class));
    }

    @Test
    @DisplayName("Should use default APPEND mode when invalid mode provided")
    void should_useDefaultAppendMode_when_invalidModeProvided() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(5).setCellValue("SINGLE_CHOICE");
        row1.createCell(6).setCellValue("B");

        MultipartFile file = createExcelFile("test.xlsx", workbook);
        
        QuizResponse quizResponse = QuizResponse.builder()
                .quizId(1L)
                .examId(examId)
                .text("What is 2+2?")
                .build();

        when(examService.createQuiz(any(QuizRequest.class))).thenReturn(quizResponse);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "INVALID_MODE");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        verify(examService).createQuiz(any(QuizRequest.class));
    }

    @Test
    @DisplayName("Should return error when question text is missing")
    void should_returnError_when_questionTextMissing() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(1).setCellValue("Option A");
        row1.createCell(2).setCellValue("Option B");
        row1.createCell(6).setCellValue("B");

        MultipartFile file = createExcelFile("test.xlsx", workbook);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getErrorCount());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("Question text is required")));
        verify(examService, never()).createQuiz(any(QuizRequest.class));
    }

    @Test
    @DisplayName("Should return error when no options provided")
    void should_returnError_when_noOptionsProvided() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(6).setCellValue("B");

        MultipartFile file = createExcelFile("test.xlsx", workbook);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getErrorCount());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("At least one option is required")));
    }

    @Test
    @DisplayName("Should return error when correct answer is missing")
    void should_returnError_when_correctAnswerMissing() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");

        MultipartFile file = createExcelFile("test.xlsx", workbook);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getErrorCount());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("Correct answer is required")));
    }

    @Test
    @DisplayName("Should return error when invalid quiz type provided")
    void should_returnError_when_invalidQuizType() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(5).setCellValue("INVALID_TYPE");
        row1.createCell(6).setCellValue("B");

        MultipartFile file = createExcelFile("test.xlsx", workbook);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getErrorCount());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("Invalid quiz type")));
    }

    @Test
    @DisplayName("Should import MULTIPLE_CHOICE quiz successfully")
    void should_importMultipleChoiceQuiz_successfully() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Which are even numbers?");
        row1.createCell(1).setCellValue("2");
        row1.createCell(2).setCellValue("3");
        row1.createCell(3).setCellValue("4");
        row1.createCell(4).setCellValue("5");
        row1.createCell(5).setCellValue("MULTIPLE_CHOICE");
        row1.createCell(6).setCellValue("A,C");
        row1.createCell(7).setCellValue("Even numbers are divisible by 2");

        MultipartFile file = createExcelFile("test.xlsx", workbook);
        
        QuizResponse quizResponse = QuizResponse.builder()
                .quizId(1L)
                .examId(examId)
                .text("Which are even numbers?")
                .build();

        when(examService.createQuiz(any(QuizRequest.class))).thenReturn(quizResponse);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        ArgumentCaptor<QuizRequest> requestCaptor = ArgumentCaptor.forClass(QuizRequest.class);
        verify(examService).createQuiz(requestCaptor.capture());
        QuizRequest capturedRequest = requestCaptor.getValue();
        assertEquals("MULTIPLE_CHOICE", capturedRequest.getType());
        assertEquals("2, 4", capturedRequest.getValidAnswer());
        assertTrue(capturedRequest.getOptions().stream()
                .anyMatch(opt -> opt.getText().equals("2") && opt.getIsCorrect()));
        assertTrue(capturedRequest.getOptions().stream()
                .anyMatch(opt -> opt.getText().equals("4") && opt.getIsCorrect()));
    }

    @Test
    @DisplayName("Should return error when single choice has multiple answers")
    void should_returnError_when_singleChoiceHasMultipleAnswers() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(5).setCellValue("SINGLE_CHOICE");
        row1.createCell(6).setCellValue("A,B");

        MultipartFile file = createExcelFile("test.xlsx", workbook);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getErrorCount());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("can only have one correct answer")));
    }

    @Test
    @DisplayName("Should return error when invalid answer key provided")
    void should_returnError_when_invalidAnswerKey() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(5).setCellValue("SINGLE_CHOICE");
        row1.createCell(6).setCellValue("E"); // Invalid - only A, B, C, D available

        MultipartFile file = createExcelFile("test.xlsx", workbook);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getErrorCount());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("Invalid answer key")));
    }

    @Test
    @DisplayName("Should handle numeric cell values correctly")
    void should_handleNumericCellValues_correctly() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue(3.0); // Numeric
        row1.createCell(2).setCellValue(4.0); // Numeric
        row1.createCell(5).setCellValue("SINGLE_CHOICE");
        row1.createCell(6).setCellValue("B");
        row1.createCell(8).setCellValue(1.0); // Numeric order number

        MultipartFile file = createExcelFile("test.xlsx", workbook);
        
        QuizResponse quizResponse = QuizResponse.builder()
                .quizId(1L)
                .examId(examId)
                .text("What is 2+2?")
                .build();

        when(examService.createQuiz(any(QuizRequest.class))).thenReturn(quizResponse);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        ArgumentCaptor<QuizRequest> requestCaptor = ArgumentCaptor.forClass(QuizRequest.class);
        verify(examService).createQuiz(requestCaptor.capture());
        QuizRequest capturedRequest = requestCaptor.getValue();
        assertEquals(1, capturedRequest.getOrderNo());
    }

    @Test
    @DisplayName("Should skip empty rows")
    void should_skipEmptyRows() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(5).setCellValue("SINGLE_CHOICE");
        row1.createCell(6).setCellValue("B");
        
        // Row 2 is intentionally empty to test empty row skipping
        
        Row row3 = sheet.createRow(3);
        row3.createCell(0).setCellValue("What is 3+3?");
        row3.createCell(1).setCellValue("5");
        row3.createCell(2).setCellValue("6");
        row3.createCell(5).setCellValue("SINGLE_CHOICE");
        row3.createCell(6).setCellValue("B");

        MultipartFile file = createExcelFile("test.xlsx", workbook);
        
        QuizResponse quizResponse1 = QuizResponse.builder()
                .quizId(1L)
                .examId(examId)
                .text("What is 2+2?")
                .build();
        
        QuizResponse quizResponse2 = QuizResponse.builder()
                .quizId(2L)
                .examId(examId)
                .text("What is 3+3?")
                .build();

        when(examService.createQuiz(any(QuizRequest.class))).thenReturn(quizResponse1, quizResponse2);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(2, response.getSuccessCount());
        assertEquals(3, response.getTotalRows()); // totalRows = lastRowNum (3), not count of non-empty rows
        verify(examService, times(2)).createQuiz(any(QuizRequest.class));
    }

    @Test
    @DisplayName("Should handle TRUE_FALSE quiz type")
    void should_handleTrueFalseQuizType() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Is Java an object-oriented language?");
        row1.createCell(1).setCellValue("True");
        row1.createCell(2).setCellValue("False");
        row1.createCell(5).setCellValue("TRUE_FALSE");
        row1.createCell(6).setCellValue("A");

        MultipartFile file = createExcelFile("test.xlsx", workbook);
        
        QuizResponse quizResponse = QuizResponse.builder()
                .quizId(1L)
                .examId(examId)
                .text("Is Java an object-oriented language?")
                .build();

        when(examService.createQuiz(any(QuizRequest.class))).thenReturn(quizResponse);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        ArgumentCaptor<QuizRequest> requestCaptor = ArgumentCaptor.forClass(QuizRequest.class);
        verify(examService).createQuiz(requestCaptor.capture());
        assertEquals("TRUE_FALSE", requestCaptor.getValue().getType());
    }

    @Test
    @DisplayName("Should use default SINGLE_CHOICE when quiz type is missing")
    void should_useDefaultSingleChoice_when_quizTypeMissing() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(6).setCellValue("B");

        MultipartFile file = createExcelFile("test.xlsx", workbook);
        
        QuizResponse quizResponse = QuizResponse.builder()
                .quizId(1L)
                .examId(examId)
                .text("What is 2+2?")
                .build();

        when(examService.createQuiz(any(QuizRequest.class))).thenReturn(quizResponse);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        ArgumentCaptor<QuizRequest> requestCaptor = ArgumentCaptor.forClass(QuizRequest.class);
        verify(examService).createQuiz(requestCaptor.capture());
        assertEquals("SINGLE_CHOICE", requestCaptor.getValue().getType());
    }

    @Test
    @DisplayName("Should use row number as default order number")
    void should_useRowNumberAsDefaultOrderNumber() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(5).setCellValue("SINGLE_CHOICE");
        row1.createCell(6).setCellValue("B");
        // Order No column is empty

        MultipartFile file = createExcelFile("test.xlsx", workbook);
        
        QuizResponse quizResponse = QuizResponse.builder()
                .quizId(1L)
                .examId(examId)
                .text("What is 2+2?")
                .build();

        when(examService.createQuiz(any(QuizRequest.class))).thenReturn(quizResponse);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        ArgumentCaptor<QuizRequest> requestCaptor = ArgumentCaptor.forClass(QuizRequest.class);
        verify(examService).createQuiz(requestCaptor.capture());
        assertEquals(2, requestCaptor.getValue().getOrderNo()); // Row number (1-based) = 2
    }

    @Test
    @DisplayName("Should handle UPDATE_OR_CREATE mode with update error")
    void should_handleUpdateOrCreateMode_withUpdateError() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(5).setCellValue("SINGLE_CHOICE");
        row1.createCell(6).setCellValue("B");

        MultipartFile file = createExcelFile("test.xlsx", workbook);
        
        Quiz existingQuiz = Quiz.builder()
                .quizId(10L)
                .examId(examId)
                .text("What is 2+2?")
                .build();

        when(quizRepository.findByExamIdAndText(examId, "What is 2+2?")).thenReturn(Optional.of(existingQuiz));
        when(examService.updateQuiz(eq(10L), any(QuizRequest.class)))
                .thenThrow(new IllegalArgumentException("Quiz not found"));

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "UPDATE_OR_CREATE");

        // Then
        assertNotNull(response);
        assertEquals(0, response.getSuccessCount());
        assertEquals(1, response.getErrorCount());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("Failed to update existing quiz")));
    }

    @Test
    @DisplayName("Should handle IOException when reading file")
    void should_handleIOException_when_readingFile() throws Exception {
        // Given
        Long examId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("test.xlsx");
        when(file.getInputStream()).thenThrow(new IOException("File read error"));

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getErrorCount());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("Error reading file")));
    }

    @Test
    @DisplayName("Should handle file with no sheets")
    void should_handleFileWithNoSheets() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = new XSSFWorkbook();
        // No sheets created
        
        MultipartFile file = mock(MultipartFile.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        when(file.getOriginalFilename()).thenReturn("test.xlsx");
        when(file.isEmpty()).thenReturn(false);
        lenient().when(file.getInputStream()).thenReturn(inputStream);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getErrorCount());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("does not contain any sheets") 
                        || error.getMessage().contains("Unexpected error")));
    }

    @Test
    @DisplayName("Should handle REPLACE mode with delete error")
    void should_handleReplaceMode_withDeleteError() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(5).setCellValue("SINGLE_CHOICE");
        row1.createCell(6).setCellValue("B");

        // Create mock file separately because exception may occur before reading file
        MultipartFile file = mock(MultipartFile.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        when(file.getOriginalFilename()).thenReturn("test.xlsx");
        when(file.isEmpty()).thenReturn(false);
        lenient().when(file.getInputStream()).thenReturn(inputStream);
        
        Quiz existingQuiz = Quiz.builder()
                .quizId(10L)
                .examId(examId)
                .text("Old question")
                .build();

        when(quizRepository.findByExamId(examId)).thenReturn(List.of(existingQuiz));
        doThrow(new RuntimeException("Delete error")).when(examService).deleteQuiz(10L);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "REPLACE");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getErrorCount());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("Failed to delete existing quizzes")));
    }

    @Test
    @DisplayName("Should handle .xls file format")
    void should_handleXlsFileFormat() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("What is 2+2?");
        row1.createCell(1).setCellValue("3");
        row1.createCell(2).setCellValue("4");
        row1.createCell(5).setCellValue("SINGLE_CHOICE");
        row1.createCell(6).setCellValue("B");

        MultipartFile file = createExcelFile("test.xls", workbook);
        
        QuizResponse quizResponse = QuizResponse.builder()
                .quizId(1L)
                .examId(examId)
                .text("What is 2+2?")
                .build();

        when(examService.createQuiz(any(QuizRequest.class))).thenReturn(quizResponse);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        verify(examService).createQuiz(any(QuizRequest.class));
    }

    @Test
    @DisplayName("Should handle boolean cell values")
    void should_handleBooleanCellValues() throws Exception {
        // Given
        Long examId = 1L;
        Workbook workbook = createWorkbookWithHeader();
        Sheet sheet = workbook.getSheetAt(0);
        
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Is 2+2 equal to 4?");
        row1.createCell(1).setCellValue("True");
        row1.createCell(2).setCellValue("False");
        row1.createCell(5).setCellValue("TRUE_FALSE");
        row1.createCell(6).setCellValue(true); // Boolean cell value

        MultipartFile file = createExcelFile("test.xlsx", workbook);

        // When
        ExcelImportResponse response = excelImportService.importQuizzesFromExcel(examId, file, "APPEND");

        // Then
        assertNotNull(response);
        // Boolean cell will be converted to string "true", which is not a valid answer key (A, B, C, D)
        // So it should result in an error
        assertEquals(1, response.getErrorCount());
        assertTrue(response.getErrors().stream()
                .anyMatch(error -> error.getMessage().contains("Invalid answer key")));
        verify(examService, never()).createQuiz(any(QuizRequest.class));
    }
}

