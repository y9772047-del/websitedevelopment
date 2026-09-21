package com.example.studyplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.ImportantNodeKeywordRule;
import com.example.studyplan.service.ImportantNodeKeywordRuleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/important-node-keyword-rules")
public class ImportantNodeKeywordRuleController {
    private static final Set<String> VALID_TYPES = Set.of("考试", "截止", "比赛", "汇报", "其他");
    private static final Set<String> VALID_EFFECTS = Set.of("SCORE", "MIN_IMPORTANT", "URGENT");

    private final ImportantNodeKeywordRuleService ruleService;

    public ImportantNodeKeywordRuleController(ImportantNodeKeywordRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public Result<List<ImportantNodeKeywordRule>> list(HttpSession session) {
        List<ImportantNodeKeywordRule> rules = ruleService.list(new LambdaQueryWrapper<ImportantNodeKeywordRule>()
                .eq(ImportantNodeKeywordRule::getUserId, SessionUtil.currentUserId(session))
                .orderByDesc(ImportantNodeKeywordRule::getEnabled)
                .orderByDesc(ImportantNodeKeywordRule::getUpdateTime)
                .orderByDesc(ImportantNodeKeywordRule::getCreateTime));
        return Result.ok(rules);
    }

    @PostMapping
    public Result<ImportantNodeKeywordRule> add(@RequestBody ImportantNodeKeywordRule rule, HttpSession session) {
        String error = prepareRule(rule, SessionUtil.currentUserId(session));
        if (error != null) {
            return Result.fail(error);
        }
        rule.setId(null);
        ruleService.save(rule);
        return Result.ok("保存关键词规则成功", rule);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ImportantNodeKeywordRule rule, HttpSession session) {
        Long userId = SessionUtil.currentUserId(session);
        String error = prepareRule(rule, userId);
        if (error != null) {
            return Result.fail(error);
        }
        rule.setId(id);
        boolean updated = ruleService.update(rule, new LambdaQueryWrapper<ImportantNodeKeywordRule>()
                .eq(ImportantNodeKeywordRule::getId, id)
                .eq(ImportantNodeKeywordRule::getUserId, userId));
        return updated ? Result.ok("修改关键词规则成功", null) : Result.fail("修改关键词规则失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        boolean removed = ruleService.remove(new LambdaQueryWrapper<ImportantNodeKeywordRule>()
                .eq(ImportantNodeKeywordRule::getId, id)
                .eq(ImportantNodeKeywordRule::getUserId, SessionUtil.currentUserId(session)));
        return removed ? Result.ok("删除关键词规则成功", null) : Result.fail("删除关键词规则失败");
    }

    private String prepareRule(ImportantNodeKeywordRule rule, Long userId) {
        if (rule == null) {
            return "关键词规则不能为空";
        }
        if (rule.getKeyword() == null || rule.getKeyword().isBlank()) {
            return "关键词不能为空";
        }
        rule.setKeyword(rule.getKeyword().trim());
        if (rule.getKeyword().length() > 60) {
            return "关键词不能超过 60 个字符";
        }
        if (rule.getNodeType() != null && rule.getNodeType().isBlank()) {
            rule.setNodeType(null);
        }
        if (rule.getNodeType() != null && !VALID_TYPES.contains(rule.getNodeType())) {
            return "适用类型只能填写：考试、截止、比赛、汇报、其他";
        }
        if (rule.getEffect() == null || rule.getEffect().isBlank()) {
            rule.setEffect("SCORE");
        }
        if (!VALID_EFFECTS.contains(rule.getEffect())) {
            return "影响方式只能填写：SCORE、MIN_IMPORTANT、URGENT";
        }
        if ("SCORE".equals(rule.getEffect())) {
            if (rule.getScoreDelta() == null) {
                rule.setScoreDelta(10);
            }
            if (rule.getScoreDelta() < 1 || rule.getScoreDelta() > 50) {
                return "加分值必须在 1 到 50 之间";
            }
        } else {
            rule.setScoreDelta(0);
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(true);
        }
        rule.setUserId(userId);
        return null;
    }
}