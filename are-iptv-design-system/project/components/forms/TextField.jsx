import React from "react";

/** TextField — labeled input. `mono` renders the value in JetBrains Mono for
 *  URLs / Xtream params / EPG links so users can verify them character-by-char.
 *  Supports leading icon node, helper/error text, and a focused glow. */
export function TextField({
  label, value, placeholder, mono = false, type = "text", icon = null,
  helper, error, prefix, focused = false, onChange, style, ...rest
}) {
  const [focus, setFocus] = React.useState(false);
  const isFocus = focused || focus;
  return (
    <label style={{ display: "block", ...style }}>
      {label && <span style={{ display: "block", font: "var(--text-label)", color: "var(--text-secondary)", marginBottom: 8 }}>{label}</span>}
      <div style={{ display: "flex", alignItems: "center", gap: 10, height: 56, padding: "0 16px", borderRadius: "var(--r-md)",
        background: "var(--surface-1)",
        border: error ? "2px solid var(--danger)" : isFocus ? "2px solid var(--focus-ring)" : "1px solid var(--border-default)",
        boxShadow: isFocus ? "var(--focus-glow-tight)" : "none", transition: "var(--tr-focus), var(--tr-color)" }}>
        {icon && <span style={{ display: "inline-flex", color: "var(--text-tertiary)", width: 20, height: 20 }}>{icon}</span>}
        {prefix && <span style={{ font: "var(--text-mono)", color: "var(--text-tertiary)", fontSize: 14 }}>{prefix}</span>}
        <input
          type={type} value={value} placeholder={placeholder}
          onChange={onChange} onFocus={() => setFocus(true)} onBlur={() => setFocus(false)}
          style={{ flex: 1, minWidth: 0, background: "none", border: "none", outline: "none", color: "var(--text-primary)",
            font: mono ? "var(--fw-medium) var(--fs-body)/1 var(--font-mono)" : "var(--fw-medium) var(--fs-body)/1 var(--font-body)",
            letterSpacing: mono ? "0.01em" : "0" }}
          {...rest}
        />
      </div>
      {(helper || error) && <span style={{ display: "block", marginTop: 8, font: "var(--text-caption)",
        color: error ? "var(--danger)" : "var(--text-tertiary)" }}>{error || helper}</span>}
    </label>
  );
}
