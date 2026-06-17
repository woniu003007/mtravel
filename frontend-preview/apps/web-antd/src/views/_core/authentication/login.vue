<script lang="ts" setup>
import type { Recordable } from '@vben/types';

import { ref } from 'vue';

import loginIllustrationUrl from '#/assets/login/mtravel-login-illustration.svg?url';

import { IconifyIcon } from '@vben/icons';
import { Button, Checkbox, Input, message } from 'ant-design-vue';

import { useAuthStore } from '#/store';

defineOptions({ name: 'Login' });

const authStore = useAuthStore();

const username = ref(localStorage.getItem('MTRAVEL_REMEMBER_USERNAME') || '');
const password = ref('');
const rememberAccount = ref(!!username.value);

/**
 * 提交后台账号登录。
 *
 * 登录页只负责表单校验和记住账号；接口错误由全局请求拦截器统一提示。
 */
async function handleLogin() {
  if (!username.value.trim()) {
    message.warning('请输入用户名');
    return;
  }
  if (!password.value) {
    message.warning('请输入密码');
    return;
  }

  if (rememberAccount.value) {
    localStorage.setItem('MTRAVEL_REMEMBER_USERNAME', username.value.trim());
  } else {
    localStorage.removeItem('MTRAVEL_REMEMBER_USERNAME');
  }

  try {
    await authStore.authLogin({
      username: username.value.trim(),
      password: password.value,
    } as Recordable<any>);
  } catch {
    // 全局请求拦截器已经展示后端错误信息，这里只阻止 Vue 事件处理警告。
  }
}
</script>

<template>
  <main class="mtravel-login-page">
    <header class="mtravel-login-header">
      <div class="mtravel-brand">
        <div class="mtravel-brand__icon">
          <IconifyIcon icon="lucide:map-pinned" />
        </div>
        <div class="mtravel-brand__text">
          <h1>旅游接待管理系统</h1>
          <p>Destination Management System</p>
        </div>
      </div>
    </header>

    <section class="mtravel-login-body">
      <section class="mtravel-slogan" aria-label="系统介绍">
        <h2>
          旅游接待管理系统，助力<span>地接业务提效</span>
        </h2>
        <img
          alt="旅游接待管理系统业务插画"
          class="mtravel-slogan__image"
          :src="loginIllustrationUrl"
        />
      </section>

      <section class="mtravel-login-card" aria-label="账号登录">
        <h2>账号登录</h2>

        <form class="mtravel-form" @submit.prevent="handleLogin">
          <Input
            v-model:value="username"
            autocomplete="username"
            class="mtravel-input"
            placeholder="请输入用户名"
            size="large"
          />

          <Input.Password
            v-model:value="password"
            autocomplete="current-password"
            class="mtravel-input"
            placeholder="请输入密码"
            size="large"
          />

          <div class="mtravel-form__meta">
            <Checkbox v-model:checked="rememberAccount">记住账号</Checkbox>
            <span>忘记密码请联系管理员</span>
          </div>

          <Button
            block
            class="mtravel-login-button"
            html-type="submit"
            :loading="authStore.loginLoading"
            size="large"
            type="primary"
          >
            登录
          </Button>
        </form>
      </section>
    </section>

    <footer class="mtravel-login-footer">
      Copyright © 2009-2026 All Right Reserved.
    </footer>
  </main>
</template>

<style scoped>
.mtravel-login-page {
  display: grid;
  min-height: 100vh;
  grid-template-rows: 92px 1fr 72px;
  background: #f3f6fb;
  color: #1f2937;
  font-family:
    'Microsoft YaHei',
    'PingFang SC',
    'Helvetica Neue',
    Arial,
    sans-serif;
}

.mtravel-login-header {
  display: flex;
  align-items: center;
  background: #ffffff;
  border-bottom: 1px solid #eef2f7;
}

.mtravel-brand {
  display: flex;
  width: min(1240px, calc(100% - 56px));
  align-items: center;
  gap: 13px;
  margin: 0 auto;
}

.mtravel-brand__icon {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  background: #0f9aa7;
  color: #ffffff;
}

.mtravel-brand__icon :deep(.iconify) {
  width: 25px;
  height: 25px;
}

.mtravel-brand__text {
  display: flex;
  align-items: baseline;
  gap: 13px;
}

.mtravel-brand h1 {
  margin: 0;
  color: #202733;
  font-size: 25px;
  font-weight: 700;
  letter-spacing: 0;
}

