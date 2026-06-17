<script lang="ts" setup>
import CrudPage from '#/views/_business/crud/CrudPage.vue';
import { customerCreditConfig } from '#/views/_business/crud/module-config';
</script>

<template>
  <CrudPage :config="customerCreditConfig" />
</template>
