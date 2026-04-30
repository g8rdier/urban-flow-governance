import { zones } from './data/mock.js';

// Karte erstellen
const map = L.map('map').setView([52.52, 13.40], 13);

// 🗺️ Tiles
L.tileLayer('https://gis.servicecluster.de/de_tiles/{z}/{x}/{y}.png', {
  maxZoom: 20
}).addTo(map);

// Zonen anzeigen
window.drawZones = function () {
  zones.forEach(zone => {
    L.polygon(zone.polygon, { color: 'orange' }).addTo(map);
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

// Routing
async function getRoute(start, end) {
  const url = `https://osrm.servicecluster.de/route/v1/driving/${start.lon},${start.lat};${end.lon},${end.lat}?overview=full&geometries=geojson`;

  const res = await fetch(url);
  const data = await res.json();

  if (!data.routes || data.routes.length === 0) {
    alert("Keine Route gefunden");
    return null;
  }

  return data.routes[0].geometry;
}

// Route zeichnen
window.drawRoute = async function () {

  console.log("Route gestartet");

  const start = await geocode("Berlin Alexanderplatz");
  const end = await geocode("Berlin Hauptbahnhof");

  if (!start || !end) return;

  const route = await getRoute(start, end);

  if (!route) return;

  const layer = L.geoJSON(route, { color: 'green' }).addTo(map);
  map.fitBounds(layer.getBounds());
};
