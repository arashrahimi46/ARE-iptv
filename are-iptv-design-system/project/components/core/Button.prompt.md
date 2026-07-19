**Button** — the primary action control; use for any confirm/play/add action. Built-in D-pad focus glow.

```jsx
<Button variant="primary" size="lg" icon={<PlayIcon/>}>Play</Button>
<Button variant="secondary">Open guide</Button>
<Button variant="ghost">Cancel</Button>
<Button variant="danger">Remove playlist</Button>
```

Variants: `primary` (accent), `secondary` (surface), `ghost` (bare), `danger`. Sizes `sm|md|lg`. Pass `focused` to force the glow in a static preview.
