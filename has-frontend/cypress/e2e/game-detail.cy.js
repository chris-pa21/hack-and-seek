describe('Game Detail Page', () => {
  const pendingGame = {
    id: 1,
    name: 'Test Game',
    started_at: null,
    created_by: { player: 'testuser' },
    team_list: [
      {
        name: 'Alpha_game1',
        challenge: 'web-easy',
        score: 0,
        defeated: false,
        team_members: [{ player: 'testuser' }, { player: 'alice' }],
      },
      {
        name: 'Beta_game1',
        challenge: 'crypto-medium',
        score: 0,
        defeated: false,
        team_members: [{ player: 'bob' }],
      },
    ],
  }

  const runningGame = {
    id: 1,
    name: 'Live Game',
    started_at: '2025-01-01T12:00:00',
    created_by: { player: 'testuser' },
    team_list: [
      {
        name: 'Alpha_game1',
        challenge: 'web-easy',
        score: 150,
        defeated: false,
        team_members: [{ player: 'testuser' }, { player: 'alice' }],
      },
      {
        name: 'Beta_game1',
        challenge: 'crypto-medium',
        score: 75,
        defeated: false,
        team_members: [{ player: 'bob' }],
      },
    ],
  }

  function stubGameAPIs(game) {
    cy.intercept('GET', `/api/games/${game.id}`, {
      statusCode: 200,
      body: game,
    }).as('getGame')

    cy.intercept('GET', '/api/games/challenges', {
      statusCode: 200,
      body: [
        { name: 'web-easy', difficulty: 1 },
        { name: 'crypto-medium', difficulty: 2 },
        { name: 'pwn-hard', difficulty: 3 },
      ],
    }).as('getChallenges')

    cy.intercept('GET', `/api/orchestration/session/game/${game.id}`, {
      statusCode: 200,
      body: [
        { team: { name: 'Alpha_game1' }, ipAddress: '10.8.19.2' },
        { team: { name: 'Beta_game1' }, ipAddress: '10.8.19.3' },
      ],
    }).as('getContainers')

    cy.intercept('GET', `/api/games/${game.id}/defeat-log`, {
      statusCode: 200,
      body: [],
    }).as('getDefeatLog')

    cy.intercept('GET', '/api/user/points*', {
      statusCode: 200,
      body: 100,
    }).as('getPoints')
  }

  // ── Lobby View (Pending Game) ──────────────────────────────────────────

  describe('Lobby View (Pending Game)', () => {
    beforeEach(() => {
      cy.login('testuser')
      stubGameAPIs(pendingGame)
      cy.visit('/game/1')
      cy.wait('@getGame')
    })

    it('displays the game name and Pending status', () => {
      cy.contains('h1', 'Test Game')
      cy.contains('Pending')
    })

    it('shows the Back button', () => {
      cy.contains('button', '← Back')
    })

    it('displays teams with members', () => {
      cy.contains('h2', 'Teams')
      cy.contains('Alpha')
      cy.contains('testuser')
      cy.contains('alice')
      cy.contains('Beta')
      cy.contains('bob')
    })

    it('shows challenge names with difficulty', () => {
      cy.contains('web-easy')
      cy.contains('Easy')
      cy.contains('crypto-medium')
      cy.contains('Medium')
    })

    it('shows Start Game button for the creator', () => {
      cy.contains('button', 'Start Game').should('be.visible')
    })

    it('does not show Start Game button for non-creators', () => {
      const otherUserGame = {
        ...pendingGame,
        created_by: { player: 'someone_else' },
      }
      cy.intercept('GET', '/api/games/1', {
        statusCode: 200,
        body: otherUserGame,
      }).as('getGameOther')

      cy.visit('/game/1')
      cy.wait('@getGameOther')

      cy.contains('Waiting for the game creator to start the game')
      cy.contains('button', 'Start Game').should('not.exist')
    })

    it('does not show Download config button before game starts', () => {
      cy.contains('button', /Download game.*\.cfg/).should('not.exist')
    })
  })

  // ── Running View ───────────────────────────────────────────────────────

  describe('Running View', () => {
    beforeEach(() => {
      cy.login('testuser')
      stubGameAPIs(runningGame)
      cy.visit('/game/1')
      cy.wait('@getGame')
    })

    it('displays Running status', () => {
      cy.contains('h1', 'Live Game')
      cy.contains('Running')
    })

    it('shows the Download config button', () => {
      cy.contains('button', /Download game.*\.cfg/).should('be.visible')
    })

    it('displays the flag submission form', () => {
      cy.contains('h2', 'Submit Flag')
      cy.get('input[placeholder="HAS{...}"]').should('be.visible')
      cy.contains('button', 'Submit')
    })

    it('displays the leaderboard with teams sorted by score', () => {
      cy.contains('h2', 'Leaderboard')
      // Alpha has 150 pts, Beta has 75 pts — Alpha should be first
      cy.get('.grid.grid-cols-12').eq(1).within(() => {
        cy.contains('Alpha')
        cy.contains('150 pts')
      })
    })

    it('displays IP addresses in leaderboard', () => {
      cy.contains('10.8.19.2')
      cy.contains('10.8.19.3')
    })

    it('displays the defeat log section', () => {
      cy.contains('h2', 'Defeat Log')
      cy.contains('No captures yet.')
    })

    it('shows defeat log entries when present', () => {
      cy.intercept('GET', '/api/games/1/defeat-log', {
        statusCode: 200,
        body: [
          {
            id: 1,
            attackingTeam: 'Alpha_game1',
            defeatedTeam: 'Beta_game1',
            pointsAwarded: 100,
            submittedAt: '2025-01-01T12:30:00',
          },
        ],
      }).as('getDefeatLogWithEntries')

      cy.visit('/game/1')
      cy.wait('@getDefeatLogWithEntries')

      cy.contains('Alpha')
      cy.contains('captured')
      cy.contains('Beta')
      cy.contains('+100 pts')
    })

    // ── Flag Submission ────────────────────────────────────────────────

    it('submits a valid flag successfully', () => {
      cy.intercept('POST', '/api/games/1/submit-flag', {
        statusCode: 200,
        body: { success: true, message: 'Flag accepted! +50 points' },
      }).as('submitFlag')

      cy.get('input[placeholder="HAS{...}"]').type('HAS{test_flag_123}')
      cy.contains('button', 'Submit').click()

      cy.wait('@submitFlag')
      cy.contains('Flag accepted! +50 points')
    })

    it('shows error for invalid flag', () => {
      cy.intercept('POST', '/api/games/1/submit-flag', {
        statusCode: 200,
        body: { success: false, message: 'Invalid flag' },
      }).as('submitFlagInvalid')

      cy.get('input[placeholder="HAS{...}"]').type('HAS{wrong}')
      cy.contains('button', 'Submit').click()

      cy.wait('@submitFlagInvalid')
      cy.contains('Invalid flag')
    })

    it('clears input after successful flag submission', () => {
      cy.intercept('POST', '/api/games/1/submit-flag', {
        statusCode: 200,
        body: { success: true, message: 'Flag accepted!' },
      }).as('submitFlag')

      cy.get('input[placeholder="HAS{...}"]').type('HAS{flag}')
      cy.contains('button', 'Submit').click()
      cy.wait('@submitFlag')

      cy.get('input[placeholder="HAS{...}"]').should('have.value', '')
    })

    it('does not submit empty flag', () => {
      cy.get('input[placeholder="HAS{...}"]').should('have.value', '')
      cy.contains('button', 'Submit').click()
      // No network request should be made
      cy.get('@submitFlag').should('not.exist')
    })

    it('submits flag on Enter key', () => {
      cy.intercept('POST', '/api/games/1/submit-flag', {
        statusCode: 200,
        body: { success: true, message: 'Flag accepted!' },
      }).as('submitFlagEnter')

      cy.get('input[placeholder="HAS{...}"]').type('HAS{enter_flag}{enter}')
      cy.wait('@submitFlagEnter')
    })

    it('shows defeated state when team is defeated', () => {
      const defeatedGame = {
        ...runningGame,
        team_list: runningGame.team_list.map((t) =>
          t.name === 'Alpha_game1' ? { ...t, defeated: true } : t,
        ),
      }

      cy.intercept('GET', '/api/games/1', {
        statusCode: 200,
        body: defeatedGame,
      }).as('getDefeatedGame')

      cy.visit('/game/1')
      cy.wait('@getDefeatedGame')

      cy.contains('Your team has been defeated')
      cy.get('input[placeholder="HAS{...}"]').should('not.exist')
    })
  })

  // ── Game Not Found ─────────────────────────────────────────────────────

  describe('Game Not Found', () => {
    it('shows "Game not found" for invalid game ID', () => {
      cy.login('testuser')
      cy.intercept('GET', '/api/games/999', {
        statusCode: 404,
        body: {},
      }).as('getGameNotFound')

      cy.intercept('GET', '/api/games/challenges', {
        statusCode: 200,
        body: [],
      }).as('getChallenges')

      cy.intercept('GET', '/api/user/points*', {
        statusCode: 200,
        body: 0,
      }).as('getPoints')

      cy.visit('/game/999')
      cy.wait('@getGameNotFound')
      cy.contains('Game not found')
    })
  })
})
