import React from "react";
import { Icon } from "../core/Icon.jsx";

const KIND = {
  live:     { icon: "radio",        unit: "channels" },
  tv:       { icon: "tv",           unit: "channels" },
  movies:   { icon: "film",         unit: "movies" },
  series:   { icon: "clapperboard", unit: "series" },
  guide:    { icon: "layout-grid",  unit: "channels" },
  catchup:  { icon: "clock",        unit: "programs" },
  favorites:{ icon: "heart",        unit: "items" },
  default:  { icon: "folder",       unit: "items" },
};

/** CategoryCard — a folder-style browse tile for an IPTV category (the grouping
 *  every content type shares: Live TV, Movies, Series, EPG). Artwork is optional
 *  and often absent in real playlists, so the no-image state is a first-class,
 *  clean folder tile (kind-icon watermark + label) — not a fabricated mosaic.
 *  When up to 4 preview images ARE supplied, it shows a 2×2 mosaic. Focusable
 *  (scale + accent glow) for D-pad. `compact` gives a denser tile for big walls. */
export function CategoryCard({
  name, count, kind = "default", posters = [], unit, smart = false, compact = false,
  width = "var(--tile-land-w)", focused = false, onClick, style, ...rest
}) {
  const [hover, setHover] = React.useState(false);
  const isFocus = focused || hover;
  const k = KIND[kind] || KIND.default;
  const noun = unit || k.unit;
  const tiles = (posters || []).filter(Boolean).slice(0, 4);
  const hasArt = tiles.length > 0;
  const iconSize = compact ? 16 : 18;

  return (
    <div
      onClick={onClick} onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)}
      style={{ width, cursor: "pointer", flex: "0 0 auto", position: "relative", zIndex: isFocus ? 2 : 1,
        transform: isFocus ? "scale(var(--focus-scale))" : "scale(1)", transition: "var(--tr-focus)", ...style }}
      {...rest}
    >
      <div style={{
        position: "relative", aspectRatio: compact ? "16 / 7" : "16 / 10", borderRadius: "var(--r-md)", overflow: "hidden",
        background: "linear-gradient(150deg, var(--surface-2), var(--ink-850))",
        boxShadow: isFocus ? "var(--shadow-tile-focus)" : "var(--shadow-tile)",
        outline: isFocus ? "3px solid var(--focus-ring)" : "1px solid var(--border-subtle)", outlineOffset: isFocus ? 0 : -1,
      }}>
        {hasArt ? (
          <>
            <div style={{ position: "absolute", inset: 0, display: "grid", gridTemplateColumns: "1fr 1fr", gridTemplateRows: "1fr 1fr", gap: 2 }}>
              {[0, 1, 2, 3].map(i => (
                <div key={i} style={{ background: tiles[i]
                  ? `center/cover no-repeat url(${tiles[i]})`
                  : "linear-gradient(150deg, var(--surface-3), var(--surface-1))" }} />
              ))}
            </div>
            <div style={{ position: "absolute", inset: 0, background: "var(--scrim-bottom)", opacity: 0.9 }} />
          </>
        ) : (
          /* artless folder tile — the common real-playlist case */
          <>
            <span aria-hidden="true" style={{ position: "absolute", right: -14, bottom: -18, color: "var(--text-primary)", opacity: 0.05,
              pointerEvents: "none" }}>
              <Icon name={k.icon} size={compact ? 96 : 132} strokeWidth={1.25} />
            </span>
            <span style={{ position: "absolute", top: 12, left: 14, display: "grid", placeItems: "center",
              width: compact ? 30 : 36, height: compact ? 30 : 36, borderRadius: "var(--r-sm)",
              background: "rgba(59,130,246,0.14)", border: "1px solid rgba(59,130,246,0.4)", color: "var(--accent)" }}>
              <Icon name={k.icon} size={compact ? 16 : 20} />
            </span>
          </>
        )}
        {smart && (
          <span style={{ position: "absolute", top: 10, right: 10, display: "inline-flex", alignItems: "center", height: 20, padding: "0 8px",
            borderRadius: "var(--r-pill)", background: "rgba(139,92,246,0.2)", border: "1px solid rgba(139,92,246,0.5)", color: "var(--violet-400)",
            font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)", letterSpacing: "var(--ls-caps)", boxShadow: "var(--glow-smart)" }}>SMART</span>
        )}
        {/* glossy top highlight + focus light-sweep */}
        <div aria-hidden="true" style={{ position: "absolute", inset: 0, overflow: "hidden", borderRadius: "inherit", pointerEvents: "none" }}>
          <div style={{ position: "absolute", top: 0, left: 0, right: 0, height: "55%",
            background: "linear-gradient(180deg, rgba(255,255,255,0.10), rgba(255,255,255,0) 70%)", opacity: isFocus ? 0.9 : 0.5, transition: "opacity var(--dur-fast)" }} />
          <div style={{ position: "absolute", top: 0, bottom: 0, width: "45%", left: "-60%",
            transform: isFocus ? "translateX(340%) skewX(-14deg)" : "translateX(0) skewX(-14deg)",
            transition: isFocus ? "transform 720ms var(--ease-out)" : "none",
            background: "linear-gradient(100deg, transparent, rgba(255,255,255,0.16), transparent)" }} />
        </div>
        {isFocus && <div aria-hidden="true" style={{ position: "absolute", inset: 0, borderRadius: "inherit", pointerEvents: "none",
          boxShadow: "inset 0 0 0 1px rgba(147,197,253,0.55), inset 0 0 24px rgba(59,130,246,0.28)" }} />}
        {/* label block */}
        <div style={{ position: "absolute", left: 14, right: 14, bottom: compact ? 10 : 12, display: "flex", alignItems: "center", gap: 10 }}>
          {hasArt && (
            <div style={{ width: 34, height: 34, borderRadius: "var(--r-sm)", flex: "0 0 auto",
              background: "var(--surface-overlay)", backdropFilter: "blur(8px)", border: "1px solid var(--border-default)",
              display: "grid", placeItems: "center", color: "var(--accent-hover)" }}>
              <Icon name={k.icon} size={iconSize} />
            </div>
          )}
          <div style={{ minWidth: 0 }}>
            <div style={{ font: `var(--fw-bold) ${compact ? "var(--fs-label)" : "var(--fs-title)"}/1.1 var(--font-display)`, color: "var(--text-primary)",
              letterSpacing: "var(--ls-tight)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{name}</div>
            {count != null && <div style={{ marginTop: 2, font: "var(--text-caption)", color: "var(--text-secondary)" }}>
              {typeof count === "number" ? count.toLocaleString() : count} {noun}</div>}
          </div>
        </div>
      </div>
    </div>
  );
}
