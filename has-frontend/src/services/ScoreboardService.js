import axios from '@/plugins/axios'

export default {
  // the logged-in user's own scoreboard; the backend takes the user from the session
  getScoreboard() {
    return axios.get('/api/scoreboard')
  },
}
