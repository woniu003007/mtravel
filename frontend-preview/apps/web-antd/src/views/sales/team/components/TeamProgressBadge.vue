<script lang="ts" setup>
import { computed } from 'vue';

const props = withDefaults(defineProps<{
  status?: string;
}>(), {
  status: 'not_departed',
});

const fallbackProgress = { className: 'progress-normal', label: '排', title: '未发团' };
const progressMap: Record<string, { className: string; label: string; title: string }> = {
  cancelled: { className: 'progress-cancelled', label: '取', title: '已取消' },
  closed: { className: 'progress-closed', label: '账', title: '待结算' },
  departed: { className: 'progress-departed', label: '排', title: '已发团' },
  done: { className: 'progress-done', label: '完', title: '已完成' },
  not_departed: fallbackProgress,
  receiving: { className: 'progress-normal', label: '排', title: '收客中' },
  stopped: { className: 'progress-stopped', label: '停', title: '已停收' },
};

const item = computed(() => progressMap[props.status] || fallbackProgress);
</script>

<template>
  <span class="team-progress-badge" :class="item.className" :title="item.title">
    {{ item.label }}
  </span>
</template>

<style scoped>
.team-progress-badge {
  display: inline-flex;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
}

.progress-normal {
  color: #0369a1;
  background: #e0f2fe;
}

.progress-departed {
  color: #0f766e;
  background: #ccfbf1;
}

.progress-closed {
  color: #7c3aed;
  background: #ede9fe;
}

.progress-done {
  color: #15803d;
  background: #dcfce7;
}

.progress-cancelled {
  color: #dc2626;
  background: #fee2e2;
}

.progress-stopped {
  color: #d97706;
  background: #fef3c7;
}
</style>
