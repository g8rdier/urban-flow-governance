# Apache Deployment (Servicecluster) – Urban Flow Governance

## Überblick

Das Frontend wird über Apache als statische Website auf dem Servicecluster ausgeliefert.
Alle API-Requests (`/api/...`) werden über Apache an das Backend weitergeleitet.

---

## Voraussetzungen

* Servicecluster-Zugang vorhanden
* Apache ist eingerichtet
* Projektstruktur:

```text
urban-flow-governance/
├── frontend/
│   ├── index.html
│   ├── app.js
│   ├── style.css
│   └── data/
```

---

## 1. Frontend deployen

### Frontend auf Servicecluster kopieren

```bash
scp -r -P 11422 frontend/* mairambekkyzy@iu.servicecluster.de:/media/sf_iu/daps-ss26/elgreti/webclient/.
```

---

## 2. Auf Servicecluster verbinden

```bash
```

---

## 3. Deployment prüfen

### Zum Webclient-Verzeichnis wechseln

```bash
cd /media/sf_iu/daps-ss26/elgreti/webclient
```

### Dateien anzeigen

```bash
ls
```

Hier müssen liegen:

```text
index.html
app.js
style.css
config.js
mock.js
```

---

## 4. Anwendung öffnen

Im Browser:

```text
https://elgreti.servicecluster.de/
```

---

## 5. Änderungen übernehmen

Nach Frontend-Änderungen erneut deployen:

```bash
scp -r -P 11422 frontend/* mairambekkyzy@iu.servicecluster.de:/media/sf_iu/daps-ss26/elgreti/webclient/.
```

Browser-Cache neu laden:

```text
Strg + Shift + R
```

---

## 6. Backend starten (Grails)

Im Projektordner:

```bash
cd ufg-app
./gradlew bootRun
```

Backend erreichbar unter:

```text
http://localhost:8080
```

---

## 7. API testen

### Zonen abrufen

```bash
curl http://localhost:8080/api/zones
```

---

### Route prüfen

```bash
curl -X POST http://localhost:8080/api/route/check \
  -H "Content-Type: application/json" \
  -d '{"route":[[52.5,13.4],[52.6,13.5]]}'
```

---

## 8. Backend anbinden (Reverse Proxy)

Damit `/api` funktioniert:

### Apache-Konfiguration

```apache
ProxyPass /api http://localhost:8080/api
ProxyPassReverse /api http://localhost:8080/api
```

Dadurch kann das Frontend einfach verwenden:

```js
fetch("/api/zones")
```

ohne direkte Tomcat-URL.

---

## 9. Nützliche Befehle

### Aktuelles Verzeichnis anzeigen

```bash
pwd
```

---

### Dateien anzeigen

```bash
ls
```

---

### Logs anzeigen

```bash
tail -f /var/log/apache2/error.log
```

---

## Troubleshooting

| Problem             | Ursache               | Lösung                       |
| ------------------- | --------------------- | ---------------------------- |
| Permission denied   | keine Rechte          | richtigen User / Pfad prüfen |
| Alte Seite sichtbar | Browser Cache         | `Strg + Shift + R`           |
| 404 Fehler          | Dateien fehlen        | `webclient/` prüfen          |
| API geht nicht      | Proxy fehlt           | Apache Config prüfen         |
| Seite lädt nicht    | Dateien nicht kopiert | `scp` erneut ausführen       |

---

## Servicecluster Hinweis

| Pfad                                       | Bedeutung                |
| ------------------------------------------ | ------------------------ |
| `/media/sf_iu/daps-ss26/elgreti/webclient` | Apache Webclient         |
| `frontend/`                                | Lokales Frontend-Projekt |
| `http://localhost:8080`                    | Grails/Tomcat Backend    |

---
# Backend & API Testing – Urban Flow Governance

## Überblick

Diese Anleitung zeigt:

* wie das Backend gestartet wird
* wie überprüft wird, ob es läuft
* wie API-Endpunkte getestet werden

---

## 1. Backend starten

Im Dev Container Terminal:

```bash
cd ufg-app
./gradlew bootRun
```

---

## Erfolgreicher Start

Wenn alles funktioniert, erscheint:

```text
Grails application running at http://localhost:8080
```

Das bedeutet:

* Backend läuft
* API ist erreichbar

---

## Wichtig

Solange `bootRun` läuft:

```text
👉 Terminal ist blockiert
```

👉 Für Tests brauchst du ein **zweites Terminal**

---

## Neues Terminal öffnen

In VS Code:

```text
Terminal → New Terminal
```

---

## 2. API testen (Backend direkt)

### Test mit Browser

```text
http://localhost:8080/api/zones
```

---

### Test mit curl

```bash
curl http://localhost:8080/api/zones
```

---

## Erwartetes Ergebnis

```json
{
  "status": "success",
  "data": {
    "zones": [...]
  }
}
```

---

## 3. Login testen

```bash
curl -X POST http://localhost:8080/api/session \
  -d "username=admin&password=adminpass"
```

---

### Antwort:

```json
{
  "data": {
    "token": "abc123"
  }
}
```

---

## 4. Token verwenden

```bash
curl http://localhost:8080/api/zones \
  -H "Authorization: Bearer TOKEN"
```

---

##  5. Logout testen

```bash
curl -X DELETE http://localhost:8080/api/session \
  -H "Authorization: Bearer TOKEN"
```

---

## 6. Test über Apache (Proxy)

Wenn Apache Proxy aktiv ist:

```text
http://localhost/api/zones
```

---

 Unterschied:

| URL            | Bedeutung      |
| -------------- | -------------- |
| localhost:8080 | direkt Backend |
| localhost/api  | über Apache    |

---

##  Nützliche Befehle

### Backend stoppen

```text
CTRL + C
```

---

### Port prüfen

```bash
lsof -i :8080
```

---

## ❗ Troubleshooting

| Problem         | Ursache             | Lösung                |
| --------------- | ------------------- | --------------------- |
| curl hängt      | falsches Terminal   | neues Terminal öffnen |
| 404 Fehler      | Endpoint fehlt      | Backend prüfen        |
| nichts passiert | Backend läuft nicht | bootRun starten       |
| 502 Fehler      | Proxy falsch        | Apache Config prüfen  |

---

## Dev Container Hinweis

| Umgebung      | Zweck            |
| ------------- | ---------------- |
| Dev Container | Backend + Apache |
| Browser       | Anzeige          |
| curl          | API testen       |

---

## Zusammenfassung

1. Backend starten (`bootRun`)
2. Neues Terminal öffnen
3. API mit curl oder Browser testen
4. Proxy testen (`/api/...`)

---

