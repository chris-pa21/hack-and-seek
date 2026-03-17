export default {
    downloadConfig(username, vpnIp, serverPublicKey, serverEndpoint, privateKey) {
        const config = `[Interface]
PrivateKey = ${privateKey}
Address = ${vpnIp}/32

[Peer]
PublicKey = ${serverPublicKey}
Endpoint = ${serverEndpoint}
AllowedIPs = 10.8.19.0/24, 10.200.0.0/16
PersistentKeepalive = 25
`
        const blob = new Blob([config], { type: 'text/plain' })
        const url = URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `${username}-wg.conf`
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        URL.revokeObjectURL(url)
    },
}
