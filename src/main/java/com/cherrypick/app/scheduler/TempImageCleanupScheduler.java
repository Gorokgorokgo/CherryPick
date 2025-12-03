package com.cherrypick.app.scheduler;

import com.cherrypick.app.domain.common.entity.UploadedImage;
import com.cherrypick.app.domain.common.repository.UploadedImageRepository;
import com.cherrypick.app.domain.common.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 임시 이미지 정리 스케줄러
 *
 * 30분 이상 경과한 TEMP 상태의 이미지를 자동으로 삭제합니다.
 * - 사용자가 이미지 업로드 후 경매 등록을 취소한 경우
 * - 경매 등록 중 오류가 발생한 경우
 * - 업로드 후 30분 이내에 PERMANENT로 전환되지 않은 모든 이미지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TempImageCleanupScheduler {

    private final UploadedImageRepository uploadedImageRepository;
    private final ImageUploadService imageUploadService;

    /**
     * 임시 이미지 정리 작업
     * 매 10분마다 실행하여 30분 이상 경과한 TEMP 이미지를 삭제
     */
    @Scheduled(cron = "0 */10 * * * *") // 매 10분마다 실행 (0분, 10분, 20분, 30분, 40분, 50분)
    public void cleanupOldTempImages() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
            List<UploadedImage> oldTempImages = uploadedImageRepository.findOldTempImages(threshold);

            if (oldTempImages.isEmpty()) {
                log.debug("정리할 임시 이미지가 없습니다.");
                return;
            }

            log.info("🗑️ 임시 이미지 정리 시작 - 대상: {}개", oldTempImages.size());

            int successCount = 0;
            int failCount = 0;

            for (UploadedImage image : oldTempImages) {
                try {
                    imageUploadService.deleteTempImageCompletely(image.getId());
                    successCount++;
                } catch (Exception e) {
                    log.error("❌ 이미지 삭제 실패 - ID: {}, URL: {}", image.getId(), image.getS3Url(), e);
                    failCount++;
                }
            }

            log.info("✅ 임시 이미지 정리 완료 - 성공: {}개, 실패: {}개", successCount, failCount);

        } catch (Exception e) {
            log.error("❌ 임시 이미지 정리 작업 실패", e);
        }
    }
}
