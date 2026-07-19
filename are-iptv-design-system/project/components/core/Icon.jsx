import React from "react";

/** Icon — renders a Lucide glyph by name (kebab-case, e.g. "tv", "chevron-right").
 *  Requires the Lucide UMD script on the page (loaded by cards / UI kits from CDN).
 *  Stroke line icons, 2px, matched to the ARE iptv voice. */
export function Icon({ name, size = 24, color = "currentColor", strokeWidth = 2, style, ...rest }) {
  const ref = React.useRef(null);

  React.useEffect(() => {
    let stop = false;
    const paint = () => {
      const el = ref.current;
      if (!el || stop) return true;
      const lucide = window.lucide;
      if (!lucide) return false;
      // Build just this icon so we don't rescan the whole document.
      const toPascal = (n) => n.split("-").map(p => p[0].toUpperCase() + p.slice(1)).join("");
      const node = (lucide.icons && (lucide.icons[toPascal(name)] || lucide.icons[name]));
      try {
        if (node && lucide.createElement) {
          const svg = lucide.createElement(node);
          svg.setAttribute("width", "100%");
          svg.setAttribute("height", "100%");
          svg.setAttribute("stroke-width", strokeWidth);
          el.replaceChildren(svg);
          return true;
        }
        // Fallback: data-attr scan.
        el.innerHTML = `<i data-lucide="${name}" style="width:100%;height:100%"></i>`;
        lucide.createIcons && lucide.createIcons({ attrs: { "stroke-width": strokeWidth } });
        return true;
      } catch (e) { return false; }
    };
    if (!paint()) {
      const t = setInterval(() => { if (paint()) clearInterval(t); }, 60);
      return () => { stop = true; clearInterval(t); };
    }
  }, [name, strokeWidth]);

  return (
    <span ref={ref} aria-hidden="true" style={{ display: "inline-flex", alignItems: "center", justifyContent: "center",
      width: size, height: size, color, flex: "0 0 auto", ...style }} {...rest} />
  );
}
