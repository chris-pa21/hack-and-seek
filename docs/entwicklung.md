# Entwicklung

Wie man Hack & Seek lokal entwickelt, testet und wo man nachsieht, wenn etwas
nicht stimmt. Den Aufbau beschreibt [architektur.md](architektur.md).

## Voraussetzungen

| Werkzeug | Version | Wofür |
|---|---|---|
| Docker mit Compose | aktuell | gesamter Stack, Datenbank, Challenges |
| Java | JDK 21 | Backend (der Gradle-Wrapper liegt bei) |
| Node.js | 20.19+ oder 22.12+ | Frontend |
| Python | 3 mit `flask` und `pytest` | Tests des VPN-Managers |

Linux als Host wird empfohlen: Der WireGuard-Container braucht das
Kernel-Modul und `NET_ADMIN`.

## Umgebungsvariablen

### Gesamter Stack (`.env` im Hauptordner)

Vorlage: `env-example`. Alle Werte in eckigen Klammern müssen ersetzt werden.

| Variable | Pflicht | Bedeutung |
|---|---|---|
| `PASSWORD` | ja | Passwort der PostgreSQL-Datenbank |
| `VPN_MANAGER_TOKEN` | ja | gemeinsames Geheimnis von Backend und VPN-Manager, z. B. `openssl rand -hex 32`. Ohne Wert startet Compose nicht, und der Manager verweigert den Start mit dem Platzhalter. |
| `VPN_SERVER_ENDPOINT` | ja | `host:port`, mit dem sich die WireGuard-Clients der Spieler verbinden; landet in jeder Config. Der Port muss `WIREGUARD_PORT` entsprechen. |
| `FRONTEND_PORT` | nein, `80` | Port der Weboberfläche |
| `WIREGUARD_PORT` | nein, `51820` | UDP-Port von WireGuard |
| `ADMINER_PORT` | nein, `8081` | Adminer, nur auf `127.0.0.1` |

Der öffentliche WireGuard-Schlüssel des Servers wird **nicht** konfiguriert;
das Backend liest ihn aus dem laufenden Interface.

### Backend auf dem Host (`has-backend/.env`)

Vorlage: `has-backend/env-example`, mit denselben drei Pflichtwerten. Das
Backend liest die Datei über `spring-dotenv` (`${env.PASSWORD}` in
`application.properties`). Unter Docker Compose überschreiben die
Umgebungsvariablen aus `docker-compose.yml` diese Werte.

## Lokal entwickeln

### Alles in Docker

```bash
docker compose up -d --build
```

Das entspricht dem Betrieb und ist der einzige Weg, bei dem Spiele mit VPN
vollständig funktionieren. Nach Änderungen am Code den betroffenen Dienst neu
bauen, z. B. `docker compose up -d --build backend`.

### Backend auf dem Host

```bash
docker compose up database -d         # PostgreSQL auf 127.0.0.1:5432
cd has-backend
cp env-example .env                   # Werte eintragen
./gradlew bootRun                     # http://localhost:8080
```

Das Backend spricht Docker über `/var/run/docker.sock` an; der Benutzer
braucht also Zugriff darauf (Gruppe `docker`).

Einschränkung: Der VPN-Manager ist nur im Compose-Netzwerk unter
`wireguard-server:9999` erreichbar, nicht vom Host. Für Arbeit an API,
Spiellogik und Frontend reicht das; Spiele mit VPN-Freigaben testet man im
vollen Stack.

### Frontend mit Hot Reload

```bash
cd has-frontend
npm install
npm run dev                           # http://localhost:5173
```

Vite leitet `/api` an `http://localhost:8080` weiter, also an ein Backend, das
mit `bootRun` auf dem Host läuft. Das Backend im Compose-Stack ist absichtlich
nicht veröffentlicht.

## Tests

