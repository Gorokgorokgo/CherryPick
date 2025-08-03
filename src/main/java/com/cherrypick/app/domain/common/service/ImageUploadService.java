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
    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    @Value("${aws.s3.region}")
    private String region;
    
    @Value("${aws.credentials.access-key}")
    private String accessKey;
    
    @Value("${aws.credentials.secret-key}")
    private String secretKey;
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    private S3Client s3Client;
    
    private S3Client getS3Client() {
        if (s3Client == null) {
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();
        }
        return s3Client;
    }
    
    public UploadedImage uploadImage(MultipartFile file, String folder, Long uploaderId) throws IOException {
        validateFile(file);
        
        String originalFilename = file.getOriginalFilename();
        String storedFilename = generateFileName(originalFilename);
        String fullPath = folder + "/" + storedFilename;
        String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fullPath);
        
        // 1단계: 데이터베이스에 이미지 정보 먼저 저장
        UploadedImage uploadedImage = saveImageRecord(originalFilename, storedFilename, 
                file.getSize(), file.getContentType(), folder, imageUrl, uploaderId);
        
        log.info("DB 저장 완료 - ID: {}, 원본: {}", uploadedImage.getId(), originalFilename);
        
        try {
            // 2단계: S3에 업로드
            uploadToS3(file, fullPath);
            log.info("S3 업로드 완료: {}", imageUrl);
            
            log.info("이미지 업로드 완료 - ID: {}, URL: {}", uploadedImage.getId(), imageUrl);
            return uploadedImage;
            
        } catch (Exception e) {
            // S3 업로드 실패 시 DB 레코드 삭제
            log.error("S3 업로드 실패, DB 레코드 삭제 - ID: {}", uploadedImage.getId(), e);
            uploadedImageRepository.delete(uploadedImage);
            throw new IOException("S3 업로드 실패: " + e.getMessage(), e);
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
        // 1. 데이터베이스에서 먼저 삭제 (트랜잭션 보장)
        UploadedImage image = uploadedImageRepository.findByS3Url(imageUrl)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다."));
        
        uploadedImageRepository.delete(image);
        log.info("DB에서 이미지 삭제 완료: {}", imageUrl);
        
        // 2. S3에서 삭제 (실패해도 DB는 이미 삭제됨)
        try {
            deleteFromS3(imageUrl);
            log.info("S3에서 이미지 삭제 완료: {}", imageUrl);
        } catch (Exception e) {
            // S3 삭제 실패 시 로깅만 하고 사용자에게는 성공으로 응답
            log.warn("S3 삭제 실패 (DB는 삭제됨): {}, 오류: {}", imageUrl, e.getMessage());
            // TODO: 추후 비동기 재시도 큐 또는 배치 정리 작업 추가
        }
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
        
        // 3. 데이터베이스에서 먼저 삭제 (트랜잭션 보장)
        uploadedImageRepository.delete(image);
        log.info("DB에서 이미지 삭제 완료 - ID: {}, URL: {}", imageId, image.getS3Url());
        
        // 4. S3에서 삭제 (실패해도 DB는 이미 삭제됨)
        try {
            deleteFromS3(image.getS3Url());
            log.info("S3에서 이미지 삭제 완료 - ID: {}, URL: {}", imageId, image.getS3Url());
        } catch (Exception e) {
            // S3 삭제 실패 시 로깅만 하고 사용자에게는 성공으로 응답
            log.warn("S3 삭제 실패 (DB는 삭제됨) - ID: {}, URL: {}, 오류: {}", imageId, image.getS3Url(), e.getMessage());
            // TODO: 추후 비동기 재시도 큐 또는 배치 정리 작업 추가
        }
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
        
        // 2. 데이터베이스에서 먼저 삭제 (트랜잭션 보장)
        uploadedImageRepository.delete(image);
        log.info("DB에서 이미지 삭제 완료 (관리자) - ID: {}, URL: {}", imageId, image.getS3Url());
        
        // 3. S3에서 삭제 (실패해도 DB는 이미 삭제됨)
        try {
            deleteFromS3(image.getS3Url());
            log.info("S3에서 이미지 삭제 완료 (관리자) - ID: {}, URL: {}", imageId, image.getS3Url());
        } catch (Exception e) {
            // S3 삭제 실패 시 로깅만 하고 사용자에게는 성공으로 응답
            log.warn("S3 삭제 실패 (DB는 삭제됨, 관리자) - ID: {}, URL: {}, 오류: {}", imageId, image.getS3Url(), e.getMessage());
            // TODO: 추후 비동기 재시도 큐 또는 배치 정리 작업 추가
        }
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
    
    private void uploadToS3(MultipartFile file, String path) throws IOException {
        try {
            log.info("S3 업로드 시작: bucket={}, path={}", bucketName, path);
            
            // S3에 파일 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(path)
                    .contentType(file.getContentType())
                    .build();
            
            getS3Client().putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            log.info("S3 업로드 완료: path={}", path);
            
        } catch (Exception e) {
            log.error("S3 업로드 실패: path={}, error={}", path, e.getMessage(), e);
            throw new IOException("S3 업로드에 실패했습니다: " + e.getMessage());
        }
    }
    
    private void deleteFromS3(String imageUrl) {
        try {
            // URL에서 S3 키 추출
            String key = imageUrl.substring(imageUrl.indexOf(bucketName) + bucketName.length() + 1);
            key = key.substring(key.indexOf('/') + 1);
            
            log.info("S3 삭제 시작: bucket={}, key={}", bucketName, key);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
                    
            getS3Client().deleteObject(deleteObjectRequest);
            
            log.info("S3 삭제 완료: {}", imageUrl);
            
        } catch (NoSuchKeyException e) {
            // 이미 삭제된 파일 - 멱등성 보장으로 정상 처리
            log.info("파일이 이미 삭제됨 (멱등성 보장): {}", imageUrl);
            
        } catch (Exception e) {
            log.error("S3 삭제 실패: url={}, error={}", imageUrl, e.getMessage(), e);
            throw e; // 다른 예외는 상위로 전달하여 재시도 로직에서 처리
        }
    }
}