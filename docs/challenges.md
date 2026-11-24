# Challenges

Eine Challenge ist ein Docker-Image mit einem absichtlich verwundbaren Dienst.
Jedes Team bekommt beim Spielstart einen eigenen Container daraus, verteidigt
ihn und greift die Container der anderen Teams an. Wie ein Spiel abläuft,
beschreibt [spielablauf.md](spielablauf.md).

## Mitgelieferte Challenges

| Challenge | Dienst | Schwierigkeit | Punkte |
|---|---|---|---|
| `ping-me` | Netzwerk-Diagnose-Webseite (nginx, PHP) | 1 | 100 |
| `note-keeper` | Notiz-App (nginx, PHP) | 1 | 100 |
| `leaky-blog` | persönlicher Blog (nginx) | 1 | 100 |

Alle basieren auf Ubuntu 22.04 und bieten einen Webdienst auf Port 80 und SSH
auf Port 22. Die Schwachstellen werden hier bewusst nicht beschrieben.

> **Hinweis:** Die drei Challenges sind Beispiele, die zeigen, wie eine
> Challenge aufgebaut ist. Weil das Repository öffentlich ist, lassen sich
> ihre Schwachstellen im Quellcode nachlesen. Das ist kein Problem: Für ein
> echtes Spiel erstellt man eigene Challenges, siehe
> [eigene-challenge-erstellen.md](eigene-challenge-erstellen.md).

## Images bauen

Docker Compose baut die Challenges **nicht** mit. Sie müssen einmal (und nach
jeder Änderung) gebaut werden; der Image-Name ist der Name, unter dem die
Challenge in der Oberfläche erscheint:

```bash
for c in ping-me note-keeper leaky-blog; do
  docker build -t "$c" "images/$c"
done
```

Das Backend findet jedes lokale Image mit dem Label `type=challenge`
automatisch; ein Neustart ist nicht nötig.

## Aufbau einer Challenge

```
images/<name>/
├── Dockerfile
├── entrypoint.sh     # startet die Dienste, SSH im Vordergrund
├── healthcheck.sh    # prüft, ob der Dienst noch funktioniert
└── html/             # Inhalte des Webdienstes
```

### Labels

| Label | Pflicht | Bedeutung |
|---|---|---|
| `type="challenge"` | ja | macht das Image zur Challenge; ohne das Label wird es nicht angeboten und beim Erstellen eines Spiels abgelehnt |
| `flag_path` | empfohlen | absoluter Pfad, an den das Backend die Flag schreibt, z. B. `/home/admin/flag.txt`. Fehlt er oder ist er unsicher (alles außer `A–Z a–z 0–9 . _ / -`, `..` oder ein `/` am Ende), gilt `/home/flag.txt`. |
| `points` | nein | Punkte für das Erbeuten der Flag, Standard 100 |
| `difficulty` | nein | Schwierigkeit zur Anzeige, Standard 0 |

### Was das Backend beim Start setzt

- **Flag:** eine zufällige `HAS{…}`-Flag, geschrieben als `root` nach
  `flag_path`. Eine Flag, die schon im Image steht, wird überschrieben;
  Besitzer und Rechte der Datei bleiben, wie das Dockerfile sie gesetzt hat.
- **Root-Passwort:** 24 zufällige Zeichen per `chpasswd`. Das Passwort im
  Dockerfile ist nur ein Platzhalter.
- **Grenzen:** 128 MB RAM, 0,5 CPU, Neustart-Policy `unless-stopped`, nur das
  Netzwerk des Spiels.

### `entrypoint.sh`

Startet die Dienste und hält den Container am Leben. Die mitgelieferten
Challenges starten nginx (und PHP-FPM) im Hintergrund und `sshd -D` im
Vordergrund:

```bash
#!/bin/bash
nginx
exec /usr/sbin/sshd -D
```

SSH wird für die Verteidiger gebraucht: Sie melden sich als `root` mit dem
Passwort aus der Spielansicht an. Im Dockerfile deshalb
`PermitRootLogin yes`; `MaxAuthTries 3` und `LoginGraceTime 20` bremsen das
Durchprobieren des Root-Passworts.

### `healthcheck.sh`

Liegt im Image unter `/healthcheck.sh` und muss ausführbar sein. Das
Monitoring ruft es alle 30 Sekunden im Container auf (Timeout 5 s); Exit-Code
`0` heißt „funktioniert“, alles andere disqualifiziert das Team.

Das Skript soll prüfen, dass der Dienst **noch das tut, wofür er da ist**, und
nicht nur, dass er läuft. Sonst könnten Teams eine Schwachstelle „patchen“,
indem sie die Funktion abschalten. Beispiel aus `note-keeper`:

```bash
#!/bin/bash
pgrep nginx   > /dev/null 2>&1 || exit 1
pgrep php-fpm > /dev/null 2>&1 || exit 1
RESPONSE=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:80/)
[ "$RESPONSE" -ge 200 ] && [ "$RESPONSE" -lt 400 ] || exit 1
[ -d /var/www/notes ] || exit 1
exit 0
```

Unabhängig vom Skript prüft das Monitoring immer, dass die Flag unverändert
an `flag_path` liegt und `http://localhost:80/` mit 200–399 antwortet. Das
Image braucht dafür `curl`.

## Eigene Challenge hinzufügen

Schritt für Schritt in [eigene-challenge-erstellen.md](eigene-challenge-erstellen.md).
