<script lang="ts">
    import AuthField from './AuthField.svelte'
    import { ApiError } from '../../../lib/api'
    import { auth } from '../../../lib/auth.svelte'

    let email = $state('')
    let password = $state('')

    let submitting = $state(false)
    let formError = $state('')
    let fieldErrors = $state<Record<string, string>>({})

    async function onsubmit(event: SubmitEvent) {
        event.preventDefault()
        if (submitting) return

        submitting = true
        formError = ''
        fieldErrors = {}

        try {
            await auth.login({ email: email.trim(), password })
            password = ''
            window.location.assign('/goals')
        } catch (error) {
            password = ''

            if (error instanceof ApiError) {
                formError = error.message
                fieldErrors = error.fieldErrors
            } else {
                formError = 'Could not reach the server. Is the backend running?'
            }
        } finally {
            submitting = false
        }
    }
</script>

<form class="flex flex-col gap-4 w-full max-w-sm" onsubmit={onsubmit} novalidate>
    <AuthField
        label="Email"
        name="email"
        type="email"
        autocomplete="email"
        placeholder="you@example.com"
        maxlength={320}
        bind:value={email}
        error={fieldErrors.email}
    />

    <AuthField
        label="Password"
        name="password"
        type="password"
        autocomplete="current-password"
        bind:value={password}
        error={fieldErrors.password}
    />

    {#if formError}
        <p role="alert" class="text-sm text-red-400">{formError}</p>
    {/if}

    <button
        type="submit"
        disabled={submitting}
        class="rounded-[5px] border border-white/20 px-3 py-2 transition-colors
               hover:bg-white/10 disabled:opacity-50"
    >
        {submitting ? 'Signing in...' : 'Sign in'}
    </button>

    <p class="text-sm opacity-70">
        No account yet?
        <a href="/create" class="underline underline-offset-2 hover:opacity-80">Create one</a>
    </p>
</form>
