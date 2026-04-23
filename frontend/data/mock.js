export const zones = [
  {
    id: 1,
    name: "Marathon Zone",
    status: "ACTIVE",
    polygon: [
      [52.52, 13.40],
      [52.53, 13.40],
      [52.53, 13.41],
      [52.52, 13.41]
    ]
  }
];

async function loadZones() {
  const res = await fetch("http://localhost:8080/zones");
  const zones = await res.json();

  zones.forEach(zone => {
    L.polygon(zone.polygon).addTo(map);
  });
}