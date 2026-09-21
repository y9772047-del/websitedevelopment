package com.example.studyplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.ImportantNode;
import com.example.studyplan.entity.ImportantNodeKeywordRule;
import com.example.studyplan.entity.StudyPlan;
import com.example.studyplan.entity.StudyTask;
import com.example.studyplan.service.ImportantNodeKeywordRuleService;
import com.example.studyplan.service.ImportantNodeService;
import com.example.studyplan.service.StudyPlanService;
import com.example.studyplan.service.StudyTaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/important-nodes")
public class ImportantNodeController {
    private static final Set<String> VALID_TYPES = Set.of("考试", "截止", "比赛", "汇报", "其他");
    private static final String NORMAL = "普通";
    private static final String IMPORTANT = "重要";
    private static final String URGENT = "紧急";

    private final ImportantNodeService importantNodeService;
    private final ImportantNodeKeywordRuleService keywordRuleService;
    private final StudyPlanService planService;
    private final StudyTaskService taskService;

    public ImportantNodeController(ImportantNodeService importantNodeService,
                                   StudyPlanService planService,
                                   StudyTaskService taskService) {
        this(importantNodeService, null, planService, taskService);
    }

    @Autowired
    public ImportantNodeController(ImportantNodeService importantNodeService,
                                   ImportantNodeKeywordRuleService keywordRuleService,
                                   StudyPlanService planService,
                                   StudyTaskService taskService) {
        this.importantNodeService = importantNodeService;
        this.keywordRuleService = keywordRuleService;
        this.planService = planService;
        this.taskService = taskService;
    }

    @GetMapping
    public Result<List<ImportantNode>> list(@RequestParam int year, HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        List<ImportantNode> nodes = importantNodeService.list(new LambdaQueryWrapper<ImportantNode>()
                .eq(ImportantNode::getUserId, userId)
                .le(ImportantNode::getStartDate, end)
                .ge(ImportantNode::getEndDate, start)
                .orderByAsc(ImportantNode::getStartDate)
                .orderByAsc(ImportantNode::getStartTime)
                .orderByDesc(ImportantNode::getCreateTime));
        List<ImportantNodeKeywordRule> rules = activeRules(userId);
        nodes.forEach(node -> node.setImportance(calculateImportance(node, rules)));
        return Result.ok(nodes);
    }

    @PostMapping
    public Result<ImportantNode> add(@RequestBody ImportantNode node, HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        String error = prepareNode(node, userId);
        if (error != null) {
            return Result.fail(error);
        }
        node.setId(null);
        importantNodeService.save(node);
        return Result.ok("保存重要节点成功", node);
    }

    @PostMapping("/batch")
    public Result<List<ImportantNode>> batchAdd(@RequestBody List<ImportantNode> nodes, HttpSession session) {
        if (nodes == null || nodes.isEmpty()) {
            return Result.fail("批量节点不能为空");
        }
        Long userId = SessionUtil.currentUserId(session);
        List<ImportantNode> preparedNodes = new ArrayList<>();
        for (ImportantNode node : nodes) {
            String error = prepareNode(node, userId);
            if (error != null) {
                return Result.fail(error);
            }
            node.setId(null);
            preparedNodes.add(node);
        }
        importantNodeService.saveBatch(preparedNodes);
        return Result.ok("导入重要节点成功", preparedNodes);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ImportantNode node, HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        String error = prepareNode(node, userId);
        if (error != null) {
            return Result.fail(error);
        }
        node.setId(id);
        boolean updated = importantNodeService.update(node, new LambdaQueryWrapper<ImportantNode>()
                .eq(ImportantNode::getId, id)
                .eq(ImportantNode::getUserId, userId));
        return updated ? Result.ok("修改重要节点成功", null) : Result.fail("修改重要节点失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        boolean removed = importantNodeService.remove(new LambdaQueryWrapper<ImportantNode>()
                .eq(ImportantNode::getId, id)
                .eq(ImportantNode::getUserId, SessionUtil.currentUserId(session)));
        return removed ? Result.ok("删除重要节点成功", null) : Result.fail("删除重要节点失败");
    }

