package com.cherrypick.app.domain.qna.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuestionRequest {

    @NotBlank(message = "질문 내용을 입력해주세요.")
    @Size(max = 1000, message = "질문은 1,000자 이내로 작성해주세요.")
    private String content;
}