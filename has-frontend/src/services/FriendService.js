import axios from '@/plugins/axios'

// The backend acts as the logged-in user from the session, so none of these
// calls say who "I" am - only who the other player is.
export default {
  addFriend(receiver) {
    return axios.post('/api/friend', { receiver })
  },

  getFriendRequests() {
    return axios.get('/api/friend/requests')
  },

  acceptFriendRequest(requester) {
    return axios.post('/api/friend/acceptRequest', { requester })
  },

  refuseFriendRequest(requester) {
    return axios.post('/api/friend/refuseRequest', { requester })
  },

  getAllFriends() {
    return axios.get('/api/friend/friends')
  },

  deleteFriend(friend) {
    return axios.delete('/api/friend', { params: { friend } })
  },
}
