package com.example.voicebox.core.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigLoader {

    public static void loadProperties() {
        // 尝试从项目根目录读取 config.properties
        Path configPath = Paths.get("config.properties");
        if (Files.exists(configPath)) {
            System.out.println("[ConfigLoader] Loading config from " + configPath.toAbsolutePath());
            try (InputStream input = Files.newInputStream(configPath)) {
                Properties prop = new Properties();
                prop.load(input);

                // 将属性注入到 System Properties，供后续模块 (如 HttpChatClient) 使用
                for (String key : prop.stringPropertyNames()) {
                    if (System.getProperty(key) == null) { // 命令行参数优先，不覆盖
                        System.setProperty(key, prop.getProperty(key));
                    }
                }
            } catch (IOException e) {
                System.err.println("[ConfigLoader] Failed to load config.properties: " + e.getMessage());
            }
        } else {
            System.out.println("[ConfigLoader] config.properties not found, skipping.");
        }
    }
}

