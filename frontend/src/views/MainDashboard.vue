<script setup lang="ts">
import { computed } from 'vue'
import DashboardSidebar from '@/components/DashboardSidebar.vue'

interface Project {
  id: string | number
  name: string
  description?: string
}

interface Workstation {
  id: string | number
  name: string
  projects: Project[]
}

const props = withDefaults(defineProps<{
  user?: { name: string; role: 'admin' | 'worker' }
  workstations?: Workstation[]
}>(), {
  workstations: () => [],
})

const emit = defineEmits<{
  addWorker: []
  assignWorker: []
  createWorkstation: []
  createProject: []
}>()

const isAdmin = computed(() => props.user?.role === 'admin')
const projectCount = computed(() =>
    props.workstations.reduce((total, workstation) => total + workstation.projects.length, 0),
)
</script>

<template>
  <UDashboardGroup>
    <DashboardSidebar
        v-if="isAdmin"
        @add-worker="emit('addWorker')"
        @assign-worker="emit('assignWorker')"
        @create-workstation="emit('createWorkstation')"
        @create-project="emit('createProject')"
    />

    <UDashboardPanel id="main-dashboard">
      <template #header>
        <UDashboardNavbar title="Dashboard" :toggle="isAdmin" />
      </template>

      <template #body>
        <main class="mx-auto w-full max-w-6xl space-y-8 p-4 sm:p-6">
          <div>
            <p class="text-sm font-medium text-primary">
              {{ isAdmin ? 'Administration overview' : 'Your workspace' }}
            </p>
            <h1 class="mt-1 text-3xl font-bold tracking-tight">
              {{ user?.name ? `Welcome, ${user.name}` : 'Welcome' }}
            </h1>
            <p class="mt-2 text-muted">
              Workstations you belong to and the projects assigned to each one.
            </p>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <UPageCard title="Workstations" icon="i-lucide-monitor">
              <p class="text-3xl font-semibold">{{ workstations.length }}</p>
            </UPageCard>
            <UPageCard title="Projects" icon="i-lucide-folder-kanban">
              <p class="text-3xl font-semibold">{{ projectCount }}</p>
            </UPageCard>
          </div>

          <section aria-labelledby="workstations-heading" class="space-y-4">
            <h2 id="workstations-heading" class="text-xl font-semibold">Your workstations</h2>

            <UPageCard v-if="workstations.length === 0">
              <div class="flex flex-col items-center gap-2 py-8 text-center">
                <UIcon name="i-lucide-monitor-x" class="size-8 text-muted" />
                <p class="font-medium">No workstations assigned</p>
                <p class="text-sm text-muted">
                  Your workstations and projects will appear here when assigned.
                </p>
              </div>
            </UPageCard>

            <UPageCard
                v-for="workstation in workstations"
                :key="workstation.id"
                :title="workstation.name"
                icon="i-lucide-monitor"
            >
              <p v-if="workstation.projects.length === 0" class="text-sm text-muted">
                No projects assigned to this workstation.
              </p>
              <ul v-else class="grid gap-3 sm:grid-cols-2">
                <li v-for="project in workstation.projects" :key="project.id">
                  <UCard>
                    <div class="flex items-start gap-3">
                      <UIcon name="i-lucide-folder-kanban" class="mt-1 size-5 text-primary" />
                      <div>
                        <h3 class="font-medium">{{ project.name }}</h3>
                        <p v-if="project.description" class="mt-1 text-sm text-muted">
                          {{ project.description }}
                        </p>
                      </div>
                    </div>
                  </UCard>
                </li>
              </ul>
            </UPageCard>
          </section>
        </main>
      </template>
    </UDashboardPanel>
  </UDashboardGroup>
</template>