package com.campustrade.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.campustrade.common.ApiResponse;
import com.campustrade.common.BusinessException;
import com.campustrade.common.ErrorCode;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB

    private final Path uploadDir;

    public UploadController(@Value("${campus-trade.upload-dir:uploads}") String dir) {
        this.uploadDir = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录: " + uploadDir, e);
        }
    }

    @PostMapping("/image")
    public ApiResponse<UploadResult> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件大小不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 JPG、PNG、GIF、WebP 格式");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + ext;

        try {
            Path target = uploadDir.resolve(filename);
            file.transferTo(target);
            return ApiResponse.success(new UploadResult("/uploads/" + filename, filename));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件上传失败");
        }
    }

    public record UploadResult(String url, String filename) {}
}
