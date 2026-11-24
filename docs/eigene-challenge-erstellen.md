# Eigene Challenge erstellen

Anleitung, wie man eine neue Challenge für Hack & Seek baut. Die Labels und
was das Backend beim Start setzt, stehen in [challenges.md](challenges.md);
hier geht es um das Vorgehen.

Die mitgelieferten Challenges (`ping-me`, `note-keeper`, `leaky-blog`) sind
Beispiele. Ihre Lösungen stehen im öffentlichen Repository; für ein echtes
Spiel baut man am besten eigene, die die Spieler noch nicht kennen.

## Was eine gute Challenge ausmacht

Bei Attack-Defense ist jede Challenge Ziel und eigener Server zugleich: Ein
Team verteidigt seinen Container und greift die der anderen an, die dieselbe
oder eine andere Challenge haben können. Daraus folgen ein paar Regeln:

- **Die Schwachstelle muss patchbar sein.** Ein Team soll sie finden und
  beheben können, ohne den Dienst abzuschalten. Gut eignen sich Fehler im
  Code oder in der Konfiguration, etwa unsaubere Eingabeprüfung, zu
  großzügige Dateirechte oder schwache Zugangsdaten.
- **Der Dienst muss eine echte Funktion haben**, die `healthcheck.sh` prüfen
  kann. Wer „patcht“, indem er die Funktion entfernt, soll auffliegen.
- **Die Flag liegt dort, wo man nach dem Einbruch hinkommt**, aber nicht
  offen im Web-Verzeichnis. Der Weg zur Flag ist die eigentliche Aufgabe.
- **Mehrere Wege sind gut.** Zwei oder drei Schwachstellen unterschiedlicher
  Schwierigkeit machen das Spiel spannender, als wenn alle denselben Fehler
  suchen.
- **Klein bleiben:** Der Container hat 128 MB RAM und eine halbe CPU.

## Schritt für Schritt

### 1. Ordner anlegen

Am einfachsten eine bestehende Challenge kopieren und umbenennen:

```bash
cp -r images/ping-me images/<name>
```

```
images/<name>/
├── Dockerfile
├── entrypoint.sh
├── healthcheck.sh
└── html/
```

### 2. Dockerfile schreiben

```dockerfile
FROM ubuntu:22.04
ENV DEBIAN_FRONTEND=noninteractive

LABEL type="challenge"
LABEL flag_path="/home/<user>/flag.txt"
LABEL points="100"
LABEL difficulty="1"

# Webserver, SSH für die Verteidiger, curl für das Monitoring
RUN apt-get update && apt-get install -y \
    openssh-server nginx curl \
    && rm -rf /var/lib/apt/lists/*

# SSH: Verteidiger melden sich als root an; das Passwort setzt das Backend
RUN mkdir /run/sshd \
    && echo "root:platzhalter" | chpasswd \
    && sed -i 's/#PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config \
    && printf 'MaxAuthTries 3\nLoginGraceTime 20\n' >> /etc/ssh/sshd_config

# Benutzer, dem die Flag gehört
RUN useradd -m -s /bin/bash <user> \
    && echo "flag{placeholder}" > /home/<user>/flag.txt \
    && chown <user>:<user> /home/<user>/flag.txt \
    && chmod 400 /home/<user>/flag.txt

COPY html/ /var/www/html/
COPY entrypoint.sh /entrypoint.sh
COPY healthcheck.sh /healthcheck.sh
RUN chmod +x /entrypoint.sh /healthcheck.sh

EXPOSE 22 80
ENTRYPOINT ["/entrypoint.sh"]
```

Worauf es ankommt:

| Punkt | Warum |
|---|---|
| `type="challenge"` | Ohne das Label wird das Image nicht angeboten. |
| `flag_path` | Absoluter Pfad, nur `A–Z a–z 0–9 . _ / -`. Sonst schreibt das Backend die Flag nach `/home/flag.txt`. |
| Flag-Datei schon im Image | Das Backend überschreibt nur den Inhalt; Besitzer und Rechte legt das Dockerfile fest. So bestimmt man, wer die Flag lesen darf. |
| `curl` | Das Monitoring ruft damit `http://localhost:80/` im Container auf. |
| SSH mit Root-Login | Die Verteidiger brauchen Zugang zum eigenen Container. Das Passwort im Dockerfile ist nur ein Platzhalter. |
| Dienst nicht als `root` | Sonst ist die Flag nach dem ersten Einbruch sofort lesbar und es gibt keinen zweiten Schritt. |
| keine echten Geheimnisse | Das Image steht im Repository und ist für alle lesbar. |

### 3. `entrypoint.sh`

Startet die Dienste; SSH läuft im Vordergrund und hält den Container am Leben:

```bash
#!/bin/bash
nginx
exec /usr/sbin/sshd -D
```

Braucht der Dienst weitere Prozesse (z. B. PHP-FPM), werden sie davor
gestartet.

### 4. `healthcheck.sh`

Das Monitoring führt das Skript alle 30 Sekunden im Container aus und gibt
ihm 5 Sekunden. Exit-Code `0` = in Ordnung, alles andere disqualifiziert das
Team. Das Skript sollte prüfen:

1. dass die nötigen Prozesse laufen,
2. dass der Dienst antwortet,
3. dass die **Funktion** noch da ist, also das, was ein Benutzer mit dem
   Dienst tut.

```bash
#!/bin/bash
pgrep nginx > /dev/null 2>&1 || exit 1
RESPONSE=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:80/)
[ "$RESPONSE" -ge 200 ] && [ "$RESPONSE" -lt 400 ] || exit 1
# Funktion prüfen, z. B. dass eine Seite den erwarteten Inhalt liefert
curl -s http://localhost:80/ | grep -q "<erwarteter Text>" || exit 1
exit 0
```

Nicht zu streng sein: Ein sauberer Patch darf den Healthcheck nicht brechen.
Geprüft wird das Verhalten für normale Benutzer, nicht der genaue Code.

### 5. Bauen und lokal testen

```bash
docker build -t <name> images/<name>
docker run -d --name test-<name> <name>

# Healthcheck muss bestehen
docker exec test-<name> /healthcheck.sh && echo "Healthcheck ok"

# Webdienst ansehen
docker exec test-<name> curl -s http://localhost:80/ | head

# Schwachstelle selbst ausnutzen, dann aufräumen
docker rm -f test-<name>
```

### 6. Im Spiel testen

1. In der Oberfläche ein Spiel mit zwei Teams und der neuen Challenge
   erstellen und starten.
2. Per VPN den gegnerischen Container angreifen und die Flag abgeben.
3. Den eigenen Container per SSH patchen und mindestens eine Minute warten:
   Das Monitoring darf das Team nicht disqualifizieren.

## Checkliste

- [ ] Label `type="challenge"` und ein gültiger `flag_path`
- [ ] Flag-Datei im Image mit den gewünschten Rechten
- [ ] Webdienst antwortet auf Port 80 mit 200–399
- [ ] `curl`, `openssh-server`, `PermitRootLogin yes`
- [ ] `entrypoint.sh` und `healthcheck.sh` ausführbar
- [ ] Healthcheck prüft die Funktion, nicht nur den Prozess
- [ ] Schwachstelle ist ausnutzbar **und** patchbar, ohne den Healthcheck zu brechen
- [ ] keine echten Passwörter oder Geheimnisse im Image
- [ ] Test-Images nach dem Ausprobieren gelöscht (`docker rmi <name>`), sonst erscheinen sie in der Challenge-Auswahl
