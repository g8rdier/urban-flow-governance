# Login/Logout Zustandsdiagramm

```mermaid
stateDiagram-v2
	[*] --> Nicht_angemeldet

	Nicht_angemeldet --> Anmeldedaten_pruefen: Login starten
	Anmeldedaten_pruefen --> Angemeldet: Gueltige Zugangsdaten
	Anmeldedaten_pruefen --> Nicht_angemeldet: Ungueltige Zugangsdaten

	Angemeldet --> Nicht_angemeldet: Logout
	Nicht_angemeldet --> [*]
```

## Sequenzdiagramm: Login und Admin-geschuetzter Request

```mermaid
sequenceDiagram
		autonumber
		participant C as Client
		participant UMC as UserManagerController
		participant U as User
		participant RCI as RoleCheckInterceptor
		participant AC as Admin Action (create/update/delete)

		C->>UMC: POST /userManager/login (username, password)
		UMC->>U: findByUsernameAndPassword(...)

		alt Login erfolgreich
				U-->>UMC: User gefunden
				UMC->>UMC: session.userId setzen
				UMC->>UMC: session.sessionKey setzen
				UMC-->>C: 302 Redirect /greeting/index
		else Login fehlgeschlagen
				U-->>UMC: kein User
				UMC-->>C: 401 Invalid credentials
		end

		C->>AC: POST/PUT/DELETE auf Admin-Endpunkt
		AC->>RCI: before()
		RCI->>RCI: resolveRequiredRoles(controller, action)

		alt Keine @RequiredRoles Annotation
				RCI-->>AC: erlaubt
				AC-->>C: 2xx
		else Annotation vorhanden (z.B. ADMIN)
				alt session.userId fehlt
						RCI-->>C: 401 Login required
				else User zur Session nicht vorhanden
						RCI->>RCI: session.invalidate()
						RCI-->>C: 401 Invalid session
				else Rolle passt nicht
						RCI-->>C: 403 Role ADMIN required
				else Rolle passt
						RCI-->>AC: erlaubt
						AC-->>C: 2xx
				end
		end
```

## API-Skizze (OpenAPI-Style)

Hinweis: Dies ist eine kompakte, dokumentierende Spezifikation basierend auf der aktuellen Implementierung.

```yaml
openapi: 3.0.3
info:
	title: Urban Flow Governance - User Management API
	version: 1.0.0
	description: Login/Logout und Admin-Nutzerverwaltung
servers:
	- url: /
paths:
	/userManager/login:
		post:
			summary: Benutzer anmelden
			requestBody:
				required: true
				content:
					application/x-www-form-urlencoded:
						schema:
							type: object
							required: [username, password]
							properties:
								username:
									type: string
								password:
									type: string
			responses:
				'302':
					description: Login erfolgreich, Redirect auf /greeting/index
				'400':
					description: username oder password fehlt
				'401':
					description: Invalid credentials

	/userManager/logout:
		post:
			summary: Benutzer abmelden
			responses:
				'302':
					description: Logout erfolgreich, Redirect auf /greeting/index

	/userManager/create:
		post:
			summary: Nutzer erstellen (nur ADMIN)
			description: Benoetigt eingeloggten Benutzer mit Rolle ADMIN.
			requestBody:
				required: true
				content:
					application/x-www-form-urlencoded:
						schema:
							type: object
							required: [username, password]
							properties:
								username:
									type: string
								password:
									type: string
								role:
									type: string
									default: NUTZER
									enum: [ADMIN, NUTZER]
			responses:
				'201':
					description: User created
				'400':
					description: Validierungsfehler
				'401':
					description: Login required oder Invalid session
				'403':
					description: Role ADMIN required

	/userManager/update:
		put:
			summary: Nutzer aktualisieren (nur ADMIN)
			description: Benoetigt Query-Parameter id.
			parameters:
				- in: query
					name: id
					required: true
					schema:
						type: integer
						format: int64
			requestBody:
				required: false
				content:
					application/x-www-form-urlencoded:
						schema:
							type: object
							properties:
								username:
									type: string
								password:
									type: string
								role:
									type: string
									enum: [ADMIN, NUTZER]
			responses:
				'200':
					description: User updated
				'404':
					description: User not found
				'400':
					description: Validierungsfehler
				'401':
					description: Login required oder Invalid session
				'403':
					description: Role ADMIN required

	/userManager/delete:
		delete:
			summary: Nutzer loeschen (nur ADMIN)
			description: Benoetigt Query-Parameter id.
			parameters:
				- in: query
					name: id
					required: true
					schema:
						type: integer
						format: int64
			responses:
				'200':
					description: User deleted
				'404':
					description: User not found
				'401':
					description: Login required oder Invalid session
				'403':
					description: Role ADMIN required

components:
	securitySchemes:
		sessionCookie:
			type: apiKey
			in: cookie
			name: JSESSIONID
```

