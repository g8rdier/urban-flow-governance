---

# 🌐 Apache Deployment - KISS Anleitung

## 🚀 Schnellstart

### 1. Apache starten

```bash
sudo service apache2 start
```

Apache neu starten:

```bash
sudo service apache2 restart
```

Apache stoppen:

```bash
sudo service apache2 stop
```

👉 Web-Interface:
**[http://localhost](http://localhost)**

---

## 📦 Schritt 1: Frontend vorbereiten

Dein Frontend sollte so aussehen:

```text
frontend/
  index.html
  app.js
  style.css
  /data
```

👉 Wichtig:
✔ alles funktioniert lokal (z. B. mit Live Server)

---

## 📤 Schritt 2: Frontend nach Apache kopieren

```bash
sudo cp -r frontend/* /var/www/html/
```

👉 Erklärung:

* `/var/www/html` = Standard-Webordner von Apache
* alles darin wird im Browser angezeigt

---

## 🌍 Schritt 3: Zugriff im Browser

```text
http://localhost
```

👉 Du solltest sehen:

✔ deine Karte
✔ dein Frontend

---

## 🔄 Schritt 4: Änderungen aktualisieren

Nach Änderungen:

```bash
sudo cp -r frontend/* /var/www/html/
sudo service apache2 restart
```

👉 dann im Browser:

```text
Strg + Shift + R
```

(Force Reload)

---

## 🔗 Schritt 5: Backend Verbindung (/api)

👉 Wichtig für dein Projekt:

Frontend ruft:

```text
/api/...
```

👉 Apache muss das weiterleiten (Reverse Proxy)

---

## ⚙️ Apache Config (Reverse Proxy)

Datei öffnen:

```bash
sudo nano /etc/apache2/sites-available/000-default.conf
```

👉 Ergänzen:

```apache
ProxyPreserveHost On

ProxyPass /api http://localhost:8080/api
ProxyPassReverse /api http://localhost:8080/api
```

---

## 🔄 Apache neu starten

```bash
sudo service apache2 restart
```

---

## 🧪 Test

Browser:

```text
http://localhost/api/zones
```

👉 Wenn JSON kommt → ✔ funktioniert

---

## ⚙️ Nützliche Kommandos

**Frontend neu deployen:**

```bash
sudo cp -r frontend/* /var/www/html/
```

---

**Apache Status prüfen:**

```bash
sudo service apache2 status
```

---

**Logs ansehen:**

```bash
tail -f /var/log/apache2/error.log
```

---

**Port prüfen:**

```bash
sudo lsof -i :80
```

---

## 🔧 Dev-Container spezifisch

```bash
# Apache starten
sudo service apache2 start

# Frontend kopieren
sudo cp -r /workspace/frontend/* /var/www/html/
```

---

## ✅ Troubleshooting

| Problem                       | Lösung                                   |
| ----------------------------- | ---------------------------------------- |
| **Seite leer**                | index.html vorhanden in `/var/www/html`? |
| **CSS/JS fehlt**              | Pfade korrekt?                           |
| **API funktioniert nicht**    | Proxy config prüfen                      |
| **403 Fehler**                | Rechte prüfen (`chmod`)                  |
| **Änderungen nicht sichtbar** | Browser Cache leeren                     |

---

# 🧠 Mini-Merksatz

```text
Apache = zeigt dein Frontend
/api → wird an Backend weitergeleitet
```

---
