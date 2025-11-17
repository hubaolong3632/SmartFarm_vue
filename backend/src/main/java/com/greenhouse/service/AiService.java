package com.greenhouse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * AI服务类
 * 调用DeepSeek API进行图片分析、报告生成和自动化控制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {
    
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.openai.chat.options.model}")
    private String model;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private RestTemplate getRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(300000); // 连接超时：5分钟（300秒）
        factory.setReadTimeout(300000);   // 读取超时：5分钟（300秒）
        return new RestTemplate(factory);
    }
    
    /**
     * 调用DeepSeek API（非流式）
     */
    private String callAiApi(String prompt) {
        try {
            String url = baseUrl + "/v1/chat/completions";
//
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", "你是一个专业的智能温室管理AI助手，擅长分析温室数据、生成报告和提供自动化控制建议。所有报告请尽量用 Markdown 表格展现关键数据，多使用图标、少用纯文字，并在每个小节结尾给出简短总结。"),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            RestTemplate restTemplate = getRestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                JsonNode choices = jsonNode.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null) {
                        return message.get("content").asText();
                    }
                }
            }
            
            log.error("AI API调用失败: {}", response.getBody());
            return "AI分析失败，请稍后重试";
            
        } catch (Exception e) {
            log.error("调用AI API异常: {}", e.getMessage(), e);
            return "AI服务异常: " + e.getMessage();
        }
    }
    
    /**
     * 流式调用DeepSeek API
     */
    public void callAiApiStream(String prompt, Consumer<String> onChunk, Runnable onComplete) {
        try {
            String url = baseUrl + "/v1/chat/completions";
            
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);
            connection.setConnectTimeout(300000);
            connection.setReadTimeout(300000);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", "你是一个专业的智能温室管理AI助手，擅长分析温室数据、生成报告和提供自动化控制建议。但是你要注意1.所有的回答都要使用表格或图标的方式回答 2.回答的内容不能太单调 3.多有图标，少用文字回答，每隔都要用总结"),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            requestBody.put("stream", true); // 启用流式输出
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            connection.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder fullContent = new StringBuilder();
                    
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) {
                                break;
                            }
                            
                            try {
                                JsonNode jsonNode = objectMapper.readTree(data);
                                JsonNode choices = jsonNode.get("choices");
                                if (choices != null && choices.isArray() && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).get("delta");
                                    if (delta != null && delta.has("content")) {
                                        String content = delta.get("content").asText();
                                        fullContent.append(content);
                                        onChunk.accept(content);
                                    }
                                }
                            } catch (Exception e) {
                                // 忽略解析错误，继续处理下一行
                            }
                        }
                    }
                    
                    onComplete.run();
                }
            } else {
                log.error("AI API流式调用失败，响应码: {}", responseCode);
                onChunk.accept("AI分析失败，请稍后重试");
                onComplete.run();
            }
            
        } catch (Exception e) {
            log.error("流式调用AI API异常: {}", e.getMessage(), e);
            onChunk.accept("AI服务异常: " + e.getMessage());
            onComplete.run();
        }
    }
    
    /**
     * 分析图片集并生成报告
     */
    public String analyzeImages(List<Map<String, Object>> images) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下温室图片数据，生成一份结构清晰的报告。报告应包括：\n");
        prompt.append("1. 整体环境状况评估。\n");
        prompt.append("2. 异常情况分析（温度、湿度、土壤湿度等）。\n");
        prompt.append("3. 植物生长状态评估。\n");
        prompt.append("4. 改进建议（给出优先级或操作建议）。\n\n");
        prompt.append("图片数据：\n");
        
        for (Map<String, Object> image : images) {
            prompt.append(String.format(
                "时间: %s, 温度: %s°C, 湿度: %s%%, 土壤湿度: %s%%, 光照: %s lux, 是否异常: %s\n",
                image.get("recordTime"),
                image.get("temperatureC"),
                image.get("humidityPct"),
                image.get("soilMoisturePct"),
                image.get("lightLux"),
                image.get("isAbnormal")
            ));
        }
        
        return callAiApi(prompt.toString());
    }
    
    /**
     * 流式分析图片集并生成报告
     */
    public void analyzeImagesStream(List<Map<String, Object>> images, Consumer<String> onChunk, Runnable onComplete) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下温室图片数据，生成一份结构清晰的报告。报告应包括：\n");
        prompt.append("1. 整体环境状况评估。\n");
        prompt.append("2. 异常情况分析（温度、湿度、土壤湿度等）。\n");
        prompt.append("3. 植物生长状态评估。\n");
        prompt.append("4. 改进建议（给出优先级或操作建议）。\n\n");
        prompt.append("图片数据：\n");
        
        for (Map<String, Object> image : images) {
            prompt.append(String.format(
                "时间: %s, 温度: %s°C, 湿度: %s%%, 土壤湿度: %s%%, 光照: %s lux, 是否异常: %s\n",
                image.get("recordTime"),
                image.get("temperatureC"),
                image.get("humidityPct"),
                image.get("soilMoisturePct"),
                image.get("lightLux"),
                image.get("isAbnormal")
            ));
        }
        
        callAiApiStream(prompt.toString(), onChunk, onComplete);
    }
    
    /**
     * 分析传感器数据并生成报告
     */
    public String analyzeSensorData(List<Map<String, Object>> sensorData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下温室传感器数据，生成一份环境分析报告。报告应包括：\n");
        prompt.append("1. 温度、湿度、土壤湿度等主要指标的趋势和当前状态。\n");
        prompt.append("2. 异常值识别及可能原因。\n");
        prompt.append("3. 环境健康度评估。\n");
        prompt.append("4. 预警与优化建议（可给出优先级）。\n\n");
        prompt.append("传感器数据：\n");
        
        for (Map<String, Object> data : sensorData) {
            prompt.append(String.format(
                "时间: %s, 温度: %s°C, 湿度: %s%%, 土壤湿度: %s%%, 光照: %s lux, 氧气: %s%%, 二氧化碳: %s ppm\n",
                data.get("recordTime"),
                data.get("temperatureC"),
                data.get("humidityPct"),
                data.get("soilMoisturePct"),
                data.get("lightLux"),
                data.get("oxygenPct"),
                data.get("co2Ppm")
            ));
        }
        
        return callAiApi(prompt.toString());
    }
    
    /**
     * 流式分析传感器数据并生成报告
     */
    public void analyzeSensorDataStream(List<Map<String, Object>> sensorData, Consumer<String> onChunk, Runnable onComplete) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下温室传感器数据，生成一份环境分析报告。报告应包括：\n");
        prompt.append("1. 温度、湿度、土壤湿度等主要指标的趋势和当前状态。\n");
        prompt.append("2. 异常值识别及可能原因。\n");
        prompt.append("3. 环境健康度评估。\n");
        prompt.append("4. 预警与优化建议（可给出优先级）。\n\n");
        prompt.append("传感器数据：\n");
        
        for (Map<String, Object> data : sensorData) {
            prompt.append(String.format(
                "时间: %s, 温度: %s°C, 湿度: %s%%, 土壤湿度: %s%%, 光照: %s lux, 氧气: %s%%, 二氧化碳: %s ppm\n",
                data.get("recordTime"),
                data.get("temperatureC"),
                data.get("humidityPct"),
                data.get("soilMoisturePct"),
                data.get("lightLux"),
                data.get("oxygenPct"),
                data.get("co2Ppm")
            ));
        }
        
        callAiApiStream(prompt.toString(), onChunk, onComplete);
    }
    
    /**
     * 生成自动化控制建议
     */
    public String generateAutomationAdvice(Map<String, Object> currentData, Map<String, Object> automationSettings) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("基于当前温室数据和自动化设置，请提供智能控制建议。报告应包括：\n");
        prompt.append("1. 温室当前环境状态摘要。\n");
        prompt.append("2. 与自动化阈值的对比情况。\n");
        prompt.append("3. 优先级排序的控制建议以及预计效果。\n");
        prompt.append("4. 如需要，给出建议的自动化策略或动作流程。\n\n");
        prompt.append("当前数据：\n");
        prompt.append(String.format(
            "温度: %s°C, 湿度: %s%%, 土壤湿度: %s%%, 光照: %s lux\n",
            currentData.get("temperatureC"),
            currentData.get("humidityPct"),
            currentData.get("soilMoisturePct"),
            currentData.get("lightLux")
        ));
        prompt.append("\n自动化设置：\n");
        prompt.append(String.format(
            "光照阈值: %s lux, 土壤湿度低阈值: %s%%, 温度高阈值: %s°C, 温度低阈值: %s°C\n",
            automationSettings.get("lightLuxThreshold"),
            automationSettings.get("soilMoistureLowThreshold"),
            automationSettings.get("temperatureHighThreshold"),
            automationSettings.get("temperatureLowThreshold")
        ));
        prompt.append("\n请提供：\n");
        prompt.append("1. 当前是否需要调整控制参数\n");
        prompt.append("2. 建议的自动化操作（如：是否需要开灯、浇水等）\n");
        prompt.append("3. 参数优化建议\n");
        
        return callAiApi(prompt.toString());
    }
    
    /**
     * 流式生成自动化控制建议
     */
    public void generateAutomationAdviceStream(Map<String, Object> currentData, Map<String, Object> automationSettings, Consumer<String> onChunk, Runnable onComplete) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("基于当前温室数据和自动化设置，请提供智能控制建议。报告应包括：\n");
        prompt.append("1. 温室当前环境状态摘要。\n");
        prompt.append("2. 与自动化阈值的对比情况。\n");
        prompt.append("3. 优先级排序的控制建议以及预计效果。\n");
        prompt.append("4. 如需要，给出建议的自动化策略或动作流程。\n\n");
        prompt.append("当前数据：\n");
        prompt.append(String.format(
            "温度: %s°C, 湿度: %s%%, 土壤湿度: %s%%, 光照: %s lux\n",
            currentData.get("temperatureC"),
            currentData.get("humidityPct"),
            currentData.get("soilMoisturePct"),
            currentData.get("lightLux")
        ));
        prompt.append("\n自动化设置：\n");
        prompt.append(String.format(
            "光照阈值: %s lux, 土壤湿度低阈值: %s%%, 温度高阈值: %s°C, 温度低阈值: %s°C\n",
            automationSettings.get("lightLuxThreshold"),
            automationSettings.get("soilMoistureLowThreshold"),
            automationSettings.get("temperatureHighThreshold"),
            automationSettings.get("temperatureLowThreshold")
        ));
        prompt.append("\n请提供：\n");
        prompt.append("1. 当前是否需要调整控制参数\n");
        prompt.append("2. 建议的自动化操作（如：是否需要开灯、浇水等）\n");
        prompt.append("3. 参数优化建议\n");
        
        callAiApiStream(prompt.toString(), onChunk, onComplete);
    }
    
    /**
     * 生成综合报告
     */
    public String generateComprehensiveReport(
            List<Map<String, Object>> images,
            List<Map<String, Object>> sensorData,
            List<Map<String, Object>> executionLogs,
            Map<String, Object> automationSettings) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于以下温室数据生成一份综合管理报告。报告应包括：\n");
        prompt.append("1. 图片、传感器数据、执行日志的摘要。\n");
        prompt.append("2. 关键指标的对比与异常说明。\n");
        prompt.append("3. 综合研判与风险分析。\n");
        prompt.append("4. 行动计划（可给出优先级和建议步骤）。\n\n");
        
        prompt.append("1. 图片数据（最近记录）：\n");
        int imageCount = Math.min(images.size(), 10);
        for (int i = 0; i < imageCount; i++) {
            Map<String, Object> img = images.get(i);
            prompt.append(String.format(
                "时间: %s, 温度: %s°C, 湿度: %s%%, 土壤湿度: %s%%, 异常: %s\n",
                img.get("recordTime"),
                img.get("temperatureC"),
                img.get("humidityPct"),
                img.get("soilMoisturePct"),
                img.get("isAbnormal")
            ));
        }
        
        prompt.append("\n2. 传感器数据（最近记录）：\n");
        int sensorCount = Math.min(sensorData.size(), 10);
        for (int i = 0; i < sensorCount; i++) {
            Map<String, Object> data = sensorData.get(i);
            prompt.append(String.format(
                "时间: %s, 温度: %s°C, 湿度: %s%%, 土壤湿度: %s%%, 光照: %s lux\n",
                data.get("recordTime"),
                data.get("temperatureC"),
                data.get("humidityPct"),
                data.get("soilMoisturePct"),
                data.get("lightLux")
            ));
        }
        
        prompt.append("\n3. 执行日志（最近记录）：\n");
        int logCount = Math.min(executionLogs.size(), 10);
        for (int i = 0; i < logCount; i++) {
            Map<String, Object> log = executionLogs.get(i);
            prompt.append(String.format(
                "时间: %s, 地块: %s, 执行次数: %s\n",
                log.get("executedAt"),
                log.get("plotId"),
                log.get("executions")
            ));
        }
        
        prompt.append("\n请生成报告，包括：\n");
        prompt.append("1. 环境状况总结\n");
        prompt.append("2. 异常情况分析\n");
        prompt.append("3. 执行效果评估\n");
        prompt.append("4. 优化建议\n");
        prompt.append("5. 下一步行动计划\n");
        
        return callAiApi(prompt.toString());
    }
    
    /**
     * 流式生成综合报告
     */
    public void generateComprehensiveReportStream(
            List<Map<String, Object>> images,
            List<Map<String, Object>> sensorData,
            List<Map<String, Object>> executionLogs,
            Map<String, Object> automationSettings,
            Consumer<String> onChunk,
            Runnable onComplete) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于以下温室数据生成一份综合管理报告。报告应包括：\n");
        prompt.append("1. 图片、传感器数据、执行日志的摘要。\n");
        prompt.append("2. 关键指标的对比与异常说明。\n");
        prompt.append("3. 综合研判与风险分析。\n");
        prompt.append("4. 行动计划（可给出优先级和建议步骤）。\n\n");
        
        prompt.append("1. 图片数据（最近记录）：\n");
        int imageCount = Math.min(images.size(), 10);
        for (int i = 0; i < imageCount; i++) {
            Map<String, Object> img = images.get(i);
            prompt.append(String.format(
                "时间: %s, 温度: %s°C, 湿度: %s%%, 土壤湿度: %s%%, 异常: %s\n",
                img.get("recordTime"),
                img.get("temperatureC"),
                img.get("humidityPct"),
                img.get("soilMoisturePct"),
                img.get("isAbnormal")
            ));
        }
        
        prompt.append("\n2. 传感器数据（最近记录）：\n");
        int sensorCount = Math.min(sensorData.size(), 10);
        for (int i = 0; i < sensorCount; i++) {
            Map<String, Object> data = sensorData.get(i);
            prompt.append(String.format(
                "时间: %s, 温度: %s°C, 湿度: %s%%, 土壤湿度: %s%%, 光照: %s lux\n",
                data.get("recordTime"),
                data.get("temperatureC"),
                data.get("humidityPct"),
                data.get("soilMoisturePct"),
                data.get("lightLux")
            ));
        }
        
        prompt.append("\n3. 执行日志（最近记录）：\n");
        int logCount = Math.min(executionLogs.size(), 10);
        for (int i = 0; i < logCount; i++) {
            Map<String, Object> log = executionLogs.get(i);
            prompt.append(String.format(
                "时间: %s, 地块: %s, 执行次数: %s\n",
                log.get("executedAt"),
                log.get("plotId"),
                log.get("executions")
            ));
        }
        
        prompt.append("\n请生成报告，包括：\n");
        prompt.append("1. 环境状况总结\n");
        prompt.append("2. 异常情况分析\n");
        prompt.append("3. 执行效果评估\n");
        prompt.append("4. 优化建议\n");
        prompt.append("5. 下一步行动计划\n");
        
        callAiApiStream(prompt.toString(), onChunk, onComplete);
    }
    
    /**
     * AI自动执行建议（返回可执行的JSON格式建议）
     */
    public Map<String, Object> getAutoExecutionAdvice(Map<String, Object> currentData) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("基于当前温室数据，请提供自动化执行建议。\n\n");
        prompt.append("当前数据：\n");
        prompt.append(String.format(
            "温度: %s°C, 湿度: %s%%, 土壤湿度: %s%%, 光照: %s lux, 氧气: %s%%, 二氧化碳: %s ppm\n",
            currentData.get("temperatureC"),
            currentData.get("humidityPct"),
            currentData.get("soilMoisturePct"),
            currentData.get("lightLux"),
            currentData.get("oxygenPct"),
            currentData.get("co2Ppm")
        ));
        prompt.append("\n请以JSON格式返回建议，格式如下（summary 字段可使用简洁的 Markdown 文字描述要点，无需图形）：\n");
        prompt.append("{\n");
        prompt.append("  \"actions\": [\n");
        prompt.append("    {\"type\": \"light\", \"action\": \"on/off\", \"reason\": \"原因\"},\n");
        prompt.append("    {\"type\": \"pump\", \"action\": \"on/off\", \"reason\": \"原因\"},\n");
        prompt.append("    {\"type\": \"recipe\", \"plotId\": 1, \"recipeId\": \"配方ID\", \"executions\": 1, \"reason\": \"原因\"}\n");
        prompt.append("  ],\n");
        prompt.append("  \"summary\": \"执行建议总结\"\n");
        prompt.append("}\n");
        
        String response = callAiApi(prompt.toString());
        
        // 尝试解析JSON响应
        try {
            // 提取JSON部分（如果响应包含markdown代码块）
            String jsonStr = response;
            if (response.contains("```json")) {
                int start = response.indexOf("```json") + 7;
                int end = response.indexOf("```", start);
                jsonStr = response.substring(start, end).trim();
            } else if (response.contains("```")) {
                int start = response.indexOf("```") + 3;
                int end = response.indexOf("```", start);
                jsonStr = response.substring(start, end).trim();
            }
            
            return objectMapper.readValue(jsonStr, Map.class);
        } catch (Exception e) {
            log.warn("无法解析AI返回的JSON，返回文本响应: {}", e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("summary", response);
            result.put("actions", new ArrayList<>());
            return result;
        }
    }
}

