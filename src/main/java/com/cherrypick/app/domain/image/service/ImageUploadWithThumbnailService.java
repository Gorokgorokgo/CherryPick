package com.cherrypick.app.domain.image.service;

import com.cherrypick.app.domain.common.entity.UploadedImage;
import com.cherrypick.app.domain.common.repository.UploadedImageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 썸네일 생성 기능이 포함된 이미지 업로드 서비스
 *
 * 기존 ImageUploadService를 확장하여 다음 기능 추가:
 * 1. 원본 이미지 리사이징 (최대 1024px)
 * 2. 썸네일 생성 (최대 300px)
 * 3. 원본 + 썸네일 모두 NCP Object Storage에 업로드
 */
@Slf4j
@Service
@Transactional
public class ImageUploadWithThumbnailService {

    private final UploadedImageRepository uploadedImageRepository;
    private final ThumbnailService thumbnailService;

    @Value("${ncp.object-storage.bucket}")
    private String bucketName;

    @Value("${ncp.object-storage.region}")
    private String region;

    @Value("${ncp.object-storage.endpoint}")
    private String endpoint;

    @Value("${ncp.credentials.access-key}")
    private String accessKey;

    @Value("${ncp.credentials.secret-key}")
    private String secretKey;

    private S3Client s3Client;

    public ImageUploadWithThumbnailService(
            UploadedImageRepository uploadedImageRepository,
            ThumbnailService thumbnailService) {
        this.uploadedImageRepository = uploadedImageRepository;
        this.thumbnailService = thumbnailService;
    }

    private S3Client getS3Client() {
        if (s3Client == null) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .endpointOverride(java.net.URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
        }
        return s3Client;
    }

    /**
     * 이미지 업로드 (썸네일 포함)
     *
     * @param file 업로드할 이미지 파일
     * @param folder 저장할 폴더
     * @param uploaderId 업로더 ID
     * @return 업로드된 이미지 정보 (원본 URL + 썸네일 URL)
     * @throws IOException 이미지 처리 또는 업로드 실패 시
     */
    public UploadedImage uploadImageWithThumbnail(MultipartFile file, String folder, Long uploaderId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String storedFilename = generateFileName(originalFilename);
        String extension = getFileExtension(originalFilename);

        // 1. 원본 이미지 리사이징 (최대 1024px)
        byte[] resizedOriginal = thumbnailService.resizeOriginal(file.getBytes(), extension);

        // 2. 썸네일 생성 (최대 300px)
        byte[] thumbnail = thumbnailService.createThumbnail(file.getBytes(), extension);

        // 3. 파일 경로 및 URL 생성
        String originalPath = folder + "/" + storedFilename;
        String thumbnailPath = folder + "/thumb_" + storedFilename;

        String originalUrl = String.format("https://%s.kr.object.ncloudstorage.com/%s", bucketName, originalPath);
        String thumbnailUrl = String.format("https://%s.kr.object.ncloudstorage.com/%s", bucketName, thumbnailPath);

        // 4. DB에 이미지 정보 저장
        UploadedImage uploadedImage = saveImageRecord(
                originalFilename, storedFilename,
                resizedOriginal.length, file.getContentType(), folder,
                originalUrl, thumbnailUrl, uploaderId
        );

        try {
            // 5. 원본 이미지 업로드
            uploadToNCP(resizedOriginal, originalPath, file.getContentType());

            // 6. 썸네일 업로드
            uploadToNCP(thumbnail, thumbnailPath, file.getContentType());

            log.info("✅ 이미지 업로드 완료 - {}", originalFilename);

            return uploadedImage;

        } catch (Exception e) {
            // 업로드 실패 시 DB 레코드 삭제
            log.error("❌ 이미지 업로드 실패: {}", originalFilename, e);
            uploadedImageRepository.delete(uploadedImage);
            throw new IOException("이미지 업로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 다중 이미지 업로드 (썸네일 포함)
     */
    public List<UploadedImage> uploadMultipleImagesWithThumbnail(List<MultipartFile> files, String folder, Long uploaderId) throws IOException {
        if (files.size() > 10) {
            throw new IllegalArgumentException("최대 10개의 이미지만 업로드 가능합니다.");
        }

        return files.stream()
                .map(file -> {
                    try {
                        return uploadImageWithThumbnail(file, folder, uploaderId);
                    } catch (IOException e) {
                        throw new RuntimeException("이미지 업로드에 실패했습니다: " + e.getMessage());
                    }
                })
                .toList();
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    protected UploadedImage saveImageRecord(String originalFilename, String storedFilename,
                                          long fileSize, String contentType, String folder,
                                          String imageUrl, String thumbnailUrl, Long uploaderId) {
        UploadedImage uploadedImage = UploadedImage.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .fileSize(fileSize)
                .contentType(contentType)
                .folderPath(folder)
                .s3Url(imageUrl)
                .thumbnailUrl(thumbnailUrl)
                .uploaderId(uploaderId)
                .build();

        return uploadedImageRepository.save(uploadedImage);
    }

    private void uploadToNCP(byte[] imageData, String path, String contentType) throws IOException {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(path)
                    .contentType(contentType)
                    .acl("public-read")
                    .build();

            getS3Client().putObject(putObjectRequest, RequestBody.fromBytes(imageData));

        } catch (Exception e) {
            log.error("❌ NCP 업로드 실패: path={}", path, e);
            throw new IOException("NCP 업로드에 실패했습니다: " + e.getMessage());
        }
    }

    private String generateFileName(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + "." + extension;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}
