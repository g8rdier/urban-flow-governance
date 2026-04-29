# User Management und Authentifizierung

Diese Doku beschreibt den aktuellen Stand der Implementierung in:
- `UserManagerController`
- `UserManagerService`
- `RoleCheckInterceptor`
- Domain-Klassen `User`, `UserCredential`, `AuthToken`

## Datenmodell (Ist-Stand)

```mermaid
erDiagram
    USER {
        Long id PK
        String username
        String role
    }

    USER_CREDENTIAL {
        Long id PK
        String passwordHash
        String salt
        Long user_id FK
    }

    AUTH_TOKEN {
        Long id PK
        String token
        Timestamp createdAt
        Long user_id FK
    }

    USER ||--|| USER_CREDENTIAL : "hasOne"
    USER ||--o{ AUTH_TOKEN : "has many"
    USER ||--o{ RESTRICTED_ZONE : "createdBy"
```

Wichtige Punkte:
- Passwoerter liegen nicht in `USER`, sondern als `passwordHash` + `salt` in `USER_CREDENTIAL`.
- `username` ist eindeutig; `UserCredential.user` ist 1:1 (unique); `AuthToken.token` ist eindeutig.
- Rollen sind aktuell: `NUTZER`, `ADMIN`.
- `TOKEN_TTL_MINUTES` kommt aus Environment (`TOKEN_TTL_MINUTES`), Default: `60`.
- Bei neuem Login werden alte Tokens des Users geloescht (Single-Session-Verhalten).

## Endpunkte und Autorisierung

Hinweis zu `POST /api/users`:
- Ohne Token kann ein `NUTZER` erstellt werden.
- `ADMIN` kann nur erstellt werden, wenn ein gueltiger Token eines `ADMIN` mitgesendet wird.

## Token-Validierung

Token werden zentral ueber `UserManagerService.validateToken()` geprueft:
- fehlt Token -> `Login required`
- Token nicht gefunden/ungueltig/abgelaufen -> `Invalid token`
- Token ohne User-Referenz -> Token wird geloescht und als ungueltig behandelt

Ablaufdatum:
- `minutesSince(createdAt) > TOKEN_TTL_MINUTES` => Token wird geloescht.

## Sequenzdiagramm: POST /api/session (username, password)

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant UMC as UserManagerController
    participant UMS as UserManagerService
    participant U as User
    participant UC as UserCredential
    participant AT as AuthToken

    C->>UMC: POST /api/session (username, password)

    alt username oder password fehlt
        UMC-->>C: 400 Provide username and password
    else Felder vorhanden
        UMC->>UMS: login(username, password)
        UMS->>U: findByUsername(username)

        alt User fehlt oder credential fehlt
            UMS-->>UMC: 401 Invalid credentials
            UMC-->>C: 401 Invalid credentials
        else User vorhanden
            UMS->>UC: verifyPassword(password, salt, passwordHash)

            alt Passwort ungueltig
                UMS-->>UMC: 401 Invalid credentials
                UMC-->>C: 401 Invalid credentials
            else Passwort gueltig
                UMC->>UMS: createTokenForUser(user)
                UMS->>AT: delete all tokens for user
                UMS->>AT: create new UUID token
                UMS-->>UMC: token
                UMC-->>C: 200 {token, token_type, expires_in_minutes}
            end
        end
    end
```

## Sequenzdiagramm: Auth-Guard fuer geschuetzte Endpunkte

Gilt fuer Actions mit `@RequiredRoles`.

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant RCI as RoleCheckInterceptor
    participant UMS as UserManagerService
    participant AT as AuthToken
    participant A as Controller Action

    C->>A: Request auf geschuetzten Endpunkt
    A->>RCI: before()
    RCI->>RCI: resolveRequiredRoles(controller, action)

    alt keine Rollen-Anforderung
        RCI-->>A: erlaubt
    else Rollen erforderlich
        alt Authorization Header/Bearer Token fehlt
            RCI-->>C: 401 Login required
        else Token vorhanden
            RCI->>UMS: validateToken(token)
            alt Token ungueltig/abgelaufen
                RCI-->>C: 401 Invalid token
            else Token gueltig
                UMS->>AT: (bei Ablauf) delete token
                alt Rolle nicht enthalten
                    RCI-->>C: 403 Role <required> required
                else Rolle passt
                    RCI-->>A: erlaubt
                end
            end
        end
    end
```

## Sequenzdiagramm: GET /api/session

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant RCI as RoleCheckInterceptor
    participant UMC as UserManagerController
    participant UMS as UserManagerService

    C->>UMC: GET /api/session (Bearer Token)
    UMC->>RCI: before() via interceptor

    alt nicht autorisiert
        RCI-->>C: 401/403
    else autorisiert
        UMC->>UMS: getSessionInfo(token)
        alt Token in Action fehlt/ungueltig
            UMC-->>C: 401 Login required / Invalid token
        else gueltig
            UMS-->>UMC: {user:{id,username,role}, remaining_ttl_minutes}
            UMC-->>C: 200 User info loaded
        end
    end
