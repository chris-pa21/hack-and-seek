import axios from '@/plugins/axios'

export default {
  getUser(username) {
    return axios.get('/api/user', { params: { username } })
  },
  updateVpnPublicKey(username, publicKey) {
    return axios.post('/api/user/vpn-public-key', null, { params: { username, publicKey } })
  },
}
