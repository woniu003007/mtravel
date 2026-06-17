<script lang="ts" setup>
import CrudPage from '#/views/_business/crud/CrudPage.vue';
import { supplierConfig } from '#/views/_business/crud/module-config';
</script>

<template>
  <CrudPage :config="supplierConfig" />
</template>
