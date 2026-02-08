# 👾 Hack and Seek

Hack and Seek ist eine Attack-Defense-CTF-Plattform für Einzelspieler oder
Teams, bei der es darum geht, gegnerische Schwachstellen zu finden und Flags zu
erbeuten. Gleichzeitig muss der eigene Container abgesichert werden – Patchen
ist erwünscht, doch das bloße Schließen der Ports führt zu Punktabzug. Dank
strikter Netzwerkisolierung bleibt das Spielfeld sicher: Die Kommunikation ist
auf die aktuelle Runde begrenzt, sodass IP-Kenntnisse aus anderen Sessions
wertlos sind.

## 👥 Mitglieder

| Name               | Aufgabe                  |
| ------------------ | ------------------------ |
| Christian Parushev | Vue Frontend             |
| Andreas Maurer     | VPN (WireGuard)          |
| Felix Bayerl       | Datenbank                |
| Andrä Florian      | Container Orchestrierung |
| Yunus Eren         | Spring Backend           |

## 💻 Technologien

- Docker
- Java 21
- Vue.js latest

## 🛠️ Anwendung

Kopiere das `env-example` zu einem `.env` und ersätze **alle** mit eckigen
Klammern markierten Platzhalter, wie `[PASSWORD]` mit sinnvollen Werten.

⚠️ Das Passwort darf nicht zu einfach sein (z.B.: 1234), da sonst, je nach
Datenbank und Version, der Container nicht starten könnte. Gehe auch sicher,
dass die gewählten Ports nicht bereits besetzt sind.

**Windows**

```powershell
C:\> docker compose -p has up -d
```

**Linux**

```bash
$ docker compose -p has up -d
```
