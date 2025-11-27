package com.example.voicebox.app.device;

import com.example.voicebox.core.util.ConfigLoader;
import com.example.voicebox.cloud.http.HttpCloudClients;
import com.example.voicebox.cloud.sample.DummyCloudClients;
import com.example.voicebox.core.*;
import com.example.voicebox.hardware.InputEventListener;
import com.example.voicebox.hardware.mock.MockHardware;
import com.example.voicebox.app.device.db.DatabaseLogger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication(scanBasePackages = "com.example.voicebox")
public class DeviceApp {
    
    // Global Engine instances to be shared with Controllers
    public static OnlineConversationEngine GLOBAL_ONLINE_ENGINE;

    public static void main(String[] args) {
        // 1. 加载本地配置文件 (config.properties)
        ConfigLoader.loadProperties();
        
        // 2. 初始化数据库 (建表等)
        // DatabaseLogger.getInstance();

        // 3. Initialize Engines & Hardware
        // In real device, you would wire concrete Raspberry Pi implementations here.
        MockHardware hardware = new MockHardware();

        boolean useHttpCloud = Boolean.parseBoolean(
                System.getProperty("voicebox.useHttpCloud",
                        System.getenv("VOICEBOX_USE_HTTP_CLOUD") != null
                                ? System.getenv("VOICEBOX_USE_HTTP_CLOUD")
                                : "false")
        );

        OnlineConversationEngine onlineEngine;
        if (useHttpCloud) {
            System.out.println("[DeviceApp] Using HTTP-based cloud clients");
            onlineEngine = new OnlineConversationEngine(
                    HttpCloudClients.asrClient(),
                    HttpCloudClients.chatClient(),
                    HttpCloudClients.ttsClient(),
                    HttpCloudClients.emotionAnalyzer()
            );
        } else {
            System.out.println("[DeviceApp] Using dummy cloud clients");
            onlineEngine = new OnlineConversationEngine(
                DummyCloudClients.asrClient(),
                DummyCloudClients.chatClient(),
                DummyCloudClients.ttsClient(),
                DummyCloudClients.emotionAnalyzer()
            );
        }
        
        // Expose to static context for Controller use (Simple approach for now)
        GLOBAL_ONLINE_ENGINE = onlineEngine;

        OfflineConversationEngine offlineEngine = new OfflineConversationEngine();

        ModeManager modeManager = new ModeManager(hardware, onlineEngine, offlineEngine);

        hardware.registerListener(new InputEventListener() {
            @Override
            public void onWakeButtonPressed() {
                System.out.println("[DeviceApp] Wake button pressed");
                hardware.showState(com.example.voicebox.hardware.DeviceState.LISTENING);
                hardware.startRecording();
                byte[] pcm = hardware.stopAndFetchPcm();
                ConversationResult result = modeManager.handle(pcm, new DeviceContext());
                hardware.showEmotion(result.getEmotionType());
                hardware.showText(result.getText());
                hardware.playPcm(result.getTtsBytes());
                
                // 记录到数据库
                DatabaseLogger.getInstance().logConversation(
                    result.getText(), 
                    result.getEmotionType().name()
                );
            }
        });

        // 4. Start Spring Boot Web Server (for Vue frontend API)
        // Note: This blocks the main thread, so hardware loop runs in background or via listeners
        // SpringApplication.run(DeviceApp.class, args); 
        
        // Workaround for "DispatcherServlet not found" (405/404 errors):
        // When running via 'mvn exec:java', Spring Boot might not initialize the Servlet container correctly 
        // if main class is just 'exec'. Better to let Spring handle the lifecycle.
        
        SpringApplication app = new SpringApplication(DeviceApp.class);
        app.run(args);
        
        // Trigger initial simulation after startup (optional, might need valid context)
        // hardware.simulateWakeButtonPress(); 
    }
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:5173") // Vue default dev port
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("*")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}
