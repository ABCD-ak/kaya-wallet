package com.kaya.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kaya.common.ApiResponse;
import com.kaya.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 免费 AI 客服代理，避免浏览器跨域验证并统一限制上下文长度。 */
@RestController
@RequestMapping("/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);
    private static final URI AI_ENDPOINT = URI.create("https://text.pollinations.ai/openai");
    private static final int MAX_ATTEMPTS = 2;
    private static final Duration ATTEMPT_TIMEOUT = Duration.ofSeconds(45);
    /** 免费通道同一 IP 每 45 秒只能成功回答一次（HTTP 402 FREE_LIMIT）。 */
    private static final int FREE_LIMIT_SECONDS = 45;
    /** 首次失败很快时值得重试；已经等过一次完整超时就不要再叠加等待。 */
    private static final long RETRY_WINDOW_MILLIS = 20_000;
    private static final Pattern RETRY_MESSAGE = Pattern.compile("Retry after (\\d+)s");
    private static final String SYSTEM_PROMPT = """
            你是卡雅（Kaya）手机钱包应用的 AI 客服。请使用简体中文，语气温和、简洁，每次回答不超过 180 字。
            不得编造交易状态、到账结果、账户数据或平台政策，不得索取密码、验证码、完整卡号等敏感信息。
            已知信息：人工客服每天 9:00-22:00 在线，邮箱 support@kaya.app，热线 400-800-1234；银行卡可在「我的账户-银行卡」冻结；转账记录可在「交易记录」查看。
            涉及具体账户或交易处理时，说明需要人工核实并引导联系人工客服。回答与钱包无关的问题时保持简短，并自然引导回客服主题。
            """;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiChatController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chat(@RequestBody JsonNode request) {
        ObjectNode payload = buildPayload(request);
        String lastUserText = lastUserText(request);
        long startNanos = System.nanoTime();
        Integer retryAfter = null;

        // 免费上游偶发限流/超时：快速失败时重试一次，限流则不浪费重试
        for (int attempt = 1; attempt <= MAX_ATTEMPTS && !Thread.currentThread().isInterrupted(); attempt++) {
            try {
                HttpRequest aiRequest = HttpRequest.newBuilder(AI_ENDPOINT)
                        .timeout(ATTEMPT_TIMEOUT)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> aiResponse = httpClient.send(
                        aiRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = aiResponse.statusCode();

                if (status == 402 || status == 429) {
                    // 免费通道每 45 秒仅 1 次，立即重试必然再次被拒
                    retryAfter = parseRetryAfter(aiResponse);
                    log.warn("Free AI rate limited (HTTP {}), retry after {}s", status, retryAfter);
                    break;
                }
                if (status < 200 || status >= 300) {
                    log.warn("Free AI returned HTTP {} (attempt {})", status, attempt);
                    if (canRetry(attempt, startNanos)) {
                        pause();
                        continue;
                    }
                    break;
                }

                String content = objectMapper.readTree(aiResponse.body())
                        .path("choices").path(0).path("message").path("content").asText("").trim();
                if (content.isEmpty()) {
                    log.warn("Free AI returned empty content (attempt {})", attempt);
                    if (canRetry(attempt, startNanos)) {
                        pause();
                        continue;
                    }
                    break;
                }
                return ResponseEntity.ok(ApiResponse.ok(Map.of("reply", content)));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (HttpTimeoutException ex) {
                // 已经等满一次超时，再重试只会让前端多等一倍时间
                log.warn("Free AI timed out after {}ms (attempt {})", elapsedMillis(startNanos), attempt);
                break;
            } catch (Exception ex) {
                log.warn("Free AI request failed (attempt {}): {}", attempt, ex.getMessage());
                if (canRetry(attempt, startNanos)) {
                    pause();
                    continue;
                }
                break;
            }
        }

        // 上游不可用时给出可用答复，避免聊天直接报错
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reply", retryAfter != null
                ? limitReply(retryAfter, lastUserText)
                : fallbackReply(lastUserText));
        if (retryAfter != null) {
            data.put("retryAfter", retryAfter);
        }
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    private boolean canRetry(int attempt, long startNanos) {
        return attempt < MAX_ATTEMPTS
                && elapsedMillis(startNanos) < RETRY_WINDOW_MILLIS
                && !Thread.currentThread().isInterrupted();
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private void pause() {
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /** 读取上游的 Retry-After，缺失时用免费通道默认的 45 秒。 */
    private int parseRetryAfter(HttpResponse<String> response) {
        String header = response.headers().firstValue("Retry-After").orElse("").trim();
        if (!header.isEmpty()) {
            try {
                return Math.max(1, Math.min(FREE_LIMIT_SECONDS, Integer.parseInt(header)));
            } catch (NumberFormatException ignored) {
                // 头部不是秒数时继续解析响应体
            }
        }
        Matcher matcher = RETRY_MESSAGE.matcher(response.body() == null ? "" : response.body());
        if (matcher.find()) {
            return Math.max(1, Math.min(FREE_LIMIT_SECONDS, Integer.parseInt(matcher.group(1))));
        }
        return FREE_LIMIT_SECONDS;
    }

    private String lastUserText(JsonNode request) {
        JsonNode incoming = request == null ? null : request.path("messages");
        if (incoming == null || !incoming.isArray()) {
            return "";
        }
        for (int i = incoming.size() - 1; i >= 0; i--) {
            JsonNode item = incoming.get(i);
            if ("user".equals(item.path("role").asText(""))) {
                return item.path("content").asText("");
            }
        }
        return "";
    }

    private String fallbackReply(String question) {
        String contact = "客服时间 9:00-22:00，热线 400-800-1234，邮箱 support@kaya.app。";
        if (question.contains("未到账") || question.contains("没到账") || question.contains("不到账")) {
            return "转账通常实时到账。若超过 30 分钟仍未到账，请准备好转账时间与金额，联系人工客服核实。" + contact;
        }
        if (question.contains("额度")) {
            return "转账额度调整需要人工审核，暂不支持自助修改。请联系人工客服处理。" + contact;
        }
        if (question.contains("卡")) {
            return "银行卡可在「我的账户 - 银行卡」查看；卡片丢失请先冻结卡片并联系人工客服补办。" + contact;
        }
        return "AI 客服当前较忙，请稍后重试。如需立即处理，可联系人工客服：" + contact;
    }

    private String limitReply(int retryAfter, String question) {
        return fallbackReply(question)
                + "（免费 AI 通道每 45 秒限 1 次，约 " + retryAfter + " 秒后可继续提问）";
    }

    private ObjectNode buildPayload(JsonNode request) {
        JsonNode incoming = request == null ? null : request.path("messages");
        if (incoming == null || !incoming.isArray() || incoming.isEmpty()) {
            throw new BusinessException(40001, "请发送至少一条消息");
        }

        ObjectNode payload = objectMapper.createObjectNode();
        // 免费通道当前只暴露 openai-fast（gpt-oss-20b，带 reasoning，token 预算要留足）
        payload.put("model", "openai-fast");
        payload.put("private", true);
        payload.put("temperature", 0.3);
        payload.put("max_tokens", 900);
        ArrayNode messages = payload.putArray("messages");
        messages.addObject().put("role", "system").put("content", SYSTEM_PROMPT);

        int start = Math.max(0, incoming.size() - 10);
        for (int i = start; i < incoming.size(); i++) {
            JsonNode item = incoming.get(i);
            String role = item.path("role").asText("");
            String content = item.path("content").asText("").trim();
            if (!("user".equals(role) || "assistant".equals(role)) || content.isEmpty()) {
                continue;
            }
            if (content.length() > 1000) {
                content = content.substring(0, 1000);
            }
            messages.addObject().put("role", role).put("content", content);
        }
        return payload;
    }
}
