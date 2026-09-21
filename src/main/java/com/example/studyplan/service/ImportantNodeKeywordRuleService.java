package com.example.studyplan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.studyplan.entity.ImportantNodeKeywordRule;

import java.util.List;

public interface ImportantNodeKeywordRuleService extends IService<ImportantNodeKeywordRule> {
    List<ImportantNodeKeywordRule> listActiveRulesByUserId(Long userId);
}