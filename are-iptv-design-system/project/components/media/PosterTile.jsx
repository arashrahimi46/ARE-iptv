import React from "react";

/** PosterTile — portrait VOD tile (movie/series). Focusable with scale + glow.
 *  Poster art via `image` (url). Falls back to an initials chip. Optional badges,
 *  rating, progress bar (for continue-watching), and title/meta below. */
export function PosterTile({
  title, meta, image, badges = [], rating, progress = null,
  width = "var(--tile-poster-w)", focused = false, onClick, style, ...rest
}) {
  const [hover, setHover] = React.useState(false);
  const isFocus = focused || hover;
  const initials = (title || "?").split(" ").slice(0, 2).map(w => w[0]).join("").toUpperCase();

  return (
    <div
      onClick={onClick} onMouseEnter={() => setHover(true)} onMouseLeave={() => setHover(false)}
      style={{ width, cursor: "pointer", flex: "0 0 auto",
        transform: isFocus ? "scale(var(--focus-scale))" : "scale(1)",
        transition: "var(--tr-focus)", zIndex: isFocus ? 2 : 1, position: "relative", ...style }}
      {...rest}
    >
      <div style={{
        position: "relative", aspectRatio: "2 / 3", borderRadius: "var(--r-md)", overflow: "hidden",
        background: image ? `center/cover no-repeat url(${image})` : "linear-gradient(150deg, var(--surface-3), var(--surface-1))",
        boxShadow: isFocus ? "var(--shadow-tile-focus)" : "var(--shadow-tile)",
        outline: isFocus ? "3px solid var(--focus-ring)" : "1px solid var(--border-subtle)",
        outlineOffset: isFocus ? "0px" : "-1px",
      }}>
        {!image && <div style={{ position: "absolute", inset: 0, display: "grid", placeItems: "center",
          font: "var(--fw-bold) 44px/1 var(--font-display)", color: "var(--text-tertiary)" }}>{initials}</div>}
        {badges.length > 0 && <div style={{ position: "absolute", top: 10, left: 10, display: "flex", gap: 6 }}>{badges}</div>}
        {rating != null && <div style={{ position: "absolute", top: 10, right: 10, display: "flex", alignItems: "center", gap: 4,
          padding: "3px 8px", borderRadius: "var(--r-pill)", background: "var(--surface-overlay)", backdropFilter: "blur(8px)",
          font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)", color: "var(--text-primary)" }}>
          <span style={{ color: "var(--amber-400)" }}>★</span>{rating}</div>}
        {progress != null && <div style={{ position: "absolute", left: 0, right: 0, bottom: 0, height: 4, background: "rgba(0,0,0,0.5)" }}>
          <div style={{ width: `${progress}%`, height: "100%", background: "var(--accent)" }} /></div>}
      </div>
      {title && <div style={{ marginTop: 10 }}>
        <div style={{ font: "var(--text-tile)", color: "var(--text-primary)", whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{title}</div>
        {meta && <div style={{ font: "var(--text-caption)", color: "var(--text-tertiary)", marginTop: 2 }}>{meta}</div>}
      </div>}
    </div>
  );
}
