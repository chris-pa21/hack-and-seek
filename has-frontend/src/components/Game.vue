<script setup>
import { ref } from 'vue'
import CreateGameModal from '@/components/CreateGameModal.vue'

const isModalOpen = ref(false)

const openModal = () => {
  isModalOpen.value = true
}

const closeModal = () => {
  isModalOpen.value = false
}

const handleCreateGame = (gameData) => {
  // TODO: implement actual game creation API call
  console.log('Game Creation Data:', gameData)
  closeModal()
}
</script>

<template>
  <div
    class="bg-[#1F2937] px-3 pt-2 pb-5 flex flex-col gap-y-4 items-center w-full max-w-[400px] lg:max-w-[600px] rounded-xl text-2xl"
  >
    <div class="relative flex items-center justify-center w-full">
      <h2 class="font-semibold">Game</h2>
    </div>

    <button
      @click="openModal"
      class="primary-button"
    >
      Create Game
    </button>

    <!-- Create Game Modal -->
    <CreateGameModal 
      :show="isModalOpen" 
      @close="closeModal" 
      @create="handleCreateGame" 
    />
  </div>
</template>

<style scoped></style>