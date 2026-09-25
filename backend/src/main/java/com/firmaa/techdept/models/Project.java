package com.firmaa.techdept.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDate dueDate;

    @ManyToMany
    @JoinTable(
            name = "project_workstations",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "workstation_id")
    )
    private Set<Workstation> workstations = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "project_completions",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties({"password", "workstation", "role"})
    private Set<User> completedBy = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Set<Workstation> getWorkstations() {
        return workstations;
    }

    public void setWorkstations(Set<Workstation> workstations) {
        this.workstations = workstations;
    }

    public Set<User> getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(Set<User> completedBy) {
        this.completedBy = completedBy;
    }

}