# has-vpn

WireGuard-Server der Plattform, erweitert um den VPN-Manager.

| Datei | Inhalt |
|---|---|
| `Dockerfile` | baut auf `linuxserver/wireguard` auf, installiert Flask und startet den VPN-Manager als zusätzlichen Dienst im Container |
| `vpn-manager.py` | kleine Flask-API auf Port 9999 (nicht veröffentlicht), über die das Backend die iptables-Regeln setzt: welcher Spieler welches Spiel-Netzwerk erreicht |
| `test_vpn_manager.py` | Tests für den VPN-Manager; `iptables` wird dabei nicht ausgeführt |

```bash
pytest has-vpn          # aus dem Hauptordner, braucht flask und pytest
```

Der Container wird über `docker-compose.yml` gestartet (Dienst
`wireguard-server`, Containername `has-wireguard-server`). Die
Server-Schlüssel liegen in `config/server/` auf dem Host.

Wie Schlüssel, Firewall-Regeln und die Wiederherstellung nach einem Neustart
funktionieren: [docs/vpn-und-isolation.md](../docs/vpn-und-isolation.md).
