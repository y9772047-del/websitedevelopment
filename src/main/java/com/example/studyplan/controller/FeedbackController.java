package com.example.studyplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.Feedback;
import com.example.studyplan.service.FeedbackService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public Result<Feedback> submit(@RequestBody Feedback feedback, HttpSession session) {
        if (feedback == null || feedback.getContent() == null || feedback.getContent().isBlank()) {
            return Result.fail("反馈内容不能为空");
        }
        feedback.setId(null);
        feedback.setUserId(SessionUtil.currentUserId(session));
        feedback.setStatus("未处理");
        feedback.setReply(null);
        feedbackService.save(feedback);
        return Result.ok("反馈提交成功", feedback);
    }

    @GetMapping("/my")
    public Result<List<Feedback>> myFeedbacks(HttpSession session) {
        List<Feedback> feedbacks = feedbackService.list(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getUserId, SessionUtil.currentUserId(session))
                .orderByDesc(Feedback::getCreateTime));
        return Result.ok(feedbacks);
    }
}
