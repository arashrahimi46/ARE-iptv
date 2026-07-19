// Live TV — category filter column (CategoryRow) + channel grid, with an animated
// transition that re-plays when you switch category (panel fade + staggered tiles).
function Live({ openPlayer }) {
  const C = window.AREIptvDesignSystem_632b75, D = window.AREDATA;
  const { CategoryRow, ChannelTile, Badge, Icon } = C;
  const [catIdx, setCatIdx] = React.useState(0);
  const cat = D.liveCats[catIdx];
  const channels = cat.all ? D.liveChannelPool : D.liveChannelPool.filter(c => c.cat === cat.name);
  const list = channels.length ? channels : D.liveChannelPool.slice(0, 6);

  return (
    <div style={{ padding: "26px 0 40px" }}>
      <div style={{ padding: "0 var(--safe-x)" }}>
        <div style={{ display: "flex", alignItems: "baseline", gap: 12, marginBottom: 22 }}>
          <h1 style={{ font: "var(--text-display)", color: "var(--text-primary)", letterSpacing: "var(--ls-tight)" }}>Live TV</h1>
          <span style={{ display: "inline-flex", alignItems: "center", gap: 7, padding: "5px 10px", borderRadius: "var(--r-pill)",
            background: "rgba(239,68,68,0.14)", border: "1px solid rgba(239,68,68,0.4)", color: "var(--red-400)", font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)", letterSpacing: "var(--ls-caps)" }}>
            <span style={{ width: 8, height: 8, borderRadius: "50%", background: "var(--live)", boxShadow: "var(--glow-live)" }} />ON AIR NOW</span>
        </div>
        <div style={{ display: "flex", gap: 32, alignItems: "flex-start" }}>
          {/* category filter column */}
          <div style={{ width: 300, flex: "0 0 auto", position: "sticky", top: 96, display: "flex", flexDirection: "column", gap: 4 }}>
            <p style={{ font: "var(--fw-bold) 11px/1 var(--font-body)", letterSpacing: "var(--ls-caps)", textTransform: "uppercase",
              color: "var(--text-tertiary)", margin: "0 0 8px", padding: "0 16px" }}>Channel groups</p>
            {D.liveCats.map((c, i) => (
              <CategoryRow key={c.name} name={c.name} count={c.count} kind={c.kind} smart={c.smart}
                active={i === catIdx} onClick={() => setCatIdx(i)} />
            ))}
          </div>
          {/* channel grid — keyed by category so the transition re-plays on switch */}
          <div key={catIdx} className="cat-panel" style={{ flex: 1, minWidth: 0 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 18 }}>
              <span style={{ display: "grid", placeItems: "center", width: 40, height: 40, borderRadius: "var(--r-sm)",
                background: "var(--surface-2)", border: "1px solid var(--border-subtle)", color: "var(--accent-hover)" }}>
                <Icon name="radio" size={22} />
              </span>
              <h2 style={{ font: "var(--text-h2)", color: "var(--text-primary)", letterSpacing: "var(--ls-tight)" }}>{cat.name}</h2>
              <span style={{ font: "var(--text-mono)", color: "var(--text-tertiary)" }}>{cat.count.toLocaleString()} channels</span>
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(250px, 1fr))", gap: 18 }}>
              {list.map((c, i) => (
                <div key={c.number} className="cat-tile" style={{ animationDelay: `${i * 34}ms` }}>
                  <ChannelTile {...c} width="100%" onClick={() => openPlayer(c)} />
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
window.Live = Live;
