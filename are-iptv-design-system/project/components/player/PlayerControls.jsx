import React from "react";
import { Icon } from "../core/Icon.jsx";
import { IconButton } from "../core/IconButton.jsx";

/** PlayerControls — glass transport HUD overlaid on live video / VOD.
 *  Shows program title + now/next, a TimeShift-aware seek bar (buffered region
 *  vs live edge), transport buttons, and quick actions (audio, subtitles,
 *  aspect, PiP, multi-view). `live` toggles the LIVE-edge treatment. */
export function PlayerControls({
  title, subtitle, live = true, playing = true, position = 62, buffered = 80,
  elapsed = "20:28", total = "20:45", channelLogoInitials = "SKY", style, ...rest
}) {
  return (
    <div style={{ padding: "var(--sp-6)", borderRadius: "var(--r-xl)", background: "var(--surface-glass)",
      backdropFilter: "blur(var(--blur-glass))", border: "1px solid var(--border-default)", boxShadow: "var(--shadow-glass)",
      display: "flex", flexDirection: "column", gap: "var(--sp-4)", ...style }} {...rest}>
      {/* Program header */}
      <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
        <div style={{ width: 52, height: 52, borderRadius: "var(--r-sm)", background: "var(--surface-overlay)", display: "grid", placeItems: "center",
          border: "1px solid var(--border-default)", font: "var(--fw-bold) 15px/1 var(--font-display)", color: "var(--text-primary)", flex: "0 0 auto" }}>{channelLogoInitials}</div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            {live && <span style={{ display: "inline-flex", alignItems: "center", gap: 5, padding: "3px 8px", borderRadius: "var(--r-xs)",
              background: "var(--live)", color: "#fff", font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)", letterSpacing: "var(--ls-caps)" }}>
              <span style={{ width: 6, height: 6, borderRadius: "50%", background: "#fff" }} />LIVE</span>}
            <span style={{ font: "var(--text-h3)", color: "var(--text-primary)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{title}</span>
          </div>
          {subtitle && <div style={{ marginTop: 3, font: "var(--text-caption)", color: "var(--text-secondary)" }}>{subtitle}</div>}
        </div>
        <span style={{ font: "var(--text-mono)", color: "var(--text-secondary)", fontSize: 14 }}>{elapsed} <span style={{ color: "var(--text-tertiary)" }}>/ {total}</span></span>
      </div>

      {/* TimeShift seek bar */}
      <div style={{ position: "relative", height: 6, borderRadius: 3, background: "var(--surface-3)" }}>
        <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: `${buffered}%`, background: "var(--border-strong)", borderRadius: 3 }} />
        <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: `${position}%`, background: "var(--accent)", borderRadius: 3 }} />
        <div style={{ position: "absolute", left: `${position}%`, top: "50%", width: 16, height: 16, marginLeft: -8, marginTop: -8, borderRadius: "50%",
          background: "#fff", boxShadow: "var(--glow-accent)" }} />
        {live && <div style={{ position: "absolute", right: 0, top: "50%", transform: "translateY(-50%)", display: "flex", alignItems: "center", gap: 5 }}>
          <span style={{ width: 8, height: 8, borderRadius: "50%", background: "var(--live)" }} />
        </div>}
      </div>

      {/* Transport + quick actions */}
      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
        <IconButton label="Rewind" variant="glass"><Icon name="rewind" /></IconButton>
        <IconButton label={playing ? "Pause" : "Play"} variant="glass" size="lg" active>
          <Icon name={playing ? "pause" : "play"} size={28} />
        </IconButton>
        <IconButton label="Fast forward" variant="glass"><Icon name="fast-forward" /></IconButton>
        <div style={{ width: 1, height: 32, background: "var(--border-default)", margin: "0 6px" }} />
        <IconButton label="Jump to live" variant="glass"><Icon name="skip-forward" /></IconButton>
        <div style={{ flex: 1 }} />
        <IconButton label="Audio track" variant="glass"><Icon name="volume-2" /></IconButton>
        <IconButton label="Subtitles" variant="glass"><Icon name="captions" /></IconButton>
        <IconButton label="Multi-view" variant="glass"><Icon name="columns-2" /></IconButton>
        <IconButton label="Picture in picture" variant="glass"><Icon name="picture-in-picture-2" /></IconButton>
        <IconButton label="Open guide" variant="glass"><Icon name="layout-grid" /></IconButton>
      </div>
    </div>
  );
}
