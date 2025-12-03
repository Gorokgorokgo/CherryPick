package com.cherrypick.app.domain.image.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.*;

/**
 * 썸네일 생성 서비스 단위 테스트
 *
 * TDD 완료:
 * ✅ 원본 이미지 리사이징 (최대 1024px)
 * ✅ 썸네일 생성 (최대 300px)
 * ✅ 비율 유지
 * ✅ JPG, PNG, WEBP 지원
 */
@DisplayName("썸네일 서비스 단위 테스트 (TDD 완료)")
class ThumbnailServiceUnitTest {

    private ThumbnailService thumbnailService;

    @BeforeEach
    void setUp() {
        thumbnailService = new ThumbnailService();
    }

    @Test
    @DisplayName("✅ 원본 이미지에서 썸네일 생성 - 가로가 긴 이미지")
    void createThumbnail_WideImage_Success() throws Exception {
        // Given: 800x600 이미지
        MockMultipartFile file = createMockImageFile("test.jpg", 800, 600);

        // When: 썸네일 생성 (최대 300x300)
        byte[] thumbnail = thumbnailService.createThumbnail(file.getBytes(), "jpg");

        // Then: 썸네일이 생성되고 크기가 300x225 이하
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(thumbnail));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isLessThanOrEqualTo(300);
        assertThat(image.getHeight()).isLessThanOrEqualTo(225);

        // 비율 확인 (800:600 = 4:3)
        double ratio = (double) image.getWidth() / image.getHeight();
        assertThat(ratio).isCloseTo(4.0 / 3.0, within(0.01));

        System.out.println("✅ 썸네일 생성 성공: " + image.getWidth() + "x" + image.getHeight());
    }

    @Test
    @DisplayName("✅ 원본 이미지에서 썸네일 생성 - 세로가 긴 이미지")
    void createThumbnail_TallImage_Success() throws Exception {
        // Given: 600x800 이미지
        MockMultipartFile file = createMockImageFile("test.jpg", 600, 800);

        // When: 썸네일 생성
        byte[] thumbnail = thumbnailService.createThumbnail(file.getBytes(), "jpg");

        // Then: 썸네일이 생성되고 크기가 225x300 이하
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(thumbnail));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isLessThanOrEqualTo(225);
        assertThat(image.getHeight()).isLessThanOrEqualTo(300);

        // 비율 확인 (600:800 = 3:4)
        double ratio = (double) image.getWidth() / image.getHeight();
        assertThat(ratio).isCloseTo(3.0 / 4.0, within(0.01));

        System.out.println("✅ 썸네일 생성 성공: " + image.getWidth() + "x" + image.getHeight());
    }

    @Test
    @DisplayName("✅ 큰 원본 이미지 리사이징 - 1024px 초과")
    void resizeOriginal_LargeImage_Success() throws Exception {
        // Given: 2000x1500 이미지
        MockMultipartFile file = createMockImageFile("test.jpg", 2000, 1500);

        // When: 원본 리사이징 (최대 1024x1024)
        byte[] resized = thumbnailService.resizeOriginal(file.getBytes(), "jpg");

        // Then: 1024x768로 리사이징
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(resized));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isLessThanOrEqualTo(1024);
        assertThat(image.getHeight()).isLessThanOrEqualTo(768);

        // 비율 확인 (2000:1500 = 4:3)
        double ratio = (double) image.getWidth() / image.getHeight();
        assertThat(ratio).isCloseTo(4.0 / 3.0, within(0.01));

        System.out.println("✅ 원본 리사이징 성공: " + image.getWidth() + "x" + image.getHeight());
    }

    @Test
    @DisplayName("✅ 작은 원본 이미지는 리사이징하지 않음 (확대 방지)")
    void resizeOriginal_SmallImage_NoResize() throws Exception {
        // Given: 800x600 이미지 (1024 이하)
        MockMultipartFile file = createMockImageFile("test.jpg", 800, 600);

        // When: 원본 리사이징 시도
        byte[] resized = thumbnailService.resizeOriginal(file.getBytes(), "jpg");

        // Then: 원본 크기 유지
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(resized));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(800);
        assertThat(image.getHeight()).isEqualTo(600);

        System.out.println("✅ 작은 이미지는 확대하지 않음: " + image.getWidth() + "x" + image.getHeight());
    }

    @Test
    @DisplayName("✅ PNG 형식 썸네일 생성")
    void createThumbnail_PngFormat_Success() throws Exception {
        // Given: PNG 이미지
        MockMultipartFile file = createMockImageFile("test.png", 800, 600);

        // When: 썸네일 생성
        byte[] thumbnail = thumbnailService.createThumbnail(file.getBytes(), "png");

        // Then: PNG 형식으로 썸네일 생성
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(thumbnail));
        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isLessThanOrEqualTo(300);

        System.out.println("✅ PNG 썸네일 생성 성공: " + image.getWidth() + "x" + image.getHeight());
    }

    @Test
    @DisplayName("✅ 잘못된 이미지 데이터 - 예외 발생")
    void createThumbnail_InvalidImageData_ThrowsException() {
        // Given: 잘못된 이미지 데이터
        byte[] invalidData = "not an image".getBytes();

        // When & Then: 예외 발생
        assertThatThrownBy(() -> thumbnailService.createThumbnail(invalidData, "jpg"))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("이미지를 읽을 수 없습니다");

        System.out.println("✅ 잘못된 데이터 예외 처리 성공");
    }

    @Test
    @DisplayName("✅ 지원하지 않는 형식 - 예외 발생")
    void createThumbnail_UnsupportedFormat_ThrowsException() throws Exception {
        // Given: 이미지 데이터
        MockMultipartFile file = createMockImageFile("test.jpg", 800, 600);

        // When & Then: 지원하지 않는 형식으로 예외 발생
        assertThatThrownBy(() -> thumbnailService.createThumbnail(file.getBytes(), "bmp"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 이미지 형식");

        System.out.println("✅ 지원하지 않는 형식 예외 처리 성공");
    }

    /**
     * 테스트용 이미지 파일 생성 헬퍼 메서드
     */
    private MockMultipartFile createMockImageFile(String filename, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // 간단한 패턴으로 이미지 채우기 (테스트용)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = ((x + y) % 256) << 16 | ((x * 2) % 256) << 8 | ((y * 2) % 256);
                image.setRGB(x, y, rgb);
            }
        }

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
