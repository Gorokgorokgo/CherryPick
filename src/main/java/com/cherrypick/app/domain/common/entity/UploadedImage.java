package com.cherrypick.app.domain.common.entity;

import com.cherrypick.app.domain.common.enums.ImageStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "uploaded_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UploadedImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "folder_path", nullable = false)
    private String folderPath;

    @Column(name = "s3_url", nullable = false, length = 500)
    private String s3Url;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "uploader_id")
    private Long uploaderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ImageStatus status = ImageStatus.TEMP; // 기본값: 임시 업로드

    @Builder
    public UploadedImage(String originalFilename, String storedFilename, Long fileSize,
                        String contentType, String folderPath, String s3Url, String thumbnailUrl, Long uploaderId) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.folderPath = folderPath;
        this.s3Url = s3Url;
        this.thumbnailUrl = thumbnailUrl;
        this.uploaderId = uploaderId;
        this.status = ImageStatus.TEMP; // 기본값: 임시 업로드
    }

    /**
     * 이미지를 영구 저장 상태로 전환
     */
    public void markAsPermanent() {
        this.status = ImageStatus.PERMANENT;
    }

    /**
     * 임시 업로드 상태 확인
     */
    public boolean isTemporary() {
        return this.status == ImageStatus.TEMP;
    }
}
