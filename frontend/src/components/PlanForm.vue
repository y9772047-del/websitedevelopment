<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { emptyPlan, planStatuses, preparePlan } from '../plan.js';
const props = defineProps({ plan: { type: Object, default: null }, busy: Boolean, formId: { type: String, default: 'planForm' } });
const emit = defineEmits(['save', 'cancel']);
const form = reactive(emptyPlan());
const error = ref('');
const editing = computed(() => props.plan !== null);
const statuses = computed(() => planStatuses(props.plan));
function reset() {
  const defaults = emptyPlan();
  for (const key of Object.keys(defaults)) form[key] = props.plan?.[key] ?? defaults[key];
  error.value = '';
}
watch(() => props.plan, reset, { immediate: true });
function submit() {
  if (props.busy) return;
  error.value = '';
  try { emit('save', preparePlan(form, props.plan)); }
  catch (failure) { error.value = failure.message; }
}
</script>

<template>
  <form :id="formId" @submit.prevent="submit">
    <p v-if="error" class="alert alert-danger" role="alert">{{ error }}</p>
    <fieldset class="row g-3" :disabled="busy">
      <div class="col-md-4"><label class="form-label" :for="`${formId}-name`">计划名称</label><input :id="`${formId}-name`" v-model="form.planName" class="form-control" name="planName" required maxlength="100"></div>
      <div class="col-md-4"><label class="form-label" :for="`${formId}-goal`">学习目标</label><input :id="`${formId}-goal`" v-model="form.goal" class="form-control" name="goal" maxlength="255"></div>
      <div class="col-md-2"><label class="form-label" :for="`${formId}-start`">开始日期</label><input :id="`${formId}-start`" v-model="form.startDate" class="form-control" name="startDate" type="date"></div>
      <div class="col-md-2"><label class="form-label" :for="`${formId}-end`">结束日期</label><input :id="`${formId}-end`" v-model="form.endDate" class="form-control" name="endDate" type="date" :min="form.startDate || undefined"></div>
      <div class="col-md-3"><label class="form-label" :for="`${formId}-status`">状态</label><select :id="`${formId}-status`" v-model="form.status" class="form-select" name="status" required><option v-for="status in statuses" :key="status" :value="status">{{ status }}</option></select></div>
      <div class="col-md-9"><label class="form-label" :for="`${formId}-description`">计划描述</label><input :id="`${formId}-description`" v-model="form.description" class="form-control" name="description" maxlength="10000"></div>
      <div class="col-12"><button class="btn btn-primary me-2" type="submit">{{ busy ? '保存中…' : editing ? '保存修改' : '保存计划' }}</button><button class="btn btn-outline-primary" type="button" @click="editing ? emit('cancel') : reset()">{{ editing ? '取消' : '清空' }}</button></div>
    </fieldset>
  </form>
</template>
