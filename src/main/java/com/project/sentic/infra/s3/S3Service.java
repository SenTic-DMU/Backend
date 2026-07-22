package com.project.sentic.infra.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // 음성 파일 업로드 (byte[] → S3)
    public String uploadAudio(byte[] audioData, String prefix) {
        String fileName = prefix + "/" + UUID.randomUUID() + ".mp3";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("audio/mpeg");
        metadata.setContentLength(audioData.length);

        try {
            amazonS3.putObject(bucket, fileName, new ByteArrayInputStream(audioData), metadata);
            log.info("[S3] 업로드 완료: {}", fileName);

            // Presigned URL 생성 (1시간 유효)
            Date expiration = new Date(System.currentTimeMillis() + 1000 * 60 * 60);
            GeneratePresignedUrlRequest presignedUrlRequest =
                    new GeneratePresignedUrlRequest(bucket, fileName)
                            .withMethod(HttpMethod.GET)
                            .withExpiration(expiration);

            return amazonS3.generatePresignedUrl(presignedUrlRequest).toString();
        } catch (Exception e) {
            log.error("[S3] 업로드 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 파일 삭제
    public void deleteFile(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.indexOf(".com/") + 5);
            amazonS3.deleteObject(bucket, fileName);
            log.info("[S3] 삭제 완료: {}", fileName);
        } catch (Exception e) {
            log.error("[S3] 삭제 실패: {}", e.getMessage());
        }
    }
}