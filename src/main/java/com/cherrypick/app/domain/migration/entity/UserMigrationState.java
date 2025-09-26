package com.cherrypick.app.domain.migration.entity;

import com.cherrypick.app.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_migration_states")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMigrationState extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String currentStrategy;

    @Column(nullable = false, length = 20)
    private String assignedPhase;

    @Column
    private LocalDateTime migrationDate;

    @Column
    private String lastBackup;

    @Builder.Default
    @Column(nullable = false)
    private Boolean rollbackAvailable = false;

    @Builder.Default
    @Column(nullable = false)
    private Integer rollbackCount = 0;

    @Column
    private LocalDateTime rollbackDeadline;

    @Column(length = 100)
    private String userHash;
}