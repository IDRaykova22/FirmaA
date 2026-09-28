package com.firmaa.techdept.repositories;

import com.firmaa.techdept.models.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByCompletedBy_Id(Long userId);

    List<Project> findByWorkstations_Id(Long workstationId);
}
