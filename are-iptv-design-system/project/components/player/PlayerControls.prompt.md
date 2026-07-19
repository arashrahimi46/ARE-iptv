**PlayerControls** — glass transport HUD over live/VOD video. Includes the TimeShift seek bar (buffered region + live edge) and quick actions (audio, subtitles, multi-view, PiP, guide). Needs Lucide + uses `Icon`/`IconButton`.

```jsx
<PlayerControls title="The News at Ten" subtitle="BBC One HD · Now · 22:00 – 22:30"
  live playing position={62} buffered={82} elapsed="20:28" total="30:00" channelLogoInitials="BBC" />
```
