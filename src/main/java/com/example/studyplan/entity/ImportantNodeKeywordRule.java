package com.example.studyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("important_node_keyword_rule")
public class ImportantNodeKeywordRule {
    private Long id;
    private Long userId;
    private String keyword;
    private String nodeType;
    private String effect;
    private Integer scoreDelta;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}