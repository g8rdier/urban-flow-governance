# User Stories — Urban Flow Governance

## Feature 1: Zone Management

### US-1.1: Sperrzone erstellen
Als Stadtverwaltungs-Admin  
Möchte ich eine neue Sperrzone auf der Karte zeichnen und speichern  
Damit ich kurzfristig Bereiche sperren kann (z.B. bei Marathon oder Baustelle)

**Akzeptanzkriterien:**
- Ich kann ein Polygon (geschlossene Fläche) zeichnen
- Ich kann der Zone einen Namen und Grund geben (Marathon/Baustelle/Umweltalarm)
- Ich kann Start- und Enddatum/uhrzeit festlegen
- Die Zone wird gespeichert

---

### US-1.2: Sperrzone bearbeiten
Als Admin  
Möchte ich eine bestehende Zone verändern (Name, Zeiten, Form)  
Damit ich auf Änderungen reagieren kann (z.B. Marathon verschoben)

**Akzeptanzkriterien:**
- Ich kann Zone auswählen und editieren
- Ich kann die Grenzen (Polygon) anpassen
- Ich kann Start/End-Zeit verschieben
- Änderungen werden gespeichert

---

### US-1.3: Sperrzone löschen
Als Admin  
Möchte ich nicht mehr benötigte Zonen löschen  
Damit die Karte übersichtlich bleibt

**Akzeptanzkriterien:**
- Ich kann eine Zone auswählen und löschen
- Bestätigung wird abgefragt (Sicherheit)
- Zone verschwindet von der Karte und aus dem System

---

### US-1.4: Zone-Status verwalten
Als Admin  
Möchte ich den Status einer Zone ändern (geplant → aktiv → abgelaufen)  
Damit das System weiß, welche Zonen gerade wirksam sind

**Akzeptanzkriterien:**
- Zone hat einen Status: "Geplant", "Aktiv", "Abgelaufen"
- Ich kann Status manuell ändern
- Nur aktive Zonen beeinflussen Routenprüfung
- Status-Übergänge können zeitgesteuert sein

---

## Feature 2: Routenprüfung

### US-2.1: Route berechnen
Als Nutzer (Taxifahrer, Lieferfahrer)  
Möchte ich eine Route von A nach B berechnen  
Damit ich weiß, welche Strecke ich fahren soll

**Akzeptanzkriterien:**
- Ich kann Start- und Zielort eingeben
- Das System berechnet eine Route
- Die Route wird auf der Karte angezeigt
- Ich sehe Dauer und Distanz

---

### US-2.2: Warnung bei Sperrzone-Konflikt
Als Nutzer  
Möchte ich eine Warnung erhalten, wenn meine Route durch eine aktive Sperrzone führt  
Damit ich die Route ändern kann oder den Grund der Sperrung verstehe

**Akzeptanzkriterien:**
- Das System prüft automatisch: schneidet Route eine Sperrzone?
- Bei Konflikt: **rote Warnung** mit Zone-Name und Zeitraum
- Beispiel: "⚠️  Route kreuzt Marathon-Sperrzone (14:00–18:00)"
- Ohne Konflikt: grüne Bestätigung "✓ Route OK"

---

### US-2.3: Alternative Route anfordert
Als Nutzer  
Möchte ich auf Wunsch eine Alternativroute bekommen, die Sperrzone umgeht  
Damit ich trotzdem schnell ans Ziel komme

**Akzeptanzkriterien:**
- Button: "Alternative Route"
- System berechnet Umleitung
- Neue Route führt nicht durch aktive Zonen

---

## Feature 3: Visualisierung

### US-3.1: Sperrzonen auf Karte anzeigen
Als Admin / Nutzer  
Möchte ich alle Sperrzonen auf der Karte sehen  
Damit ich den geografischen Überblick habe

**Akzeptanzkriterien:**
- Alle Zonen werden farblich gekennzeichnet
- Farb-Code: z.B. Rot = Aktiv, Gelb = Geplant, Grau = Abgelaufen
- Zonen-Rahmen ist deutlich sichtbar
- Zoom rein/raus funktioniert

---

### US-3.2: Zone-Details beim Klick anzeigen
Als Nutzer  
Möchte ich auf eine Zone klicken und Details sehen  
Damit ich weiß, warum sie gesperrt ist und wie lange

