package com.example.studyplan.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.studyplan.common.Result;
import com.example.studyplan.common.SessionUtil;
import com.example.studyplan.entity.ImportantNode;
import com.example.studyplan.entity.ImportantNodeKeywordRule;
import com.example.studyplan.entity.User;
import com.example.studyplan.service.ImportantNodeService;
import com.example.studyplan.service.ImportantNodeKeywordRuleService;
import com.example.studyplan.service.StudyPlanService;
import com.example.studyplan.service.StudyTaskService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportantNodeControllerTest {

    @Test
    void addRejectsNodeWhenTypeIsNotFixedOption() {
        ImportantNodeService nodeService = mock(ImportantNodeService.class);
        ImportantNodeController controller = new ImportantNodeController(nodeService, mock(StudyPlanService.class), mock(StudyTaskService.class));
        ImportantNode node = validNode();
        node.setType("\u968f\u4fbf\u5199");

        Result<ImportantNode> result = controller.add(node, mockStudentSession());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("\u7c7b\u578b\u53ea\u80fd\u586b\u5199");
        verify(nodeService, never()).save(any(ImportantNode.class));
    }

    @Test
    void addCalculatesImportanceAndIgnoresManualImportance() {
        ImportantNodeService nodeService = mock(ImportantNodeService.class);
        ImportantNodeController controller = new ImportantNodeController(nodeService, mock(StudyPlanService.class), mock(StudyTaskService.class));
        ImportantNode node = validNode();
        node.setType("\u6bd4\u8d5b");
        node.setTitle("\u6821\u8d5b\u62a5\u540d");
        node.setDescription("\u6821\u5185\u666e\u901a\u6bd4\u8d5b");
        node.setStartDate(LocalDate.now().plusDays(30));
        node.setEndDate(LocalDate.now().plusDays(30));
        node.setImportance("\u7d27\u6025");

        Result<ImportantNode> result = controller.add(node, mockStudentSession());

        assertThat(result.isSuccess()).isTrue();
        assertThat(node.getImportance()).isEqualTo("\u666e\u901a");
        assertThat(node.getUserId()).isEqualTo(9L);
        verify(nodeService).save(node);
    }

    @Test
    void addAppliesCurrentUserKeywordScoreRulesWhenCalculatingImportance() {
        ImportantNodeService nodeService = mock(ImportantNodeService.class);
        ImportantNodeKeywordRuleService ruleService = mock(ImportantNodeKeywordRuleService.class);
        when(ruleService.listActiveRulesByUserId(9L)).thenReturn(List.of(scoreRule("\u56fd\u8d5b", "\u6bd4\u8d5b", 20)));
        ImportantNodeController controller = new ImportantNodeController(nodeService, ruleService, mock(StudyPlanService.class), mock(StudyTaskService.class));
        ImportantNode node = validNode();
        node.setType("\u6bd4\u8d5b");
        node.setTitle("\u521b\u65b0\u5927\u8d5b\u56fd\u8d5b");
        node.setDescription(null);
        node.setStartDate(LocalDate.now().plusDays(30));
        node.setEndDate(LocalDate.now().plusDays(30));

        Result<ImportantNode> result = controller.add(node, mockStudentSession());

        assertThat(result.isSuccess()).isTrue();
        assertThat(node.getImportance()).isEqualTo("\u91cd\u8981");
        verify(nodeService).save(node);
    }

    @Test
    void addAppliesUrgentKeywordRuleBeforeScoreThresholds() {
        ImportantNodeService nodeService = mock(ImportantNodeService.class);
        ImportantNodeKeywordRuleService ruleService = mock(ImportantNodeKeywordRuleService.class);
        when(ruleService.listActiveRulesByUserId(9L)).thenReturn(List.of(effectRule("XCPC", "\u6bd4\u8d5b", "URGENT")));
        ImportantNodeController controller = new ImportantNodeController(nodeService, ruleService, mock(StudyPlanService.class), mock(StudyTaskService.class));
        ImportantNode node = validNode();
        node.setType("\u6bd4\u8d5b");
        node.setTitle("XCPC \u6821\u961f\u9009\u62d4");
        node.setStartDate(LocalDate.now().plusDays(60));
        node.setEndDate(LocalDate.now().plusDays(60));

        Result<ImportantNode> result = controller.add(node, mockStudentSession());

        assertThat(result.isSuccess()).isTrue();
        assertThat(node.getImportance()).isEqualTo("\u7d27\u6025");
        verify(nodeService).save(node);
    }

    @Test
    void addDefaultsMissingEndDateToStartDateForSingleDayEvent() {
        ImportantNodeService nodeService = mock(ImportantNodeService.class);
        ImportantNodeController controller = new ImportantNodeController(nodeService, mock(StudyPlanService.class), mock(StudyTaskService.class));
        ImportantNode node = validNode();
        node.setEndDate(null);

        Result<ImportantNode> result = controller.add(node, mockStudentSession());

        assertThat(result.isSuccess()).isTrue();
        assertThat(node.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 13));
        assertThat(node.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 13));
        verify(nodeService).save(node);
    }

    @Test
    void addRejectsNodeWhenEndDateIsBeforeStartDate() {
        ImportantNodeService nodeService = mock(ImportantNodeService.class);
        ImportantNodeController controller = new ImportantNodeController(nodeService, mock(StudyPlanService.class), mock(StudyTaskService.class));
        ImportantNode node = validNode();
        node.setStartDate(LocalDate.of(2026, 6, 22));
        node.setEndDate(LocalDate.of(2026, 6, 20));

        Result<ImportantNode> result = controller.add(node, mockStudentSession());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("\u7ed3\u675f\u65e5\u671f\u4e0d\u80fd\u65e9\u4e8e\u5f00\u59cb\u65e5\u671f");
        verify(nodeService, never()).save(any(ImportantNode.class));
    }

    @Test
    void addRejectsLinkedPlanWhenPlanDoesNotBelongToCurrentUser() {
        ImportantNodeService nodeService = mock(ImportantNodeService.class);
        StudyPlanService planService = mock(StudyPlanService.class);
        when(planService.exists(any(Wrapper.class))).thenReturn(false);
        ImportantNodeController controller = new ImportantNodeController(nodeService, planService, mock(StudyTaskService.class));
        ImportantNode node = validNode();
        node.setPlanId(3L);

        Result<ImportantNode> result = controller.add(node, mockStudentSession());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("\u5173\u8054\u5b66\u4e60\u8ba1\u5212\u4e0d\u5b58\u5728");
        verify(nodeService, never()).save(any(ImportantNode.class));
    }

    private ImportantNode validNode() {
        ImportantNode node = new ImportantNode();
        node.setTitle("\u516d\u7ea7\u8003\u8bd5");
        node.setType("\u8003\u8bd5");
        node.setStartDate(LocalDate.of(2026, 6, 13));
        node.setEndDate(LocalDate.of(2026, 6, 13));
        node.setImportance("\u7d27\u6025");
        return node;
    }

    private HttpSession mockStudentSession() {
        HttpSession session = mock(HttpSession.class);
        User user = new User();
        user.setId(9L);
        when(session.getAttribute(SessionUtil.LOGIN_USER)).thenReturn(user);
        return session;
    }

    private ImportantNodeKeywordRule scoreRule(String keyword, String nodeType, int scoreDelta) {
        ImportantNodeKeywordRule rule = effectRule(keyword, nodeType, "SCORE");
        rule.setScoreDelta(scoreDelta);
        return rule;
    }

    private ImportantNodeKeywordRule effectRule(String keyword, String nodeType, String effect) {
        ImportantNodeKeywordRule rule = new ImportantNodeKeywordRule();
        rule.setUserId(9L);
        rule.setKeyword(keyword);
        rule.setNodeType(nodeType);
        rule.setEffect(effect);
        rule.setEnabled(true);
        return rule;
    }
}