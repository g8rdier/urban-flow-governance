// ════════════════════════════════════════════════════════════════════════════
// BLOCK 1 — KARTE & THEME
// Karten-Initialisierung, Tile-Layer, Sidebar, Theme-Toggle, globaler Zustand
// ════════════════════════════════════════════════════════════════════════════

// [Elizat 8.1]

// Leaflet-Karte erstellen, zentriert auf München (Koordinaten + Zoomstufe 13)
// doubleClickZoom deaktiviert, damit Doppelklick zum Zeichnen genutzt werden kann
const map = L.map('map', { doubleClickZoom: false }).setView([48.137, 11.576], 13);

// Aktueller Karten-Tile-Layer (wird bei Theme-Wechsel ausgetauscht)
let tileLayer = null;

// Lädt den passenden CARTO-Tile-Layer je nach Theme (dark / light)
function applyTileLayer(theme) {
  if (tileLayer) tileLayer.remove();
  tileLayer = L.tileLayer(
    theme === 'dark' ? window.TILES_DARK : window.TILES_LIGHT,
    { maxZoom: 20, attribution: window.TILES_ATTR }
  ).addTo(map);
}
// Beim Laden: gespeichertes Theme aus localStorage lesen, Standard ist 'light'
applyTileLayer(localStorage.getItem('theme') || 'light');

// Layer-Gruppe für alle Sperrzonen auf der Karte
const zonesLayer = L.layerGroup().addTo(map);

// Klappt die Sidebar ein oder aus, Karte passt Größe danach an
window.toggleSidebar = function () {
  const sidebar = document.getElementById('sidebar');
  const btn = document.getElementById('sidebar-collapse-btn');
  const collapsed = sidebar.classList.toggle('collapsed');
  btn.classList.toggle('collapsed', collapsed);
  btn.innerHTML = collapsed ? '&#8250;' : '&#8249;';
  setTimeout(() => map.invalidateSize(), 310); // warten bis CSS-Animation fertig
};

// Wechselt zwischen Dark- und Light-Theme, speichert Auswahl im localStorage
window.toggleTheme = function () {
  const next = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', next);
  localStorage.setItem('theme', next);
  applyTileLayer(next);
};

// ─── Globaler Zustand ────────────────────────────────────────────────────────
let currentUser = null;       // eingeloggter Benutzer (Rolle etc.)

// Zeichenmodus
let isDrawing = false;        // true = Admin zeichnet gerade ein Polygon
let drawVertices = [];        // gesammelte Klick-Koordinaten beim Zeichnen
let drawMarkers = [];         // kleine Punkte auf der Karte pro Vertex
let drawPolyline = null;      // gestrichelte Linie während des Zeichnens
let drawPolygon = null;       // fertiges Polygon auf der Karte

// Routenplanung
let startCoords = null;       // Startkoordinaten der Route
let endCoords = null;         // Zielkoordinaten der Route
let startMarker = null;       // Marker am Startpunkt der Route
let endMarker = null;         // Marker am Zielpunkt der Route
let routeLayer = null;        // GeoJSON-Layer der berechneten Route
let conflictLayer = null;     // Hervorhebung der Konflikt-Abschnitte mit Sperrzonen

// Sperrzonen
let zonesVisible = false;     // ob Sperrzonen aktuell auf der Karte sichtbar sind
let currentEditZoneId = null; // ID der Zone, die gerade bearbeitet wird (null = Neu-Erstellung)
let adminZonesCache = [];     // zwischengespeicherte Zonenliste für schnellen Zugriff bei Bearbeitung

// Benutzerverwaltung
let adminUsersCache = [];     // zwischengespeicherte Benutzerliste
let currentEditUserId = null; // ID des Benutzers der gerade bearbeitet wird


// ════════════════════════════════════════════════════════════════════════════
// BLOCK 2 — AUTHENTIFIZIERUNG
// Login, Logout, Registrierung, Session, JWT-Token
// ════════════════════════════════════════════════════════════════════════════

// [Elizat 8.2]

// Erstellt den Authorization-Header mit dem gespeicherten JWT-Token
// Wird jedem API-Request mitgegeben, der Authentifizierung benötigt
function authHeaders(extra = {}) {
  const token = localStorage.getItem('token');
  return { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...extra };
}

