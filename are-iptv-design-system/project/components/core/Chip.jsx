import React from "react";

/** Chip — filter / category pill. Toggles selected state. Optional leading dot or icon.
 *  Used for genre filters, category tabs on browse, quick-add source types. */
export function Chip({
  children, selected = false, focused = false, icon = null, dot = null,
  size = "md", onClick, style, ...rest
}) {
  const [hover, setHover] = React.useState(false);
  const isFocus = focused || hover;
  const h = { sm: 34, md: 42 }[size];

  return (
    <button
      type="button" onClick={onClick}
      onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)}
      style={{
        display: "inline-flex", alignItems: "center", gap: 8,
        height: h, padding: `0 ${size === "sm" ? 14 : 18}px`,
        borderRadius: "var(--r-pill)",
        font: "var(--fw-semibold) var(--fs-label)/1 var(--font-body)",
        cursor: "pointer", whiteSpace: "nowrap",
        color: selected ? "var(--accent-fg)" : "var(--text-secondary)",
        background: selected ? "var(--accent)" : "var(--surface-2)",
        border: selected ? "1px solid transparent" : "1px solid var(--border-default)",
        transform: isFocus ? "scale(1.04)" : "scale(1)",
        boxShadow: isFocus ? "var(--focus-glow-tight)" : "none",
        transition: "var(--tr-focus), var(--tr-color)",
        ...style,
      }}
      {...rest}
    >
      {dot && <span style={{ width: 8, height: 8, borderRadius: "50%", background: dot }} />}
      {icon && <span style={{ display: "inline-flex", width: 18, height: 18 }}>{icon}</span>}
      {children}
    </button>
  );
}
