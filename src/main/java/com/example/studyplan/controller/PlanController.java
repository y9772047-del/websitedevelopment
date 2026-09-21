package com.example.studyplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.StudyPlan;
import com.example.studyplan.service.StudyPlanService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {
    private final StudyPlanService planService;

    public PlanController(StudyPlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    public Result<StudyPlan> add(@RequestBody StudyPlan plan, HttpSession session) {
        String error = validatePlan(plan);
        if (error != null) {
            return Result.fail(error);
        }
        plan.setId(null);
        plan.setUserId(SessionUtil.currentUserId(session));
        planService.save(plan);
        return Result.ok("保存计划成功", plan);
    }

    @GetMapping
    public Result<List<StudyPlan>> list(HttpSession session) {
        List<StudyPlan> plans = planService.list(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getUserId, SessionUtil.currentUserId(session))
                .orderByDesc(StudyPlan::getCreateTime));
        return Result.ok(plans);
    }

    @GetMapping("/{id}")
    public Result<StudyPlan> detail(@PathVariable Long id, HttpSession session) {
        StudyPlan plan = planService.getOne(new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getId, id)
                .eq(StudyPlan::getUserId, SessionUtil.currentUserId(session)));
        return plan == null ? Result.fail("计划不存在") : Result.ok(plan);
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody StudyPlan plan, HttpSession session) {
        String error = validatePlan(plan);
        if (error != null) {
            return Result.fail(error);
        }
        Long userId = SessionUtil.currentUserId(session);
        plan.setId(id);
        plan.setUserId(userId);
        boolean updated = planService.update(plan, new LambdaQueryWrapper<StudyPlan>()
                .eq(StudyPlan::getId, id)
                .eq(StudyPlan::getUserId, userId));
        return updated ? Result.ok("修改计划成功", null) : Result.fail("修改计划失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        boolean removed = planService.removePlanWithTasksAndRecords(id, SessionUtil.currentUserId(session));
        return removed ? Result.ok("\u5220\u9664\u8ba1\u5212\u6210\u529f", null) : Result.fail("\u5220\u9664\u8ba1\u5212\u5931\u8d25");
    }

    private String validatePlan(StudyPlan plan) {
        if (plan == null) {
            return "计划数据不能为空";
        }
        if (plan.getPlanName() == null || plan.getPlanName().isBlank()) {
            return "计划名称不能为空";
        }
        return null;
    }
}
