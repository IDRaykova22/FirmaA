package com.firmaa.techdept.dto;

import com.firmaa.techdept.models.User;

import java.math.BigDecimal;

/** Safe view of a user for the admin dashboard: never includes the password hash. */
public record EmployeeResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String role,
        String department,
        String jobTitle,
        String phoneNumber,
        String address,
        String dateOfBirth,
        BigDecimal salary,
        String joinDate
) {
    public static EmployeeResponse from(User u) {
        // Missing text fields are sent as "" so the frontend can bind them to inputs directly
        return new EmployeeResponse(
                u.getId(),
                u.getUsername(),
                orEmpty(u.getFirstName()),
                orEmpty(u.getLastName()),
                u.getRole().name(),
                orEmpty(u.getDepartment()),
                orEmpty(u.getJobTitle()),
                orEmpty(u.getPhoneNumber()),
                orEmpty(u.getAddress()),
                u.getDateOfBirth() != null ? u.getDateOfBirth().toString() : "",
                u.getSalary(),
                u.getJoinDate() != null ? u.getJoinDate().toString() : ""
        );
    }

    static String orEmpty(String value) {
        return value != null ? value : "";
    }
}
