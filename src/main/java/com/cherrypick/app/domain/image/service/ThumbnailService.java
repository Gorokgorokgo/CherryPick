package com.cherrypick.app.domain.image.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * 썸네일 생성 서비스
 *
 * 요구사항:
 * 1. 원본 이미지 → 썸네일 (최대 300x300, 비율 유지)
 * 2. 원본 이미지 → 리사이징 (최대 1024x1024, 비율 유지)
 * 3. 지원 형식: JPG, PNG, WEBP
 */
@Slf4j
@Service
public class ThumbnailService {

    private static final int THUMBNAIL_MAX_SIZE = 300;
    private static final int ORIGINAL_MAX_SIZE = 1024;
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final float JPEG_QUALITY = 0.85f;

    /**
     * 썸네일 생성 (최대 300x300, 비율 유지)
     *
     * @param imageData 원본 이미지 바이트 배열
     * @param format 이미지 형식 (jpg, png, webp)
     * @return 썸네일 이미지 바이트 배열
     * @throws IOException 이미지 처리 실패 시
     */
    public byte[] createThumbnail(byte[] imageData, String format) throws IOException {
        validateFormat(format);

        BufferedImage originalImage = readImage(imageData);
        BufferedImage thumbnail = resizeImage(originalImage, THUMBNAIL_MAX_SIZE);

        return writeImage(thumbnail, format);
    }

    /**
     * 원본 이미지 리사이징 (최대 1024x1024, 비율 유지)
     *
     * @param imageData 원본 이미지 바이트 배열
     * @param format 이미지 형식
     * @return 리사이징된 이미지 바이트 배열
     * @throws IOException 이미지 처리 실패 시
     */
    public byte[] resizeOriginal(byte[] imageData, String format) throws IOException {
        validateFormat(format);

        BufferedImage originalImage = readImage(imageData);
        BufferedImage resized = resizeImage(originalImage, ORIGINAL_MAX_SIZE);

        return writeImage(resized, format);
    }

    /**
     * 이미지 형식 유효성 검증
     */
    private void validateFormat(String format) {
        String normalizedFormat = format.toLowerCase();
        if (!SUPPORTED_FORMATS.contains(normalizedFormat)) {
            throw new IllegalArgumentException(
                    "지원하지 않는 이미지 형식입니다: " + format + " (지원 형식: jpg, jpeg, png, webp)"
            );
        }
    }

    /**
     * 바이트 배열에서 BufferedImage 읽기
     */
    private BufferedImage readImage(byte[] imageData) throws IOException {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) {
                throw new IOException("이미지를 읽을 수 없습니다. 올바른 이미지 형식인지 확인하세요.");
            }
            return image;
        } catch (IOException e) {
            log.error("이미지 읽기 실패", e);
            throw new IOException("이미지를 읽을 수 없습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 이미지 리사이징 (비율 유지, 최대 크기 제한)
     *
     * @param originalImage 원본 이미지
     * @param maxSize 최대 크기 (가로 또는 세로 중 큰 값)
     * @return 리사이징된 이미지
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int maxSize) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 원본이 최대 크기보다 작으면 리사이징하지 않음 (확대 방지)
        if (originalWidth <= maxSize && originalHeight <= maxSize) {
            return originalImage;
        }

        // 비율 유지하면서 새로운 크기 계산
        double scaleFactor;
        if (originalWidth > originalHeight) {
            scaleFactor = (double) maxSize / originalWidth;
        } else {
            scaleFactor = (double) maxSize / originalHeight;
        }

        int newWidth = (int) (originalWidth * scaleFactor);
        int newHeight = (int) (originalHeight * scaleFactor);

        // 고품질 리사이징 (SCALE_SMOOTH)
        Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

        // BufferedImage로 변환
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();

        // 고품질 렌더링 힌트 설정
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        return resizedImage;
    }

    /**
     * BufferedImage를 바이트 배열로 변환 (압축 품질 설정)
     */
    private byte[] writeImage(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String outputFormat = format.toLowerCase();
        if (outputFormat.equals("jpg") || outputFormat.equals("jpeg")) {
            // JPEG 압축 품질 설정
            writeJpegWithQuality(image, baos);
        } else {
            // PNG, WEBP는 기본 설정 사용
            ImageIO.write(image, outputFormat, baos);
        }

        return baos.toByteArray();
    }

    /**
     * JPEG 이미지를 압축 품질을 지정하여 저장
     */
    private void writeJpegWithQuality(BufferedImage image, ByteArrayOutputStream baos) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("JPEG writer를 찾을 수 없습니다.");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();

        // 압축 품질 설정 (0.85 = 85% 품질)
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionQuality(JPEG_QUALITY);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }
}
