import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { VitePWA } from 'vite-plugin-pwa';
// https://vite.dev/config/
export default defineConfig({
    server: {
        host: '0.0.0.0',
        proxy: {
            '/api': { target: process.env.VITE_BACKEND_TARGET || 'http://127.0.0.1:8080', changeOrigin: true },
            '/alphabet': { target: process.env.VITE_BACKEND_TARGET || 'http://127.0.0.1:8080', changeOrigin: true, rewrite: p => '/api/public/media' + p },
            '/audio': { target: process.env.VITE_BACKEND_TARGET || 'http://127.0.0.1:8080', changeOrigin: true, rewrite: p => '/api/public/media' + p },
            '/example_images': { target: process.env.VITE_BACKEND_TARGET || 'http://127.0.0.1:8080', changeOrigin: true, rewrite: p => '/api/public/media' + p },
        },
    },
    plugins: [
        react(),
        VitePWA({
            registerType: 'autoUpdate',
            includeAssets: ['pwa-icon.svg', 'pwa-192x192.png', 'pwa-512x512.png'],
            manifest: {
                name: 'Học Chữ Lớp 1',
                short_name: 'Học Chữ',
                description: 'Khu vực quản lý và giảng dạy Vui Học cho giáo viên và quản trị viên',
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