// Speichert Token und Rolle, blendet Login-Overlay aus
// Bei Admin-Rolle: Mode-Toggle einblenden und Admin-Panel öffnen
async function initSession(token, role = null) {
  localStorage.setItem('token', token);
  if (role !== null) localStorage.setItem('userRole', role);
  const effectiveRole = role ?? localStorage.getItem('userRole');
  currentUser = { role: effectiveRole };

  document.getElementById('login-overlay').style.display = 'none';
  if (effectiveRole === 'ADMIN') {
    document.getElementById('mode-toggle').style.display = 'flex';
    setMode('admin');
  }
}

// Wechselt zwischen Login- und Registrierungs-Tab im Overlay
window.switchTab = function (tab) {
  document.getElementById('login-form').style.display = tab === 'login' ? '' : 'none';
  document.getElementById('register-form').style.display = tab === 'register' ? '' : 'none';
  document.querySelectorAll('.auth-tab').forEach((btn, i) => {
    btn.classList.toggle('active', (i === 0) === (tab === 'login'));
  });
};

// Login: sendet Benutzername + Passwort an die API, speichert Token bei Erfolg
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

// Logout: löscht alle gespeicherten Daten und lädt die Seite neu
window.logout = function () {
  localStorage.clear();
  location.reload();
};

// Registrierung: legt neuen Nutzer mit Rolle NUTZER an
// Nach Erfolg: automatischer Login und Session starten
window.submitRegister = async function (e) {
  e.preventDefault();
  const username = document.getElementById('reg-username').value;
  const password = document.getElementById('reg-password').value;
  const password2 = document.getElementById('reg-password2').value;
  const error = document.getElementById('register-error');

  // Passwörter müssen übereinstimmen
  if (password !== password2) {
    error.textContent = 'Passwörter stimmen nicht überein.';
    return;
  }

  // Benutzer über die API anlegen
  const res = await fetch(`${window.API_BASE}/api/users`, {
    method: 'POST',
    body: new URLSearchParams({ username, password, role: 'NUTZER' })
  });
  const result = await res.json();

  if (result.status !== 'success') {
    error.textContent = result.msg || 'Registrierung fehlgeschlagen.';
    return;
  }

  // Nach erfolgreicher Registrierung direkt einloggen
  const loginRes = await fetch(`${window.API_BASE}/api/session`, {
    method: 'POST',
    body: new URLSearchParams({ username, password })
  });
  const loginResult = await loginRes.json();

  if (loginResult.status !== 'success') {
    error.textContent = loginResult.msg || 'Auto-Login fehlgeschlagen.';
    return;
  }
  await initSession(loginResult.data.token, loginResult.data.role);
};


// ════════════════════════════════════════════════════════════════════════════
// BLOCK 3 — ADMIN-PANEL
// Panel-Steuerung, Sperrzonen verwalten, Zeichenmodus, Benutzerverwaltung
// ════════════════════════════════════════════════════════════════════════════

// ─── Panel-Steuerung ─────────────────────────────────────────────────────────

// Wechselt zwischen Nutzer-Panel und Admin-Panel
// Admin: lädt automatisch Zonenliste und zeigt alle Zonen auf der Karte
window.setMode = function (mode) {
  document.getElementById('user-panel').style.display = mode === 'nutzer' ? '' : 'none';
  document.getElementById('admin-panel').style.display = mode === 'admin' ? 'flex' : 'none';
  document.getElementById('mode-btn-nutzer').classList.toggle('active', mode === 'nutzer');
  document.getElementById('mode-btn-admin').classList.toggle('active', mode === 'admin');
  if (mode === 'admin') { setAdminTab('zones'); loadAdminZoneList(); loadZones(); }
  if (mode === 'nutzer') { zonesLayer.clearLayers(); zonesVisible = false; updateZonesToggleBtn(); }
};

// Wechselt im Admin-Panel zwischen Zonen-Tab und Benutzer-Tab
window.setAdminTab = function (tab) {
  document.getElementById('admin-zones-section').style.display = tab === 'zones' ? '' : 'none';
  document.getElementById('admin-users-section').style.display = tab === 'users' ? '' : 'none';
  document.getElementById('admin-tab-zones').classList.toggle('active', tab === 'zones');
  document.getElementById('admin-tab-users').classList.toggle('active', tab === 'users');
  if (tab === 'users') loadAdminUserList();
};

// ─── Sperrzonen auf der Karte ────────────────────────────────────────────────

