package com.sep.educonnect.controller;

import com.sep.educonnect.dto.common.ApiResponse;
import com.sep.educonnect.dto.exam.*;
import com.sep.educonnect.service.ExamService;
import com.sep.educonnect.service.ExcelImportService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamController {
    ExamService examService;
    ExcelImportService excelImportService;

    @PostMapping
    public ApiResponse<ExamResponse> createExam(@RequestBody ExamRequest request) {
        return ApiResponse.<ExamResponse>builder().result(examService.createExam(request)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ExamResponse> getExam(@PathVariable Long id) {
        return ApiResponse.<ExamResponse>builder().result(examService.getById(id)).build();
    }

    @GetMapping("/lessons/{lessonId}")
    public ApiResponse<Page<ExamResponse>> getExamsByLesson(
            @PathVariable Long lessonId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ApiResponse.<Page<ExamResponse>>builder()
                .result(examService.getExamsByLesson(lessonId, page, size, sortBy, direction))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<ExamResponse> updateExam(
            @PathVariable Long id, @RequestBody ExamRequest request) {
        return ApiResponse.<ExamResponse>builder()
                .result(examService.updateExam(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ApiResponse.<Void>builder().build();
    }

    // Quiz endpoints - nested under exam
    @PostMapping("/{examId}/quizzes")
    public ApiResponse<QuizResponse> createQuiz(
            @PathVariable Long examId, @RequestBody QuizRequest request) {
        request.setExamId(examId); // Ensure examId is set from path
        return ApiResponse.<QuizResponse>builder().result(examService.createQuiz(request)).build();
    }

    @GetMapping("/{examId}/quizzes")
    public ApiResponse<Page<QuizResponse>> getQuizzesByExam(
            @PathVariable Long examId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "orderNo") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ApiResponse.<Page<QuizResponse>>builder()
                .result(examService.getQuizzesByExam(examId, page, size, sortBy, direction))
                .build();
    }

    @GetMapping("/{examId}/quizzes/{quizId}")
    public ApiResponse<QuizResponse> getQuiz(@PathVariable Long examId, @PathVariable Long quizId) {
        return ApiResponse.<QuizResponse>builder()
                .result(examService.getQuizByIdAndExamId(quizId, examId))
                .build();
    }

    @PutMapping("/{examId}/quizzes/{quizId}")
    public ApiResponse<QuizResponse> updateQuiz(
            @PathVariable Long examId,
            @PathVariable Long quizId,
            @RequestBody QuizRequest request) {
        request.setExamId(examId); // Ensure examId is set from path
        return ApiResponse.<QuizResponse>builder()
                .result(examService.updateQuiz(quizId, request))
                .build();
    }

    @DeleteMapping("/{examId}/quizzes/{quizId}")
    public ApiResponse<Void> deleteQuiz(@PathVariable Long examId, @PathVariable Long quizId) {
        examService.deleteQuiz(quizId);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping(value = "/{examId}/quizzes/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ExcelImportResponse> importQuizzesFromExcel(
            @PathVariable Long examId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mode", defaultValue = "APPEND") String mode) {
        ExcelImportResponse response =
                excelImportService.importQuizzesFromExcel(examId, file, mode);
        return ApiResponse.<ExcelImportResponse>builder().result(response).build();
    }

    @GetMapping("/quizzes/template")
    public ResponseEntity<byte[]> downloadExcelTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Quiz Import Template");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            // Create data style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Questions",
                "A",
                "B",
                "C",
                "D",
                "Quiz Type",
                "Correct Answer",
                "Explanation",
                "Order No"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create sample data rows
            Object[][] sampleData = {
                {
                    "Tốc độ ánh sáng trong chân không xấp xỉ bằng bao nhiêu?",
                    "150,000,000 m/s",
                    "299,792,458 m/s",
                    "3,000,000 m/s",
                    "30,000 km/s",
                    "SINGLE_CHOICE",
                    "B",
                    "Giá trị chuẩn quốc tế là 299,792,458 m/s.",
                    1
                },
                {
                    "Công thức hóa học của nước là gì?",
                    "H2O",
                    "CO2",
                    "O2",
                    "H2O2",
                    "SINGLE_CHOICE",
                    "A",
                    "Nước gồm 2 nguyên tử H và 1 nguyên tử O.",
                    2
                },
                {
                    "Đơn vị đo cường độ dòng điện trong hệ SI là gì?",
                    "Volt",
                    "Ohm",
                    "Ampere",
                    "Watt",
                    "SINGLE_CHOICE",
                    "C",
                    "Ampere (A) là đơn vị chuẩn SI cho cường độ dòng điện.",
                    3
                },
                {
                    "Hãy chọn các nguyên tố thuộc nhóm halogen:",
                    "Fluor",
                    "Chlor",
                    "Brom",
                    "Iod",
                    "MULTIPLE_CHOICE",
                    "A,B,C,D",
                    "Tất cả các nguyên tố trên đều thuộc nhóm halogen trong bảng tuần hoàn.",
                    4
                },
                {
                    "Trái Đất quay quanh Mặt Trời?",
                    "True",
                    "False",
                    "",
                    "",
                    "TRUE_FALSE",
                    "A",
                    "Trái Đất quay quanh Mặt Trời theo quỹ đạo hình elip.",
                    5
                }
            };

            for (int rowIndex = 0; rowIndex < sampleData.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                Object[] rowData = sampleData[rowIndex];
                for (int colIndex = 0; colIndex < rowData.length; colIndex++) {
                    Cell cell = row.createCell(colIndex);
                    Object value = rowData[colIndex];
                    if (value instanceof String) {
                        cell.setCellValue((String) value);
                    } else if (value instanceof Integer) {
                        cell.setCellValue((Integer) value);
                    }
                    cell.setCellStyle(dataStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Add some padding
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
            }

            // Write workbook to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            byte[] bytes = outputStream.toByteArray();

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(
                    MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            httpHeaders.setContentDispositionFormData("attachment", "quiz_import_template.xlsx");
            httpHeaders.setContentLength(bytes.length);

            return ResponseEntity.ok().headers(httpHeaders).body(bytes);
        }
    }
}