    private String prepareNode(ImportantNode node, Long userId) {
        if (node == null) {
            return "重要节点数据不能为空";
        }
        if (node.getTitle() == null || node.getTitle().isBlank()) {
            return "节点标题不能为空";
        }
        if (node.getType() == null || node.getType().isBlank()) {
            return "类型不能为空";
        }
        if (!VALID_TYPES.contains(node.getType())) {
            return "类型只能填写：考试、截止、比赛、汇报、其他";
        }
        if (node.getStartDate() == null) {
            return "开始日期不能为空";
        }
        if (node.getEndDate() == null) {
            node.setEndDate(node.getStartDate());
        }
        if (node.getEndDate().isBefore(node.getStartDate())) {
            return "结束日期不能早于开始日期";
        }
        if (node.getPlanId() != null && !planBelongsToUser(node.getPlanId(), userId)) {
            return "关联学习计划不存在";
        }
        if (node.getTaskId() != null && !taskBelongsToUser(node.getTaskId(), userId)) {
            return "关联学习任务不存在";
        }
        node.setUserId(userId);
        node.setImportance(calculateImportance(node, activeRules(userId)));
        return null;
    }

    private String calculateImportance(ImportantNode node, List<ImportantNodeKeywordRule> rules) {
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), node.getStartDate());
        int score = switch (node.getType()) {
            case "考试", "截止" -> 55;
            case "比赛", "汇报" -> 45;
            default -> 30;
        };

        if (daysLeft >= 0 && daysLeft <= 1) {
            score += 35;
        } else if (daysLeft >= 0 && daysLeft <= 3) {
            score += 30;
        } else if (daysLeft >= 0 && daysLeft <= 7) {
            score += 20;
        } else if (daysLeft >= 0 && daysLeft <= 14) {
            score += 10;
        }

        int minimumLevel = 0;
        String searchableText = normalizeText(node.getTitle()) + " " + normalizeText(node.getDescription());
        for (ImportantNodeKeywordRule rule : rules) {
            if (!ruleMatches(rule, node.getType(), searchableText)) {
                continue;
            }
            switch (rule.getEffect()) {
                case "URGENT" -> minimumLevel = Math.max(minimumLevel, 2);
                case "MIN_IMPORTANT" -> minimumLevel = Math.max(minimumLevel, 1);
                case "SCORE" -> score += safeScoreDelta(rule.getScoreDelta());
                default -> {
                    // Ignore invalid historical rule values; rule controller prevents new ones.
                }
            }
        }

        String byScore = score >= 85 ? URGENT : score >= 60 ? IMPORTANT : NORMAL;
        if (minimumLevel == 2) {
            return URGENT;
        }
        if (minimumLevel == 1 && NORMAL.equals(byScore)) {
            return IMPORTANT;
        }
        return byScore;
    }

    private boolean ruleMatches(ImportantNodeKeywordRule rule, String nodeType, String searchableText) {
        if (rule == null || rule.getEnabled() == null || !rule.getEnabled()) {
            return false;
        }
        if (rule.getKeyword() == null || rule.getKeyword().isBlank()) {
            return false;
        }
        if (rule.getNodeType() != null && !rule.getNodeType().isBlank() && !rule.getNodeType().equals(nodeType)) {
            return false;
        }
        return searchableText.contains(normalizeText(rule.getKeyword()));
    }

    private int safeScoreDelta(Integer scoreDelta) {
        if (scoreDelta == null || scoreDelta < 0) {
            return 0;
        }
        return Math.min(scoreDelta, 50);
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.toUpperCase(Locale.ROOT);
    }

    private List<ImportantNodeKeywordRule> activeRules(Long userId) {
        if (keywordRuleService == null) {
            return List.of();
        }
        return keywordRuleService.listActiveRulesByUserId(userId);
    }

    private boolean planBelongsToUser(Long planId, Long userId) {
        return planService.exists(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getId, planId)
                .eq(StudyPlan::getUserId, userId));
    }

    private boolean taskBelongsToUser(Long taskId, Long userId) {
        return taskService.exists(new LambdaQueryWrapper<StudyTask>()
                .eq(StudyTask::getId, taskId)
                .eq(StudyTask::getUserId, userId));
    }
}