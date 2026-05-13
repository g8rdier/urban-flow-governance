// Map
const map = L.map('map', { doubleClickZoom: false }).setView([48.137, 11.576], 13);

L.tileLayer(window.TILES_URL, { maxZoom: 20 }).addTo(map);

const zonesLayer = L.layerGroup().addTo(map);

// State
let currentUser = null;
let isDrawing = false;
let drawVertices = [];
let drawMarkers = [];
let drawPolyline = null;
let drawPolygon = null;

let startCoords = null;
let endCoords = null;
let selectionMarker = null;
let startMarker = null;
let endMarker = null;
let routeLayer = null;
let conflictLayer = null;
let zonesVisible = false;
let isRouteDrawing = false;
let routeDrawVertices = [];
let routeDrawMarkers = [];
let routeDrawPolyline = null;
let currentEditZoneId = null;

// Auth
function authHeaders(extra = {}) {
  const token = localStorage.getItem('token');
  return { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...extra };
}

async function initSession(token, role = null) {
  localStorage.setItem('token', token);
  if (role !== null) localStorage.setItem('userRole', role);
  const effectiveRole = role ?? localStorage.getItem('userRole');
  currentUser = { role: effectiveRole };

  document.getElementById('login-overlay').style.display = 'none';
  if (effectiveRole === 'ADMIN') {
    document.getElementById('mode-toggle').style.display = 'flex';
  }
}

// Auth tab switch
window.switchTab = function (tab) {
  document.getElementById('login-form').style.display = tab === 'login' ? '' : 'none';
  document.getElementById('register-form').style.display = tab === 'register' ? '' : 'none';
  document.querySelectorAll('.auth-tab').forEach((btn, i) => {
    btn.classList.toggle('active', (i === 0) === (tab === 'login'));
  });
};

// Login
window.submitLogin = async function (e) {
  e.preventDefault();
  const username = document.getElementById('login-username').value;
  const password = document.getElementById('login-password').value;
  const error = document.getElementById('login-error');

  const res = await fetch(`${window.API_BASE}/api/session`, {
    method: 'POST',
    body: new URLSearchParams({ username, password })
  });
  const result = await res.json();

  if (result.status !== 'success') {
    error.textContent = result.msg || 'Ungültige Anmeldedaten.';
    return;
  }

  await initSession(result.data.token, result.data.role);
};
window.logout = function () {

  localStorage.clear();

  location.reload();
};
// Register
window.submitRegister = async function (e) {
  e.preventDefault();
  const username = document.getElementById('reg-username').value;
  const password = document.getElementById('reg-password').value;
  const password2 = document.getElementById('reg-password2').value;
  const error = document.getElementById('register-error');

  if (password !== password2) {
    error.textContent = 'Passwörter stimmen nicht überein.';
    return;
  }

  const res = await fetch(`${window.API_BASE}/api/users`, {
    method: 'POST',
    body: new URLSearchParams({ username, password, role: 'NUTZER' })
  });
  const result = await res.json();

  if (result.status !== 'success') {
    error.textContent = result.msg || 'Registrierung fehlgeschlagen.';
    return;
  }

  const loginRes = await fetch(`${window.API_BASE}/api/session`, {
    method: 'POST',
    body: new URLSearchParams({ username, password })
  });
  const loginResult = await loginRes.json();

  await initSession(loginResult.data.token, loginResult.data.role);
};

// Zones (map)
window.loadZones = async function () {
  zonesLayer.clearLayers();
  const zoneColors = { PLANNED: 'blue', ACTIVE: 'red', EXPIRED: 'gray' };
  try {
    const res = await fetch(`${window.API_BASE}/api/zones`, { headers: authHeaders() });
    const result = await res.json();
    (result.data.zones || []).forEach(zone => {
      if (!zone.geometry) return;
      const coords = zone.geometry.coordinates[0].map(([lng, lat]) => [lat, lng]);
      L.polygon(coords, { color: zoneColors[zone.status] ?? 'orange' }).addTo(zonesLayer);
    });
    zonesVisible = true;
    updateZonesToggleBtn();
  } catch (e) {
    console.warn('Zonen konnten nicht geladen werden');
  }
};

