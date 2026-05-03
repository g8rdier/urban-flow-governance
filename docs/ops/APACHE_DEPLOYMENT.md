# Apache Deployment (Dev Container) – Urban Flow Governance

## Überblick

Das Frontend wird über Apache als statische Website ausgeliefert.
Alle API-Requests (`/api/...`) werden über Apache an das Backend weitergeleitet.

---

## Voraussetzungen

* Dev Container läuft
* Apache ist installiert
* Projektstruktur:

```
urban-flow-governance/
├── frontend/
│   ├── index.html
│   ├── app.js
│   └── style.css
```

---

## 1. Apache starten

```bash
sudo service apache2 start
```

### Apache stoppen

```bash
sudo service apache2 stop
```

### Apache neu starten

```bash
sudo service apache2 restart
```

---

## 2. Frontend deployen

### Frontend in Apache kopieren

```bash
sudo cp -r frontend/* /var/www/html/
```

---

## Apache Web-Verzeichnis

```bash
/var/www/html/
```

Hier müssen liegen:

```
index.html
app.js
style.css
```

---

## 3. Änderungen übernehmen

Nach Änderungen:

```bash
sudo cp -r frontend/* /var/www/html/
sudo service apache2 restart
```

---

## 4. Anwendung öffnen

Im Browser:

```
http://localhost
```

---

## 5. Backend anbinden (Reverse Proxy)

Damit `/api` funktioniert:

### Apache Config öffnen

```bash
sudo nano /etc/apache2/sites-available/000-default.conf
```

---

### Diese Zeilen hinzufügen:

```apache
ProxyPass /api http://localhost:8080/api
ProxyPassReverse /api http://localhost:8080/api
```

---

### Module aktivieren

```bash
sudo a2enmod proxy
sudo a2enmod proxy_http
```

---

### Apache neu starten

```bash
sudo service apache2 restart
```

---

##  6. Test

Browser öffnen:

```
http://localhost
```

API testen (z. B. im Browser oder Console):

```
http://localhost/api/zones
```

---

## Nützliche Befehle

### Dateien prüfen

```bash
ls /var/www/html/
```

---

### Logs anzeigen

```bash
tail -f /var/log/apache2/error.log
```

---

## Troubleshooting

| Problem             | Ursache            | Lösung                  |
| ------------------- | ------------------ | ----------------------- |
| Permission denied   | keine Rechte       | `sudo` benutzen         |
| Alte Seite sichtbar | Browser Cache      | `Strg + Shift + R`      |
| 404 Fehler          | Dateien fehlen     | `/var/www/html` prüfen  |
| API geht nicht      | Proxy fehlt        | Config prüfen           |
| Seite lädt nicht    | Apache läuft nicht | `service apache2 start` |

---

## Dev Container Hinweis

| Pfad            | Bedeutung      |
| --------------- | -------------- |
| `/workspace`    | Projekt        |
| `/var/www/html` | Apache Webroot |

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

