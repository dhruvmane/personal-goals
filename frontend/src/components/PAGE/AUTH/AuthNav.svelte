<script lang="ts">
    import { onMount } from 'svelte'

    import { auth } from '../../../lib/auth.svelte'

    let signingOut = $state(false)

    onMount(() => {
        auth.restore()
    })

    async function signOut() {
        signingOut = true
        try {
            await auth.logout()
            window.location.assign('/login')
        } finally {
            signingOut = false
        }
    }
</script>

{#if !auth.ready}
    <div class="w-20" aria-hidden="true"></div>
{:else if auth.isAuthenticated}
    <div class="flex items-center gap-3">
        <a href="/profile" class="max-w-32 truncate text-sm opacity-70 hover:opacity-100">
            {auth.user?.displayName || auth.user?.email}
        </a>
        <button
            onclick={signOut}
            disabled={signingOut}
            class="min-w-15 text-center px-2 py-1 rounded-[5px] border border-white/15
                   hover:bg-white/10 transition-colors disabled:opacity-50"
        >
            {signingOut ? '...' : 'Sign out'}
        </button>
    </div>
{:else}
    <a
        href="/login"
        class="min-w-15 text-center px-2 py-1 rounded-[5px] border border-white/15 hover:bg-white/10 transition-colors"
    >
        Sign in
    </a>
{/if}
