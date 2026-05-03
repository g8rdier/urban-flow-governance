---

## Teil 1: Architektur & Infrastruktur

### 1. Delivery & Proxy-Konzept
Das Frontend wird als **statische Webapplikation** (HTML, JS, CSS) über einen Apache Webserver ausgeliefert. Um Cross-Origin Resource Sharing (CORS) Probleme zu vermeiden, agiert der Apache als **Reverse Proxy**.

*   **URL:** `http://localhost` bedient die statischen Dateien.
*   **API-Weiterleitung:** Alle Anfragen an `/api/*` werden intern an das Backend (Tomcat/Grails auf Port 8080) weitergereicht.

**Apache Konfigurations-Snippet:**
```apache
ProxyPass /api http://localhost:8080/api
ProxyPassReverse /api http://localhost:8080/api
```

### 2. Authentifizierung (JWT/Token-Handling)
Das System nutzt eine zustandslose Token-Authentifizierung.

*   **Speicherung:** Das vom Backend erhaltene Token wird im `localStorage` gespeichert.
*   **Übertragung:** Bei jedem Request muss das Token im HTTP-Header mitgeschickt werden:
    `Authorization: Bearer <dein_token>`
*   **Session-Ablauf:** Empfängt das Frontend einen `401 Unauthorized` Fehler, muss der `localStorage` bereinigt und der User zum Login umgeleitet werden.

---

## Teil 2: Feature-Implementierung

### 1. Karten-Integration (Leaflet)
Die Karte ist das zentrale Element. Sie visualisiert drei Informationsebenen:
1.  **Basiskarte:** OpenStreetMap-Tiles.
2.  **Sperrzonen:** Polygone, die über `GET /api/zones` geladen werden.
3.  **Routen:** Polylines, deren Farbe sich nach dem Prüfergebnis richtet.



### 2. Sperrzonen-Management (Admin)
Admins nutzen das Plugin **Leaflet.Draw**, um neue Zonen zu definieren.
*   **Ablauf:** Zeichnen → GeoJSON extrahieren → Metadaten (Name, Zeit) hinzufügen → `POST /api/zones`.
*   **Format:** Das Backend erwartet die Geometrie im Standard-GeoJSON-Format.

### 3. Der Routing-Workflow (User)
Dies ist der komplexeste Prozess im Frontend. Er erfolgt in vier Schritten:

| Schritt | Aktion | Ziel/Endpunkt |
| :--- | :--- | :--- |
| **1. Suche** | Start/Ziel über Nominatim oder Kartenklick | Koordinaten (`lat`, `lon`) |
| **2. Routing** | Wegstrecke berechnen | OSRM API |
| **3. Validierung** | Route auf Sperrzonen prüfen lassen | `POST /api/route/check` |
| **4. Feedback** | Anzeige der Route in **Grün** (OK) oder **Rot** (Konflikt) | Leaflet Map |

**Code-Logik für die Prüfung:**
```javascript
// Beispiel für den API-Check nach dem OSRM-Call
const validateRoute = async (coordinates) => {
    const response = await fetch('/api/route/check', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ route: coordinates }) // Array von [lat, lng]
    });
    const result = await response.json();
    
    if (result.data.status === 'WARNING') {
        renderRoute(coordinates, 'red');
        showWarning(result.data.zones); // Liste der blockierenden Zonen anzeigen
    } else {
        renderRoute(coordinates, 'green');
    }
};
```

### 4. Adresssuche & Geocoding
Um US-4.1 und US-4.2 zu erfüllen, bietet das Frontend zwei Einstiegspunkte:
*   **Nominatim API:** Verwandelt Texteingaben (z.B. "Marktplatz 1") in Koordinaten.
*   **Reverse Geocoding:** Ein Klick auf die Karte nutzt `e.latlng`, um den Punkt direkt zu setzen.


---

## 🛠️ API-Referenz für Entwickler (Cheat Sheet)

| Kategorie | Methode | Endpunkt | Funktion |
| :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/api/session` | Login (erhält Token) |
| **Zonen** | `GET` | `/api/zones` | Alle aktiven Sperrzonen laden |
| **Zonen** | `POST` | `/api/zones` | Neue Zone speichern (Admin) |
| **Route** | `POST` | `/api/route/check` | Prüft Route gegen Datenbank |
| **User** | `GET` | `/api/users` | Benutzerliste verwalten (Admin) |

Diese Dokumentation dient als Leitfaden für die Implementierung der UI-Logik und stellt sicher, dass das "Smart-Frontend"-Prinzip (Darstellung im Frontend, Logik im Backend) eingehalten wird.