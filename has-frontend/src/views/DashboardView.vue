<script setup>
import AddFriends from '@/components/AddFriends.vue'
import FriendRequests from '@/components/FriendRequests.vue'
import Friends from '@/components/Friends.vue'
import Game from '@/components/Game.vue'
import Scoreboard from '@/components/Scoreboard.vue'
import UserNavbar from '@/components/UserNavbar.vue'
import VpnManagement from '@/components/VpnManagement.vue'
</script>

<template>
  <UserNavbar />
  <div class="flex flex-col lg:flex-row gap-6 justify-center mx-auto p-4">
    <!-- Left column: Scoreboard + future cards -->
    <div class="flex flex-col gap-6 items-center w-full lg:w-[600px]">
      <VpnManagement />
      <Scoreboard />
    </div>
    <!-- Right column: Game + Friend cards -->
    <div class="flex flex-col gap-6 items-center w-full lg:w-[600px]">
      <Game />
      <AddFriends />
      <FriendRequests />
      <Friends />
    </div>
  </div>
</template>

<style scoped></style>