// Lädt alle Zonen (PLANNED/ACTIVE/EXPIRED) von der API und zeichnet sie farbig auf die Karte
window.loadZones = async function () {
  zonesLayer.clearLayers();
  const zoneColors = { PLANNED: 'blue', ACTIVE: 'red', EXPIRED: 'gray' };
  try {
    const res = await fetch(`${window.API_BASE}/api/zones`, { headers: authHeaders() });
    const result = await res.json();
    (result.data.zones || []).forEach(zone => {
      if (!zone.geometry) return;
      // GeoJSON-Koordinaten sind [lng, lat], Leaflet erwartet [lat, lng] → tauschen
      const coords = zone.geometry.coordinates[0].map(([lng, lat]) => [lat, lng]);
      L.polygon(coords, { color: zoneColors[zone.status] ?? 'orange' }).addTo(zonesLayer);
    });
    zonesVisible = true;
    updateZonesToggleBtn();
  } catch (e) {
    console.warn('Zonen konnten nicht geladen werden');
  }
};

// Blendet Zonen ein oder aus (Nutzer-Panel: nur aktive Zonen)
window.toggleZones = async function () {
  if (zonesVisible) {
    zonesLayer.clearLayers();
    zonesVisible = false;
    updateZonesToggleBtn();
    return;
  }
  zonesLayer.clearLayers();
  try {
    // Nur aktive Sperrzonen laden (für normale Nutzer)
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

// Aktualisiert den Button-Text je nachdem ob Zonen sichtbar sind oder nicht
function updateZonesToggleBtn() {
  const btn = document.getElementById('zones-toggle-btn');
  if (btn) btn.textContent = zonesVisible ? 'Aktive Zonen ausblenden' : 'Aktive Zonen anzeigen';
  const adminBtn = document.getElementById('admin-zones-toggle-btn');
  if (adminBtn) adminBtn.textContent = zonesVisible ? 'Zonen ausblenden' : 'Alle Zonen anzeigen';
}

// Admin-Version: blendet alle Zonen ein/aus (nicht nur aktive)
window.toggleAdminZones = async function () {
  if (zonesVisible) {
    zonesLayer.clearLayers();
    zonesVisible = false;
    updateZonesToggleBtn();
  } else {
    await loadZones();
  }
};

// ─── Zeichenmodus für Sperrzonen ─────────────────────────────────────────────

// [Elizat 8.4]

// Aktiviert den Zeichenmodus: Cursor wird zum Kreuz, Vertices-Array wird geleert
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

// Bricht den Zeichenmodus ab und löscht alle bisherigen Punkte
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

// Schließt das Zonen-Formular und setzt den Zeichenstatus zurück
window.cancelZoneForm = function () {
  drawMarkers.forEach(m => m.remove());
  drawMarkers = [];
  if (drawPolygon) { drawPolygon.remove(); drawPolygon = null; }
  drawVertices = [];
  document.getElementById('zone-form-panel').style.display = 'none';
  document.getElementById('draw-btn').style.display = '';
  document.getElementById('zone-form').reset();
  document.getElementById('zone-error').textContent = '';
  document.getElementById('zone-status-group').style.display = 'none';
};

// Zeichnet die gestrichelte Linie zwischen den bisherigen Vertices (Vorschau)
function updateDrawPolyline() {
  if (drawPolyline) { drawPolyline.remove(); drawPolyline = null; }
  if (drawVertices.length > 1) {
    drawPolyline = L.polyline(drawVertices, { color: '#4a90e2', dashArray: '5 5', weight: 2 }).addTo(map);
  }
}

// Schließt das Polygon ab und zeigt das Zonen-Formular zur Dateneingabe
function finishDrawing() {
  isDrawing = false;
  map.getContainer().style.cursor = '';
  if (drawPolyline) { drawPolyline.remove(); drawPolyline = null; }
  drawPolygon = L.polygon(drawVertices, { color: '#4a90e2', fillOpacity: 0.15, weight: 2 }).addTo(map);
  document.getElementById('draw-active').style.display = 'none';
  document.getElementById('zone-form-panel').style.display = '';
}

// ─── Zonenverwaltung (CRUD) ───────────────────────────────────────────────────

// Speichert eine neue Zone (POST) oder aktualisiert eine bestehende (PUT)
// Konvertiert die gezeichneten Leaflet-Koordinaten in ein GeoJSON-Polygon
window.submitZone = async function (e) {
  e.preventDefault();
  const error = document.getElementById('zone-error');
  // datetime-local liefert "YYYY-MM-DDTHH:MM", API erwartet "...:00Z"
  const toApiDate = v => v.length === 16 ? v + ':00Z' : v;

  // Mindestens 3 Punkte nötig für ein gültiges Polygon
  if (drawVertices.length < 3) {
    error.textContent = 'Polygon fehlt.';
    return;
  }

  const validVertices = drawVertices.filter(v => Array.isArray(v) && v[0] != null && v[1] != null);
  if (validVertices.length < 3) {
    error.textContent = 'Polygon enthält ungültige Koordinaten.';
    return;
  }

  // Leaflet [lat, lng] → GeoJSON [lng, lat] umkehren
  const ring = [...validVertices.map(([lat, lng]) => [lng, lat])];
  // GeoJSON-Polygon: letzter Punkt muss gleich dem ersten sein (Ring schließen)
  ring.push(ring[0]);

  // Bei Bearbeitung PUT an bestehende ID, sonst POST für neue Zone
  const isEdit = currentEditZoneId !== null;
  const url = isEdit
    ? `${window.API_BASE}/api/zones/${currentEditZoneId}`
    : `${window.API_BASE}/api/zones`;
  const method = isEdit ? 'PUT' : 'POST';

  const body = {
    name: document.getElementById('zone-name').value,
    reason: document.getElementById('zone-reason').value,
    startTime: toApiDate(document.getElementById('zone-start').value),
    endTime: toApiDate(document.getElementById('zone-end').value),
    createdBy: currentUser?.username,
    geometry: { type: 'Polygon', coordinates: [ring] }
  };
  // Status-Änderung (PLANNED/ACTIVE/EXPIRED) nur beim Bearbeiten erlaubt
  if (isEdit) body.status = document.getElementById('zone-status').value;

  const res = await fetch(url, {
    method,
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(body)
  });

  const result = await res.json();
  if (result.status !== 'success') {
    error.textContent = result.msg || 'Fehler beim Speichern.';
    return;
  }

  // Formular zurücksetzen und Karte + Liste neu laden
  window.cancelZoneForm();
  loadZones();
  loadAdminZoneList();
};

// Lädt alle Zonen von der API und rendert sie als Liste mit Aktions-Buttons
async function loadAdminZoneList() {
  const list = document.getElementById('zone-list');
  list.innerHTML = '';
  let zones;
  try {
    const res = await fetch(`${window.API_BASE}/api/zones`, { headers: authHeaders() });
    const result = await res.json();
    zones = result.data?.zones || [];
    adminZonesCache = zones; // Cache für schnellen Zugriff bei editZone()
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

    // Aktions-Buttons je nach aktuellem Status der Zone
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

// Löscht eine Zone nach Bestätigung und aktualisiert Karte + Liste
window.deleteZone = async function (id) {
  if (!confirm('Zone wirklich löschen?')) return;
  const res = await fetch(`${window.API_BASE}/api/zones/${id}`, { method: 'DELETE', headers: authHeaders() });
  const result = await res.json();
  if (result.status === 'success') { loadZones(); loadAdminZoneList(); }
};

// Setzt den Status einer geplanten Zone auf ACTIVE
window.activateZone = async function (id) {
  const res = await fetch(`${window.API_BASE}/api/zones/${id}/activate`, { method: 'PUT', headers: authHeaders() });
  const result = await res.json();
  if (result.status === 'success') { loadZones(); loadAdminZoneList(); }
};

// Setzt den Status einer aktiven Zone auf EXPIRED (deaktiviert)
window.deactivateZone = async function (id) {
  const res = await fetch(`${window.API_BASE}/api/zones/${id}/deactivate`, { method: 'PUT', headers: authHeaders() });
  const result = await res.json();
  if (result.status === 'success') { loadZones(); loadAdminZoneList(); }
};

// Lädt eine Zone aus dem Cache und öffnet das Bearbeitungs-Formular
// Zeigt das bestehende Polygon auf der Karte an
window.editZone = async function (id) {
  const zone = adminZonesCache.find(z => z.id === id);
  if (!zone) { console.warn('Zone nicht im Cache gefunden:', id); return; }

  document.getElementById('zone-form-panel').style.display = '';

  // Formularfelder mit den bestehenden Zonendaten befüllen
  document.getElementById('zone-name').value = zone.name || '';
  document.getElementById('zone-reason').value = zone.reason || '';
  // Datum auf 16 Zeichen kürzen ("YYYY-MM-DDTHH:MM") für datetime-local Input
  document.getElementById('zone-start').value = zone.startTime?.slice(0, 16) || '';
  document.getElementById('zone-end').value = zone.endTime?.slice(0, 16) || '';

  currentEditZoneId = id;
  document.getElementById('zone-status-group').style.display = '';
  document.getElementById('zone-status').value = zone.status || 'PLANNED';

  // GeoJSON-Koordinaten in Leaflet-Format umwandeln und Polygon anzeigen
  if (zone.geometry && zone.geometry.coordinates) {
    // Letzten Punkt entfernen (ist gleich dem ersten → GeoJSON schließt Ring)
    drawVertices = zone.geometry.coordinates[0]
      .slice(0, -1)
      .map(([lng, lat]) => [lat, lng]);

    if (drawPolygon) { drawPolygon.remove(); drawPolygon = null; }
    drawPolygon = L.polygon(drawVertices, { color: '#4a90e2', fillOpacity: 0.15, weight: 2 }).addTo(map);
  } else {
    console.warn('Keine Geometry gefunden');
    drawVertices = [];
  }
};

// ─── Benutzerverwaltung ───────────────────────────────────────────────────────

// Lädt alle Benutzer und zeigt sie im Admin-Panel an
async function loadAdminUserList() {
  const list = document.getElementById('user-list');
  list.innerHTML = '';
  try {
    const res = await fetch(`${window.API_BASE}/api/users`, { headers: authHeaders() });
    const result = await res.json();
    adminUsersCache = result.data?.users || [];
  } catch (e) { return; }

  if (adminUsersCache.length === 0) {
    list.innerHTML = '<p class="draw-hint">Keine Benutzer vorhanden.</p>';
    return;
  }

  adminUsersCache.forEach(user => {
    const item = document.createElement('div');
    item.className = 'zone-item';
    item.innerHTML = `
      <div class="zone-item-header">
        <strong>${user.username}</strong>
        <span class="zone-status-tag">${user.role}</span>
      </div>
      <div class="zone-item-actions">
        <button class="btn-small btn-edit" onclick="editUser(${user.id})">Bearbeiten</button>
        <button class="btn-small btn-delete" onclick="deleteUser(${user.id})">Löschen</button>
      </div>
    `;
    list.appendChild(item);
  });
}

// Öffnet das Formular zum Anlegen eines neuen Benutzers
window.showUserForm = function () {
  currentEditUserId = null;
  document.getElementById('user-form').reset();
  document.getElementById('user-password-label').textContent = 'Passwort';
  document.getElementById('user-password').required = true;
  document.getElementById('user-error').textContent = '';
  document.getElementById('user-form-panel').style.display = '';
  document.getElementById('user-add-btn').style.display = 'none';
};

// Füllt das Formular mit den Daten eines bestehenden Benutzers zum Bearbeiten
window.editUser = function (id) {
  const user = adminUsersCache.find(u => u.id === id);
  if (!user) return;
  currentEditUserId = id;
  document.getElementById('user-username').value = user.username;
  document.getElementById('user-role').value = user.role;
  document.getElementById('user-password').value = '';
  // Passwort ist beim Bearbeiten optional
  document.getElementById('user-password').required = false;
  document.getElementById('user-password-label').textContent = 'Neues Passwort (leer lassen = unverändert)';
  document.getElementById('user-error').textContent = '';
  document.getElementById('user-form-panel').style.display = '';
  document.getElementById('user-add-btn').style.display = 'none';
};

// Schließt das Benutzer-Formular und setzt den Zustand zurück
window.cancelUserForm = function () {
  document.getElementById('user-form-panel').style.display = 'none';
  document.getElementById('user-form').reset();
  document.getElementById('user-error').textContent = '';
  document.getElementById('user-add-btn').style.display = '';
  currentEditUserId = null;
};

// Löscht einen Benutzer nach Bestätigung
window.deleteUser = async function (id) {
  if (!confirm('Benutzer wirklich löschen?')) return;
  const res = await fetch(`${window.API_BASE}/api/users/${id}`, { method: 'DELETE', headers: authHeaders() });
  const result = await res.json();
  if (result.status === 'success') loadAdminUserList();
};

// Speichert einen neuen Benutzer (POST) oder aktualisiert einen bestehenden (PUT)
window.submitUser = async function (e) {
  e.preventDefault();
  const error = document.getElementById('user-error');
  const username = document.getElementById('user-username').value;
  const password = document.getElementById('user-password').value;
  const role = document.getElementById('user-role').value;

  const isEdit = currentEditUserId !== null;
  const url = isEdit
    ? `${window.API_BASE}/api/users/${currentEditUserId}`
    : `${window.API_BASE}/api/users`;
  const method = isEdit ? 'PUT' : 'POST';

  const body = new URLSearchParams({ username, role });
  // Passwort nur mitsenden wenn es ausgefüllt wurde
  if (password) body.append('password', password);

  const res = await fetch(url, { method, headers: authHeaders(), body });
  const result = await res.json();
  if (result.status !== 'success') {
    error.textContent = result.msg || 'Fehler beim Speichern.';
    return;
  }
  cancelUserForm();
  loadAdminUserList();
};


// ════════════════════════════════════════════════════════════════════════════
// BLOCK 4 — NUTZER-PANEL
// Geocoding, Routenberechnung, Adress-Autocomplete, Marker, Karten-Events
// ════════════════════════════════════════════════════════════════════════════

// [Elizat 8.3]

// ─── Geocoding ───────────────────────────────────────────────────────────────

// Sucht eine Adresse über die Nominatim-API und gibt lat/lon zurück
async function geocode(address) {
  const res = await fetch(`${window.NOMINATIM_URL}/search?q=${encodeURIComponent(address)}&format=json`);
  const data = await res.json();
  if (!data || data.length === 0) { alert('Adresse nicht gefunden'); return null; }
  return { lat: parseFloat(data[0].lat), lon: parseFloat(data[0].lon) };
}

// Reverse Geocoding: Koordinaten → Adresstext (für Klick auf Karte und Marker-Drag)
async function reverseGeocode(lat, lon) {
  try {
    const res = await fetch(`${window.NOMINATIM_URL}/reverse?lat=${lat}&lon=${lon}&format=json`);
    const data = await res.json();
    return data.display_name || null;
  } catch { return null; }
}

// Sucht ab 3 Zeichen Vorschläge über Nominatim und zeigt max. 5 Ergebnisse
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

// Übernimmt einen Vorschlag: setzt Koordinaten, Marker und Eingabefeld
// Startet automatisch Routenberechnung wenn beide Felder gefüllt sind
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

// Tauscht Start und Ziel (Koordinaten, Felder und Marker), berechnet Route neu
window.swapRoute = function () {
  const s = document.getElementById('start');
  const e = document.getElementById('end');
  [s.value, e.value] = [e.value, s.value];
  [startCoords, endCoords] = [endCoords, startCoords];
  [startMarker, endMarker] = [endMarker, startMarker];
  if (s.value && e.value) window.calculateRoute();
};

// ─── Routenberechnung ────────────────────────────────────────────────────────

// [Gregor 7.1]

// Formatiert Sekunden in lesbare Zeitangabe (z.B. "1 h 23 Min")
function formatDuration(seconds) {
  const mins = Math.round(seconds / 60);
  if (mins < 60) return `${mins} Min`;
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  return m > 0 ? `${h} h ${m} Min` : `${h} h`;
}

// Formatiert Meter in lesbare Distanzangabe (z.B. "1.4 km")
function formatDistance(meters) {
  if (meters < 1000) return `${Math.round(meters)} m`;
  return `${(meters / 1000).toFixed(1)} km`;
}

// Zeigt Distanz und geschätzte Fahrtzeiten für Auto, Rad und Fußgänger an
// Rad- und Gehzeit werden aus der Autozeit rechnerisch geschätzt (15 km/h, 5 km/h)
function showRouteInfo(distanceMeters, drivingSeconds) {
  const bikeSeconds = (distanceMeters / 1000) / 15 * 3600;
  const walkSeconds = (distanceMeters / 1000) / 5 * 3600;
  document.getElementById('route-info-distance').textContent = formatDistance(distanceMeters);
  document.getElementById('route-time-car').textContent = formatDuration(drivingSeconds);
  document.getElementById('route-time-bike').textContent = formatDuration(bikeSeconds);
  document.getElementById('route-time-walk').textContent = formatDuration(walkSeconds);
  document.getElementById('route-info').style.display = '';
}

// Berechnet Route über OSRM, zeichnet sie auf der Karte und prüft Sperrzonen
async function doRouting() {
  if (!startCoords || !endCoords) return;
  const url = `${window.OSRM_URL}/route/v1/driving/${startCoords.lon},${startCoords.lat};${endCoords.lon},${endCoords.lat}?overview=full&geometries=geojson`;
  const res = await fetch(url);
  const data = await res.json();
  if (!data.routes || data.routes.length === 0) { alert('Keine Route gefunden'); return; }
  const { geometry: route, distance, duration } = data.routes[0];
  // Alte Route und Konflikte entfernen bevor neue gezeichnet wird
  if (conflictLayer) { conflictLayer.remove(); conflictLayer = null; }
  if (routeLayer) { routeLayer.remove(); }
  routeLayer = L.geoJSON(route, { color: '#4a90e2', weight: 4 }).addTo(map);
  map.fitBounds(routeLayer.getBounds());
  showRouteInfo(distance, duration);
  document.getElementById('zone-warning').style.display = 'none';
  document.getElementById('zone-warning-modal').style.display = 'none';
  try {
    // Route gegen aktive Sperrzonen prüfen
    const result = await checkRoute(route);
    if (result?.status === 'WARNING') {
      const names = result.zones.map(z => z.name).join(', ');
      const msg = `⚠️ Route kreuzt Sperrzone: ${names}`;
      document.getElementById('zone-warning-text').textContent = msg;
      document.getElementById('zone-warning-text-small').textContent = msg;
      document.getElementById('zone-warning-modal').style.display = 'flex';
      // Konflikt-Abschnitte rot hervorheben
      const intersections = result.zones.map(z => z.intersection).filter(Boolean);
      if (intersections.length) {
        conflictLayer = L.layerGroup(intersections.map(g => L.geoJSON(g, { color: '#e05252', weight: 5 }))).addTo(map);
      }
    }
  } catch { /* Zonenprüfung fehlgeschlagen, Route wird trotzdem angezeigt */ }
}

// Geocodiert Start/Ziel falls nötig und startet dann die Routenberechnung
window.calculateRoute = async function () {
  const startInput = document.getElementById('start').value;
  const endInput = document.getElementById('end').value;
  if (!startInput || !endInput) return;
  if (!startCoords) {
    startCoords = await geocode(startInput);
    if (!startCoords) return;
    setRouteMarker(startCoords.lat, startCoords.lon, 'start');
  }
  if (!endCoords) {
    endCoords = await geocode(endInput);
    if (!endCoords) return;
    setRouteMarker(endCoords.lat, endCoords.lon, 'end');
  }
  await doRouting();
};

// Sendet die Route-Koordinaten an die API zur Sperrzonenprüfung
async function checkRoute(route) {
  const res = await fetch(`${window.API_BASE}/api/route/check`, {
    method: 'POST',
    headers: authHeaders({ 'Content-Type': 'application/json' }),
    // GeoJSON ist [lng, lat], API erwartet [lat, lng]
    body: JSON.stringify({ route: route.coordinates.map(([lng, lat]) => [lat, lng]) })
  });
  return (await res.json()).data;
}

// Schließt das Warn-Modal und zeigt die kleine Warnung in der Sidebar
window.dismissZoneWarning = function () {
  document.getElementById('zone-warning-modal').style.display = 'none';
  document.getElementById('zone-warning').style.display = 'flex';
};

// [Gregor 7.3]

// Fordert eine alternative Route an, die keine Sperrzonen kreuzt
window.requestAlternativeRoute = async function () {
  const btn = document.getElementById('alt-route-btn');
  const warningText = document.getElementById('zone-warning-text');
  btn.disabled = true;
  btn.textContent = 'Suche...';

  try {
    const res = await fetch(`${window.API_BASE}/api/route/alternative`, {
      method: 'POST',
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({
        start: [startCoords.lat, startCoords.lon],
        end:   [endCoords.lat,   endCoords.lon]
      })
    });
    const result = (await res.json()).data;

    if (result?.status === 'OK') {
      // Alternative Route grün anzeigen
      if (conflictLayer) { conflictLayer.remove(); conflictLayer = null; }
      if (routeLayer) routeLayer.remove();
      routeLayer = L.geoJSON(result.geometry, { color: '#27ae60', weight: 4 }).addTo(map);
      map.fitBounds(routeLayer.getBounds());
      showRouteInfo(result.distance, result.duration);
      document.getElementById('zone-warning-modal').style.display = 'none';
      document.getElementById('zone-warning').style.display = 'none';
    } else {
      warningText.textContent = '⚠️ Keine zonenfreie Alternative gefunden.';
    }
  } catch {
    warningText.textContent = '⚠️ Fehler bei der Routenberechnung.';
  } finally {
    btn.disabled = false;
    btn.textContent = 'Route umberechnen';
  }
};

// ─── Marker & Karten-Events ───────────────────────────────────────────────────

// Setzt einen verschiebbaren Start- oder Ziel-Marker
// Bei Drag-Ende: Koordinaten und Adressfeld aktualisieren, Route neu berechnen
function setRouteMarker(lat, lon, type) {
  if (type === 'start') {
    if (startMarker) startMarker.remove();
    startMarker = L.marker([lat, lon], { draggable: true }).addTo(map);
    startMarker.on('dragend', async function () {
      const pos = startMarker.getLatLng();
      startCoords = { lat: pos.lat, lon: pos.lng };
      const place = await reverseGeocode(pos.lat, pos.lng);
      if (place) document.getElementById('start').value = place;
      doRouting();
    });
  } else {
    if (endMarker) endMarker.remove();
    endMarker = L.marker([lat, lon], { draggable: true }).addTo(map);
    endMarker.on('dragend', async function () {
      const pos = endMarker.getLatLng();
      endCoords = { lat: pos.lat, lon: pos.lng };
      const place = await reverseGeocode(pos.lat, pos.lng);
      if (place) document.getElementById('end').value = place;
      doRouting();
    });
  }
}

// Klick auf die Karte: im Zeichenmodus → Vertex hinzufügen
// Im Nutzer-Panel → ersten Klick als Start, zweiten als Ziel setzen
map.on('click', async function (e) {
  const { lat, lng } = e.latlng;
  if (isDrawing) {
    drawVertices.push([lat, lng]);
    const m = L.circleMarker([lat, lng], { radius: 4, color: '#4a90e2', fillColor: '#4a90e2', fillOpacity: 1, weight: 1 }).addTo(map);
    drawMarkers.push(m);
    updateDrawPolyline();
    return;
  }
  if (document.getElementById('user-panel').style.display === 'none') return;
  const place = await reverseGeocode(lat, lng);
  if (!place) return;
  const startInput = document.getElementById('start');
  const endInput = document.getElementById('end');
  if (!startInput.value) {
    startInput.value = place;
    startCoords = { lat, lon: lng };
    setRouteMarker(lat, lng, 'start');
  } else {
    endInput.value = place;
    endCoords = { lat, lon: lng };
    setRouteMarker(lat, lng, 'end');
    doRouting();
  }
});

// Doppelklick im Zeichenmodus: letzten 2 Vertices entfernen
// (Doppelklick feuert zuerst 2× click, dann 1× dblclick → deshalb splice(-2))
map.on('dblclick', function () {
  if (isDrawing) {
    drawVertices.splice(-2, 2);
    drawMarkers.splice(-2).forEach(m => m.remove());
    updateDrawPolyline();
    if (drawVertices.length >= 3) finishDrawing();
  }
});


// ════════════════════════════════════════════════════════════════════════════
// BLOCK 5 — INITIALISIERUNG
// Seite laden, Theme & Session wiederherstellen
// ════════════════════════════════════════════════════════════════════════════

document.addEventListener('DOMContentLoaded', function () {
  // Gespeichertes Theme wiederherstellen
  const saved = localStorage.getItem('theme');
  if (saved) document.documentElement.setAttribute('data-theme', saved);

  // Falls noch ein Token vorhanden ist (z.B. nach Seiten-Reload): Session fortsetzen
  const token = localStorage.getItem('token');
  if (token) initSession(token);

  // Wenn der Nutzer das Eingabefeld ändert, Koordinaten zurücksetzen damit neu geocodiert wird
  document.getElementById('start').addEventListener('input', () => { startCoords = null; });
  document.getElementById('end').addEventListener('input', () => { endCoords = null; });

  positionSwapButton();
});

// Positioniert den Tausch-Button vertikal genau zwischen Start- und Ziel-Eingabe
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
