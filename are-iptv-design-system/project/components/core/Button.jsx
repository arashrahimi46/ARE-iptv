import React from "react";

/**
 * Button — primary action control, built for D-pad focus.
 * Variants: primary | secondary | ghost | danger.
 * Sizes: sm | md | lg. Optional leading/trailing icon (pass a Lucide <i> or svg node).
 */
export function Button({
  children,
  variant = "primary",
  size = "md",
  icon = null,
  trailingIcon = null,
  full = false,
  disabled = false,
  focused = false,
  onClick,
  style,
  ...rest
}) {
  const sizes = {
    sm: { h: 40, px: 16, fs: "var(--fs-label)", gap: 8, ic: 18 },
    md: { h: 52, px: 22, fs: "var(--fs-body)", gap: 10, ic: 20 },
    lg: { h: 62, px: 30, fs: "var(--fs-h3)", gap: 12, ic: 24 },
  }[size];

  const variants = {
    primary: {
      background: "var(--accent)",
      color: "var(--accent-fg)",
      border: "1px solid transparent",
    },
    secondary: {
      background: "var(--surface-2)",
      color: "var(--text-primary)",
      border: "1px solid var(--border-default)",
    },
    ghost: {
      background: "transparent",
      color: "var(--text-secondary)",
      border: "1px solid transparent",
    },
    danger: {
      background: "var(--danger)",
      color: "#fff",
      border: "1px solid transparent",
    },
  }[variant];

  const [hover, setHover] = React.useState(false);
  const [press, setPress] = React.useState(false);
  const isFocus = focused || hover;

  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => { setHover(false); setPress(false); }}
      onMouseDown={() => setPress(true)}
      onMouseUp={() => setPress(false)}
      style={{
        display: "inline-flex", alignItems: "center", justifyContent: "center",
        gap: sizes.gap, height: sizes.h, padding: `0 ${sizes.px}px`,
        width: full ? "100%" : "auto",
        font: sizes.fs === "var(--fs-body)"
          ? "var(--fw-semibold) var(--fs-body)/1 var(--font-body)"
          : `var(--fw-semibold) ${sizes.fs}/1 var(--font-body)`,
        letterSpacing: "0.01em",
        borderRadius: "var(--r-md)",
        cursor: disabled ? "not-allowed" : "pointer",
        opacity: disabled ? 0.4 : 1,
        transform: press ? "scale(var(--press-scale))" : isFocus ? "scale(1.02)" : "scale(1)",
        boxShadow: isFocus && !disabled ? "var(--focus-glow-tight)" : "none",
        transition: "var(--tr-focus), var(--tr-color)",
        ...variants,
        ...style,
      }}
      {...rest}
    >
      {icon && <span style={{ display: "inline-flex", width: sizes.ic, height: sizes.ic }}>{icon}</span>}
      {children}
      {trailingIcon && <span style={{ display: "inline-flex", width: sizes.ic, height: sizes.ic }}>{trailingIcon}</span>}
    </button>
  );
}
