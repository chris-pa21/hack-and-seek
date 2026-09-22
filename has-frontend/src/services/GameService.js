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

    getChallenges() {
        return axios.get('/api/games/challenges')
    },

    submitFlag(gameId, flag) {
        return axios.post(`/api/games/${gameId}/submit-flag`, { flag })
    },

    getDefeatLog(gameId) {
        return axios.get(`/api/games/${gameId}/defeat-log`)
    },

    getCredentials(gameId) {
        return axios.get(`/api/games/${gameId}/credentials`)
    },

    stopGame(gameId) {
        return axios.post(`/api/games/${gameId}/stop`)
    },

    dismissGame(gameId) {
        return axios.post(`/api/games/${gameId}/dismiss`)
    },
}
