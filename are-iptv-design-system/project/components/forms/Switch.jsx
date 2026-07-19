import React from "react";

/** Switch — on/off toggle (theme, parental lock, PiP, autoplay). Accent when on. */
export function Switch({ checked = false, onChange, disabled = false, focused = false, style, ...rest }) {
  const [hover, setHover] = React.useState(false);
  return (
    <button type="button" role="switch" aria-checked={checked} disabled={disabled}
      onClick={() => onChange && onChange(!checked)}
      onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)}
      style={{ width: 58, height: 34, borderRadius: "var(--r-pill)", border: "none", cursor: disabled ? "not-allowed" : "pointer",
        padding: 4, position: "relative", opacity: disabled ? 0.5 : 1,
        background: checked ? "var(--accent)" : "var(--surface-3)",
        boxShadow: (focused || hover) ? "var(--focus-glow-tight)" : "none",
        transition: "background var(--dur-fast), box-shadow var(--dur-fast)", ...style }}
      {...rest}
    >
      <span style={{ position: "absolute", top: 4, left: checked ? 28 : 4, width: 26, height: 26, borderRadius: "50%",
        background: "#fff", boxShadow: "0 2px 6px rgba(0,0,0,0.4)", transition: "left var(--dur-fast) var(--ease-emph)" }} />
    </button>
  );
}
