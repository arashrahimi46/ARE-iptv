// ARE iptv — app shell: rail nav + screen router + overlays (detail, player, multi-view, onboarding).
function App() {
  const C = window.AREIptvDesignSystem_632b75;
  const { SidebarNav, IconButton, Icon } = C;
  const [theme, setThemeState] = React.useState("dark");
  const [booted, setBooted] = React.useState(false);
  const [screen, setScreen] = React.useState("home");
  const [detail, setDetail] = React.useState(null);
  const [player, setPlayer] = React.useState(null);
  const [multi, setMulti] = React.useState(false);

  const setTheme = (t) => { setThemeState(t); document.documentElement.setAttribute("data-theme", t); };
  React.useEffect(() => { document.documentElement.setAttribute("data-theme", theme); }, []);

  const navItems = [
    { id: "home", label: "Home", icon: "home" },
    { id: "live", label: "Live TV", icon: "tv" },
    { id: "guide", label: "TV Guide", icon: "layout-grid" },
    { id: "movies", label: "Movies", icon: "film" },
    { id: "series", label: "Series", icon: "clapperboard" },
    { id: "search", label: "Search", icon: "search" },
    { id: "favorites", label: "Favorites", icon: "heart" },
    { id: "settings", label: "Settings", icon: "settings" },
  ];

  const openDetail = (item) => setDetail(item || {});
  const openPlayer = (ch) => setPlayer(ch || {});
  const D = window.AREDATA;

  const Screen = () => {
    switch (screen) {
      case "home": return <window.Home openDetail={openDetail} openPlayer={openPlayer} />;
      case "live": return <window.Live openPlayer={openPlayer} />;
      case "guide": return <window.Guide openPlayer={openPlayer} />;
      case "movies": return <window.Browse key="movies" title="Movies" data={D.movies} openDetail={openDetail} />;
      case "series": return <window.Browse key="series" title="Series" data={D.series} openDetail={openDetail} />;
      case "search": return <window.Search openDetail={openDetail} openPlayer={openPlayer} />;
      case "favorites": return <window.Favorites openDetail={openDetail} openPlayer={openPlayer} />;
      case "settings": return <window.Settings theme={theme} setTheme={setTheme} />;
      default: return null;
    }
  };

  return (
    <div style={{ position: "relative", height: "100vh", width: "100%", overflow: "hidden", background: "var(--bg-base)", display: "flex" }}>
      <SidebarNav items={navItems} active={screen === "live" ? "live" : screen} onSelect={setScreen} />
      <div style={{ flex: 1, position: "relative", overflowY: "auto", overflowX: "hidden" }}>
        {/* top app bar */}
        <div style={{ position: "sticky", top: 0, zIndex: 10, display: "flex", alignItems: "center", gap: 12, padding: "18px var(--safe-x)",
          background: "linear-gradient(180deg, var(--bg-base) 55%, transparent)" }}>
          <div style={{ flex: 1 }} />
          <IconButton label="Multi-view" variant="solid" onClick={() => setMulti(true)}><Icon name="grid-2x2" /></IconButton>
          <IconButton label="Search" variant="solid" onClick={() => setScreen("search")}><Icon name="search" /></IconButton>
          <IconButton label="Add playlist" variant="solid" onClick={() => setBooted(false)}><Icon name="plus" /></IconButton>
          <div style={{ width: 44, height: 44, borderRadius: "50%", background: `center/cover url(${D.img("avatar", 100, 100)})`, border: "2px solid var(--border-strong)" }} />
        </div>
        <Screen />
      </div>

      {detail && <window.Detail item={detail} onClose={() => setDetail(null)} openPlayer={openPlayer} />}
      {player && <window.LivePlayer channel={player} onClose={() => setPlayer(null)} />}
      {multi && <window.MultiView onClose={() => setMulti(false)} />}
      {!booted && <window.Onboarding onDone={() => setBooted(true)} />}
    </div>
  );
}
window.App = App;
