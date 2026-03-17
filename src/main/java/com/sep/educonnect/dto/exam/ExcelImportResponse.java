package com.sep.educonnect.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportResponse {
    private Integer totalRows;
    private Integer successCount;
    private Integer errorCount;
    private List<ImportError> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportError {
        private Integer rowNumber;
        private String column;
        private String message;
        private String data;
    }

    public void addError(Integer rowNumber, String column, String message, String data) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(ImportError.builder()
                .rowNumber(rowNumber)
                .column(column)
                .message(message)
                .data(data)
                .build());
    }
}
