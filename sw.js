const CACHE_NAME = 'remember-v10';
const ASSETS = [
  './',
  './index.html',
  './style.css',
  './app.js',
  './manifest.json',
  './icon-180.png',
  './icon-192.png',
  './icon-512.png'
];

// Instalar Service Worker y cachear recursos
self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      console.log('Service Worker: Cacheando archivos principales');
      return cache.addAll(ASSETS).catch(err => {
        console.warn('Error al precachear algunos archivos:', err);
      });
    })
  );
  self.skipWaiting();
});

// Activar Service Worker y limpiar caches viejas
self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            console.log('Service Worker: Eliminando cache vieja', key);
            return caches.delete(key);
          }
        })
      );
    })
  );
  self.clients.claim();
});

// ==========================================================================
// Alarmas programadas desde la app principal
// ==========================================================================

const swAlarms = new Map(); // id -> timeoutId

self.addEventListener('message', (event) => {
  const { type, id, title, body, dueDate } = event.data || {};

  if (type === 'SCHEDULE_NOTIFICATION') {
    if (swAlarms.has(id)) clearTimeout(swAlarms.get(id));
    const delay = new Date(dueDate).getTime() - Date.now();
    if (delay <= 0) return;
    const tid = setTimeout(() => {
      self.registration.showNotification(title, {
        body,
        icon: './icon-192.png',
        badge: './icon-192.png',
        tag: id,
        requireInteraction: true
      });
      swAlarms.delete(id);
    }, delay);
    swAlarms.set(id, tid);
  }

  if (type === 'CANCEL_NOTIFICATION') {
    if (swAlarms.has(id)) { clearTimeout(swAlarms.get(id)); swAlarms.delete(id); }
    self.registration.getNotifications({ tag: id }).then(notifs => notifs.forEach(n => n.close()));
  }
});

// Abrir la app al pulsar una notificación
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(list => {
      for (const client of list) {
        if (client.url.includes(self.location.origin) && 'focus' in client) return client.focus();
      }
      return clients.openWindow('./');
    })
  );
});

// ==========================================================================
// Estrategia Network-First (Red primero, cae en caché si está offline)
self.addEventListener('fetch', (e) => {
  // Evitar interceptar peticiones que no sean HTTP/HTTPS (por ejemplo, extensiones o esquemas chrome)
  if (!e.request.url.startsWith('http')) return;

  e.respondWith(
    fetch(e.request)
      .then((response) => {
        // Si la respuesta es válida, guardarla/actualizarla en la caché
        if (response.status === 200) {
          const responseClone = response.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(e.request, responseClone);
          });
        }
        return response;
      })
      .catch(() => {
        // Si no hay red, buscar en la caché
        return caches.match(e.request).then((cachedResponse) => {
          if (cachedResponse) {
            return cachedResponse;
          }
          // Si es una navegación y no está en caché, dar el fallback index
          if (e.request.mode === 'navigate') {
            return caches.match('./index.html');
          }
        });
      })
  );
});
