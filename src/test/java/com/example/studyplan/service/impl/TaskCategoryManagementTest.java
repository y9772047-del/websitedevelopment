package com.example.studyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.studyplan.entity.UserTaskCategory;
import com.example.studyplan.mapper.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaskCategoryManagementTest {
    private final UserTaskCategoryMapper categories = mock(UserTaskCategoryMapper.class);
    private final StudyTaskMapper tasks = mock(StudyTaskMapper.class);
    private final StudyRecordMapper records = mock(StudyRecordMapper.class);
    private final StudyPlanMapper plans = mock(StudyPlanMapper.class);
    private final StudyTaskServiceImpl service = new StudyTaskServiceImpl(mock(StudyTaskCategoryMapper.class), records, plans, categories);

    @BeforeEach
    void setup() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), UserTaskCategory.class);
        ReflectionTestUtils.setField(service, "baseMapper", tasks);
    }

    private UserTaskCategory category(long id, String name) {
        UserTaskCategory category = new UserTaskCategory();
        category.setId(id); category.setUserId(9L); category.setCategoryName(name);
        return category;
    }

    @Test
    void creationKeepsOwnershipDefaultsAndGeneratedId() {
        UserTaskCategory input = category(999, "  阅读  ");
        input.setUserId(999L);
        when(categories.insert(any(UserTaskCategory.class))).thenAnswer(call -> {
            UserTaskCategory saved = call.getArgument(0);
            assertThat(saved.getId()).isNull();
            saved.setId(30L); return 1;
        });
        assertThat(service.createCategory(input, 9L)).isNull();
        assertThat(input.getCategoryName()).isEqualTo("阅读");
        assertThat(input.getUserId()).isEqualTo(9L);
        assertThat(input.getSortOrder()).isEqualTo(100);
        assertThat(input.getId()).isEqualTo(30L);
        verifyNoInteractions(tasks, records, plans);
    }

    @Test
    void creationRejectsInvalidNamesAndDuplicatesWithoutWriting() {
        assertThat(service.createCategory(null, 9L)).isNotNull();
        assertThat(service.createCategory(category(1, " "), 9L)).isNotNull();
        assertThat(service.createCategory(category(1, "字".repeat(51)), 9L)).isNotNull();
        // Preserve the existing creation contract: the raw input length is checked first.
        assertThat(service.createCategory(category(1, " " + "字".repeat(49) + " "), 9L)).isNotNull();
        when(categories.selectCount(any())).thenReturn(1L);
        assertThat(service.createCategory(category(1, "阅读"), 9L)).contains("已存在");
        verify(categories, never()).insert(any(UserTaskCategory.class));
    }

    @Test
    void taskCategoryValidationLocksTheOwnedCategoryUntilTheSaveTransactionEnds() {
        when(categories.selectOne(any())).thenReturn(category(1, "阅读"));
        assertThat(service.isValidCategory("阅读", 9L)).isTrue();
        var capture = ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(categories).selectOne(capture.capture());
        assertThat(capture.getValue().getSqlSegment()).contains("user_id", "category_name", "FOR UPDATE");
        assertThat(capture.getValue().getParamNameValuePairs().values()).contains(9L, "阅读");
    }

    @Test
    void renameUpdatesOnlyTheOwnersTasksWithTheOldCategoryName() {
        when(categories.selectOne(any())).thenReturn(category(1, "阅读"));
        when(categories.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(tasks.update(isNull(), any(Wrapper.class))).thenReturn(2);
        assertThat(service.renameCategory(1L, "  读书  ", 9L)).isNull();
        var capture = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(tasks).update(isNull(), capture.capture());
        var query = capture.getValue();
        assertThat(query.getSqlSet()).contains("category");
        assertThat(query.getSqlSegment()).contains("user_id", "category");
        assertThat(query.getParamNameValuePairs().values()).contains(9L, "阅读", "读书");
        verifyNoInteractions(records, plans);
    }

    @Test
    void renameRejectsAnotherUsersCategoryOrDuplicateNamesBeforeWriting() {
        assertThat(service.renameCategory(1L, "读书", 9L)).isNotNull();
        verify(categories, never()).update(any(), any(Wrapper.class));
        when(categories.selectOne(any())).thenReturn(category(1, "阅读"));
        when(categories.selectCount(any())).thenReturn(1L);
        assertThat(service.renameCategory(1L, "工作", 9L)).contains("已存在");
        verifyNoInteractions(tasks, records, plans);
    }

    @Test
    void deletingUsedCategoryRequiresAnOwnedDifferentDestination() {
        when(categories.selectOne(any())).thenReturn(category(1, "阅读"));
        when(tasks.selectCount(any())).thenReturn(2L);
        assertThat(service.deleteCategory(1L, null, 9L)).contains("转入");
        assertThat(service.deleteCategory(1L, 1L, 9L)).isNotNull();
        when(categories.selectOne(any())).thenReturn(category(1, "阅读"), null);
        assertThat(service.deleteCategory(1L, 8L, 9L)).isNotNull();
        verify(categories, never()).delete(any(Wrapper.class));
        verify(tasks, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void deletionMovesTasksBeforeRemovingTheCategoryWithoutTouchingRecords() {
        when(categories.selectOne(any())).thenReturn(category(1, "阅读"), category(2, "生活"));
        when(tasks.selectCount(any())).thenReturn(2L);
        when(tasks.update(isNull(), any(Wrapper.class))).thenReturn(2);
        when(categories.delete(any(Wrapper.class))).thenReturn(1);
        assertThat(service.deleteCategory(1L, 2L, 9L)).isNull();
        var order = inOrder(tasks, categories);
        var capture = ArgumentCaptor.forClass(UpdateWrapper.class);
        order.verify(tasks).update(isNull(), capture.capture());
        order.verify(categories).delete(any(Wrapper.class));
        assertThat(capture.getValue().getSqlSegment()).contains("user_id", "category");
        assertThat(capture.getValue().getParamNameValuePairs().values()).contains(9L, "阅读", "生活");
        verifyNoInteractions(records, plans);
    }

    @Test
    void unusedCategoryCanBeDeletedWithoutMovingAnyTasks() {
        when(categories.selectOne(any())).thenReturn(category(1, "阅读"));
        when(categories.delete(any(Wrapper.class))).thenReturn(1);
        assertThat(service.deleteCategory(1L, null, 9L)).isNull();
        verify(tasks, never()).update(any(), any(Wrapper.class));
        verifyNoInteractions(records, plans);
    }

    @Test
    void failedDeleteAfterMigrationRollsBackTheTaskChanges() {
        when(categories.selectOne(any())).thenReturn(category(1, "阅读"), category(2, "生活"));
        when(tasks.selectCount(any())).thenReturn(2L);
        var moved = new java.util.concurrent.atomic.AtomicBoolean();
        when(tasks.update(isNull(), any(Wrapper.class))).thenAnswer(call -> { moved.set(true); return 2; });
        var manager = new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object tx, org.springframework.transaction.TransactionDefinition def) {}
            protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status) {}
            protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status) { moved.set(false); }
        };
        var factory = new org.springframework.aop.framework.ProxyFactory(service);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(manager,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        var proxy = (StudyTaskServiceImpl) factory.getProxy();
        assertThatThrownBy(() -> proxy.deleteCategory(1L, 2L, 9L)).isInstanceOf(IllegalStateException.class);
        assertThat(moved).isFalse();
    }
}
