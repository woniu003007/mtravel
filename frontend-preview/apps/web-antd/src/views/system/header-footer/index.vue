<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card } from 'ant-design-vue';
</script>

<template>
  <Page title="页眉页脚印章" description="管理文档页眉页脚和印章图片">
    <Card title="页眉设置" class="mb-4">
      <div class="flex items-start gap-6">
        <div
          class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center flex-1"
        >
          <p class="text-gray-500 mb-2">点击或拖拽上传页眉图片</p>
          <p class="text-gray-400 text-sm">建议尺寸：1200 x 150px，支持 PNG/JPG 格式</p>
        </div>
        <div class="flex flex-col gap-2">
          <Button type="primary">上传</Button>
          <Button>预览</Button>
        </div>
      </div>
      <div class="mt-4 p-4 bg-gray-50 rounded">
        <p class="text-gray-400 text-sm">当前页眉：默认页眉（云南国际旅行社）</p>
      </div>
    </Card>

    <Card title="页脚设置" class="mb-4">
      <div class="flex items-start gap-6">
        <div
          class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center flex-1"
        >
          <p class="text-gray-500 mb-2">点击或拖拽上传页脚图片</p>
          <p class="text-gray-400 text-sm">建议尺寸：1200 x 100px，支持 PNG/JPG 格式</p>
        </div>
        <div class="flex flex-col gap-2">
          <Button type="primary">上传</Button>
          <Button>预览</Button>
        </div>
      </div>
      <div class="mt-4 p-4 bg-gray-50 rounded">
        <p class="text-gray-400 text-sm">当前页脚：默认页脚（含公司地址及联系方式）</p>
      </div>
    </Card>

    <Card title="印章设置">
      <div class="flex items-start gap-6">
        <div
          class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center flex-1"
        >
          <p class="text-gray-500 mb-2">点击或拖拽上传印章图片</p>
          <p class="text-gray-400 text-sm">建议尺寸：200 x 200px，支持透明背景 PNG 格式</p>
        </div>
        <div class="flex flex-col gap-2">
          <Button type="primary">上传</Button>
          <Button>预览</Button>
        </div>
      </div>
      <div class="mt-4 p-4 bg-gray-50 rounded">
        <p class="text-gray-400 text-sm">当前印章：云南国际旅行社有限公司（合同专用章）</p>
      </div>
    </Card>
  </Page>
</template>
