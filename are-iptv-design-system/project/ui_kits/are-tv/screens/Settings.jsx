// Settings — playback, appearance (theme), parental, playlists. Wired to app theme.
function Settings({ theme, setTheme }) {
  const C = window.AREIptvDesignSystem_632b75, D = window.AREDATA;
  const { Switch, Icon, Badge, Button, Chip } = C;
  const [ext, setExt] = React.useState("ExoPlayer");
  const Row = ({ icon, title, desc, control }) => (
    <div style={{ display: "flex", alignItems: "center", gap: 16, padding: "18px 20px", borderBottom: "1px solid var(--border-subtle)" }}>
      <div style={{ width: 42, height: 42, borderRadius: "var(--r-sm)", background: "var(--surface-2)", display: "grid", placeItems: "center", flex: "0 0 auto" }}><Icon name={icon} size={22} color="var(--text-secondary)" /></div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ font: "var(--text-label)", color: "var(--text-primary)" }}>{title}</div>
        {desc && <div style={{ font: "var(--text-caption)", color: "var(--text-tertiary)", marginTop: 3 }}>{desc}</div>}
      </div>
      {control}
    </div>
  );
  const Section = ({ title, children }) => (
    <div style={{ marginBottom: 34 }}>
      <h3 style={{ font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)", letterSpacing: "var(--ls-caps)", textTransform: "uppercase", color: "var(--text-tertiary)", marginBottom: 12 }}>{title}</h3>
      <div style={{ background: "var(--surface-1)", borderRadius: "var(--r-lg)", border: "1px solid var(--border-subtle)", overflow: "hidden" }}>{children}</div>
    </div>
  );
  return (
    <div style={{ padding: "26px var(--safe-x) 40px", maxWidth: 900 }}>
      <h1 style={{ font: "var(--text-display)", color: "var(--text-primary)", letterSpacing: "var(--ls-tight)", marginBottom: 30 }}>Settings</h1>

      <Section title="Appearance">
        <Row icon="moon" title="Dark theme" desc="Recommended for lean-back viewing." control={<Switch checked={theme === "dark"} onChange={v => setTheme(v ? "dark" : "light")} />} />
        <Row icon="zap" title="Reduce motion" desc="Softer focus animations." control={<Switch checked={false} onChange={() => {}} />} />
      </Section>

      <Section title="Playback">
        <Row icon="gauge" title="Hardware decoding" desc="H.264 / H.265 / AV1 · HDR10 · Dolby Vision" control={<Switch checked onChange={() => {}} />} />
        <Row icon="captions" title="Autoplay next episode" control={<Switch checked onChange={() => {}} />} />
        <Row icon="picture-in-picture-2" title="Picture-in-picture" desc="Keep playing while you browse." control={<Switch checked onChange={() => {}} />} />
        <Row icon="external-link" title="External player" desc="Hand off to another player per content type." control={
          <div style={{ display: "flex", gap: 8 }}>{["ExoPlayer", "VLC", "MX"].map(p => <Chip key={p} size="sm" selected={p === ext} onClick={() => setExt(p)}>{p}</Chip>)}</div>} />
      </Section>

      <Section title="Parental controls">
        <Row icon="lock" title="Lock adult categories" desc="Require a PIN to open restricted content." control={<Switch checked onChange={() => {}} />} />
        <Row icon="key-round" title="Change PIN" control={<Button variant="secondary" size="sm">Change</Button>} />
      </Section>

      <Section title="Playlists & sync">
        <Row icon="server" title="Living room · Xtream" desc="842 channels · EPG auto-matched" control={<Badge tone="catchup">ACTIVE</Badge>} />
        <Row icon="plus" title="Add playlist" desc="M3U link or Xtream Codes login." control={<Button variant="secondary" size="sm">Add</Button>} />
        <Row icon="hard-drive-download" title="Backup & restore" desc="Export favorites, history and settings to a file — no account needed." control={<Button variant="secondary" size="sm">Export</Button>} />
      </Section>

      <Section title="About & support">
        <div style={{ display: "flex", gap: 20, alignItems: "center", padding: 22, flexWrap: "wrap" }}>
          <div style={{ flex: 1, minWidth: 260 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 10 }}>
              <img src="../../assets/logo-mark.svg" width={40} height={40} alt="" style={{ borderRadius: "var(--r-sm)" }} />
              <div>
                <div style={{ font: "var(--fw-bold) var(--fs-title)/1 var(--font-display)", color: "var(--text-primary)" }}>ARE iptv</div>
                <div style={{ font: "var(--text-mono)", color: "var(--text-tertiary)", marginTop: 2 }}>v2.4.1 · free, no account</div>
              </div>
            </div>
            <p style={{ font: "var(--text-body)", color: "var(--text-secondary)", margin: 0, maxWidth: 440 }}>
              ARE iptv is free and always will be — no sign-up, no subscription, no ads. If it earns a place on your TV, you can chip in to keep it maintained.
            </p>
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 10, minWidth: 220 }}>
            <Button size="lg" icon={<Icon name="coffee" size={20} />} style={{ background: "#ffdd00", color: "#0a0a0a" }}>Buy me a coffee</Button>
            <Button variant="secondary" size="lg" icon={<Icon name="heart" size={18} />}>Donate with PayPal</Button>
          </div>
        </div>
        <Row icon="star" title="Rate ARE iptv" desc="Leave a review on the store." control={<Button variant="secondary" size="sm">Rate</Button>} />
        <Row icon="shield-check" title="Privacy" desc="Playlists and credentials stay on this device. Nothing is sent to us." control={<Icon name="chevron-right" size={18} color="var(--text-tertiary)" />} />
        <Row icon="file-text" title="Open-source licenses" control={<Icon name="chevron-right" size={18} color="var(--text-tertiary)" />} />
      </Section>
    </div>
  );
}
window.Settings = Settings;
