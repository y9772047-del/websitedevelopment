package com.example.studyplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.studyplan.entity.StudyPlan;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface StudyPlanMapper extends BaseMapper<StudyPlan> {
    @Update("""
            UPDATE study_plan
            SET total_study_minutes = GREATEST(COALESCE(total_study_minutes, 0) + #{minutesDelta}, 0),
                total_study_record_count = GREATEST(COALESCE(total_study_record_count, 0) + #{recordCountDelta}, 0)
            WHERE id = #{planId}
            """)
    int incrementInvestment(@Param("planId") Long planId,
                            @Param("minutesDelta") Integer minutesDelta,
                            @Param("recordCountDelta") Integer recordCountDelta);
}
