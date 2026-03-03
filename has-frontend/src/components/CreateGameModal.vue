<script setup>
import { ref, computed, watch } from 'vue'
import FriendService from '@/services/FriendService'
import GameService from '@/services/GameService'
import { userAuthStore } from '@/stores/auth'

const props = defineProps({
  show: Boolean
})
const emit = defineEmits(['close', 'create'])

const gameName = ref('')
const teams = ref([])
const availablePlayers = ref([])
const challenges = ref([])

const loadPlayers = async () => {
  try {
    const response = await FriendService.getAllFriends()
    const friends = response.data

    const authStore = userAuthStore()
    const user = authStore.currentUser
    const username = user?.username ?? user?.player ?? null

    // Add the creator if not already in the friends list
    if (username && !friends.some(f => f.player === username)) {
      friends.unshift({ player: username, points: null })
    }

    availablePlayers.value = friends
  } catch (error) {
    console.error('Failed to load players:', error)
  }
}

const loadChallenges = async () => {
  try {
    const response = await GameService.getChallenges()
    challenges.value = response.data
  } catch (error) {
    console.error('Failed to load challenges:', error)
  }
}

watch(() => props.show, (newVal) => {
  if (newVal) {
    loadPlayers()
    loadChallenges()
  }
})

const addTeam = () => {
  teams.value.push({
    name: '',
    selectedChallenge: challenges.value[0]?.name ?? '',
    players: [],
    dropdownOpen: false
  })
}

const removeTeam = (index) => {
  teams.value.splice(index, 1)
}

const addPlayerToTeam = (teamIndex, playerUsername) => {
  if (!teams.value[teamIndex].players.includes(playerUsername)) {
    teams.value[teamIndex].players.push(playerUsername)
  }
}

const removePlayerFromTeam = (teamIndex, playerUsername) => {
  teams.value[teamIndex].players = teams.value[teamIndex].players.filter(p => p !== playerUsername)
}

const handleCreate = () => {
  if (!gameName.value.trim()) {
    alert('Please provide a game name.')
    return
  }
  if (teams.value.length === 0) {
    alert('Please add at least one team.')
    return
  }
  if (teams.value.some(t => !t.name.trim())) {
    alert('All teams must have a name.')
    return
  }
  emit('create', {
    name: gameName.value.trim(),
    teams: teams.value.map(t => ({
      name: t.name.trim(),
      challenge: t.selectedChallenge,
      players: t.players
    }))
  })
  resetForm()
}

const resetForm = () => {
  gameName.value = ''
  teams.value = []
}

const close = () => {
  emit('close')
}

const difficultyColor = (name) => {
  const c = challenges.value.find(ch => ch.name === name)
  if (!c) return '#6B7280'
  if (c.difficulty <= 1) return '#00C853'
  if (c.difficulty === 2) return '#FCD34D'
  return '#DC0B00'
}

const difficultyLabel = (name) => {
  const c = challenges.value.find(ch => ch.name === name)
  if (!c) return 'Unknown'
  if (c.difficulty <= 1) return 'Easy'
  if (c.difficulty === 2) return 'Medium'
  return 'Hard'
}

// Collect all players already assigned to any team
const allAssignedPlayers = computed(() => {
  return teams.value.flatMap(t => t.players)
})

// Filter to players not assigned to any team
const getAvailableForTeam = () => {
  return availablePlayers.value.filter(p => !allAssignedPlayers.value.includes(p.player))
}

</script>

