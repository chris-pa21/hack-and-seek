import axios from '@/plugins/axios'

export default {
    /**
     * Downloads the WireGuard config for the logged-in player. The file is
     * built server-side from the session, so no key material is ever handled
     * by the browser.
     */
    async downloadConfig(username) {
        const response = await axios.get('/api/user/me/vpn-config', { responseType: 'blob' })

        const url = URL.createObjectURL(response.data)
        const link = document.createElement('a')
        link.href = url
        link.download = `${username}-wg.conf`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)
    },
}
