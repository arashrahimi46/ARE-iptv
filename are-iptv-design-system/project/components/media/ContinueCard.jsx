import React from "react";

/** ContinueCard — landscape "continue watching" tile with a still image, play
 *  overlay on focus, resume progress bar, and remaining-time label. */
export function ContinueCard({
  title, meta, image, progress = 60, remaining, width = 340, focused = false, onClick, style, ...rest
}) {
  const [hover, setHover] = React.useState(false);
  const isFocus = focused || hover;
  return (
    <div onClick={onClick} onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)}
      style={{ width, flex: "0 0 auto", cursor: "pointer", position: "relative", zIndex: isFocus ? 2 : 1,
        transform: isFocus ? "scale(var(--focus-scale))" : "scale(1)", transition: "var(--tr-focus)", ...style }} {...rest}>
      <div style={{ position: "relative", aspectRatio: "16 / 9", borderRadius: "var(--r-md)", overflow: "hidden",
        background: image ? `center/cover no-repeat url(${image})` : "linear-gradient(135deg, var(--surface-3), var(--ink-850))",
        boxShadow: isFocus ? "var(--shadow-tile-focus)" : "var(--shadow-tile)",
        outline: isFocus ? "3px solid var(--focus-ring)" : "1px solid var(--border-subtle)", outlineOffset: isFocus ? 0 : -1 }}>
        <div style={{ position: "absolute", inset: 0, background: "var(--scrim-bottom)", opacity: 0.85 }} />
        <div style={{ position: "absolute", inset: 0, display: "grid", placeItems: "center", opacity: isFocus ? 1 : 0, transition: "opacity var(--dur-fast)" }}>
          <div style={{ width: 56, height: 56, borderRadius: "50%", background: "var(--accent)", display: "grid", placeItems: "center",
            boxShadow: "var(--glow-accent)" }}>
            <span style={{ marginLeft: 3, borderStyle: "solid", borderWidth: "9px 0 9px 15px", borderColor: "transparent transparent transparent #fff" }} />
          </div>
        </div>
        {remaining && <div style={{ position: "absolute", top: 10, right: 10, padding: "3px 8px", borderRadius: "var(--r-pill)",
          background: "var(--surface-overlay)", backdropFilter: "blur(8px)", font: "var(--fw-semibold) var(--fs-micro)/1 var(--font-body)",
          color: "var(--text-primary)" }}>{remaining}</div>}
        <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, height: 4, background: "rgba(0,0,0,0.5)" }}>
          <div style={{ width: `${progress}%`, height: "100%", background: "var(--accent)" }} /></div>
      </div>
      <div style={{ marginTop: 9 }}>
        <div style={{ font: "var(--text-tile)", color: "var(--text-primary)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{title}</div>
        {meta && <div style={{ font: "var(--text-caption)", color: "var(--text-tertiary)", marginTop: 2 }}>{meta}</div>}
      </div>
    </div>
  );
}