.mtravel-brand p {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.mtravel-login-body {
  display: grid;
  width: min(1240px, calc(100% - 56px));
  grid-template-columns: minmax(0, 1fr) 420px;
  align-items: center;
  column-gap: 92px;
  margin: 0 auto;
}

.mtravel-slogan {
  min-width: 0;
}

.mtravel-slogan h2 {
  max-width: 720px;
  margin: 0 0 18px;
  color: #374151;
  font-size: 25px;
  font-weight: 500;
  letter-spacing: 0;
  line-height: 1.5;
}

.mtravel-slogan h2 span {
  color: #0f9aa7;
  font-weight: 700;
}

.mtravel-slogan__image {
  display: block;
  width: min(720px, 100%);
  height: auto;
  margin-top: 8px;
}

.mtravel-login-card {
  padding: 40px 38px 36px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 46px rgba(31, 41, 55, 0.08);
}

.mtravel-login-card h2 {
  margin: 0 0 34px;
  color: #2f343d;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0;
}

.mtravel-form {
  display: grid;
  gap: 22px;
}

:deep(.mtravel-input.ant-input),
:deep(.mtravel-input.ant-input-affix-wrapper),
.mtravel-input :deep(.ant-input),
.mtravel-input :deep(.ant-input-affix-wrapper) {
  font-size: 15px;
}

:deep(.mtravel-input.ant-input),
:deep(.mtravel-input.ant-input-affix-wrapper),
.mtravel-input :deep(.ant-input-affix-wrapper),
.mtravel-input :deep(.ant-input) {
  border: 1px solid #d9dce3;
  border-radius: 4px;
  background-color: #ffffff !important;
  background: #ffffff !important;
  color: #111827 !important;
  box-shadow: none !important;
}

:deep(.mtravel-input.ant-input-affix-wrapper),
.mtravel-input :deep(.ant-input-affix-wrapper) {
  height: 48px;
  padding: 0 15px;
  background-color: #ffffff !important;
  background: #ffffff !important;
}

:deep(.mtravel-input.ant-input),
.mtravel-input :deep(.ant-input) {
  height: 46px;
  padding: 0 15px;
  background-color: #ffffff !important;
  background: #ffffff !important;
  line-height: 46px;
}

.mtravel-input :deep(.ant-input-prefix),
.mtravel-input :deep(.ant-input-suffix),
:deep(.mtravel-input.ant-input-affix-wrapper .ant-input-prefix),
:deep(.mtravel-input.ant-input-affix-wrapper .ant-input-suffix) {
  background: transparent !important;
}

.mtravel-input :deep(.ant-input-affix-wrapper .ant-input),
:deep(.mtravel-input.ant-input-affix-wrapper .ant-input) {
  color: #111827 !important;
}

.mtravel-input :deep(.ant-input-affix-wrapper .ant-input),
:deep(.mtravel-input.ant-input-affix-wrapper > .ant-input) {
  height: auto;
  padding: 0;
  border: 0 !important;
  background: transparent !important;
  line-height: normal;
  box-shadow: none !important;
}

:deep(.mtravel-input.ant-input-affix-wrapper-focused),
:deep(.mtravel-input.ant-input-affix-wrapper:focus),
:deep(.mtravel-input.ant-input:focus),
.mtravel-input :deep(.ant-input-affix-wrapper-focused),
.mtravel-input :deep(.ant-input-affix-wrapper:focus),
.mtravel-input :deep(.ant-input:focus) {
  border-color: #0f9aa7;
  box-shadow: 0 0 0 3px rgba(15, 154, 167, 0.1);
}

:deep(.mtravel-input.ant-input::placeholder),
.mtravel-input :deep(.ant-input::placeholder) {
  color: #a0a7b2;
}

.mtravel-input :deep(.ant-input-password-icon) {
  color: #9ca3af;
}

.mtravel-form__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  color: #6b7280;
  font-size: 14px;
}

.mtravel-form__meta :deep(.ant-checkbox-wrapper) {
  color: #374151;
  font-size: 14px;
}

.mtravel-form__meta :deep(.ant-checkbox-inner) {
  background: #ffffff;
  border-color: #d1d5db;
}

.mtravel-form__meta :deep(.ant-checkbox-checked .ant-checkbox-inner) {
  background: #0f9aa7;
  border-color: #0f9aa7;
}

.mtravel-login-button {
  height: 48px;
  border-color: #0f9aa7;
  border-radius: 4px;
  background: #0f9aa7;
  box-shadow: none;
  font-size: 17px;
  font-weight: 700;
}

.mtravel-login-button:hover,
.mtravel-login-button:focus {
  border-color: #0c8792;
  background: #0c8792;
}

.mtravel-login-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid #eef2f7;
  background: #ffffff;
  color: #9ca3af;
  font-size: 13px;
  text-align: center;
}

@media (max-width: 960px) {
  .mtravel-login-page {
    grid-template-rows: 82px 1fr 62px;
  }

  .mtravel-brand__text {
    display: block;
  }

  .mtravel-brand h1 {
    font-size: 21px;
  }

  .mtravel-brand p {
    margin-top: 4px;
    font-size: 12px;
  }

  .mtravel-login-body {
    width: min(520px, calc(100% - 32px));
    grid-template-columns: 1fr;
    padding: 28px 0;
  }

  .mtravel-slogan {
    display: none;
  }
}

@media (max-width: 520px) {
  .mtravel-login-card {
    padding: 30px 22px;
  }

  .mtravel-form__meta {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
