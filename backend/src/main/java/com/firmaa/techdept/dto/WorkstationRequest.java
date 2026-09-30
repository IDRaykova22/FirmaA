package com.firmaa.techdept.dto;

import java.util.List;

/**
 * Body of POST/PUT /api/admin/workstations. A missing computer count means 0.
 * On update, a missing {@code employeeIds} leaves the assigned employees untouched.
 */
public record WorkstationRequest(
        String title,
        String description,
        Integer computerCount,
        List<Long> employeeIds
) {
}
