<script lang="ts" setup>
import { IconifyIcon } from '@vben/icons';

import { Tag } from 'ant-design-vue';

defineOptions({ name: 'FormalTeamPageHeader' });

type FormalHeaderAction = {
  icon?: string;
  key: string;
  label: string;
};

type FormalHeaderBadge = {
  color?: string;
  editable?: boolean;
  key: string;
  label: string;
  value: string;
};

type FormalHeaderMetric = {
  clickable?: boolean;
  key: string;
  label: string;
  value: string;
};

type FormalHeaderStage = {
  label: string;
  state?: string;
};

const props = withDefaults(defineProps<{
  actions?: FormalHeaderAction[];
  badges?: FormalHeaderBadge[];
  kicker?: string;
  metrics?: FormalHeaderMetric[];
  note?: string;
  noteMetrics?: string[];
  noteTitle?: string;
  showNote?: boolean;
  stages?: FormalHeaderStage[];
  title: string;
  toolActions?: FormalHeaderAction[];
  toolTitle?: string;
}>(), {
  actions: () => [],
  badges: () => [],
  kicker: '团队安排总览 · Group Arrange',
  metrics: () => [],
  note: '未填写',
  noteMetrics: () => [],
  noteTitle: '内部备注',
  showNote: true,
  stages: () => [],
  toolActions: () => [],
  toolTitle: '团队工具',
});

const emit = defineEmits<{
  actionClick: [action: FormalHeaderAction];
  badgeClick: [badge: FormalHeaderBadge];
  metricClick: [metric: FormalHeaderMetric];
  noteEdit: [];
  toolClick: [action: FormalHeaderAction];
}>();
</script>

<template>
  <div class="formal-team-page-header">
    <div v-if="props.toolActions.length" class="formal-arrangement-tool-strip" aria-label="团队安排工具">
      <div class="formal-tool-strip-title">
        <IconifyIcon icon="lucide:wrench" />
        <span>{{ props.toolTitle }}</span>
      </div>
      <div class="formal-tool-strip-actions top-tool-actions">
        <button
          v-for="action in props.toolActions"
          :key="action.key"
          type="button"
          class="formal-arrangement-tool-button"
          @click="emit('toolClick', action)"
        >
          <IconifyIcon v-if="action.icon" :icon="action.icon" />
          <span>{{ action.label }}</span>
        </button>
      </div>
    </div>

    <div class="arrangement-command-bar">
      <div class="command-main">
        <div class="form-kicker">{{ props.kicker }}</div>
        <div class="team-title-line formal-team-title-line">
          <div class="team-name">{{ props.title }}</div>
        </div>
      </div>

      <div class="command-side">
        <div v-if="props.stages.length" class="workflow-rail" aria-label="团队阶段">
          <div
            v-for="(stage, index) in props.stages"
            :key="stage.label"
            class="stage-flow-item"
            :class="stage.state"
          >
            <span class="stage-index">{{ index + 1 }}</span>
            <span class="stage-label">{{ stage.label }}</span>
          </div>
        </div>
        <div v-if="props.actions.length" class="team-profile-actions">
          <button
            v-for="action in props.actions"
            :key="action.key"
            type="button"
            class="compact-action"
            @click="emit('actionClick', action)"
          >
            <IconifyIcon v-if="action.icon" :icon="action.icon" />
            <span>{{ action.label }}</span>
          </button>
        </div>
      </div>

      <div v-if="props.badges.length" class="team-badges formal-team-badges formal-team-badges-line">
        <Tag
          v-for="badge in props.badges"
          :key="badge.key"
          :color="badge.color"
          :class="{ editable: badge.editable }"
          @click="badge.editable && emit('badgeClick', badge)"
        >
          {{ badge.label }}：{{ badge.value }}
        </Tag>
      </div>

      <div v-if="props.metrics.length" class="team-metric-strip formal-team-metric-strip">
        <span
          v-for="metric in props.metrics"
          :key="metric.key"
          class="team-metric-item"
          :class="[`metric-${metric.key}`, { clickable: metric.clickable }]"
          @click="metric.clickable && emit('metricClick', metric)"
        >
          <em>{{ metric.label }}</em>
          <strong>{{ metric.value }}</strong>
        </span>
      </div>
    </div>

    <div v-if="props.showNote" class="team-note-row">
      <div class="internal-note-main">
        <div class="internal-note-heading">
          <IconifyIcon icon="lucide:info" />
          <span>{{ props.noteTitle }}</span>
          <button type="button" class="formal-note-edit-button" @click="emit('noteEdit')">
            <IconifyIcon icon="lucide:file-pen-line" />
            <span>编辑</span>
          </button>
        </div>
        <div v-if="props.noteMetrics.length" class="internal-note-metrics">
          <span v-for="item in props.noteMetrics" :key="item">{{ item }}</span>
        </div>
        <div class="internal-note-text">{{ props.note || '未填写' }}</div>
      </div>
    </div>
  </div>
</template>
