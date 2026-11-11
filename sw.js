// Quizee PWA Offline Service Worker
const CACHE_NAME = 'quizee-offline-v9';

const PRECACHE_ASSETS = [
    './',
    './index.html',
    './mock-tests.html',
    './byjus-gk.html',
    './tables.html',
    './assets/web_tests.css?v=4.5',
    './assets/web_tests.css',
    './assets/web_tests.js?v=4.5',
    './assets/web_tests.js',
    './assets/byjus_gk.css',
    './assets/byjus_gk.js',
    './assets/tables.css?v=2',
    './assets/tables.js?v=2',
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

// Fetch: Stale-While-Revalidate with full offline fallback
self.addEventListener('fetch', (event) => {
    const request = event.request;

    // Only handle GET requests
    if (request.method !== 'GET') return;

    // Do not cache non-http schemes
    if (!request.url.startsWith('http')) return;

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
                .catch((err) => {
                    // Network failed (device is offline)
                    if (cachedResponse) {
                        return cachedResponse;
                    }
                    // For HTML navigations, fallback to cached index or mock-tests
                    if (request.mode === 'navigate') {
                        if (request.url.includes('mock-tests')) {
                            return caches.match('./mock-tests.html');
                        }
                        return caches.match('./index.html') || caches.match('./');
                    }
                    return null;
                });

            // Return cached response instantly if available, otherwise wait for network
            return cachedResponse || fetchPromise;
        })
    );
});
