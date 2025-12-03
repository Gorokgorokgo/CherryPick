-- Add thumbnail URL and file size fields to uploaded_images table
-- Migration: V1.7
-- Date: 2025-12-03
-- Description: Add thumbnail_url and thumbnail_file_size columns for automatic thumbnail generation feature

-- Add thumbnail_url column
ALTER TABLE uploaded_images
ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(500);

-- Add thumbnail_file_size column
ALTER TABLE uploaded_images
ADD COLUMN IF NOT EXISTS thumbnail_file_size BIGINT;

-- Add comment for documentation
COMMENT ON COLUMN uploaded_images.thumbnail_url IS '썸네일 이미지 URL (300x300)';
COMMENT ON COLUMN uploaded_images.thumbnail_file_size IS '썸네일 파일 크기 (bytes)';

-- Note: Existing records will have NULL values for these fields
-- They will be populated when images are re-uploaded or regenerated