window.toggleZones = async function () {
  if (zonesVisible) {
    zonesLayer.clearLayers();
    zonesVisible = false;
    updateZonesToggleBtn();
    return;
  }
  zonesLayer.clearLayers();
  try {
    const res = await fetch(`${window.API_BASE}/api/zones/active`, { headers: authHeaders() });
    const result = await res.json();
    (result.data.zones || []).forEach(zone => {
      if (!zone.geometry) return;
      const coords = zone.geometry.coordinates[0].map(([lng, lat]) => [lat, lng]);
      L.polygon(coords, { color: 'red' }).addTo(zonesLayer);
    });
    zonesVisible = true;
  } catch (e) {
    console.warn('Zonen konnten nicht geladen werden');
  }
  updateZonesToggleBtn();
};

function updateZonesToggleBtn() {
  const btn = document.getElementById('zones-toggle-btn');
  if (btn) btn.textContent = zonesVisible ? 'Aktive Zonen ausblenden' : 'Aktive Zonen anzeigen';
}

// Mode toggle
window.setMode = function (mode) {
  document.getElementById('user-panel').style.display = mode === 'nutzer' ? '' : 'none';
  document.getElementById('admin-panel').style.display = mode === 'admin' ? 'flex' : 'none';
  document.getElementById('mode-btn-nutzer').classList.toggle('active', mode === 'nutzer');
  document.getElementById('mode-btn-admin').classList.toggle('active', mode === 'admin');
  if (mode === 'admin') loadAdminZoneList();
};

// Draw mode
window.startDrawing = function () {
  isDrawing = true;
  drawVertices = [];
  drawMarkers.forEach(m => m.remove());
  drawMarkers = [];
  if (drawPolyline) { drawPolyline.remove(); drawPolyline = null; }
  if (drawPolygon) { drawPolygon.remove(); drawPolygon = null; }
  document.getElementById('draw-btn').style.display = 'none';
  document.getElementById('draw-active').style.display = '';
  map.getContainer().style.cursor = 'crosshair';
};

window.cancelDrawing = function () {
  isDrawing = false;
  drawVertices = [];
  drawMarkers.forEach(m => m.remove());
  drawMarkers = [];
  if (drawPolyline) { drawPolyline.remove(); drawPolyline = null; }
  if (drawPolygon) { drawPolygon.remove(); drawPolygon = null; }
  document.getElementById('draw-btn').style.display = '';
  document.getElementById('draw-active').style.display = 'none';
  map.getContainer().style.cursor = '';
};

window.cancelZoneForm = function () {
  drawMarkers.forEach(m => m.remove());
  drawMarkers = [];
  if (drawPolygon) { drawPolygon.remove(); drawPolygon = null; }
  drawVertices = [];
  document.getElementById('zone-form-panel').style.display = 'none';
  document.getElementById('draw-btn').style.display = '';
  document.getElementById('zone-form').reset();
  document.getElementById('zone-error').textContent = '';
};

function updateDrawPolyline() {
  if (drawPolyline) { drawPolyline.remove(); drawPolyline = null; }
  if (drawVertices.length > 1) {
    drawPolyline = L.polyline(drawVertices, { color: '#4a90e2', dashArray: '5 5', weight: 2 }).addTo(map);
  }
}

function finishDrawing() {
  isDrawing = false;
  map.getContainer().style.cursor = '';
  if (drawPolyline) { drawPolyline.remove(); drawPolyline = null; }
  drawPolygon = L.polygon(drawVertices, { color: '#4a90e2', fillOpacity: 0.15, weight: 2 }).addTo(map);
  document.getElementById('draw-active').style.display = 'none';
  document.getElementById('zone-form-panel').style.display = '';
}

