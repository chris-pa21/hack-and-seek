import axios from '@/plugins/axios'
import { userAuthStore } from '@/stores/auth'

export default {
  addFriend(receiver) {
    const authStore = userAuthStore()
    const user = authStore.currentUser

    const username = user.username ?? user.player ?? null
    if (!username) {
      return Promise.reject(new Error('Could not determine current username'))
    }

    return axios.post('/api/friend', {
      requester: username,
      receiver,
    })
  },

  getFriendRequests() {
    const authStore = userAuthStore()
    const user = authStore.currentUser

    const username = user.username ?? user.player ?? null
    if (!username) {
      return Promise.reject(new Error('Could not determine current username'))
    }

    return axios.get('/api/friend/requests', {
      params: {
        username: username,
      },
    })
  },
}
