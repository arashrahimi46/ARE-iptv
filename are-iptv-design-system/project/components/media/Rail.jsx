import React from "react";

/** Rail — a titled horizontal row of tiles (the core Home building block).
 *  Header shows title, optional "smart" tag, and a see-all affordance.
 *  Children are tiles (PosterTile / ChannelTile / any node). */
export function Rail({ title, smart = false, seeAll = true, children, style, ...rest }) {
  return (
    <section style={{ marginBottom: "var(--sp-10)", ...style }} {...rest}>
      <div style={{ display: "flex", alignItems: "center", gap: 12, padding: `0 var(--safe-x)`, marginBottom: "var(--sp-4)" }}>
        <h2 style={{ font: "var(--text-h2)", color: "var(--text-primary)", letterSpacing: "var(--ls-tight)" }}>{title}</h2>
        {smart && (
          <span style={{ display: "inline-flex", alignItems: "center", gap: 5, height: 22, padding: "0 8px", borderRadius: "var(--r-pill)",
            background: "rgba(139,92,246,0.16)", border: "1px solid rgba(139,92,246,0.45)", color: "var(--violet-400)",
            font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)", letterSpacing: "var(--ls-caps)", boxShadow: "var(--glow-smart)" }}>SMART</span>
        )}
        {seeAll && (
          <button style={{ marginLeft: "auto", display: "inline-flex", alignItems: "center", gap: 4, background: "none", border: "none",
            color: "var(--text-tertiary)", font: "var(--text-label)", cursor: "pointer" }}>
            See all <span style={{ fontSize: 18 }}>›</span>
          </button>
        )}
      </div>
      <div style={{ display: "flex", gap: "var(--rail-gap)", overflowX: "auto", padding: `4px var(--safe-x) 12px`, scrollbarWidth: "none" }}>
        {children}
      </div>
    </section>
  );
}
