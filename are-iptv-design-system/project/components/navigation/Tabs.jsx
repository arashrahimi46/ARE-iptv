import React from "react";

/** Tabs — horizontal category switcher (browse, detail sections). Underline
 *  indicator slides under the active tab. items: [{ id, label }]. */
export function Tabs({ items = [], active, onSelect, style, ...rest }) {
  return (
    <div style={{ display: "flex", gap: 8, borderBottom: "1px solid var(--border-subtle)", ...style }} {...rest}>
      {items.map((it) => {
        const on = it.id === active;
        return (
          <button key={it.id} onClick={() => onSelect && onSelect(it.id)}
            style={{ position: "relative", background: "none", border: "none", cursor: "pointer",
              padding: "0 6px 16px", font: on ? "var(--fw-bold) var(--fs-h3)/1 var(--font-display)" : "var(--fw-medium) var(--fs-h3)/1 var(--font-display)",
              color: on ? "var(--text-primary)" : "var(--text-tertiary)", transition: "var(--tr-color)" }}
          >
            {it.label}
            <span style={{ position: "absolute", left: 6, right: 6, bottom: -1, height: 3, borderRadius: 3,
              background: on ? "var(--accent)" : "transparent", boxShadow: on ? "var(--glow-accent)" : "none",
              transition: "background var(--dur-fast)" }} />
          </button>
        );
      })}
    </div>
  );
}
