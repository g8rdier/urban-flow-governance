# Urban Flow Governance

Webbasierte Anwendung zur Verwaltung und Visualisierung von Sperrzonen sowie zur Routenpruefung gegen aktive Zonen.

## Doku

- [Projekt-Spezifikation](docs/project/PROJECT_SPECIFICATION.md)
- [User Stories](docs/project/USER_STORIES.md)
- [Kanban-Board / Todo-Liste](docs/project/PLAN.md)
- [Zonenmodell](docs/architecture/ZONE_DATA_STRUCTURE.md)
- [Zonen-Management](docs/domain/ZONE_MANAGEMENT.md)
- [Nutzerverwaltung](docs/domain/USER_MANAGEMENT.md)
- [Tomcat-Deployment](docs/ops/TOMCAT_DEPLOYMENT.md)

## Kurzstart

1. Repository in VS Code oeffnen und per Dev Container starten.
2. Anwendung im Projektordner `ufg-app` mit `./gradlew bootRun` starten.

## Betrieb

- Benoetigte Umgebungsvariablen:
	- `DB_HOST`
	- `DB_PORT`
	- `DB_NAME`
	- `DB_USER`
	- `DB_PASSWORD`
- Datenbank: PostgreSQL mit PostGIS
- Admin-Links:
	- DB Management: `iu.servicecluster.de/phppgadmin/`
	- Tomcat Manager: `iu-tomcat.servicecluster.de/manager/html`
	- Swagger UI: `iu-tomcat.servicecluster.de/elgreti-ufg/swagger-ui`
	- Frontend: `elgreti.servicecluster.de`

## Externe Dienste

- Leaflet fuer die Karte
- Nominatim fuer Geocoding
- OSRM fuer Routenberechnung

## Kurz

Tech-Stack: Grails 6.3.2 auf Java 17, mit Leaflet, Nominatim und OSRM fuer die GIS-Funktionen.
