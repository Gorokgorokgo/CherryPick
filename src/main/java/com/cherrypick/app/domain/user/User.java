package com.cherrypick.app.domain.user;

import com.cherrypick.app.domain.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Pattern(regexp = "^010[0-9]{8}$", message = "올바른 전화번호 형식이 아닙니다.")
    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false)
    private Long pointBalance = 0L;

    @Column(nullable = false)
    private Integer level = 1;

    @Column(nullable = false)
    private Long experience = 0L;
}