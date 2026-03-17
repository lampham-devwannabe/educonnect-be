package com.sep.educonnect.service;

import com.sep.educonnect.dto.exam.ExcelImportResponse;
import com.sep.educonnect.dto.exam.QuizOptionRequest;
import com.sep.educonnect.dto.exam.QuizRequest;
import com.sep.educonnect.entity.Quiz;
import com.sep.educonnect.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private final ExamService examService;
    private final QuizRepository quizRepository;

    public enum ImportMode {
        APPEND, // Thêm mới, giữ nguyên quiz cũ
        REPLACE, // Xóa tất cả quiz cũ, thêm quiz mới
        UPDATE_OR_CREATE, // Nếu quiz đã tồn tại (theo text) thì update, không thì tạo mới
        SKIP_DUPLICATE // Nếu quiz đã tồn tại thì bỏ qua, không thì tạo mới
    }

    private static final int HEADER_ROW = 0;
    private static final int COL_QUESTIONS = 0; // Column A
    private static final int COL_OPTION_A = 1; // Column B
    private static final int COL_OPTION_B = 2; // Column C
    private static final int COL_OPTION_C = 3; // Column D
    private static final int COL_OPTION_D = 4; // Column E
    private static final int COL_QUIZ_TYPE = 5; // Column F
    private static final int COL_CORRECT_ANSWER = 6; // Column G
    private static final int COL_EXPLANATION = 7; // Column H
    private static final int COL_ORDER_NO = 8; // Column I

    @Transactional
    public ExcelImportResponse importQuizzesFromExcel(Long examId, MultipartFile file, String mode) {
        // Parse import mode
        ImportMode importMode;
        try {
            importMode = ImportMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid import mode: {}, using default APPEND", mode);
            importMode = ImportMode.APPEND;
        }

        return importQuizzesFromExcel(examId, file, importMode);
    }

    @Transactional
    private ExcelImportResponse importQuizzesFromExcel(Long examId, MultipartFile file, ImportMode importMode) {
        ExcelImportResponse response = ExcelImportResponse.builder()
                .errors(new ArrayList<>())
                .successCount(0)
                .errorCount(0)
                .totalRows(0)
                .build();

        // Validate file
        if (file == null || file.isEmpty()) {
            response.addError(0, "FILE", "File is empty or null", "");
            response.setErrorCount(1);
            return response;
        }

        // Validate file extension
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            response.addError(0, "FILE", "Invalid file format. Only .xlsx and .xls files are supported", filename);
            response.setErrorCount(1);
            return response;
        }

        // Handle REPLACE mode: delete all existing quizzes first
        if (importMode == ImportMode.REPLACE) {
            try {
                List<Quiz> existingQuizzes = quizRepository.findByExamId(examId);
                existingQuizzes.forEach(quiz -> examService.deleteQuiz(quiz.getQuizId()));
            } catch (Exception e) {
                log.error("Error deleting existing quizzes for REPLACE mode: {}", e.getMessage(), e);
                response.addError(0, "REPLACE", "Failed to delete existing quizzes: " + e.getMessage(), "");
                response.setErrorCount(response.getErrorCount() + 1);
                return response;
            }
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                response.addError(0, "FILE", "Excel file does not contain any sheets", filename);
                response.setErrorCount(1);
                return response;
            }

            int totalRows = sheet.getLastRowNum(); // Exclude header row
            response.setTotalRows(totalRows);

            // Start from row 1 (skip header row 0)
            for (int rowIndex = 1; rowIndex <= totalRows; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                try {
                    QuizRequest quizRequest = parseRowToQuizRequest(row, rowIndex + 1, examId, response);
                    if (quizRequest != null) {
                        boolean processed = processQuizRequest(quizRequest, importMode, rowIndex + 1, response);
                        if (processed) {
                            response.setSuccessCount(response.getSuccessCount() + 1);
                        } else {
                            response.setErrorCount(response.getErrorCount() + 1);
                        }
                    } else {
                        response.setErrorCount(response.getErrorCount() + 1);
                    }
                } catch (Exception e) {
                    log.error("Error importing row {}: {}", rowIndex + 1, e.getMessage(), e);
                    response.addError(rowIndex + 1, "ALL", "Unexpected error: " + e.getMessage(), "");
                    response.setErrorCount(response.getErrorCount() + 1);
                }
            }
        } catch (IOException e) {
            log.error("Error reading Excel file: {}", e.getMessage(), e);
            response.addError(0, "FILE", "Error reading file: " + e.getMessage(), filename);
            response.setErrorCount(1);
            return response;
        } catch (Exception e) {
            log.error("Unexpected error processing Excel file: {}", e.getMessage(), e);
            response.addError(0, "FILE", "Unexpected error: " + e.getMessage(), filename);
            response.setErrorCount(1);
            return response;
        }

        return response;
    }

    private QuizRequest parseRowToQuizRequest(Row row, int rowNumber, Long examId, ExcelImportResponse response) {
        // Get question text
        String questionText = getCellValueAsString(row, COL_QUESTIONS);
        if (questionText == null || questionText.trim().isEmpty()) {
            response.addError(rowNumber, "Questions", "Question text is required", "");
            return null;
        }

        // Get options
        List<QuizOptionRequest> options = new ArrayList<>();
        String optionA = getCellValueAsString(row, COL_OPTION_A);
        String optionB = getCellValueAsString(row, COL_OPTION_B);
        String optionC = getCellValueAsString(row, COL_OPTION_C);
        String optionD = getCellValueAsString(row, COL_OPTION_D);

        if (optionA != null && !optionA.trim().isEmpty()) {
            options.add(createOption(optionA, "A"));
        }
        if (optionB != null && !optionB.trim().isEmpty()) {
            options.add(createOption(optionB, "B"));
        }
        if (optionC != null && !optionC.trim().isEmpty()) {
            options.add(createOption(optionC, "C"));
        }
        if (optionD != null && !optionD.trim().isEmpty()) {
            options.add(createOption(optionD, "D"));
        }

        if (options.isEmpty()) {
            response.addError(rowNumber, "Options", "At least one option is required", "");
            return null;
        }

        // Get quiz type (default to SINGLE_CHOICE)
        String quizType = getCellValueAsString(row, COL_QUIZ_TYPE);
        if (quizType == null || quizType.trim().isEmpty()) {
            quizType = "SINGLE_CHOICE";
        } else {
            quizType = quizType.toUpperCase().trim();
            if (!isValidQuizType(quizType)) {
                response.addError(rowNumber, "Quiz Type",
                        "Invalid quiz type. Must be SINGLE_CHOICE, MULTIPLE_CHOICE, or TRUE_FALSE", quizType);
                return null;
            }
        }

        // Get correct answer
        String correctAnswer = getCellValueAsString(row, COL_CORRECT_ANSWER);
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            response.addError(rowNumber, "Correct Answer", "Correct answer is required", "");
            return null;
        }
        correctAnswer = correctAnswer.trim().toUpperCase();

        // Validate and set correct answers
        String[] correctAnswers = correctAnswer.split(",");
        String validAnswer = null;

        if (quizType.equals("SINGLE_CHOICE") || quizType.equals("TRUE_FALSE")) {
            if (correctAnswers.length > 1) {
                response.addError(rowNumber, "Correct Answer",
                        "Single choice questions can only have one correct answer", correctAnswer);
                return null;
            }
            String answerKey = correctAnswers[0].trim();
            validAnswer = findOptionTextByKey(options, answerKey);
            if (validAnswer == null) {
                response.addError(rowNumber, "Correct Answer", "Invalid answer key: " + answerKey, correctAnswer);
                return null;
            }
            // Mark option as correct
            markOptionAsCorrect(options, answerKey);
        } else if (quizType.equals("MULTIPLE_CHOICE")) {
            List<String> validAnswers = new ArrayList<>();
            for (String answerKey : correctAnswers) {
                answerKey = answerKey.trim();
                String optionText = findOptionTextByKey(options, answerKey);
                if (optionText == null) {
                    response.addError(rowNumber, "Correct Answer", "Invalid answer key: " + answerKey, correctAnswer);
                    return null;
                }
                validAnswers.add(optionText);
                markOptionAsCorrect(options, answerKey);
            }
            validAnswer = String.join(", ", validAnswers);
        }

        // Get explanation
        String explanation = getCellValueAsString(row, COL_EXPLANATION);
        if (explanation == null) {
            explanation = "";
        }

        // Get order number (default to row number)
        Integer orderNo = getCellValueAsInteger(row, COL_ORDER_NO);
        if (orderNo == null) {
            orderNo = rowNumber;
        }

        return QuizRequest.builder()
                .examId(examId)
                .text(questionText.trim())
                .orderNo(orderNo)
                .type(quizType)
                .validAnswer(validAnswer)
                .explanation(explanation.trim())
                .options(options)
                .build();
    }

    private QuizOptionRequest createOption(String text, String key) {
        return QuizOptionRequest.builder()
                .text(text.trim())
                .isCorrect(false)
                .build();
    }

    private void markOptionAsCorrect(List<QuizOptionRequest> options, String answerKey) {
        int index = answerKey.charAt(0) - 'A';
        if (index >= 0 && index < options.size()) {
            options.get(index).setIsCorrect(true);
        }
    }

    private String findOptionTextByKey(List<QuizOptionRequest> options, String answerKey) {
        int index = answerKey.charAt(0) - 'A';
        if (index >= 0 && index < options.size()) {
            return options.get(index).getText();
        }
        return null;
    }

    private boolean isValidQuizType(String type) {
        return type.equals("SINGLE_CHOICE") ||
                type.equals("MULTIPLE_CHOICE") ||
                type.equals("TRUE_FALSE");
    }

    private String getCellValueAsString(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Convert to string without decimal if it's a whole number
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private Integer getCellValueAsInteger(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i <= 8; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValueAsString(row, i);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean processQuizRequest(QuizRequest quizRequest, ImportMode importMode, int rowNumber,
                                       ExcelImportResponse response) {
        switch (importMode) {
            case APPEND:
                // Always create new quiz
                examService.createQuiz(quizRequest);
                return true;

            case REPLACE:
                // Already deleted all quizzes, just create new
                examService.createQuiz(quizRequest);
                return true;

            case UPDATE_OR_CREATE:
                // Check if quiz with same text exists
                return quizRepository.findByExamIdAndText(quizRequest.getExamId(), quizRequest.getText())
                        .map(existingQuiz -> {
                            // Update existing quiz
                            try {
                                examService.updateQuiz(existingQuiz.getQuizId(), quizRequest);
                                return true;
                            } catch (Exception e) {
                                log.error("Error updating quiz {}: {}", existingQuiz.getQuizId(), e.getMessage(), e);
                                response.addError(rowNumber, "UPDATE",
                                        "Failed to update existing quiz: " + e.getMessage(), quizRequest.getText());
                                return false;
                            }
                        })
                        .orElseGet(() -> {
                            // Create new quiz
                            examService.createQuiz(quizRequest);
                            return true;
                        });

            case SKIP_DUPLICATE:
                // Check if quiz with same text exists
                if (quizRepository.findByExamIdAndText(quizRequest.getExamId(), quizRequest.getText()).isPresent()) {
                    response.addError(rowNumber, "SKIP", "Quiz already exists, skipped", quizRequest.getText());
                    return false;
                } else {
                    // Create new quiz
                    examService.createQuiz(quizRequest);
                    return true;
                }

            default:
                // Default to APPEND behavior
                examService.createQuiz(quizRequest);
                return true;
        }
    }
}
