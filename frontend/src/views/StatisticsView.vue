<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useToast } from '@nuxt/ui/composables'
import AppHeader from '@/components/AppHeader.vue'
import { fetchEmployees, fetchWorkstations, fetchProjects } from '@/api/dashboard'

const toast = useToast()

const loading = ref(true)
const employees = ref<any[]>([])
const workstations = ref<any[]>([])
const projects = ref<any[]>([])

onMounted(async () => {
  try {
    const [emps, wss, projs] = await Promise.all([fetchEmployees(), fetchWorkstations(), fetchProjects()])
    employees.value = emps
    workstations.value = wss
    projects.value = projs
  } catch {
    toast.add({ title: 'Failed to load statistics', color: 'error', icon: 'i-lucide-circle-x' })
  } finally {
    loading.value = false
  }
})

// ── Helpers ─────────────────────────────────────────────────────────
const fmt = (n: number) => n.toLocaleString('en-US', { maximumFractionDigits: 0 })
// All money in the system is in EUR
const eurFormat = new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 })
const eur = (n: number) => eurFormat.format(n)
const fullName = (p: any) => `${p.firstName ?? ''} ${p.lastName ?? ''}`.trim() || p.username
const pct = (part: number, whole: number) => (whole > 0 ? Math.round((part / whole) * 100) : 0)

// Due dates are plain yyyy-MM-dd, so compare them as local calendar days
function daysUntil(date: string) {
  const [y, m, d] = date.split('-').map(Number)
  const due = new Date(y!, m! - 1, d!)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((due.getTime() - today.getTime()) / 86_400_000)
}

function dueLabel(days: number) {
  if (days < 0) return `${-days} day${days === -1 ? '' : 's'} overdue`
  if (days === 0) return 'Due today'
  return `${days} day${days === 1 ? '' : 's'} left`
}

// ── People ──────────────────────────────────────────────────────────
const workers = computed(() => employees.value.filter((e: any) => e.role === 'ROLE_USER'))
const managers = computed(() => employees.value.filter((e: any) => e.role === 'ROLE_ADMIN'))

const managersByDepartment = computed(() => {
  const groups = new Map<string, any[]>()
  for (const m of managers.value) {
    const dept = m.department || 'No department'
    groups.set(dept, [...(groups.get(dept) ?? []), m])
  }
  return [...groups.entries()].map(([department, people]) => ({ department, people })).sort((a, b) => b.people.length - a.people.length)
})

// userId -> workstation id
const workstationOf = computed(() => {
  const map = new Map<number, number>()
  for (const ws of workstations.value) for (const e of ws.employees) map.set(e.id, ws.id)
  return map
})

const unassignedWorkers = computed(() => workers.value.filter((w: any) => !workstationOf.value.has(w.id)))

// ── Projects ────────────────────────────────────────────────────────
type Status = 'completed' | 'inProgress' | 'overdue' | 'unstaffed'

// A project is done by the workers on its workstations; admins don't count
const projectStats = computed(() =>
  projects.value.map((p: any) => {
    const wsIds = new Set(p.workstations.map((w: any) => w.id))
    const assigned = workers.value.filter((w: any) => wsIds.has(workstationOf.value.get(w.id)))
    const assignedIds = new Set(assigned.map((w: any) => w.id))
    const done = p.completedBy.filter((u: any) => assignedIds.has(u.id)).length
    const days = daysUntil(p.dueDate)
    let status: Status
    if (assigned.length === 0) status = 'unstaffed'
    else if (done === assigned.length) status = 'completed'
    else if (days < 0) status = 'overdue'
    else status = 'inProgress'
    return { ...p, assigned: assigned.length, done, percent: pct(done, assigned.length), days, status }
  }),
)

