// Universal search — one query across live, movies, series, catch-up. Shows a
// simulated keyboard-driven query with typed value and grouped results.
function Search({ openDetail, openPlayer }) {
  const C = window.AREIptvDesignSystem_632b75, D = window.AREDATA;
  const { TextField, Chip, PosterTile, ChannelTile, Badge, Icon } = C;
  const [q, setQ] = React.useState("f1");
  const [scope, setScope] = React.useState("All");
  const scopes = ["All", "Live TV", "Movies", "Series", "Catch-up"];
  return (
    <div style={{ padding: "26px 0 40px" }}>
      <div style={{ padding: "0 var(--safe-x)" }}>
        <div style={{ maxWidth: 760 }}>
          <TextField value={q} onChange={e => setQ(e.target.value)} placeholder="Search channels, movies, actors…"
            icon={<Icon name="search" size={20} />} focused />
        </div>
        <div style={{ display: "flex", gap: 10, margin: "20px 0 30px" }}>
          {scopes.map(s => <Chip key={s} selected={s === scope} onClick={() => setScope(s)}>{s}</Chip>)}
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 16 }}>
          <span style={{ font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)", letterSpacing: "var(--ls-caps)", textTransform: "uppercase", color: "var(--violet-400)" }}>Universal results</span>
          <Badge tone="smart">AI RANKED</Badge>
        </div>
        <h3 style={{ font: "var(--text-h3)", color: "var(--text-secondary)", margin: "6px 0 16px" }}>Live &amp; catch-up</h3>
        <div style={{ display: "flex", gap: 20, overflow: "hidden", marginBottom: 30 }}>
          {D.liveNow.slice(1, 4).map((c, i) => <ChannelTile key={i} {...c} onClick={() => openPlayer(c)} />)}
        </div>
        <h3 style={{ font: "var(--text-h3)", color: "var(--text-secondary)", margin: "6px 0 16px" }}>Movies &amp; series</h3>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))", gap: 22 }}>
          {D.recommended.map((m, i) => <PosterTile key={i} {...m} width="100%" onClick={() => openDetail(m)} />)}
        </div>
      </div>
    </div>
  );
}
window.Search = Search;
