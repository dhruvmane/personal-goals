<script lang="ts">
    import AuthField from './AuthField.svelte'
    import { ApiError } from '../../../lib/api'
    import { auth } from '../../../lib/auth.svelte'

    let displayName = $state('')
    let email = $state('')
    let password = $state('')
    let confirmPassword = $state('')

    let submitting = $state(false)
    let formError = $state('')
    let fieldErrors = $state<Record<string, string>>({})
    let mismatch = $state('')

    async function onsubmit(event: SubmitEvent) {
        event.preventDefault()
        if (submitting) return

        formError = ''
        fieldErrors = {}
        mismatch = ''

        // Caught here so the user gets an answer without a round trip. The server
        // re-validates regardless.
        if (password !== confirmPassword) {
            mismatch = 'Passwords do not match.'
            return
        }

        submitting = true

        try {
            const display = displayName.trim()
            await auth.register({
                email: email.trim(),
                password,
                ...(display ? { displayName: display } : {}),
            })
            password = ''
            confirmPassword = ''
            window.location.assign('/goals')
        } catch (error) {
            password = ''
            confirmPassword = ''

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
        label="Display name"
        name="displayName"
        autocomplete="name"
        placeholder="Optional"
        required={false}
        maxlength={120}
        bind:value={displayName}
        error={fieldErrors.displayName}
    />

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
        autocomplete="new-password"
        minlength={8}
        maxlength={72}
        bind:value={password}
        error={fieldErrors.password}
    />

    <AuthField
        label="Confirm password"
        name="confirmPassword"
        type="password"
        autocomplete="new-password"
        minlength={8}
        maxlength={72}
        bind:value={confirmPassword}
        error={mismatch || fieldErrors.confirmPassword}
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
        {submitting ? 'Creating account...' : 'Create account'}
    </button>

    <p class="text-sm opacity-70">
        Already have an account?
        <a href="/login" class="underline underline-offset-2 hover:opacity-80">Sign in</a>
    </p>
</form>
