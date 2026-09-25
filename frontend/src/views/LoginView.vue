<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as z from 'zod'
import { useWindowSize } from '@vueuse/core'
import { useToast } from '@nuxt/ui/composables'
import type { AuthFormField, FormSubmitEvent } from '@nuxt/ui'
import InteractiveGridPattern from '@/components/inspira/InteractiveGridPattern.vue'
import { login } from '@/api/auth'

const CELL_SIZE = 40

const router = useRouter()
const toast = useToast()
const loading = ref(false)

// Size the grid to the viewport so it always covers the whole screen.
const { width, height } = useWindowSize()
const squares = computed<[number, number]>(() => [
  Math.ceil(width.value / CELL_SIZE) + 1,
  Math.ceil(height.value / CELL_SIZE) + 1,
])

const fields: AuthFormField[] = [
  {
    name: 'username',
    type: 'text',
    label: 'Username',
    placeholder: 'Enter your username',
    icon: 'i-lucide-user',
    autocomplete: 'username',
    size: 'lg',
    required: true,
  },
  {
    name: 'password',
    type: 'password',
    label: 'Password',
    placeholder: 'Enter your password',
    icon: 'i-lucide-lock',
    autocomplete: 'current-password',
    size: 'lg',
    required: true,
  },
]

const schema = z.object({
  username: z.string('Username is required').trim().min(1, 'Username is required'),
  password: z.string('Password is required').min(1, 'Password is required'),
})

type Schema = z.output<typeof schema>

async function onSubmit(event: FormSubmitEvent<Schema>) {
  loading.value = true
  try {
    await login(event.data)
    await router.push('/dashboard')
  } catch (error) {
    toast.add({
      title: error instanceof Error && error.message ? error.message : 'Login failed',
      color: 'error',
      icon: 'i-lucide-circle-x',
    })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="relative flex min-h-dvh items-center justify-center overflow-hidden bg-default p-4">
    <InteractiveGridPattern
      class="mask-[radial-gradient(ellipse_at_center,white_10%,transparent_70%)]"
      :width="CELL_SIZE"
      :height="CELL_SIZE"
      :squares="squares"
    />

    <!-- Soft green glow behind the card -->
    <div
      class="pointer-events-none absolute top-1/2 left-1/2 size-[32rem] -translate-1/2 rounded-full bg-primary/15 blur-3xl"
    />

    <div class="relative z-10 w-full max-w-sm">
      <!-- Highlight along the top edge of the card -->
      <div
        class="absolute inset-x-10 -top-px z-10 h-px bg-linear-to-r from-transparent via-primary to-transparent"
      />

      <UPageCard
        class="rounded-2xl bg-default/70 shadow-2xl shadow-primary/10 ring-primary/25 backdrop-blur-xl"
        :ui="{ container: 'p-6 sm:p-8' }"
      >
        <UAuthForm
          :fields="fields"
          :schema="schema"
          :loading="loading"
          :submit="{ label: 'Login', size: 'lg' }"
          @submit="onSubmit"
        >
        </UAuthForm>
      </UPageCard>
    </div>
  </main>
</template>
