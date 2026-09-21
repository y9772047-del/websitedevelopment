package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.studyplan.entity.ImportantNode;
import com.example.studyplan.mapper.ImportantNodeMapper;
import com.example.studyplan.service.ImportantNodeService;
import org.springframework.stereotype.Service;

@Service
public class ImportantNodeServiceImpl extends ServiceImpl<ImportantNodeMapper, ImportantNode> implements ImportantNodeService {
}
