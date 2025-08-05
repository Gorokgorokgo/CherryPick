package com.cherrypick.app.domain.common.controller;

import com.cherrypick.app.domain.common.dto.ImageUploadResponse;
import com.cherrypick.app.domain.common.entity.UploadedImage;
import com.cherrypick.app.domain.common.service.ImageUploadService;
import com.cherrypick.app.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "5단계 - 이미지 업로드", description = "경매 상품 이미지 업로드 | AWS S3 연동, 최대 5MB")
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageUploadController {
    
    private final ImageUploadService imageUploadService;
    private final UserService userService;
    
    @Operation(summary = "단일 이미지 업로드", description = "단일 이미지 파일을 AWS S3에 업로드합니다. JPG, PNG, WEBP 형식만 지원하며, 최대 5MB까지 업로드 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이미지 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식 또는 파일 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ImageUploadResponse> uploadSingleImage(
            @Parameter(description = "업로드할 이미지 파일") @RequestParam("file") MultipartFile file,
            @Parameter(description = "저장할 폴더명 (기본: general)") @RequestParam(value = "folder", defaultValue = "general") String folder,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            Long userId = userService.getUserIdByEmail(userDetails.getUsername());
            UploadedImage uploadedImage = imageUploadService.uploadImage(file, folder, userId);
            ImageUploadResponse response = ImageUploadResponse.from(uploadedImage);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(summary = "다중 이미지 업로드", description = "여러 이미지 파일을 동시에 AWS S3에 업로드합니다. 최대 10개 파일까지 업로드 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이미지 업로드 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 파일 형식 또는 파일 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping(value = "/upload/multiple", consumes = "multipart/form-data")
    public ResponseEntity<List<ImageUploadResponse>> uploadMultipleImages(
            @Parameter(description = "업로드할 이미지 파일 목록") @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "저장할 폴더명 (기본: general)") @RequestParam(value = "folder", defaultValue = "general") String folder,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            Long userId = userService.getUserIdByEmail(userDetails.getUsername());
            List<UploadedImage> uploadedImages = imageUploadService.uploadMultipleImages(files, folder, userId);
            List<ImageUploadResponse> responses = uploadedImages.stream()
                    .map(ImageUploadResponse::from)
                    .toList();
            
            return ResponseEntity.ok(responses);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(summary = "이미지 삭제 (URL)", description = "업로드된 이미지를 AWS S3에서 삭제합니다.")
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
    
    @Operation(summary = "이미지 삭제 (ID)", description = "이미지 ID로 업로드된 이미지를 AWS S3에서 삭제합니다. 본인이 업로드한 이미지만 삭제 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이미지 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
            @ApiResponse(responseCode = "404", description = "이미지를 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteImageById(
            @Parameter(description = "삭제할 이미지 ID") @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            Long currentUserId = userService.getUserIdByEmail(userDetails.getUsername());
            imageUploadService.deleteImageById(id, currentUserId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "이미지가 삭제되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            
            // 에러 메시지에 따라 HTTP 상태 코드 결정
            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("권한")) {
                return ResponseEntity.status(403).body(errorResponse);
            } else {
                return ResponseEntity.badRequest().body(errorResponse);
            }
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "이미지 삭제 중 오류가 발생했습니다.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @Operation(summary = "이미지 목록 조회", description = "업로드된 이미지 목록을 조회합니다. 폴더별 필터링 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @GetMapping
    public ResponseEntity<List<UploadedImage>> getImages(
            @Parameter(description = "폴더명 (선택사항)") @RequestParam(required = false) String folder) {
        
        List<UploadedImage> images;
        if (folder != null && !folder.trim().isEmpty()) {
            images = imageUploadService.getImagesByFolder(folder);
        } else {
            images = imageUploadService.getAllImages();
        }
        
        return ResponseEntity.ok(images);
    }
    
    @Operation(summary = "이미지 정보 조회 (ID)", description = "이미지 ID로 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "이미지를 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UploadedImage> getImageById(
            @Parameter(description = "이미지 ID") @PathVariable Long id) {
        
        try {
            UploadedImage image = imageUploadService.getImageById(id);
            return ResponseEntity.ok(image);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @Operation(summary = "이미지 정보 조회 (URL)", description = "이미지 URL로 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "이미지를 찾을 수 없음")
    })
    @GetMapping("/info")
    public ResponseEntity<UploadedImage> getImageInfo(
            @Parameter(description = "조회할 이미지 URL") @RequestParam("imageUrl") String imageUrl) {
        
        try {
            UploadedImage image = imageUploadService.getImageInfo(imageUrl);
            return ResponseEntity.ok(image);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}