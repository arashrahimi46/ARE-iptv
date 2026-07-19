import React from "react";

/** Hero — full-bleed featured banner for the top of Home / a detail page.
 *  Backdrop image with left + bottom scrims, badges, title, meta, synopsis,
 *  and an action row (pass Button children via `actions`). */
export function Hero({
  title, kicker, meta, synopsis, image, badges = [], actions = null, height = 520, style, ...rest
}) {
  return (
    <div style={{ position: "relative", height, borderRadius: "var(--r-xl)", overflow: "hidden",
      background: image ? `center 20%/cover no-repeat url(${image})` : "linear-gradient(120deg, var(--surface-2), var(--ink-900))", ...style }} {...rest}>
      <div style={{ position: "absolute", inset: 0, background: "var(--scrim-left)" }} />
      <div style={{ position: "absolute", inset: 0, background: "var(--scrim-bottom)" }} />
      <div style={{ position: "absolute", left: 0, bottom: 0, padding: "var(--sp-12)", maxWidth: 640 }}>
        {kicker && <div style={{ font: "var(--fw-bold) var(--fs-caption)/1 var(--font-body)", letterSpacing: "var(--ls-caps)",
          textTransform: "uppercase", color: "var(--accent-hover)", marginBottom: 14 }}>{kicker}</div>}
        {badges.length > 0 && <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>{badges}</div>}
        <h1 style={{ font: "var(--text-hero)", color: "#fff", letterSpacing: "var(--ls-tight)", textShadow: "0 2px 20px rgba(0,0,0,0.5)" }}>{title}</h1>
        {meta && <div style={{ marginTop: 14, font: "var(--text-label)", color: "var(--ink-100)" }}>{meta}</div>}
        {synopsis && <p style={{ marginTop: 14, font: "var(--text-body)", color: "var(--ink-100)", lineHeight: "var(--lh-normal)", maxWidth: 560,
          display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden" }}>{synopsis}</p>}
        {actions && <div style={{ display: "flex", gap: 14, marginTop: 26 }}>{actions}</div>}
      </div>
    </div>
  );
}
