package com.example.voicebox.app.device.controller;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    private static final File UPLOAD_DIR = new File("/tmp/voicebox/uploads");
    private static final File OUTPUT_DIR = new File("/tmp/voicebox/converted");

    @PostMapping("/convert")
    public Map<String, String> convert(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先选择要上传的视频文件");
        }

        ensureDir(UPLOAD_DIR);
        ensureDir(OUTPUT_DIR);

        try {
            String originalName = StringUtils.cleanPath(file.getOriginalFilename());
            if (originalName == null || originalName.isEmpty()) {
                originalName = "video.mp4";
            }

            File uploaded = new File(UPLOAD_DIR, System.currentTimeMillis() + "_" + originalName);
            file.transferTo(uploaded);

            String baseName = originalName.contains(".")
                    ? originalName.substring(0, originalName.lastIndexOf('.'))
                    : originalName;
            File converted = new File(OUTPUT_DIR, baseName + "_converted.mp4");

            // 模拟转换：实际项目中可以调用 FFmpeg 或自研算法。这里简单复制文件，保持接口连通性。
            Files.copy(uploaded.toPath(), converted.toPath(), StandardCopyOption.REPLACE_EXISTING);

            Map<String, String> resp = new HashMap<>();
            resp.put("message", "视频转换完成！");
            resp.put("outputPath", converted.getAbsolutePath());
            resp.put("fileName", converted.getName());
            return resp;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "视频处理失败：" + e.getMessage());
        }
    }

    private void ensureDir(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}

