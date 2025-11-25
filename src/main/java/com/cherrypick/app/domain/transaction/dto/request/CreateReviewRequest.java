package com.cherrypick.app.domain.transaction.dto.request;

import com.cherrypick.app.domain.transaction.enums.RatingType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

    @NotNull(message = "평가 타입은 필수입니다")
    private RatingType ratingType;

    @Size(max = 200, message = "후기 내용은 200자 이하로 작성해주세요")
    private String content;
}
