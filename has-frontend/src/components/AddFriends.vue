<script setup>
import FriendService from '@/services/FriendService'
import UserService from '@/services/UserService'
import { ref } from 'vue'

const userSearch = ref('')
const foundUser = ref(null)
const errorMessage = ref('')
const successMessage = ref('')
const isSending = ref(false)
const sendRequestButton = ref(true)

const searchUser = async () => {
  errorMessage.value = ''
  successMessage.value = ''
  foundUser.value = null
  sendRequestButton.value = true

  if (userSearch.value === '') return

  try {
    const response = await UserService.getUser(userSearch.value)
    foundUser.value = response.data
  } catch (error) {
    if (error.response && error.response.status === 404) {
      errorMessage.value = error.response.data?.message || 'User not found.'
    } else {
      // Generic fallback for other errors
      console.error(error)
      errorMessage.value = 'An unexpected error occurred while searching for the user.'
    }
  }
}

const sendFriendRequest = async () => {
  if (!foundUser.value) return
  errorMessage.value = ''
  successMessage.value = ''
  isSending.value = true

  try {
    await FriendService.addFriend(foundUser.value.username)
    successMessage.value = 'Friend request sent successfully.'
    sendRequestButton.value = false
  } catch (error) {
    if (error.response) {
      if (error.response.status === 400) {
        if (error.response.data === 'Friend request already sent') {
          errorMessage.value = error.response.data
          sendRequestButton.value = false
        }
      } else if (error.response.status === 404) {
        errorMessage.value = error.response.data?.message || 'User not found.'
      } else {
        errorMessage.value = 'An error occurred while sending the friend request.'
      }
    } else {
      errorMessage.value = 'Network error while sending the friend request.'
    }
    console.error(error)
  } finally {
    isSending.value = false
  }
}
</script>
<template>
  <div
    class="bg-[#1F2937] px-3 pt-3 pb-6 flex flex-col gap-y-4 justify-center items-center w-full max-w-[400px] lg:max-w-[600px] max-h-[250px] rounded-xl text-2xl"
  >
    <h2 class="font-semibold">Add Friends</h2>
    <input
      v-model="userSearch"
      @keyup.enter="searchUser"
      class="w-full sm:w-3/4 md:w-2/3 text-center bg-[#E0E6F0] text-[#0A0F1E] rounded-lg text-lg py-1"
      placeholder="username"
    />

    <p v-if="errorMessage" class="text-[#fca5a5] text-sm mt-2">
      {{ errorMessage }}
    </p>
    <p v-if="successMessage" class="text-[#6ee7b7] text-sm mt-2">
      {{ successMessage }}
    </p>

    <div
      v-if="foundUser"
      class="flex items-center justify-between gap-x-8 px-4 py-3 border-b border-gray-700"
    >
      <span class="font-bold text-[#E0E6F0] text-lg">{{ foundUser.username }}</span>
      <span class="hidden md:block text-[#9CA3AF] text-base">{{ foundUser.points }} pts</span>
      <button
        @click="sendFriendRequest"
        :disabled="isSending || !sendRequestButton"
        class="primary-button"
      >
        {{ isSending ? 'Sending...' : !sendRequestButton ? '&check;' : 'add friend' }}
      </button>
    </div>
  </div>
</template>

<style scoped></style>
