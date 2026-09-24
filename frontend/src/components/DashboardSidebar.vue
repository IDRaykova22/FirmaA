<script setup lang="ts">
defineEmits<{
  addWorker: []
  assignWorker: []
  createWorkstation: []
  createProject: []
}>()

const actions = [
  { label: 'Add a worker', icon: 'i-lucide-user-plus', event: 'addWorker' },
  { label: 'Assign worker to project', icon: 'i-lucide-user-round-check', event: 'assignWorker' },
  { label: 'Create a workstation', icon: 'i-lucide-monitor-plus', event: 'createWorkstation' },
  { label: 'Create a project', icon: 'i-lucide-folder-plus', event: 'createProject' },
] as const
</script>

<template>
  <UDashboardSidebar collapsible resizable>
    <template #header>
      <div class="flex items-center gap-2 px-2 font-semibold">
        <UIcon name="i-lucide-layout-dashboard" class="size-5 text-primary" />
        Administration
      </div>
    </template>

    <template #default>
      <nav aria-label="Administration actions" class="flex flex-col gap-1 p-2">
        <UButton
            v-for="action in actions"
            :key="action.event"
            :icon="action.icon"
            :label="action.label"
            color="neutral"
            variant="ghost"
            block
            class="justify-start"
            @click="$emit(action.event)"
        />
      </nav>
    </template>
  </UDashboardSidebar>
</template>