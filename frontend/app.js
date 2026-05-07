
// Karte erstellen
const map = L.map('map').setView([48.137, 11.576], 13);

// 🗺️ Tiles
L.tileLayer(window.TILES_URL, {
  maxZoom: 20
}).addTo(map);

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

  localStorage.setItem('token', result.data.token);
  document.getElementById('login-overlay').style.display = 'none';
  loadZones();
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

  // Auto-login after successful registration
  const loginRes = await fetch(`${window.API_BASE}/api/session`, {
    method: 'POST',
    body: new URLSearchParams({ username, password })
  });
  const loginResult = await loginRes.json();

  localStorage.setItem('token', loginResult.data.token);
  document.getElementById('login-overlay').style.display = 'none';
  loadZones();
};

import { mockZones } from "./data/mock.js";

function authHeaders(extra = {}) {
  const token = localStorage.getItem("token");
  return { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...extra };
}

// Zonen laden

window.loadZones = async function () {
  console.log("Lade Zonen...");

  const zoneColors = { PLANNED: "blue", ACTIVE: "red", EXPIRED: "gray" };

  let zones;
  try {
    const res = await fetch(`${window.API_BASE}/api/zones`, { headers: authHeaders() });
    const result = await res.json();
    zones = result.data.zones;
  } catch (e) {
    console.warn("API nicht erreichbar, nutze Mock-Daten");
    zones = mockZones;
  }

  zones.forEach(zone => {
    const coords = zone.geometry.coordinates[0].map(([lng, lat]) => [lat, lng]);
    const color = zoneColors[zone.status] ?? "orange";
    L.polygon(coords, { color }).addTo(map);
  });
};

// Geocoding
async function geocode(address) {
  const url = `${window.NOMINATIM_URL}/search?q=${encodeURIComponent(address)}&format=json`;

  const res = await fetch(url);
  const data = await res.json();

  if (!data || data.length === 0) {
    alert("Adresse nicht gefunden");
    return null;
  }

  return {
    lat: parseFloat(data[0].lat),
    lon: parseFloat(data[0].lon)
  };
}

// Route berechnen
window.calculateRoute = async function () {
  const startInput = document.getElementById("start").value;
  const endInput = document.getElementById("end").value;

  if (!startInput || !endInput) return;

  startCoords = await geocode(startInput);
  if (!startCoords) return;
  setRouteMarker(startCoords.lat, startCoords.lon, "start");

  endCoords = await geocode(endInput);
  if (!endCoords) return;
  setRouteMarker(endCoords.lat, endCoords.lon, "end");

  const url = `${window.OSRM_URL}/route/v1/driving/${startCoords.lon},${startCoords.lat};${endCoords.lon},${endCoords.lat}?overview=full&geometries=geojson`;

  const res = await fetch(url);
  const data = await res.json();

  if (!data.routes || data.routes.length === 0) {
    alert("Keine Route gefunden");
    return;
  }

  const route = data.routes[0].geometry;

  // Route zeichnen
  const layer = L.geoJSON(route, { color: "green" }).addTo(map);

  // Zoom auf Route
  map.fitBounds(layer.getBounds());
}

// Route Check
async function checkRoute(route) {
  const res = await fetch(`${window.API_BASE}/api/route/check`, {
    method: "POST",
    headers: authHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify({
      route: route.coordinates.map(([lng, lat]) => [lat, lng])
    })
  });

  const result = await res.json();
  return result.data;
}

// Route zeichnen (mit Zonen-Check)
window.drawRoute = async function () {
  const startInput = document.getElementById("start").value;
  const endInput = document.getElementById("end").value;

  const start = await geocode(startInput);
  const end = await geocode(endInput);
  if (!start || !end) return;

  const url = `${window.OSRM_URL}/route/v1/driving/${start.lon},${start.lat};${end.lon},${end.lat}?overview=full&geometries=geojson`;
  const res = await fetch(url);
  const data = await res.json();

  if (!data.routes || data.routes.length === 0) {
    alert("Keine Route gefunden");
    return;
  }

  const route = data.routes[0].geometry;
  const result = await checkRoute(route);

  let color = "green";
  if (result.status === "WARNING") {
    color = "red";
    alert("⚠️ Route kreuzt Sperrzone!");
  }

  L.geoJSON(route, { color }).addTo(map);
};
//

// 🗺️ 1. Klick auf Karte
// → User klickt auf Karte
// → Marker wird gesetzt
// → Koordinaten werden ausgegeben

