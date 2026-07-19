// Onboarding wizard — guided playlist add: Source → Credentials → EPG → Confirm.
// Supports both an M3U/URL playlist and Xtream Codes (host/user/pass) params.
function Onboarding({ onDone }) {
  const C = window.AREIptvDesignSystem_632b75, D = window.AREDATA;
  const { StepIndicator, TextField, Button, Switch, Icon, Badge } = C;
  const steps = ["Source", "Credentials", "EPG", "Confirm"];
  const [step, setStep] = React.useState(0);
  const [source, setSource] = React.useState("xtream"); // "m3u" | "xtream"
  const [epgAuto, setEpgAuto] = React.useState(true);

  const SourceCard = ({ id, icon, title, desc }) => {
    const on = source === id;
    return (
      <button onClick={() => setSource(id)} style={{ textAlign: "left", flex: 1, padding: 24, borderRadius: "var(--r-lg)", cursor: "pointer",
        background: on ? "var(--accent-wash)" : "var(--surface-1)", border: on ? "2px solid var(--accent)" : "1px solid var(--border-default)",
        boxShadow: on ? "var(--glow-accent)" : "none", transition: "var(--tr-color)" }}>
        <div style={{ width: 52, height: 52, borderRadius: "var(--r-md)", background: on ? "var(--accent)" : "var(--surface-3)", display: "grid", placeItems: "center", marginBottom: 16 }}>
          <Icon name={icon} size={26} color={on ? "#fff" : "var(--text-secondary)"} />
        </div>
        <div style={{ font: "var(--text-h3)", color: "var(--text-primary)", marginBottom: 6 }}>{title}</div>
        <div style={{ font: "var(--text-caption)", color: "var(--text-tertiary)", lineHeight: "var(--lh-normal)" }}>{desc}</div>
      </button>
    );
  };

  return (
    <div style={{ position: "absolute", inset: 0, zIndex: 60, background: "radial-gradient(1200px 600px at 20% -10%, rgba(59,130,246,.18), transparent), var(--bg-base)", overflowY: "auto" }}>
      <div style={{ maxWidth: 900, margin: "0 auto", padding: "56px 40px" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 8 }}>
          <div style={{ width: 44, height: 44, borderRadius: "var(--r-sm)", background: "var(--accent)", display: "grid", placeItems: "center", font: "var(--fw-bold) 20px/1 var(--font-display)", color: "#fff", boxShadow: "var(--glow-accent)" }}>A</div>
          <span style={{ font: "var(--fw-bold) var(--fs-h2)/1 var(--font-display)", color: "var(--text-primary)" }}>ARE <span style={{ color: "var(--text-tertiary)", fontWeight: 500 }}>iptv</span></span>
        </div>
        <h1 style={{ font: "var(--text-display)", color: "var(--text-primary)", letterSpacing: "var(--ls-tight)", margin: "18px 0 6px" }}>Add a playlist</h1>
        <p style={{ font: "var(--text-body)", color: "var(--text-secondary)", marginBottom: 36 }}>Bring your own IPTV subscription. ARE iptv never stores your credentials off-device.</p>

        <StepIndicator current={step} steps={steps} />

        <div style={{ marginTop: 40, minHeight: 260 }}>
          {step === 0 && <div style={{ display: "flex", gap: 20 }}>
            <SourceCard id="xtream" icon="server" title="Xtream Codes" desc="Enter your portal host, username and password. Best for live TV + VOD with automatic EPG." />
            <SourceCard id="m3u" icon="link" title="M3U / URL playlist" desc="Paste a single M3U or M3U-plus link. Add an XMLTV EPG URL separately if you have one." />
          </div>}

          {step === 1 && <div style={{ maxWidth: 640 }}>
            <TextField label="Portal name" placeholder="Living room" value="Living room" style={{ marginBottom: 20 }} />
            {source === "xtream" ? <>
              <TextField label="Server URL" mono prefix="http://" placeholder="portal.example.tv:8080" icon={<Icon name="server" size={18} />} style={{ marginBottom: 20 }} />
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20 }}>
                <TextField label="Username" mono value="are_user_01" />
                <TextField label="Password" mono type="password" value="••••••••••" />
              </div>
            </> : <>
              <TextField label="M3U playlist URL" mono prefix="http://" placeholder="host:8080/get.php?type=m3u_plus&output=ts" icon={<Icon name="link" size={18} />} />
            </>}
          </div>}

          {step === 2 && <div style={{ maxWidth: 640 }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "18px 20px", borderRadius: "var(--r-md)", background: "var(--surface-1)", border: "1px solid var(--border-default)", marginBottom: 18 }}>
              <div><div style={{ font: "var(--text-label)", color: "var(--text-primary)" }}>Auto-match EPG &amp; logos</div>
                <div style={{ font: "var(--text-caption)", color: "var(--text-tertiary)", marginTop: 4 }}>Fetch guide data, channel logos and posters automatically.</div></div>
              <Switch checked={epgAuto} onChange={setEpgAuto} />
            </div>
            <TextField label="XMLTV EPG URL (optional)" mono prefix="http://" placeholder="portal.example.tv/xmltv.php?username=…" disabled={epgAuto}
              helper={epgAuto ? "Disabled while auto-match is on." : "Provide your own guide source."} />
          </div>}

          {step === 3 && <div style={{ maxWidth: 640 }}>
            <div style={{ padding: 24, borderRadius: "var(--r-lg)", background: "var(--surface-1)", border: "1px solid var(--border-default)" }}>
              <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 20 }}>
                <Icon name="check-circle-2" size={24} color="var(--green-500)" />
                <span style={{ font: "var(--text-h3)", color: "var(--text-primary)" }}>Ready to add</span>
                <Badge tone="catchup" style={{ marginLeft: "auto" }}>842 CHANNELS</Badge>
              </div>
              {[["Portal", "Living room"], ["Source", source === "xtream" ? "Xtream Codes" : "M3U playlist"], ["Server", "portal.example.tv:8080"], ["EPG", epgAuto ? "Auto-matched" : "Custom XMLTV"], ["VOD", "1,204 movies · 386 series"]].map(([k, v]) => (
                <div key={k} style={{ display: "flex", justifyContent: "space-between", padding: "11px 0", borderBottom: "1px solid var(--border-subtle)" }}>
                  <span style={{ font: "var(--text-caption)", color: "var(--text-tertiary)" }}>{k}</span>
                  <span style={{ font: "var(--fw-medium) var(--fs-caption)/1 var(--font-mono)", color: "var(--text-primary)" }}>{v}</span>
                </div>
              ))}
            </div>
          </div>}
        </div>

        <div style={{ display: "flex", gap: 14, marginTop: 36 }}>
          {step > 0 && <Button variant="ghost" size="lg" onClick={() => setStep(step - 1)}>Back</Button>}
          <div style={{ flex: 1 }} />
          <Button variant="ghost" size="lg" onClick={onDone}>Skip for now</Button>
          {step < 3 ? <Button size="lg" trailingIcon={<Icon name="chevron-right" size={20} />} onClick={() => setStep(step + 1)}>Continue</Button>
            : <Button size="lg" icon={<Icon name="check" size={20} />} onClick={onDone}>Add playlist</Button>}
        </div>
      </div>
    </div>
  );
}
window.Onboarding = Onboarding;
