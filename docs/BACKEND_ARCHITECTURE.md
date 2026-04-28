# Backend-Architektur

## Technologie-Stack

Grails 6 (Groovy / Spring Boot), H2-Datenbank, JTS für Geometrie-Operationen.

## Schichtendiagramm

```mermaid
graph TD
    Client["Client (Frontend / API)"]

    subgraph Controllers
        UC[UserManagerController]
        ZC[RestrictedZoneController]
        RC[RouteController]
        SC[SwaggerController]
    end

    subgraph Interceptor
        RI[RoleCheckInterceptor\n@RequiredRoles]
    end

    subgraph Services
        UMS[UserManagerService]
        RZS[RestrictedZoneService]
        RCS[RouteCheckService]
        PHS[PasswordHashService]
    end

    subgraph Domain
        U[User]
        UC2[UserCredential]
        AT[AuthToken]
        RZ[RestrictedZone]
    end

    DB[(H2 Database)]

    Client -->|HTTP Request| RI
    RI -->|Token & Rolle OK| Controllers
    UC --> UMS
    ZC --> RZS
    RC --> RCS
    UMS --> PHS
    UMS --> U
    UMS --> UC2
    UMS --> AT
    RZS --> RZ
    RCS --> RZ
    U --> DB
    UC2 --> DB
    AT --> DB
    RZ --> DB
```

## Auth-Konzept

Kein Spring Security — die Authentifizierung läuft über einen eigenen `AuthToken` (UUID). Der `RoleCheckInterceptor` prüft vor jedem Request den Bearer-Token und wertet die `@RequiredRoles`-Annotation auf der jeweiligen Controller-Methode aus.

## OpenAPI

Die `openapi.yaml` liegt einmalig im Projekt-Root und wird von Gradle beim Build automatisch in den Grails-Classpath kopiert. Sie ist über `/openapi.yaml` (YAML) und `/swagger-ui` (UI) erreichbar.
