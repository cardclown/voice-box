package com.example.voicebox.app.device;

import com.example.voicebox.core.util.ConfigLoader;
import com.example.voicebox.cloud.AsrClient;
import com.example.voicebox.cloud.ChatClient;
import com.example.voicebox.cloud.ChatRequest;
import com.example.voicebox.cloud.ChatResponse;
import com.example.voicebox.cloud.TtsClient;
import com.example.voicebox.cloud.VoiceStyle;
import com.example.voicebox.cloud.http.HttpCloudClients;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * 本地命令行测试工具
 * 用于单独测试云端 API (豆包、DeepSeek、阿里云 ASR/TTS) 是否连通
 */
public class CloudServiceTester {

    private static final BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
    private static ChatClient chatClient;
    private static AsrClient asrClient;
    private static TtsClient ttsClient;

    public static void main(String[] args) throws IOException {
        System.out.println(">>> Loading config.properties...");
        ConfigLoader.loadProperties();

        // 初始化所有客户端
        chatClient = HttpCloudClients.chatClient();
        asrClient = HttpCloudClients.asrClient();
        ttsClient = HttpCloudClients.ttsClient();

        while (true) {
            System.out.println("\n==========================================");
            System.out.println("   VoiceBox Cloud Service Tester");
            System.out.println("==========================================");
            System.out.println("1. Chat Mode (Test Doubao/DeepSeek)");
            System.out.println("2. TTS Test (Text -> Audio File)");
            System.out.println("3. ASR Test (Audio File -> Text)");
            System.out.println("q. Quit");
            System.out.print("Select mode: ");

            String input = console.readLine();
            if (input == null || "q".equalsIgnoreCase(input.trim())) {
                break;
            }

            switch (input.trim()) {
                case "1":
                    runChatMode();
                    break;
                case "2":
                    runTtsTest();
                    break;
                case "3":
                    runAsrTest();
                    break;
                default:
                    System.out.println("Invalid selection.");
            }
        }
        System.out.println("Bye!");
    }

    private static void runChatMode() throws IOException {
        System.out.println("\n[Chat Mode] Select Model:");
        System.out.println("1. Default (from config)");
        System.out.println("2. Doubao (voicebox.chat.model.doubao)");
        System.out.println("3. DeepSeek (voicebox.chat.model.deepseek)");
        System.out.print("Select > ");
        String sel = console.readLine();
        
        String useModel = null;
        String modelName = "Default";
        
        if ("2".equals(sel.trim())) {
            useModel = System.getProperty("voicebox.chat.model.doubao");
            modelName = "Doubao";
        } else if ("3".equals(sel.trim())) {
            useModel = System.getProperty("voicebox.chat.model.deepseek");
            modelName = "DeepSeek";
        }
        
        // 如果选了特定模型，临时创建一个新的 client；否则用默认全局 client
        ChatClient activeClient = chatClient;
        if (useModel != null && !useModel.isEmpty()) {
            System.out.println(">>> Switching to " + modelName + " Endpoint: " + useModel);
            // 复用默认配置的 URL 和 Key，只换 Model
            String url = System.getProperty("voicebox.chat.url");
            String key = System.getProperty("voicebox.chat.apiKey");
            // 这里我们需要构造 HttpChatClient，因为接口 ChatClient 没有 getModel()
            // 简单起见，利用反射或者直接依赖实现类 (因为这是测试类)
            activeClient = new com.example.voicebox.cloud.http.HttpChatClient(url, key, useModel);
        } else {
            System.out.println(">>> Using Default Configured Model");
        }

        System.out.println("\nType your message (or 'exit' to return):");
        while (true) {
            System.out.print("You > ");
            String text = console.readLine();
            if (text == null || "exit".equalsIgnoreCase(text.trim())) {
                break;
            }
            if (text.trim().isEmpty()) continue;

            try {
                long start = System.currentTimeMillis();
                ChatResponse response = activeClient.chat(new ChatRequest(Collections.emptyList(), text));
                long end = System.currentTimeMillis();
                
                System.out.println("AI  > " + response.getText());
                System.out.println("      (Took " + (end - start) + "ms)");
            } catch (Exception e) {
                System.err.println("Error calling Chat API: " + e.getMessage());
            }
        }
    }

    private static void runTtsTest() throws IOException {
        System.out.println("\n[TTS Test] Enter text to synthesize:");
        System.out.print("Text > ");
        String text = console.readLine();
        if (text == null || text.trim().isEmpty()) return;

        System.out.println("Synthesizing...");
        try {
            long start = System.currentTimeMillis();
            byte[] audioData = ttsClient.synthesize(text, VoiceStyle.DEFAULT);
            long end = System.currentTimeMillis();

            if (audioData.length > 0) {
                String filename = "tts_output_" + System.currentTimeMillis() + ".wav"; // 假设是wav, 具体看你阿里云配置
                try (FileOutputStream fos = new FileOutputStream(filename)) {
                    fos.write(audioData);
                }
                System.out.println("Success! Audio saved to: " + new File(filename).getAbsolutePath());
                System.out.println("Size: " + audioData.length + " bytes, Time: " + (end - start) + "ms");
            } else {
                System.err.println("Error: Received empty audio data.");
            }
        } catch (Exception e) {
            System.err.println("Error calling TTS API: " + e.getMessage());
        }
    }

    private static void runAsrTest() throws IOException {
        System.out.println("\n[ASR Test] Enter path to local audio file (e.g. /tmp/test.wav):");
        System.out.print("Path > ");
        String pathStr = console.readLine();
        if (pathStr == null || pathStr.trim().isEmpty()) return;

        File file = new File(pathStr.trim());
        if (!file.exists()) {
            System.err.println("File not found: " + pathStr);
            return;
        }

        System.out.println("Reading file (" + file.length() + " bytes)...");
        byte[] pcmData = Files.readAllBytes(Paths.get(file.getPath()));

        System.out.println("Recognizing...");
        try {
            long start = System.currentTimeMillis();
            String result = asrClient.recognize(pcmData);
            long end = System.currentTimeMillis();

            System.out.println("ASR Result > " + result);
            System.out.println("             (Took " + (end - start) + "ms)");
        } catch (Exception e) {
            System.err.println("Error calling ASR API: " + e.getMessage());
        }
    }
}

