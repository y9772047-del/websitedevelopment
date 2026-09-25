import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';
export default defineConfig(({ mode }) => ({
  plugins: [vue()],
  build: {
    outDir: fileURLToPath(new URL(mode === 'standalone' ? './dist' : '../src/main/resources/static', import.meta.url)),
    emptyOutDir: true,
  },
}));
