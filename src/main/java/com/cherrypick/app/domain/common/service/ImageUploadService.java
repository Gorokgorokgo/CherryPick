package com.cherrypick.app.domain.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageUploadService {
    
    @Value("${supabase.storage.url}")
    private String supabaseStorageUrl;
    
    @Value("${supabase.storage.bucket}")
    private String bucketName;
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        validateFile(file);
        
        String fileName = generateFileName(file.getOriginalFilename());
        String fullPath = folder + "/" + fileName;
        
        // TODO: Supabase Storage API 연동
        String imageUrl = uploadToSupabase(file, fullPath);
        
        return imageUrl;
    }
    
    public List<String> uploadMultipleImages(List<MultipartFile> files, String folder) throws IOException {
        if (files.size() > 10) {
            throw new IllegalArgumentException("최대 10개의 이미지만 업로드 가능합니다.");
        }
        
        return files.stream()
                .map(file -> {
                    try {
                        return uploadImage(file, folder);
                    } catch (IOException e) {
                        throw new RuntimeException("이미지 업로드에 실패했습니다: " + e.getMessage());
                    }
                })
                .toList();
    }
    
    public void deleteImage(String imageUrl) {
        // TODO: Supabase Storage에서 이미지 삭제
        deleteFromSupabase(imageUrl);
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
    
    private String uploadToSupabase(MultipartFile file, String path) throws IOException {
        // TODO: 실제 Supabase Storage API 연동
        // 현재는 임시 URL 반환
        String tempUrl = String.format("%s/%s/%s", supabaseStorageUrl, bucketName, path);
        
        // 파일 업로드 시뮬레이션
        try {
            Thread.sleep(500); // 업로드 시뮬레이션
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("업로드 중 오류가 발생했습니다.");
        }
        
        return tempUrl;
    }
    
    private void deleteFromSupabase(String imageUrl) {
        // TODO: 실제 Supabase Storage API 연동
        // 현재는 로그만 출력
        System.out.println("이미지 삭제: " + imageUrl);
    }
}