<script lang="ts">
    import { daysUntil, formatDeadlineHint, formatTargetDate } from '../../../lib/date'
    import { GOAL_STATUS_LABELS, goalStore } from '../../../lib/goals.svelte'
    import type { Goal } from '../../../lib/types'
    import Trash from '../../UI/Trash.svelte'

    interface Props {
        goal: Goal;
    }

    let { goal }: Props = $props();

    let actionError = $state('');

    const days = $derived(goal.targetDate === null ? null : daysUntil(goal.targetDate));
    const urgent = $derived(days !== null && days <= 7);
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

<article
    class="snap-center shrink-0 w-70 sm:w-90 aspect-video p-4 flex flex-col gap-2
           border border-white/10 rounded-xl bg-black/20"
>
    <header class="flex items-start gap-2">
        <h3 class="text-lg leading-tight line-clamp-2 flex-1 break-words">{goal.title}</h3>
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
    </header>

    <div class="flex items-baseline gap-2">
        {#if days !== null}
            <span
                class="text-3xl tabular-nums {overdue
                    ? 'text-red-300'
                    : urgent
                      ? 'text-secondary'
                      : 'text-ui'}"
            >
                {overdue ? Math.abs(days) : days}
            </span>
            <span class="text-sm opacity-50">
                {overdue ? 'days overdue' : days === 1 ? 'day left' : 'days left'}
            </span>
        {:else}
            <span class="text-3xl text-ui">-</span>
            <span class="text-sm opacity-50">no deadline</span>
        {/if}
    </div>

    <div class="mt-auto flex items-center gap-3">
        <div class="h-2 flex-1 rounded-full bg-white/10 overflow-hidden">
            <div
                class="h-full rounded-full bg-secondary transition-[width] duration-300"
                style:width="{goal.progress}%"
            ></div>
        </div>
        <span class="text-xs opacity-50 shrink-0">{formatDeadlineHint(goal.targetDate)}</span>
    </div>

    <footer class="flex items-center justify-between text-xs opacity-60">
        <span>{GOAL_STATUS_LABELS[goal.status]}</span>
        <span>{formatTargetDate(goal.targetDate)}</span>
    </footer>

    {#if actionError}
        <p role="alert" class="text-sm text-red-400">{actionError}</p>
    {/if}
</article>
