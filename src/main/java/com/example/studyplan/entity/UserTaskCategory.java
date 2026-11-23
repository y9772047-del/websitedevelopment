package com.example.studyplan.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_task_category")
public class UserTaskCategory {
    @TableId
    private Long id;
    private Long userId;
    private String categoryName;
    private Integer sortOrder;
    private String description;
}
