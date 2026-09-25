package com.firmaa.techdept.repositories;

import com.firmaa.techdept.TestcontainersConfiguration;
import com.firmaa.techdept.models.Project;
import com.firmaa.techdept.models.Workstation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkstationRepository workstationRepository;

    private Workstation savedWorkstation() {
        Workstation ws = new Workstation();
        ws.setTitle("Dev Station");
        return workstationRepository.save(ws);
    }

    private Project createProject(String title) {
        Project project = new Project();
        project.setTitle(title);
        project.setDueDate(LocalDate.now().plusDays(30));
        project.setWorkstations(Set.of(savedWorkstation()));
        return projectRepository.save(project);
    }

    @Test
    void save_persistsProjectCorrectly() {
        Project saved = createProject("Alpha");

        assertNotNull(saved.getId());
        assertEquals("Alpha", saved.getTitle());
    }

    @Test
    void findById_existingProject_returnsProject() {
        Project saved = createProject("Beta");

        Optional<Project> result = projectRepository.findById(saved.getId());

        assertTrue(result.isPresent());
        assertEquals("Beta", result.get().getTitle());
    }

    @Test
    void findById_nonExistingProject_returnsEmpty() {
        Optional<Project> result = projectRepository.findById(9999L);

        assertFalse(result.isPresent());
    }

    @Test
    void delete_removesProject() {
        Project saved = createProject("Gamma");
        projectRepository.delete(saved);

        assertFalse(projectRepository.findById(saved.getId()).isPresent());
    }

    @Test
    void findAll_returnsAllSavedProjects() {
        createProject("Project X");
        createProject("Project Y");

        assertEquals(2, projectRepository.findAll().size());
    }
}
