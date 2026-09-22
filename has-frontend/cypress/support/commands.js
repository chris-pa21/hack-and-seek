/**
 * Stub the /api/auth/me endpoint and set the auth cookie
 * so the app considers the user logged in.
 */
Cypress.Commands.add('login', (username = 'testuser') => {
  cy.intercept('GET', '/api/auth/me', {
    statusCode: 200,
    body: { username, points: 100 },
  }).as('authMe')

  cy.intercept('POST', '/api/auth/login', {
    statusCode: 200,
    body: { username, points: 100 },
  }).as('login')
})

/**
 * Stub /api/auth/me to return 401 (not authenticated).
 */
Cypress.Commands.add('mockUnauthenticated', () => {
  cy.intercept('GET', '/api/auth/me', {
    statusCode: 401,
    body: {},
  }).as('authMe')
})

/**
 * Stub common dashboard API calls so pages render without a backend.
 */
Cypress.Commands.add('stubDashboardAPIs', (username = 'testuser') => {
  cy.intercept('GET', '/api/scoreboard*', {
    statusCode: 200,
    body: [
      { username: 'alice', points: 500 },
      { username: 'bob', points: 300 },
      { username: 'testuser', points: 100 },
    ],
  }).as('getScoreboard')

  cy.intercept('GET', '/api/games', {
    statusCode: 200,
    body: [],
  }).as('getGames')

  cy.intercept('GET', '/api/games/challenges', {
    statusCode: 200,
    body: [
      { name: 'web-easy', difficulty: 1 },
      { name: 'crypto-medium', difficulty: 2 },
      { name: 'pwn-hard', difficulty: 3 },
    ],
  }).as('getChallenges')

  cy.intercept('GET', '/api/friend/requests*', {
    statusCode: 200,
    body: [],
  }).as('getFriendRequests')

  cy.intercept('GET', '/api/friend/friends*', {
    statusCode: 200,
    body: [],
  }).as('getFriends')

  cy.intercept('GET', '/api/user/me', {
    statusCode: 200,
    body: { username, points: 100, vpnIp: '10.8.19.2' },
  }).as('getMe')
})
