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
     * NCP URL로 이미지 조회
     */
    @Query("SELECT ui FROM UploadedImage ui " +
           "WHERE ui.s3Url = :s3Url " +
           "AND ui.deletedAt IS NULL")
    Optional<UploadedImage> findByS3Url(@Param("s3Url") String s3Url);

    /**
     * NCP URL 목록으로 이미지 일괄 조회
     */
    @Query("SELECT ui FROM UploadedImage ui " +
           "WHERE ui.s3Url IN :s3Urls " +
           "AND ui.deletedAt IS NULL")
    List<UploadedImage> findByS3UrlIn(@Param("s3Urls") List<String> s3Urls);

    /**
     * 업로더별 이미지 목록 조회
     */
    @Query("SELECT ui FROM UploadedImage ui " +
           "WHERE ui.uploaderId = :uploaderId " +
           "AND ui.deletedAt IS NULL " +
           "ORDER BY ui.createdAt DESC")
    List<UploadedImage> findByUploaderIdOrderByCreatedAtDesc(@Param("uploaderId") Long uploaderId);

    /**
     * 폴더별 이미지 목록 조회
     */
    @Query("SELECT ui FROM UploadedImage ui " +
           "WHERE ui.folderPath = :folderPath " +
           "AND ui.deletedAt IS NULL " +
           "ORDER BY ui.createdAt DESC")
    List<UploadedImage> findByFolderPathOrderByCreatedAtDesc(@Param("folderPath") String folderPath);

    /**
     * 최근 업로드된 이미지 조회
     */
    @Query("SELECT ui FROM UploadedImage ui " +
           "WHERE ui.createdAt >= :since " +
           "AND ui.deletedAt IS NULL " +
           "ORDER BY ui.createdAt DESC")
    List<UploadedImage> findRecentUploads(@Param("since") LocalDateTime since);

    /**
     * 파일 크기별 통계 조회
     */
    @Query("SELECT SUM(ui.fileSize) FROM UploadedImage ui " +
           "WHERE ui.uploaderId = :uploaderId " +
           "AND ui.deletedAt IS NULL")
    Long getTotalFileSizeByUploader(@Param("uploaderId") Long uploaderId);

    /**
     * 오래된 임시 이미지 조회 (배치 정리용)
     * 지정된 시간보다 오래되고 TEMP 상태인 이미지 조회
     */
    @Query("SELECT ui FROM UploadedImage ui " +
           "WHERE ui.status = 'TEMP' " +
           "AND ui.createdAt < :before " +
           "AND ui.deletedAt IS NULL")
    List<UploadedImage> findOldTempImages(@Param("before") LocalDateTime before);
}