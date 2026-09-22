describe('Dashboard', () => {
  beforeEach(() => {
    cy.login('testuser')
    cy.stubDashboardAPIs()
    cy.visit('/dashboard')
    cy.wait('@authMe')
  })

  // ── Layout ─────────────────────────────────────────────────────────────

  describe('Layout', () => {
    it('displays the UserNavbar with username and points', () => {
      cy.get('nav').within(() => {
        cy.contains('testuser')
        // points come from /api/user/me (stubbed with 100 in stubDashboardAPIs)
        cy.contains('100 pts')
        cy.contains('button', 'Logout')
      })
    })

    it('shows all dashboard sections', () => {
      cy.contains('h2', 'Ranking')
      cy.contains('h2', 'Game')
      cy.contains('h2', 'Add Friends')
      cy.contains('h2', 'Incoming Requests')
      cy.contains('h2', 'My Friends')
    })
  })

  // ── Scoreboard ─────────────────────────────────────────────────────────

  describe('Scoreboard', () => {
    it('displays ranking table with friends', () => {
      cy.contains('h2', 'Ranking')
      cy.contains('Top 10 friends')
      cy.contains('alice')
      cy.contains('500')
      cy.contains('bob')
      cy.contains('300')
    })

    it('refreshes scoreboard on reload button click', () => {
      cy.wait('@getScoreboard')

      cy.intercept('GET', '/api/scoreboard*', {
        statusCode: 200,
        body: [
          { username: 'alice', points: 600 },
          { username: 'bob', points: 400 },
        ],
      }).as('getScoreboardRefresh')

      // Click the refresh button (the ↻ character)
      cy.contains('h2', 'Ranking').parent().parent().find('button').first().click()
      cy.wait('@getScoreboardRefresh')
      cy.contains('600')
    })
  })

  // ── Game Section ───────────────────────────────────────────────────────

  describe('Game Section', () => {
    it('shows empty state when no games exist', () => {
      cy.contains('No games yet. Create one!')
    })

    it('shows the Create Game button', () => {
      cy.contains('button', '+ Create Game')
    })

    it('displays games when they exist', () => {
      cy.intercept('GET', '/api/games', {
        statusCode: 200,
        body: [
          {
            id: 1,
            name: 'Test Game',
            startedAt: null,
            teams: [
              { name: 'Alpha_game1', challenge: 'web-easy', members: [] },
              { name: 'Beta_game1', challenge: 'crypto-medium', members: [] },
            ],
          },
        ],
      }).as('getGamesWithData')

      cy.visit('/dashboard')
      cy.wait('@getGamesWithData')

      cy.contains('Test Game')
      cy.contains('Pending')
      cy.contains('2 teams')
      cy.contains('Alpha')
      cy.contains('Beta')
    })

    it('shows Running badge for started games', () => {
      cy.intercept('GET', '/api/games', {
        statusCode: 200,
        body: [
          {
            id: 2,
            name: 'Running Match',
            startedAt: '2025-01-01T12:00:00',
            teams: [],
          },
        ],
      }).as('getGamesRunning')

      cy.visit('/dashboard')
      cy.wait('@getGamesRunning')

      cy.contains('Running Match')
      cy.contains('Running')
    })

    it('navigates to game detail when clicking a game card', () => {
      cy.intercept('GET', '/api/games', {
        statusCode: 200,
        body: [
          { id: 42, name: 'Click Me', startedAt: null, teams: [] },
        ],
      }).as('getGamesClick')

      cy.intercept('GET', '/api/games/42', {
        statusCode: 200,
        body: { id: 42, name: 'Click Me', startedAt: null, teams: [] },
      }).as('getGame42')

      cy.visit('/dashboard')
      cy.wait('@getGamesClick')

      cy.contains('Click Me').click()
      cy.url().should('include', '/game/42')
    })
  })

  // ── Create Game Modal ──────────────────────────────────────────────────

  describe('Create Game Modal', () => {
    beforeEach(() => {
      cy.intercept('GET', '/api/friend/friends*', {
        statusCode: 200,
        body: [
          { username: 'alice', points: 500 },
          { username: 'bob', points: 300 },
        ],
      }).as('getFriendsForModal')
    })

    it('opens the modal when clicking Create Game', () => {
      cy.contains('button', '+ Create Game').click()
      cy.contains('h2', 'Create New Game')
      cy.get('input[placeholder="Enter match name..."]').should('be.visible')
    })

    it('closes the modal with the close button', () => {
      cy.contains('button', '+ Create Game').click()
      cy.contains('h2', 'Create New Game').should('be.visible')
      cy.contains('button', 'Cancel').click()
      cy.contains('h2', 'Create New Game').should('not.exist')
    })

    it('shows empty teams state', () => {
      cy.contains('button', '+ Create Game').click()
      cy.contains('No teams added yet')
    })

    it('adds a team when clicking "+ Add Team"', () => {
      cy.contains('button', '+ Create Game').click()
      cy.contains('button', '+ Add Team').click()
      cy.get('input[placeholder="Team Name"]').should('be.visible')
    })

    it('removes a team', () => {
      cy.contains('button', '+ Create Game').click()
      cy.contains('button', '+ Add Team').click()
      cy.get('input[placeholder="Team Name"]').should('have.length', 1)

      // Click the trash icon button
      cy.get('input[placeholder="Team Name"]').parent().parent().parent().find('button').first().click()
      cy.contains('No teams added yet')
    })

    it('creates a game with valid data', () => {
      cy.intercept('POST', '/api/games/create', {
        statusCode: 201,
        body: { id: 99, name: 'New Game' },
      }).as('createGame')

      cy.intercept('GET', '/api/games', {
        statusCode: 200,
        body: [{ id: 99, name: 'New Game', startedAt: null, teams: [] }],
      }).as('getGamesAfterCreate')

      cy.contains('button', '+ Create Game').click()
      cy.get('input[placeholder="Enter match name..."]').type('New Game')
      cy.contains('button', '+ Add Team').click()
      cy.get('input[placeholder="Team Name"]').type('Alpha')
      // exact match: 'Create Game' is also a substring of the dashboard's
      // '+ Create Game' button, which sits behind the modal overlay
      cy.contains('button', /^\s*Create Game\s*$/).click()

      cy.wait('@createGame')
      cy.contains('h2', 'Create New Game').should('not.exist')
    })
  })

  // ── Add Friends ────────────────────────────────────────────────────────

  describe('Add Friends', () => {
    it('searches for a user and displays result', () => {
      cy.intercept('GET', '/api/user*', {
        statusCode: 200,
        body: { username: 'charlie', points: 200 },
      }).as('searchUser')

      cy.contains('h2', 'Add Friends')
        .parent()
        .find('input[placeholder="username"]')
        .type('charlie{enter}')

      cy.wait('@searchUser')
      cy.contains('charlie')
      cy.contains('button', 'add friend')
    })

    it('shows error when user is not found', () => {
      cy.intercept('GET', '/api/user*', {
        statusCode: 404,
        body: { message: 'User not found.' },
      }).as('searchUserNotFound')

      cy.contains('h2', 'Add Friends')
        .parent()
        .find('input[placeholder="username"]')
        .type('nobody{enter}')

      cy.wait('@searchUserNotFound')
      cy.contains('User not found.')
    })

    it('sends a friend request successfully', () => {
      cy.intercept('GET', '/api/user*', {
        statusCode: 200,
        body: { username: 'charlie', points: 200 },
      }).as('searchUser')

      cy.intercept('POST', '/api/friend', {
        statusCode: 200,
        body: {},
      }).as('sendFriendRequest')

      cy.contains('h2', 'Add Friends')
        .parent()
        .find('input[placeholder="username"]')
        .type('charlie{enter}')

      cy.wait('@searchUser')
      cy.contains('button', 'add friend').click()
      // only the other player is named; the backend knows who is asking
      cy.wait('@sendFriendRequest').its('request.body').should('deep.equal', { receiver: 'charlie' })
      cy.contains('Friend request sent successfully.')
    })

    it('shows error when friend request already sent', () => {
      cy.intercept('GET', '/api/user*', {
        statusCode: 200,
        body: { username: 'charlie', points: 200 },
      }).as('searchUser')

      cy.intercept('POST', '/api/friend', {
        statusCode: 400,
        body: { status: 400, message: 'Friend request already sent' },
      }).as('sendDuplicate')

      cy.contains('h2', 'Add Friends')
        .parent()
        .find('input[placeholder="username"]')
        .type('charlie{enter}')

      cy.wait('@searchUser')
      cy.contains('button', 'add friend').click()
      cy.wait('@sendDuplicate')
      cy.contains('Friend request already sent')
    })
  })

  // ── Friend Requests ────────────────────────────────────────────────────

  describe('Friend Requests', () => {
    it('shows incoming requests', () => {
      cy.intercept('GET', '/api/friend/requests*', {
        statusCode: 200,
        body: [
          { username: 'dave', points: 150 },
          { username: 'eve', points: 250 },
        ],
      }).as('getRequests')

      cy.visit('/dashboard')
      cy.wait('@getRequests')

      cy.contains('dave')
      cy.contains('eve')
    })

    it('accepts a friend request', () => {
      cy.intercept('GET', '/api/friend/requests*', {
        statusCode: 200,
        body: [{ username: 'dave', points: 150 }],
      }).as('getRequests')

      cy.intercept('POST', '/api/friend/acceptRequest', {
        statusCode: 200,
        body: {},
      }).as('acceptRequest')

      cy.visit('/dashboard')
      cy.wait('@getRequests')

      // Click the accept (checkmark) button
      cy.contains('dave').parent().find('.primary-button-sm').click()
      cy.wait('@acceptRequest').its('request.body').should('deep.equal', { requester: 'dave' })
    })

    it('refuses a friend request', () => {
      cy.intercept('GET', '/api/friend/requests*', {
        statusCode: 200,
        body: [{ username: 'dave', points: 150 }],
      }).as('getRequests')

      cy.intercept('POST', '/api/friend/refuseRequest', {
        statusCode: 200,
        body: {},
      }).as('refuseRequest')

      cy.visit('/dashboard')
      cy.wait('@getRequests')

      // Click the refuse (X) button
      cy.contains('dave').parent().find('.danger-button-sm').click()
      cy.wait('@refuseRequest').its('request.body').should('deep.equal', { requester: 'dave' })
    })
  })

  // ── Friends List ───────────────────────────────────────────────────────

  describe('Friends List', () => {
    it('shows the friends list', () => {
      cy.intercept('GET', '/api/friend/friends*', {
        statusCode: 200,
        body: [
          { username: 'alice', points: 500 },
          { username: 'bob', points: 300 },
        ],
      }).as('getFriendsList')

      cy.visit('/dashboard')
      cy.wait('@getFriendsList')

      cy.contains('h2', 'My Friends')
      cy.contains('alice')
      cy.contains('bob')
    })

    it('deletes a friend', () => {
      cy.intercept('GET', '/api/friend/friends*', {
        statusCode: 200,
        body: [{ username: 'alice', points: 500 }],
      }).as('getFriendsList')

      cy.intercept('DELETE', '/api/friend*', {
        statusCode: 200,
        body: {},
      }).as('deleteFriend')

      cy.visit('/dashboard')
      cy.wait('@getFriendsList')

      cy.contains('h2', 'My Friends')
        .parent()
        .parent()
        .contains('alice')
        .parent()
        .find('.danger-button-sm')
        .click()

      cy.wait('@deleteFriend').then(({ request }) => {
        expect(request.url).to.include('friend=alice')
        expect(request.url).not.to.include('username=')
      })
    })
  })
})
