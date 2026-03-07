<script setup>
import { ref, onMounted, watch } from 'vue'
import { userAuthStore } from '@/stores/auth'
import UserService from '@/services/UserService'
import VpnService from '@/services/VpnService'

const auth = userAuthStore()
const vpnData = ref(null)
const loading = ref(false)
const error = ref(null)
const success = ref(false)

const fetchVpnData = async () => {
  const username = auth.user?.username || auth.user?.player
  if (!username) return
  loading.value = true
  try {
    const response = await UserService.getUser(username)
    vpnData.value = response.data
  } catch (err) {
    error.value = 'Failed to fetch VPN data'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const setupVpn = async () => {
  const username = auth.user?.username || auth.user?.player
  if (!username) return
  loading.value = true
  error.value = null
  success.value = false
  try {
    const { privateKey, publicKey } = VpnService.generateKeyPair()
    VpnService.storePrivateKey(username, privateKey)
    await UserService.updateVpnPublicKey(username, publicKey)
    await fetchVpnData()
    success.value = true
    setTimeout(() => { success.value = false }, 5000)
  } catch (err) {
    error.value = 'Failed to setup VPN'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const downloadConfig = () => {
  const username = auth.user?.username || auth.user?.player
  if (!vpnData.value || !username) return
  const privateKey = VpnService.getStoredPrivateKey(username)
  if (!privateKey) {
    error.value = 'Private key not found. Please re-initialize VPN.'
    return
  }
  
  VpnService.downloadConfig(
    username,
    vpnData.value.vpnIp,
    vpnData.value.vpnServerPublicKey,
    vpnData.value.vpnServerEndpoint,
    privateKey
  )
}

watch(() => auth.user, (newUser) => {
  if (newUser) fetchVpnData()
}, { immediate: true })

onMounted(() => {
  if (auth.user) {
    fetchVpnData()
  }
})
</script>

<template>
  <div
    class="bg-[#1F2937] px-8 py-6 flex flex-col gap-y-6 items-center w-full max-w-[400px] lg:max-w-[600px] rounded-2xl shadow-xl border border-gray-700/50"
  >
    <div class="flex flex-col items-center justify-center w-full gap-y-2 text-center">
      <h2 class="font-bold text-3xl text-[#E0E6F0] flex items-center gap-3">
        <span class="text-blue-500">🛡️</span> VPN Settings
      </h2>
      <p class="text-[#94A3B8] text-sm">Secure access to game networks (172.x.x.x)</p>
    </div>

    <div v-if="loading" class="w-full flex justify-center py-4">
      <div class="animate-pulse text-blue-400 font-medium">Communicating with Guard...</div>
    </div>
    
    <div v-else-if="error" class="w-full p-4 bg-red-900/30 border border-red-500/50 rounded-lg text-red-300 text-sm">
      {{ error }}
    </div>

    <div v-if="success" class="w-full p-4 bg-green-900/30 border border-green-500/50 rounded-lg text-green-300 text-sm flex items-center gap-2">
      <span>✅</span> VPN Initialized Successfully!
    </div>

    <div v-else class="w-full space-y-6">
      <div v-if="vpnData && vpnData.vpnIp" class="space-y-6">
        <!-- Status Card -->
        <div class="bg-[#0F172A] rounded-xl p-5 border border-gray-800">
          <div class="flex items-center justify-between mb-3">
            <span class="text-[#94A3B8] text-xs font-semibold uppercase tracking-wider">Status</span>
            <span class="flex items-center gap-1.5 text-xs text-green-400 font-bold">
              <span class="w-2 h-2 bg-green-500 rounded-full animate-pulse"></span>
              ACTIVE
            </span>
          </div>
          <div class="flex flex-col gap-1">
            <span class="text-[#94A3B8] text-xs font-semibold">Assigned WireGuard IP</span>
            <div class="text-2xl font-mono text-blue-400 font-bold">{{ vpnData.vpnIp }}</div>
          </div>
        </div>

        <!-- Download Section -->
        <div class="flex flex-col gap-4">
          <button
            @click="downloadConfig"
            class="w-full bg-blue-600 hover:bg-blue-500 text-white font-bold py-4 rounded-xl transition-all shadow-lg shadow-blue-900/40 flex items-center justify-center gap-3 transform active:scale-[0.98]"
          >
             📥 Download {{ auth.user?.username || auth.user?.player }}-wg.conf
          </button>
          
          <div class="flex items-start gap-2 px-1">
            <span class="text-yellow-500 text-lg mt-0.5">⚠️</span>
            <p class="text-[#94A3B8] text-[11px] leading-relaxed">
              The private key is stored securely in your browser's local storage. 
              If you switch devices or clear your cache, you will need to re-initialize your keys.
            </p>
          </div>
        </div>

        <button 
          @click="setupVpn"
          class="w-full text-gray-500 hover:text-red-400 text-xs transition-colors underline"
        >
          Regenerate Keys (Resets Connection)
        </button>
      </div>

      <!-- Not initialized state -->
      <div v-else class="flex flex-col items-center gap-6 py-4">
        <div class="w-20 h-20 bg-[#0F172A] rounded-full flex items-center justify-center border-2 border-dashed border-gray-700">
           <span class="text-3xl opacity-50">🔒</span>
        </div>
        <div class="text-center">
          <p class="text-[#E0E6F0] font-medium mb-1">VPN is not yet configured</p>
          <p class="text-[#94A3B8] text-xs">Initialize your access parameters to connect.</p>
        </div>
        <button
          @click="setupVpn"
          class="w-full bg-green-600 hover:bg-green-500 text-white font-bold py-4 rounded-xl transition-all shadow-lg shadow-green-900/40"
        >
          🚀 Initialize My VPN
        </button>
      </div>
    </div>
  </div>
</template>
