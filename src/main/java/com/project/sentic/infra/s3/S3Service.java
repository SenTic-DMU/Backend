package com.project.sentic.infra.s3;

import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // 음성 파일 업로드 (byte[] → S3)
    public String uploadAudio(byte[] audioData, String prefix) {
        String fileName = prefix + "/" + UUID.randomUUID() + ".mp3";

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType("audio/mpeg")
                    .contentLength((long) audioData.length)
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromBytes(audioData)
            );

            log.info("[S3] 업로드 완료: {}", fileName);

            // Presigned URL 생성 (1시간 유효)
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();

            GetObjectPresignRequest presignRequest =
                    GetObjectPresignRequest.builder()
                            .signatureDuration(Duration.ofHours(1))
                            .getObjectRequest(getObjectRequest)
                            .build();

            String presignedUrl = s3Presigner
                    .presignGetObject(presignRequest)
                    .url()
                    .toString();

            log.info("[S3] Presigned URL 생성 완료: {}", fileName);

            return presignedUrl;

        } catch (Exception e) {
            log.error("[S3] 업로드 실패: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // 파일 삭제
    public void deleteFile(String fileUrl) {
        try {
            URI uri = URI.create(fileUrl);

            String path = uri.getPath();
            String fileName = path.startsWith("/")
                    ? path.substring(1)
                    : path;

            s3Client.deleteObject(builder -> builder
                    .bucket(bucket)
                    .key(fileName)
            );

            log.info("[S3] 삭제 완료: {}", fileName);

        } catch (Exception e) {
            log.error("[S3] 삭제 실패: {}", e.getMessage(), e);
        }
    }
}