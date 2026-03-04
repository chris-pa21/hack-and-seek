import axios from '@/plugins/axios'

export default {
    createGame(payload) {
        // payload: { name: String, teams: [{ name, challenge, players: [username] }] }
        return axios.post('/api/games/create', payload)
    },

    getAllGames() {
        return axios.get('/api/games')
    },

    getGame(gameId) {
        return axios.get(`/api/games/${gameId}`)
    },

    startGame(gameId) {
        return axios.post(`/api/games/${gameId}/start`)
    },

    deleteGame(gameId) {
        return axios.delete(`/api/games/${gameId}`)
    },

    getChallenges() {
        return axios.get('/api/games/challenges')
    },

    getContainers(gameId) {
        return axios.get(`/api/orchestration/session/game/${gameId}`)
    },
}
