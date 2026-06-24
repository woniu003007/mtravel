<script lang="ts" setup>
import { IconifyIcon } from '@vben/icons';

const props = withDefaults(defineProps<{
  status?: 'confirmed' | 'none' | 'pending' | 'warning';
}>(), {
  status: 'none',
});

const statusMap = {
  confirmed: { className: 'status-confirmed', icon: 'lucide:circle-check', title: '已安排' },
  none: { className: 'status-none', icon: '', title: '--' },
  pending: { className: 'status-pending', icon: 'lucide:clock-3', title: '待安排' },
  warning: { className: 'status-warning', icon: 'lucide:circle-alert', title: '需处理' },
};
</script>

<template>
  <span class="team-arrange-status" :class="statusMap[props.status].className" :title="statusMap[props.status].title">
    <IconifyIcon v-if="statusMap[props.status].icon" :icon="statusMap[props.status].icon" />
    <span v-else>--</span>
  </span>
</template>

<style scoped>
.team-arrange-status {
  display: inline-flex;
  width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-confirmed {
  color: #16a34a;
  background: #dcfce7;
}

.status-pending {
  color: #d97706;
  background: #fef3c7;
}

.status-warning {
  color: #dc2626;
  background: #fee2e2;
}

.status-none {
  color: #2563eb;
  background: transparent;
}
</style>
