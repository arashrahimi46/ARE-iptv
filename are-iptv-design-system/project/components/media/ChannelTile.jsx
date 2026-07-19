import React from "react";
import { Icon } from "../core/Icon.jsx";

/** ChannelTile — logo-first live-TV tile. IPTV providers can't reliably serve
 *  stream previews, so the tile leads with the channel logo (mono initials chip
 *  when the playlist ships none) over a subtle glow, plus an in-card info panel:
 *  now-playing program with progress, next up, and quality info
 *  (resolution badge · codec · stream-health dot · catch-up). */
export function ChannelTile({
  channel, number, logo, now, next, progress = 45, health = "stable",
  quality, codec, catchup = false, fav = false,
  width = "var(--tile-land-w)", focused = false, onClick, style, ...rest
}) {
  const [hover, setHover] = React.useState(false);
  const isFocus = focused || hover;
  const healthColor = { stable: "var(--health-stable)", moderate: "var(--health-moderate)", poor: "var(--health-poor)" }[health];
  const healthLabel = { stable: "Stable stream", moderate: "Unstable stream", poor: "Poor stream" }[health];
  const initials = (channel || "?").replace(/\s?HD$/i, "").split(" ").slice(0, 2).map(w => w[0]).join("").toUpperCase();

  return (
    <div
      onClick={onClick} onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)}
      style={{ width, cursor: "pointer", flex: "0 0 auto", position: "relative", zIndex: isFocus ? 2 : 1,
        transform: isFocus ? "scale(var(--focus-scale))" : "scale(1)", transition: "var(--tr-focus)", ...style }}
      {...rest}
    >
      <div style={{ position: "relative", borderRadius: "var(--r-md)", overflow: "hidden",
        background: "linear-gradient(150deg, var(--surface-2), var(--ink-850))",
        boxShadow: isFocus ? "var(--shadow-tile-focus)" : "var(--shadow-tile)",
        outline: isFocus ? "3px solid var(--focus-ring)" : "1px solid var(--border-subtle)", outlineOffset: isFocus ? 0 : -1 }}>
        {/* logo zone — no stream preview; the logo IS the artwork */}
        <div style={{ position: "relative", aspectRatio: "16 / 8" }}>
          <div aria-hidden="true" style={{ position: "absolute", inset: 0,
            background: "radial-gradient(90% 130% at 50% 0%, rgba(59,130,246,0.14), transparent 62%)" }} />
          <div style={{ position: "absolute", top: 10, left: 10, display: "flex", alignItems: "center", gap: 6 }}>
            <span style={{ padding: "3px 7px", borderRadius: "var(--r-xs)", background: "var(--live)", color: "#fff",
              font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)", letterSpacing: "var(--ls-caps)" }}>LIVE</span>
            {catchup && <span title="Catch-up available" style={{ display: "inline-flex", alignItems: "center", gap: 4, padding: "3px 7px",
              borderRadius: "var(--r-xs)", background: "var(--surface-overlay)", backdropFilter: "blur(8px)",
              border: "1px solid var(--border-default)", color: "var(--text-secondary)",
              font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)", letterSpacing: "var(--ls-caps)" }}>
              <Icon name="clock" size={11} />CATCH-UP</span>}
          </div>
          <div style={{ position: "absolute", top: 10, right: 10, display: "flex", alignItems: "center", gap: 8 }}>
            {quality && <span style={{ padding: "3px 7px", borderRadius: "var(--r-xs)", background: "rgba(59,130,246,0.16)",
              border: "1px solid rgba(59,130,246,0.4)", color: "var(--accent-hover)",
              font: "var(--fw-bold) var(--fs-micro)/1 var(--font-mono)", letterSpacing: "var(--ls-caps)" }}>{quality}</span>}
            <span title={healthLabel} style={{ width: 9, height: 9, borderRadius: "50%", background: healthColor, boxShadow: `0 0 9px ${healthColor}` }} />
          </div>
          <div style={{ position: "absolute", inset: 0, display: "grid", placeItems: "center" }}>
            <div style={{ width: 62, height: 62, borderRadius: "var(--r-sm)", background: "var(--surface-overlay)", backdropFilter: "blur(8px)",
              display: "grid", placeItems: "center", border: "1px solid var(--border-default)",
              boxShadow: isFocus ? "0 6px 22px rgba(0,0,0,0.45), 0 0 0 1px rgba(59,130,246,0.35)" : "0 4px 14px rgba(0,0,0,0.35)",
              font: "var(--fw-bold) 21px/1 var(--font-display)", color: "var(--text-primary)", transition: "box-shadow var(--dur-fast)" }}>
              {logo ? <img src={logo} alt="" style={{ width: 46, height: 46, objectFit: "contain" }} /> : initials}
            </div>
          </div>
          {fav && <span title="Favorite" style={{ position: "absolute", bottom: 8, right: 11, color: "var(--live)", display: "inline-flex" }}>
            <Icon name="heart" size={14} /></span>}
        </div>
        {/* in-card info panel */}
        <div style={{ position: "relative", padding: "12px 12px 11px", background: "var(--surface-1)", borderTop: "1px solid var(--border-subtle)" }}>
          <div style={{ position: "absolute", top: -1, left: 0, right: 0, height: 3, background: "rgba(0,0,0,0.5)" }}>
            <div style={{ width: `${Math.max(0, Math.min(100, progress))}%`, height: "100%", background: "var(--accent)", boxShadow: "0 0 8px rgba(59,130,246,0.7)" }} /></div>
          <div style={{ display: "flex", alignItems: "baseline", gap: 8 }}>
            {number && <span style={{ font: "var(--text-mono)", color: "var(--text-tertiary)", flex: "0 0 auto" }}>{number}</span>}
            <span style={{ font: "var(--text-tile)", color: "var(--text-primary)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", minWidth: 0, flex: 1 }}>{channel}</span>
          </div>
          {now && <div style={{ marginTop: 4, font: "var(--text-caption)", color: "var(--text-secondary)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
            <span style={{ color: "var(--accent-hover)" }}>Now</span> · {now}</div>}
          <div style={{ marginTop: 2, display: "flex", alignItems: "baseline", gap: 10 }}>
            <span style={{ font: "var(--text-caption)", color: "var(--text-tertiary)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", minWidth: 0, flex: 1 }}>{next ? `Next · ${next}` : "\u00a0"}</span>
            {codec && <span style={{ font: "var(--text-mono)", color: "var(--text-tertiary)", flex: "0 0 auto" }}>{codec}</span>}
          </div>
        </div>
        {/* gloss + focus light sweep */}
        <div aria-hidden="true" style={{ position: "absolute", inset: 0, pointerEvents: "none", overflow: "hidden", borderRadius: "inherit" }}>
          <div style={{ position: "absolute", top: 0, left: 0, right: 0, height: "38%",
            background: "linear-gradient(180deg, rgba(255,255,255,0.08), rgba(255,255,255,0) 70%)", opacity: isFocus ? 1 : 0.55, transition: "opacity var(--dur-fast)" }} />
          <div style={{ position: "absolute", top: 0, bottom: 0, width: "45%", left: "-60%",
            transform: isFocus ? "translateX(340%) skewX(-14deg)" : "translateX(0) skewX(-14deg)",
            transition: isFocus ? "transform 720ms var(--ease-out)" : "none",
            background: "linear-gradient(100deg, transparent, rgba(255,255,255,0.10), transparent)" }} />
        </div>
      </div>
    </div>
  );
}
