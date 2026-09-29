<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { logout as doLogout, isAdmin, getUsername } from '@/api/auth'

defineProps<{
  title: string
  icon: string
}>()

const router = useRouter()
const admin = computed(() => isAdmin())
const username = computed(() => getUsername())

// Statistics is admin-only, so workers don't get a nav at all
const links = [
  { label: 'Dashboard', icon: 'i-lucide-layout-dashboard', to: '/dashboard' },
  { label: 'Statistics', icon: 'i-lucide-chart-column', to: '/statistics' },
]

function logout() {
  doLogout()
  router.push('/')
}
</script>

<template>
  <header class="sticky top-0 z-50 flex items-center justify-between gap-4 border-b border-default bg-default/80 px-6 py-3 backdrop-blur-lg">
    <!-- Highlight along the bottom edge, like the login card's top edge -->
    <div class="pointer-events-none absolute inset-x-0 -bottom-px h-px bg-linear-to-r from-transparent via-primary/60 to-transparent" />
    <div class="flex min-w-0 items-center gap-3">
      <UIcon :name="icon" class="size-6 shrink-0 text-primary" />
      <h1 class="truncate text-lg font-bold text-highlighted">{{ title }}</h1>
      <UBadge v-if="admin" color="primary" variant="subtle" size="sm">Admin</UBadge>
    </div>

    <nav v-if="admin" aria-label="Admin pages" class="hidden items-center gap-1 sm:flex">
      <UButton
        v-for="link in links"
        :key="link.to"
        :to="link.to"
        :icon="link.icon"
        size="sm"
        color="neutral"
        variant="ghost"
        active-color="primary"
        active-variant="soft"
      >
        {{ link.label }}
      </UButton>
    </nav>

    <div class="flex shrink-0 items-center gap-4">
      <!-- On small screens the nav collapses into a single icon link -->
      <UButton
        v-if="admin"
        class="sm:hidden"
        :to="$route.path === '/statistics' ? '/dashboard' : '/statistics'"
        :icon="$route.path === '/statistics' ? 'i-lucide-layout-dashboard' : 'i-lucide-chart-column'"
        :aria-label="$route.path === '/statistics' ? 'Dashboard' : 'Statistics'"
        size="sm"
        color="neutral"
        variant="ghost"
      />
      <span class="hidden text-sm text-muted sm:inline">{{ username }}</span>
      <UButton color="error" variant="soft" icon="i-lucide-log-out" size="sm" @click="logout">
        Logout
      </UButton>
    </div>
  </header>
</template>
