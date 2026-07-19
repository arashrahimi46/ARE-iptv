// Browse — category filter column (CategoryRow) + poster grid. Doubles for Movies / Series.
function Browse({ title = "Movies", data, initialCat = 0, openDetail }) {
  const C = window.AREIptvDesignSystem_632b75, D = window.AREDATA;
  const { CategoryRow, PosterTile, Badge } = C;
  const cats = title === "Series" ? D.seriesCats : D.movieCats;
  const [catIdx, setCatIdx] = React.useState(initialCat);
  React.useEffect(() => { setCatIdx(initialCat); }, [initialCat]);
  const all = data || [...D.movies, ...D.series];
  const cat = cats[catIdx];
  const items = cat.all || cat.recent ? all : all.filter(m => D.genreOf(m).includes(cat.match));
  const posterBadges = (b) => b ? [<Badge key="b" tone={b === "NEW" ? "new" : "quality"}>{b}</Badge>] : [];

  return (
    <div style={{ padding: "26px 0 40px" }}>
      <div style={{ padding: "0 var(--safe-x)" }}>
        <h1 style={{ font: "var(--text-display)", color: "var(--text-primary)", letterSpacing: "var(--ls-tight)", marginBottom: 22 }}>{title}</h1>
        <div style={{ display: "flex", gap: 32, alignItems: "flex-start" }}>
          {/* category filter column */}
          <div style={{ width: 300, flex: "0 0 auto", position: "sticky", top: 96, display: "flex", flexDirection: "column", gap: 4 }}>
            <p style={{ font: "var(--fw-bold) 11px/1 var(--font-body)", letterSpacing: "var(--ls-caps)", textTransform: "uppercase",
              color: "var(--text-tertiary)", margin: "0 0 8px", padding: "0 16px" }}>Categories</p>
            {cats.map((c, i) => (
              <CategoryRow key={c.name} name={c.name} count={c.count} kind={c.kind} smart={c.smart}
                active={i === catIdx} onClick={() => setCatIdx(i)} />
            ))}
          </div>
          {/* poster grid for the selected category */}
          <div key={catIdx} className="cat-panel" style={{ flex: 1, minWidth: 0 }}>
            <div style={{ display: "flex", alignItems: "baseline", gap: 10, marginBottom: 18 }}>
              <h2 style={{ font: "var(--text-h2)", color: "var(--text-primary)", letterSpacing: "var(--ls-tight)" }}>{cat.name}</h2>
              <span style={{ font: "var(--text-mono)", color: "var(--text-tertiary)" }}>{cat.count.toLocaleString()} titles</span>
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))", gap: 24 }}>
              {items.map((m, i) => (
                <div key={i} className="cat-tile" style={{ animationDelay: `${i * 30}ms` }}>
                  <PosterTile {...m} width="100%" badges={posterBadges(m.badge)} onClick={() => openDetail(m)} />
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
window.Browse = Browse;
