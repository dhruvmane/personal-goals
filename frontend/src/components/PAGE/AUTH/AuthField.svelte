<script lang="ts">
    interface Props {
        label: string;
        name: string;
        value: string;
        type?: string;
        error?: string;
        autocomplete?: string;
        placeholder?: string;
        required?: boolean;
        maxlength?: number;
        minlength?: number;
    }

    let {
        label,
        name,
        value = $bindable(''),
        type = 'text',
        error = undefined,
        autocomplete = undefined,
        placeholder = undefined,
        required = true,
        maxlength = undefined,
        minlength = undefined,
    }: Props = $props();
</script>

<label class="flex flex-col gap-1 w-full">
    <span class="text-sm opacity-70">{label}</span>

    <input
        {name}
        {type}
        {required}
        {autocomplete}
        {placeholder}
        {maxlength}
        {minlength}
        bind:value
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={error ? `${name}-error` : undefined}
        class="w-full rounded-[5px] border border-white/15 bg-black/40 px-3 py-2 text-ui
               outline-none transition-colors placeholder:opacity-40
               focus:border-white/40 aria-[invalid=true]:border-red-400"
    />

    {#if error}
        <span id="{name}-error" class="text-sm text-red-400">{error}</span>
    {/if}
</label>
