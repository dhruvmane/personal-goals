<script lang="ts">
    import { onDestroy } from 'svelte'

    import { formatDeadlineHint, formatTargetDate, daysUntil } from '../../../lib/date'
    import { GOAL_STATUS_LABELS, goalStore } from '../../../lib/goals.svelte'
    import { GOAL_STATUSES, type Goal, type GoalStatus } from '../../../lib/types'
    import Trash from '../../UI/Trash.svelte'

    interface Props {
        goal: Goal;
    }

    let { goal }: Props = $props();

    let progress = $state(goal.progress);
    let confirming = $state(false);
    let actionError = $state('');
    let confirmTimer: ReturnType<typeof setTimeout> | undefined;

    // Re-sync the slider whenever the store hands back a fresh copy of the goal.
    $effect(() => {
        progress = goal.progress;
    });

    const overdue = $derived(goal.targetDate !== null && daysUntil(goal.targetDate) < 0);
    const done = $derived(goal.status === "COMPLETED");

    function onDeleteClick() {
        // Two clicks, not a native confirm(): the second one is the destructive action.
        if (!confirming) {
            confirming = true;
            confirmTimer = setTimeout(() => (confirming = false), 4000);
            return;
        }

        clearTimeout(confirmTimer);
        confirming = false;
        void onDelete();
    }

    async function onDelete() {
        actionError = '';
        try {
            await goalStore.remove(goal.id);
        } catch (error) {
            actionError = goalStore.describe(error);
        }
    }

    async function onProgress(event: Event & { currentTarget: HTMLInputElement }) {
        const next = Number(event.currentTarget.value);
        const previous = progress;
        progress = next;
        actionError = '';

        try {
            await goalStore.update(goal.id, { progress: next });
        } catch (error) {
            progress = previous;
            actionError = goalStore.describe(error);
        }
    }

    async function onStatus(event: Event & { currentTarget: HTMLSelectElement }) {
        const next = event.currentTarget.value as GoalStatus;
        actionError = '';

        try {
            await goalStore.update(goal.id, { status: next });
        } catch (error) {
            actionError = goalStore.describe(error);
        }
    }

    onDestroy(() => clearTimeout(confirmTimer));
</script>

<article
    class="flex flex-col gap-3 p-4 modern-border bg-black/20 transition-colors
           hover:border-white/25 {done ? 'opacity-70' : ''}"
>
    <header class="flex items-start gap-2">
        <div class="min-w-0 flex-1">
            <h3 class="text-xl leading-tight break-words {done ? 'line-through' : ''}">
                {goal.title}
            </h3>
            {#if goal.description}
                <p class="mt-1 text-sm opacity-60 line-clamp-3 break-words">{goal.description}</p>
            {/if}
        </div>

        <button
            type="button"
            onclick={onDeleteClick}
            onblur={() => (confirming = false)}
            disabled={goalStore.isBusy(goal.id)}
            aria-label={confirming ? `Confirm deleting ${goal.title}` : `Delete ${goal.title}`}
            class="shrink-0 p-1.5 rounded-lg cursor-pointer transition-colors
                   hover:bg-white/10 disabled:opacity-40
                   {confirming ? 'bg-red-500/20 text-red-300' : 'text-white/50 hover:text-white'}"
        >
            {#if confirming}
                <span class="text-xs px-1 font-ui">Sure?</span>
            {:else}
                <Trash class="size-5" />
            {/if}
        </button>
    </header>

    <div class="flex flex-wrap items-center gap-2 text-xs">
        <span
            class="px-2 py-0.5 rounded-full border border-white/15
                   {goal.status === 'COMPLETED'
                ? 'text-secondary border-secondary/40'
                : goal.status === 'IN_PROGRESS'
                  ? 'text-ui border-white/30'
                  : 'text-white/50'}"
        >
            {GOAL_STATUS_LABELS[goal.status]}
        </span>

        <span class="text-white/40 {overdue ? 'text-red-300' : ''}">
            {formatTargetDate(goal.targetDate)}
        </span>
        {#if goal.targetDate}
            <span class="text-white/30">({formatDeadlineHint(goal.targetDate)})</span>
        {/if}
    </div>

    <div class="mt-auto flex flex-col gap-2">
        <div class="flex items-center gap-2">
            <div class="h-2 flex-1 rounded-full bg-white/10 overflow-hidden">
                <div
                    class="h-full rounded-full transition-[width] duration-300
                           {goal.status === 'COMPLETED' ? 'bg-secondary' : 'bg-white/70'}"
                    style:width="{progress}%"
                ></div>
            </div>
            <span class="text-sm tabular-nums w-9 text-right">{progress}%</span>
        </div>

        <input
            type="range"
            min="0"
            max="100"
            step="5"
            bind:value={progress}
            onchange={onProgress}
            disabled={goalStore.isBusy(goal.id)}
            aria-label="Progress for {goal.title}"
            class="w-full accent-secondary disabled:opacity-40 cursor-pointer"
        />

        <select
            value={goal.status}
            onchange={onStatus}
            disabled={goalStore.isBusy(goal.id)}
            aria-label="Status for {goal.title}"
            class="w-full rounded-[5px] border border-white/15 bg-black/40 px-2 py-1.5 text-sm
                   outline-none focus:border-white/40 disabled:opacity-40 cursor-pointer"
        >
            {#each GOAL_STATUSES as status (status)}
                <option value={status}>{GOAL_STATUS_LABELS[status]}</option>
            {/each}
        </select>
    </div>

    {#if actionError}
        <p role="alert" class="text-sm text-red-400">{actionError}</p>
    {/if}
</article>
