package com.stepsentry.evidence;

import com.stepsentry.workflow.WorkflowRequirement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "evidence")
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id", nullable = false)
    private WorkflowRequirement requirement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EvidenceType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 2048)
    private String evidenceUrl;

    @Column(length = 2000)
    private String description;

    @Column(length = 100)
    private String sourceVersion;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Evidence() {
    }

    public Evidence(
            WorkflowRequirement requirement,
            EvidenceType type,
            String title,
            String evidenceUrl,
            String description,
            String sourceVersion
    ) {
        this.requirement = requirement;
        this.type = type;
        this.title = title;
        this.evidenceUrl = evidenceUrl;
        this.description = description;
        this.sourceVersion = sourceVersion;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public WorkflowRequirement getRequirement() {
        return requirement;
    }

    public EvidenceType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
