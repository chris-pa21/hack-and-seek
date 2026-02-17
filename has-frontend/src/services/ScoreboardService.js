import axios from '@/plugins/axios'
import { userAuthStore } from '@/stores/auth'

export default {
  getScoreboard() {
    const authStore = userAuthStore()
    const user = authStore.currentUser

    const username = user.username ?? user.player ?? null

    if (!username) {
      return Promise.reject(new Error('Could not determine current username'))
    }

    return axios.get('/api/scoreboard', {
      params: {
        username: username,
      },
    })
  },
}
