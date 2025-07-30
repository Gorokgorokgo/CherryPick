package com.cherrypick.app.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "계좌 등록 요청")
@Getter
@Setter
public class AddAccountRequest {
    
    @Schema(description = "은행 코드", example = "004", required = true)
    @NotBlank(message = "은행 코드는 필수입니다.")
    private String bankCode;
    
    @Schema(description = "은행명", example = "국민은행", required = true)
    @NotBlank(message = "은행명은 필수입니다.")
    private String bankName;
    
    @Schema(description = "계좌번호", example = "123456789012", required = true)
    @NotBlank(message = "계좌번호는 필수입니다.")
    private String accountNumber;
    
    @Schema(description = "예금주명", example = "홍길동", required = true)
    @NotBlank(message = "예금주명은 필수입니다.")
    private String accountHolder;
    
    @Schema(description = "기본 계좌 설정 여부", example = "false")
    private Boolean isPrimary = false;
}