// Zone creation
window.submitZone = async function (e) {
  e.preventDefault();
  const error = document.getElementById('zone-error');
  const toApiDate = v => v.length === 16 ? v + ':00Z' : v;

  const ring = [...drawVertices.map(([lat, lng]) => [lng, lat])];
  ring.push(ring[0]);

  const isEdit = currentEditZoneId !== null;

const url = isEdit
  ? `${window.API_BASE}/api/zones/${currentEditZoneId}`
  : `${window.API_BASE}/api/zones`;

const method = isEdit ? 'PUT' : 'POST';

const res = await fetch(url, {
  method,
  headers: authHeaders({
    'Content-Type': 'application/json'
  }),
  body: JSON.stringify({
    name: document.getElementById('zone-name').value,
    reason: document.getElementById('zone-reason').value,
    startTime: toApiDate(document.getElementById('zone-start').value),
    endTime: toApiDate(document.getElementById('zone-end').value),
    createdBy: currentUser?.username,
    geometry: {
      type: 'Polygon',
      coordinates: [ring]
    }
  })
});


  const result = await res.json();
  if (result.status !== 'success') {
    error.textContent = result.msg || 'Fehler beim Speichern.';
    return;
  }

  window.cancelZoneForm();
  loadZones();
  loadAdminZoneList();
};

// Zone list (admin)
async function loadAdminZoneList() {
  const list = document.getElementById('zone-list');
  list.innerHTML = '';
  let zones;
  try {
    const res = await fetch(`${window.API_BASE}/api/zones`, { headers: authHeaders() });
    const result = await res.json();
    zones = result.data?.zones || [];
  } catch (e) { return; }

  if (zones.length === 0) {
    list.innerHTML = '<p class="draw-hint">Keine Zonen vorhanden.</p>';
    return;
  }

  const statusColor = { PLANNED: 'blue', ACTIVE: 'red', EXPIRED: 'gray' };
  const statusLabel = { PLANNED: 'Geplant', ACTIVE: 'Aktiv', EXPIRED: 'Abgelaufen' };

  zones.forEach(zone => {
    const color = statusColor[zone.status] || 'orange';
    const label = statusLabel[zone.status] || zone.status;
    let actions = '';
    
    if (zone.status === 'PLANNED') actions += `<button class="btn-small btn-activate" onclick="activateZone(${zone.id})">Aktivieren</button>`;
    if (zone.status === 'ACTIVE') actions += `<button class="btn-small btn-deactivate" onclick="deactivateZone(${zone.id})">Deaktivieren</button>`;
    actions += `<button class="btn-small btn-edit" onclick="editZone(${zone.id})">Bearbeiten</button>`;
    actions += `<button class="btn-small btn-delete" onclick="deleteZone(${zone.id})">Löschen</button>`;

    const item = document.createElement('div');
    item.className = 'zone-item';
    item.innerHTML = `
      <div class="zone-item-header">
        <span class="legend-dot" style="background:${color}"></span>
        <strong>${zone.name}</strong>
        <span class="zone-status-tag" style="color:${color}">${label}</span>
      </div>
      ${zone.reason ? `<div class="zone-item-meta">${zone.reason}</div>` : ''}
      <div class="zone-item-actions">${actions}</div>
    `;
    list.appendChild(item);
  });
}

window.deleteZone = async function (id) {
  if (!confirm('Zone wirklich löschen?')) return;
  const res = await fetch(`${window.API_BASE}/api/zones/${id}`, { method: 'DELETE', headers: authHeaders() });
  const result = await res.json();
  if (result.status === 'success') { loadZones(); loadAdminZoneList(); }
};

window.activateZone = async function (id) {
  const res = await fetch(`${window.API_BASE}/api/zones/${id}/activate`, { method: 'PUT', headers: authHeaders() });
  const result = await res.json();
  if (result.status === 'success') { loadZones(); loadAdminZoneList(); }
};

