package com.cherrypick.app.domain.common;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "regions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Region {

    @Id
    @Column(name = "code")
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_code")
    private String parentCode;

    @Column(nullable = false)
    private Integer level;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}