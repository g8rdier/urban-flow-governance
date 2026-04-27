# Entity-Relationship-Diagramm: Datenbankstruktur

```mermaid
erDiagram
    USER {
        Long id PK
        String username
        String password
		String role
    }

	AUTH_TOKEN {
		Long id PK
		String token
		Timestamp createdAt
		Long user_id FK
    }

	USER ||--o{ AUTH_TOKEN : "hat"
    USER ||--o{ RESTRICTED_ZONE : "erstellt"

    RESTRICTED_ZONE {
        Long id PK
        String name
        String description
        String reason
        Geometry geometry
        Timestamp startTime
        Timestamp endTime
        String status
        Timestamp createdAt
        Timestamp updatedAt
        Long createdBy FK
    }
```

> Hinweis: `USER.role` verwendet aktuell die Werte `NUTZER` und `ADMIN`.
> `RESTRICTED_ZONE.status` kann `PLANNED`, `ACTIVE` oder `EXPIRED` sein. `geometry` wird als PostGIS-Polygon (`GEOMETRY(Polygon, 4326)`) gespeichert.

# Login/Logout Zustandsdiagramm

```mermaid
stateDiagram-v2
	[*] --> Nicht_angemeldet

	Nicht_angemeldet --> Anmeldedaten_pruefen: Login starten
	Anmeldedaten_pruefen --> Token_gueltig: Gueltige Zugangsdaten
	Anmeldedaten_pruefen --> Nicht_angemeldet: Ungueltige Zugangsdaten

	Token_gueltig --> Token_abgelaufen: > TOKEN_TTL_MINUTES seit Token-Erstellung
	Token_gueltig --> Nicht_angemeldet: Logout
	Token_abgelaufen --> Nicht_angemeldet: Token aus DB loeschen, neu anmelden
	Nicht_angemeldet --> [*]
```

## Sequenzdiagramm: Login und Admin-geschuetzter Request

```mermaid
sequenceDiagram
		autonumber
		participant C as Client
		participant UMC as UserManagerController
		participant U as User
		participant ATS as AuthToken
		participant RCI as RoleCheckInterceptor
		participant AC as Admin Action (create/update/delete)

		C->>UMC: POST /api/session (username, password)
		UMC->>U: findByUsernameAndPassword(...)

		alt Login erfolgreich
				U-->>UMC: User gefunden
				UMC->>ATS: UUID Token speichern (createdAt)
				UMC-->>C: 200 { token, token_type, expires_in_minutes }
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
				alt Authorization Header fehlt/ungueltig
						RCI-->>C: 401 Login required
				else Token nicht gefunden
						RCI-->>C: 401 Invalid token
				else Token abgelaufen
						RCI->>ATS: Token loeschen
						RCI-->>C: 401 Session expired
				else Rolle passt nicht
						RCI-->>C: 403 Role ADMIN required
				else Rolle passt
						RCI-->>AC: erlaubt
						AC-->>C: 2xx
				end
		end
```

## API-Skizze (OpenAPI-Style)

<!-- include: USER_MANAGEMENT.openapi.yaml -->

Die vollständige Spezifikation liegt ausgelagert in [USER_MANAGEMENT.openapi.yaml](USER_MANAGEMENT.openapi.yaml). Wenn dein Doku-Renderer Includes unterstützt, kann diese Datei hier direkt eingebunden werden; ansonsten bleibt sie als separate, versionierte Quelle erhalten.

## Curl-Beispiele

```bash
# 1) Login und Token erhalten
curl -s -X POST "http://localhost:8080/api/session" \
	-d "username=admin&password=adminpass"

# 2) Session-Userinfo mit Bearer-Token abrufen
curl -X GET "http://localhost:8080/api/session" \
	-H "Authorization: Bearer <TOKEN>"

# 3) Logout mit Bearer-Token
curl -X DELETE "http://localhost:8080/api/session" \
	-H "Authorization: Bearer <TOKEN>"

# 4) User erstellen (ADMIN)
curl -X POST "http://localhost:8080/api/users" \
	-H "Authorization: Bearer <TOKEN>" \
	-d "username=max&password=secret&role=NUTZER"

# 5) User aktualisieren (ADMIN)
curl -X PUT "http://localhost:8080/api/users/1" \
	-H "Authorization: Bearer <TOKEN>" \
	-d "username=max2&role=ADMIN"

# 6) User loeschen (ADMIN)
curl -X DELETE "http://localhost:8080/api/users/1" \
	-H "Authorization: Bearer <TOKEN>"
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

	A->>UMC: POST /api/users (username, password, role)
	UMC->>RCI: before()
	alt Kein/ungueltiger/abgelaufener Token
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

	A->>UMC: PUT /api/users/{id} (username?, password?, role?)
	UMC->>RCI: before()
	alt Kein/ungueltiger/abgelaufener Token
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

	A->>UMC: DELETE /api/users/{id}
	UMC->>RCI: before()
	alt Kein/ungueltiger/abgelaufener Token
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

	note over A,UMC: Session pruefen

	A->>UI: Navigiert zu /admin
	UI->>UMC: GET /api/session (mit Bearer-Token)
	alt Token gueltig
		UMC-->>UI: 200 User info loaded
		UI-->>A: Admin-Bereich bleibt sichtbar
	else Kein/ungueltiger Token
		UMC-->>UI: 401 Login required / Invalid token
		UI-->>A: Weiterleitung zu Login
	end

	note over A,UMC: Neuen Nutzer anlegen

	A->>UI: Klickt "Nutzer erstellen"
	UI-->>A: Formular (Username, Passwort, Rolle)
	A->>UI: Formular abschicken
	UI->>UMC: POST /api/users
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
	UI->>UMC: PUT /api/users/{id}
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
	UI->>UMC: DELETE /api/users/{id}
	alt Erfolg
		UMC-->>UI: 200 User deleted
		UI-->>A: Erfolgsmeldung, Nutzer aus Liste entfernt
	else Fehler
		UMC-->>UI: 401/403/404
		UI-->>A: Fehlermeldung anzeigen
	end
```
