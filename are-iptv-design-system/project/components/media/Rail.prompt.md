**Rail** — titled horizontal scroller; the primary Home layout unit. Drop tiles inside.

```jsx
<Rail title="Continue watching" seeAll>
  <ContinueCard .../> <ContinueCard .../>
</Rail>
<Rail title="Recommended for you" smart>{/* posters */}</Rail>
```
`smart` adds the violet SMART tag for AI-organized rows.
