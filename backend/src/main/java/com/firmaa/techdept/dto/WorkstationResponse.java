package com.firmaa.techdept.dto;

import com.firmaa.techdept.models.Workstation;

import java.util.List;

public record WorkstationResponse(
        Long id,
        String title,
        String description,
        int computerCount,
        List<PersonSummary> employees
) {
    public static WorkstationResponse from(Workstation ws) {
        return new WorkstationResponse(
                ws.getId(),
                ws.getTitle(),
                EmployeeResponse.orEmpty(ws.getDescription()),
                ws.getComputerCount() != null ? ws.getComputerCount() : 0,
                ws.getEmployees().stream().map(PersonSummary::from).toList()
        );
    }
}
