// Quizee PWA Offline Service Worker
const CACHE_NAME = 'quizee-offline-v14';

const PRECACHE_ASSETS = [
    './',
    './index.html',
    './mock-tests.html',
    './byjus-gk.html',
    './tables.html',
    './tasks.html',
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
    './assets/quzeaudio.mp3',
    './assets/quzeaudio.mpeg',
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
        // Special handler for Audio files (handles HTTP 206 Range requests for offline media playback)
    if (url.pathname.endsWith('.mp3') || url.pathname.endsWith('.mpeg') || request.destination === 'audio') {
        event.respondWith(
            (async () => {
                const cache = await caches.open(CACHE_NAME);
                let cachedResponse = await cache.match(request);
                if (!cachedResponse) {
                    cachedResponse = await cache.match(url.pathname) || 
                                     await cache.match('.' + url.pathname) ||
                                     await cache.match('./assets/quzeaudio.mp3') ||
                                     await cache.match('./assets/quzeaudio.mpeg');
                }

                const rangeHeader = request.headers.get('range');
                if (rangeHeader && cachedResponse) {
                    try {
                        const arrayBuffer = await cachedResponse.arrayBuffer();
                        const bytesMatch = rangeHeader.match(/bytes=(\d+)-(\d+)?/);
                        if (bytesMatch) {
                            const start = parseInt(bytesMatch[1], 10);
                            const end = bytesMatch[2] ? parseInt(bytesMatch[2], 10) : arrayBuffer.byteLength - 1;
                            const sliced = arrayBuffer.slice(start, end + 1);
                            return new Response(sliced, {
                                status: 206,
                                statusText: 'Partial Content',
                                headers: [
                                    ['Content-Type', cachedResponse.headers.get('Content-Type') || 'audio/mpeg'],
                                    ['Content-Range', `bytes ${start}-${end}/${arrayBuffer.byteLength}`],
                                    ['Content-Length', `${sliced.byteLength}`],
                                    ['Accept-Ranges', 'bytes']
                                ]
                            });
                        }
                    } catch (e) {
                        console.warn('[Quizee SW] Range response slice failed, returning full cached audio:', e);
                    }
                }

                if (cachedResponse) return cachedResponse;

                try {
                    const netResponse = await fetch(request);
                    if (netResponse && (netResponse.status === 200 || netResponse.status === 206)) {
                        cache.put(request, netResponse.clone());
                    }
                    return netResponse;
                } catch (netErr) {
                    return cachedResponse || new Response('', { status: 404, statusText: 'Audio Not Found' });
                }
            })()
        );
        return;
    }

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
                            if (request.url.includes('tasks')) return caches.match('./tasks.html');
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
