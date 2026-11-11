import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';
// https://vite.dev/config/
export default defineConfig({
    plugins: [
        react(),
        VitePWA({
            registerType: 'autoUpdate',
            includeAssets: ['pwa-icon.svg', 'pwa-192x192.png', 'pwa-512x512.png'],
            manifest: {
                name: 'Học Chữ Lớp 1',
                short_name: 'Học Chữ',
                description: 'Ứng dụng luyện viết chữ tiếng Việt cho học sinh',
                theme_color: '#bae6fd',
                background_color: '#ffffff',
                display: 'standalone',
                orientation: 'portrait',
                icons: [
                    {
                        src: 'pwa-icon.svg',
                        sizes: 'any',
                        type: 'image/svg+xml'
                    },
                    {
                        src: 'pwa-192x192.png',
                        sizes: '192x192',
                        type: 'image/png'
                    },
                    {
                        src: 'pwa-512x512.png',
                        sizes: '512x512',
                        type: 'image/png',
                        purpose: 'any maskable'
                    }
                ]
            }
        })
    ],
});
