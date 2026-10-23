package com.example.studyplan.service.impl;

import com.example.studyplan.entity.AiPlanReviewResponse;
import com.example.studyplan.entity.StudyPlan;
import com.example.studyplan.entity.StudyRecord;
import com.example.studyplan.entity.StudyTask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiPlanReviewParsingTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void preservesAllProblemsFromCapturedProviderArray() throws Exception {
        // Captured real provider content; this test replays it locally without an external call.
        byte[] raw;
        try (var stream = getClass().getResourceAsStream("/ai/plan-review-array-response.json")) {
            assertThat(stream).isNotNull();
            raw = stream.readAllBytes();
        }
        var input = new AtomicReference<String>();
        var result = review(raw, input);

        assertThat(result.getProblems())
                .contains("对个别概念的边界理解不够清晰")
                .contains("独立完成时检查速度偏慢")
                .contains("零散知识尚未整理成可复用清单")
                .contains("初次练习时步骤不够熟练")
                .doesNotContain("记录中未发现明确问题");
        assertThat(result.getRecordCount()).isEqualTo(2);
        assertThat(result.getActions()).hasSize(3);
        assertThat(input.get()).contains("对个别概念的边界理解不够清晰");
    }

    @Test
    void preservesExistingStringProblems() throws Exception {
        var result = review(envelope(json.readTree("\"连接条件容易遗漏，需要逐项核查\"")), new AtomicReference<>());
        assertThat(result.getProblems()).isEqualTo("连接条件容易遗漏，需要逐项核查");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "\"\"", "[\"\",\"  \"]"})
    void doesNotTurnMissingModelAnalysisIntoNoProblems(String problems) throws Exception {
        var result = review(envelope(json.readTree(problems)), new AtomicReference<>());
        assertThat(result.getProblems()).contains("核查").doesNotContain("未发现明确问题");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "42", "true", "[\"有效问题\",{\"text\":\"不能静默丢弃的问题\"}]"})
    void rejectsUnsupportedProblemTypesInsteadOfSilentlyLosingContent(String problems) throws Exception {
        byte[] response = envelope(json.readTree(problems));
        assertThatThrownBy(() -> review(response, new AtomicReference<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private byte[] envelope(JsonNode problems) throws Exception {
        var content = json.createObjectNode();
        content.put("summary", "基于学习记录的复盘");
        content.set("problems", problems);
        content.putArray("actions").add("练习后核查连接条件");
        return json.writeValueAsBytes(Map.of("choices", List.of(Map.of("message", Map.of("content", content.toString())))));
    }

    private AiPlanReviewResponse review(byte[] response, AtomicReference<String> input) throws Exception {
        HttpServer provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        provider.createContext("/review", exchange -> {
            input.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var stream = exchange.getResponseBody()) { stream.write(response); }
        });
        provider.start();
        try {
            AiServiceImpl service = new AiServiceImpl(null, json, null, null);
            ReflectionTestUtils.setField(service, "apiKey", "local-replay-only");
            ReflectionTestUtils.setField(service, "model", "local-replay");
            ReflectionTestUtils.setField(service, "apiUrl", "http://127.0.0.1:" + provider.getAddress().getPort() + "/review");
            StudyPlan plan = new StudyPlan(); plan.setId(16L); plan.setPlanName("SQL 与数据分析"); plan.setGoal("核对学习困难");
            StudyTask task = new StudyTask(); task.setId(30L); task.setTaskName("基础查询练习"); task.setStatus("进行中");
            StudyRecord first = new StudyRecord(); first.setTaskId(30L); first.setStudyDate(LocalDate.of(2026, 9, 4)); first.setProblems("对个别概念的边界理解不够清晰");
            StudyRecord second = new StudyRecord(); second.setTaskId(30L); second.setStudyDate(LocalDate.of(2026, 9, 5)); second.setProblems("独立完成时检查速度偏慢");
            return ReflectionTestUtils.invokeMethod(service, "callDeepSeekPlanReview", plan, List.of(task), List.of(first, second));
        } finally {
            provider.stop(0);
        }
    }
}
