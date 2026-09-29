<script lang="ts">
    import { ApiError } from '../../../lib/api'
    import { GOAL_STATUS_LABELS, goalStore } from '../../../lib/goals.svelte'
    import { GOAL_STATUSES, type GoalStatus } from '../../../lib/types'
    import Plus from '../../UI/Plus.svelte'

    let title = $state('');
    let description = $state('');
    let status = $state<GoalStatus>('PENDING');
    let targetDate = $state('');
    let progress = $state(0);

    let submitting = $state(false);
    let formError = $state('');
    let fieldErrors = $state<Record<string, string>>({});
    let created = $state('');

    function reset() {
        title = '';
        description = '';
        status = 'PENDING';
        targetDate = '';
        progress = 0;
    }

    async function onsubmit(event: SubmitEvent) {
        event.preventDefault();
        if (submitting) return;

        submitting = true;
        formError = '';
        fieldErrors = {};
        created = '';

        try {
            const goal = await goalStore.create({
                title,
                description,
                status,
                progress,
                targetDate,
            });

            created = `"${goal.title}" added`;
            reset();
        } catch (error) {
            if (error instanceof ApiError) {
                formError = error.message;
                fieldErrors = error.fieldErrors;
            } else {
                formError = goalStore.describe(error);
            }
        } finally {
            submitting = false;
        }
    }
</script>

<form class="flex flex-col gap-3" onsubmit={onsubmit} novalidate>
    <label class="flex flex-col gap-1">
        <span class="text-sm opacity-70">Goal</span>
        <input
            name="title"
            type="text"
            bind:value={title}
            required
            maxlength={200}
            placeholder="Run a 10k"
            aria-invalid={fieldErrors.title ? 'true' : undefined}
            aria-describedby={fieldErrors.title ? 'goal-title-error' : undefined}
            class="w-full rounded-[5px] border border-white/15 bg-black/40 px-3 py-2 text-ui
                   outline-none transition-colors placeholder:opacity-40
                   focus:border-white/40 aria-[invalid=true]:border-red-400"
        />
        {#if fieldErrors.title}
            <span id="goal-title-error" class="text-sm text-red-400">{fieldErrors.title}</span>
        {/if}
    </label>

    <label class="flex flex-col gap-1">
        <span class="text-sm opacity-70">Notes</span>
        <textarea
            name="description"
            bind:value={description}
            rows="2"
            maxlength="5000"
            placeholder="Easy pace, no hills"
            class="w-full resize-y rounded-[5px] border border-white/15 bg-black/40 px-3 py-2
                   text-ui outline-none transition-colors placeholder:opacity-40
                   focus:border-white/40"
        ></textarea>
        {#if fieldErrors.description}
            <span class="text-sm text-red-400">{fieldErrors.description}</span>
        {/if}
    </label>

    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <label class="flex flex-col gap-1">
            <span class="text-sm opacity-70">Status</span>
            <select
                name="status"
                bind:value={status}
                class="w-full rounded-[5px] border border-white/15 bg-black/40 px-3 py-2 text-ui
                       outline-none focus:border-white/40 cursor-pointer"
            >
                {#each GOAL_STATUSES as option (option)}
                    <option value={option}>{GOAL_STATUS_LABELS[option]}</option>
                {/each}
            </select>
        </label>

        <label class="flex flex-col gap-1">
            <span class="text-sm opacity-70">Deadline</span>
            <input
                name="targetDate"
                type="date"
                bind:value={targetDate}
                aria-invalid={fieldErrors.targetDate ? 'true' : undefined}
                class="w-full rounded-[5px] border border-white/15 bg-black/40 px-3 py-2 text-ui
                       outline-none transition-colors focus:border-white/40
                       aria-[invalid=true]:border-red-400"
            />
            {#if fieldErrors.targetDate}
                <span class="text-sm text-red-400">{fieldErrors.targetDate}</span>
            {/if}
        </label>
    </div>

    <label class="flex flex-col gap-1">
        <span class="text-sm opacity-70 flex items-center justify-between">
            <span>Progress</span>
            <span class="tabular-nums">{progress}%</span>
        </span>
        <input
            name="progress"
            type="range"
            min="0"
            max="100"
            step="5"
            bind:value={progress}
            aria-label="Progress"
            class="w-full accent-secondary cursor-pointer"
        />
    </label>

    {#if formError}
        <p role="alert" class="text-sm text-red-400">{formError}</p>
    {/if}

    <div class="flex flex-wrap items-center gap-3">
        <button
            type="submit"
            disabled={submitting || title.trim().length === 0}
            class="flex items-center gap-2 px-4 py-2 rounded-xl border border-white/15 cursor-pointer
                   transition-colors hover:bg-white/10 disabled:opacity-40 disabled:cursor-not-allowed"
        >
            <Plus class="size-4" />
            {submitting ? 'Adding...' : 'Add goal'}
        </button>

        <button
            type="button"
            onclick={reset}
            disabled={submitting}
            class="px-3 py-2 rounded-xl text-sm opacity-60 hover:opacity-100
                   transition-colors cursor-pointer disabled:opacity-40"
        >
            Clear
        </button>

        {#if created}
            <p role="status" class="text-sm text-secondary">{created}</p>
        {/if}
    </div>
</form>
