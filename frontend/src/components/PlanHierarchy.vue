<script setup>
import { computed, ref, watch } from 'vue';
import { buildPlanTree } from '../planTree.js';

const props = defineProps({ plan: { type: Object, required: true }, tasks: { type: Array, required: true }, records: { type: Array, required: true } });
const tree = computed(() => buildPlanTree([props.plan], props.tasks, props.records)[0]);
const expandedTaskIds = ref(new Set());

watch(() => props.plan.id, () => { expandedTaskIds.value = new Set(); });

function toggleTask(id) {
  const next = new Set(expandedTaskIds.value);
  if (next.has(id)) next.delete(id);
  else next.add(id);
  expandedTaskIds.value = next;
}
</script>

<template>
  <div class="plan-tree-map" role="group" :aria-label="`${tree.planName}的学习目标树`">
    <div class="plan-tree-lanes" aria-hidden="true"><span>目标</span><span>→</span><span>任务</span><span>→</span><span>学习记录</span></div>
    <div class="plan-tree">
      <div class="plan-tree-root">
        <span class="plan-tree-kicker">核心目标</span>
        <div class="plan-tree-root-mark">◎</div>
        <h3>{{ tree.goal || tree.planName }}</h3>
        <p>{{ tree.planName }}</p>
        <div class="plan-tree-root-footer"><span>{{ tree.tasks.length }} 项任务</span><span>{{ tree.recordCount }} 条记录</span></div>
      </div>
      <div class="plan-tree-tasks">
        <div v-if="!tree.tasks.length" class="plan-tree-node plan-tree-empty">还没有任务，从目标出发添加第一项任务。</div>
        <div v-for="(task, index) in tree.tasks" :key="task.id" class="plan-tree-row" :class="{ 'is-expanded': expandedTaskIds.has(task.id) }">
          <button type="button" class="plan-tree-node plan-tree-task" :class="{ 'is-complete': task.status === '已完成' }" :aria-expanded="expandedTaskIds.has(task.id)" :aria-controls="`plan-tree-records-${task.id}`" @click="toggleTask(task.id)">
            <span class="plan-tree-task-top"><span class="plan-tree-index">{{ String(index + 1).padStart(2, '0') }}</span><span class="plan-tree-state">{{ task.status || '未设置状态' }}</span></span>
            <span class="plan-tree-task-title">{{ task.taskName }}</span>
            <span class="plan-tree-task-footer"><span>{{ task.category || '未分类' }}</span><span>{{ task.deadline || '未设截止日期' }}</span></span>
            <span class="plan-tree-task-action">{{ task.records.length }} 条记录 <span aria-hidden="true">{{ expandedTaskIds.has(task.id) ? '收起 ←' : '展开 →' }}</span></span>
          </button>
          <div v-show="expandedTaskIds.has(task.id)" :id="`plan-tree-records-${task.id}`" class="plan-tree-leaves">
            <div class="plan-tree-leaves-heading">学习记录 · {{ task.records.length }}</div>
            <div v-if="!task.records.length" class="plan-tree-node plan-tree-empty">暂无学习记录</div>
            <details v-for="record in task.records" :key="record.id" class="plan-tree-node plan-tree-record" :class="{ 'is-complete': record.completionStatus === '已完成', 'is-missed': record.completionStatus === '未完成' }">
              <summary><span class="plan-tree-record-top"><time :datetime="record.studyDate || undefined">{{ record.studyDate || '未设置日期' }}</time><span>{{ record.duration || 0 }} 分钟</span></span><span class="plan-tree-record-content">{{ record.content || '未填写学习内容' }}</span><span class="plan-tree-record-bottom"><span>{{ record.completionStatus || '未设置完成情况' }}</span><span class="plan-tree-record-action"><span class="plan-tree-open-label">展开全文 ↓</span><span class="plan-tree-close-label">收起 ↑</span></span></span></summary>
            </details>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
