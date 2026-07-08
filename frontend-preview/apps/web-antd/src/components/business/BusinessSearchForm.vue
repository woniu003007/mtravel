<script lang="ts" setup>
import { Form } from 'ant-design-vue';
import { computed } from 'vue';

import BusinessActionButtons from './BusinessActionButtons.vue';

/**
 * 业务列表页查询表单壳。
 *
 * <p>统一查询条件网格、表单 label 宽度和操作按钮区。普通 CRUD 页面和复杂业务页面都应复用该组件，
 * 页面只负责提供查询字段，不再单独维护一套搜索区 CSS。</p>
 */
const props = withDefaults(defineProps<{
  actionsClass?: string;
  actionsInGrid?: boolean;
  createText?: string;
  gridClass?: string;
  labelWidth?: string;
  model: Record<string, any>;
  searchLoading?: boolean;
  searchText?: string;
  showCreate?: boolean;
}>(), {
  actionsClass: '',
  actionsInGrid: true,
  createText: '新增',
  gridClass: '',
  labelWidth: '72px',
  searchLoading: false,
  searchText: '查询',
  showCreate: true,
});

const emit = defineEmits<{
  create: [];
  reset: [];
  search: [];
}>();

const labelCol = computed(() => ({ style: { width: props.labelWidth } }));
</script>

<template>
  <Form
    class="business-search-form"
    :label-col="labelCol"
    :model="model"
    :wrapper-col="{ flex: 1 }"
  >
    <div :class="['business-search-grid', gridClass]">
      <slot />
      <BusinessActionButtons
        v-if="actionsInGrid"
        :class="actionsClass"
        :create-text="createText"
        :search-loading="searchLoading"
        :search-text="searchText"
        :show-create="showCreate"
        @create="emit('create')"
        @reset="emit('reset')"
        @search="emit('search')"
      >
        <template #extra>
          <slot name="extraActions" />
        </template>
      </BusinessActionButtons>
    </div>
    <BusinessActionButtons
      v-if="!actionsInGrid"
      :class="actionsClass"
      :create-text="createText"
      :search-loading="searchLoading"
      :search-text="searchText"
      :show-create="showCreate"
      @create="emit('create')"
      @reset="emit('reset')"
      @search="emit('search')"
    >
      <template #extra>
        <slot name="extraActions" />
      </template>
    </BusinessActionButtons>
  </Form>
</template>

<style scoped>
.business-search-form {
  margin-bottom: 8px;
}

.business-search-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px 12px;
  align-items: center;
}

.business-search-form :deep(.ant-form-item) {
  min-width: 0;
  margin-bottom: 0;
}

.business-search-form :deep(.ant-form-item-row) {
  min-height: 30px;
  flex-wrap: nowrap;
  align-items: center;
}

.business-search-form :deep(.ant-form-item-label) {
  flex: 0 0 v-bind('props.labelWidth');
  max-width: v-bind('props.labelWidth');
  padding: 0 8px 0 0;
  overflow: hidden;
  text-align: right;
  white-space: nowrap;
}

.business-search-form :deep(.ant-form-item-label > label) {
  color: #475569;
  font-size: 13px;
  font-weight: 500;
}

.business-search-form :deep(.ant-form-item-control) {
  min-width: 0;
}

.business-search-form :deep(.ant-form-item-control-input-content) {
  min-width: 0;
}

.business-search-form :deep(.ant-input),
.business-search-form :deep(.ant-input-affix-wrapper),
.business-search-form :deep(.ant-input-number),
.business-search-form :deep(.ant-picker),
.business-search-form :deep(.ant-select-selector) {
  height: 30px !important;
  min-height: 30px !important;
  border-radius: 6px;
}

.business-search-form :deep(.ant-input-affix-wrapper .ant-input) {
  height: 20px !important;
  min-height: 20px !important;
  line-height: 20px;
}

.business-search-form :deep(.ant-input),
.business-search-form :deep(.ant-input-affix-wrapper .ant-input),
.business-search-form :deep(.ant-picker-input > input),
.business-search-form :deep(.ant-input-number-input),
.business-search-form :deep(.ant-select-selection-item),
.business-search-form :deep(.ant-select-selection-placeholder) {
  font-size: 13px;
}

.business-search-form :deep(.ant-input),
.business-search-form :deep(.ant-input-affix-wrapper),
.business-search-form :deep(.ant-input-number),
.business-search-form :deep(.ant-picker),
.business-search-form :deep(.ant-select) {
  width: 100%;
}

.business-search-form :deep(.ant-select-selector) {
  align-items: center;
}

.business-search-form :deep(.business-search-item--wide) {
  grid-column: span 2;
}

.business-search-form :deep(.business-action-buttons) {
  grid-column: 5 / 8;
}

@media (max-width: 1600px) {
  .business-search-grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }

  .business-search-form :deep(.business-action-buttons) {
    grid-column: 4 / 6;
  }
}

@media (max-width: 1100px) {
  .business-search-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .business-search-form :deep(.business-action-buttons) {
    grid-column: 1 / -1;
  }
}

@media (max-width: 900px) {
  .business-search-grid {
    grid-template-columns: 1fr;
  }

  .business-search-form :deep(.business-search-item--wide) {
    grid-column: span 1;
  }
}
</style>
