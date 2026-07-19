import React from "react";

/** StepIndicator — the onboarding wizard progress header. steps: string[].
 *  Marks completed (check), current (accent), and upcoming steps with a
 *  connecting track. */
export function StepIndicator({ steps = [], current = 0, style, ...rest }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 0, ...style }} {...rest}>
      {steps.map((s, i) => {
        const done = i < current, active = i === current;
        return (
          <React.Fragment key={i}>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              <span style={{ width: 36, height: 36, borderRadius: "50%", display: "grid", placeItems: "center", flex: "0 0 auto",
                font: "var(--fw-bold) var(--fs-label)/1 var(--font-body)",
                background: active ? "var(--accent)" : done ? "var(--accent-wash)" : "var(--surface-2)",
                color: active ? "#fff" : done ? "var(--accent-hover)" : "var(--text-tertiary)",
                border: done || active ? "none" : "1px solid var(--border-default)",
                boxShadow: active ? "var(--glow-accent)" : "none" }}>{done ? "✓" : i + 1}</span>
              <span style={{ font: "var(--text-label)", color: active ? "var(--text-primary)" : "var(--text-tertiary)", whiteSpace: "nowrap" }}>{s}</span>
            </div>
            {i < steps.length - 1 && <span style={{ flex: 1, height: 2, margin: "0 16px", borderRadius: 2,
              background: i < current ? "var(--accent)" : "var(--border-default)", minWidth: 32 }} />}
          </React.Fragment>
        );
      })}
    </div>
  );
}
