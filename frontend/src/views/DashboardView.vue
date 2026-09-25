<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function logout() {
  localStorage.removeItem('jwt')
  router.push('/')
}
</script>

<template>
  <main class="flex min-h-dvh flex-col items-center justify-center gap-6 bg-default p-8">
    <h1 class="text-3xl font-bold text-highlighted">Welcome to the Dashboard</h1>
    <p class="text-muted">You are logged in.</p>
    <UButton color="error" variant="soft" icon="i-lucide-log-out" @click="logout">
      Logout
    </UButton>
  </main>
</template>
