# has-frontend

Weboberfläche von Hack & Seek: Vue 3 mit Vite, Pinia, Vue Router,
Tailwind CSS 4 und axios. Im Betrieb liefert nginx den Build aus und leitet
`/api/` an das Backend weiter (`nginx.conf`, `Dockerfile`).

## Befehle

```bash
npm install
npm run dev        # Entwicklungsserver mit Hot Reload, http://localhost:5173
npm run build      # Produktions-Build nach dist/
npm run preview    # Build lokal ansehen
npm run format     # Prettier über src/
npm run cy:run     # Cypress-E2E-Tests (Entwicklungsserver muss laufen)
npm run cy:open    # Cypress interaktiv
```

`npm run dev` leitet `/api` an `http://localhost:8080` weiter, also an ein
Backend, das mit `./gradlew bootRun` auf dem Host läuft. Die Cypress-Tests
ersetzen die API mit `cy.intercept` und brauchen kein Backend.

Node.js 20.19+ oder 22.12+.

## Weiterlesen

- Aufbau, Routen und Ordner: [docs/architektur.md](../docs/architektur.md#frontend)
- Lokale Entwicklung und Tests: [docs/entwicklung.md](../docs/entwicklung.md)
