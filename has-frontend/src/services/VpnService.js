import nacl from 'tweetnacl'

const PRIVATE_KEY_STORAGE_KEY = 'wg_private_key'

function uint8ArrayToBase64(array) {
    let binary = ''
    const len = array.byteLength
    for (let i = 0; i < len; i++) {
        binary += String.fromCharCode(array[i])
    }
    return window.btoa(binary)
}

function base64ToUint8Array(base64) {
    const binaryString = window.atob(base64)
    const len = binaryString.length
    const bytes = new Uint8Array(len)
    for (let i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i)
    }
    return bytes
}

export default {
    generateKeyPair() {
        const keyPair = nacl.box.keyPair()
        const privateKey = uint8ArrayToBase64(keyPair.secretKey)
        const publicKey = uint8ArrayToBase64(keyPair.publicKey)
        return { privateKey, publicKey }
    },

    getStoredPrivateKey(username) {
        const stored = localStorage.getItem(`${PRIVATE_KEY_STORAGE_KEY}_${username}`)
        return stored
    },

    storePrivateKey(username, privateKey) {
        localStorage.setItem(`${PRIVATE_KEY_STORAGE_KEY}_${username}`, privateKey)
    },

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
