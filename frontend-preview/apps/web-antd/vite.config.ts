import { defineConfig } from '@vben/vite-config';

export default defineConfig(async () => {
  return {
    application: {},
    vite: {
      server: {
        proxy: {
          '/api': {
            changeOrigin: true,
            rewrite: (path) => path.replace(/^\/api/, ''),
            // 默认代理到 Spring Boot 后端。需要临时使用 mock 时，可设置 VITE_PROXY_TARGET=http://localhost:5320/api。
            target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
            ws: true,
          },
        },
      },
    },
  };
});