```

## Sequenzdiagramm: DELETE /api/session

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant RCI as RoleCheckInterceptor
    participant UMC as UserManagerController
    participant UMS as UserManagerService

    C->>UMC: DELETE /api/session (Bearer Token)
    UMC->>RCI: before() via interceptor

    alt nicht autorisiert
        RCI-->>C: 401/403
    else autorisiert
        UMC->>UMS: getUserByToken(token)
        alt token fehlt/ungueltig
            UMC-->>C: 401 Login required / Invalid token
        else token gueltig
            UMC->>UMS: deleteToken(token)
            UMC-->>C: 200 Logout successful
        end
    end
```

## Sequenzdiagramm: POST /api/users

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant UMC as UserManagerController
    participant UMS as UserManagerService
    participant U as User
    participant UC as UserCredential

    C->>UMC: POST /api/users (username, password, role)
    UMC->>UMS: createUser(username, password, role, token?)

    alt token vorhanden und ungueltig
        UMS-->>UMC: 401 Login required/Invalid token
        UMC-->>C: 401
    else role == ADMIN und currentUser != ADMIN
        UMS-->>UMC: 403 Role ADMIN required
        UMC-->>C: 403
    else User speichern
        UMS->>U: save(username, role default NUTZER)
        alt User-Validierung fehlschlaegt
            UMS-->>UMC: 400 validation error
            UMC-->>C: 400
        else User gespeichert
            UMS->>UC: save(passwordHash, salt, user)
            alt Credential-Validierung fehlschlaegt
                UMS-->>UMC: 400 validation error (rollback)
                UMC-->>C: 400
            else Erfolg
                UMS-->>UMC: 201 User created
                UMC-->>C: 201 User created
            end
        end
    end
```

## Sequenzdiagramm: PUT/PATCH /api/users/{id}

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant RCI as RoleCheckInterceptor
    participant UMC as UserManagerController
    participant UMS as UserManagerService
    participant U as User
    participant UC as UserCredential

    C->>UMC: PUT/PATCH /api/users/{id}
    UMC->>RCI: before() via interceptor

    alt nicht autorisiert
        RCI-->>C: 401/403
    else autorisiert
        UMC->>UMS: updateUser(id, username?, password?, role?, token)
        UMS->>U: User.get(id)

        alt User nicht gefunden
            UMS-->>UMC: 404 User not found
            UMC-->>C: 404
        else User gefunden
            alt currentUser != ADMIN und currentUser.id != user.id
                UMS-->>UMC: 403 You can only update your own account
                UMC-->>C: 403
            else role == ADMIN und currentUser != ADMIN
                UMS-->>UMC: 403 Role ADMIN required
                UMC-->>C: 403
            else Updates anwenden
                alt password gesetzt
                    UMS->>UC: create/update credential mit neuem salt+hash
                end
                UMS->>U: save()
                alt Validierungsfehler
                    UMS-->>UMC: 400 validation error
                    UMC-->>C: 400
                else Erfolg
                    UMS-->>UMC: 200 User updated
                    UMC-->>C: 200
                end
            end
        end
    end
```

## Sequenzdiagramm: DELETE /api/users/{id}

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant RCI as RoleCheckInterceptor
    participant UMC as UserManagerController
    participant UMS as UserManagerService
    participant U as User
    participant UC as UserCredential
    participant AT as AuthToken

    C->>UMC: DELETE /api/users/{id}
    UMC->>RCI: before() ADMIN

    alt nicht autorisiert
        RCI-->>C: 401/403
    else autorisiert
        UMC->>UMS: deleteUser(id)
        UMS->>U: User.get(id)

        alt User nicht gefunden
            UMS-->>UMC: 404 User not found
            UMC-->>C: 404
        else User gefunden
            UMS->>AT: delete token for user (if exists)
            UMS->>UC: delete credential (if exists)
            UMS->>U: delete user
            UMS-->>UMC: 200 User deleted
            UMC-->>C: 200
        end
    end
```

## Sequenzdiagramm: GET /api/users

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant RCI as RoleCheckInterceptor
    participant UMC as UserManagerController
    participant UMS as UserManagerService
    participant U as User

    C->>UMC: GET /api/users
    UMC->>RCI: before() ADMIN

    alt nicht autorisiert
        RCI-->>C: 401/403
    else autorisiert
        UMC->>UMS: listUsers()
        UMS->>U: User.list()
        UMS-->>UMC: users[] (id, username, role)
        UMC-->>C: 200 Users loaded
    end
```

## Offene Besonderheiten (bewusst dokumentiert)

- `POST /api/users` ist nicht per Annotation geschuetzt, die Rollenregel fuer ADMIN-Erstellung sitzt im Service.
- `PUT/PATCH /api/users/{id}` erlaubt auch `NUTZER`, aber nur fuer das eigene Konto.
- Bei abgelaufenem Token ist die Fehlermeldung aktuell `Invalid token` (nicht `Session expired`).
- Service-Hilfsmethode `success(...)` fuegt `extra` auf Top-Level hinzu; dadurch liegt `users` aktuell nicht unter `data`, sondern direkt im Ergebnisobjekt.
