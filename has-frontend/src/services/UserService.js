import axios from '@/plugins/axios'

export default {
  getUser(username) {
    return axios.get('/api/user', { params: { username } })
  },
}
