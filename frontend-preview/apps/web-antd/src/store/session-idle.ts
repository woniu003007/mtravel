import type { Router } from 'vue-router';

import { LOGIN_PATH } from '@vben/constants';
import { useAccessStore } from '@vben/stores';

import { message } from 'ant-design-vue';

import { heartbeatApi } from '#/api';
import { useAuthStore } from '#/store';

const activityEvents = [
  'mousemove',
  'mousedown',
  'keydown',
  'scroll',
  'touchstart',
  'click',
] as const;

const heartbeatIntervalMs = 5 * 60 * 1000;

let idleTimer: number | undefined;
let heartbeatTimer: number | undefined;
let lastActivityAt = Date.now();
let installed = false;

export function setupSessionIdleGuard(router: Router) {
  if (installed || typeof window === 'undefined') {
    return;
  }
  installed = true;

  const onActivity = () => {
    lastActivityAt = Date.now();
    scheduleIdleTimer(router);
  };

  activityEvents.forEach((eventName) => {
    window.addEventListener(eventName, onActivity, { passive: true });
  });

  router.afterEach(() => {
    onActivity();
  });

  heartbeatTimer = window.setInterval(() => {
    void sendHeartbeatIfActive(router);
  }, heartbeatIntervalMs);

  scheduleIdleTimer(router);
}

function scheduleIdleTimer(router: Router) {
  if (idleTimer) {
    window.clearTimeout(idleTimer);
  }
  const accessStore = useAccessStore();
  if (!accessStore.accessToken || router.currentRoute.value.path === LOGIN_PATH) {
    return;
  }
  if (accessStore.loginIdleTimeoutMinutes <= 0) {
    return;
  }
  const timeoutMs = accessStore.loginIdleTimeoutMinutes * 60 * 1000;
  idleTimer = window.setTimeout(() => {
    void logoutByIdleTimeout();
  }, timeoutMs);
}

async function sendHeartbeatIfActive(router: Router) {
  const accessStore = useAccessStore();
  if (!accessStore.accessToken || router.currentRoute.value.path === LOGIN_PATH) {
    return;
  }
  if (accessStore.loginIdleTimeoutMinutes <= 0) {
    return;
  }
  const timeoutMs = accessStore.loginIdleTimeoutMinutes * 60 * 1000;
  if (Date.now() - lastActivityAt >= timeoutMs) {
    return;
  }
  try {
    await heartbeatApi();
  } catch {
    // 普通请求拦截器会处理登录失效提示，这里不重复弹窗。
  }
}

async function logoutByIdleTimeout() {
  const accessStore = useAccessStore();
  if (!accessStore.accessToken) {
    return;
  }
  message.warning('长时间未操作，已自动退出登录');
  const authStore = useAuthStore();
  await authStore.logout(true);
}

export function stopSessionIdleGuard() {
  if (idleTimer) {
    window.clearTimeout(idleTimer);
    idleTimer = undefined;
  }
  if (heartbeatTimer) {
    window.clearInterval(heartbeatTimer);
    heartbeatTimer = undefined;
  }
  installed = false;
}
