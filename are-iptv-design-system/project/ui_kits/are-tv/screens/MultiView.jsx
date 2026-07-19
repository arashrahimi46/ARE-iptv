// Multi-view — 2 or 4 simultaneous live streams; one is the active (audio) pane.
function MultiView({ onClose }) {
  const C = window.AREIptvDesignSystem_632b75, D = window.AREDATA;
  const { IconButton, Icon, Badge, StreamHealth, Chip } = C;
  const [count, setCount] = React.useState(4);
  const [active, setActive] = React.useState(0);
  const panes = [
    { title: "British Grand Prix", ch: "Sky Sports F1", seed: "mv-f1", health: "stable" },
    { title: "Lakers @ Celtics", ch: "ESPN", seed: "mv-nba", health: "moderate" },
    { title: "Live Football", ch: "Sky Sports Main", seed: "mv-football", health: "stable" },
    { title: "Global News Hour", ch: "CNN", seed: "mv-cnn", health: "stable" },
  ].slice(0, count);
  return (
    <div style={{ position: "absolute", inset: 0, zIndex: 40, background: "#000", display: "flex", flexDirection: "column" }}>
      <div style={{ display: "flex", alignItems: "center", gap: 14, padding: "18px 28px", zIndex: 2 }}>
        <IconButton label="Back" variant="glass" onClick={onClose}><Icon name="arrow-left" /></IconButton>
        <h2 style={{ font: "var(--text-h2)", color: "#fff" }}>Multi-view</h2>
        <div style={{ flex: 1 }} />
        <Chip icon={<Icon name="grid-2x2" size={16} />} selected={count === 4} onClick={() => setCount(4)}>4-up</Chip>
        <Chip icon={<Icon name="columns-2" size={16} />} selected={count === 2} onClick={() => setCount(2)}>2-up</Chip>
      </div>
      <div style={{ flex: 1, display: "grid", gap: 10, padding: "0 28px 28px",
        gridTemplateColumns: count === 2 ? "1fr 1fr" : "1fr 1fr", gridTemplateRows: count === 2 ? "1fr" : "1fr 1fr" }}>
        {panes.map((p, i) => {
          const on = i === active;
          return (
            <div key={i} onClick={() => setActive(i)} style={{ position: "relative", borderRadius: "var(--r-md)", overflow: "hidden", cursor: "pointer",
              background: `center/cover no-repeat url(${D.wide(p.seed)})`,
              outline: on ? "3px solid var(--accent)" : "1px solid var(--border-default)", boxShadow: on ? "var(--glow-accent)" : "none" }}>
              <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(6,7,10,.85), rgba(6,7,10,0) 55%)" }} />
              <div style={{ position: "absolute", top: 12, left: 12, display: "flex", gap: 8 }}>
                <Badge tone="live" glow>LIVE</Badge>
                {on && <Badge tone="new"><Icon name="volume-2" size={12} /> AUDIO</Badge>}
              </div>
              <div style={{ position: "absolute", top: 12, right: 12 }}><StreamHealth level={p.health} label={false} /></div>
              <div style={{ position: "absolute", left: 16, bottom: 14 }}>
                <div style={{ font: "var(--text-h3)", color: "#fff" }}>{p.title}</div>
                <div style={{ font: "var(--text-caption)", color: "var(--ink-100)", marginTop: 3 }}>{p.ch}</div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
window.MultiView = MultiView;
