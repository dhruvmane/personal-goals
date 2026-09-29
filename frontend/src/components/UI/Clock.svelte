<script lang="ts">
    import { onMount } from 'svelte'

    let now = $state(new Date());

    onMount(() => {
        const timer = setInterval(() => (now = new Date()), 30_000);
        return () => clearInterval(timer);
    });
</script>

<!-- <time datetime={now.toISOString()}>{now.toLocaleTimeString()}</time> -->
