<script setup>
import ScoreboardService from '@/services/ScoreboardService'
import { ref, onMounted } from 'vue'

const friends = ref([])
const loadingRefresh = ref(false)

const reloadRequests = async () => {
  loadingRefresh.value = true
  try {
    const response = await ScoreboardService.getScoreboard()
    friends.value = response.data
  } catch (error) {
    console.error('Error loading scoreboard:', error)
  } finally {
    loadingRefresh.value = false
  }
}

onMounted(() => {
  reloadRequests()
})
</script>

<template>
  <div
    class="bg-[#1F2937] px-6 pt-6 pb-6 flex flex-col gap-y-4 items-center w-full max-w-[400px] lg:max-w-[600px] rounded-2xl"
  >
    <div class="relative flex flex-col items-center justify-center w-full gap-y-2">
      <h2 class="font-bold text-4xl text-[#E0E6F0]">Ranking</h2>
      <p class="text-[#94A3B8] text-base">Top 10 friends</p>
      <button
        @click="reloadRequests"
        :disabled="loadingRefresh"
        :class="[
          'absolute right-0 top-0 w-10 h-10 text-[#E0E6F0] rounded-lg text-2xl font-bold hover:bg-[#334155] transition-colors cursor-pointer',
          loadingRefresh ? 'animate-spin' : '',
        ]"
      >
        &#10227;
      </button>
    </div>

    <!-- Table Header -->
    <div class="w-full bg-[#0F172A] rounded-lg px-4 py-3 flex items-center">
      <span class="text-[#94A3B8] text-sm font-semibold w-12">#</span>
      <span class="text-[#94A3B8] text-sm font-semibold flex-1">user</span>
      <span class="text-[#94A3B8] text-sm font-semibold w-20 text-right">points</span>
    </div>

    <!-- Table Rows -->
    <div class="w-full overflow-y-auto flex flex-col gap-y-2 max-h-[500px]">
      <div
        v-for="(friend, index) in friends"
        :key="friend.player"
        :class="[
          'flex items-center px-4 py-3 rounded-lg w-full transition-colors',
          index % 2 === 0 ? 'bg-[#0F172A]' : 'bg-[#1E293B]',
          index === 0 ? 'bg-[#1E3A5F]' : '',
        ]"
      >
        <span
          :class="['font-bold text-lg w-12', index === 0 ? 'text-[#FCD34D]' : 'text-[#E0E6F0]']"
        >
          {{ index + 1 }}
        </span>
        <span
          :class="[
            'font-semibold text-base flex-1 truncate',
            index === 0 ? 'text-[#FCD34D]' : 'text-[#E0E6F0]',
          ]"
        >
          {{ friend.username }}
        </span>
        <span
          :class="[
            'text-base w-20 text-right font-medium',
            index === 0 ? 'text-[#FCD34D]' : 'text-[#E0E6F0]',
          ]"
        >
          {{ friend.points }}
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped></style>
