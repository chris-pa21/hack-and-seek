import axios from '@/plugins/axios'

export default {
    createGame(payload) {
        // payload: { name: String, teams: [{ name, challenge, players: [username] }] }
        return axios.post('/api/games/create', payload)
    },

    getAllGames() {
        return axios.get('/api/games')
    },

    deleteGame(gameId) {
        return axios.delete(`/api/games/${gameId}`)
    },

    getChallenges() {
        return axios.get('/api/games/challenges')
    },
}
