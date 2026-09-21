package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyplan.entity.ImportantNodeKeywordRule;
import com.example.studyplan.mapper.ImportantNodeKeywordRuleMapper;
import com.example.studyplan.service.ImportantNodeKeywordRuleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImportantNodeKeywordRuleServiceImpl extends ServiceImpl<ImportantNodeKeywordRuleMapper, ImportantNodeKeywordRule> implements ImportantNodeKeywordRuleService {
    @Override
    public List<ImportantNodeKeywordRule> listActiveRulesByUserId(Long userId) {
        return list(new LambdaQueryWrapper<ImportantNodeKeywordRule>()
                .eq(ImportantNodeKeywordRule::getUserId, userId)
                .eq(ImportantNodeKeywordRule::getEnabled, true)
                .orderByDesc(ImportantNodeKeywordRule::getUpdateTime)
                .orderByDesc(ImportantNodeKeywordRule::getCreateTime));
    }
}