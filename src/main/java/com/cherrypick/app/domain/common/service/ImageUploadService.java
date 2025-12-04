package com.cherrypick.app.domain.common.service;

import com.cherrypick.app.domain.common.entity.UploadedImage;
import com.cherrypick.app.domain.common.repository.UploadedImageRepository;
import lombok.RequiredArgsConstructor;
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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ImageUploadService {
    
    private final UploadedImageRepository uploadedImageRepository;
    // AWS 설정 (기존 설정 보관)
    // @Value("${aws.s3.bucket}")
    // private String bucketName;
    // @Value("${aws.s3.region}")
    // private String region;
    // @Value("${aws.credentials.access-key}")
    // private String accessKey;
    // @Value("${aws.credentials.secret-key}")
    // private String secretKey;
    
    // NCP Object Storage 설정
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
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    private S3Client s3Client;
    
    private S3Client getS3Client() {
        if (s3Client == null) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .endpointOverride(java.net.URI.create(endpoint)) // NCP Object Storage 엔드포인트
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
        }
        return s3Client;
    }
    
    public UploadedImage uploadImage(MultipartFile file, String folder, Long uploaderId) throws IOException {
        validateFile(file);
        
        String originalFilename = file.getOriginalFilename();
        String storedFilename = generateFileName(originalFilename);
        String fullPath = folder + "/" + storedFilename;
        // NCP Object Storage URL 형식
        String imageUrl = String.format("https://%s.kr.object.ncloudstorage.com/%s", bucketName, fullPath);
        
        // 1단계: 데이터베이스에 이미지 정보 먼저 저장
        UploadedImage uploadedImage = saveImageRecord(originalFilename, storedFilename, 
                file.getSize(), file.getContentType(), folder, imageUrl, uploaderId);
        
        log.info("DB 저장 완료 - ID: {}, 원본: {}", uploadedImage.getId(), originalFilename);
        
        try {
            // 2단계: NCP Object Storage에 업로드
            uploadToNCP(file, fullPath);
            log.info("NCP 업로드 완료: {}", imageUrl);
            
            log.info("이미지 업로드 완료 - ID: {}, URL: {}", uploadedImage.getId(), imageUrl);
            return uploadedImage;
            
        } catch (Exception e) {
            // NCP 업로드 실패 시 DB 레코드 삭제
            log.error("NCP 업로드 실패, DB 레코드 삭제 - ID: {}", uploadedImage.getId(), e);
            uploadedImageRepository.delete(uploadedImage);
            throw new IOException("NCP 업로드 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 이미지 정보를 데이터베이스에 저장 (별도 트랜잭션)
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public UploadedImage saveImageRecord(String originalFilename, String storedFilename, 
                                       long fileSize, String contentType, String folder, String imageUrl, Long uploaderId) {
        UploadedImage uploadedImage = UploadedImage.builder()
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .fileSize(fileSize)
                .contentType(contentType)
                .folderPath(folder)
                .s3Url(imageUrl)
                .uploaderId(uploaderId) // 실제 업로더 ID 설정
                .build();
        
        return uploadedImageRepository.save(uploadedImage);
    }
    
    public List<UploadedImage> uploadMultipleImages(List<MultipartFile> files, String folder, Long uploaderId) throws IOException {
        if (files.size() > 10) {
            throw new IllegalArgumentException("최대 10개의 이미지만 업로드 가능합니다.");
        }
        
        return files.stream()
                .map(file -> {
                    try {
                        return uploadImage(file, folder, uploaderId);
                    } catch (IOException e) {
                        throw new RuntimeException("이미지 업로드에 실패했습니다: " + e.getMessage());
                    }
                })
                .toList();
    }
    
    public void deleteImage(String imageUrl) {
        // 1. 데이터베이스에서 Soft Delete (메타데이터 보존)
        UploadedImage image = uploadedImageRepository.findByS3Url(imageUrl)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다."));

        // Soft Delete 수행 (업로더 ID 사용, 없으면 null)
        image.softDelete(image.getUploaderId());
        uploadedImageRepository.save(image);
        log.info("이미지 메타데이터 Soft Delete 완료: {}", imageUrl);

        // 2. NCP 파일은 배치 작업으로 정리 예정
        // TODO: 삭제된 이미지 배치 정리 작업 추가 (예: 30일 후 완전 삭제)
        log.info("NCP 파일은 배치 작업에서 정리됩니다: {}", imageUrl);
    }
    
    /**
     * ID로 이미지 삭제 (권한 체크 포함)
     *
     * @param imageId 삭제할 이미지 ID
     * @param userId 요청 사용자 ID (권한 체크용)
     */
    public void deleteImageById(Long imageId, Long userId) {
        // 1. 이미지 조회
        UploadedImage image = uploadedImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다."));

        // 2. 권한 체크 (업로더 본인만 삭제 가능)
        if (image.getUploaderId() != null && !image.getUploaderId().equals(userId)) {
            throw new IllegalArgumentException("본인이 업로드한 이미지만 삭제할 수 있습니다.");
        }

        // 3. 데이터베이스에서 Soft Delete (메타데이터 보존)
        image.softDelete(userId);
        uploadedImageRepository.save(image);
        log.info("이미지 메타데이터 Soft Delete 완료 - ID: {}, URL: {}", imageId, image.getS3Url());

        // 4. NCP 파일은 배치 작업으로 정리 예정
        // TODO: 삭제된 이미지 배치 정리 작업 추가 (예: 30일 후 완전 삭제)
        log.info("NCP 파일은 배치 작업에서 정리됩니다 - ID: {}, URL: {}", imageId, image.getS3Url());
    }
    
    /**
     * ID로 이미지 삭제 (권한 체크 없음 - 관리자용)
     *
     * @param imageId 삭제할 이미지 ID
     */
    public void deleteImageById(Long imageId) {
        // 1. 이미지 조회
        UploadedImage image = uploadedImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다."));

        // 2. 데이터베이스에서 Soft Delete (메타데이터 보존)
        image.softDelete(null); // 관리자 삭제이므로 deletedBy는 null
        uploadedImageRepository.save(image);
        log.info("이미지 메타데이터 Soft Delete 완료 (관리자) - ID: {}, URL: {}", imageId, image.getS3Url());

        // 3. NCP 파일은 배치 작업으로 정리 예정
        // TODO: 삭제된 이미지 배치 정리 작업 추가 (예: 30일 후 완전 삭제)
        log.info("NCP 파일은 배치 작업에서 정리됩니다 (관리자) - ID: {}, URL: {}", imageId, image.getS3Url());
    }
    
    /**
     * 폴더별 이미지 목록 조회
     */
    @Transactional(readOnly = true)
    public List<UploadedImage> getImagesByFolder(String folder) {
        return uploadedImageRepository.findByFolderPathOrderByCreatedAtDesc(folder);
    }
    
    /**
     * 모든 이미지 목록 조회
     */
    @Transactional(readOnly = true)
    public List<UploadedImage> getAllImages() {
        return uploadedImageRepository.findAll();
    }
    
    /**
     * ID로 이미지 정보 조회
     */
    @Transactional(readOnly = true)
    public UploadedImage getImageById(Long id) {
        return uploadedImageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다: " + id));
    }
    
    /**
     * URL로 이미지 정보 조회
     */
    @Transactional(readOnly = true)
    public UploadedImage getImageInfo(String imageUrl) {
        return uploadedImageRepository.findByS3Url(imageUrl)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다: " + imageUrl));
    }

    /**
     * URL 목록으로 이미지 정보 일괄 조회
     */
    @Transactional(readOnly = true)
    public List<UploadedImage> getImageInfos(List<String> imageUrls) {
        return uploadedImageRepository.findByS3UrlIn(imageUrls);
    }

    /**
     * 이미지를 영구 저장 상태로 전환
     *
     * @param imageUrl 이미지 URL
     */
    public void markImageAsPermanent(String imageUrl) {
        UploadedImage image = uploadedImageRepository.findByS3Url(imageUrl)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다: " + imageUrl));

        image.markAsPermanent();
        uploadedImageRepository.save(image);
    }

    /**
     * 임시 파일 완전 삭제 (NCP + DB)
     * 배치 정리 작업에서 사용
     *
     * @param imageId 삭제할 이미지 ID
     */
    public void deleteTempImageCompletely(Long imageId) {
        UploadedImage image = uploadedImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다: " + imageId));

        try {
            // 1. 원본 이미지 NCP 삭제
            deleteFromNCP(image.getS3Url());

            // 2. 썸네일 이미지 NCP 삭제 (있는 경우)
            if (image.getThumbnailUrl() != null && !image.getThumbnailUrl().isEmpty()) {
                deleteFromNCP(image.getThumbnailUrl());
            }

            // 3. DB 레코드 삭제
            uploadedImageRepository.delete(image);

            log.info("✅ 임시 이미지 완전 삭제 완료 - ID: {}, URL: {}", imageId, image.getS3Url());

        } catch (Exception e) {
            log.error("❌ 임시 이미지 삭제 실패 - ID: {}, URL: {}", imageId, image.getS3Url(), e);
            throw new RuntimeException("이미지 삭제 실패: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기는 5MB를 넘을 수 없습니다.");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }
        
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, webp만 가능)");
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
    
    /**
     * 외부 URL에서 이미지를 다운로드하여 NCP에 업로드
     * 소셜 로그인 프로필 이미지 등에 사용
     *
     * @param externalUrl 외부 이미지 URL (카카오, 구글 등)
     * @param folder 저장할 폴더
     * @param uploaderId 업로더 ID (소셜 로그인의 경우 userId)
     * @return 업로드된 NCP URL
     */
    public String uploadFromUrl(String externalUrl, String folder, Long uploaderId) {
        if (externalUrl == null || externalUrl.isEmpty()) {
            return null;
        }
        
        try {
            log.info("외부 URL에서 이미지 다운로드 시작: {}", externalUrl);
            
            // 1. 외부 URL에서 이미지 다운로드
            java.net.URL url = new java.net.URL(externalUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            if (connection.getResponseCode() != 200) {
                log.warn("외부 이미지 다운로드 실패 (HTTP {}): {}", connection.getResponseCode(), externalUrl);
                return externalUrl; // 실패 시 원본 URL 반환
            }
            
            String contentType = connection.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("유효하지 않은 이미지 타입: {}", contentType);
                return externalUrl;
            }
            
            // 확장자 결정
            String extension = "jpg";
            if (contentType.contains("png")) extension = "png";
            else if (contentType.contains("webp")) extension = "webp";
            else if (contentType.contains("gif")) extension = "gif";
            
            // 2. 이미지 바이트 읽기
            byte[] imageBytes;
            try (java.io.InputStream inputStream = connection.getInputStream()) {
                imageBytes = inputStream.readAllBytes();
            }
            
            // 파일 크기 체크 (5MB 이상이면 원본 URL 사용)
            if (imageBytes.length > MAX_FILE_SIZE) {
                log.warn("이미지 크기가 너무 큼 ({} bytes), 원본 URL 사용", imageBytes.length);
                return externalUrl;
            }
            
            // 3. NCP에 업로드
            String storedFilename = UUID.randomUUID().toString() + "." + extension;
            String fullPath = folder + "/" + storedFilename;
            String imageUrl = String.format("https://%s.kr.object.ncloudstorage.com/%s", bucketName, fullPath);
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fullPath)
                    .contentType(contentType)
                    .acl("public-read")
                    .build();
            
            getS3Client().putObject(putObjectRequest, RequestBody.fromBytes(imageBytes));
            
            // 4. DB에 이미지 정보 저장
            UploadedImage uploadedImage = UploadedImage.builder()
                    .originalFilename("social_profile." + extension)
                    .storedFilename(storedFilename)
                    .fileSize((long) imageBytes.length)
                    .contentType(contentType)
                    .folderPath(folder)
                    .s3Url(imageUrl)
                    .uploaderId(uploaderId)
                    .build();
            uploadedImage.markAsPermanent(); // 프로필 이미지는 영구 저장
            uploadedImageRepository.save(uploadedImage);
            
            log.info("외부 이미지 NCP 업로드 완료: {} -> {}", externalUrl, imageUrl);
            return imageUrl;
            
        } catch (Exception e) {
            log.error("외부 이미지 업로드 실패, 원본 URL 사용: {}", externalUrl, e);
            return externalUrl; // 실패 시 원본 URL 반환
        }
    }
    
    private void uploadToNCP(MultipartFile file, String path) throws IOException {
        try {
            log.info("NCP 업로드 시작: bucket={}, path={}", bucketName, path);

            // NCP에 파일 업로드 (Public Read ACL 설정)
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(path)
                    .contentType(file.getContentType())
                    .acl("public-read") // 공개 읽기 권한 설정
                    .build();

            getS3Client().putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("NCP 업로드 완료: path={}", path);

        } catch (Exception e) {
            log.error("NCP 업로드 실패: path={}, error={}", path, e.getMessage(), e);
            throw new IOException("NCP 업로드에 실패했습니다: " + e.getMessage());
        }
    }
    
    private void deleteFromNCP(String imageUrl) {
        try {
            // URL에서 Object Storage 키 추출 (NCP 형식)
            String key = imageUrl.substring(imageUrl.indexOf(bucketName) + bucketName.length() + 1);
            
            log.info("NCP 삭제 시작: bucket={}, key={}", bucketName, key);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
                    
            getS3Client().deleteObject(deleteObjectRequest);
            
            log.info("NCP 삭제 완료: {}", imageUrl);
            
        } catch (NoSuchKeyException e) {
            // 이미 삭제된 파일 - 멱등성 보장으로 정상 처리
            log.info("파일이 이미 삭제됨 (멱등성 보장): {}", imageUrl);
            
        } catch (Exception e) {
            log.error("NCP 삭제 실패: url={}, error={}", imageUrl, e.getMessage(), e);
            throw e; // 다른 예외는 상위로 전달하여 재시도 로직에서 처리
        }
    }
}