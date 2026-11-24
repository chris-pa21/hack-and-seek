> **Archiv – veraltet.** Diese englische Dokumentation stammt aus einem
> früheren Stand des Projekts und wird nicht mehr gepflegt; einige Angaben
> (z. B. Ports und Umgebungsvariablen) stimmen nicht mehr. Die aktuelle
> Dokumentation liegt in [docs/](../) und im [README](../../README.md).

# Hack & Seek — Project Documentation

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture Overview](#2-architecture-overview)
3. [Infrastructure & Deployment](#3-infrastructure--deployment)
4. [Backend](#4-backend)
   - 4.1 [Tech Stack](#41-tech-stack)
   - 4.2 [Configuration](#42-configuration)
   - 4.3 [Security](#43-security)
   - 4.4 [Entities & Database Schema](#44-entities--database-schema)
   - 4.5 [Repositories](#45-repositories)
   - 4.6 [Controllers & API Endpoints](#46-controllers--api-endpoints)
   - 4.7 [Services](#47-services)
   - 4.8 [Orchestration Layer](#48-orchestration-layer)
   - 4.9 [VPN Services](#49-vpn-services)
   - 4.10 [Exception Handling](#410-exception-handling)
5. [Frontend](#5-frontend)
   - 5.1 [Tech Stack](#51-tech-stack)
   - 5.2 [Routing & Auth Guards](#52-routing--auth-guards)
   - 5.3 [State Management (Pinia)](#53-state-management-pinia)
   - 5.4 [API Services Layer](#54-api-services-layer)
   - 5.5 [Views](#55-views)
   - 5.6 [Components](#56-components)
   - 5.7 [Styling](#57-styling)
6. [VPN Manager](#6-vpn-manager)
7. [Game Lifecycle](#7-game-lifecycle)
8. [Network Isolation Model](#8-network-isolation-model)
9. [Data Relationships](#9-data-relationships)
10. [Team](#10-team)

---

## 1. Project Overview

**Hack & Seek** is an Attack-Defense Capture The Flag (CTF) platform developed as a diploma thesis project. Players are organized into teams and compete by simultaneously attacking each other's containerized challenge services while defending their own.

**Core Mechanics:**
- Each team receives an identical containerized challenge service to defend
- Teams attack opponents' containers to capture flags
- Defending by blocking ports causes point penalties to balance offense and defense
- WireGuard VPN enforces strict per-game network isolation, ensuring teams can only reach services within the same game

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                      Docker Host                        │
│                                                         │
│  ┌──────────┐    ┌──────────┐    ┌───────────────────┐  │
│  │ Frontend │    │ Backend  │    │  WireGuard + VPN  │  │
│  │  Vue.js  │◄──►│Spring Boot│◄──►│  Manager (Flask) │  │
│  │  :80     │    │  :8080   │    │  :51820 / :9999  │  │
│  └──────────┘    └────┬─────┘    └───────────────────┘  │
│                       │                    │             │
│                  ┌────▼─────┐              │             │
│                  │PostgreSQL│      ┌───────▼──────┐      │
│                  │  :5432   │      │ Game Networks│      │
│                  └──────────┘      │ net_{game}   │      │
│                                    │ per-game     │      │
│                  ┌──────────┐      │ containers   │      │
│                  │ Adminer  │      └──────────────┘      │
│                  │  :8081   │                            │
│                  └──────────┘                            │
└─────────────────────────────────────────────────────────┘
```

The backend communicates with the Docker daemon via the Unix socket (`/var/run/docker.sock`) to manage containers and networks dynamically at runtime.

---

## 3. Infrastructure & Deployment

### Docker Compose Services

| Service | Image | Ports | Description |
|---|---|---|---|
| `frontend` | Built from `has-frontend/Dockerfile` | 80 | Vue.js app served via Nginx |
| `backend` | Built from `has-backend/Dockerfile` | 8080 | Spring Boot REST API |
| `database` | `postgres:15-alpine` | 5432 | PostgreSQL database |
| `adminer` | `adminer` | 8081 | Database admin UI |
| `wireguard-server` | Built from `has-vpn/Dockerfile` | 51820/UDP, 9999/TCP | WireGuard VPN + Flask firewall manager |

The backend container mounts `/var/run/docker.sock` to allow Docker API access. The WireGuard container requires `NET_ADMIN` capability and `SYS_MODULE` for kernel-level networking and iptables management.

### Environment Variables (`.env`)

| Variable | Default | Description |
|---|---|---|
| `PASSWORD` | `***REMOVED***` | PostgreSQL password |
| `FRONTEND_PORT` | `80` | Frontend exposed port |
| `BACKEND_PORT` | `8080` | Backend exposed port |
| `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `ADMINER_PORT` | `8081` | Adminer UI port |
| `WIREGUARD_PORT` | `51820` | WireGuard UDP port |
| `VPN_MANAGER_PORT` | `9999` | Flask VPN manager port |

### Dockerfiles

**Backend** (`has-backend/Dockerfile`):
- Multi-stage build: `eclipse-temurin:21-jdk` (build) → `eclipse-temurin:21-jre` (runtime)
- Builds with `./gradlew bootJar`
- Exposes port 8080

**Frontend** (`has-frontend/Dockerfile`):
- Multi-stage build: `node:20-alpine` (build) → `nginx:stable-alpine` (runtime)
- Builds with `npm ci && npm run build`
- Serves `dist/` via custom `nginx.conf`
- Exposes port 80

**VPN** (`has-vpn/Dockerfile`):
- Base: `lscr.io/linuxserver/wireguard:latest`
- Installs Python 3, pip, Flask
- Registers Flask app as an s6-overlay service for auto-restart
- Exposes UDP 51820 (WireGuard) and TCP 9999 (Flask API)

---

## 4. Backend

### 4.1 Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Runtime |
| Spring Boot | 3.5.6 | Web framework |
| Spring Data JPA | — | ORM / database access |
| Spring Security | — | Authentication & authorization |
| PostgreSQL | 15 | Relational database |
| Docker Java API | 3.7.0 | Container/network orchestration |
| Hibernate | — | JPA implementation (DDL auto-update) |
| Lombok | — | Boilerplate reduction |
| ModelMapper | — | Entity-DTO mapping |
| Micrometer Prometheus | — | Metrics / monitoring |
| Spring-dotenv | 4.0.0 | `.env` file support |
| Gradle | — | Build tool |

### 4.2 Configuration

**`application.properties`** key settings:

```properties
# Database
spring.datasource.url=jdbc:postgresql://database:5432/...
spring.jpa.hibernate.ddl-auto=update

# Session
spring.session.store-type=jdbc
server.servlet.session.timeout=604800s   # 7 days
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=lax
```

**`DockerConfig.java`** — Configures a singleton `DockerClient`:
- Transport: ApacheHttpClient5
- Socket: `unix:///var/run/docker.sock`
- Max connections: 100
- Connect timeout: 30s / Response timeout: 45s

### 4.3 Security

**`SecurityConfig.java`**:
- Password encoding: BCrypt
- CSRF: **disabled**
- CORS: enabled for `localhost`, `192.168.x.x`, and `10.x.x.x` ranges (credentials allowed)
- Session-based authentication
- Public endpoints: `/api/auth/**`
- All other endpoints require authentication
- Login endpoint: `POST /api/auth/login` (form-urlencoded)
- Logout endpoint: `POST /api/auth/logout` (invalidates session)

**`UserDetailsServiceImpl.java`**:
- Loads `Player` entity by username
- Maps to Spring Security `User` with authority `USER`

### 4.4 Entities & Database Schema

#### `Player`
```
Table: player
─────────────────────────────────────
player    VARCHAR  PK (username)
password  VARCHAR
email     VARCHAR
points    INTEGER
vpn_key   VARCHAR

Table: player_friends (join table)
player_player   FK → player.player
friends_player  FK → player.player
```

#### `Game`
```
Table: game
─────────────────────────────────────
id             BIGINT   PK (auto)
name           VARCHAR
created_by     VARCHAR  FK → player.player
created_at     TIMESTAMP
started_at     TIMESTAMP (null until started)
points         INTEGER
description    VARCHAR

Table: game_team_list (join table)
game_id         FK → game.id
team_list_name  FK → team.name
```

#### `Team`
```
Table: team
─────────────────────────────────────
name       VARCHAR  PK
game_id    INTEGER
challenge  VARCHAR  (Docker image name)

Table: team_team_members (join table)
team_name           FK → team.name
team_members_player FK → player.player
```

#### `ContainerEntity`
```
Table: container_entity
─────────────────────────────────────
id           BIGINT   PK (auto)
container_id VARCHAR  (Docker container ID)
team_name    VARCHAR  FK → team.name (OneToOne)
game_id      BIGINT   FK → game.id (ManyToOne)
network_id   VARCHAR
image        VARCHAR
created_at   TIMESTAMP
public_port  INTEGER  (host port mapped to container:80)
ip_address   VARCHAR  (container IP on game network)
```

#### `Friendrequest`
```
Table: friendrequest
─────────────────────────────────────
id         BIGINT   PK (auto)
requester  VARCHAR
receiver   VARCHAR
```

### 4.5 Repositories

| Repository | Custom Methods |
|---|---|
| `PlayerRepository` | `findByPlayer`, `findByEmail`, `findTopFriendsByPoints` (custom JPQL, pageable) |
| `GameRepository` | `findGamesByPlayer` (custom JPQL: games where user is member or creator) |
| `TeamRepository` | `findByName`, `findByGameId`, `findByGameIdAndName` |
| `ContainerRepository` | `findByTeam`, `findByGame`, `findByContainerId`, `deleteByContainerId` |
| `FriendrequestRepository` | `findByRequester`, `findByReceiver`, `existsByRequesterAndReceiver`, `getFriendrequestByRequesterAndReceiver` |

### 4.6 Controllers & API Endpoints

#### `AuthController` — `/api/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/register` | Public | Register new player. Body: `{username, email, password, confirmPassword}`. Returns 201 or 409 (duplicate). |
| `POST` | `/login` | Public | Form-based login (`application/x-www-form-urlencoded`). Sets session cookie. |
| `GET` | `/me` | Required | Returns `{username, authorities}` of current session. |
| `POST` | `/logout` | Required | Invalidates session. |

#### `GameController` — `/api/games`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/challenges` | Required | List Docker images labeled `type=challenge` with difficulty. |
| `POST` | `/create` | Required | Create game. Body: `{name, teams: [{name, challenge, players: [username]}]}`. Returns 201. |
| `GET` | `/` | Required | List all games the authenticated user is part of (member or creator). |
| `GET` | `/{gameId}` | Required | Get game details with team list. |
| `POST` | `/{gameId}/start` | Required | Start game (creator only). Triggers container + network creation. |
| `POST` | `/{id}/team` | Required | Add a team to an existing game. |
| `GET` | `/{gameId}/teams` | Required | List teams in game. |
| `GET` | `/{gameId}/teams/{teamId}` | Required | Get single team. |
| `POST` | `/{gameId}/{teamId}/addUser` | Required | Add player to team. |
| `PATCH` | `/{gameId}` | Required | Edit game name. |
| `DELETE` | `/{gameId}` | Required | Delete game. |
| `DELETE` | `/{gameId}/teams/{teamId}` | Required | Delete team from game. |

#### `PlayerController` — `/api/user`

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Returns `{username, points}` for queried player. |
| `GET` | `/points` | Returns integer points for current user. |

#### `FriendController` — `/api/friend`

| Method | Path | Description |
|---|---|---|
| `POST` | `/` | Send friend request. Body: `{requester, receiver}`. Returns 201. |
| `GET` | `/requests` | Get incoming friend requests for user (with points). |
| `POST` | `/acceptRequest` | Accept friend request. Creates bidirectional friendship. |
| `POST` | `/refuseRequest` | Reject friend request. |
| `GET` | `/friends` | List all friends of user. |
| `DELETE` | `/` | Remove friendship. |

#### `ScoreboardController` — `/api/scoreboard`

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Top 10 friends by points (includes current user if in top 10). |

#### `OrchestrationController` — `/api/orchestration`

| Method | Path | Description |
|---|---|---|
| `GET` | `/session/all` | List all active `ContainerEntity` records. |
| `GET` | `/session/game/{gameId}` | List containers for a specific game (includes IP addresses). |
| `DELETE` | `/stop/{gameId}` | Terminate game: remove all containers, delete network, clean DB. |

### 4.7 Services

#### `GameService`
- **`startGame(Game)`** — Core game start logic:
  1. Sets `game.started_at = now()`
  2. Calls `NetworkService.createIsolatedNetwork(game.getName())` → returns `networkId`
  3. For each team: calls `OrchestrationService.setupTeamContainer(game, team, networkId)`
- **`addTeam`** / **`deleteTeam`** — Team management on Game entity
- **`addPlayer`** / **`deletePlayer`** — Player assignment to team

#### `PlayerService`
- **`findByPlayer(username)`** — Returns Player or throws `NullPointerException`

#### `FriendService`
- **`acceptFriendRequest(requester, accepter)`** — Creates bidirectional friendship (both sides), deletes the Friendrequest
- **`refuseFriendRequest(requester, refuser)`** — Deletes Friendrequest without creating friendship
- **`getFriends(name)`** — Returns `Set<Player>`
- **`deleteFriend(player, friend)`** — Removes friendship from both sides

#### `FriendrequestService`
- **`sendRequest(requester, receiver)`** — Validates users exist, prevents self-requests and duplicates, creates `Friendrequest`
- **`getAllFriendRequests(username)`** — Returns all incoming requests

#### `ScoreboardService`
- **`getScoreboard(name)`** — Queries top 10 friends by points using pageable, includes current user

#### `ImageService`
- **`listChallengeImages()`** — Queries Docker daemon for images with label `type=challenge`, returns `List<ChallengeDTO>(name, difficulty)`

#### `MonitoringService`
- **`monitorAndScale()`** — Scheduled every 30 seconds. Lists running challenge containers. Placeholder for future auto-scaling logic.

### 4.8 Orchestration Layer

#### `OrchestrationService`

**`setupTeamContainer(Game, Team, networkId)`**:
1. Calls `ContainerService.spawnChallengeContainer(imageName, teamName, networkId)`
2. Parses response string `containerId:hostPort:ipAddress`
3. Builds and saves `ContainerEntity` with all relationships

**`terminateGame(Game)`**:
1. Loads all `ContainerEntity` for the game
2. Force-removes each Docker container
3. Deletes all `ContainerEntity` records from DB
4. Calls `NetworkService.deleteIsolatedNetwork(game.getName())`

#### `ContainerService`

**`spawnChallengeContainer(imageName, teamName, networkId)`**:
1. Creates container with:
   - Name: `{teamName}_{currentTimeMillis}` (avoids collisions)
   - Network: game's isolated Docker network
   - Port binding: container port 80 → random host port
   - RAM limit: 512 MB
   - CPU quota: 0.5 cores (`cpuQuota=50000`)
   - Restart policy: `unless-stopped`
   - Labels: `owner=teamName`, `type=challenge`
2. Starts the container
3. Inspects container to retrieve:
   - Assigned host port (from port bindings)
   - Container IP on the game network (from network settings)
4. Returns `"containerId:hostPort:ipAddress"`

#### `NetworkService`

**`createIsolatedNetwork(gameName)`**:
1. Creates Docker bridge network named `net_{gameName}`
2. Calls `VpnService.includeNetwork(networkId)` to attach WireGuard container
3. Returns the Docker network ID

**`deleteIsolatedNetwork(gameName)`**:
1. Calls `VpnService.excludeNetwork(networkId)` to detach WireGuard
2. Removes the Docker network

### 4.9 VPN Services

#### `VpnService`
- **`includeNetwork(networkId)`** — Connects the WireGuard container (`has-wireguard-server`) to the game network via Docker API
- **`excludeNetwork(networkId)`** — Disconnects WireGuard from the game network
- **`addPeer()`** / **`removePeer()`** — Stub methods (not yet implemented)

#### `VpnConfigService`
- Stub service with commented-out methods for parsing and compiling WireGuard peer configuration files

### 4.10 Exception Handling

**`GlobalExceptionHandler`** (`@RestControllerAdvice`):

| Exception | HTTP Status |
|---|---|
| `NullPointerException` | 404 Not Found |
| `IllegalStateException` | 400 Bad Request |

---

## 5. Frontend

### 5.1 Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Vue.js | 3.5.18 | UI framework |
| Vue Router | 4.5.1 | Client-side routing |
| Pinia | 3.0.3 | State management |
| Axios | 1.12.2 | HTTP client |
| TailwindCSS | 4.1.13 | Utility-first CSS |
| Vite | 7 | Build tool / dev server |
| Node.js | ≥20.19 | Runtime |

### 5.2 Routing & Auth Guards

**File:** `src/router/index.js`

| Route | Component | Auth Required |
|---|---|---|
| `/` | `HomeView` | Redirects to dashboard if logged in |
| `/login` | `LoginView` | Public |
| `/register` | `RegisterView` | Public |
| `/dashboard` | `DashboardView` | Yes |
| `/game/:id` | `GameDetailView` | Yes |
| `/*` | — | Redirects to dashboard or login |

The `beforeEach` navigation guard calls `authStore.checkAuth()` to verify session validity before every route transition.

### 5.3 State Management (Pinia)

**File:** `src/stores/auth.js`

**State:**
```js
user: null           // { username, authorities }
isAuthenticated: false
loading: false
```

**Actions:**

| Action | Method | Description |
|---|---|---|
| `login(credentials)` | `POST /api/auth/login` | Form-urlencoded login, sets user + isAuthenticated |
| `register(userData)` | `POST /api/auth/register` | JSON register, handles 409 conflict |
| `logout()` | `POST /api/auth/logout` | Clears state + session |
| `checkAuth()` | `GET /api/auth/me` | Silent auth check for route guards |

### 5.4 API Services Layer

All services use an Axios instance configured with `baseURL: ''` and `withCredentials: true` (session cookies).

#### `GameService.js`

| Method | HTTP | Endpoint |
|---|---|---|
| `createGame(payload)` | `POST` | `/api/games/create` |
| `getAllGames()` | `GET` | `/api/games` |
| `getGame(gameId)` | `GET` | `/api/games/{gameId}` |
| `startGame(gameId)` | `POST` | `/api/games/{gameId}/start` |
| `deleteGame(gameId)` | `DELETE` | `/api/games/{gameId}` |
| `getChallenges()` | `GET` | `/api/games/challenges` |
| `getContainers(gameId)` | `GET` | `/api/orchestration/session/game/{gameId}` |

#### `UserService.js`

| Method | HTTP | Endpoint |
|---|---|---|
| `getUser(username)` | `GET` | `/api/user?username={username}` |

#### `FriendService.js`

| Method | HTTP | Endpoint |
|---|---|---|
| `addFriend(receiver)` | `POST` | `/api/friend` |
| `getFriendRequests()` | `GET` | `/api/friend/requests` |
| `acceptFriendRequest(requester)` | `POST` | `/api/friend/acceptRequest` |
| `refuseFriendRequest(requester)` | `POST` | `/api/friend/refuseRequest` |
| `getAllFriends()` | `GET` | `/api/friend/friends` |
| `deleteFriend(friend)` | `DELETE` | `/api/friend` |

#### `ScoreboardService.js`

| Method | HTTP | Endpoint |
|---|---|---|
| `getScoreboard()` | `GET` | `/api/scoreboard` |

### 5.5 Views

#### `HomeView.vue`
Public landing page. Hero section with "Capture The Flag / On another level" headline, info sections explaining CTF and Hack & Seek mechanics. "GET STARTED" button redirects to `/login`.

#### `LoginView.vue`
Login form (username + password). Displays error on 401. Loading state on submit button. Link to `/register`.

#### `RegisterView.vue`
Registration form (username, email, password, confirm password). Client-side validations: email format (regex), password match. Success message with 3-second redirect to dashboard. Handles 400 (invalid) and 409 (user exists) errors.

#### `DashboardView.vue`
Two-column layout after login:
- **Left column:** `Scoreboard` component
- **Right column:** `Game`, `AddFriends`, `FriendRequests`, `Friends` components

#### `GameDetailView.vue`
Dual-mode game view with shared header (game name, status badge, back button, download config button):

**Lobby Mode** (`started_at == null`):
- Teams overview: team name, challenge with difficulty badge (color-coded), member chips
- "Start Game" button (creator only, with confirmation dialog)
- "Waiting for creator..." for non-creators

**Running Mode** (`started_at != null`):
- Flag submission form
- Leaderboard table:
  | # | Team | IP Address | Challenge | Points |
  |---|---|---|---|---|
  | rank | team name | container IP (monospace) | challenge name (color-coded) | points |
- Defeat log (placeholder)
- Download WireGuard config button

**Key computed properties:**
- `difficultyColor(name)` — `#00C853` (Easy ≤1), `#FCD34D` (Medium =2), `#DC0B00` (Hard >2)
- `isCreator` — compares `game.created_by.player` with current username
- `isStarted` — `game.started_at != null`
- `displayTeamName(name)` — strips `_gameN` suffix from team name

**Data flow on start:**
1. `startGame()` → `POST /api/games/{id}/start`
2. `fetchContainers()` → `GET /api/orchestration/session/game/{id}`
3. Builds `containerMap: { teamName → ipAddress }`
4. Leaderboard displays IP per team

### 5.6 Components

#### `Navbar.vue` (Unauthenticated)
Top navigation for public pages. Logo + "Hack&Seek" title. Login and Register buttons (desktop). Hamburger menu (mobile).

#### `UserNavbar.vue` (Authenticated)
Top navigation for authenticated pages. Username display. Points display (fetched from `/api/user/points` on mount). Logout button. Mobile-responsive.

#### `Game.vue`
Game list management card:
- "Create Game" button → opens `CreateGameModal`
- Lists all user's games with status badge (Running/Pending)
- Shows team count and challenge tags with difficulty colors
- Click → navigate to `/game/{id}`

#### `CreateGameModal.vue`
Full game creation wizard (modal overlay):
- Game name input
- Dynamic team builder:
  - Add/remove teams
  - Team name input
  - Challenge dropdown (with difficulty badges)
  - Player multi-select (filters already-assigned players)
  - Player chips with remove button
- Client-side validation: game name required, at least 1 team, all teams named
- Emits `close` and `create` events

#### `Scoreboard.vue`
Top 10 friends ranking:
- Refresh button with spin animation
- Table: `#`, username, points
- Gold highlight for rank 1
- Scrollable (max-height 500px)

#### `AddFriends.vue`
Friend search and request:
- Username input (search on Enter or button click)
- Displays found user: username, points, "Add Friend" button
- Button states: default, "Sending...", "✓ Sent"
- Error/success feedback messages

#### `FriendRequests.vue`
Incoming friend request management:
- Lists all incoming requests with username and points
- Accept (✓) and Refuse (✗) buttons per request
- Auto-refreshes list after action
- Scrollable (max-height 400px)

#### `Friends.vue`
Friend list management:
- Lists all friends with username and points
- Delete (✗) button per friend
- Refresh button with spin animation
- Auto-refreshes after deletion
- Scrollable (max-height 600px)

### 5.7 Styling

TailwindCSS 4 with dark theme and custom utility classes:

| Class | Purpose |
|---|---|
| `.primary-button` | Primary action button |
| `.secondary-button` | Secondary/neutral button |
| `.danger-button` | Destructive action button (red) |

**Color palette:**

| Token | Hex | Usage |
|---|---|---|
| Background dark | `#0A0F1E` | Page background |
| Surface | `#1F2937` | Cards, panels |
| Surface dark | `#0F172A` | Table headers, inputs |
| Text primary | `#E0E6F0` | Main text |
| Text muted | `#9CA3AF`, `#6B7280` | Secondary text |
| Green | `#00C853` | Easy difficulty, running state, success |
| Yellow | `#FCD34D` | Medium difficulty, pending state, rank 1 |
| Red | `#DC0B00` | Hard difficulty, danger actions |
| Blue | `#018EF4` | Accent |

**Font:** Fira Code (monospace) for IP addresses and code-like content.

---

## 6. VPN Manager

**File:** `has-vpn/vpn-manager.py`
**Runtime:** Flask (Python 3), port 9999

The VPN manager is a minimal Flask API running inside the WireGuard container. It manages iptables firewall rules to enforce per-game network isolation.

### Initialization

On startup, `init_firewall()` runs:
1. Flushes all existing FORWARD chain rules
2. Sets default FORWARD policy to `DROP` (deny all)
3. Adds stateful rule: `ESTABLISHED,RELATED` connections → `ACCEPT`

### Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | Returns `{"status": "ok"}` |
| `POST` | `/api/game` | Add per-game iptables rule. Body: `{"subnet": "10.8.19.0/24"}`. Validates CIDR, adds: `iptables -A FORWARD -s {subnet} -d {subnet} -j ACCEPT` |
| `DELETE` | `/api/game` | Remove per-game rule. Same body. Removes: `iptables -D FORWARD -s {subnet} -d {subnet} -j ACCEPT` |
| `GET` | `/api/game` | List active FORWARD rules. Runs `iptables -L FORWARD -n`, returns parsed lines. |

### Security Model
- **Default-deny:** All inter-container traffic is blocked by default
- **Per-game allow:** Only traffic within the same game subnet (`-s subnet -d subnet`) is permitted
- **Safe execution:** All iptables commands use `subprocess.run()` with argument lists (no shell injection)
- **CIDR validation:** Subnet input is validated before use

---

## 7. Game Lifecycle

```
1. CREATE
   POST /api/games/create
   ├── Creates Game entity (started_at = null)
   └── Creates Team entities with assigned players

2. CONFIGURE (optional, pre-start)
   - Add/remove teams
   - Add/remove players from teams

3. START
   POST /api/games/{gameId}/start  (creator only)
   ├── Sets game.started_at = now()
   ├── NetworkService.createIsolatedNetwork("net_{gameName}")
   │   └── VpnService.includeNetwork() → attaches WireGuard to network
   └── For each team:
       └── OrchestrationService.setupTeamContainer()
           └── ContainerService.spawnChallengeContainer()
               ├── Creates container (512MB RAM, 0.5 CPU)
               ├── Attaches to game network
               ├── Binds random host port → container:80
               └── Reads IP address from Docker inspect

4. RUN
   - Teams attack each other's container IPs
   - Players submit captured flags
   - Leaderboard shows team rankings with IPs

5. CLEANUP
   DELETE /api/orchestration/stop/{gameId}
   ├── Force-removes all containers
   ├── Deletes ContainerEntity records
   └── NetworkService.deleteIsolatedNetwork()
       └── VpnService.excludeNetwork() → detaches WireGuard
```

---

## 8. Network Isolation Model

```
┌─────────────────────────────────────────────────┐
│  Docker Network: net_{gameName}                 │
│                                                 │
│  ┌─────────────┐  ┌─────────────┐               │
│  │ Team A      │  │ Team B      │  ...           │
│  │ Container   │  │ Container   │               │
│  │ 172.x.x.2  │  │ 172.x.x.3  │               │
│  └─────────────┘  └─────────────┘               │
│                                                 │
│  ┌────────────────────────────────────┐         │
│  │ WireGuard Server (attached)        │         │
│  └────────────────────────────────────┘         │
└─────────────────────────────────────────────────┘
         ↑ iptables rule: FORWARD -s subnet -d subnet ACCEPT
         (only same-game subnet traffic allowed)
```

- Each game gets its own Docker bridge network
- All team containers and the WireGuard server share the network
- The VPN manager enforces default-deny at the iptables level
- Players connect to the game via WireGuard VPN to access container IPs
- Blocking ports (defending) causes point penalties

---

## 9. Data Relationships

```
Player ──────────────────── Player
  │  (ManyToMany: friends)     │
  │                            │
  └──── created_by ──── Game ──┘
                          │
                    ManyToMany
                          │
                        Team ──── ContainerEntity
                          │            │
                    ManyToMany         │
                          │            │
                        Player       Game
```

| Relationship | Cardinality | Description |
|---|---|---|
| `Player ↔ Player` | M:N (self-referential) | Bidirectional friendships |
| `Player → Game` | M:1 | Game creator |
| `Game ↔ Team` | M:N | Game contains multiple teams |
| `Team ↔ Player` | M:N | Team members |
| `Team ↔ ContainerEntity` | 1:1 | Each team has one container |
| `Game → ContainerEntity` | 1:N | Game owns all its containers |

---

## 10. Team

| Member | Role |
|---|---|
| Christian Parushev | Vue.js Frontend |
| Andreas Maurer | VPN / WireGuard |
| Felix Bayerl | Database |
| Andrä Florian | Container Orchestration |
| Yunus Eren | Spring Boot Backend |