window.deactivateZone = async function (id) {
  const res = await fetch(`${window.API_BASE}/api/zones/${id}/deactivate`, { method: 'PUT', headers: authHeaders() });
  const result = await res.json();
  if (result.status === 'success') { loadZones(); loadAdminZoneList(); }
};
window.editZone = async function(id) {

  const res = await fetch(
    `${window.API_BASE}/api/zones/${id}`,
    {
      headers: authHeaders()
    }
  );

  const result = await res.json();

  const zone = result.data.zone;

  // Formular anzeigen
  document.getElementById('zone-form-panel').style.display = '';

  // Formular füllen
  document.getElementById('zone-name').value =
    zone.name || '';

  document.getElementById('zone-reason').value =
    zone.reason || '';

  document.getElementById('zone-start').value =
    zone.startTime?.slice(0,16) || '';

  document.getElementById('zone-end').value =
    zone.endTime?.slice(0,16) || '';

  // ID merken
  currentEditZoneId = id;
};
// Geocoding
async function geocode(address) {
  const res = await fetch(`${window.NOMINATIM_URL}/search?q=${encodeURIComponent(address)}&format=json`);
  const data = await res.json();
  if (!data || data.length === 0) { alert('Adresse nicht gefunden'); return null; }
  return { lat: parseFloat(data[0].lat), lon: parseFloat(data[0].lon) };
}

// Route
window.calculateRoute = async function () {
  const startInput = document.getElementById('start').value;
  const endInput = document.getElementById('end').value;
  if (!startInput || !endInput) return;

  startCoords = await geocode(startInput);
  if (!startCoords) return;
  setRouteMarker(startCoords.lat, startCoords.lon, 'start');

  endCoords = await geocode(endInput);
  if (!endCoords) return;
  setRouteMarker(endCoords.lat, endCoords.lon, 'end');

  const url = `${window.OSRM_URL}/route/v1/driving/${startCoords.lon},${startCoords.lat};${endCoords.lon},${endCoords.lat}?overview=full&geometries=geojson`;
  const res = await fetch(url);
  const data = await res.json();
  if (!data.routes || data.routes.length === 0) { alert('Keine Route gefunden'); return; }

  const route = data.routes[0].geometry;
  const result = await checkRoute(route);
  document.getElementById('zone-warning').style.display = 'none';
  document.getElementById('zone-warning-modal').style.display = 'none';
  if (conflictLayer) { conflictLayer.remove(); conflictLayer = null; }
  if (routeLayer) { routeLayer.remove(); }
  routeLayer = L.geoJSON(route, { color: '#4a90e2', weight: 4 }).addTo(map);
  map.fitBounds(routeLayer.getBounds());

  if (result.status === 'WARNING') {
    const names = result.zones.map(z => z.name).join(', ');
    const msg = `⚠️ Route kreuzt Sperrzone: ${names}`;
    document.getElementById('zone-warning-text').textContent = msg;
    document.getElementById('zone-warning-text-small').textContent = msg;
    document.getElementById('zone-warning-modal').style.display = 'flex';

    const intersections = result.zones.map(z => z.intersection).filter(Boolean);
    if (intersections.length > 0) {
      conflictLayer = L.layerGroup(
        intersections.map(geom => L.geoJSON(geom, { color: '#e05252', weight: 5 }))
      ).addTo(map);
    }
  }
};

async function checkRoute(route) {
  const res = await fetch(`${window.API_BASE}/api/route/check`, {
    method: 'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify({ route: route.coordinates.map(([lng, lat]) => [lat, lng]) })
  });
  return (await res.json()).data;
}

window.dismissZoneWarning = function () {
  document.getElementById('zone-warning-modal').style.display = 'none';
  document.getElementById('zone-warning').style.display = 'flex';
};

