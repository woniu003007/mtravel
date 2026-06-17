<script lang="ts" setup>
import { computed, useSlots } from 'vue';

import { preferences } from '@vben/preferences';

import { PreferencesButton } from '../../widgets';

interface Props {
  /**
   * Logo 主题
   */
  theme?: string;
}

defineOptions({
  name: 'LayoutHeader',
});

withDefaults(defineProps<Props>(), {
  theme: 'light',
});

const emit = defineEmits<{ clearPreferencesAndLogout: [] }>();

const REFERENCE_VALUE = 50;

const slots = useSlots();

const rightSlots = computed(() => {
  const list = [
    { index: REFERENCE_VALUE + 10, name: 'preferences' },
    { index: REFERENCE_VALUE + 100, name: 'user-dropdown' },
  ];

  Object.keys(slots).forEach((key) => {
    const name = key.split('-');
    if (key.startsWith('header-right')) {
      list.push({ index: Number(name[2]), name: key });
    }
  });
  return list.toSorted((a, b) => a.index - b.index);
});

const leftSlots = computed(() => {
  const list: Array<{ index: number; name: string }> = [];

  Object.keys(slots).forEach((key) => {
    const name = key.split('-');
    if (key.startsWith('header-left')) {
      list.push({ index: Number(name[2]), name: key });
    }
  });
  return list.toSorted((a, b) => a.index - b.index);
});

function clearPreferencesAndLogout() {
  emit('clearPreferencesAndLogout');
}
</script>

<template>
  <template
    v-for="slot in leftSlots.filter((item) => item.index < REFERENCE_VALUE)"
    :key="slot.name"
  >
    <slot :name="slot.name">
    </slot>
  </template>
  <div class="flex-center hidden lg:block">
    <slot name="breadcrumb"></slot>
  </div>
  <template
    v-for="slot in leftSlots.filter((item) => item.index > REFERENCE_VALUE)"
    :key="slot.name"
  >
    <slot :name="slot.name"></slot>
  </template>
  <div
    :class="`menu-align-${preferences.header.menuAlign}`"
    class="flex h-full min-w-0 flex-1 items-center"
  >
    <slot name="menu"></slot>
  </div>
  <div class="flex h-full min-w-0 shrink-0 items-center">
    <template v-for="slot in rightSlots" :key="slot.name">
      <slot :name="slot.name">
        <template v-if="slot.name === 'preferences'">
          <PreferencesButton
            class="mr-1"
            @clear-preferences-and-logout="clearPreferencesAndLogout"
          />
        </template>
      </slot>
    </template>
  </div>
</template>
<style lang="scss" scoped>
.menu-align-start {
  --menu-align: start;
}

.menu-align-center {
  --menu-align: center;
}

.menu-align-end {
  --menu-align: end;
}
</style>
