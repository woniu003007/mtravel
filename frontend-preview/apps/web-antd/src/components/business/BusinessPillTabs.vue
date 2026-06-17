<script lang="ts" setup>
/**
 * 业务胶囊页签组件。
 *
 * <p>用于后台业务页面的分类 tab，统一成蓝色选中、白色未选中的胶囊样式。费用项目、产品字典、
 * 产品编辑等页面都复用该组件，避免每个页面复制一套 tab CSS 后样式再次跑偏。</p>
 */
defineProps<{
  activeKey: string;
  ariaLabel?: string;
  tabs: Array<{
    key: string;
    label: string;
  }>;
}>();

const emit = defineEmits<{
  change: [key: string];
}>();
</script>

<template>
  <div class="business-pill-tabs" role="tablist" :aria-label="ariaLabel || '业务分类'">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      type="button"
      class="business-pill-tab"
      :class="{ 'is-active': activeKey === tab.key }"
      role="tab"
      :aria-selected="activeKey === tab.key"
      @click="emit('change', tab.key)"
    >
      {{ tab.label }}
    </button>
  </div>
</template>

<style scoped>
.business-pill-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px;
  margin-bottom: 14px;
  overflow-x: auto;
  background: #f6f8fb;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
}

.business-pill-tabs::-webkit-scrollbar {
  height: 6px;
}

.business-pill-tabs::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 999px;
}

.business-pill-tab {
  flex: 0 0 auto;
  min-width: 72px;
  height: 34px;
  padding: 0 14px;
  font-size: 14px;
  font-weight: 500;
  line-height: 32px;
  color: #334155;
  text-align: center;
  cursor: pointer;
  background: #fff;
  border: 1px solid #d8dee8;
  border-radius: 999px;
  transition:
    color 0.18s ease,
    background-color 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.business-pill-tab:hover {
  color: #1677ff;
  border-color: #91caff;
}

.business-pill-tab:focus-visible {
  outline: 2px solid #91caff;
  outline-offset: 2px;
}

.business-pill-tab.is-active {
  color: #fff;
  background: #1677ff;
  border-color: #1677ff;
  box-shadow: 0 6px 14px rgb(22 119 255 / 22%);
}
</style>
