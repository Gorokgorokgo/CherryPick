package com.cherrypick.app.domain.common.repository;

import com.cherrypick.app.domain.common.entity.UploadedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UploadedImageRepository extends JpaRepository<UploadedImage, Long> {
    
    /**
     * S3 URL로 이미지 조회
     */
    Optional<UploadedImage> findByS3Url(String s3Url);
    
    /**
     * 업로더별 이미지 목록 조회
     */
    List<UploadedImage> findByUploaderIdOrderByCreatedAtDesc(Long uploaderId);
    
    /**
     * 폴더별 이미지 목록 조회
     */
    List<UploadedImage> findByFolderPathOrderByCreatedAtDesc(String folderPath);
    
    /**
     * 최근 업로드된 이미지 조회
     */
    @Query("SELECT ui FROM UploadedImage ui WHERE ui.createdAt >= :since ORDER BY ui.createdAt DESC")
    List<UploadedImage> findRecentUploads(@Param("since") LocalDateTime since);
    
    /**
     * 파일 크기별 통계 조회
     */
    @Query("SELECT SUM(ui.fileSize) FROM UploadedImage ui WHERE ui.uploaderId = :uploaderId")
    Long getTotalFileSizeByUploader(@Param("uploaderId") Long uploaderId);
}