<script lang="ts">
    interface Props {
        class?: string;
    }

    let { class: className = "size-5" }: Props = $props();
</script>

<svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="2"
    stroke-linecap="round"
    stroke-linejoin="round"
    class={className}
    aria-hidden="true"
>
    <path d="M12 5v14M5 12h14" />
</svg>
