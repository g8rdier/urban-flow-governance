# Onboarding auf Windows — Urban Flow Governance

Dieser Leitfaden führt ein neues Teammmitglied schrittweise durch die Setup-Phase auf Windows.

---

## Phase 1: Notwendige Software installieren

### 1.1 Git installieren

1. Öffne https://git-scm.com/download/win
2. Lade **Git for Windows** herunter (64-bit, falls nicht anders nötig)
3. Installiere mit Standard-Optionen:
   - Editor: Visual Studio Code (default)
   - Line endings: "Checkout as-is, commit as-is" (wichtig für Kompatibilität)
4. **Terminal neustarten** nach Installation

Überprüfung:
```powershell
git --version
```
Sollte etwas wie `git version 2.45.0` ausgeben.

---

### 1.2 Visual Studio Code installieren

1. Öffne https://code.visualstudio.com
2. Lade **VS Code für Windows** herunter
3. Installiere mit Standard-Optionen

Nach Installation: **VS Code neustarten**

---

### 1.3 Docker Desktop installieren

Docker ist **essentiell** für den Dev Container.

1. Öffne https://www.docker.com/products/docker-desktop
2. Lade **Docker Desktop for Windows** herunter
3. Installiere und folge dem Setup-Wizard
4. **Computer neustarten**
5. Starte Docker Desktop (sollte nach Neustart auto-starten)

Überprüfung:
```powershell
docker --version
docker run hello-world
```

---

### 1.4 WSL2 konfigurieren (wichtig für Performance!)

WSL2 = Windows Subsystem for Linux. Das ist für beste Performance nötig.

#### WSL2 aktivieren:

1. Öffne PowerShell **als Administrator**
2. Führe aus:
```powershell
wsl --install -d Ubuntu-24.04
```
Das installiert Ubuntu 24.04 als WSL-Distribution.

3. Nach Installation: WSL-Terminal öffnen und Ubuntu-Passwort setzen
4. VSCode Erweiterung installieren:
   - VSCode öffnen
   - Extensions (Ctrl+Shift+X)
   - Suche: "Remote - WSL"
   - Installiere von Microsoft

#### Überprüfung:
```powershell
wsl --list --verbose
```
Sollte Ubuntu 24.04 mit Version 2 zeigen.

---

## Phase 2: Repository klonen

### 2.1 Arbeitsverzeichnis im WSL erstellen

**Wichtig**: Das Repo sollte im WSL-Dateisystem liegen, **nicht** unter `/mnt/c/...`

Öffne WSL-Terminal:
```bash
mkdir -p ~/projects
cd ~/projects
```

---

### 2.2 SSH-Key erzeugen

Du brauchst einen SSH-Key, um auf den Git-Server zuzugreifen.

```bash
ssh-keygen -t ed25519 -C "dein.email@example.com"
```

Drücke **Enter** für alle Fragen (Standard-Speicherort, kein Passwort).

Output sollte zeigen:
```
Your public key has been saved in /home/USERNAME/.ssh/id_ed25519.pub
```

---

### 2.3 Public Key zum Server hinzufügen

Kopiere deinen Public Key:
```bash
cat ~/.ssh/id_ed25519.pub
```

Markiere den gesamten Output (beginnt mit `ssh-ed25519`) und **kopiere**.

Schicke den Key an deinen Admin, damit er ihn auf den Git-Server hinzufügt. (Alternative: `ssh-copy-id` wenn du Zugriff hast)

---

### 2.4 Repository klonen

```bash
cd ~/projects
git clone ssh://DEINUSERNAME@iu-server:11422/media/sf_iu/git/elgreti/urban-flow-governance.git
cd urban-flow-governance
```

Falls es nach dem SSH-Key fragt: Verwende den Key den du gerade erzeugt hast.

---

### 2.5 Git-Identität lokal setzen

```bash
git config user.email "dein.email@example.com"
git config user.name "Dein Name"
```

Überprüfung:
```bash
git config --list
```

---

## Phase 3: VSCode mit Dev Container öffnen

### 3.1 Ordner in VSCode öffnen (über WSL)

1. Öffne VSCode
2. Klick: **File → Open Folder**
3. Navigiere zu `\\wsl$\Ubuntu-24.04\home\USERNAME\projects\urban-flow-governance`
   - Ersetze `USERNAME` mit deinem WSL-Benutzernamen
