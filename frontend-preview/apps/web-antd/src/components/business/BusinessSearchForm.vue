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
  createText?: string;
  labelWidth?: string;
  model: Record<string, any>;
  searchLoading?: boolean;
  searchText?: string;
  showCreate?: boolean;
}>(), {
  createText: '新增',
  labelWidth: '96px',
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
    <div class="business-search-grid">
      <slot />
    </div>
    <BusinessActionButtons
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
  margin-bottom: 16px;
}

.business-search-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(260px, 1fr));
  gap: 12px 16px;
}

.business-search-form :deep(.ant-form-item) {
  margin-bottom: 0;
}

.business-search-form :deep(.ant-form-item-control-input-content) {
  min-width: 0;
}

@media (max-width: 1200px) {
  .business-search-grid {
    grid-template-columns: repeat(2, minmax(240px, 1fr));
  }
}

@media (max-width: 768px) {
  .business-search-grid {
    grid-template-columns: 1fr;
  }
}
</style>
