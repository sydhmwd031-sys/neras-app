// Offline support: the app and its fonts keep working without internet after the first visit
const CACHE = 'neras-v6';
const CORE = ['./', './index.html', './manifest.webmanifest', './icons/icon-192.png', './icons/icon-512.png', './icons/apple-touch-icon.png'];
self.addEventListener('install', e => { e.waitUntil(caches.open(CACHE).then(c => c.addAll(CORE))); self.skipWaiting(); });
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))));
  self.clients.claim();
});
self.addEventListener('fetch', e => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  // App page: network first so updates arrive, cache as fallback
  if (req.mode === 'navigate') {
    e.respondWith(fetch(req).then(r => { const c = r.clone(); caches.open(CACHE).then(x => x.put('./index.html', c)); return r; })
      .catch(() => caches.match('./index.html')));
    return;
  }
  // Fonts and icons: cache first
  if (url.origin === location.origin || url.host.endsWith('fonts.googleapis.com') || url.host.endsWith('fonts.gstatic.com')) {
    e.respondWith(caches.match(req).then(hit => hit || fetch(req).then(r => {
      const c = r.clone(); caches.open(CACHE).then(x => x.put(req, c)); return r;
    })));
  }
});
