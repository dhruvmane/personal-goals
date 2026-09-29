<script lang="ts">
    import { onMount } from 'svelte'

    import { daysUntil } from '../../../lib/date'
    import { GOAL_STATUS_LABELS, goalStore } from '../../../lib/goals.svelte'
    import { GOAL_STATUSES, type GoalStatus } from '../../../lib/types'
    import GoalBanner from '../../UI/Goals/Banner.svelte'
    import GoalSquare from '../../UI/Goals/Square.svelte'
    import GoalTimeline from '../../UI/Goals/Timeline.svelte'
    import GoalForm from './GoalForm.svelte'

    type Filter = GoalStatus | "ALL";

    const DEADLINE_SOON_DAYS = 45;

    let filter = $state<Filter>("ALL");

    onMount(() => {
        goalStore.load();
    });

    const visible = $derived(
        filter === "ALL" ? goalStore.goals : goalStore.goals.filter((goal) => goal.status === filter),
    );

    const dueSoon = $derived(
        goalStore.withDeadline
            .filter((goal) => {
                if (goal.status === "COMPLETED" || goal.status === "ARCHIVED") return false;
                return daysUntil(goal.targetDate as string) <= DEADLINE_SOON_DAYS;
            })
            .slice(0, 8),
    );

    const FILTERS: Filter[] = ["ALL", ...GOAL_STATUSES];
</script>

<div class="flex flex-col gap-ui w-full">
    {#if !goalStore.loaded}
        <section class="p-4 modern-border flex flex-col gap-3 animate-pulse" aria-busy="true">
            <div class="h-8 w-40 rounded-lg bg-white/5"></div>
            <div class="h-40 w-full rounded-xl bg-white/5"></div>
            <div class="h-4 w-64 rounded bg-white/5"></div>
            <p class="sr-only">Loading your goals</p>
        </section>
    {:else if !goalStore.signedIn}
        <section class="p-8 modern-border flex flex-col items-center gap-3 text-center">
            <h1 class="text-3xl">Sign in to track your goals</h1>
            <p class="opacity-60 max-w-md">
                Goals are stored per account, so sign in and everything you create here stays
                yours.
            </p>
            <a
                href="/login"
                class="px-4 py-2 rounded-xl border border-white/15 hover:bg-white/10 transition-colors"
            >
                Sign in
            </a>
        </section>
    {:else}
        <!-- SUMMARY -->
        <section class="p-4 modern-border grid grid-cols-2 lg:grid-cols-4 gap-3">
            <div class="flex flex-col">
                <span class="text-3xl tabular-nums">{goalStore.total}</span>
                <span class="text-sm opacity-50">Total goals</span>
            </div>
            <div class="flex flex-col">
                <span class="text-3xl tabular-nums">{goalStore.active}</span>
                <span class="text-sm opacity-50">Still open</span>
            </div>
            <div class="flex flex-col">
                <span class="text-3xl tabular-nums text-secondary">{goalStore.completed}</span>
                <span class="text-sm opacity-50">Completed</span>
            </div>
            <div class="flex flex-col">
                <span class="text-3xl tabular-nums">{goalStore.completionRate}%</span>
                <span class="text-sm opacity-50">Done</span>
            </div>

            <div class="col-span-2 lg:col-span-4 flex items-center gap-3 pt-1">
                <div class="h-2 flex-1 rounded-full bg-white/10 overflow-hidden">
                    <div
                        class="h-full rounded-full bg-secondary transition-[width] duration-300"
                        style:width="{goalStore.averageProgress}%"
                    ></div>
                </div>
                <span class="text-sm tabular-nums opacity-60">
                    {goalStore.averageProgress}% average progress
                </span>
            </div>
        </section>

        <div class="flex flex-col lg:flex-row gap-ui">
            <!-- CREATE -->
            <section class="p-4 modern-border lg:max-w-100">
                <h1 class="text-2xl mb-3">New <span class="text-secondary">Goal</span></h1>
                <GoalForm />
            </section>

            <!-- TIMELINE -->
            <section class="p-2 modern-border flex-1 min-w-0">
                <h1 class="text-2xl my-3 px-2">Timeline</h1>

                {#if goalStore.withDeadline.length === 0}
                    <p class="px-2 pb-3 opacity-50 text-sm">
                        Goals with a deadline show up here.
                    </p>
                {:else}
                    <div class="flex flex-col max-h-72 overflow-y-auto snap-y snap-mandatory">
                        {#each goalStore.withDeadline as goal (goal.id)}
                            <GoalTimeline {goal} />
                        {/each}
                    </div>
                {/if}
            </section>
        </div>

        <!-- MONTHLY / DEADLINE BANNERS -->
        {#if dueSoon.length > 0}
            <section class="p-4 modern-border">
                <h1 class="text-2xl mb-3">
                    Deadlines <span class="text-secondary">closing in</span>
                </h1>
                <div class="w-full gap-ui flex overflow-x-auto snap-x snap-mandatory pb-1">
                    {#each dueSoon as goal (goal.id)}
                        <GoalBanner {goal} />
                    {/each}
                </div>
            </section>
        {/if}

        <!-- GOALS -->
        <section class="p-4 modern-border flex flex-col gap-3">
            <div class="flex flex-wrap items-center gap-2">
                <h1 class="text-2xl mr-auto">Your <span class="text-secondary">Goals</span></h1>

                <div class="flex flex-wrap gap-1">
                    {#each FILTERS as option (option)}
                        <button
                            type="button"
                            onclick={() => (filter = option)}
                            aria-pressed={filter === option}
                            class="px-3 py-1 rounded-[5px] text-sm cursor-pointer transition-colors
                                   {filter === option
                                ? 'bg-white/15'
                                : 'hover:bg-white/5 opacity-60'}"
                        >
                            {option === "ALL" ? 'All' : GOAL_STATUS_LABELS[option]}
                        </button>
                    {/each}
                </div>
            </div>

            {#if goalStore.loading}
                <p class="opacity-50 text-sm py-6 text-center">Loading your goals...</p>
            {:else if goalStore.loadError}
                <div class="flex flex-col items-center gap-3 py-6 text-center">
                    <p role="alert" class="text-red-400">{goalStore.loadError}</p>
                    <button
                        type="button"
                        onclick={() => goalStore.load(true)}
                        class="px-4 py-2 rounded-xl border border-white/15 hover:bg-white/10
                               transition-colors cursor-pointer"
                    >
                        Try again
                    </button>
                </div>
            {:else if goalStore.total === 0}
                <div class="flex flex-col items-center gap-2 py-10 text-center">
                    <h2 class="text-2xl">Nothing here yet</h2>
                    <p class="opacity-50 text-sm max-w-sm">
                        Add your first goal with the form above. It is saved to your account straight
                        away.
                    </p>
                </div>
            {:else if visible.length === 0}
                <p class="opacity-50 text-sm py-6 text-center">
                    No {GOAL_STATUS_LABELS[filter as GoalStatus].toLowerCase()} goals right now.
                </p>
            {:else}
                <div class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-ui">
                    {#each visible as goal (goal.id)}
                        <GoalSquare {goal} />
                    {/each}
                </div>
            {/if}
        </section>
    {/if}
</div>
