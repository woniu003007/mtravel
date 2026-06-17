<script lang="ts" setup>
import type { EmployeeRecord } from '../data';

import { computed, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';

import { message } from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';

import { useFormSchema } from '../data';

const emit = defineEmits(['success']);
const formData = ref<Partial<EmployeeRecord>>();
const getTitle = computed(() => {
  return formData.value?.id ? '编辑员工' : '添加员工';
});

const [Form, formApi] = useVbenForm({
  schema: useFormSchema(),
  showDefaultActions: false,
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (valid) {
      drawerApi.lock();
      try {
        await new Promise((resolve) => setTimeout(resolve, 300));
        message.success(formData.value?.id ? '修改成功' : '添加成功');
        drawerApi.close();
        emit('success');
      } finally {
        drawerApi.lock(false);
      }
    }
  },
  onOpenChange(isOpen) {
    if (isOpen) {
      const data = drawerApi.getData<EmployeeRecord>();
      if (data) {
        formData.value = data;
        formApi.setValues(data);
      } else {
        formData.value = {};
        formApi.resetForm();
      }
    }
  },
});
</script>

<template>
  <Drawer :title="getTitle">
    <Form />
  </Drawer>
</template>
