package com.firmaa.techdept.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Body of POST/PUT /api/admin/projects. The value is in EUR. */
public record ProjectRequest(
        String title,
        String description,
        LocalDate dueDate,
        BigDecimal projectValue,
        List<Long> workstationIds
) {
}