## Sequenzdiagramm: Admin User-CRUD

```mermaid
sequenceDiagram
	autonumber
	participant A as Admin (Client)
	participant RCI as RoleCheckInterceptor
	participant UMC as UserManagerController
	participant DB as User (Datenbank)

	note over A,DB: Nutzer erstellen

	A->>UMC: POST /userManager/create (username, password, role)
	UMC->>RCI: before()
	alt Nicht eingeloggt oder ungueltige Session
		RCI-->>A: 401
	else Rolle != ADMIN
		RCI-->>A: 403 Role ADMIN required
	else Autorisiert
		RCI-->>UMC: erlaubt
		UMC->>DB: new User(...).save()
		alt Validierungsfehler
			DB-->>UMC: Fehler
			UMC-->>A: 400 Fehlermeldung
		else Erfolg
			DB-->>UMC: User gespeichert
			UMC-->>A: 201 User created
		end
	end

	note over A,DB: Nutzer aktualisieren

	A->>UMC: PUT /userManager/update?id=<id> (username?, password?, role?)
	UMC->>RCI: before()
	alt Nicht eingeloggt oder ungueltige Session
		RCI-->>A: 401
	else Rolle != ADMIN
		RCI-->>A: 403 Role ADMIN required
	else Autorisiert
		RCI-->>UMC: erlaubt
		UMC->>DB: User.get(id)
		alt User nicht gefunden
			DB-->>UMC: null
			UMC-->>A: 404 User not found
		else User gefunden
			UMC->>DB: user.save()
			alt Validierungsfehler
				DB-->>UMC: Fehler
				UMC-->>A: 400 Fehlermeldung
			else Erfolg
				DB-->>UMC: User aktualisiert
				UMC-->>A: 200 User updated
			end
		end
	end

	note over A,DB: Nutzer loeschen

	A->>UMC: DELETE /userManager/delete?id=<id>
	UMC->>RCI: before()
	alt Nicht eingeloggt oder ungueltige Session
		RCI-->>A: 401
	else Rolle != ADMIN
		RCI-->>A: 403 Role ADMIN required
	else Autorisiert
		RCI-->>UMC: erlaubt
		UMC->>DB: User.get(id)
		alt User nicht gefunden
			DB-->>UMC: null
			UMC-->>A: 404 User not found
		else User gefunden
			UMC->>DB: user.delete()
			DB-->>UMC: geloescht
			UMC-->>A: 200 User deleted
		end
	end
```

## Sequenzdiagramm: Admin-UI Seitenablaeufe

```mermaid
sequenceDiagram
	autonumber
	participant A as Admin (Browser)
	participant UI as Admin-Seite (Frontend)
	participant UMC as UserManagerController

	note over A,UMC: Userliste anzeigen

	A->>UI: Navigiert zu /admin
	UI->>UMC: GET /userManager/list
	UMC-->>UI: Liste aller User
	UI-->>A: Tabelle mit Nutzern (Username, Rolle, Aktionen)

	note over A,UMC: Neuen Nutzer anlegen

	A->>UI: Klickt "Nutzer erstellen"
	UI-->>A: Formular (Username, Passwort, Rolle)
	A->>UI: Formular abschicken
	UI->>UMC: POST /userManager/create
	alt Erfolg
		UMC-->>UI: 201 User created
		UI-->>A: Erfolgsmeldung, Userliste aktualisiert
	else Fehler
		UMC-->>UI: 400/401/403
		UI-->>A: Fehlermeldung anzeigen
	end

	note over A,UMC: Nutzer bearbeiten

	A->>UI: Klickt "Bearbeiten" bei einem Nutzer
	UI-->>A: Formular vorausgefuellt (Username, Rolle)
	A->>UI: Aenderungen bestaetigen
	UI->>UMC: PUT /userManager/update?id=<id>
	alt Erfolg
		UMC-->>UI: 200 User updated
		UI-->>A: Erfolgsmeldung, Userliste aktualisiert
	else Fehler
		UMC-->>UI: 400/401/403/404
		UI-->>A: Fehlermeldung anzeigen
	end

	note over A,UMC: Nutzer loeschen

	A->>UI: Klickt "Loeschen" bei einem Nutzer
	UI-->>A: Bestaetigung anfordern
	A->>UI: Bestaetigt
	UI->>UMC: DELETE /userManager/delete?id=<id>
	alt Erfolg
		UMC-->>UI: 200 User deleted
		UI-->>A: Erfolgsmeldung, Nutzer aus Liste entfernt
	else Fehler
		UMC-->>UI: 401/403/404
		UI-->>A: Fehlermeldung anzeigen
	end
```
