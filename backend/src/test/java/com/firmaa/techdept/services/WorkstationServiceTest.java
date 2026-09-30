package com.firmaa.techdept.services;

import com.firmaa.techdept.dto.WorkstationRequest;
import com.firmaa.techdept.exceptions.BadRequestException;
import com.firmaa.techdept.exceptions.NotFoundException;
import com.firmaa.techdept.models.Project;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkstationServiceTest {

    @Mock
    private WorkstationRepository workstationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private WorkstationService workstationService;

    private static Workstation workstation(long id) {
        Workstation ws = new Workstation();
        ws.setId(id);
        ws.setTitle("WS" + id);
        return ws;
    }

    @Test
    void create_savesComputerCountAndSeatsEmployees() {
        User ivan = new User();
        when(workstationRepository.save(any(Workstation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(5L)).thenReturn(Optional.of(ivan));

        Workstation saved = workstationService.create(new WorkstationRequest("Dev", "desc", 6, List.of(5L)));

        assertThat(saved.getComputerCount()).isEqualTo(6);
        assertThat(ivan.getWorkstation()).isSameAs(saved);
    }

    @Test
    void create_missingComputerCount_meansZero() {
        when(workstationRepository.save(any(Workstation.class))).thenAnswer(inv -> inv.getArgument(0));

        Workstation saved = workstationService.create(new WorkstationRequest("Dev", null, null, null));

        assertThat(saved.getComputerCount()).isZero();
    }

    @Test
    void create_negativeComputerCount_isRejected() {
        assertThatThrownBy(() -> workstationService.create(new WorkstationRequest("Dev", null, -1, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("компютри");
    }

    @Test
    void create_blankTitle_isRejected() {
        assertThatThrownBy(() -> workstationService.create(new WorkstationRequest(" ", null, 1, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Заглавието");
    }

    @Test
    void update_withoutEmployeeIds_leavesEmployeesAlone() {
        Workstation ws = workstation(1);
        User seated = new User();
        seated.setWorkstation(ws);
        ws.getEmployees().add(seated);
        when(workstationRepository.findById(1L)).thenReturn(Optional.of(ws));

        workstationService.update(1L, new WorkstationRequest("Renamed", null, 3, null));

        assertThat(ws.getTitle()).isEqualTo("Renamed");
        assertThat(seated.getWorkstation()).isSameAs(ws);
    }

    @Test
    void delete_lastWorkstationOfAProject_isRejected() {
        Workstation ws = workstation(1);
        Project project = new Project();
        project.setTitle("Alpha");
        project.setWorkstations(new HashSet<>(Set.of(ws)));
        when(workstationRepository.findById(1L)).thenReturn(Optional.of(ws));
        when(projectRepository.findByWorkstations_Id(1L)).thenReturn(List.of(project));

        assertThatThrownBy(() -> workstationService.delete(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Alpha");
        verify(workstationRepository, never()).delete(any());
    }

    @Test
    void delete_unseatsEmployeesAndDetachesFromProjects() {
        Workstation ws = workstation(1);
        Workstation other = workstation(2);
        User seated = new User();
        seated.setWorkstation(ws);
        ws.getEmployees().add(seated);
        Project project = new Project();
        project.setWorkstations(new HashSet<>(Set.of(ws, other)));
        when(workstationRepository.findById(1L)).thenReturn(Optional.of(ws));
        when(projectRepository.findByWorkstations_Id(1L)).thenReturn(List.of(project));

        workstationService.delete(1L);

        assertThat(seated.getWorkstation()).isNull();
        assertThat(project.getWorkstations()).containsExactly(other);
        verify(workstationRepository).delete(ws);
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        when(workstationRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workstationService.delete(9L)).isInstanceOf(NotFoundException.class);
    }
}
