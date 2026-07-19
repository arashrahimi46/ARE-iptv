import React from "react";

/** GuideCell — one program block in the EPG grid. Width is set by the caller
 *  (proportional to duration). Shows time range + title; marks the live/now
 *  program with an accent edge, and catch-up availability. Long titles
 *  truncate with an ellipsis at rest and marquee-scroll on focus so the full
 *  title is readable in place; pair the grid with a focused-program info bar
 *  (see Guide screen) for full details. */
export function GuideCell({
  title, time, live = false, now = false, catchup = false, progress = 0,
  focused = false, width = 240, onClick, onFocusChange, style, ...rest
}) {
  const [hover, setHover] = React.useState(false);
  const isFocus = focused || hover;
  const tRef = React.useRef(null);
  const [shift, setShift] = React.useState(0);
  React.useEffect(() => {
    const el = tRef.current; if (!el || !el.parentElement) return;
    const d = el.offsetWidth - el.parentElement.clientWidth;
    setShift(isFocus && d > 0 ? d : 0);
  }, [isFocus, title, width]);
  const enter = () => { setHover(true); onFocusChange && onFocusChange(true); };
  const leave = () => { setHover(false); onFocusChange && onFocusChange(false); };
  return (
    <button onClick={onClick} onMouseEnter={enter} onMouseLeave={leave} aria-label={`${time} ${title}`}
      style={{ position: "relative", width, height: "var(--guide-row-h)", flex: "0 0 auto", textAlign: "left",
        padding: "0 16px", borderRadius: "var(--r-sm)", cursor: "pointer", overflow: "hidden",
        background: now ? "var(--accent-wash)" : "var(--surface-1)",
        border: isFocus ? "2px solid var(--focus-ring)" : "1px solid var(--border-subtle)",
        boxShadow: isFocus ? "var(--focus-glow-tight)" : "none",
        transform: isFocus ? "scale(1.015)" : "scale(1)", transition: "var(--tr-focus), var(--tr-color)", zIndex: isFocus ? 2 : 1, ...style }}
      {...rest}
    >
      {now && <span style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: 3, background: "var(--accent)" }} />}
      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
        <span style={{ font: "var(--text-mono)", color: now ? "var(--accent-hover)" : "var(--text-tertiary)", fontSize: 12 }}>{time}</span>
        {live && <span style={{ width: 6, height: 6, borderRadius: "50%", background: "var(--live)", boxShadow: "var(--glow-live)" }} />}
        {catchup && <span style={{ font: "var(--fw-bold) 10px/1 var(--font-body)", letterSpacing: "var(--ls-caps)", color: "var(--green-400)" }}>⟲</span>}
      </div>
      {/* title — ellipsis at rest; marquee-scrolls on focus when truncated */}
      <div style={{ marginTop: 4, overflow: "hidden", whiteSpace: "nowrap", textOverflow: shift ? "clip" : "ellipsis",
        font: "var(--fw-semibold) var(--fs-label)/1.15 var(--font-body)", color: "var(--text-primary)" }}>
        <span ref={tRef} style={{ display: "inline-block", transform: `translateX(${-shift}px)`,
          transition: shift ? `transform ${Math.max(1.4, shift / 36)}s linear 0.55s` : "none" }}>{title}</span>
      </div>
      {now && progress > 0 && <div style={{ position: "absolute", left: 0, bottom: 0, height: 2, width: `${progress}%`, background: "var(--accent)" }} />}
    </button>
  );
}
