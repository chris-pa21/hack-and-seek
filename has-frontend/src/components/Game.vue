<script setup>
const createGame = () => {
  // TODO: implement game creation logic
  console.log('Create Game clicked')
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
      @click="createGame"
      class="bg-[#00C853] hover:bg-[#009a40] cursor-pointer text-[#E0E6F0] px-4 py-1 rounded-lg text-sm font-semibold transition-colors"
    >
      Create Game
    </button>
  </div>
</template>

<style scoped></style>