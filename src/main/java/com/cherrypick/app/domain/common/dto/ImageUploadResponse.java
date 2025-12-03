package com.cherrypick.app.domain.common.dto;

import com.cherrypick.app.domain.common.entity.UploadedImage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "이미지 업로드 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageUploadResponse {

    @Schema(description = "이미지 ID", example = "1")
    private Long imageId;

    @Schema(description = "원본 이미지 URL", example = "https://cherrypick-bucket.kr.object.ncloudstorage.com/general/original/abc123.jpg")
    private String originalUrl;

    @Schema(description = "썸네일 이미지 URL", example = "https://cherrypick-bucket.kr.object.ncloudstorage.com/general/thumbnail/abc123.jpg")
    private String thumbnailUrl;

    /**
     * UploadedImage 엔티티를 응답 DTO로 변환
     */
    public static ImageUploadResponse from(UploadedImage uploadedImage) {
        return ImageUploadResponse.builder()
                .imageId(uploadedImage.getId())
                .originalUrl(uploadedImage.getOriginalUrl())
                .thumbnailUrl(uploadedImage.getThumbnailUrl())
                .build();
    }
}