<template>
  <div v-if="show" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
    <div class="bg-[#1F2937] w-full max-w-2xl rounded-2xl shadow-2xl flex flex-col max-h-[90vh]">
      <!-- Header -->
      <div class="p-6 border-b border-gray-700 flex justify-between items-center">
        <h2 class="text-3xl font-bold text-[#E0E6F0]">Create New Game</h2>
        <button @click="close" class="text-gray-400 hover:text-white transition-colors text-2xl">&times;</button>
      </div>

      <!-- Content -->
      <div class="p-6 overflow-y-auto space-y-8 flex-grow">
        <!-- Game Name -->
        <div class="space-y-2">
          <label class="block text-sm font-medium text-gray-400 uppercase tracking-wider">Game Name</label>
          <input 
            v-model="gameName" 
            type="text" 
            placeholder="Enter match name..."
            class="w-full bg-[#0F172A] border border-gray-700 rounded-lg px-4 py-3 text-[#E0E6F0] focus:ring-2 focus:ring-[#00C853] focus:border-transparent outline-none transition-all"
          >
        </div>

        <!-- Teams Section -->
        <div class="space-y-4">
          <div class="flex justify-between items-center">
            <h3 class="text-xl font-semibold text-[#E0E6F0]">Teams</h3>
            <button @click="addTeam" class="primary-button text-sm py-1.5 px-4">+ Add Team</button>
          </div>

          <div v-if="teams.length === 0" class="text-center py-8 bg-[#0F172A] rounded-xl border border-dashed border-gray-700 text-gray-500">
            No teams added yet. Click "+ Add Team" to start.
          </div>

          <div v-for="(team, tIdx) in teams" :key="tIdx" class="bg-[#0F172A] rounded-xl p-5 border border-gray-700 space-y-4 relative group">
            <button @click="removeTeam(tIdx)" class="absolute top-4 right-4 text-gray-500 hover:text-red-500 transition-colors">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
                </svg>
            </button>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <!-- Team Name -->
              <div class="space-y-1">
                <label class="text-xs text-gray-500 uppercase">Team Name</label>
                <input 
                  v-model="team.name" 
                  type="text" 
                  placeholder="Team Name"
                  class="w-full bg-[#1F2937] border border-gray-600 rounded-lg px-3 py-2 text-sm text-[#E0E6F0] outline-none focus:border-[#00C853]"
                >
              </div>

              <!-- Challenge Select -->
              <div class="space-y-1 relative">
                <label class="text-xs text-gray-500 uppercase">Challenge</label>
                <button
                  type="button"
                  @click="team.dropdownOpen = !team.dropdownOpen"
                  class="w-full bg-[#1F2937] border border-gray-600 rounded-lg px-3 py-2 text-sm text-[#E0E6F0] outline-none focus:border-[#00C853] cursor-pointer flex items-center gap-2 text-left"
                >
                  <span class="w-2 h-2 rounded-full shrink-0" :style="{ backgroundColor: difficultyColor(team.selectedChallenge) }"></span>
                  <span class="flex-grow">{{ team.selectedChallenge || 'Select challenge...' }}</span>
                  <span class="text-gray-500 text-xs">{{ difficultyLabel(team.selectedChallenge) }}</span>
                </button>
                <div
                  v-if="team.dropdownOpen"
                  class="absolute z-10 mt-1 w-full bg-[#1F2937] border border-gray-600 rounded-lg overflow-hidden shadow-lg"
                >
                  <div
                    v-for="c in challenges"
                    :key="c.name"
                    @click="team.selectedChallenge = c.name; team.dropdownOpen = false"
                    class="flex items-center gap-2 px-3 py-2 text-sm cursor-pointer hover:bg-[#0F172A] transition-colors"
                    :class="team.selectedChallenge === c.name ? 'bg-[#0F172A] text-[#E0E6F0]' : 'text-gray-400'"
                  >
                    <span class="w-2 h-2 rounded-full shrink-0" :style="{ backgroundColor: difficultyColor(c.name) }"></span>
                    <span class="flex-grow">{{ c.name }}</span>
                    <span class="text-[10px] font-semibold px-1.5 py-0.5 rounded border" :style="{ color: difficultyColor(c.name), borderColor: difficultyColor(c.name) + '4D', backgroundColor: difficultyColor(c.name) + '1A' }">
                      {{ difficultyLabel(c.name) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Players -->
            <div class="space-y-2">
              <label class="text-xs text-gray-500 uppercase">Players</label>
              
              <!-- Added Players Chips -->
              <div class="flex flex-wrap gap-2 mb-3">
                <div v-for="p in team.players" :key="p" class="bg-[#00C853]/10 text-[#00C853] px-2 py-1 rounded-md text-xs flex items-center gap-1 border border-[#00C853]/30">
                  {{ p }}
                  <button @click="removePlayerFromTeam(tIdx, p)" class="hover:text-red-500 text-lg">&times;</button>
                </div>
                <div v-if="team.players.length === 0" class="text-xs text-gray-600 italic">No players added.</div>
              </div>

              <!-- Add Player Dropdown/List (Simplified) -->
              <div class="flex gap-2">
                 <select 
                    @change="(e) => { addPlayerToTeam(tIdx, e.target.value); e.target.value = '' }"
                    class="flex-grow bg-[#1F2937] border border-gray-600 rounded-lg px-3 py-1.5 text-xs text-gray-400 outline-none"
                 >
                    <option value="" disabled selected>Add player to team...</option>
                    <option v-for="player in getAvailableForTeam()" :key="player.player" :value="player.player">
                        {{ player.player }}{{ player.points != null ? ` (${player.points} pts)` : '' }}
                    </option>
                 </select>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="p-6 border-t border-gray-700 flex justify-end gap-4">
        <button @click="close" class="px-6 py-2 text-gray-400 hover:text-white transition-colors font-semibold">Cancel</button>
        <button @click="handleCreate" class="primary-button">Create Game</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Custom Scrollbar for the modal content */
::-webkit-scrollbar {
  width: 6px;
}
::-webkit-scrollbar-track {
  background: #1F2937;
}
::-webkit-scrollbar-thumb {
  background: #374151;
  border-radius: 10px;
}
::-webkit-scrollbar-thumb:hover {
  background: #4B5563;
}
</style>
