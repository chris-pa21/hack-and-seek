<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import CreateGameModal from '@/components/CreateGameModal.vue'
import GameService from '@/services/GameService'

const router = useRouter()
const isModalOpen = ref(false)
const games = ref([])
const isLoading = ref(false)
const challenges = ref([])

const difficultyColor = (name) => {
  const c = challenges.value.find(ch => ch.name === name)
  if (!c) return '#6B7280'
  if (c.difficulty <= 1) return '#00C853'
  if (c.difficulty === 2) return '#FCD34D'
  return '#DC0B00'
}

const fetchGames = async () => {
  isLoading.value = true
  try {
    const response = await GameService.getAllGames()
    games.value = response.data
  } catch (error) {
    console.error('Failed to fetch games:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(async () => {
  fetchGames()
  try {
    const res = await GameService.getChallenges()
    challenges.value = res.data
  } catch (e) {}
})

const handleCreateGame = async (gameData) => {
  try {
    await GameService.createGame(gameData)
    isModalOpen.value = false
    await fetchGames() // refresh list
  } catch (error) {
    console.error('Failed to create game:', error)
  }
}
</script>

<template>
  <div class="bg-[#1F2937] px-3 pt-2 pb-5 flex flex-col gap-y-4 items-center w-full max-w-[400px] lg:max-w-[600px] rounded-xl">

    <!-- Header -->
    <div class="relative flex items-center justify-center w-full pt-2">
      <h2 class="font-semibold text-2xl">Game</h2>
    </div>

    <!-- Create Game Button -->
    <button @click="isModalOpen = true" class="primary-button w-full">
      + Create Game
    </button>

    <!-- Game List -->
    <div class="w-full flex flex-col gap-y-3 mt-1">
      <!-- Loading -->
      <div v-if="isLoading" class="text-center text-gray-500 text-sm py-4">
        Loading games...
      </div>

      <!-- Empty state -->
      <div
        v-else-if="games.length === 0"
        class="text-center py-6 bg-[#0F172A] rounded-xl border border-dashed border-gray-700 text-gray-500 text-sm"
      >
        No games yet. Create one!
      </div>

      <!-- Game cards -->
      <div
        v-else
        v-for="game in games"
        :key="game.id"
        @click="router.push('/game/' + game.id)"
        class="bg-[#0F172A] rounded-xl px-4 py-3 flex flex-col gap-y-1 border border-gray-700 hover:border-[#00C853] transition-colors cursor-pointer"
      >
        <div class="flex justify-between items-center">
          <div class="flex items-center gap-2 min-w-0">
            <span class="font-bold text-[#E0E6F0] text-base truncate">{{ game.name }}</span>
            <span
              :class="[
                'text-[10px] font-semibold px-1.5 py-0.5 rounded border shrink-0',
                game.started_at
                  ? 'bg-[#00C853]/10 border-[#00C853]/30 text-[#00C853]'
                  : 'bg-[#FCD34D]/10 border-[#FCD34D]/30 text-[#FCD34D]'
              ]"
            >
              {{ game.started_at ? 'Running' : 'Pending' }}
            </span>
          </div>
          <span class="text-xs text-gray-500 ml-2 shrink-0">
            {{ game.team_list?.length ?? 0 }} team{{ (game.team_list?.length ?? 0) !== 1 ? 's' : '' }}
          </span>
        </div>
        <div class="flex flex-wrap gap-1 mt-1">
          <span
            v-for="team in game.team_list"
            :key="team.name"
            class="bg-[#1F2937] text-[#00C853] text-xs px-2 py-0.5 rounded-md border border-[#00C853]/30"
          >
            {{ team.name.split('_game')[0] }}
            <span class="text-gray-500">·</span>
            <span :style="{ color: difficultyColor(team.challenge) }">{{ team.challenge }}</span>
          </span>
        </div>
      </div>
    </div>

    <!-- Create Game Modal -->
    <CreateGameModal
      :show="isModalOpen"
      @close="isModalOpen = false"
      @create="handleCreateGame"
    />
  </div>
</template>

<style scoped></style>