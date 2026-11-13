// Quizee PWA Offline Service Worker
const CACHE_NAME = 'quizee-offline-v11';

const PRECACHE_ASSETS = [
    './',
    './index.html',
    './mock-tests.html',
    './byjus-gk.html',
    './tables.html',
    './assets/quizee-theme.css?v=8',
    './assets/quizee-theme.css',
    './assets/quizee-audio.js?v=1',
    './assets/quizee-audio.js',
    './assets/web_tests.css?v=4.6',
    './assets/web_tests.css',
    './assets/web_tests.js?v=4.7',
    './assets/web_tests.js',
    './assets/byjus_gk.css?v=2',
    './assets/byjus_gk.css',
    './assets/byjus_gk.js?v=4',
    './assets/byjus_gk.js',
    './assets/tables.css?v=2',
    './assets/tables.js?v=5',
    './assets/tables.js',
    './assets/icon.png',
    './assets/favicon.png',
    './data/all_questions.js',
    './data/byjus_gk_data.js',
    './data/study_tables_data.js',
    './manifest.json'
];

// Install: Pre-cache core offline assets
self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => {
            console.log('[Quizee SW] Pre-caching offline curriculum & app shell...');
            return cache.addAll(PRECACHE_ASSETS.map(url => new Request(url, { cache: 'reload' })))
                .catch(err => {
                    console.warn('[Quizee SW] Pre-cache partial fail, caching individually:', err);
                    return Promise.allSettled(PRECACHE_ASSETS.map(url => cache.add(url)));
                });
        }).then(() => self.skipWaiting())
    );
});

// Activate: Clean up older cache versions
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((cacheNames) => {
            return Promise.all(
                cacheNames.map((name) => {
                    if (name !== CACHE_NAME) {
                        console.log('[Quizee SW] Deleting obsolete cache:', name);
                        return caches.delete(name);
                    }
                })
            );
        }).then(() => self.clients.claim())
    );
});

// Fetch: Network-First for HTML & App Shell (so deployments appear immediately), Cache Fallback Offline
self.addEventListener('fetch', (event) => {
    const request = event.request;

    // Only handle GET requests
    if (request.method !== 'GET') return;

    // Do not cache non-http schemes
    if (!request.url.startsWith('http')) return;

    const url = new URL(request.url);
    const isHtmlOrShell = request.mode === 'navigate' || url.pathname.endsWith('.html') || url.pathname.endsWith('.css') || url.pathname.endsWith('.js');

    if (isHtmlOrShell) {
        event.respondWith(
            fetch(request)
                .then((networkResponse) => {
                    if (networkResponse && networkResponse.status === 200) {
                        const clone = networkResponse.clone();
                        caches.open(CACHE_NAME).then((cache) => cache.put(request, clone));
                    }
                    return networkResponse;
                })
                .catch(() => {
                    return caches.match(request).then((cached) => {
                        if (cached) return cached;
                        if (request.mode === 'navigate') {
                            if (request.url.includes('tables')) return caches.match('./tables.html');
                            if (request.url.includes('byjus-gk')) return caches.match('./byjus-gk.html');
                            if (request.url.includes('mock-tests')) return caches.match('./mock-tests.html');
                            return caches.match('./index.html') || caches.match('./');
                        }
                        return null;
                    });
                })
        );
        return;
    }

    event.respondWith(
        caches.match(request).then((cachedResponse) => {
            const fetchPromise = fetch(request)
                .then((networkResponse) => {
                    if (networkResponse && networkResponse.status === 200) {
                        const responseToCache = networkResponse.clone();
                        caches.open(CACHE_NAME).then((cache) => {
                            cache.put(request, responseToCache);
                        });
                    }
                    return networkResponse;
                })
                .catch(() => cachedResponse || null);

            return cachedResponse || fetchPromise;
        })
    );
});
