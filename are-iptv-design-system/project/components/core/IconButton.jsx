import React from "react";

/** IconButton — square control for a single glyph. Used in player HUD, nav, toolbars.
 *  Variants: solid | glass | ghost. Optional active (accent) state. */
export function IconButton({
  children, label, variant = "ghost", size = "md",
  active = false, focused = false, disabled = false, onClick, style, ...rest
}) {
  const dims = { sm: 40, md: 52, lg: 64 }[size];
  const [hover, setHover] = React.useState(false);
  const [press, setPress] = React.useState(false);
  const isFocus = focused || hover;

  const variants = {
    solid: { background: "var(--surface-2)", color: "var(--text-primary)", border: "1px solid var(--border-default)" },
    glass: { background: "var(--surface-glass)", color: "#fff", border: "1px solid var(--border-default)", backdropFilter: "blur(var(--blur-glass))" },
    ghost: { background: "transparent", color: "var(--text-secondary)", border: "1px solid transparent" },
  }[variant];

  return (
    <button
      type="button" aria-label={label} title={label} disabled={disabled} onClick={onClick}
      onMouseEnter={() => setHover(true)} onMouseLeave={() => { setHover(false); setPress(false); }}
      onMouseDown={() => setPress(true)} onMouseUp={() => setPress(false)}
      style={{
        display: "inline-flex", alignItems: "center", justifyContent: "center",
        width: dims, height: dims, borderRadius: "var(--r-md)",
        cursor: disabled ? "not-allowed" : "pointer", opacity: disabled ? 0.4 : 1,
        color: active ? "var(--accent-fg)" : variants.color,
        background: active ? "var(--accent)" : variants.background,
        border: variants.border, backdropFilter: variants.backdropFilter,
        transform: press ? "scale(var(--press-scale))" : isFocus ? "scale(1.04)" : "scale(1)",
        boxShadow: isFocus && !disabled ? "var(--focus-glow-tight)" : "none",
        transition: "var(--tr-focus), var(--tr-color)",
        ...style,
      }}
      {...rest}
    >
      <span style={{ display: "inline-flex", width: size === "lg" ? 28 : 24, height: size === "lg" ? 28 : 24 }}>{children}</span>
    </button>
  );
}
