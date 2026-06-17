<script lang="ts" setup>
import { Page } from '@vben/common-ui';
import { Button, Card, Form, Radio } from 'ant-design-vue';
import { ref } from 'vue';

const themeColor = ref('blue');
const navMode = ref('side');
const contentWidth = ref('wide');
</script>

<template>
  <Page title="系统风格设置" description="自定义系统界面风格">
    <Card>
      <Form layout="vertical">
        <Form.Item label="主题色">
          <Radio.Group v-model:value="themeColor">
            <Radio.Button value="blue">蓝色</Radio.Button>
            <Radio.Button value="green">绿色</Radio.Button>
            <Radio.Button value="purple">紫色</Radio.Button>
            <Radio.Button value="orange">橙色</Radio.Button>
          </Radio.Group>
        </Form.Item>

        <Form.Item label="导航模式">
          <Radio.Group v-model:value="navMode">
            <Radio.Button value="side">侧边导航</Radio.Button>
            <Radio.Button value="top">顶部导航</Radio.Button>
            <Radio.Button value="mix">混合导航</Radio.Button>
          </Radio.Group>
        </Form.Item>

        <Form.Item label="内容区域宽度">
          <Radio.Group v-model:value="contentWidth">
            <Radio.Button value="wide">宽屏模式</Radio.Button>
            <Radio.Button value="narrow">窄屏模式</Radio.Button>
          </Radio.Group>
        </Form.Item>

        <Form.Item>
          <Button type="primary">保存设置</Button>
        </Form.Item>
      </Form>
    </Card>
  </Page>
</template>
