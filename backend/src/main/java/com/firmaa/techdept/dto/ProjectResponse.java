package com.firmaa.techdept.dto;

import com.firmaa.techdept.models.Project;
import com.firmaa.techdept.models.Role;

import java.math.BigDecimal;
import java.util.List;

public record ProjectResponse(
        Long id,
        String title,
        String description,
        String dueDate,
        BigDecimal projectValue,
        long employeeCount,
        List<WorkstationSummary> workstations,
        List<PersonSummary> completedBy
) {
    public record WorkstationSummary(Long id, String title) {
    }

    public static ProjectResponse from(Project p) {
        // Employees seated at the project's workstations (managers don't sit at one)
        long employeeCount = p.getWorkstations().stream()
                .flatMap(ws -> ws.getEmployees().stream())
                .filter(u -> u.getRole() == Role.ROLE_USER)
                .count();
        return new ProjectResponse(
                p.getId(),
                p.getTitle(),
                EmployeeResponse.orEmpty(p.getDescription()),
                p.getDueDate() != null ? p.getDueDate().toString() : "",
                p.getProjectValue(),
                employeeCount,
                p.getWorkstations().stream().map(ws -> new WorkstationSummary(ws.getId(), ws.getTitle())).toList(),
                p.getCompletedBy().stream().map(PersonSummary::from).toList()
        );
    }
}
