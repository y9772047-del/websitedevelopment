package com.example.studyplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.studyplan.entity.UserTaskCategory;
import com.example.studyplan.dto.TaskCategorySummary;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface UserTaskCategoryMapper extends BaseMapper<UserTaskCategory> {
    @Select("""
            SELECT c.id, c.category_name, COUNT(t.id) AS task_count
            FROM user_task_category c
            LEFT JOIN study_task t ON t.user_id = c.user_id AND t.category = c.category_name
            WHERE c.user_id = #{userId}
            GROUP BY c.id, c.category_name, c.sort_order
            ORDER BY c.sort_order, c.id
            """)
    List<TaskCategorySummary> listWithTaskCounts(@Param("userId") Long userId);
}