| Teil | Befehl | Umfang | Braucht |
|---|---|---|---|
| Backend | `cd has-backend && ./gradlew test` | 143 Tests (JUnit 5, Spring Boot Test) | nichts: H2 im Speicher, Docker gemockt |
| VPN-Manager | `pytest has-vpn` | 39 Tests | `flask`, `pytest`; `run_cmd` ist ersetzt, es läuft kein iptables |
| Frontend | `cd has-frontend && npm run dev`, dann `npm run cy:run` | Cypress-E2E: `auth`, `home`, `dashboard`, `game-detail` | laufender Vite-Server; die API wird mit `cy.intercept` ersetzt, kein Backend nötig |

### Einzelne Tests

```bash
./gradlew test --tests '*VpnServiceTest'
./gradlew test --tests '*GameServiceTest.startGame*'
```

Achtung: Ein Filter, der mit dem Paket beginnt (`Diplo10.service...`),
findet nichts. Gradle liest ein Muster mit großem Anfangsbuchstaben als
einfachen Klassennamen. Deshalb mit `*` beginnen.

Cypress interaktiv: `npm run cy:open`.

### Testkonfiguration

`has-backend/src/test/resources/application.properties` ersetzt im Test die
normale Konfiguration: H2 statt PostgreSQL, `ddl-auto=create-drop`. Docker und
der VPN-Manager werden in den Tests gemockt.

## Nachsehen und Fehlersuche

| Was | Wie |
|---|---|
| Logs | `docker compose logs -f backend` (bzw. `wireguard-server`, `frontend`) |
| Datenbank | Adminer unter `http://127.0.0.1:8081` (System PostgreSQL, Server `database`, Benutzer `postgres`, Datenbank `has`) oder `docker compose exec database psql -U postgres -d has` |
| WireGuard-Peers | `docker exec has-wireguard-server wg show wg0` |
| Firewall | `docker exec has-wireguard-server iptables -S FORWARD` |
| Spiel-Netzwerke | `docker network ls --filter name=net_game` |
| Challenge-Container | `docker ps --filter label=type=challenge` |
| VPN-Manager | `docker exec has-wireguard-server wget -qO- 127.0.0.1:9999/health` |

Adminer und PostgreSQL hören nur auf `127.0.0.1`. Von einem anderen Rechner
erreicht man sie über einen SSH-Tunnel, z. B.
`ssh -L 8081:127.0.0.1:8081 <server>`.

### Häufige Probleme

| Symptom | Ursache |
|---|---|
| `docker compose up` bricht mit „set VPN_MANAGER_TOKEN in .env“ ab | Pflichtwert fehlt in `.env` |
| VPN-Config liefert `503` | WireGuard-Container läuft nicht oder startet noch |
| Datenbank-Container startet nicht | zu einfaches `PASSWORD` |
| Spieler erreichen nach einem WireGuard-Neustart nichts | höchstens 15 s warten, bis `VpnResyncService` alles wiederhergestellt hat |
| Alle Spieler sind ausgeloggt | das Backend wurde neu gestartet; Sessions liegen nur im Speicher |
| Alle Spieler müssen ihre Config neu laden | `config/server/` wurde gelöscht, der Server hat neue Schlüssel |

## Konventionen

- **Backend:** Controller → Service → Repository; Controller geben DTOs aus
  `Diplo10.dto` zurück, nie Entities. Lombok auf Entities. Javadoc erklärt,
  *warum* etwas so gelöst ist. Logging vorerst über `System.out`/`System.err`.
- **Fehler:** `NotFoundException` (404), `IllegalArgumentException` oder
  `IllegalStateException` (400), `VpnUnavailableException` (503); die Zuordnung
  übernimmt `GlobalExceptionHandler`.
- **Sicherheit:** Endpunkte handeln immer als der eingeloggte Benutzer, nie
  über einen Benutzernamen als Parameter.
- **Commits:** Betreff im Imperativ, im Text das Problem und die Lösung.