const statusMeta: Record<Status, { label: string; icon: string; bar: string; text: string }> = {
  completed: { label: 'Completed', icon: 'i-lucide-circle-check', bar: 'bg-success', text: 'text-success' },
  inProgress: { label: 'In progress', icon: 'i-lucide-loader-circle', bar: 'bg-info', text: 'text-info' },
  overdue: { label: 'Overdue', icon: 'i-lucide-circle-alert', bar: 'bg-error', text: 'text-error' },
  unstaffed: { label: 'No workers', icon: 'i-lucide-circle-dashed', bar: 'bg-inverted/30', text: 'text-muted' },
}
const statusOrder: Status[] = ['completed', 'inProgress', 'overdue', 'unstaffed']

const statusCounts = computed(() =>
  statusOrder.map((key) => ({
    key,
    ...statusMeta[key],
    count: projectStats.value.filter((p) => p.status === key).length,
  })),
)

const totalAssignments = computed(() => projectStats.value.reduce((sum, p) => sum + p.assigned, 0))
const totalDone = computed(() => projectStats.value.reduce((sum, p) => sum + p.done, 0))
const completionRate = computed(() => pct(totalDone.value, totalAssignments.value))
const overdueCount = computed(() => statusCounts.value.find((s) => s.key === 'overdue')!.count)

// Unfinished first, then by how close the deadline is
const projectProgress = computed(() =>
  [...projectStats.value].sort((a, b) => Number(a.status === 'completed') - Number(b.status === 'completed') || a.days - b.days),
)

const upcoming = computed(() =>
  projectStats.value
    .filter((p) => p.status === 'inProgress' || p.status === 'overdue')
    .sort((a, b) => a.days - b.days)
    .slice(0, 5),
)

// ── Workstations ────────────────────────────────────────────────────
const workstationStats = computed(() =>
  workstations.value
    .map((ws: any) => {
      const wsProjects = projectStats.value.filter((p) => p.workstations.some((w: any) => w.id === ws.id))
      const memberIds = new Set(ws.employees.map((e: any) => e.id))
      const expected = wsProjects.length * ws.employees.length
      const done = wsProjects.reduce((sum, p) => sum + p.completedBy.filter((u: any) => memberIds.has(u.id)).length, 0)
      return {
        id: ws.id,
        title: ws.title,
        computers: ws.computerCount ?? 0,
        employees: ws.employees.length,
        projects: wsProjects.length,
        overdue: wsProjects.filter((p) => p.status === 'overdue').length,
        percent: pct(done, expected),
        hasWork: expected > 0,
      }
    })
    .sort((a, b) => b.projects - a.projects || b.employees - a.employees),
)

// ── Workers leaderboard ─────────────────────────────────────────────
const workerStats = computed(() =>
  workers.value
    .map((w: any) => {
      const wsId = workstationOf.value.get(w.id)
      const theirProjects = wsId == null ? [] : projectStats.value.filter((p) => p.workstations.some((x: any) => x.id === wsId))
      const done = theirProjects.filter((p) => p.completedBy.some((u: any) => u.id === w.id)).length
      return { id: w.id, name: fullName(w), jobTitle: w.jobTitle, assigned: theirProjects.length, done }
    })
    .filter((w) => w.assigned > 0)
    .sort((a, b) => b.done - a.done || pct(b.done, b.assigned) - pct(a.done, a.assigned)),
)
const maxDone = computed(() => Math.max(1, ...workerStats.value.map((w) => w.done)))

// ── Salaries ────────────────────────────────────────────────────────
const salaries = computed(() =>
  employees.value
    .map((e: any) => (e.salary == null || e.salary === '' ? null : Number(e.salary)))
    // 0 is the old form default, not a real salary (minimum wage applies)
    .filter((s): s is number => s !== null && !Number.isNaN(s) && s > 0)
    .sort((a, b) => a - b),
)
const payroll = computed(() => salaries.value.reduce((sum, s) => sum + s, 0))
const salaryStats = computed(() => {
  const s = salaries.value
  if (s.length === 0) return null
  const mid = Math.floor(s.length / 2)
  const median = s.length % 2 ? s[mid]! : (s[mid - 1]! + s[mid]!) / 2
  return [
    { label: 'Lowest', value: s[0]! },
    { label: 'Median', value: median },
    { label: 'Average', value: payroll.value / s.length },
    { label: 'Highest', value: s[s.length - 1]! },
  ]
})

