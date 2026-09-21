package com.example.studyplan.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("study_task_category")
public class StudyTaskCategory {
    @TableId
    private String categoryName;
    private Integer sortOrder;
    private String description;
}