// Markers
function setSelectionMarker(lat, lon) {
  if (selectionMarker) selectionMarker.remove();
  selectionMarker = L.marker([lat, lon]).addTo(map);
}

function setRouteMarker(lat, lon, type) {
  if (type === 'start') {
    if (startMarker) startMarker.remove();
    startMarker = L.marker([lat, lon]).addTo(map);
  } else {
    if (endMarker) endMarker.remove();
    endMarker = L.marker([lat, lon]).addTo(map);
  }
}

// Route drawing mode
window.startRouteDrawing = function () {
  isRouteDrawing = true;
  routeDrawVertices = [];
  routeDrawMarkers.forEach(m => m.remove());
  routeDrawMarkers = [];
  if (routeDrawPolyline) { routeDrawPolyline.remove(); routeDrawPolyline = null; }
  document.getElementById('route-draw-btn').style.display = 'none';
  document.getElementById('route-draw-active').style.display = '';
  map.getContainer().style.cursor = 'crosshair';
};

window.cancelRouteDrawing = function () {
  isRouteDrawing = false;
  routeDrawVertices = [];
  routeDrawMarkers.forEach(m => m.remove());
  routeDrawMarkers = [];
  if (routeDrawPolyline) { routeDrawPolyline.remove(); routeDrawPolyline = null; }
  document.getElementById('route-draw-btn').style.display = '';
  document.getElementById('route-draw-active').style.display = 'none';
  map.getContainer().style.cursor = '';
};

async function finishRouteDrawing() {
  if (routeDrawVertices.length < 2) return;
  const vertices = [...routeDrawVertices];
  cancelRouteDrawing();
  const waypoints = vertices.map(([lat, lng]) => `${lng},${lat}`).join(';');
  const url = `${window.OSRM_URL}/route/v1/driving/${waypoints}?overview=full&geometries=geojson`;
  let data;
  try { data = await (await fetch(url)).json(); } catch { alert('Route konnte nicht berechnet werden.'); return; }
  if (!data.routes || !data.routes.length) { alert('Keine Route gefunden'); return; }
  const route = data.routes[0].geometry;

  if (conflictLayer) { conflictLayer.remove(); conflictLayer = null; }
  if (routeLayer) { routeLayer.remove(); }
  routeLayer = L.geoJSON(route, { color: '#4a90e2', weight: 4 }).addTo(map);
  map.fitBounds(routeLayer.getBounds());
  document.getElementById('zone-warning').style.display = 'none';
  document.getElementById('zone-warning-modal').style.display = 'none';

  try {
    const result = await checkRoute(route);
    if (result?.status === 'WARNING') {
      const names = result.zones.map(z => z.name).join(', ');
      const msg = `⚠️ Route kreuzt Sperrzone: ${names}`;
      document.getElementById('zone-warning-text').textContent = msg;
      document.getElementById('zone-warning-text-small').textContent = msg;
      document.getElementById('zone-warning-modal').style.display = 'flex';
      const intersections = result.zones.map(z => z.intersection).filter(Boolean);
      if (intersections.length) {
        conflictLayer = L.layerGroup(intersections.map(g => L.geoJSON(g, { color: '#e05252', weight: 5 }))).addTo(map);
      }
    }
  } catch { /* zone check failed, route still shown */ }
}

// Map events
map.on('click', function (e) {
  const { lat, lng } = e.latlng;
  if (isDrawing) {
    drawVertices.push([lat, lng]);
    const m = L.circleMarker([lat, lng], { radius: 4, color: '#4a90e2', fillColor: '#4a90e2', fillOpacity: 1, weight: 1 }).addTo(map);
    drawMarkers.push(m);
    updateDrawPolyline();
    return;
  }
  if (isRouteDrawing) {
    routeDrawVertices.push([lat, lng]);
    const m = L.circleMarker([lat, lng], { radius: 4, color: '#4a90e2', fillColor: '#4a90e2', fillOpacity: 1, weight: 1 }).addTo(map);
    routeDrawMarkers.push(m);
    if (routeDrawPolyline) routeDrawPolyline.remove();
    if (routeDrawVertices.length > 1) {
      routeDrawPolyline = L.polyline(routeDrawVertices, { color: '#4a90e2', dashArray: '5 5', weight: 2 }).addTo(map);
    }
  }
});

