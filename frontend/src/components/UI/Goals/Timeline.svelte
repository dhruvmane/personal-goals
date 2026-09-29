<script lang="ts">
    import { formatTargetDate, daysUntil } from '../../../lib/date'
    import { goalStore } from '../../../lib/goals.svelte'
    import type { Goal } from '../../../lib/types'
    import Trash from '../../UI/Trash.svelte'

    interface Props {
        goal: Goal;
    }

    let { goal }: Props = $props();

    let actionError = $state('');

    const days = $derived(goal.targetDate === null ? null : daysUntil(goal.targetDate));
    const overdue = $derived(days !== null && days < 0);

    async function onDelete() {
        actionError = '';
        try {
            await goalStore.remove(goal.id);
        } catch (error) {
            actionError = goalStore.describe(error);
        }
    }
</script>

<div class="flex items-center gap-3 p-3 snap-center shrink-0 w-full">
    <div
        class="flex flex-col items-center justify-center shrink-0 w-14 rounded-lg
               border border-white/10 bg-black/30 py-1"
    >
        <span class="text-lg leading-none tabular-nums">{goal.progress}</span>
        <span class="text-[10px] opacity-50 uppercase">pct</span>
    </div>

    <div class="min-w-0 flex-1">
        <h3 class="truncate">{goal.title}</h3>
        <p class="text-sm {overdue ? 'text-red-300' : 'opacity-50'}">
            {formatTargetDate(goal.targetDate)}
        </p>
    </div>

    <div class="hidden sm:block h-2 w-24 rounded-full bg-white/10 overflow-hidden shrink-0">
        <div
            class="h-full rounded-full bg-white/70"
            style:width="{goal.progress}%"
        ></div>
    </div>

    <button
        type="button"
        onclick={onDelete}
        disabled={goalStore.isBusy(goal.id)}
        aria-label="Delete {goal.title}"
        class="shrink-0 p-1.5 rounded-lg text-white/40 hover:text-white hover:bg-white/10
               transition-colors cursor-pointer disabled:opacity-40"
    >
        <Trash class="size-4" />
    </button>

    {#if actionError}
        <p role="alert" class="text-sm text-red-400 shrink-0">{actionError}</p>
    {/if}
</div>
