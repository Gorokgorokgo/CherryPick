package com.cherrypick.app.domain.user.entity;

import com.cherrypick.app.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bank_code", nullable = false)
    private String bankCode;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "account_number", nullable = false)
    private String accountNumber; // 암호화된 계좌번호

    @Column(name = "account_holder", nullable = false)
    private String accountHolder;

    @Builder.Default
    @Column(name = "is_verified", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isVerified = false;

    @Builder.Default
    @Column(name = "is_primary", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isPrimary = false;

    /**
     * 계좌 삭제 가능 여부 확인
     */
    public boolean canDelete() {
        return !this.isPrimary;
    }
}