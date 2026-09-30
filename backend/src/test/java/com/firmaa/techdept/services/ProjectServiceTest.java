package com.firmaa.techdept.services;

import com.firmaa.techdept.dto.ProjectRequest;
import com.firmaa.techdept.dto.ProjectResponse;
import com.firmaa.techdept.exceptions.BadRequestException;
import com.firmaa.techdept.exceptions.NotFoundException;
import com.firmaa.techdept.models.Project;
import com.firmaa.techdept.models.Role;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.models.Workstation;
import com.firmaa.techdept.repositories.ProjectRepository;
import com.firmaa.techdept.repositories.UserRepository;
import com.firmaa.techdept.repositories.WorkstationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkstationRepository workstationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    private static final LocalDate DUE = LocalDate.of(2026, 12, 31);

    private static Workstation workstation(long id) {
        Workstation ws = new Workstation();
        ws.setId(id);
        ws.setTitle("WS" + id);
        return ws;
    }

    @Test
    void create_savesValueAndWorkstations() {
        Workstation ws = workstation(1);
        when(workstationRepository.findById(1L)).thenReturn(Optional.of(ws));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        Project saved = projectService.create(new ProjectRequest("Alpha", "d", DUE, new BigDecimal("12500.50"), List.of(1L)));

        assertThat(saved.getProjectValue()).isEqualByComparingTo("12500.50");
        assertThat(saved.getWorkstations()).containsExactly(ws);
    }

    @Test
    void create_missingValue_isRejected() {
        assertThatThrownBy(() -> projectService.create(new ProjectRequest("Alpha", null, DUE, null, List.of(1L))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Стойността");
    }

    @Test
    void create_negativeValue_isRejected() {
        assertThatThrownBy(() -> projectService.create(new ProjectRequest("Alpha", null, DUE, new BigDecimal("-1"), List.of(1L))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_withoutWorkstations_isRejected() {
        assertThatThrownBy(() -> projectService.create(new ProjectRequest("Alpha", null, DUE, BigDecimal.TEN, List.of())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("работно място");
    }

    @Test
    void create_withoutDueDate_isRejected() {
        assertThatThrownBy(() -> projectService.create(new ProjectRequest("Alpha", null, null, BigDecimal.TEN, List.of(1L))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("крайният срок");
    }

    @Test
    void update_replacesWorkstations() {
        Workstation oldWs = workstation(1);
        Workstation newWs = workstation(2);
        Project project = new Project();
        project.getWorkstations().add(oldWs);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));
        when(workstationRepository.findById(2L)).thenReturn(Optional.of(newWs));
        when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

        projectService.update(5L, new ProjectRequest("Beta", null, DUE, BigDecimal.ONE, List.of(2L)));

        assertThat(project.getTitle()).isEqualTo("Beta");
        assertThat(project.getWorkstations()).containsExactly(newWs);
    }

    @Test
    void markDone_addsUserToCompletedBy() {
        User ivan = new User();
        ivan.setId(3L);
        Project project = new Project();
        when(userRepository.findByUsername("ivan")).thenReturn(Optional.of(ivan));
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));

        projectService.markDone(5L, "ivan");

        assertThat(project.getCompletedBy()).containsExactly(ivan);
    }

    @Test
    void getById_countsOnlyEmployeesNotManagers() {
        Workstation ws = workstation(1);
        User employee = new User();
        employee.setRole(Role.ROLE_USER);
        User manager = new User();
        manager.setRole(Role.ROLE_ADMIN);
        ws.getEmployees().addAll(List.of(employee, manager));
        Project project = new Project();
        project.setId(5L);
        project.setTitle("Alpha");
        project.setDueDate(DUE);
        project.getWorkstations().add(ws);
        when(projectRepository.findById(5L)).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.getById(5L);

        assertThat(response.employeeCount()).isEqualTo(1);
        assertThat(response.dueDate()).isEqualTo("2026-12-31");
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        when(projectRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(9L)).isInstanceOf(NotFoundException.class);
    }
}
