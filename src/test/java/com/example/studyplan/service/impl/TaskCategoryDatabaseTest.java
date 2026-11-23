package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.example.studyplan.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.Statement;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

// Explicit local verification only; all test rows are rolled back.
@EnabledIfEnvironmentVariable(named = "RUN_TASK3_DB_TEST", matches = "true")
class TaskCategoryDatabaseTest {
    private long insert(JdbcTemplate jdbc, String sql, Object... args) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int index = 0; index < args.length; index++) statement.setObject(index + 1, args[index]);
            return statement;
        }, key);
        return key.getKey().longValue();
    }

    private Map<String, String> snapshot(JdbcTemplate jdbc) {
        var result = new LinkedHashMap<String, String>();
        for (String table : List.of("study_plan", "study_task", "study_record", "study_task_category", "user_task_category")) {
            String order = table.equals("study_task_category") ? "category_name" : "id";
            result.put(table, jdbc.queryForList("SELECT * FROM " + table + " ORDER BY " + order).toString());
        }
        return result;
    }

    @Test
    void realDatabaseRenameAndDeletePreserveRecordsAndRollbackAllTestRows() throws Exception {
        var source = new DriverManagerDataSource("jdbc:mysql://localhost:3306/study_plan_system?serverTimezone=Asia/Shanghai&characterEncoding=UTF-8", "root", System.getenv("DB_PASSWORD"));
        var jdbc = new JdbcTemplate(source);
        var before = snapshot(jdbc);
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils.setGlobalConfig(configuration,
                new com.baomidou.mybatisplus.core.config.GlobalConfig().setDbConfig(
                        new com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig().setIdType(com.baomidou.mybatisplus.annotation.IdType.AUTO)));
        for (Class<?> mapper : List.of(UserTaskCategoryMapper.class, StudyTaskMapper.class, StudyTaskCategoryMapper.class, StudyRecordMapper.class, StudyPlanMapper.class)) configuration.addMapper(mapper);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(source); factory.setConfiguration(configuration);
        var session = new SqlSessionTemplate(factory.getObject());
        var categoryMapper = session.getMapper(UserTaskCategoryMapper.class);
        var taskMapper = session.getMapper(StudyTaskMapper.class);
        var service = new StudyTaskServiceImpl(session.getMapper(StudyTaskCategoryMapper.class), session.getMapper(StudyRecordMapper.class), session.getMapper(StudyPlanMapper.class), categoryMapper);
        ReflectionTestUtils.setField(service, "baseMapper", taskMapper);
        var transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        transaction.execute(status -> {
            status.setRollbackOnly();
            long userId = jdbc.queryForObject("SELECT MIN(id) FROM user", Long.class);
            var existing = categoryMapper.listWithTaskCounts(userId).get(0);
            assertThat(service.isValidCategory(existing.getCategoryName(), userId)).isTrue();
            // A second transaction cannot rename/delete a category between validation and task save.
            assertThatThrownBy(() -> {
                try (var contender = source.getConnection(); var statement = contender.createStatement()) {
                    statement.executeQuery("SELECT id FROM user_task_category WHERE id=" + existing.getId() + " FOR UPDATE NOWAIT");
                }
            }).isInstanceOfSatisfying(java.sql.SQLException.class, failure -> assertThat(failure.getErrorCode()).isEqualTo(3572));
            String name = "task3_" + UUID.randomUUID().toString().substring(0, 12);
            var created = new com.example.studyplan.entity.UserTaskCategory();
            created.setCategoryName(name); created.setUserId(Long.MAX_VALUE);
            assertThat(service.createCategory(created, userId)).isNull();
            assertThat(created.getUserId()).isEqualTo(userId);
            long first = created.getId();
            long second = insert(jdbc, "INSERT INTO user_task_category(user_id,category_name,sort_order) VALUES(?,?,100)", userId, name + "_target");
            long task = insert(jdbc, "INSERT INTO study_task(user_id,task_name,category) VALUES(?,?,?)", userId, "task3 verification", name);
            long record = insert(jdbc, "INSERT INTO study_record(user_id,task_id,study_date,duration,content) VALUES(?,?,CURRENT_DATE(),7,?)", userId, task, "task3 verification");
            session.clearCache(); // Test fixtures use raw JDBC, bypassing MyBatis cache invalidation.
            assertThat(service.listMyCategories(userId).stream().filter(item -> item.getId() == first).findFirst().orElseThrow().getTaskCount()).isEqualTo(1);
            assertThat(service.renameCategory(first, name + "_renamed", Long.MAX_VALUE)).isNotNull();
            assertThat(service.renameCategory(first, name + "_renamed", userId)).isNull();
            assertThat(taskMapper.selectById(task).getCategory()).isEqualTo(name + "_renamed");
            assertThat(service.renameCategory(first, name + "_target", userId)).contains("已存在");
            assertThat(service.deleteCategory(first, null, userId)).contains("转入");
            assertThat(service.deleteCategory(first, second, userId)).isNull();
            assertThat(taskMapper.selectById(task).getCategory()).isEqualTo(name + "_target");
            assertThat(categoryMapper.selectById(first)).isNull();
            assertThat(jdbc.queryForObject("SELECT task_id FROM study_record WHERE id=?", Long.class, record)).isEqualTo(task);
            assertThat(jdbc.queryForObject("SELECT duration FROM study_record WHERE id=?", Integer.class, record)).isEqualTo(7);
            return null;
        });
        assertThat(snapshot(jdbc).equals(before)).as("all existing rows remain identical after rollback").isTrue();
    }
}
