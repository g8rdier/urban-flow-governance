# Frontend Integration

## Serving

Das Frontend wird als statische Files über Apache ausgeliefert. API-Calls werden per Reverse Proxy an Tomcat/Grails weitergeleitet — kein CORS-Problem, der Browser sieht nur eine Domain.

Minimale Apache-Config:

```apache
ProxyPass /api http://localhost:8080/api
ProxyPassReverse /api http://localhost:8080/api
```

Alles andere (`/`, Assets) bedient Apache direkt aus dem Dokumentenverzeichnis.

## Auth-Token

Nach erfolgreichem Login gibt `POST /api/session` einen UUID-Token zurück. Dieser wird in `localStorage` abgelegt und bei jedem geschützten Request als Bearer-Token mitgeschickt.

```js
// Login
const res = await fetch('/api/session', { method: 'POST', body: ... });
const { data } = await res.json();
localStorage.setItem('token', data.token);

// Geschützter Request
fetch('/api/zones', {
  headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
});

// Logout
await fetch('/api/session', { method: 'DELETE', headers: { 'Authorization': ... } });
localStorage.removeItem('token');
```

Bei einer `401`-Antwort Token löschen und zur Login-Seite weiterleiten.

## Polygon zeichnen (Zonerstellung)

Für die Admin-UI wird [Leaflet.Draw](https://leaflet.github.io/Leaflet.draw/) eingebunden. Nach dem Zeichnen liefert das Plugin die Koordinaten als GeoJSON — dieses Format erwartet das Backend direkt.

```js
map.on('draw:created', async (e) => {
  const geojson = e.layer.toGeoJSON().geometry; // { type: "Polygon", coordinates: [...] }

  await fetch('/api/zones', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    },
    body: JSON.stringify({ name, description, reason, startTime, endTime, geometry: geojson })
  });
});
```

## Routenprüfung

Der Ablauf ist zweistufig:

1. OSRM liefert die berechnete Route als GeoJSON-Koordinaten.
2. Diese Koordinaten werden als Array von `[lat, lng]`-Paaren an `POST /api/route/check` geschickt.

```js
const osrmRes = await fetch(`/route/v1/driving/${lon1},${lat1};${lon2},${lat2}?overview=full&geometries=geojson`);
const route = osrmRes.routes[0].geometry.coordinates; // [[lng, lat], ...]
const routeLatLng = route.map(([lng, lat]) => [lat, lng]);

const checkRes = await fetch('/api/route/check', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${localStorage.getItem('token')}` },
  body: JSON.stringify({ route: routeLatLng })
});
const { data } = await checkRes.json();
// data.status === "OK" | "WARNING"
// data.zones — Liste der betroffenen Zonen
```

## Adresssuche

Für die Suche nach Adressen und Stadtteilen (US-4.1, US-4.2) werden beide Eingabewege unterstützt — analog zu Google Maps:

- **Texteingabe** → Geocoding via Nominatim (`/search?q=...&format=json`)
- **Klick auf die Karte** → Leaflet liefert `lat/lng` direkt über das Click-Event

Beide Wege liefern am Ende Koordinaten, die identisch weiterverarbeitet werden (OSRM → Routenprüfung).