// ── Project value ───────────────────────────────────────────────────
const valuedProjects = computed(() =>
  projectStats.value
    .filter((p) => p.projectValue != null)
    .map((p) => ({ ...p, value: Number(p.projectValue) }))
    .sort((a, b) => b.value - a.value),
)
const portfolioValue = computed(() => valuedProjects.value.reduce((sum, p) => sum + p.value, 0))
const maxProjectValue = computed(() => Math.max(1, ...valuedProjects.value.map((p) => p.value)))
const unvaluedCount = computed(() => projects.value.length - valuedProjects.value.length)
// Value of the work that's actually been delivered (fully completed projects)
const deliveredValue = computed(() => valuedProjects.value.filter((p) => p.status === 'completed').reduce((sum, p) => sum + p.value, 0))

// ── Computers ───────────────────────────────────────────────────────
const totalComputers = computed(() => workstations.value.reduce((sum, ws: any) => sum + (ws.computerCount ?? 0), 0))

// ── KPI row ─────────────────────────────────────────────────────────
const kpis = computed(() => [
  {
    label: 'People',
    icon: 'i-lucide-users',
    value: fmt(employees.value.length),
    note: `${workers.value.length} employee${workers.value.length === 1 ? '' : 's'} · ${managers.value.length} manager${managers.value.length === 1 ? '' : 's'}`,
  },
  {
    label: 'Workstations',
    icon: 'i-lucide-monitor',
    value: fmt(workstations.value.length),
    note: `${fmt(totalComputers.value)} computer${totalComputers.value === 1 ? '' : 's'} in total`,
  },
  {
    label: 'Projects',
    icon: 'i-lucide-folder-kanban',
    value: fmt(projects.value.length),
    note: overdueCount.value ? `${overdueCount.value} overdue` : 'Nothing overdue',
    alert: overdueCount.value > 0,
  },
  {
    label: 'Portfolio value',
    icon: 'i-lucide-euro',
    value: eur(portfolioValue.value),
    note: unvaluedCount.value
      ? `${unvaluedCount.value} project${unvaluedCount.value === 1 ? '' : 's'} without a value`
      : `across ${projects.value.length} project${projects.value.length === 1 ? '' : 's'}`,
  },
  {
    label: 'Monthly payroll',
    icon: 'i-lucide-banknote',
    value: eur(payroll.value),
    note: `${salaries.value.length} of ${employees.value.length} with a salary set`,
  },
])
</script>

