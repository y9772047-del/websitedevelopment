package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyplan.entity.Feedback;
import com.example.studyplan.mapper.FeedbackMapper;
import com.example.studyplan.service.FeedbackService;
import org.springframework.stereotype.Service;

@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {
}
