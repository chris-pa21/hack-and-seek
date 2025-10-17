// stores/auth.js
import { defineStore } from 'pinia'
import axios from 'axios'

export const userAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    isAuthenticated: false,
    loading: false
  }),

  getters: {
    currentUser: (state) => state.user,
    isLoggedIn: (state) => state.isAuthenticated
  },

  actions: {
    async login(credentials) {
      this.loading = true
      try {
        const response = await axios.post('/api/auth/login', credentials, {
          withCredentials: true 
        })
        this.user = response.data
        this.isAuthenticated = true
        return response.data
      } catch (error) {
        this.isAuthenticated = false
        throw error
      } finally {
        this.loading = false
      }
    },

    async logout() {
      try {
        await axios.post('/api/auth/logout', {}, {
          withCredentials: true
        })
        this.user = null
        this.isAuthenticated = false
      } catch (error) {
        console.error('Logout failed:', error)
      }
    },

    async register(userData) {
      this.loading = true
      try {
        const response = await axios.post('/api/auth/register', userData, {
          withCredentials: true
        })
        this.user = response.data
        this.isAuthenticated = true
        return response.data
      } catch (error) {
        throw error
      } finally {
        this.loading = false
      }
    },

    async checkAuth() {
      try {
        const response = await axios.get('/api/auth/me', {
          withCredentials: true
        })
        this.user = response.data
        this.isAuthenticated = true
      } catch (error) {
        this.user = null
        this.isAuthenticated = false
      }
    },

    async fetchUserProfile() {
      try {
        const response = await axios.get('/api/user/profile', {
          withCredentials: true
        })
        this.user = response.data
      } catch (error) {
        console.error('Failed to fetch profile:', error)
      }
    }
  }
})