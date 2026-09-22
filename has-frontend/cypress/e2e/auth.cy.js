describe('Authentication', () => {
  // ── Login ──────────────────────────────────────────────────────────────

  describe('Login Page', () => {
    beforeEach(() => {
      cy.mockUnauthenticated()
      cy.visit('/login')
    })

    it('displays the login form', () => {
      cy.contains('h1', 'Login')
      cy.get('input[placeholder="username"]').should('be.visible')
      cy.get('input[placeholder="password"]').should('be.visible')
      cy.contains('button', 'login')
    })

    it('shows error when fields are empty', () => {
      cy.contains('button', 'login').click()
      cy.contains('Please fill in all fields')
    })

    it('shows error on invalid credentials (401)', () => {
      cy.intercept('POST', '/api/auth/login', {
        statusCode: 401,
        body: {},
      }).as('loginFail')

      cy.get('input[placeholder="username"]').type('wrong')
      cy.get('input[placeholder="password"]').type('wrong')
      cy.contains('button', 'login').click()

      cy.wait('@loginFail')
      cy.contains('Wrong username and/or password')
    })

    it('redirects to dashboard on successful login', () => {
      cy.intercept('POST', '/api/auth/login', {
        statusCode: 200,
        body: { username: 'testuser', points: 100 },
      }).as('loginSuccess')

      cy.intercept('GET', '/api/auth/me', {
        statusCode: 200,
        body: { username: 'testuser', points: 100 },
      }).as('authMe')

      cy.stubDashboardAPIs()

      cy.get('input[placeholder="username"]').type('testuser')
      cy.get('input[placeholder="password"]').type('password123')
      cy.contains('button', 'login').click()

      cy.wait('@loginSuccess')
      cy.url().should('include', '/dashboard')
    })

    it('navigates to register via "create account" link', () => {
      cy.contains('create account').click()
      cy.url().should('include', '/register')
    })

    it('shows generic error on server error', () => {
      cy.intercept('POST', '/api/auth/login', {
        statusCode: 500,
        body: {},
      }).as('loginError')

      cy.get('input[placeholder="username"]').type('user')
      cy.get('input[placeholder="password"]').type('pass')
      cy.contains('button', 'login').click()

      cy.wait('@loginError')
      cy.contains('An error occurred')
    })
  })

  // ── Register ───────────────────────────────────────────────────────────

  describe('Register Page', () => {
    beforeEach(() => {
      cy.mockUnauthenticated()
      cy.visit('/register')
    })

    it('displays the registration form', () => {
      cy.contains('h1', 'Create Account')
      cy.get('input[placeholder="username"]').should('be.visible')
      cy.get('input[placeholder="password"]').should('be.visible')
      cy.get('input[placeholder="confirm password"]').should('be.visible')
      cy.contains('button', 'register')
    })

    it('shows error when fields are empty', () => {
      cy.contains('button', 'register').click()
      cy.contains('Please fill in all fields')
    })



    it('rejects a password shorter than 8 characters without calling the API', () => {
      cy.intercept('POST', '/api/auth/register', cy.spy().as('registerCall'))

      cy.get('input[placeholder="username"]').type('shorty')
      cy.get('input[placeholder="password"]').type('short')
      cy.get('input[placeholder="confirm password"]').type('short')
      cy.contains('button', 'register').click()

      cy.contains('Password must be at least 8 characters')
      cy.get('@registerCall').should('not.have.been.called')
    })

    it('shows password mismatch inline warning', () => {
      cy.get('input[placeholder="password"]').type('pass123')
      cy.get('input[placeholder="confirm password"]').type('different')
      cy.contains('Passwords do not match')
    })

    it('shows error on password mismatch submit', () => {
      cy.get('input[placeholder="username"]').type('user1')
      cy.get('input[placeholder="password"]').type('password123')
      cy.get('input[placeholder="confirm password"]').type('different123')
      cy.contains('button', 'register').click()
      cy.contains('passwords do not match')
    })

    it('shows success message on valid registration', () => {
      cy.intercept('POST', '/api/auth/register', {
        statusCode: 201,
        body: { username: 'newuser' },
      }).as('register')

      cy.get('input[placeholder="username"]').type('newuser')
      cy.get('input[placeholder="password"]').type('password123')
      cy.get('input[placeholder="confirm password"]').type('password123')
      cy.contains('button', 'register').click()

      cy.wait('@register')
      cy.contains('Registration successfull')
    })

    it('shows error when user already exists (409)', () => {
      cy.intercept('POST', '/api/auth/register', {
        statusCode: 409,
        body: {},
      }).as('registerConflict')

      cy.get('input[placeholder="username"]').type('existinguser')
      cy.get('input[placeholder="password"]').type('password123')
      cy.get('input[placeholder="confirm password"]').type('password123')
      cy.contains('button', 'register').click()

      cy.wait('@registerConflict')
      cy.contains('User already exists')
    })

    it('shows error on invalid input data (400)', () => {
      cy.intercept('POST', '/api/auth/register', {
        statusCode: 400,
        body: {},
      }).as('registerBadRequest')

      cy.get('input[placeholder="username"]').type('u')
      cy.get('input[placeholder="password"]').type('password123')
      cy.get('input[placeholder="confirm password"]').type('password123')
      cy.contains('button', 'register').click()

      cy.wait('@registerBadRequest')
      cy.contains('Invalid input data')
    })

    it('navigates to login via "already have an account?" link', () => {
      cy.contains('already have an account?').click()
      cy.url().should('include', '/login')
    })
  })

  // ── Route Guards ───────────────────────────────────────────────────────

  describe('Route Guards', () => {
    it('redirects unauthenticated user from /dashboard to /login', () => {
      cy.mockUnauthenticated()
      cy.visit('/dashboard')
      cy.url().should('include', '/login')
    })

    it('redirects unauthenticated user from /game/1 to /login', () => {
      cy.mockUnauthenticated()
      cy.visit('/game/1')
      cy.url().should('include', '/login')
    })

    it('redirects unknown routes to /login when not authenticated', () => {
      cy.mockUnauthenticated()
      cy.visit('/nonexistent')
      cy.url().should('include', '/login')
    })

    it('redirects unknown routes to /dashboard when authenticated', () => {
      cy.login('testuser')
      cy.stubDashboardAPIs()
      cy.visit('/nonexistent')
      cy.url().should('include', '/dashboard')
    })
  })

  // ── Logout ─────────────────────────────────────────────────────────────

  describe('Logout', () => {
    it('logs the user out and redirects to home', () => {
      cy.login('testuser')
      cy.stubDashboardAPIs()

      cy.intercept('POST', '/api/auth/logout', {
        statusCode: 200,
        body: {},
      }).as('logout')

      cy.visit('/dashboard')
      cy.wait('@authMe')

      // After logout, auth/me should return 401
      cy.intercept('GET', '/api/auth/me', {
        statusCode: 401,
        body: {},
      }).as('authMeAfterLogout')

      cy.contains('button', 'Logout').click()
      cy.wait('@logout')
      cy.url().should('eq', Cypress.config().baseUrl + '/')
    })
  })
})
