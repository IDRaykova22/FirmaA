package com.firmaa.techdept.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body of POST/PUT /api/admin/employees. Used for both employees and managers;
 * {@code role} decides which ("ROLE_ADMIN" = manager, anything else = employee).
 * On update, a blank password keeps the current one and a missing role keeps the current role.
 */
public record EmployeeRequest(
        String username,
        String password,
        String firstName,
        String lastName,
        String role,
        String department,
        String jobTitle,
        String address,
        LocalDate dateOfBirth,
        BigDecimal salary,
        String phoneNumber
) {
}
