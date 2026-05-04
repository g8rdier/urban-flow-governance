
// Karte erstellen
const map = L.map('map').setView([52.52, 13.40], 13);

// 🗺️ Tiles
L.tileLayer('https://gis.servicecluster.de/de_tiles/{z}/{x}/{y}.png', {
  maxZoom: 20
}).addTo(map);

// Login Seite
window.login = async function () {
  const res = await fetch("/api/session", {
    method: "POST",
    body: new URLSearchParams({
      username: "admin",
      password: "adminpass"
    })
  });

  const result = await res.json();

  localStorage.setItem("token", result.data.token);
  alert("Login erfolgreich!");
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
    const res = await fetch("/api/zones", { headers: authHeaders() });
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
  const url = `https://nominatim.servicecluster.de/search?q=${encodeURIComponent(address)}&format=json`;

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

// Route holen
async function getRoute(start, end) {
  const res = await fetch(
    `https://osrm.servicecluster.de/route/v1/driving/${start.lon},${start.lat};${end.lon},${end.lat}?overview=full&geometries=geojson`
  );

  const data = await res.json();
  return data.routes[0].geometry;
}

// Route Check
async function checkRoute(route) {
  const res = await fetch("/api/route/check", {
    method: "POST",
    headers: authHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify({
      route: route.coordinates.map(([lng, lat]) => [lat, lng])
    })
  });

  const result = await res.json();
  return result.data;
}

// Route zeichnen
window.drawRoute = async function () {

  const startInput = document.getElementById("start").value;
  const endInput = document.getElementById("end").value;

  const start = await geocode(startInput);
  const end = await geocode(endInput);

  const route = await getRoute(start, end);

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

function setSelectionMarker(lat, lon) {
  if (selectionMarker) selectionMarker.remove();
  selectionMarker = L.marker([lat, lon]).addTo(map);
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
    `https://nominatim.servicecluster.de/search?q=${encodeURIComponent(query)}&format=json`
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

  setSelectionMarker(lat, lon);
  map.setView([lat, lon], 15);

  if (type === "start") {
    startCoords = { lat, lon };
    document.getElementById("start").value = place.display_name;
  } else {
    endCoords = { lat, lon };
    document.getElementById("end").value = place.display_name;
  }

  document.getElementById(`suggestions-${type}`).innerHTML = "";
}

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