let selectionMarker = null;
let startMarker = null;
let endMarker = null;

function setSelectionMarker(lat, lon) {
  if (selectionMarker) selectionMarker.remove();
  selectionMarker = L.marker([lat, lon]).addTo(map);
}

function setRouteMarker(lat, lon, type) {
  if (type === "start") {
    if (startMarker) startMarker.remove();
    startMarker = L.marker([lat, lon]).addTo(map);
  } else {
    if (endMarker) endMarker.remove();
    endMarker = L.marker([lat, lon]).addTo(map);
  }
}

map.on('click', function (e) {
  const lat = e.latlng.lat;
  const lon = e.latlng.lng;

  console.log("Klick:", lat, lon);

  setSelectionMarker(lat, lon);
});


// 🔍 2. Autocomplete (Adresssuche)
// → User tippt in Input
// → Vorschläge werden von Nominatim geladen
// → Liste wird angezeigt

window.searchAddress = async function (type) {
  const query = document.getElementById(type).value;

  if (query.length < 3) return;

  const res = await fetch(
    `${window.NOMINATIM_URL}/search?q=${encodeURIComponent(query)}&format=json`
  );

  const data = await res.json();

  const list = document.getElementById(`suggestions-${type}`);
  list.innerHTML = "";

  data.slice(0, 5).forEach(place => {
    const li = document.createElement("li");
    li.innerText = place.display_name;
    li.style.cursor = "pointer";

    li.onclick = () => selectAddress(place, type);

    list.appendChild(li);
  });
};
// 3. Vorschlag auswählen
// → User klickt auf Vorschlag
// → Marker wird gesetzt
// → Karte zoomt
// → Input wird aktualisiert

let startCoords = null;
let endCoords = null;

function selectAddress(place, type) {
  const lat = parseFloat(place.lat);
  const lon = parseFloat(place.lon);

  setRouteMarker(lat, lon, type);
  map.setView([lat, lon], 15);

  if (type === "start") {
    startCoords = { lat, lon };
    document.getElementById("start").value = place.display_name;
  } else {
    endCoords = { lat, lon };
    document.getElementById("end").value = place.display_name;
  }

  document.getElementById(`suggestions-${type}`).innerHTML = "";

  const otherType = type === "start" ? "end" : "start";
  if (document.getElementById(otherType).value) {
    window.calculateRoute();
  }
}

// Dark/Light Mode
window.toggleTheme = function () {
  const html = document.documentElement;
  const next = html.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
  html.setAttribute('data-theme', next);
  localStorage.setItem('theme', next);
};

document.addEventListener('DOMContentLoaded', function () {
  const saved = localStorage.getItem('theme');
  if (saved) document.documentElement.setAttribute('data-theme', saved);

  if (localStorage.getItem('token')) {
    document.getElementById('login-overlay').style.display = 'none';
    loadZones();
  }
});

// Swap-Button exakt zwischen den beiden Inputs positionieren
function positionSwapButton() {
  const startInput = document.getElementById('start');
  const endInput = document.getElementById('end');
  const btn = document.getElementById('swap-btn');
  const container = document.getElementById('route-inputs');

  const containerTop = container.getBoundingClientRect().top;
  const midY = (startInput.getBoundingClientRect().bottom + endInput.getBoundingClientRect().top) / 2;
  btn.style.top = (midY - containerTop) + 'px';
}

document.addEventListener('DOMContentLoaded', positionSwapButton);
window.addEventListener('resize', positionSwapButton);

// Start / Ziel tauschen
window.swapRoute = function () {
  const startInput = document.getElementById("start");
  const endInput = document.getElementById("end");

  [startInput.value, endInput.value] = [endInput.value, startInput.value];
  [startCoords, endCoords] = [endCoords, startCoords];
  [startMarker, endMarker] = [endMarker, startMarker];

  if (startInput.value && endInput.value) {
    window.calculateRoute();
  }
};



// 4. Manuelle Suche (Button)
// → User klickt auf Button
// → Geocode wird ausgeführt
// → Marker wird gesetzt

window.testGeocode = async function () {
  const address = document.getElementById("address").value;

  console.log("Adresse:", address);

  const result = await geocode(address);

  console.log("Result:", result);

  if (!result) return;

  setSelectionMarker(result.lat, result.lon);
  map.setView([result.lat, result.lon], 15);
};

