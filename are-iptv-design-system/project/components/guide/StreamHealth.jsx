import React from "react";

/** StreamHealth — traffic-light indicator for stream reliability. A next-gen
 *  ARE iptv feature: green stable / amber moderate / red poor, with optional
 *  label and bitrate readout. */
export function StreamHealth({ level = "stable", label = true, bitrate, size = "md", style, ...rest }) {
  const map = {
    stable:   { c: "var(--health-stable)", t: "Stable" },
    moderate: { c: "var(--health-moderate)", t: "Moderate" },
    poor:     { c: "var(--health-poor)", t: "Poor" },
  }[level];
  const d = { sm: 8, md: 11, lg: 14 }[size];

  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: 8, ...style }} {...rest}>
      <span style={{ position: "relative", display: "inline-flex" }}>
        <span style={{ width: d, height: d, borderRadius: "50%", background: map.c, boxShadow: `0 0 12px ${map.c}` }} />
        {level === "stable" && <span style={{ position: "absolute", inset: -3, borderRadius: "50%", border: `2px solid ${map.c}`, opacity: 0.35 }} />}
      </span>
      {label && <span style={{ font: "var(--text-caption)", color: "var(--text-secondary)", fontWeight: 600 }}>{map.t}</span>}
      {bitrate && <span style={{ font: "var(--text-mono)", color: "var(--text-tertiary)", fontSize: 12 }}>{bitrate}</span>}
    </span>
  );
}
