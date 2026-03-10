describe('Home Page', () => {
  beforeEach(() => {
    cy.mockUnauthenticated()
    cy.visit('/')
  })

  it('displays the hero section', () => {
    cy.contains('h1', 'Capture The Flag')
    cy.contains('h2', 'On another level')
    cy.get('img[src*="logo"]').should('be.visible')
  })

  it('displays the navbar with login and register buttons', () => {
    cy.get('nav').within(() => {
      cy.contains('Hack&Seek')
      cy.contains('button', 'Login')
      cy.contains('button', 'Register')
    })
  })

  it('navigates to login when Login button is clicked', () => {
    cy.get('nav').contains('button', 'Login').click()
    cy.url().should('include', '/login')
  })

  it('navigates to register when Register button is clicked', () => {
    cy.get('nav').contains('button', 'Register').click()
    cy.url().should('include', '/register')
  })

  it('displays the CTF info section', () => {
    cy.get('#what').within(() => {
      cy.contains('What is a CTF?')
      cy.contains('cybersecurity competition')
    })
  })

  it('displays the Hack&Seek info section', () => {
    cy.get('#how').within(() => {
      cy.contains('How does Hack&Seek work?')
      cy.contains('Attack-Defense CTF platform')
    })
  })

  it('navigates to login via GET STARTED card', () => {
    cy.contains('GET STARTED').click()
    cy.url().should('include', '/login')
  })

  it('redirects authenticated user from / to /dashboard', () => {
    cy.login('testuser')
    cy.stubDashboardAPIs()
    cy.visit('/')
    cy.url().should('include', '/dashboard')
  })
})
