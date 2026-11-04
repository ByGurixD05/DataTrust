package com.datatrust.api.validation.dto;

import java.util.List;
import java.util.Map;

import com.datatrust.application.validation.ValidationReport;

public record ValidationReportResponse(
        long rows,
        boolean valid,
        List<String> headerErrors,
        Map<String, Long> errorsByColumn,
        List<CellErrorResponse> samples) {

    public static ValidationReportResponse from(ValidationReport report) {
        return new ValidationReportResponse(
                report.rows(),
                report.valid(),
                report.headerErrors(),
                report.errorsByColumn(),
                report.samples().stream().map(CellErrorResponse::from).toList());
    }

    public record CellErrorResponse(long row, String column, String value, String reason) {

        static CellErrorResponse from(ValidationReport.CellError cell) {
            return new CellErrorResponse(cell.row(), cell.column(), cell.value(), cell.reason());
        }
    }
}
