package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.studyplan.entity.AiImprovementRequest;
import com.example.studyplan.entity.AiImprovementResponse;
import com.example.studyplan.entity.AiWeeklyReportResponse;
import com.example.studyplan.entity.AiPlanReviewResponse;
import com.example.studyplan.entity.StudyPlan;
import com.example.studyplan.entity.StudyRecord;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.service.AiService;
import com.example.studyplan.service.StudyPlanService;
import com.example.studyplan.service.StudyRecordService;
import com.example.studyplan.service.StudyTaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {
    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    private static final String SOURCE_DEEPSEEK = "deepseek";
    private static final String SOURCE_DEEPSEEK_LABEL = "DeepSeek 生成";
    private static final String SOURCE_LOCAL = "local";
    private static final String SOURCE_LOCAL_LABEL = "本地规则生成";

    private final StudyPlanService planService;
    private final StudyRecordService recordService;
    private final StudyTaskService taskService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${ai.deepseek.enabled:true}")
    private boolean deepSeekEnabled;

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.api-url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    public AiServiceImpl(StudyPlanService planService, ObjectMapper objectMapper) {
        this(planService, objectMapper, null, null);
    }

    @Autowired
    public AiServiceImpl(StudyPlanService planService, ObjectMapper objectMapper, StudyRecordService recordService, StudyTaskService taskService) {
        this.planService = planService;
        this.recordService = recordService;
        this.taskService = taskService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public AiImprovementResponse generateImprovement(AiImprovementRequest request, Long userId) {
        StudyPlan plan = null;
        if (request.getPlanId() != null) {
            plan = planService.getOne(new LambdaQueryWrapper<StudyPlan>()
                    .eq(StudyPlan::getId, request.getPlanId())
                    .eq(StudyPlan::getUserId, userId));
            if (plan == null) {
                throw new RuntimeException("所属学习计划不存在");
            }
        }
        if (!deepSeekEnabled) {
            return fallbackImprovement(request, plan, "DeepSeek 已关闭");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackImprovement(request, plan, "DeepSeek 未配置 API Key，请设置 DEEPSEEK_API_KEY 后重启服务");
        }
        try {
            return callDeepSeek(request, plan);
        } catch (Exception exception) {
            log.warn("DeepSeek improvement generation failed, falling back to local rules: {}", exception.getMessage());
            return fallbackImprovement(request, plan, "DeepSeek 调用失败：" + exception.getMessage());
        }
    }


    @Override
    public AiWeeklyReportResponse generateWeeklyReport(Long userId) {
        if (!deepSeekEnabled) {
            throw new RuntimeException("DeepSeek 已关闭，无法生成 AI 周报");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("DeepSeek API Key 未配置，无法生成 AI 周报");
        }
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        List<StudyRecord> records = recordService == null ? List.of() : recordService.list(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, userId)
                .ge(StudyRecord::getStudyDate, startDate)
                .le(StudyRecord::getStudyDate, endDate)
                .orderByDesc(StudyRecord::getStudyDate));
        List<StudyTask> tasks = taskService == null ? List.of() : taskService.list(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getUserId, userId));
        try {
            return callDeepSeekWeeklyReport(records, tasks, startDate, endDate);
        } catch (Exception exception) {
            log.warn("DeepSeek weekly report generation failed: {}", exception.getMessage());
            throw new RuntimeException("AI 周报生成失败：" + exception.getMessage());
        }
    }

    @Override
    public AiPlanReviewResponse generatePlanReview(Long planId, Long userId) {
        StudyPlan plan = planService.getOne(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getId, planId).eq(StudyPlan::getUserId, userId));
        if (plan == null) throw new RuntimeException("计划不存在");
        List<StudyTask> tasks = taskService.list(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getPlanId, planId).eq(StudyTask::getUserId, userId));
        List<Long> taskIds = tasks.stream().map(StudyTask::getId).toList();
        List<StudyRecord> records = taskIds.isEmpty() ? List.of() : recordService.list(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, userId).in(StudyRecord::getTaskId, taskIds)
                .orderByDesc(StudyRecord::getStudyDate));
        if (records.size() < 2) throw new RuntimeException("至少 2 条学习记录才能生成目标复盘");
        if (!deepSeekEnabled) throw new RuntimeException("DeepSeek 已关闭，无法生成目标复盘");
        if (apiKey == null || apiKey.isBlank()) throw new RuntimeException("DeepSeek API Key 未配置，无法生成目标复盘");
        try {
            return callDeepSeekPlanReview(plan, tasks, records);
        } catch (Exception exception) {
            log.warn("DeepSeek plan review generation failed: {}", exception.getMessage());
            throw new RuntimeException("AI 目标复盘生成失败：" + exception.getMessage());
        }
    }

    private AiPlanReviewResponse callDeepSeekPlanReview(StudyPlan plan, List<StudyTask> tasks, List<StudyRecord> records) throws Exception {
        StringBuilder evidence = new StringBuilder();
        for (StudyRecord record : records.stream().limit(40).toList()) {
            String taskName = tasks.stream().filter(task -> task.getId().equals(record.getTaskId()))
                    .map(StudyTask::getTaskName).findFirst().orElse("未知任务");
            evidence.append("日期:").append(record.getStudyDate()).append(" 任务:").append(taskName)
                    .append(" 时长:").append(record.getDuration() == null ? 0 : record.getDuration())
                    .append(" 内容:").append(limitText(record.getContent(), 180))
                    .append(" 问题:").append(limitText(record.getProblems(), 180))
                    .append(" 改进:").append(limitText(record.getImprovementPlan(), 180)).append('\n');
        }
        long completed = tasks.stream().filter(task -> "已完成".equals(task.getStatus())).count();
        String prompt = "请根据证据做中文学习目标复盘，只输出严格 JSON：summary、problems、actions（3条具体行动的字符串数组）。"
                + "不得编造未记录的学习成果或问题。计划：" + limitText(plan.getPlanName(), 100)
                + "；目标：" + limitText(plan.getGoal(), 200) + "；任务完成：" + completed + "/" + tasks.size()
                + "；记录总数：" + records.size() + "；以下是最近最多40条记录：\n" + evidence;
        Map<String, Object> body = Map.of("model", model, "temperature", 0.35, "messages", List.of(
                Map.of("role", "system", "content", "你是大学生学习目标复盘助手，只依据提供的记录分析。"),
                Map.of("role", "user", "content", prompt)));
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new RuntimeException("HTTP " + response.statusCode());
        String content = objectMapper.readTree(response.body()).path("choices").path(0).path("message").path("content").asText();
        JsonNode json = objectMapper.readTree(extractJson(content));
        AiPlanReviewResponse result = new AiPlanReviewResponse();
        result.setSummary(cleanText(json.path("summary").asText(), "暂无有效总结"));
        result.setProblems(cleanText(json.path("problems").asText(), "记录中未发现明确问题"));
        List<String> actions = new ArrayList<>();
        if (json.path("actions").isArray()) json.path("actions").forEach(item -> {
            if (!item.asText().isBlank()) actions.add(item.asText().trim());
        });
        result.setActions(actions);
        result.setRecordCount(records.size());
        result.setStartDate(records.stream().map(StudyRecord::getStudyDate).filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(null));
        result.setEndDate(records.stream().map(StudyRecord::getStudyDate).filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(null));
        result.setGenerationSourceLabel(SOURCE_DEEPSEEK_LABEL);
        return result;
    }

    private String limitText(String value, int maxLength) {
        if (value == null) return "";
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private AiWeeklyReportResponse callDeepSeekWeeklyReport(List<StudyRecord> records, List<StudyTask> tasks, LocalDate startDate, LocalDate endDate) throws Exception {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.35,
                "messages", List.of(
                        Map.of("role", "system", "content", "你是大学生学习周报助手。只输出严格 JSON，不要输出 Markdown。"),
                        Map.of("role", "user", "content", buildWeeklyReportPrompt(records, tasks, startDate, endDate))
                )
        );
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode());
        }
        String content = objectMapper.readTree(response.body())
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();
        JsonNode json = objectMapper.readTree(extractJson(content));
        AiWeeklyReportResponse result = new AiWeeklyReportResponse();
        result.setWeeklySummary(cleanText(json.path("weeklySummary").asText(), ""));
        result.setMainProblems(cleanText(json.path("mainProblems").asText(), ""));
        result.setNextWeekSuggestions(cleanText(json.path("nextWeekSuggestions").asText(), ""));
        List<String> actions = new ArrayList<>();
        if (json.path("suggestedActions").isArray()) {
            json.path("suggestedActions").forEach(item -> {
                String action = item.asText();
                if (action != null && !action.isBlank()) {
                    actions.add(action.trim());
                }
            });
        }
        result.setSuggestedActions(actions);
        result.setGenerationSource(SOURCE_DEEPSEEK);
        result.setGenerationSourceLabel(SOURCE_DEEPSEEK_LABEL);
        result.setGenerationSourceDetail("DeepSeek 接口调用成功");
        return result;
    }

    private String buildWeeklyReportPrompt(List<StudyRecord> records, List<StudyTask> tasks, LocalDate startDate, LocalDate endDate) {
        long completedTasks = tasks.stream().filter(task -> "已完成".equals(task.getStatus())).count();
        long openTasks = tasks.stream().filter(task -> !"已完成".equals(task.getStatus())).count();
        int totalMinutes = records.stream().mapToInt(record -> record.getDuration() == null ? 0 : record.getDuration()).sum();
        StringBuilder recordText = new StringBuilder();
        for (StudyRecord record : records) {
            recordText.append("- 日期：").append(nullToEmpty(record.getStudyDate()))
                    .append("，任务ID：").append(nullToEmpty(record.getTaskId()))
                    .append("，时长：").append(record.getDuration() == null ? 0 : record.getDuration()).append("分钟")
                    .append("，内容：").append(nullToEmpty(record.getContent()))
                    .append("，完成情况：").append(nullToEmpty(record.getCompletionStatus()))
                    .append("，问题：").append(nullToEmpty(record.getProblems()))
                    .append("，改进：").append(nullToEmpty(record.getImprovementPlan()))
                    .append("\n");
        }
        return """
                请根据以下学习数据生成一份中文 AI 周报。
                输出严格 JSON，字段必须为 weeklySummary、mainProblems、nextWeekSuggestions、suggestedActions。
                suggestedActions 是 3 到 5 条字符串数组。
                周期：%s 至 %s
                本周学习记录数：%d
                本周学习总时长：%d 分钟
                当前已完成任务数：%d
                当前未完成任务数：%d
                学习记录：
                %s
                """.formatted(startDate, endDate, records.size(), totalMinutes, completedTasks, openTasks, recordText);
    }
    private AiImprovementResponse callDeepSeek(AiImprovementRequest request, StudyPlan plan) throws Exception {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.35,
                "messages", List.of(
                        Map.of("role", "system", "content", "你是大学生学习改进措施助手。只输出严格 JSON，不要输出 Markdown。"),
                        Map.of("role", "user", "content", buildPrompt(request, plan))
                )
        );
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode());
        }
        String content = objectMapper.readTree(response.body())
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText();
        JsonNode json = objectMapper.readTree(extractJson(content));
        AiImprovementResponse result = new AiImprovementResponse();
        result.setImprovementPlan(cleanText(json.path("improvementPlan").asText(), fallbackImprovement(request, plan, "").getImprovementPlan()));
        List<String> actions = new ArrayList<>();
        if (json.path("suggestedActions").isArray()) {
            json.path("suggestedActions").forEach(item -> {
                String action = item.asText();
                if (action != null && !action.isBlank()) {
                    actions.add(action.trim());
                }
            });
        }
        result.setSuggestedActions(actions.isEmpty() ? fallbackActions(request) : actions);
        result.setGenerationSource(SOURCE_DEEPSEEK);
        result.setGenerationSourceLabel(SOURCE_DEEPSEEK_LABEL);
        result.setGenerationSourceDetail("DeepSeek 接口调用成功");
        return result;
    }

    private String buildPrompt(AiImprovementRequest request, StudyPlan plan) {
        String planName = plan == null ? request.getPlanName() : plan.getPlanName();
        return """
                请根据学生的学习情况生成具体、可执行的学习改进措施。
                只生成改进措施，不要生成完整反思，不要改写问题总结。
                输出严格 JSON，字段固定为 improvementPlan 和 suggestedActions。
                suggestedActions 需要 3-5 条，每条是具体行动。
                improvementPlan 要具体、可执行，避免“继续努力、加强学习”等空话。
                必须贴合“学习内容”的具体材料、题型或任务对象。
                如果遇到的问题是“无、没有、暂无、无问题”等含义，不要编造薄弱点，也不要写“暴露的问题”，应改为给出巩固、复盘、迁移练习和下次安排建议。

                学习计划：%s
                学习任务：%s
                学习内容：%s
                完成情况：%s
                遇到的问题：%s
                问题总结：%s
                """.formatted(
                nullToEmpty(planName),
                nullToEmpty(request.getTaskName()),
                nullToEmpty(request.getStudyContent()),
                nullToEmpty(request.getCompletion()),
                nullToEmpty(request.getProblems()),
                nullToEmpty(request.getProblemSummary())
        );
    }

    private AiImprovementResponse fallbackImprovement(AiImprovementRequest request, StudyPlan plan, String reason) {
        String taskName = firstNotBlank(request.getTaskName(), "当前任务");
        String studyContent = firstNotBlank(request.getStudyContent(), taskName);
        String problem = firstMeaningfulProblem(request.getProblemSummary(), request.getProblems());
        AiImprovementResponse response = new AiImprovementResponse();
        if (problem.isBlank()) {
            response.setImprovementPlan(noProblemFallbackPlan(taskName, studyContent));
        } else {
            response.setImprovementPlan("针对“" + taskName + "”学习中记录的“" + problem + "”，后续不要只泛泛重学，而是回到本次材料“" + studyContent + "”定位具体卡点。建议先标出出错或不确定的步骤，再安排 15-20 分钟做同类型小练习；练习后把错误原因写成一句话，并在下一次学习开始前先复查这条原因。");
        }
        response.setSuggestedActions(fallbackActions(request));
        response.setGenerationSource(SOURCE_LOCAL);
        response.setGenerationSourceLabel(reason == null || reason.isBlank()
                ? SOURCE_LOCAL_LABEL
                : SOURCE_LOCAL_LABEL + "（" + reason + "）");
        response.setGenerationSourceDetail(reason);
        return response;
    }

    private List<String> fallbackActions(AiImprovementRequest request) {
        String taskName = firstNotBlank(request.getTaskName(), "当前学习任务");
        String studyContent = firstNotBlank(request.getStudyContent(), taskName);
        String problem = firstMeaningfulProblem(request.getProblemSummary(), request.getProblems());
        if (problem.isBlank() && isReadingTask(taskName, studyContent)) {
            return List.of(
                    "回看“" + studyContent + "”，整理每道题的定位句和答案依据",
                    "记录文章里的高频生词、长难句和选项干扰点",
                    "下次换一篇同类型阅读，限时完成后对比定位速度",
                    "把本篇可复用的做题顺序写成 3 步流程"
            );
        }
        if (problem.isBlank()) {
            return List.of(
                    "把“" + studyContent + "”的完成步骤整理成可复用清单",
                    "标记本次最顺利的一步和仍需加速的一步",
                    "下一次安排一个相邻材料或同类型任务做迁移练习",
                    "用 5 分钟复盘本次方法是否可以继续沿用"
            );
        }
        return List.of(
                "回到“" + studyContent + "”标出与“" + problem + "”相关的位置",
                "选择 2-3 个同类型小练习验证是否已经解决",
                "记录再次卡住的位置，并写下错误原因",
                "下一次学习优先处理这个问题，再推进新内容"
        );
    }

    private String noProblemFallbackPlan(String taskName, String studyContent) {
        if (isReadingTask(taskName, studyContent)) {
            return "本次已完成“" + taskName + "”中的“" + studyContent + "”，且没有记录明确问题。后续改进重点应放在巩固阅读方法：复盘每道题的定位句、选项干扰点和文章生词，把“会做”沉淀成稳定流程。下一次可以换一篇同类型阅读限时训练，完成后重点比较定位速度和错误选项排除理由，而不是重复刷已经会的题。";
        }
        return "本次已完成“" + taskName + "”中的“" + studyContent + "”，且没有记录明确问题。后续可以把本次顺利完成的步骤整理成清单，再安排一个相邻材料或同类型任务做迁移练习。下一次学习结束后，用 5 分钟检查完成速度、准确度和方法是否稳定。";
    }

    private boolean isReadingTask(String taskName, String studyContent) {
        String text = (nullToEmpty(taskName) + " " + nullToEmpty(studyContent)).toLowerCase();
        return text.contains("阅读") || text.contains("reading");
    }

    private String firstMeaningfulProblem(String... values) {
        for (String value : values) {
            if (value != null && !isNoProblemText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isNoProblemText(String value) {
        String text = value == null ? "" : value.trim();
        if (text.isBlank()) return true;
        String normalized = text.replaceAll("[。.!！\\s]", "");
        return List.of("无", "没有", "暂无", "无问题", "没有问题", "暂无问题", "没问题", "无明显问题").contains(normalized);
    }

    private String extractJson(String content) {
        String text = content == null ? "" : content.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```json", "").replaceFirst("^```", "").replaceFirst("```$", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String cleanText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
