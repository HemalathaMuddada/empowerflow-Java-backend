package com.hrms.employee.core.enums;

public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    COMPLETED,
    CLOSED_SUCCESS, // For tasks that have a specific success/failure outcome
    CLOSED_FAILED
}
