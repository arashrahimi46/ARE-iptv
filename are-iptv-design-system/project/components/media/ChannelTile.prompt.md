**ChannelTile** — logo-first live-TV tile. No stream preview (IPTV previews are unreliable) — the channel logo is the artwork (mono initials chip fallback), with an in-card info panel: now-playing + progress, next up, and quality info.

```jsx
<ChannelTile channel="BBC One HD" number="101" now="The News at Ten" next="Match of the Day"
  progress={70} health="stable" quality="FHD" codec="H.264" catchup fav />
```
`health`: `stable` (green) · `moderate` (amber) · `poor` (red). `quality`: "4K" | "FHD" | "HD" | "SD". Grid them dense: `repeat(auto-fill, minmax(250px, 1fr))`.
