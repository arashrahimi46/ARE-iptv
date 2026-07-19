import React from "react";

/** Badge — tiny status overline. tone: live | new | quality | catchup | smart | neutral.
 *  Renders in wide-caps micro type. LIVE and SMART glow. */
export function Badge({ children, tone = "neutral", glow = false, style, ...rest }) {
  const tones = {
    live:    { bg: "var(--live)", fg: "#fff", glow: "var(--glow-live)" },
    new:     { bg: "var(--accent)", fg: "#fff", glow: "var(--glow-accent)" },
    quality: { bg: "var(--surface-glass)", fg: "var(--text-primary)", bd: "1px solid var(--border-strong)" },
    catchup: { bg: "rgba(34,197,94,0.16)", fg: "var(--green-400)", bd: "1px solid rgba(34,197,94,0.4)" },
    smart:   { bg: "rgba(139,92,246,0.16)", fg: "var(--violet-400)", bd: "1px solid rgba(139,92,246,0.45)", glow: "var(--glow-smart)" },
    neutral: { bg: "var(--surface-2)", fg: "var(--text-secondary)", bd: "1px solid var(--border-default)" },
  }[tone];

  return (
    <span
      style={{
        display: "inline-flex", alignItems: "center", gap: 5,
        height: 22, padding: "0 8px", borderRadius: "var(--r-xs)",
        font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)",
        letterSpacing: "var(--ls-caps)", textTransform: "uppercase",
        background: tones.bg, color: tones.fg, border: tones.bd || "1px solid transparent",
        boxShadow: glow ? tones.glow : "none",
        ...style,
      }}
      {...rest}
    >
      {tone === "live" && <span style={{ width: 6, height: 6, borderRadius: "50%", background: "#fff" }} />}
      {children}
    </span>
  );
}
