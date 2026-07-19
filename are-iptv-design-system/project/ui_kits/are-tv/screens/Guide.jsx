// Full EPG guide grid — timeline header, channel column, proportional program cells.
// Guide is filterable by channel group (the TiviMate / OTT Navigator pattern:
// the guide always scopes to a group so 1000+-channel playlists stay usable).
function Guide({ openPlayer }) {
  const C = window.AREIptvDesignSystem_632b75, D = window.AREDATA;
  const { GuideCell, Chip, Icon, StreamHealth } = C;
  const PX = 3.2; // px per minute
  const times = ["18:00", "18:30", "19:00", "19:30", "20:00", "20:30", "21:00", "21:30", "22:00", "22:30", "23:00", "23:30"];
  const [day, setDay] = React.useState("Today");
  const [grp, setGrp] = React.useState("All");
  const GRP_ICON = { All: "layout-grid", Entertainment: "tv", Sports: "trophy", News: "newspaper", Documentary: "globe", Kids: "baby" };
  const groups = ["All", ...Array.from(new Set(D.epgChannels.map(c => c.cat)))];
  const rows = D.epgChannels.map((c, i) => ({ ...c, ri: i })).filter(c => grp === "All" || c.cat === grp);
  // focused-program info bar — full details for the cell under focus (10-foot
  // pattern: no tooltips on TV). Sticky: keeps the last focused program.
  const endOf = (t, dur) => { const [h, m] = t.split(":").map(Number); const e = h * 60 + m + dur; return `${String(Math.floor(e / 60) % 24).padStart(2, "0")}:${String(e % 60).padStart(2, "0")}`; };
  const [info, setInfo] = React.useState(() => ({ chan: D.epgChannels[3], p: D.epgRows[3].find(p => p[3] === "now") }));
  return (
    <div style={{ padding: "26px 0 40px" }}>
      <div style={{ display: "flex", alignItems: "center", gap: 16, padding: "0 var(--safe-x) 20px" }}>
        <h1 style={{ font: "var(--text-display)", color: "var(--text-primary)", letterSpacing: "var(--ls-tight)" }}>TV Guide</h1>
        <div style={{ flex: 1 }} />
        {["Yesterday", "Today", "Tomorrow"].map(d => <Chip key={d} selected={d === day} onClick={() => setDay(d)}>{d}</Chip>)}
        <Chip icon={<Icon name="refresh-cw" size={16} />}>Refresh EPG</Chip>
      </div>
      {/* channel-group filter — scopes the guide to one playlist group */}
      <div style={{ display: "flex", alignItems: "center", gap: 10, padding: "0 var(--safe-x) 18px", flexWrap: "wrap" }}>
        {groups.map(g => {
          const on = g === grp;
          const n = g === "All" ? D.epgChannels.length : D.epgChannels.filter(c => c.cat === g).length;
          return (
            <button key={g} onClick={() => setGrp(g)}
              style={{ display: "inline-flex", alignItems: "center", gap: 9, height: 44, padding: "0 18px", borderRadius: "var(--r-pill)",
                cursor: "pointer", border: "none", font: "var(--text-label)", transition: "var(--tr-color)",
                background: on ? "var(--accent)" : "var(--surface-2)", color: on ? "var(--accent-fg)" : "var(--text-secondary)",
                outline: on ? "1px solid var(--accent)" : "1px solid var(--border-subtle)", outlineOffset: -1,
                boxShadow: on ? "var(--glow-accent)" : "none" }}>
              <Icon name={GRP_ICON[g] || "folder"} size={18} color={on ? "var(--accent-fg)" : "var(--text-tertiary)"} />
              {g === "All" ? "All channels" : g}
              <span style={{ font: "var(--fw-bold) var(--fs-micro)/1 var(--font-mono)", opacity: 0.75 }}>{n}</span>
            </button>
          );
        })}
      </div>
      {/* focused-program info bar */}
      {info && info.p && <div style={{ display: "flex", alignItems: "center", gap: 14, margin: "0 var(--safe-x) 18px", padding: "12px 16px",
        background: "var(--surface-1)", border: "1px solid var(--border-subtle)", borderRadius: "var(--r-md)" }}>
        <div style={{ width: 44, height: 44, borderRadius: "var(--r-xs)", background: "var(--surface-3)", display: "grid", placeItems: "center",
          font: "var(--fw-bold) 13px/1 var(--font-display)", color: "var(--text-primary)", flex: "0 0 auto" }}>{info.chan.logo}</div>
        <div style={{ minWidth: 0, flex: 1 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10, minWidth: 0 }}>
            <span style={{ font: "var(--fw-semibold) var(--fs-title)/1.15 var(--font-display)", color: "var(--text-primary)", letterSpacing: "var(--ls-tight)",
              whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{info.p[1]}</span>
            {info.p[3] === "now" && <span style={{ padding: "3px 7px", borderRadius: "var(--r-xs)", background: "var(--live)", color: "#fff", flex: "0 0 auto",
              font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)", letterSpacing: "var(--ls-caps)" }}>LIVE</span>}
            {info.p[3] === "catchup" && <span style={{ padding: "3px 7px", borderRadius: "var(--r-xs)", background: "rgba(34,197,94,0.14)", border: "1px solid rgba(34,197,94,0.4)",
              color: "var(--green-400)", flex: "0 0 auto", font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)", letterSpacing: "var(--ls-caps)" }}>CATCH-UP</span>}
          </div>
          <div style={{ marginTop: 3, font: "var(--text-caption)", color: "var(--text-secondary)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
            {info.chan.name} · <span style={{ font: "var(--text-mono)" }}>{info.p[0]} – {endOf(info.p[0], info.p[2])}</span> · {info.p[2]} min</div>
        </div>
        <span style={{ font: "var(--text-caption)", color: "var(--text-tertiary)", flex: "0 0 auto" }}>OK · Watch</span>
      </div>}
      <div style={{ overflowX: "auto", padding: "0 var(--safe-x)" }}>
        <div style={{ minWidth: 12 * 30 * PX + 220 }}>
          {/* timeline header */}
          <div style={{ display: "flex", position: "sticky", top: 0, zIndex: 2 }}>
            <div style={{ width: "var(--guide-chan-w)", flex: "0 0 auto" }} />
            <div style={{ display: "flex", flex: 1 }}>
              {times.map(t => <div key={t} style={{ width: 30 * PX, flex: "0 0 auto", font: "var(--text-mono)", fontSize: 13, color: "var(--text-tertiary)", padding: "0 0 12px 4px", borderLeft: "1px solid var(--border-subtle)" }}>{t}</div>)}
            </div>
          </div>
          {/* rows — keyed by group so the filter transition re-plays */}
          <div key={grp} className="cat-panel" style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {rows.map((chan) => (
              <div key={chan.ri} style={{ display: "flex", gap: 8, alignItems: "center" }}>
                <div style={{ width: "var(--guide-chan-w)", flex: "0 0 auto", display: "flex", alignItems: "center", gap: 12, padding: "0 12px", height: "var(--guide-row-h)", background: "var(--surface-1)", borderRadius: "var(--r-sm)", border: "1px solid var(--border-subtle)" }}>
                  <div style={{ width: 42, height: 42, borderRadius: "var(--r-xs)", background: "var(--surface-3)", display: "grid", placeItems: "center", font: "var(--fw-bold) 13px/1 var(--font-display)", color: "var(--text-primary)", flex: "0 0 auto" }}>{chan.logo}</div>
                  <div style={{ minWidth: 0 }}>
                    <div style={{ font: "var(--text-mono)", fontSize: 12, color: "var(--text-tertiary)" }}>{chan.num}</div>
                    <div style={{ font: "var(--fw-semibold) 14px/1.1 var(--font-body)", color: "var(--text-primary)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{chan.name}</div>
                  </div>
                </div>
                <div style={{ display: "flex", gap: 6, flex: 1 }}>
                  {D.epgRows[chan.ri].map((p, ci) => (
                    <GuideCell key={ci} time={p[0]} title={p[1]} width={p[2] * PX - 6}
                      now={p[3] === "now"} live={p[3] === "now" && chan.ri === 3} catchup={p[3] === "catchup"} progress={p[3] === "now" ? 55 : 0}
                      onFocusChange={(on) => on && setInfo({ chan, p })}
                      onClick={() => openPlayer(D.liveNow[Math.min(chan.ri, D.liveNow.length - 1)])} />
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
window.Guide = Guide;
