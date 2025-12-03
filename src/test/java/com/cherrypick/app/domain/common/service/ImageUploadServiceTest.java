package com.cherrypick.app.domain.common.service;

import com.cherrypick.app.domain.common.entity.UploadedImage;
import com.cherrypick.app.domain.common.repository.UploadedImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ImageUploadService 테스트 (TDD)
 * 요구사항:
 * 1. 이미지 업로드 시 자동으로 썸네일 생성
 * 2. originalUrl과 thumbnailUrl 구분
 * 3. 응답: imageId, originalUrl, thumbnailUrl
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.jpa.show-sql=false",
    "logging.level.org.hibernate.SQL=WARN"
})
class ImageUploadServiceTest {

    @Autowired
    private ImageUploadService imageUploadService;

    @Autowired
    private UploadedImageRepository uploadedImageRepository;

    private MockMultipartFile testImageFile;
    private Long testUserId;

    @BeforeEach
    void setUp() throws IOException {
        // 테스트용 이미지 파일 생성 (800x600 JPG)
        testImageFile = createTestImageFile(800, 600, "test-image.jpg");
        testUserId = 1L;
    }

    @Test
    @DisplayName("이미지 업로드 시 썸네일이 자동으로 생성되어야 한다")
    void uploadImage_shouldCreateThumbnailAutomatically() throws IOException {
        // When
        UploadedImage uploadedImage = imageUploadService.uploadImage(testImageFile, "general", testUserId);

        // Then
        assertThat(uploadedImage).isNotNull();
        assertThat(uploadedImage.getId()).isNotNull();
        assertThat(uploadedImage.getOriginalUrl()).isNotNull();
        assertThat(uploadedImage.getThumbnailUrl()).isNotNull();
        assertThat(uploadedImage.getOriginalUrl()).isNotEqualTo(uploadedImage.getThumbnailUrl());
    }

    @Test
    @DisplayName("업로드된 이미지는 originalUrl과 thumbnailUrl을 명확히 구분해야 한다")
    void uploadedImage_shouldHaveDistinctUrls() throws IOException {
        // When
        UploadedImage uploadedImage = imageUploadService.uploadImage(testImageFile, "general", testUserId);

        // Then
        String originalUrl = uploadedImage.getOriginalUrl();
        String thumbnailUrl = uploadedImage.getThumbnailUrl();

        assertThat(originalUrl).contains("original");
        assertThat(thumbnailUrl).contains("thumbnail");
        assertThat(originalUrl).doesNotContain("thumbnail");
        assertThat(thumbnailUrl).doesNotContain("original");
    }

    @Test
    @DisplayName("업로드된 이미지는 DB에 정상적으로 저장되어야 한다")
    void uploadedImage_shouldBeSavedInDatabase() throws IOException {
        // When
        UploadedImage uploadedImage = imageUploadService.uploadImage(testImageFile, "general", testUserId);

        // Then
        UploadedImage foundImage = uploadedImageRepository.findById(uploadedImage.getId()).orElse(null);
        assertThat(foundImage).isNotNull();
        assertThat(foundImage.getOriginalFilename()).isEqualTo("test-image.jpg");
        assertThat(foundImage.getUploaderId()).isEqualTo(testUserId);
        assertThat(foundImage.getOriginalUrl()).isNotNull();
        assertThat(foundImage.getThumbnailUrl()).isNotNull();
    }

    @Test
    @DisplayName("빈 파일 업로드 시 예외가 발생해야 한다")
    void uploadImage_withEmptyFile_shouldThrowException() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        // When & Then
        assertThatThrownBy(() -> imageUploadService.uploadImage(emptyFile, "general", testUserId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("빈 파일은 업로드할 수 없습니다.");
    }

    @Test
    @DisplayName("5MB를 초과하는 파일 업로드 시 예외가 발생해야 한다")
    void uploadImage_withOversizedFile_shouldThrowException() {
        // Given
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
            "file", "large.jpg", "image/jpeg", largeContent
        );

        // When & Then
        assertThatThrownBy(() -> imageUploadService.uploadImage(largeFile, "general", testUserId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("파일 크기는 5MB를 넘을 수 없습니다.");
    }

    @Test
    @DisplayName("지원하지 않는 파일 형식 업로드 시 예외가 발생해야 한다")
    void uploadImage_withUnsupportedFormat_shouldThrowException() {
        // Given
        MockMultipartFile pdfFile = new MockMultipartFile(
            "file", "document.pdf", "application/pdf", "fake pdf content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> imageUploadService.uploadImage(pdfFile, "general", testUserId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, webp만 가능)");
    }

    @Test
    @DisplayName("썸네일은 원본보다 작은 크기로 생성되어야 한다")
    void thumbnail_shouldBeSmallerThanOriginal() throws IOException {
        // When
        UploadedImage uploadedImage = imageUploadService.uploadImage(testImageFile, "general", testUserId);

        // Then
        assertThat(uploadedImage.getThumbnailFileSize()).isNotNull();
        assertThat(uploadedImage.getThumbnailFileSize()).isLessThan(uploadedImage.getFileSize());
    }

    /**
     * 테스트용 이미지 파일 생성 헬퍼 메서드
     */
    private MockMultipartFile createTestImageFile(int width, int height, String filename) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();

        return new MockMultipartFile(
            "file",
            filename,
            "image/jpeg",
            imageBytes
        );
    }
}
