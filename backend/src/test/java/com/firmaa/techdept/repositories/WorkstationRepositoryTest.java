package com.firmaa.techdept.repositories;

import com.firmaa.techdept.models.Workstation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class WorkstationRepositoryTest {

    @Autowired
    private WorkstationRepository workstationRepository;

    private Workstation createWorkstation(String title) {
        Workstation ws = new Workstation();
        ws.setTitle(title);
        ws.setDescription("Description for " + title);
        return workstationRepository.save(ws);
    }

    @Test
    void save_persistsWorkstationCorrectly() {
        Workstation saved = createWorkstation("Station A");

        assertNotNull(saved.getId());
        assertEquals("Station A", saved.getTitle());
        assertEquals("Description for Station A", saved.getDescription());
    }

    @Test
    void findById_existingWorkstation_returnsWorkstation() {
        Workstation saved = createWorkstation("Station B");

        Optional<Workstation> result = workstationRepository.findById(saved.getId());

        assertTrue(result.isPresent());
        assertEquals("Station B", result.get().getTitle());
    }

    @Test
    void findById_nonExistingWorkstation_returnsEmpty() {
        Optional<Workstation> result = workstationRepository.findById(9999L);

        assertFalse(result.isPresent());
    }

    @Test
    void findAll_returnsAllSavedWorkstations() {
        createWorkstation("Station C");
        createWorkstation("Station D");

        List<Workstation> all = workstationRepository.findAll();

        assertEquals(2, all.size());
    }

    @Test
    void delete_removesWorkstation() {
        Workstation saved = createWorkstation("Station E");
        workstationRepository.delete(saved);

        assertFalse(workstationRepository.findById(saved.getId()).isPresent());
    }
}
