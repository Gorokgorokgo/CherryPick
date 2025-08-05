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
    private Long id;
    
    @Schema(description = "이미지 URL", example = "https://cherrypick-bucket.s3.ap-northeast-2.amazonaws.com/general/abc123.jpg")
    private String imageUrl;
    
    @Schema(description = "원본 파일명", example = "sample.jpg")
    private String originalFilename;
    
    @Schema(description = "파일 크기 (bytes)", example = "1024000")
    private Long fileSize;
    
    @Schema(description = "MIME 타입", example = "image/jpeg")
    private String contentType;
    
    @Schema(description = "폴더 경로", example = "general")
    private String folderPath;
    
    /**
     * UploadedImage 엔티티를 응답 DTO로 변환
     */
    public static ImageUploadResponse from(UploadedImage uploadedImage) {
        return ImageUploadResponse.builder()
                .id(uploadedImage.getId())
                .imageUrl(uploadedImage.getS3Url())
                .originalFilename(uploadedImage.getOriginalFilename())
                .fileSize(uploadedImage.getFileSize())
                .contentType(uploadedImage.getContentType())
                .folderPath(uploadedImage.getFolderPath())
                .build();
    }
}