4. Klick: **Select Folder**

VSCode sollte die WSL-Umgebung erkennen (grünes Badge unten links: "WSL: Ubuntu-24.04")

---

### 3.2 Dev Container öffnen

1. VSCode Command Palette öffnen: `Ctrl+Shift+P`
2. Suche: "Dev Containers: Reopen in Container"
3. Klick und warte...

Das baut das Docker Image (~5-10 Min beim ersten Mal).

Du siehst: "Starting container..." → "Container started."

Danach öffnet sich das Terminal **im Container** (siehst du am Prompt).

---

### 3.3 Überprüfung: Alles läuft?

```bash
java -version
grails --version
```

Sollte ausgeben:
```
openjdk version "21.0.10" 2026-01-20
Grails Version: 6.1.1
```

---

## Phase 4: Datenbank konfigurieren

### 4.1 PostgreSQL lokal konfigurieren (optional für MVP)

Für lokale Entwicklung kannst du PostgreSQL in einem separaten Docker Container laufen lassen:

```bash
docker run --name postgres-urban-flow \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=urban_flow \
  -p 5432:5432 \
  -d postgis/postgis:latest
```

Danach setze in VSCode die Env-Variablen:
- **File → Preferences → Settings → Dev Containers**
- Suche: "containerEnv"
- Setze die PostgreSQL-Variablen

---

## Phase 5: Erstes Arbeiten im Repo

### 5.1 Repository erkunden

```bash
ls -la
git log --oneline -5
```

Zeigt die letzten 5 Commits.

---

### 5.2 Neue Feature Branch erstellen

Folge den **dev-best-practices** (Conventional Commits):

```bash
git checkout -b feat/my-first-feature
```

Branch-Namen: `feat/...`, `fix/...`, `docs/...`

---

### 5.3 Dokumentation lesen

Lese diese Dateien (in der Reihenfolge):
1. `README.md` — Projekt-Übersicht & Setup
2. `PROJECT_SPECIFICATION.md` — Was wird gebaut
3. `docs/USER_STORIES.md` — Anforderungen
4. `docs/ZONE_DATA_STRUCTURE.md` — Datenmodell

---

## Phase 6: Tipps & Troubleshooting

### VSCode Extensions (optional aber empfohlen)

Im Dev Container könnten diese nützlich sein:
- **GitLens** (git history)
- **REST Client** (API testen)
- **Groovy** (syntax highlighting)

### Häufige Probleme

**Problem: "Docker not running"**
- Starte Docker Desktop manuell (Windows Start → Docker Desktop)

**Problem: "WSL not found"**
- Überprüfe: `wsl --list --verbose`
- Falls nicht da: `wsl --install -d Ubuntu-24.04`

**Problem: "SSH connection refused"**
- Public Key wurde nicht auf Server hinzugefügt
- Kontaktiere Admin, um SSH Key freizugeben

**Problem: Container baut nicht**
- Versuche: `Dev Containers: Rebuild and Reopen in Container`
- Oder: `docker system prune` (räumt alte Images auf)

---

## Checklist: Fertig?

- [ ] Git installiert & configured
- [ ] VSCode installiert
- [ ] Docker Desktop läuft
- [ ] WSL2 aktiviert (Ubuntu 24.04)
- [ ] Repository geklont in `~/projects/`
- [ ] SSH-Key erzeugt und auf Server hinzugefügt
- [ ] VSCode öffnet Repo im Dev Container
- [ ] `java -version` & `grails --version` funktioniert
- [ ] README, PROJECT_SPECIFICATION gelesen
- [ ] Erste Feature Branch erstellt

---

## Nächste Schritte

Nach erfolgreichem Onboarding:
1. **Anforderungen verstehen**: USER_STORIES.md durchlesen
2. **Erste Task**: Einen Issue/Story aus dem Backlog auswählen
3. **Feature Branch**: `git checkout -b feat/assigned-story`
4. **Implementieren**: Code schreiben
5. **Commit & Push**: Nach dev-best-practices
6. **Pull Request**: Auf GitHub/GitLab erstellen
7. **Review**: Team-Feedback einholen
8. **Merge**: Nach Approval

---

## Fragen?

- **Setup-Probleme**: Kontaktiere den Admin
- **Git-Fragen**: Siehe `git --help` oder https://git-scm.com
- **Projekt-Fragen**: Frag das Team in der Slack/Discord
