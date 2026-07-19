import React from "react";
import { Icon } from "../core/Icon.jsx";

/** SidebarNav — the left rail nav. Collapsed to icons; expands to labels when
 *  the nav column has focus. items: [{ id, label, icon }]. Highlights `active`. */
export function SidebarNav({
  items = [], active, expanded = false, brand = "ARE", onSelect, style, ...rest
}) {
  const [hoverExpand, setHoverExpand] = React.useState(false);
  const open = expanded || hoverExpand;

  return (
    <nav
      onMouseEnter={() => setHoverExpand(true)} onMouseLeave={() => setHoverExpand(false)}
      style={{
        width: open ? "var(--sidebar-w-open)" : "var(--sidebar-w)",
        transition: "width var(--dur-base) var(--ease-out)",
        background: "linear-gradient(180deg, var(--surface-1), var(--bg-base))",
        borderRight: "1px solid var(--border-subtle)",
        display: "flex", flexDirection: "column", padding: "var(--sp-8) 0",
        height: "100%", flex: "0 0 auto", overflow: "hidden", ...style,
      }}
      {...rest}
    >
      <div style={{ display: "flex", alignItems: "center", gap: 12, padding: "0 26px", marginBottom: "var(--sp-10)", height: 44 }}>
        <div style={{ width: 40, height: 40, borderRadius: "var(--r-sm)", background: "var(--accent)", display: "grid", placeItems: "center",
          font: "var(--fw-bold) 18px/1 var(--font-display)", color: "#fff", flex: "0 0 auto", boxShadow: "var(--glow-accent)" }}>{brand[0]}</div>
        <span style={{ font: "var(--fw-bold) var(--fs-h3)/1 var(--font-display)", color: "var(--text-primary)", whiteSpace: "nowrap",
          opacity: open ? 1 : 0, transition: "opacity var(--dur-fast)" }}>ARE <span style={{ color: "var(--text-tertiary)", fontWeight: 500 }}>iptv</span></span>
      </div>

      <div style={{ display: "flex", flexDirection: "column", gap: 6, padding: "0 16px", flex: 1 }}>
        {items.map((it) => {
          const on = it.id === active;
          return (
            <button key={it.id} onClick={() => onSelect && onSelect(it.id)}
              style={{ display: "flex", alignItems: "center", gap: 16, height: 52, padding: "0 20px", borderRadius: "var(--r-md)",
                background: on ? "var(--accent-wash)" : "transparent", border: "none", cursor: "pointer", position: "relative",
                color: on ? "var(--accent-hover)" : "var(--text-tertiary)", transition: "var(--tr-color)" }}
              onMouseEnter={(e) => { if (!on) e.currentTarget.style.background = "var(--surface-2)"; }}
              onMouseLeave={(e) => { if (!on) e.currentTarget.style.background = "transparent"; }}
            >
              {on && <span style={{ position: "absolute", left: 0, top: 12, bottom: 12, width: 3, borderRadius: 3, background: "var(--accent)" }} />}
              <Icon name={it.icon} size={26} />
              <span style={{ font: "var(--text-label)", whiteSpace: "nowrap", opacity: open ? 1 : 0, transition: "opacity var(--dur-fast)",
                color: on ? "var(--text-primary)" : "var(--text-secondary)" }}>{it.label}</span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
