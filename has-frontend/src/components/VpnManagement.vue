<script setup>
import { ref, watch, onMounted } from 'vue'
import { userAuthStore } from '@/stores/auth'
import UserService from '@/services/UserService'
import VpnService from '@/services/VpnService'

const auth = userAuthStore()
const vpnData = ref(null)
const loading = ref(false)
const error = ref(null)

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

const downloadConfig = () => {
  const username = auth.user?.username || auth.user?.player
  if (!vpnData.value || !username) return
  if (!vpnData.value.vpnPrivateKey || !vpnData.value.vpnPublicKey) {
    error.value = 'VPN keys are not ready yet. Please refresh and try again.'
    return
  }

  VpnService.downloadConfig(
    username,
    vpnData.value.vpnIp,
    vpnData.value.vpnServerPublicKey,
    vpnData.value.vpnServerEndpoint,
    vpnData.value.vpnPrivateKey
  )
}

watch(() => auth.user, (newUser) => {
  if (newUser) fetchVpnData()
}, { immediate: true })

onMounted(() => {
  if (auth.user) fetchVpnData()
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
      <p class="text-[#94A3B8] text-sm">Secure access to game networks (10.200.x.x)</p>
    </div>

    <div v-if="loading" class="w-full flex justify-center py-4">
      <div class="animate-pulse text-blue-400 font-medium">Communicating with Guard...</div>
    </div>

    <div v-else-if="error" class="w-full p-4 bg-red-900/30 border border-red-500/50 rounded-lg text-red-300 text-sm">
      {{ error }}
    </div>

    <div v-else-if="vpnData && vpnData.vpnIp" class="w-full space-y-6">
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

      <!-- Download -->
      <button
        @click="downloadConfig"
        class="w-full bg-blue-600 hover:bg-blue-500 text-white font-bold py-4 rounded-xl transition-all shadow-lg shadow-blue-900/40 flex items-center justify-center gap-3 transform active:scale-[0.98]"
      >
        📥 Download {{ auth.user?.username || auth.user?.player }}-wg.conf
      </button>
    </div>

    <div v-else class="text-[#94A3B8] text-sm py-4">
      VPN configuration is being set up. Please try again shortly.
    </div>
  </div>
</template>