map.on('dblclick', function (e) {
  if (isDrawing) {
    drawVertices.splice(-2, 2);
    drawMarkers.splice(-2).forEach(m => m.remove());
    updateDrawPolyline();
    if (drawVertices.length >= 3) finishDrawing();
    return;
  }
  if (isRouteDrawing) {
    routeDrawVertices.splice(-2, 2);
    routeDrawMarkers.splice(-2).forEach(m => m.remove());
    if (routeDrawPolyline) { routeDrawPolyline.remove(); routeDrawPolyline = null; }
    if (routeDrawVertices.length > 1) {
      routeDrawPolyline = L.polyline(routeDrawVertices, { color: '#4a90e2', dashArray: '5 5', weight: 2 }).addTo(map);
    }
    finishRouteDrawing();
  }
});

// Autocomplete
window.searchAddress = async function (type) {
  const query = document.getElementById(type).value;
  if (query.length < 3) return;
  const data = await (await fetch(`${window.NOMINATIM_URL}/search?q=${encodeURIComponent(query)}&format=json`)).json();
  const list = document.getElementById(`suggestions-${type}`);
  list.innerHTML = '';
  data.slice(0, 5).forEach(place => {
    const li = document.createElement('li');
    li.innerText = place.display_name;
    li.onclick = () => selectAddress(place, type);
    list.appendChild(li);
  });
};

function selectAddress(place, type) {
  const lat = parseFloat(place.lat);
  const lon = parseFloat(place.lon);
  setRouteMarker(lat, lon, type);
  map.setView([lat, lon], 15);
  if (type === 'start') { startCoords = { lat, lon }; document.getElementById('start').value = place.display_name; }
  else { endCoords = { lat, lon }; document.getElementById('end').value = place.display_name; }
  document.getElementById(`suggestions-${type}`).innerHTML = '';
  if (document.getElementById(type === 'start' ? 'end' : 'start').value) window.calculateRoute();
}

// Swap
window.swapRoute = function () {
  const s = document.getElementById('start');
  const e = document.getElementById('end');
  [s.value, e.value] = [e.value, s.value];
  [startCoords, endCoords] = [endCoords, startCoords];
  [startMarker, endMarker] = [endMarker, startMarker];
  if (s.value && e.value) window.calculateRoute();
};

// Theme
window.toggleSidebar = function () {
  const sidebar = document.getElementById('sidebar');
  const btn = document.getElementById('sidebar-collapse-btn');
  const collapsed = sidebar.classList.toggle('collapsed');
  btn.classList.toggle('collapsed', collapsed);
  btn.innerHTML = collapsed ? '&#8250;' : '&#8249;';
  setTimeout(() => map.invalidateSize(), 310);
};

window.toggleTheme = function () {
  const next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem('theme', next);
};

// Init
document.addEventListener('DOMContentLoaded', function () {
  const saved = localStorage.getItem('theme');
  if (saved) document.documentElement.setAttribute('data-theme', saved);

  const token = localStorage.getItem('token');
  if (token) initSession(token);

  positionSwapButton();
});

function positionSwapButton() {
  const startInput = document.getElementById('start');
  const endInput = document.getElementById('end');
  const btn = document.getElementById('swap-btn');
  const container = document.getElementById('route-inputs');
  const containerTop = container.getBoundingClientRect().top;
  const midY = (startInput.getBoundingClientRect().bottom + endInput.getBoundingClientRect().top) / 2;
  btn.style.top = (midY - containerTop) + 'px';
}

window.addEventListener('resize', positionSwapButton);
