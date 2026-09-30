package com.firmaa.techdept.services;

import com.firmaa.techdept.dto.WorkstationRequest;
import com.firmaa.techdept.dto.WorkstationResponse;
import com.firmaa.techdept.exceptions.BadRequestException;
import com.firmaa.techdept.exceptions.NotFoundException;
import com.firmaa.techdept.models.Project;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.models.Workstation;
import com.firmaa.techdept.repositories.ProjectRepository;
import com.firmaa.techdept.repositories.UserRepository;
import com.firmaa.techdept.repositories.WorkstationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Business logic for workstations and who sits at them. */
@Service
public class WorkstationService {

    private final WorkstationRepository workstationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    public WorkstationService(WorkstationRepository workstationRepository,
                              UserRepository userRepository,
                              ProjectRepository projectRepository) {
        this.workstationRepository = workstationRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkstationResponse> getAll() {
        return workstationRepository.findAll().stream().map(WorkstationResponse::from).toList();
    }

    @Transactional
    public Workstation create(WorkstationRequest request) {
        validate(request);

        Workstation ws = new Workstation();
        ws.setTitle(request.title());
        ws.setDescription(request.description());
        ws.setComputerCount(computerCountOf(request));
        Workstation saved = workstationRepository.save(ws);

        assignEmployees(saved, request.employeeIds());
        return saved;
    }

    @Transactional
    public Workstation update(Long id, WorkstationRequest request) {
        Workstation ws = findWorkstation(id);
        validate(request);

        ws.setTitle(request.title());
        ws.setDescription(request.description());
        ws.setComputerCount(computerCountOf(request));
        workstationRepository.save(ws);

        // Only touch the employee list when the client sends one
        if (request.employeeIds() != null) {
            replaceEmployees(ws, request.employeeIds());
        }
        return ws;
    }

    /** Replaces everyone at the workstation with the given employees. */
    @Transactional
    public void setEmployees(Long id, List<Long> employeeIds) {
        replaceEmployees(findWorkstation(id), employeeIds);
    }

    @Transactional
    public void delete(Long id) {
        Workstation ws = findWorkstation(id);
        List<Project> projects = projectRepository.findByWorkstations_Id(id);

        // A project must keep at least one workstation, so refuse if this is the last one
        List<String> blocking = projects.stream()
                .filter(p -> p.getWorkstations().size() <= 1)
                .map(Project::getTitle)
                .toList();
        if (!blocking.isEmpty()) {
            throw new BadRequestException("Грешка: Работното място е единственото за проект(и): "
                    + String.join(", ", blocking) + ". Първо редактирайте или изтрийте тези проекти.");
        }

        for (Project project : projects) {
            project.getWorkstations().removeIf(w -> w.getId().equals(id));
            projectRepository.save(project);
        }
        unassignEmployees(ws);

        workstationRepository.delete(ws);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void validate(WorkstationRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new BadRequestException("Грешка: Заглавието е задължително!");
        }
        if (computerCountOf(request) < 0) {
            throw new BadRequestException("Грешка: Броят компютри трябва да е цяло число, 0 или повече!");
        }
    }

    private static int computerCountOf(WorkstationRequest request) {
        return request.computerCount() != null ? request.computerCount() : 0;
    }

    private Workstation findWorkstation(Long id) {
        return workstationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Грешка: Работното място не е намерено!"));
    }

    private void replaceEmployees(Workstation ws, List<Long> employeeIds) {
        unassignEmployees(ws);
        assignEmployees(ws, employeeIds);
    }

    private void unassignEmployees(Workstation ws) {
        for (User emp : ws.getEmployees()) {
            emp.setWorkstation(null);
            userRepository.save(emp);
        }
        ws.getEmployees().clear();
    }

    private void assignEmployees(Workstation ws, List<Long> employeeIds) {
        if (employeeIds == null) {
            return;
        }
        for (Long empId : employeeIds) {
            userRepository.findById(empId).ifPresent(user -> {
                user.setWorkstation(ws);
                userRepository.save(user);
            });
        }
    }
}
