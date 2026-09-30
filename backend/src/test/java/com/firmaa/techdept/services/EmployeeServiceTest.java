package com.firmaa.techdept.services;

import com.firmaa.techdept.dto.EmployeeRequest;
import com.firmaa.techdept.exceptions.BadRequestException;
import com.firmaa.techdept.exceptions.NotFoundException;
import com.firmaa.techdept.models.Project;
import com.firmaa.techdept.models.Role;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.models.Workstation;
import com.firmaa.techdept.repositories.ProjectRepository;
import com.firmaa.techdept.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private EmployeeService employeeService;

    private static EmployeeRequest request(String role, String department, LocalDate dob, BigDecimal salary, String phone) {
        return new EmployeeRequest("ivan", "Str0ng!pass", "Ivan", "Petrov", role, department,
                "Developer", "Burgas", dob, salary, phone);
    }

    private static EmployeeRequest validEmployee() {
        return request("ROLE_USER", null, LocalDate.of(1995, 1, 1), new BigDecimal("1500"), "0888123456");
    }

    private static User existing(long id, String username, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("old-hash");
        user.setRole(role);
        return user;
    }

    // ── create ──────────────────────────────────────────────────────────

    @Test
    void create_validEmployee_savesWithHashedPasswordAndDetails() {
        when(encoder.encode("Str0ng!pass")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = employeeService.create(validEmployee());

        assertThat(saved.getPassword()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(saved.getFirstName()).isEqualTo("Ivan");
        assertThat(saved.getLastName()).isEqualTo("Petrov");
        assertThat(saved.getDepartment()).isNull();
        assertThat(saved.getSalary()).isEqualByComparingTo("1500");
    }

    @Test
    void create_manager_keepsDepartment() {
        when(encoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = employeeService.create(request("ROLE_ADMIN", " QA ", null, null, null));

        assertThat(saved.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(saved.getDepartment()).isEqualTo("QA");
    }

    @Test
    void create_managerWithoutDepartment_isRejected() {
        when(encoder.encode(any())).thenReturn("hashed");

        assertThatThrownBy(() -> employeeService.create(request("ROLE_ADMIN", " ", null, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Отделът е задължителен");
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_takenUsername_isRejected() {
        when(userRepository.existsByUsername("ivan")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(validEmployee()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("заето");
    }

    @Test
    void create_weakPassword_isRejected() {
        EmployeeRequest weak = new EmployeeRequest("ivan", "password", "Ivan", "Petrov", "ROLE_USER", null,
                null, null, null, null, null);

        assertThatThrownBy(() -> employeeService.create(weak))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Паролата");
    }

    @Test
    void create_missingLastName_isRejected() {
        when(encoder.encode(any())).thenReturn("hashed");
        EmployeeRequest noName = new EmployeeRequest("ivan", "Str0ng!pass", "Ivan", "", "ROLE_USER", null,
                null, null, null, null, null);

        assertThatThrownBy(() -> employeeService.create(noName))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Името и фамилията");
    }

    @Test
    void create_salaryBelowMinimumWage_isRejected() {
        when(encoder.encode(any())).thenReturn("hashed");

        assertThatThrownBy(() -> employeeService.create(request("ROLE_USER", null, null, new BigDecimal("620.19"), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("620.20");
    }

    @Test
    void create_salaryExactlyMinimumWage_isAccepted() {
        when(encoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = employeeService.create(request("ROLE_USER", null, null, new BigDecimal("620.20"), null));

        assertThat(saved.getSalary()).isEqualByComparingTo("620.20");
    }

    @Test
    void create_under18_isRejected() {
        when(encoder.encode(any())).thenReturn("hashed");
        LocalDate seventeen = LocalDate.now().minusYears(17);

        assertThatThrownBy(() -> employeeService.create(request("ROLE_USER", null, seventeen, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("18");
    }

    @Test
    void create_invalidPhone_isRejected() {
        when(encoder.encode(any())).thenReturn("hashed");

        assertThatThrownBy(() -> employeeService.create(request("ROLE_USER", null, null, null, "12345")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("телефонен");
    }

    // ── update ──────────────────────────────────────────────────────────

    @Test
    void update_blankPassword_keepsCurrentPassword() {
        User user = existing(2, "ivan", Role.ROLE_USER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        EmployeeRequest noPassword = new EmployeeRequest("ivan", "", "Ivan", "Petrov", "ROLE_USER", null,
                "Lead", null, null, null, null);

        employeeService.update(2L, noPassword, "admin");

        assertThat(user.getPassword()).isEqualTo("old-hash");
        assertThat(user.getJobTitle()).isEqualTo("Lead");
        verify(encoder, never()).encode(any());
    }

    @Test
    void update_promotionToManager_freesWorkstationSeat() {
        User user = existing(2, "ivan", Role.ROLE_USER);
        user.setWorkstation(new Workstation());
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        employeeService.update(2L, new EmployeeRequest("ivan", "", "Ivan", "Petrov", "ROLE_ADMIN", "QA",
                null, null, null, null, null), "admin");

        assertThat(user.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(user.getWorkstation()).isNull();
    }

    @Test
    void update_renamingYourself_isRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing(1, "admin", Role.ROLE_ADMIN)));

        assertThatThrownBy(() -> employeeService.update(1L, new EmployeeRequest("boss", "", "A", "B", "ROLE_ADMIN", "IT",
                null, null, null, null, null), "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("потребителско име");
    }

    @Test
    void update_changingYourOwnRole_isRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing(1, "admin", Role.ROLE_ADMIN)));

        assertThatThrownBy(() -> employeeService.update(1L, new EmployeeRequest("admin", "", "A", "B", "ROLE_USER", null,
                null, null, null, null, null), "admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("роля");
    }

    @Test
    void update_unknownId_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.update(99L, validEmployee(), "admin"))
                .isInstanceOf(NotFoundException.class);
    }

    // ── delete ──────────────────────────────────────────────────────────

    @Test
    void delete_removesCompletionsThenUser() {
        User user = existing(2, "ivan", Role.ROLE_USER);
        Project project = new Project();
        project.setCompletedBy(new HashSet<>(Set.of(user)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(projectRepository.findByCompletedBy_Id(2L)).thenReturn(List.of(project));

        employeeService.delete(2L, "admin");

        assertThat(project.getCompletedBy()).isEmpty();
        verify(userRepository).delete(user);
    }

    @Test
    void delete_yourself_isRejected() {
        User admin = existing(1, "admin", Role.ROLE_ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> employeeService.delete(1L, "admin"))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).delete(any());
    }
}
