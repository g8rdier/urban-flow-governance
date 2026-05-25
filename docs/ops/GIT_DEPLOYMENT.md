# Automatisches Deployment

Deployment via server-seitigem `post-receive` Git Hook — kein Ansible, kein externer CI-Dienst.

## Ablauf

```
git push origin main
  → post-receive Hook (Server)
    → geänderte Pfade auswerten
      → frontend/** → Frontend nach Apache kopieren
      → ufg-app/** oder openapi.yaml → Grails WAR bauen und nach Tomcat deployen
```

Nur die tatsächlich geänderten Teile werden deployt. Ein reines Frontend-Update baut kein WAR, ein reines Backend-Update kopiert keine Frontend-Dateien.

## post-receive Hook

Liegt auf dem Server unter `/media/sf_iu/git/elgreti/urban-flow-governance.git/hooks/post-receive`.

Prüft ob `main` gepusht wurde, wertet mit `git log --name-only` die geänderten Pfade aus und führt nur die notwendigen Deploy-Schritte aus.

### Frontend deployen

Wird ausgelöst wenn Dateien unter `frontend/` geändert wurden.

```sh
cp -r "$WORK_DIR/frontend/." /media/sf_iu/daps-ss26/elgreti/webclient/
```

### Backend deployen

Wird ausgelöst wenn Dateien unter `ufg-app/` oder `openapi.yaml` geändert wurden.

```sh
cd "$WORK_DIR/ufg-app"
./gradlew war -q
cp build/libs/ufg-app-0.1-plain.war /var/lib/tomcat9/webapps/elgreti-ufg.war
```

Tomcat erkennt das neue WAR automatisch und deployt es (Hot-Deploy).

## Hinweise

- Der Checkout erfolgt in ein temporäres Verzeichnis (`/tmp/ufg-deploy-<uid>`), das vor jedem Deploy neu angelegt wird.
- Das `openapi.yaml` im Projekt-Root löst ein Backend-Deploy aus, da es beim Gradle-Build in den Classpath kopiert wird.
- Schlägt der Gradle-Build fehl, wird kein WAR nach Tomcat kopiert — das bestehende Deployment bleibt unverändert.
