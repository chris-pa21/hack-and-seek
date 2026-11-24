# Bekannte Einschränkungen

Hack & Seek ist für den Einsatz im Schulumfeld gebaut: ein Server, ein
vertrauenswürdiger Betreiber, Spiele mit einer Handvoll Teams. Einige
Entscheidungen sind dafür bewusst einfach gehalten. Diese Seite listet sie
mit ihren Auswirkungen und einem möglichen Lösungsweg.

## Sicherheit

### Schlüssel der Spieler liegen in der Datenbank

- **Was:** Das Backend erzeugt die WireGuard-Schlüsselpaare der Spieler selbst
  und speichert auch den privaten Schlüssel in der Tabelle `player`.
- **Warum:** Die Spieler laden eine fertige Config herunter und müssen keine
  Schlüssel selbst erzeugen.
- **Auswirkung:** Wer die Datenbank liest, hat die Tunnel-Zugänge aller
  Spieler.
- **Lösung:** Schlüssel im Browser erzeugen und nur den öffentlichen an das
  Backend schicken; der private Schlüssel verlässt den Rechner des Spielers
  dann nie.

### Kurzes offenes Forwarding beim Start von WireGuard

- **Was:** Das WireGuard-Image legt beim Hochfahren von `wg0` eigene
  `FORWARD … ACCEPT`-Regeln an. Bis der VPN-Manager kurz danach die Firewall
  auf `DROP` setzt, würde der Container Tunnel-Verkehr in alle angehängten
  Netzwerke weiterleiten.
- **Auswirkung:** ein Zeitfenster von etwa einer Sekunde bei jedem Start des
  WireGuard-Containers.
- **Lösung:** eine eigene `server.conf`-Vorlage ohne diese Regeln über das
  Dockerfile mitliefern.

### Challenge-Container haben Internetzugang

- **Was:** Die Spiel-Netzwerke sind normale Docker-Bridges (`internal=false`).
- **Auswirkung:** Gegeneinander abgeschottet sind die Spiele trotzdem. Aus
  einem Container heraus erreicht man aber das Internet, kann also Werkzeuge
  nachladen oder erbeutete Daten nach außen schicken.
- **Lösung:** interne Netzwerke; Pakete, die eine Challenge braucht, gehören
  dann ins Image.

### Kein HTTPS

- **Was:** nginx liefert die Plattform über HTTP aus, das Session-Cookie ist
  nicht `Secure`.
- **Auswirkung:** Im Schulnetz ausreichend; über das Internet würden
  Passwörter und Session-Cookies im Klartext übertragen.
- **Lösung:** einen Reverse-Proxy mit TLS vorschalten und das Cookie auf
  `Secure` stellen.

### Lösungen der Beispiel-Challenges sind öffentlich

- **Was:** Quellcode und Dockerfiles von `ping-me`, `note-keeper` und
  `leaky-blog` liegen im öffentlichen Repository.
- **Auswirkung:** Wer das Repository kennt, kennt die Schwachstellen. Die drei
  sind als Beispiele gedacht; für echte Spiele erstellt man eigene
  Challenges ([eigene-challenge-erstellen.md](eigene-challenge-erstellen.md)).

## Fairness

### Das Monitoring läuft im Container des Teams

- **Was:** Alle drei Prüfungen (Flag, Erreichbarkeit, `healthcheck.sh`) laufen
  per `docker exec` im Challenge-Container. Dort hat das verteidigende Team
  Root-Rechte.
- **Auswirkung:** Ein Team könnte `healthcheck.sh` oder `curl` durch Versionen
  ersetzen, die immer Erfolg melden, und danach den Dienst abschalten, ohne
  disqualifiziert zu werden.
- **Lösung:** von außen prüfen, etwa mit einem eigenen Checker-Container im
  Spiel-Netzwerk, der den Dienst wie ein normaler Benutzer aufruft. Das wäre
  ein Umbau des Monitorings.

### Besiegte Spieler behalten den VPN-Zugang

- **Was:** Ein besiegtes oder disqualifiziertes Team kann keine Flags mehr
  abgeben, seine Spieler erreichen das Spiel-Netzwerk aber bis zum Spielende.
- **Auswirkung:** Sie könnten die verbleibenden Container weiter stören,
  ohne selbst etwas zu gewinnen.

## Betrieb

### Sessions überstehen keinen Neustart des Backends

- **Was:** Die Login-Sessions liegen nur im Speicher des Backends.
- **Auswirkung:** Nach jedem Neustart des Backends müssen sich alle neu
  einloggen. Laufende Spiele sind nicht betroffen.
- **Lösung:** Spring Session JDBC einbinden, dann liegen die Sessions in
  PostgreSQL.

### Ein Stack pro Host

- **Was:** Der WireGuard-Container und das Datenbank-Volume haben feste Namen
  (`has-wireguard-server`, `has-db-data`), damit die Plattform unabhängig vom
  Compose-Projektnamen funktioniert.
- **Auswirkung:** Auf einem Host läuft nur eine Instanz.

### Keine Administrator-Rolle

- Spiele verwaltet nur ihr Ersteller. Es gibt keine Oberfläche, mit der ein
  Lehrer alle Spiele sieht oder beendet; das geht nur direkt über Docker und
  die Datenbank.

## Daten und Grenzen

### Vergabe von Adressen ohne Sperre

- **Was:** VPN-Adressen (`10.8.19.x`) und Spiel-Subnetze (`10.200.x.0/24`)
  werden als „höchste bisherige plus eins“ vergeben, ohne Sperre oder
  eindeutigen Index.
- **Auswirkung:** Zwei gleichzeitige Registrierungen könnten dieselbe Adresse
  bekommen; bei Schulgröße praktisch ausgeschlossen. Außerdem gibt es höchstens
  253 Spieler und 255 gleichzeitig existierende Spiel-Netzwerke.
- **Lösung:** Vergabe in einer Transaktion mit eindeutigem Index, freie
  Adressen wiederverwenden.

### Gelöschte Spiele hinterlassen Einträge

- **Was:** Wird ein Spiel gelöscht, bleiben seine Teams und
  Monitoring-Ergebnisse (`team`, `sla_event`) in der Datenbank.
- **Auswirkung:** Die Datenbank wächst mit der Zeit; sichtbar ist davon
  nichts.
