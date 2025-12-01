package com.example.voicebox.app.device.voice;

import com.example.voicebox.app.device.service.voice.TextSegmentationService;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 文本分段服务单元测试
 * Validates: Requirements 7.1
 */
public class TextSegmentationServiceTest {
    
    private TextSegmentationService service;
    
    @Before
    public void setUp() {
        service = new TextSegmentationService();
    }
    
    @Test
    public void testSegmentBySentence() {
        // 测试按句子分段
        String text = "这是第一句。这是第二句！这是第三句？";
        List<String> segments = service.segmentText(text);
        
        assertEquals("应该分成3个段落", 3, segments.size());
        assertEquals("第一句应该正确", "这是第一句。", segments.get(0));
        assertEquals("第二句应该正确", "这是第二句！", segments.get(1));
        assertEquals("第三句应该正确", "这是第三句？", segments.get(2));
    }
    
    @Test
    public void testSegmentByLength() {
        // 测试按长度分段（超过100字符的句子）
        StringBuilder longSentence = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            longSentence.append("这是一个很长的句子");
        }
        
        List<String> segments = service.segmentText(longSentence.toString());
        
        assertTrue("长句子应该被分段", segments.size() > 1);
        for (String segment : segments) {
            assertTrue("每个段落长度应该合理", segment.length() <= 150);
        }
    }
    
    @Test
    public void testSegmentWithMixedPunctuation() {
        // 测试混合标点符号
        String text = "你好，世界！How are you? 我很好。Thank you;再见";
        List<String> segments = service.segmentText(text);
        
        assertTrue("应该正确分段", segments.size() >= 3);
        
        // 验证所有段落都非空
        for (String segment : segments) {
            assertFalse("段落不应为空", segment.trim().isEmpty());
        }
    }
    
    @Test
    public void testSegmentWithSpecialCharacters() {
        // 测试特殊字符处理
        String text = "这是一句话，包含逗号、顿号；还有分号：以及冒号。最后是句号！";
        List<String> segments = service.segmentText(text);
        
        assertTrue("应该能处理特殊字符", segments.size() >= 1);
        
        // 重新组合应该保持内容
        String reconstructed = String.join("", segments);
        assertEquals("内容应该保持完整", 
                    text.replaceAll("\\s+", ""), 
                    reconstructed.replaceAll("\\s+", ""));
    }
    
    @Test
    public void testSegmentEmptyText() {
        // 测试空文本
        List<String> segments1 = service.segmentText("");
        assertTrue("空字符串应该返回空列表", segments1.isEmpty());
        
        List<String> segments2 = service.segmentText("   ");
        assertTrue("空白字符串应该返回空列表", segments2.isEmpty());
        
        List<String> segments3 = service.segmentText(null);
        assertTrue("null应该返回空列表", segments3.isEmpty());
    }
    
    @Test
    public void testSegmentSingleSentence() {
        // 测试单句
        String text = "这是一个简单的句子。";
        List<String> segments = service.segmentText(text);
        
        assertEquals("单句应该返回一个段落", 1, segments.size());
        assertEquals("内容应该正确", text, segments.get(0));
    }
    
    @Test
    public void testSegmentWithoutPunctuation() {
        // 测试没有标点符号的文本
        String text = "这是一段没有标点符号的文本";
        List<String> segments = service.segmentText(text);
        
        assertEquals("没有标点符号应该返回一个段落", 1, segments.size());
        assertEquals("内容应该正确", text, segments.get(0));
    }
    
    @Test
    public void testSegmentWithMultipleSpaces() {
        // 测试多个空格
        String text = "这是第一句。   这是第二句。";
        List<String> segments = service.segmentText(text);
        
        assertEquals("应该分成2个段落", 2, segments.size());
        assertFalse("段落不应包含多余空格", segments.get(0).contains("   "));
        assertFalse("段落不应包含多余空格", segments.get(1).contains("   "));
    }
    
    @Test
    public void testSegmentWithNewlines() {
        // 测试换行符
        String text = "这是第一句。\n这是第二句。\n\n这是第三句。";
        List<String> segments = service.segmentText(text);
        
        assertEquals("应该分成3个段落", 3, segments.size());
    }
    
    @Test
    public void testSegmentEnglishText() {
        // 测试英文文本
        String text = "Hello world. How are you? I am fine!";
        List<String> segments = service.segmentText(text);
        
        assertEquals("应该分成3个段落", 3, segments.size());
        assertTrue("应该包含Hello", segments.get(0).contains("Hello"));
        assertTrue("应该包含How", segments.get(1).contains("How"));
        assertTrue("应该包含fine", segments.get(2).contains("fine"));
    }
    
    @Test
    public void testEstimateDuration() {
        // 测试时长估算
        String text1 = "这是一个测试";
        double duration1 = service.estimateDuration(text1);
        assertTrue("时长应该大于0", duration1 > 0);
        
        String text2 = "这是一个更长的测试文本，包含更多的字符";
        double duration2 = service.estimateDuration(text2);
        assertTrue("更长的文本应该有更长的时长", duration2 > duration1);
        
        String emptyText = "";
        double duration3 = service.estimateDuration(emptyText);
        assertEquals("空文本时长应该为0", 0.0, duration3, 0.001);
    }
    
    @Test
    public void testEstimateDurationNull() {
        // 测试null时长估算
        double duration = service.estimateDuration(null);
        assertEquals("null时长应该为0", 0.0, duration, 0.001);
    }
    
    @Test
    public void testSegmentPreservesOrder() {
        // 测试分段保持顺序
        String text = "第一句。第二句。第三句。第四句。第五句。";
        List<String> segments = service.segmentText(text);
        
        assertEquals("应该分成5个段落", 5, segments.size());
        assertTrue("第一个段落应该包含'第一'", segments.get(0).contains("第一"));
        assertTrue("第二个段落应该包含'第二'", segments.get(1).contains("第二"));
        assertTrue("第三个段落应该包含'第三'", segments.get(2).contains("第三"));
        assertTrue("第四个段落应该包含'第四'", segments.get(3).contains("第四"));
        assertTrue("第五个段落应该包含'第五'", segments.get(4).contains("第五"));
    }
    
    @Test
    public void testSegmentWithConsecutivePunctuation() {
        // 测试连续标点符号
        String text = "什么？！真的吗。。。好的！！";
        List<String> segments = service.segmentText(text);
        
        assertTrue("应该能处理连续标点", segments.size() >= 1);
        
        // 验证内容完整性
        String reconstructed = String.join("", segments);
        assertFalse("重组后不应为空", reconstructed.trim().isEmpty());
    }
}
