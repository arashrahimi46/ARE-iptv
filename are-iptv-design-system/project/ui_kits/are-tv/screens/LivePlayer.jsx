// Live TV player — fullscreen video with the glass HUD, plus a "now playing"
// channel strip that slides up (mini-EPG) over the video.
function LivePlayer({ channel, onClose }) {
  const C = window.AREIptvDesignSystem_632b75, D = window.AREDATA;
  const { PlayerControls, IconButton, Icon, ChannelTile } = C;
  const [strip, setStrip] = React.useState(false);
  const ch = channel || D.liveNow[1];
  const initials = (ch.channel || "F1").replace(/\s?HD$/i, "").split(" ").slice(0, 2).map(w => w[0]).join("").toUpperCase();
  return (
    <div style={{ position: "absolute", inset: 0, background: "#000", zIndex: 40, overflow: "hidden" }}>
      <div style={{ position: "absolute", inset: 0, background: `center/cover no-repeat url(${D.wide("silverstone-live")})` }} />
      <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(6,7,10,.7), rgba(6,7,10,0) 45%)" }} />
      {/* top bar */}
      <div style={{ position: "absolute", top: 24, left: 28, right: 28, display: "flex", alignItems: "center", gap: 14, zIndex: 3 }}>
        <IconButton label="Back" variant="glass" onClick={onClose}><Icon name="arrow-left" /></IconButton>
        <div style={{ flex: 1 }} />
        <IconButton label="Channels" variant="glass" onClick={() => setStrip(s => !s)}><Icon name="list" /></IconButton>
        <IconButton label="Settings" variant="glass"><Icon name="settings" /></IconButton>
      </div>
      {/* mini channel strip */}
      <div style={{ position: "absolute", left: 0, right: 0, bottom: strip ? 220 : -260, transition: "bottom var(--dur-base) var(--ease-out)", zIndex: 3, padding: "0 28px" }}>
        <div style={{ display: "flex", gap: 20, overflow: "hidden", padding: "10px 0" }}>
          {D.liveNow.map((c, i) => <div key={i} style={{ width: 300, flex: "0 0 auto" }}><ChannelTile {...c} width="100%" /></div>)}
        </div>
      </div>
      {/* HUD */}
      <div style={{ position: "absolute", left: 28, right: 28, bottom: 24, zIndex: 3 }}>
        <PlayerControls title="British Grand Prix" subtitle={`${ch.channel || "Sky Sports F1"} · Now · Lap 34 of 52`}
          live playing position={62} buffered={84} elapsed="1:24:08" total="2:10:00" channelLogoInitials={initials} />
      </div>
    </div>
  );
}
window.LivePlayer = LivePlayer;