<template>
  <main class="relative min-h-dvh overflow-x-clip bg-default">
    <!-- Soft green glow, same as on the login page -->
    <div
      aria-hidden="true"
      class="pointer-events-none absolute top-0 left-1/2 h-[28rem] w-[56rem] max-w-full -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary/15 blur-3xl"
    />

    <AppHeader title="Statistics" icon="i-lucide-chart-column" />

    <div v-if="loading" class="relative flex items-center justify-center py-32">
      <UIcon name="i-lucide-loader-2" class="size-8 animate-spin text-primary" />
    </div>

    <div v-else class="relative mx-auto max-w-7xl space-y-8 p-6">
      <div>
        <p class="text-sm font-medium text-primary">Administration insights</p>
        <h2 class="mt-1 text-3xl font-bold tracking-tight text-highlighted">Statistics</h2>
        <p class="mt-1 text-sm text-muted">Live figures from employees, workstations and projects.</p>
      </div>

      <!-- KPI row -->
      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        <UCard v-for="kpi in kpis" :key="kpi.label">
          <div class="flex items-center gap-2 text-sm text-muted">
            <UIcon :name="kpi.icon" class="size-4 text-primary" />
            {{ kpi.label }}
          </div>
          <p class="mt-2 text-3xl font-semibold tracking-tight text-highlighted tabular-nums">{{ kpi.value }}</p>
          <p class="mt-1 flex items-center gap-1 text-xs" :class="kpi.alert ? 'text-error' : 'text-muted'">
            <UIcon v-if="kpi.alert" name="i-lucide-circle-alert" class="size-3.5" />
            {{ kpi.note }}
          </p>
        </UCard>
      </div>

      <div class="grid gap-6 lg:grid-cols-3">
        <!-- Overall completion -->
        <UCard>
          <template #header>
            <h3 class="font-semibold text-highlighted">Overall completion</h3>
            <p class="text-sm text-muted">Workers who marked their projects done</p>
          </template>
          <p class="text-5xl font-bold tracking-tight text-highlighted tabular-nums">{{ completionRate }}%</p>
          <UTooltip :text="`${totalDone} of ${totalAssignments} project assignments completed`">
            <div
              class="mt-4 h-2 w-full overflow-hidden rounded bg-elevated"
              role="meter"
              :aria-valuenow="completionRate"
              aria-valuemin="0"
              aria-valuemax="100"
              aria-label="Overall completion"
            >
              <div class="h-full rounded bg-primary transition-[width] duration-500" :style="{ width: `${completionRate}%` }" />
            </div>
          </UTooltip>
          <p class="mt-2 text-sm text-muted tabular-nums">{{ totalDone }} of {{ totalAssignments }} assignments done</p>
        </UCard>

        <!-- Project status (part-to-whole) -->
        <UCard class="lg:col-span-2">
          <template #header>
            <h3 class="font-semibold text-highlighted">Project status</h3>
            <p class="text-sm text-muted">{{ projects.length }} project{{ projects.length === 1 ? '' : 's' }} by current state</p>
          </template>
          <template v-if="projects.length">
            <div class="flex h-3 w-full gap-0.5" role="img" :aria-label="statusCounts.map((s) => `${s.label}: ${s.count}`).join(', ')">
              <template v-for="s in statusCounts" :key="s.key">
                <UTooltip v-if="s.count" :text="`${s.label}: ${s.count} (${pct(s.count, projects.length)}%)`">
                  <div class="h-full rounded first:rounded-l last:rounded-r" :class="s.bar" :style="{ flexGrow: s.count, flexBasis: 0 }" />
                </UTooltip>
              </template>
            </div>
            <ul class="mt-5 grid gap-3 sm:grid-cols-4">
              <li v-for="s in statusCounts" :key="s.key" class="flex items-start gap-2">
                <UIcon :name="s.icon" class="mt-0.5 size-4 shrink-0" :class="s.text" />
                <div>
                  <p class="text-sm text-muted">{{ s.label }}</p>
                  <p class="text-xl font-semibold text-highlighted tabular-nums">
                    {{ s.count }}
                    <span class="text-xs font-normal text-muted">{{ pct(s.count, projects.length) }}%</span>
                  </p>
                </div>
              </li>
            </ul>
          </template>
          <p v-else class="py-6 text-center text-sm text-muted">No projects yet.</p>
        </UCard>
      </div>

      <div class="grid gap-6 lg:grid-cols-3">
        <!-- Progress per project -->
        <UCard class="lg:col-span-2">
          <template #header>
            <h3 class="font-semibold text-highlighted">Progress per project</h3>
            <p class="text-sm text-muted">Share of assigned workers who are done</p>
          </template>
          <ul v-if="projectProgress.length" class="space-y-4">
            <li v-for="p in projectProgress" :key="p.id">
              <div class="mb-1.5 flex items-center gap-2 text-sm">
                <UBadge color="primary" variant="outline" size="xs" class="shrink-0 font-mono">#{{ p.id }}</UBadge>
                <span class="truncate font-medium text-highlighted">{{ p.title }}</span>
                <span class="ml-auto flex shrink-0 items-center gap-1 text-xs" :class="statusMeta[p.status as Status].text">
                  <UIcon :name="statusMeta[p.status as Status].icon" class="size-3.5" />
                  {{ statusMeta[p.status as Status].label }}
                </span>
              </div>
              <UTooltip :text="p.assigned ? `${p.done} of ${p.assigned} workers done · due ${p.dueDate} (${dueLabel(p.days)})` : `No workers on its workstations · due ${p.dueDate}`">
                <div class="flex items-center gap-3">
                  <div class="h-2 flex-1 overflow-hidden rounded bg-elevated">
                    <div class="h-full rounded bg-primary transition-[width] duration-500" :style="{ width: `${p.percent}%` }" />
                  </div>
                  <span class="w-20 shrink-0 text-right text-xs text-muted tabular-nums">{{ p.done }}/{{ p.assigned }} · {{ p.percent }}%</span>
                </div>
              </UTooltip>
            </li>
          </ul>
          <p v-else class="py-6 text-center text-sm text-muted">No projects yet.</p>
        </UCard>

        <!-- Upcoming deadlines -->
        <UCard>
          <template #header>
            <h3 class="font-semibold text-highlighted">Upcoming deadlines</h3>
            <p class="text-sm text-muted">Unfinished projects, soonest first</p>
          </template>
          <ul v-if="upcoming.length" class="divide-y divide-default">
            <li v-for="p in upcoming" :key="p.id" class="flex items-center gap-3 py-2.5 first:pt-0 last:pb-0">
              <UIcon
                :name="p.days < 0 ? 'i-lucide-circle-alert' : p.days <= 7 ? 'i-lucide-clock-alert' : 'i-lucide-calendar'"
                class="size-4 shrink-0"
                :class="p.days < 0 ? 'text-error' : p.days <= 7 ? 'text-warning' : 'text-muted'"
              />
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-medium text-highlighted">
                  <span class="font-mono text-xs text-muted">#{{ p.id }}</span> {{ p.title }}
                </p>
                <p class="text-xs text-muted">{{ p.dueDate }}</p>
              </div>
              <span class="shrink-0 text-xs font-medium" :class="p.days < 0 ? 'text-error' : p.days <= 7 ? 'text-warning' : 'text-muted'">
                {{ dueLabel(p.days) }}
              </span>
            </li>
          </ul>
          <div v-else class="flex flex-col items-center gap-2 py-6 text-center">
            <UIcon name="i-lucide-party-popper" class="size-7 text-primary" />
            <p class="text-sm text-muted">No open deadlines.</p>
          </div>
        </UCard>
      </div>

      <div class="grid gap-6 lg:grid-cols-3">
        <!-- Project value (magnitude, one hue) -->
        <UCard class="lg:col-span-2">
          <template #header>
            <div class="flex flex-wrap items-end justify-between gap-2">
              <div>
                <h3 class="font-semibold text-highlighted">Project value</h3>
                <p class="text-sm text-muted">Value of each project, largest first</p>
              </div>
              <div class="text-right">
                <p class="text-xs text-muted">Delivered (completed projects)</p>
                <p class="font-semibold text-highlighted tabular-nums">
                  {{ eur(deliveredValue) }}
                  <span class="text-xs font-normal text-muted">of {{ eur(portfolioValue) }}</span>
                </p>
              </div>
            </div>
          </template>
          <ul v-if="valuedProjects.length" class="space-y-3">
            <li v-for="p in valuedProjects" :key="p.id" class="flex items-center gap-3">
              <div class="flex w-40 min-w-0 shrink-0 items-center gap-2">
                <UBadge color="primary" variant="outline" size="xs" class="shrink-0 font-mono">#{{ p.id }}</UBadge>
                <span class="truncate text-sm font-medium text-highlighted">{{ p.title }}</span>
              </div>
              <UTooltip :text="`${p.title}: ${eur(p.value)} · ${pct(p.value, portfolioValue)}% of the portfolio · ${p.employeeCount} employee${p.employeeCount === 1 ? '' : 's'}`" class="flex-1">
                <div class="h-2 w-full overflow-hidden rounded bg-elevated">
                  <div class="h-full rounded bg-primary transition-[width] duration-500" :style="{ width: `${(p.value / maxProjectValue) * 100}%` }" />
                </div>
              </UTooltip>
              <span class="w-24 shrink-0 text-right text-sm text-highlighted tabular-nums">{{ eur(p.value) }}</span>
            </li>
          </ul>
          <p v-else class="py-6 text-center text-sm text-muted">No project has a value yet. Add one when editing a project.</p>
          <p v-if="valuedProjects.length && unvaluedCount" class="mt-4 flex items-center gap-1.5 text-xs text-muted">
            <UIcon name="i-lucide-info" class="size-3.5" />
            {{ unvaluedCount }} project{{ unvaluedCount === 1 ? '' : 's' }} without a value {{ unvaluedCount === 1 ? 'is' : 'are' }} not shown.
          </p>
        </UCard>

        <!-- Managers by department -->
        <UCard>
          <template #header>
            <h3 class="font-semibold text-highlighted">Managers by department</h3>
            <p class="text-sm text-muted">{{ managers.length }} manager{{ managers.length === 1 ? '' : 's' }}</p>
          </template>
          <ul v-if="managersByDepartment.length" class="space-y-4">
            <li v-for="d in managersByDepartment" :key="d.department">
              <p class="flex items-center gap-2 text-sm font-medium text-highlighted">
                <UIcon name="i-lucide-building-2" class="size-4 text-primary" />
                {{ d.department }}
                <span class="ml-auto text-xs font-normal text-muted tabular-nums">{{ d.people.length }}</span>
              </p>
              <div class="mt-2 flex flex-wrap gap-1.5 pl-6">
                <UBadge v-for="m in d.people" :key="m.id" color="neutral" variant="subtle" size="sm">{{ fullName(m) }}</UBadge>
              </div>
            </li>
          </ul>
          <p v-else class="py-6 text-center text-sm text-muted">No managers yet.</p>
        </UCard>
      </div>

      <div class="grid gap-6 lg:grid-cols-2">
        <!-- Workstation workload (table) -->
        <UCard :ui="{ body: 'p-0 sm:p-0' }">
          <template #header>
            <h3 class="font-semibold text-highlighted">Workstation workload</h3>
            <p class="text-sm text-muted">People, projects and progress per workstation</p>
          </template>
          <div v-if="workstationStats.length" class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-default text-left text-xs text-muted">
                  <th scope="col" class="px-4 py-2.5 font-medium sm:px-6">Workstation</th>
                  <th scope="col" class="px-2 py-2.5 text-right font-medium">Computers</th>
                  <th scope="col" class="px-2 py-2.5 text-right font-medium">Employees</th>
                  <th scope="col" class="px-2 py-2.5 text-right font-medium">Projects</th>
                  <th scope="col" class="px-4 py-2.5 font-medium sm:px-6">Completion</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-default">
                <tr v-for="ws in workstationStats" :key="ws.id">
                  <td class="px-4 py-3 font-medium text-highlighted sm:px-6">
                    <span class="font-mono text-xs font-normal text-muted">№{{ ws.id }}</span> {{ ws.title }}
                  </td>
                  <td class="px-2 py-3 text-right tabular-nums">{{ ws.computers }}</td>
                  <td class="px-2 py-3 text-right tabular-nums">{{ ws.employees }}</td>
                  <td class="px-2 py-3 text-right tabular-nums">
                    {{ ws.projects }}
                    <span v-if="ws.overdue" class="ml-1 inline-flex items-center gap-0.5 text-xs text-error">
                      <UIcon name="i-lucide-circle-alert" class="size-3" />{{ ws.overdue }}
                    </span>
                  </td>
                  <td class="px-4 py-3 sm:px-6">
                    <div v-if="ws.hasWork" class="flex items-center gap-2">
                      <div class="h-1.5 w-full min-w-16 overflow-hidden rounded bg-elevated">
                        <div class="h-full rounded bg-primary" :style="{ width: `${ws.percent}%` }" />
                      </div>
                      <span class="w-9 text-right text-xs text-muted tabular-nums">{{ ws.percent }}%</span>
                    </div>
                    <span v-else class="text-xs text-dimmed">—</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="px-6 py-8 text-center text-sm text-muted">No workstations yet.</p>
        </UCard>

        <!-- Worker leaderboard -->
        <UCard>
          <template #header>
            <h3 class="font-semibold text-highlighted">Completed projects per worker</h3>
            <p class="text-sm text-muted">Workers with at least one assigned project</p>
          </template>
          <ul v-if="workerStats.length" class="space-y-3">
            <li v-for="(w, i) in workerStats" :key="w.id" class="flex items-center gap-3">
              <span class="w-5 shrink-0 text-right text-xs text-muted tabular-nums">{{ i + 1 }}</span>
              <div class="w-28 min-w-0 shrink-0">
                <p class="truncate text-sm font-medium text-highlighted">{{ w.name }}</p>
                <p class="truncate text-xs text-muted">{{ w.jobTitle || 'No title' }}</p>
              </div>
              <UTooltip :text="`${w.done} of ${w.assigned} projects completed (${pct(w.done, w.assigned)}%)`" class="flex-1">
                <div class="h-2 w-full overflow-hidden rounded bg-elevated">
                  <div
                    class="h-full rounded"
                    :class="i === 0 && w.done > 0 ? 'bg-primary' : 'bg-primary/50'"
                    :style="{ width: `${(w.done / maxDone) * 100}%` }"
                  />
                </div>
              </UTooltip>
              <span class="w-12 shrink-0 text-right text-xs text-muted tabular-nums">{{ w.done }}/{{ w.assigned }}</span>
            </li>
          </ul>
          <p v-else class="py-6 text-center text-sm text-muted">No workers have projects yet.</p>
        </UCard>
      </div>

      <div class="grid gap-6 lg:grid-cols-2">
        <!-- Salary overview -->
        <UCard>
          <template #header>
            <h3 class="font-semibold text-highlighted">Salaries</h3>
            <p class="text-sm text-muted">Monthly, in EUR</p>
          </template>
          <dl v-if="salaryStats" class="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <div v-for="s in salaryStats" :key="s.label">
              <dt class="text-xs text-muted">{{ s.label }}</dt>
              <dd class="mt-1 text-lg font-semibold text-highlighted tabular-nums">{{ eur(s.value) }}</dd>
            </div>
          </dl>
          <p v-else class="py-6 text-center text-sm text-muted">No salaries recorded yet.</p>
        </UCard>

        <!-- Unassigned workers -->
        <UCard>
          <template #header>
            <h3 class="font-semibold text-highlighted">Workers without a workstation</h3>
            <p class="text-sm text-muted">They can't receive any projects until assigned</p>
          </template>
          <div v-if="unassignedWorkers.length" class="flex flex-wrap gap-2">
            <UBadge v-for="w in unassignedWorkers" :key="w.id" color="warning" variant="subtle" icon="i-lucide-user-x">
              {{ fullName(w) }}
            </UBadge>
          </div>
          <div v-else class="flex items-center gap-2 text-sm text-muted">
            <UIcon name="i-lucide-circle-check" class="size-4 text-success" />
            Every worker is assigned to a workstation.
          </div>
        </UCard>
      </div>
    </div>
  </main>
</template>
