<script setup>
import { userAuthStore } from '@/stores/auth'
import { ref } from 'vue'
</script>

<template>
  <div v-if="userAuthStore.checkAuth" class="bg-[#1F2937]"></div>
  <div v-else="$router.push('/login')"></div>
</template>

<style scoped></style>
