package com.firmaa.techdept.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** The logged-in user's own profile, with their workstation and its projects (null if unassigned). */
public record MyInfoResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String role,
        String department,
        String jobTitle,
        String address,
        LocalDate dateOfBirth,
        BigDecimal salary,
        String phoneNumber,
        LocalDate joinDate,
        MyWorkstation workstation
) {
    public record MyWorkstation(Long id, String title, String description, int computerCount, List<MyProject> projects) {
    }

    public record MyProject(Long id, String title, String description, LocalDate dueDate, boolean completedByMe) {
    }
}
