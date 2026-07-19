**PosterTile** — portrait movie/series tile for VOD rails. Focus = scale 1.06 + accent ring.

```jsx
<PosterTile title="Dune: Part Two" meta="2024 · Sci-Fi" image={url} rating={9.1}
  badges={[<Badge tone="quality">4K HDR</Badge>]} />
<PosterTile title="Breaking Bad" meta="S5 · E14" image={url} progress={62} />
```
Falls back to an initials chip when `image` is omitted.
