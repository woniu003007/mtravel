<script lang="ts" setup>
import CrudPage from '#/views/_business/crud/CrudPage.vue';
import { hotelResourceConfig } from '#/views/_business/crud/module-config';
</script>

<template>
  <CrudPage :config="hotelResourceConfig" />
</template>
