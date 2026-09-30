package com.firmaa.techdept.services;

import com.firmaa.techdept.dto.ProjectRequest;
import com.firmaa.techdept.dto.ProjectResponse;
import com.firmaa.techdept.exceptions.BadRequestException;
import com.firmaa.techdept.exceptions.NotFoundException;
import com.firmaa.techdept.exceptions.UnauthorizedException;
import com.firmaa.techdept.models.Project;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.models.Workstation;
import com.firmaa.techdept.repositories.ProjectRepository;
import com.firmaa.techdept.repositories.UserRepository;
import com.firmaa.techdept.repositories.WorkstationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Business logic for projects and their completion by employees. */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkstationRepository workstationRepository;
    private final UserRepository userRepository;

    public ProjectService(ProjectRepository projectRepository,
                         WorkstationRepository workstationRepository,
                         UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.workstationRepository = workstationRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAll() {
        return projectRepository.findAll().stream().map(ProjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        return ProjectResponse.from(findProject(id));
    }

    @Transactional
    public Project create(ProjectRequest request) {
        validate(request);

        Project project = new Project();
        applyFields(project, request);
        return projectRepository.save(project);
    }

    @Transactional
    public Project update(Long id, ProjectRequest request) {
        Project project = findProject(id);
        validate(request);

        applyFields(project, request);
        return projectRepository.save(project);
    }

    @Transactional
    public void delete(Long id) {
        // Project owns both join tables, so its workstation/completion rows go with it
        projectRepository.delete(findProject(id));
    }

    /** Records that the logged-in employee has finished their part of the project. */
    @Transactional
    public void markDone(Long projectId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        Project project = findProject(projectId);

        project.getCompletedBy().add(user);
        projectRepository.save(project);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void validate(ProjectRequest request) {
        if (request.title() == null || request.title().isBlank() || request.dueDate() == null) {
            throw new BadRequestException("Грешка: Заглавието и крайният срок са задължителни!");
        }
        if (request.projectValue() == null || request.projectValue().signum() < 0) {
            throw new BadRequestException("Грешка: Стойността на проекта е задължителна и не може да е отрицателна!");
        }
        if (request.workstationIds() == null || request.workstationIds().isEmpty()) {
            throw new BadRequestException("Проектът трябва да има поне едно работно място!");
        }
    }

    private void applyFields(Project project, ProjectRequest request) {
        project.setTitle(request.title());
        project.setDescription(request.description());
        project.setDueDate(request.dueDate());
        project.setProjectValue(request.projectValue());
        project.getWorkstations().clear();
        project.getWorkstations().addAll(findWorkstations(request.workstationIds()));
    }

    private Set<Workstation> findWorkstations(List<Long> ids) {
        Set<Workstation> workstations = new HashSet<>();
        for (Long id : ids) {
            workstationRepository.findById(id).ifPresent(workstations::add);
        }
        return workstations;
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Грешка: Проектът не е намерен!"));
    }
}
