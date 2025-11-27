package com.example.voicebox.app.device;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/video")
public class VideoGenerationController {

    @PostMapping("/generate-path")
    public ResponseEntity<Resource> generateFromPath(@RequestParam("path") String inputPath) {
        try {
            // Validate input path
            File inputDir = new File(inputPath);
            if (!inputDir.exists() || !inputDir.isDirectory()) {
                return ResponseEntity.badRequest().body(null);
            }

            // Create temp output file
            File tempVideoFile = File.createTempFile("video_", ".mp4");
            
            // Generate video
            ImageToVideoGenerator.generateVideo(inputPath, tempVideoFile.getAbsolutePath());

            // Zip the video file
            File zipFile = createZipFile(tempVideoFile);

            // Clean up original video file (optional, but good practice)
            tempVideoFile.delete();

            return createDownloadResponse(zipFile);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/generate-upload")
    public ResponseEntity<Resource> generateFromUpload(@RequestParam("files") MultipartFile[] files) {
        Path uploadBatchDir = null;
        try {
            // 1. Upload to a temporary directory
            Path tempRoot = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"));
            Path uploadRootDir = tempRoot.resolve("voice-box-uploads");
            
            String batchId = String.valueOf(System.currentTimeMillis());
            uploadBatchDir = uploadRootDir.resolve(batchId);
            Files.createDirectories(uploadBatchDir);
            
            System.out.println("Uploading files to temp dir: " + uploadBatchDir.toAbsolutePath());

            // Save uploaded files
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String fileName = file.getOriginalFilename();
                // Simple security check to avoid directory traversal
                if (fileName != null && !fileName.contains("..")) {
                     // Use simple name to flatten structure
                     String simpleName = new File(fileName).getName();
                     Path targetPath = uploadBatchDir.resolve(simpleName);
                     Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // 2. Define persistent output video path: /tmpVideo
            // This directory is mounted to the host via Docker volume
            File videoOutputDir = new File("/tmpVideo");
            if (!videoOutputDir.exists()) {
                videoOutputDir.mkdirs();
            }
            
            String videoFileName = "video_" + batchId + ".mp4";
            File persistentVideoFile = new File(videoOutputDir, videoFileName);

            // 3. Generate video
            ImageToVideoGenerator.generateVideo(uploadBatchDir.toAbsolutePath().toString(), persistentVideoFile.getAbsolutePath());

            // 4. Zip the video file (source is the persistent file)
            File zipFile = createZipFile(persistentVideoFile);

            // 5. Clean up uploaded images
            deleteDirectory(uploadBatchDir.toFile());
            
            // Note: We keep persistentVideoFile in /tmpVideo as requested.
            System.out.println("Video persistent saved to: " + persistentVideoFile.getAbsolutePath());

            return createDownloadResponse(zipFile);

        } catch (Exception e) {
            e.printStackTrace();
            // Clean up on error
            if (uploadBatchDir != null) {
                deleteDirectory(uploadBatchDir.toFile());
            }
            return ResponseEntity.internalServerError().build();
        }
    }

    private File createZipFile(File sourceFile) throws IOException {
        File zipFile = File.createTempFile("video_download_", ".zip");
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos);
             FileInputStream fis = new FileInputStream(sourceFile)) {

            ZipEntry zipEntry = new ZipEntry(sourceFile.getName());
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) >= 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
        }
        return zipFile;
    }

    private ResponseEntity<Resource> createDownloadResponse(File file) {
        FileSystemResource resource = new FileSystemResource(file);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=video_timelapse.zip");
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
    
    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}

