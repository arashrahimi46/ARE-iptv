**GuideCell** — one program in the EPG grid. Caller sets `width` proportional to duration.

```jsx
<GuideCell time="20:00 – 20:45" title="The News at Ten" now progress={40} width={220} />
<GuideCell time="19:00 – 20:00" title="Regional News" catchup width={180} />
```
Long titles: ellipsis at rest, marquee-scroll inside the cell on focus. Wire `onFocusChange` to a fixed focused-program info bar above the grid (full title, time range, channel) — the 10-foot answer to truncation; no tooltips on TV.
