// Favorites — multiple favorite lists (channels, movies, sports, kids) + custom
// groups. Smart favorites surfaces frequently-watched automatically.
function Favorites({ openDetail, openPlayer }) {
  const C = window.AREIptvDesignSystem_632b75, D = window.AREDATA;
  const { Tabs, ChannelTile, PosterTile, ContinueCard, Rail, Icon, Badge, Chip } = C;
  const [tab, setTab] = React.useState("channels");
  return (
    <div style={{ padding: "26px 0 40px" }}>
      <div style={{ padding: "0 var(--safe-x)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 24 }}>
          <h1 style={{ font: "var(--text-display)", color: "var(--text-primary)", letterSpacing: "var(--ls-tight)" }}>Favorites</h1>
          <Chip icon={<Icon name="plus" size={16} />} style={{ marginLeft: "auto" }}>New group</Chip>
        </div>
        <Tabs active={tab} onSelect={setTab} items={[
          { id: "channels", label: "Channels" }, { id: "movies", label: "Movies" }, { id: "sports", label: "Sports" }, { id: "kids", label: "Kids" },
        ]} />
      </div>

      <div style={{ marginTop: 26 }}>
        <div style={{ padding: "0 var(--safe-x) 14px", display: "flex", alignItems: "center", gap: 10 }}>
          <Icon name="sparkles" size={18} color="var(--violet-400)" />
          <span style={{ font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)", letterSpacing: "var(--ls-caps)", textTransform: "uppercase", color: "var(--violet-400)" }}>Smart favorites</span>
          <span style={{ font: "var(--text-caption)", color: "var(--text-tertiary)" }}>Frequently watched, surfaced automatically</span>
        </div>
        {tab === "channels" && <div style={{ display: "flex", gap: 20, overflow: "hidden", padding: "0 var(--safe-x)" }}>
          {D.liveNow.map((c, i) => <ChannelTile key={i} {...c} onClick={() => openPlayer(c)} />)}
        </div>}
        {(tab === "movies") && <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(190px,1fr))", gap: 24, padding: "0 var(--safe-x)" }}>
          {D.movies.map((m, i) => <PosterTile key={i} {...m} width="100%" onClick={() => openDetail(m)} />)}
        </div>}
        {tab === "sports" && <div style={{ display: "flex", gap: 20, overflow: "hidden", padding: "0 var(--safe-x)" }}>
          {D.liveNow.slice(1, 4).map((c, i) => <ChannelTile key={i} {...c} onClick={() => openPlayer(c)} />)}
          {D.continueWatching.slice(3, 5).map((c, i) => <ContinueCard key={i} {...c} />)}
        </div>}
        {tab === "kids" && <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(190px,1fr))", gap: 24, padding: "0 var(--safe-x)" }}>
          {D.series.slice(0, 4).map((m, i) => <PosterTile key={i} {...m} width="100%" onClick={() => openDetail(m)} />)}
        </div>}
      </div>

      <div style={{ marginTop: 40 }}>
        <Rail title="Weekend sports" seeAll={false}>
          {D.liveNow.slice(0, 4).map((c, i) => <ChannelTile key={i} {...c} onClick={() => openPlayer(c)} />)}
        </Rail>
      </div>
    </div>
  );
}
window.Favorites = Favorites;
