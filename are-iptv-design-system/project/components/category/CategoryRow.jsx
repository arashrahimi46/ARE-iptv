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

/** CategoryRow — compact list row for the category filter column used down the
 *  left of Live TV, Movies, Series and the EPG guide. Kind icon, name, item
 *  count and a chevron. Active row gets a glossy accent-gradient wash, glow,
 *  solid accent icon chip and edge bar; focus/hover adds the accent ring and a
 *  light sweep. Optional SMART tag. */
export function CategoryRow({
  name, count, kind = "default", unit, smart = false, active = false,
  focused = false, onClick, style, ...rest
}) {
  const [hover, setHover] = React.useState(false);
  const isFocus = focused || hover;
  const k = KIND[kind] || KIND.default;
  const noun = unit || k.unit;

  return (
    <button
      onClick={onClick} onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)}
      style={{
        display: "flex", alignItems: "center", gap: 14, width: "100%", height: 56, padding: "0 16px", border: "none", font: "inherit",
        borderRadius: "var(--r-md)", cursor: "pointer", position: "relative", textAlign: "left", overflow: "hidden",
        background: active ? "linear-gradient(105deg, rgba(59,130,246,0.30), rgba(59,130,246,0.09) 75%)" : isFocus ? "var(--surface-2)" : "transparent",
        outline: isFocus ? "2px solid var(--focus-ring)" : active ? "1px solid rgba(59,130,246,0.45)" : "1px solid transparent", outlineOffset: -1,
        boxShadow: active ? "0 0 22px rgba(59,130,246,0.16), inset 0 1px 0 rgba(255,255,255,0.10)" : "none",
        transition: "var(--tr-color)", ...style,
      }}
      {...rest}
    >
      {active && <span style={{ position: "absolute", left: 0, top: 12, bottom: 12, width: 3, borderRadius: 3, background: "var(--accent)",
        boxShadow: "0 0 10px rgba(59,130,246,0.8)" }} />}
      {/* light sweep on focus/hover */}
      <span aria-hidden="true" style={{ position: "absolute", top: 0, bottom: 0, width: "40%", left: "-55%", pointerEvents: "none",
        transform: isFocus ? "translateX(370%) skewX(-14deg)" : "translateX(0) skewX(-14deg)",
        transition: isFocus ? "transform 640ms var(--ease-out)" : "none",
        background: "linear-gradient(100deg, transparent, rgba(255,255,255,0.10), transparent)" }} />
      <span style={{ display: "grid", placeItems: "center", width: 38, height: 38, borderRadius: "var(--r-sm)", flex: "0 0 auto",
        background: active ? "var(--accent)" : "var(--surface-3)", color: active ? "var(--accent-fg)" : "var(--text-tertiary)",
        boxShadow: active ? "var(--glow-accent)" : "none", transition: "var(--tr-color)" }}>
        <Icon name={k.icon} size={20} />
      </span>
      <span style={{ minWidth: 0, flex: 1 }}>
        <span style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <span style={{ font: "var(--text-label)", color: active ? "var(--text-primary)" : "var(--text-secondary)",
            whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{name}</span>
          {smart && <span style={{ display: "inline-flex", alignItems: "center", height: 16, padding: "0 6px", borderRadius: "var(--r-pill)",
            background: "rgba(139,92,246,0.18)", border: "1px solid rgba(139,92,246,0.45)", color: "var(--violet-400)",
            font: "var(--fw-bold) var(--fs-nano, 9px)/1 var(--font-body)", letterSpacing: "var(--ls-caps)", flex: "0 0 auto" }}>SMART</span>}
        </span>
      </span>
      {count != null && <span title={`${count} ${noun}`} style={{ flex: "0 0 auto", font: "var(--text-mono)",
        color: active ? "var(--text-secondary)" : "var(--text-tertiary)",
        background: active ? "rgba(255,255,255,0.10)" : "transparent", padding: active ? "3px 8px" : 0,
        borderRadius: "var(--r-pill)", transition: "var(--tr-color)" }}>
        {typeof count === "number" ? count.toLocaleString() : count}</span>}
      <Icon name="chevron-right" size={18} color="var(--text-quaternary, var(--text-tertiary))" style={{ opacity: isFocus || active ? 1 : 0.5 }} />
    </button>
  );
}
