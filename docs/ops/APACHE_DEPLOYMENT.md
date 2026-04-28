# Apache Deployment

## Frontend deployen

```bash
sudo cp -r /workspace/frontend/* /var/www/html/
```

## Reverse Proxy konfigurieren

`/etc/apache2/sites-available/000-default.conf` ergänzen:

```apache
ProxyPreserveHost On

ProxyPass /api http://localhost:8080/api
ProxyPassReverse /api http://localhost:8080/api
```

## Apache steuern

```bash
sudo service apache2 start
sudo service apache2 restart
sudo service apache2 status
```

## Logs

```bash
tail -f /var/log/apache2/error.log
```

## Troubleshooting

| Problem | Lösung |
|---------|--------|
| Seite leer | `index.html` in `/var/www/html` vorhanden? |
| CSS/JS fehlt | Pfade prüfen |
| API antwortet nicht | Proxy-Config und Tomcat-Status prüfen |
| 403 Fehler | Rechte prüfen (`chmod`) |
| Änderungen nicht sichtbar | Browser-Cache leeren (`Ctrl+Shift+R`) |
