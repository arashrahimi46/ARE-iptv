// Movie/Series detail — blurred backdrop, poster, metadata, cast, episodes.
function Detail({ item, onClose, openPlayer }) {
  const C = window.AREIptvDesignSystem_632b75, D = window.AREDATA;
  const { Button, Badge, Tabs, Icon, IconButton } = C;
  const d = D.detail;
  const [tab, setTab] = React.useState("episodes");
  return (
    <div style={{ position: "absolute", inset: 0, zIndex: 30, background: "var(--bg-base)", overflowY: "auto" }}>
      {/* backdrop */}
      <div style={{ position: "absolute", top: 0, left: 0, right: 0, height: 620, background: `center 15%/cover no-repeat url(${d.backdrop})` }} />
      <div style={{ position: "absolute", top: 0, left: 0, right: 0, height: 620, background: "var(--scrim-bottom)" }} />
      <div style={{ position: "absolute", top: 0, left: 0, right: 0, height: 620, background: "var(--scrim-left)" }} />
      <div style={{ position: "relative" }}>
        <div style={{ padding: "24px var(--safe-x) 0" }}>
          <IconButton label="Back" variant="glass" onClick={onClose}><Icon name="arrow-left" /></IconButton>
        </div>
        <div style={{ display: "flex", gap: 44, padding: "120px var(--safe-x) 40px", alignItems: "flex-end" }}>
          <img src={d.poster} alt="" style={{ width: 240, aspectRatio: "2/3", objectFit: "cover", borderRadius: "var(--r-lg)", boxShadow: "var(--shadow-xl)", flex: "0 0 auto", border: "1px solid var(--border-default)" }} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
              <Badge tone="new">NEW SEASON</Badge><Badge tone="quality">{d.quality}</Badge>
            </div>
            <h1 style={{ font: "var(--text-hero)", color: "#fff", letterSpacing: "var(--ls-tight)" }}>{d.title}</h1>
            <div style={{ display: "flex", alignItems: "center", gap: 18, marginTop: 16, font: "var(--text-label)", color: "var(--ink-100)" }}>
              <span style={{ display: "inline-flex", alignItems: "center", gap: 6 }}><span style={{ color: "var(--amber-400)" }}>★</span><b style={{ color: "#fff" }}>{d.rating}</b></span>
              <span>{d.year}</span>
              <span style={{ padding: "1px 8px", border: "1px solid var(--border-strong)", borderRadius: "var(--r-xs)", fontSize: 13 }}>{d.maturity}</span>
              <span>{d.genres}</span>
              <span style={{ color: "var(--text-tertiary)" }}>{d.seasons}</span>
            </div>
            <p style={{ marginTop: 18, font: "var(--text-body)", color: "var(--ink-100)", lineHeight: "var(--lh-normal)", maxWidth: 720 }}>{d.synopsis}</p>
            <div style={{ display: "flex", gap: 14, marginTop: 26 }}>
              <Button size="lg" icon={<Icon name="play" size={22} />} onClick={() => openPlayer({ channel: d.title })}>Play S1 · E1</Button>
              <Button variant="secondary" size="lg" icon={<Icon name="plus" size={20} />}>My list</Button>
              <IconButton label="Favorite" variant="solid" size="lg"><Icon name="heart" /></IconButton>
              <IconButton label="Trailer" variant="solid" size="lg"><Icon name="clapperboard" /></IconButton>
            </div>
          </div>
        </div>
        <div style={{ padding: "0 var(--safe-x)" }}>
          <Tabs active={tab} onSelect={setTab} items={[{ id: "episodes", label: "Episodes" }, { id: "cast", label: "Cast & crew" }, { id: "related", label: "More like this" }]} />
          {tab === "episodes" && <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20, marginTop: 26 }}>
            {d.episodes.map(ep => (
              <div key={ep.n} onClick={() => openPlayer({ channel: `${d.title} · E${ep.n}` })} style={{ display: "flex", gap: 16, padding: 12, borderRadius: "var(--r-md)", background: "var(--surface-1)", border: "1px solid var(--border-subtle)", cursor: "pointer" }}>
                <div style={{ position: "relative", width: 160, aspectRatio: "16/9", borderRadius: "var(--r-sm)", overflow: "hidden", flex: "0 0 auto", background: `center/cover url(${ep.still})` }}>
                  <div style={{ position: "absolute", inset: 0, display: "grid", placeItems: "center", background: "rgba(6,7,10,.3)" }}>
                    <span style={{ marginLeft: 3, borderStyle: "solid", borderWidth: "8px 0 8px 13px", borderColor: "transparent transparent transparent #fff" }} /></div>
                </div>
                <div style={{ minWidth: 0 }}>
                  <div style={{ display: "flex", alignItems: "baseline", gap: 8 }}>
                    <span style={{ font: "var(--fw-bold) var(--fs-title)/1.2 var(--font-body)", color: "var(--text-primary)" }}>{ep.n}. {ep.title}</span>
                    <span style={{ font: "var(--text-caption)", color: "var(--text-tertiary)", marginLeft: "auto" }}>{ep.dur}</span>
                  </div>
                  <p style={{ marginTop: 6, font: "var(--text-caption)", color: "var(--text-secondary)", lineHeight: "var(--lh-normal)" }}>{ep.desc}</p>
                </div>
              </div>
            ))}
          </div>}
          {tab === "cast" && <div style={{ display: "flex", flexWrap: "wrap", gap: 14, marginTop: 26, paddingBottom: 40 }}>
            {d.cast.map((name, i) => (
              <div key={i} style={{ display: "flex", alignItems: "center", gap: 12, padding: "10px 18px 10px 10px", borderRadius: "var(--r-pill)", background: "var(--surface-1)", border: "1px solid var(--border-subtle)" }}>
                <div style={{ width: 44, height: 44, borderRadius: "50%", background: `center/cover url(${D.img("cast" + i, 100, 100)})` }} />
                <span style={{ font: "var(--text-label)", color: "var(--text-primary)" }}>{name}</span>
              </div>
            ))}
          </div>}
          {tab === "related" && <div style={{ display: "flex", gap: 20, marginTop: 26, flexWrap: "wrap", paddingBottom: 40 }}>
            {D.series.slice(0, 6).map((m, i) => <C.PosterTile key={i} {...m} width={180} />)}
          </div>}
        </div>
      </div>
    </div>
  );
}
window.Detail = Detail;
