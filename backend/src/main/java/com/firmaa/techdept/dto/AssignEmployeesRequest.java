package com.firmaa.techdept.dto;

import java.util.List;

/** Body of PUT /api/admin/workstations/{id}/employees: the full new list of employees. */
public record AssignEmployeesRequest(List<Long> employeeIds) {
}
