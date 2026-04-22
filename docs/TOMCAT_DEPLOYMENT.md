# Tomcat Deployment - KISS Anleitung

## 🚀 Schnellstart

### 1. Tomcat starten

```bash
cd /opt/tomcat
# oder je nach Installation:
/opt/tomcat/bin/startup.sh
```

Web-Interface: **http://localhost:8080**

Tomcat stoppen:
```bash
/opt/tomcat/bin/shutdown.sh
```

---

## 📦 Schritt 1: WAR-Datei erstellen

Im `ufg-app/` Verzeichnis:

```bash
cd ufg-app
./gradlew war
```

Die WAR-Datei befindet sich danach hier:
```
ufg-app/build/libs/ufg-app-0.1.war
```

---

## 📤 Schritt 2: WAR auf Tomcat deployen

### Option A: Manuell per Dateimanager

1. WAR-Datei kopieren:
   ```bash
   cp ufg-app/build/libs/ufg-app-0.1.war /opt/tomcat/webapps/
   ```

2. Tomcat startet automatisch und entpackt die WAR
3. Zugriff: **http://localhost:8080/ufg-app-0.1**

### Option B: Manager GUI (wenn verfügbar)

1. Öffne **http://localhost:8080/manager/html**
2. Login mit Tomcat-Benutzerdaten
3. Datei hochladen und deployen

---

## ⚙️ Nützliche Kommandos

**WAR erstellen und sofort deployen:**
```bash
cd ufg-app
./gradlew war && cp build/libs/ufg-app-0.1.war /opt/tomcat/webapps/
```

**Tomcat-Logs prüfen:**
```bash
tail -f /opt/tomcat/logs/catalina.out
```

**WAR entfernen:**
```bash
rm /opt/tomcat/webapps/ufg-app-0.1.war
# Tomcat entpackte Verzeichnis wird auch gelöscht
```

---

## 🔧 Dev-Container spezifisch

Im Dev-Container sind Tomcat und Gradle bereits installiert:

```bash
# In den Container gehen
docker exec -it <container-name> bash

# Dann:
cd /workspace/ufg-app
./gradlew war
cp build/libs/ufg-app-0.1.war /opt/tomcat/webapps/

# Starten falls nicht laufen:
/opt/tomcat/bin/startup.sh
```

---

## ✅ Troubleshooting

| Problem | Lösung |
|---------|--------|
| **404 beim Zugriff** | WAR im korrekten `/webapps/` Ordner? |
| **Tomcat läuft nicht** | Port 8080 schon belegt? `sudo lsof -i :8080` |
| **Permissions-Fehler** | `chmod +x /opt/tomcat/bin/*.sh` |
| **Alte Version lädt** | Browser-Cache leeren (Ctrl+Shift+Delete) |

