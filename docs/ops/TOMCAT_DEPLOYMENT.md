# Tomcat Deployment

## WAR-Datei erstellen

```bash
cd ufg-app
./gradlew war
```

Ausgabe: `ufg-app/build/libs/ufg-app-0.1.war`

## WAR deployen

**Manuell:**
```bash
cp build/libs/ufg-app-0.1.war /opt/tomcat/webapps/
```

**Über Manager GUI:** `http://localhost:8080/manager/html`

## Tomcat steuern

```bash
# Dev
~/.sdkman/candidates/tomcat/9.0.116/bin/startup.sh
~/.sdkman/candidates/tomcat/9.0.116/bin/shutdown.sh

# Prod
/opt/tomcat/bin/startup.sh
/opt/tomcat/bin/shutdown.sh
```

## Logs

```bash
tail -f /opt/tomcat/logs/catalina.out
```

## Troubleshooting

| Problem | Lösung |
|---------|--------|
| 404 beim Zugriff | WAR im korrekten `/webapps/` Ordner? |
| Tomcat läuft nicht | Port 8080 belegt? `sudo lsof -i :8080` |
| Permissions-Fehler | `chmod +x /opt/tomcat/bin/*.sh` |
| Alte Version lädt | Browser-Cache leeren (`Ctrl+Shift+Delete`) |
