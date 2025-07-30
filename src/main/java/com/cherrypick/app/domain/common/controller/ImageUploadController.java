package com.cherrypick.app.domain.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "이미지 업로드", description = "이미지 업로드 및 삭제 API")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageUploadController {
    
    private final ImageUploadService imageUploadService;
    
    @Operation(summary = "단일 이미지 업로드", description = "단일 이미지 파일을 Supabase Storage에 업로드합니다. JPG, PNG 형식만 지원하며, 최대 10MB까지 업로드 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이미지 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식 또는 파일 오류")
    })
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadSingleImage(
            @Parameter(description = "업로드할 이미지 파일") @RequestParam("file") MultipartFile file,
            @Parameter(description = "저장할 폴더명 (기본: general)") @RequestParam(value = "folder", defaultValue = "general") String folder) {
        
        try {
            String imageUrl = imageUploadService.uploadImage(file, folder);
            
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("message", "이미지 업로드가 완료되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @Operation(summary = "다중 이미지 업로드", description = "여러 이미지 파일을 동시에 Supabase Storage에 업로드합니다. 최대 10개 파일까지 업로드 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이미지 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식 또는 파일 오류")
    })
    @PostMapping("/upload/multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleImages(
            @Parameter(description = "업로드할 이미지 파일 목록") @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "저장할 폴더명 (기본: general)") @RequestParam(value = "folder", defaultValue = "general") String folder) {
        
        try {
            List<String> imageUrls = imageUploadService.uploadMultipleImages(files, folder);
            
            Map<String, Object> response = new HashMap<>();
            response.put("imageUrls", imageUrls);
            response.put("count", imageUrls.size());
            response.put("message", "이미지 업로드가 완료되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @Operation(summary = "이미지 삭제", description = "업로드된 이미지를 Supabase Storage에서 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이미지 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 이미지 URL 또는 삭제 오류")
    })
    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteImage(
            @Parameter(description = "삭제할 이미지 URL") @RequestParam("imageUrl") String imageUrl) {
        
        try {
            imageUploadService.deleteImage(imageUrl);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "이미지가 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}