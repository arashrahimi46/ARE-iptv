// Home dashboard — hero + rails (continue watching, live now, movies, series, AI recs)
function Home({ openDetail, openPlayer }) {
  const C = window.AREIptvDesignSystem_632b75, D = window.AREDATA;
  const { Hero, Rail, ContinueCard, ChannelTile, PosterTile, CategoryCard, Badge, Button, Icon } = C;
  const posterBadges = (b) => b ? [<Badge key="b" tone={b === "NEW" ? "new" : "quality"}>{b}</Badge>] : [];
  return (
    <div style={{ paddingTop: 28, paddingBottom: 40 }}>
      <div style={{ padding: "0 var(--safe-x) 12px" }}>
        <Hero {...D.featured} height={460}
          badges={[<Badge key="l" tone="live" glow>LIVE</Badge>, <Badge key="q" tone="quality">4K HDR</Badge>]}
          actions={<>
            <Button size="lg" icon={<Icon name="play" size={22} />} onClick={() => openPlayer(D.liveNow[1])}>Watch live</Button>
            <Button variant="secondary" size="lg" icon={<Icon name="info" size={20} />}>More info</Button>
          </>} />
      </div>
      <Rail title="Continue watching" seeAll>
        {D.continueWatching.map((c, i) => <ContinueCard key={i} {...c} onClick={() => openPlayer({ channel: c.title, now: c.meta })} />)}
      </Rail>
      <Rail title="Live now" seeAll>
        {D.liveNow.map((c, i) => <ChannelTile key={i} {...c} onClick={() => openPlayer(c)} />)}
      </Rail>
      <Rail title="Browse by category" seeAll>
        {D.browseCats.map((c, i) => <CategoryCard key={i} {...c} width={272} />)}
      </Rail>
      <Rail title="Recommended for you" smart seeAll>
        {D.recommended.map(({ smart, ...m }, i) => <PosterTile key={i} {...m} onClick={() => openDetail(m)} />)}
      </Rail>
      <Rail title="Movies" seeAll>
        {D.movies.map((m, i) => <PosterTile key={i} {...m} badges={posterBadges(m.badge)} onClick={() => openDetail(m)} />)}
      </Rail>
      <Rail title="Series" seeAll>
        {D.series.map((m, i) => <PosterTile key={i} {...m} badges={posterBadges(m.badge)} onClick={() => openDetail(m)} />)}
      </Rail>
    </div>
  );
}
window.Home = Home;
