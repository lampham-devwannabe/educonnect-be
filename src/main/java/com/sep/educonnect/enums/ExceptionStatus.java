package com.sep.educonnect.enums;

public enum ExceptionStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Từ chối"),
    CANCELLED("Đã hủy");

    private final String description;

    ExceptionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
