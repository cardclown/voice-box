package com.example.voicebox.app.device;

import org.jcodec.api.awt.AWTSequenceEncoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

public class ImageToVideoGenerator {

    /**
     * 将指定文件夹下的照片合成为视频
     *
     * @param inputDirPath  照片所在的文件夹路径
     * @param outputVideoPath 输出视频的文件路径 (e.g., output.mp4)
     * @throws IOException
     */
    public static void generateVideo(String inputDirPath, String outputVideoPath) throws IOException {
        File inputDir = new File(inputDirPath);
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            throw new IllegalArgumentException("Input path is not a valid directory: " + inputDirPath);
        }

        // 1. 获取文件夹下的所有图片文件
        File[] files = inputDir.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png");
        });

        if (files == null || files.length == 0) {
            System.out.println("No images found in " + inputDirPath);
            return;
        }

        // 2. 排序文件，确保按名称顺序生成视频 (这对于大量照片非常重要)
        Arrays.sort(files, Comparator.comparing(File::getName));

        System.out.println("Found " + files.length + " images. Starting video generation...");

        // 3. 创建编码器
        // 10 FPS means 10 frames per second.
        AWTSequenceEncoder encoder = AWTSequenceEncoder.createSequenceEncoder(new File(outputVideoPath), 10);

        try {
            int count = 0;
            for (File imgFile : files) {
                // 读取图片
                BufferedImage image = ImageIO.read(imgFile);
                if (image == null) {
                    System.err.println("Could not read image: " + imgFile.getName() + ", skipping.");
                    continue;
                }

                // 写入帧
                encoder.encodeImage(image);
                
                count++;
                if (count % 100 == 0) {
                    System.out.println("Processed " + count + " / " + files.length + " frames.");
                }
            }
            System.out.println("Video generation completed! Processed " + count + " frames.");
        } finally {
            // 4. 结束并保存
            encoder.finish();
        }
    }

    // Main method for testing
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java ImageToVideoGenerator <input_dir> <output_file>");
            return;
        }
        
        try {
            generateVideo(args[0], args[1]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

