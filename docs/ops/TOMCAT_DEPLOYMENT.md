# Tomcat Deployment

## Prod WAR-Datei erstellen

```bash
cd /workspaces/urban-flow-governance/ufg-app
./gradlew war

# Rename for correct URL base path
cd build/libs
mv ufg-app-0.1-plain.war elgreti-ufg.war
```

Ausgabe: ``

**Manager GUI:**`iu-tomcat.servicecluster.de/manager/html`


## Dev WAR deployen

**Manuell:**
```bash
cp build/libs/ufg-app-0.1.war /opt/tomcat/webapps/
```

**Über Manager GUI:** `http://localhost:8080/manager/html`

## Dev Tomcat steuern

```bash
# Dev
sudo chmod +x ~/.sdkman/candidates/tomcat/9.0.116/bin/*.sh 

~/.sdkman/candidates/tomcat/9.0.116/bin/startup.sh
~/.sdkman/candidates/tomcat/9.0.116/bin/shutdown.sh
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
