<script lang="ts" setup>
import { Button } from 'ant-design-vue';

/**
 * 业务列表页查询按钮组。
 *
 * <p>后台列表页统一使用“重置 / 查询 / 新增”的顺序和按钮样式，避免每个页面手写按钮后出现
 * 顺序、间距、主次按钮不一致的问题。</p>
 */
withDefaults(defineProps<{
  createText?: string;
  searchLoading?: boolean;
  searchText?: string;
  showCreate?: boolean;
}>(), {
  createText: '新增',
  searchLoading: false,
  searchText: '查询',
  showCreate: true,
});

const emit = defineEmits<{
  create: [];
  reset: [];
  search: [];
}>();
</script>

<template>
  <div class="business-action-buttons">
    <Button @click="emit('reset')">重置</Button>
    <Button type="primary" :loading="searchLoading" @click="emit('search')">
      {{ searchText }}
    </Button>
    <Button v-if="showCreate" type="primary" @click="emit('create')">
      {{ createText }}
    </Button>
    <slot name="extra" />
  </div>
</template>

<style scoped>
.business-action-buttons {
  display: flex;
  width: 100%;
  height: 30px;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 0;
  white-space: nowrap;
}

.business-action-buttons :deep(.ant-btn) {
  min-width: 64px;
  height: 30px;
  padding-inline: 12px;
  border-radius: 6px;
}

.business-action-buttons :deep(.ant-btn:last-child) {
  min-width: 78px;
}

@media (max-width: 768px) {
  .business-action-buttons {
    height: auto;
    flex-wrap: wrap;
    justify-content: flex-start;
  }
}
</style>
