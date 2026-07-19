import React from "react";

/** Dialog — modal sheet on a scrim (confirm remove, parental PIN, add source).
 *  Glass panel, title, body, and an action row. Not a full focus-trap — a
 *  visual recreation for the design system. */
export function Dialog({ open = true, title, children, actions, width = 520, onClose, style, ...rest }) {
  if (!open) return null;
  return (
    <div onClick={onClose} style={{ position: "absolute", inset: 0, display: "grid", placeItems: "center",
      background: "rgba(6,7,10,0.6)", backdropFilter: "blur(var(--blur-scrim))", zIndex: 50 }}>
      <div onClick={(e) => e.stopPropagation()} role="dialog" aria-modal="true"
        style={{ width, maxWidth: "90%", background: "var(--surface-2)", borderRadius: "var(--r-xl)",
          border: "1px solid var(--border-default)", boxShadow: "var(--shadow-xl)", padding: "var(--sp-8)", ...style }} {...rest}>
        {title && <h3 style={{ font: "var(--text-h2)", color: "var(--text-primary)", letterSpacing: "var(--ls-tight)", marginBottom: "var(--sp-4)" }}>{title}</h3>}
        <div style={{ font: "var(--text-body)", color: "var(--text-secondary)", lineHeight: "var(--lh-normal)" }}>{children}</div>
        {actions && <div style={{ display: "flex", justifyContent: "flex-end", gap: 12, marginTop: "var(--sp-8)" }}>{actions}</div>}
      </div>
    </div>
  );
}
