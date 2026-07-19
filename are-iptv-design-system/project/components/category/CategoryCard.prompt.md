**CategoryCard** — folder-style browse tile for an IPTV category (the grouping every content type shares: Live TV, Movies, Series, EPG). 2×2 artwork mosaic + name + count + kind icon. Focus = scale 1.06 + accent ring.

```jsx
<CategoryCard kind="live" name="Sports" count={128}
  posters={[a, b, c, d]} />
<CategoryCard kind="movies" name="Action & Adventure" count={642} posters={[...]} />
<CategoryCard kind="series" name="Suggested for you" count={40} smart />
```
Falls back to a tinted kind-icon panel when `posters` is empty. Use in a `<Rail>` (category rail) or a grid.
