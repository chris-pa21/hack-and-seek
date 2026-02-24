<script setup>
import AddFriends from '@/components/AddFriends.vue'
import FriendRequests from '@/components/FriendRequests.vue'
import Friends from '@/components/Friends.vue'
import Scoreboard from '@/components/Scoreboard.vue'
</script>

<template>
  <div class="flex flex-col lg:flex-row gap-6 justify-center mx-auto p-4">
    <!-- Left column: Scoreboard + future cards -->
    <div class="flex flex-col gap-6 items-center w-full lg:w-[600px]">
      <Scoreboard />
    </div>
    <!-- Right column: Friend cards -->
    <div class="flex flex-col gap-6 items-center w-full lg:w-[600px]">
      <AddFriends />
      <FriendRequests />
      <Friends />
    </div>
  </div>
</template>

<style scoped></style>