**Akzeptanzkriterien:**
- Klick auf Zone zeigt Popup/Modal
- Popup zeigt: Name, Grund, Start/End-Zeit, Status
- Ich kann Zone bearbeiten oder löschen (wenn Admin)

---

### US-3.3: Route-Konflikt visuell hervorheben
Als Nutzer  
Möchte ich sehen, **wo genau** meine Route die Sperrzone kreuzt  
Damit ich das verstehe und alternative Strecken wähle

**Akzeptanzkriterien:**
- Konflikt-Punkt wird auf der Karte markiert (z.B. Kreuz oder Punkt)
- Route-Segment im Konflikt ist rot
- Rest der Route ist grün

---

## Feature 4: Suchfunktion

### US-4.1: Stadtteil/Straße suchen
Als Admin  
Möchte ich nach einem Stadtteil oder einer Straße suchen  
Damit ich schnell die richtige Position finde, um eine Zone zu zeichnen

**Akzeptanzkriterien:**
- Suchfeld in der Oberfläche
- Ich tippe "Marienplatz" oder "Darmstädter Landstraße"
- Ergebnisse werden vorgeschlagen (Auto-Complete)
- Klick auf Ergebnis zentriert Karte auf diesen Ort

---

### US-4.2: Startpunkt/Ziel suchen
Als Nutzer  
Möchte ich Start- und Zielort per Adresse eingeben, nicht nur koordinaten  
Damit ich nicht mit GPS-Koordinaten arbeiten muss

**Akzeptanzkriterien:**
- Suchfelder für Start und Ziel
- Ich tippe "München Hauptbahnhof"
- System schlägt Adresse vor
- Klick auf Vorschlag setzt Punkt auf Karte

---

## Feature 5: Echtzeit-Statusanzeige

### US-5.1: Aktive Zonen anzeigen
Als Nutzer / Admin  
Möchte ich in einer Liste sehen, welche Zonen **gerade aktiv** sind  
Damit ich den aktuellen Status überblicke

**Akzeptanzkriterien:**
- Sidebar oder Panel zeigt aktive Zonen
- Für jede Zone: Name, Grund, verbleibende Zeit
- Sortierung nach Start/End-Zeit
- Live-Update (wenn Uhrzeit sich ändert)

---

### US-5.2: Benachrichtigung bei Zone-Aktivierung
Als Admin  
Möchte ich benachrichtigt werden, wenn eine geplante Zone gerade aktiv wird  
Damit ich keine Zeitpunkt verpasse

**Akzeptanzkriterien:**
- 5 Minuten vor Start: Benachrichtigung "Zone aktiviert sich in 5 Min"
- Beim Start: Benachrichtigung "Zone ist jetzt aktiv"
- Beim Ende: Benachrichtigung "Zone ist abgelaufen"

---

## Feature 6: Audit & Logging (optional für MVP)

### US-6.1: Zone-Verlauf ansehen
Als Admin  
Möchte ich sehen, wer wann welche Zone erstellt/geändert hat  
Damit ich Verantwortlichkeit nachverfolgen kann

**Akzeptanzkriterien:**
- Jede Zone hat Änderungshistorie
- Einträge zeigen: Wer, Was, Wann
- Alte Versionen können eingesehen werden

---

## Priorisierung für MVP (Minimum Viable Product)

### **Must-Have (Sprint 1–2)**
- US-1.1: Sperrzone erstellen
- US-2.1: Route berechnen
- US-2.2: Warnung bei Konflikt
- US-3.1: Zonen auf Karte anzeigen
- US-3.3: Route-Konflikt visualisieren

### **Should-Have (Sprint 3–4)**
- US-1.2: Zone bearbeiten
- US-1.4: Zone-Status verwalten
- US-3.2: Zone-Details anzeigen
- US-4.1: Stadtteil suchen
- US-5.1: Aktive Zonen-Übersicht

### **Nice-to-Have (Sprint 5+)**
- US-1.3: Zone löschen
- US-2.3: Alternative Route
- US-4.2: Zielort suchen
- US-5.2: Benachrichtigungen
- US-6.1: Audit-Log
