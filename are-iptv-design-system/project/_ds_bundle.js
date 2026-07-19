/* @ds-bundle: {"format":4,"namespace":"AREIptvDesignSystem_632b75","components":[{"name":"CategoryCard","sourcePath":"components/category/CategoryCard.jsx"},{"name":"CategoryRow","sourcePath":"components/category/CategoryRow.jsx"},{"name":"Badge","sourcePath":"components/core/Badge.jsx"},{"name":"Button","sourcePath":"components/core/Button.jsx"},{"name":"Chip","sourcePath":"components/core/Chip.jsx"},{"name":"Icon","sourcePath":"components/core/Icon.jsx"},{"name":"IconButton","sourcePath":"components/core/IconButton.jsx"},{"name":"StepIndicator","sourcePath":"components/forms/StepIndicator.jsx"},{"name":"Switch","sourcePath":"components/forms/Switch.jsx"},{"name":"TextField","sourcePath":"components/forms/TextField.jsx"},{"name":"GuideCell","sourcePath":"components/guide/GuideCell.jsx"},{"name":"StreamHealth","sourcePath":"components/guide/StreamHealth.jsx"},{"name":"ChannelTile","sourcePath":"components/media/ChannelTile.jsx"},{"name":"ContinueCard","sourcePath":"components/media/ContinueCard.jsx"},{"name":"Hero","sourcePath":"components/media/Hero.jsx"},{"name":"PosterTile","sourcePath":"components/media/PosterTile.jsx"},{"name":"Rail","sourcePath":"components/media/Rail.jsx"},{"name":"SidebarNav","sourcePath":"components/navigation/SidebarNav.jsx"},{"name":"Tabs","sourcePath":"components/navigation/Tabs.jsx"},{"name":"Dialog","sourcePath":"components/overlay/Dialog.jsx"},{"name":"PlayerControls","sourcePath":"components/player/PlayerControls.jsx"}],"sourceHashes":{"components/category/CategoryCard.jsx":"82fe6124c30b","components/category/CategoryRow.jsx":"b958887e70ed","components/core/Badge.jsx":"19f777ebf3f9","components/core/Button.jsx":"8d2629184227","components/core/Chip.jsx":"ec78f0c1b850","components/core/Icon.jsx":"73a1c567ecbc","components/core/IconButton.jsx":"6519ad17103f","components/forms/StepIndicator.jsx":"65fb0b06128e","components/forms/Switch.jsx":"e06218946bb6","components/forms/TextField.jsx":"28ce3efadd29","components/guide/GuideCell.jsx":"06372d7613bd","components/guide/StreamHealth.jsx":"9eedf9a4ddca","components/media/ChannelTile.jsx":"6c660ea9970c","components/media/ContinueCard.jsx":"d2b141905603","components/media/Hero.jsx":"7392d50d6f4c","components/media/PosterTile.jsx":"77d496e02796","components/media/Rail.jsx":"47001d183f94","components/navigation/SidebarNav.jsx":"50b27f93d0a2","components/navigation/Tabs.jsx":"5997ef3ffcb8","components/overlay/Dialog.jsx":"c5f6bd08819a","components/player/PlayerControls.jsx":"be481d3b7223","ui_kits/are-tv/app.jsx":"8f0ae1177acc","ui_kits/are-tv/data.js":"9fc0264ad26d","ui_kits/are-tv/screens/Browse.jsx":"d9f66b8f39f9","ui_kits/are-tv/screens/Detail.jsx":"8c2ae8d21e4d","ui_kits/are-tv/screens/Favorites.jsx":"b502667f3adb","ui_kits/are-tv/screens/Guide.jsx":"dc952c0546ff","ui_kits/are-tv/screens/Home.jsx":"87bc17e43978","ui_kits/are-tv/screens/Live.jsx":"0b1eee176df7","ui_kits/are-tv/screens/LivePlayer.jsx":"4b44e424d40d","ui_kits/are-tv/screens/MultiView.jsx":"9c5b334c0273","ui_kits/are-tv/screens/Onboarding.jsx":"86b5c42f366c","ui_kits/are-tv/screens/Search.jsx":"b9991e878833","ui_kits/are-tv/screens/Settings.jsx":"4d88ffd12984"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.AREIptvDesignSystem_632b75 = window.AREIptvDesignSystem_632b75 || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/core/Badge.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** Badge — tiny status overline. tone: live | new | quality | catchup | smart | neutral.
 *  Renders in wide-caps micro type. LIVE and SMART glow. */
function Badge({
  children,
  tone = "neutral",
  glow = false,
  style,
  ...rest
}) {
  const tones = {
    live: {
      bg: "var(--live)",
      fg: "#fff",
      glow: "var(--glow-live)"
    },
    new: {
      bg: "var(--accent)",
      fg: "#fff",
      glow: "var(--glow-accent)"
    },
    quality: {
      bg: "var(--surface-glass)",
      fg: "var(--text-primary)",
      bd: "1px solid var(--border-strong)"
    },
    catchup: {
      bg: "rgba(34,197,94,0.16)",
      fg: "var(--green-400)",
      bd: "1px solid rgba(34,197,94,0.4)"
    },
    smart: {
      bg: "rgba(139,92,246,0.16)",
      fg: "var(--violet-400)",
      bd: "1px solid rgba(139,92,246,0.45)",
      glow: "var(--glow-smart)"
    },
    neutral: {
      bg: "var(--surface-2)",
      fg: "var(--text-secondary)",
      bd: "1px solid var(--border-default)"
    }
  }[tone];
  return /*#__PURE__*/React.createElement("span", _extends({
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 5,
      height: 22,
      padding: "0 8px",
      borderRadius: "var(--r-xs)",
      font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)",
      textTransform: "uppercase",
      background: tones.bg,
      color: tones.fg,
      border: tones.bd || "1px solid transparent",
      boxShadow: glow ? tones.glow : "none",
      ...style
    }
  }, rest), tone === "live" && /*#__PURE__*/React.createElement("span", {
    style: {
      width: 6,
      height: 6,
      borderRadius: "50%",
      background: "#fff"
    }
  }), children);
}
Object.assign(__ds_scope, { Badge });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Badge.jsx", error: String((e && e.message) || e) }); }

// components/core/Button.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Button — primary action control, built for D-pad focus.
 * Variants: primary | secondary | ghost | danger.
 * Sizes: sm | md | lg. Optional leading/trailing icon (pass a Lucide <i> or svg node).
 */
function Button({
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
    sm: {
      h: 40,
      px: 16,
      fs: "var(--fs-label)",
      gap: 8,
      ic: 18
    },
    md: {
      h: 52,
      px: 22,
      fs: "var(--fs-body)",
      gap: 10,
      ic: 20
    },
    lg: {
      h: 62,
      px: 30,
      fs: "var(--fs-h3)",
      gap: 12,
      ic: 24
    }
  }[size];
  const variants = {
    primary: {
      background: "var(--accent)",
      color: "var(--accent-fg)",
      border: "1px solid transparent"
    },
    secondary: {
      background: "var(--surface-2)",
      color: "var(--text-primary)",
      border: "1px solid var(--border-default)"
    },
    ghost: {
      background: "transparent",
      color: "var(--text-secondary)",
      border: "1px solid transparent"
    },
    danger: {
      background: "var(--danger)",
      color: "#fff",
      border: "1px solid transparent"
    }
  }[variant];
  const [hover, setHover] = React.useState(false);
  const [press, setPress] = React.useState(false);
  const isFocus = focused || hover;
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    disabled: disabled,
    onClick: onClick,
    onMouseEnter: () => setHover(true),
    onMouseLeave: () => {
      setHover(false);
      setPress(false);
    },
    onMouseDown: () => setPress(true),
    onMouseUp: () => setPress(false),
    style: {
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      gap: sizes.gap,
      height: sizes.h,
      padding: `0 ${sizes.px}px`,
      width: full ? "100%" : "auto",
      font: sizes.fs === "var(--fs-body)" ? "var(--fw-semibold) var(--fs-body)/1 var(--font-body)" : `var(--fw-semibold) ${sizes.fs}/1 var(--font-body)`,
      letterSpacing: "0.01em",
      borderRadius: "var(--r-md)",
      cursor: disabled ? "not-allowed" : "pointer",
      opacity: disabled ? 0.4 : 1,
      transform: press ? "scale(var(--press-scale))" : isFocus ? "scale(1.02)" : "scale(1)",
      boxShadow: isFocus && !disabled ? "var(--focus-glow-tight)" : "none",
      transition: "var(--tr-focus), var(--tr-color)",
      ...variants,
      ...style
    }
  }, rest), icon && /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      width: sizes.ic,
      height: sizes.ic
    }
  }, icon), children, trailingIcon && /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      width: sizes.ic,
      height: sizes.ic
    }
  }, trailingIcon));
}
Object.assign(__ds_scope, { Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Button.jsx", error: String((e && e.message) || e) }); }

// components/core/Chip.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** Chip — filter / category pill. Toggles selected state. Optional leading dot or icon.
 *  Used for genre filters, category tabs on browse, quick-add source types. */
function Chip({
  children,
  selected = false,
  focused = false,
  icon = null,
  dot = null,
  size = "md",
  onClick,
  style,
  ...rest
}) {
  const [hover, setHover] = React.useState(false);
  const isFocus = focused || hover;
  const h = {
    sm: 34,
    md: 42
  }[size];
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    onClick: onClick,
    onMouseEnter: () => setHover(true),
    onMouseLeave: () => setHover(false),
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 8,
      height: h,
      padding: `0 ${size === "sm" ? 14 : 18}px`,
      borderRadius: "var(--r-pill)",
      font: "var(--fw-semibold) var(--fs-label)/1 var(--font-body)",
      cursor: "pointer",
      whiteSpace: "nowrap",
      color: selected ? "var(--accent-fg)" : "var(--text-secondary)",
      background: selected ? "var(--accent)" : "var(--surface-2)",
      border: selected ? "1px solid transparent" : "1px solid var(--border-default)",
      transform: isFocus ? "scale(1.04)" : "scale(1)",
      boxShadow: isFocus ? "var(--focus-glow-tight)" : "none",
      transition: "var(--tr-focus), var(--tr-color)",
      ...style
    }
  }, rest), dot && /*#__PURE__*/React.createElement("span", {
    style: {
      width: 8,
      height: 8,
      borderRadius: "50%",
      background: dot
    }
  }), icon && /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      width: 18,
      height: 18
    }
  }, icon), children);
}
Object.assign(__ds_scope, { Chip });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Chip.jsx", error: String((e && e.message) || e) }); }

// components/core/Icon.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** Icon — renders a Lucide glyph by name (kebab-case, e.g. "tv", "chevron-right").
 *  Requires the Lucide UMD script on the page (loaded by cards / UI kits from CDN).
 *  Stroke line icons, 2px, matched to the ARE iptv voice. */
function Icon({
  name,
  size = 24,
  color = "currentColor",
  strokeWidth = 2,
  style,
  ...rest
}) {
  const ref = React.useRef(null);
  React.useEffect(() => {
    let stop = false;
    const paint = () => {
      const el = ref.current;
      if (!el || stop) return true;
      const lucide = window.lucide;
      if (!lucide) return false;
      // Build just this icon so we don't rescan the whole document.
      const toPascal = n => n.split("-").map(p => p[0].toUpperCase() + p.slice(1)).join("");
      const node = lucide.icons && (lucide.icons[toPascal(name)] || lucide.icons[name]);
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
        lucide.createIcons && lucide.createIcons({
          attrs: {
            "stroke-width": strokeWidth
          }
        });
        return true;
      } catch (e) {
        return false;
      }
    };
    if (!paint()) {
      const t = setInterval(() => {
        if (paint()) clearInterval(t);
      }, 60);
      return () => {
        stop = true;
        clearInterval(t);
      };
    }
  }, [name, strokeWidth]);
  return /*#__PURE__*/React.createElement("span", _extends({
    ref: ref,
    "aria-hidden": "true",
    style: {
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      width: size,
      height: size,
      color,
      flex: "0 0 auto",
      ...style
    }
  }, rest));
}
Object.assign(__ds_scope, { Icon });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Icon.jsx", error: String((e && e.message) || e) }); }

// components/category/CategoryCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const KIND = {
  live: {
    icon: "radio",
    unit: "channels"
  },
  tv: {
    icon: "tv",
    unit: "channels"
  },
  movies: {
    icon: "film",
    unit: "movies"
  },
  series: {
    icon: "clapperboard",
    unit: "series"
  },
  guide: {
    icon: "layout-grid",
    unit: "channels"
  },
  catchup: {
    icon: "clock",
    unit: "programs"
  },
  favorites: {
    icon: "heart",
    unit: "items"
  },
  default: {
    icon: "folder",
    unit: "items"
  }
};

/** CategoryCard — a folder-style browse tile for an IPTV category (the grouping
 *  every content type shares: Live TV, Movies, Series, EPG). Artwork is optional
 *  and often absent in real playlists, so the no-image state is a first-class,
 *  clean folder tile (kind-icon watermark + label) — not a fabricated mosaic.
 *  When up to 4 preview images ARE supplied, it shows a 2×2 mosaic. Focusable
 *  (scale + accent glow) for D-pad. `compact` gives a denser tile for big walls. */
function CategoryCard({
  name,
  count,
  kind = "default",
  posters = [],
  unit,
  smart = false,
  compact = false,
  width = "var(--tile-land-w)",
  focused = false,
  onClick,
  style,
  ...rest
}) {
  const [hover, setHover] = React.useState(false);
  const isFocus = focused || hover;
  const k = KIND[kind] || KIND.default;
  const noun = unit || k.unit;
  const tiles = (posters || []).filter(Boolean).slice(0, 4);
  const hasArt = tiles.length > 0;
  const iconSize = compact ? 16 : 18;
  return /*#__PURE__*/React.createElement("div", _extends({
    onClick: onClick,
    onMouseEnter: () => setHover(true),
    onMouseLeave: () => setHover(false),
    style: {
      width,
      cursor: "pointer",
      flex: "0 0 auto",
      position: "relative",
      zIndex: isFocus ? 2 : 1,
      transform: isFocus ? "scale(var(--focus-scale))" : "scale(1)",
      transition: "var(--tr-focus)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      aspectRatio: compact ? "16 / 7" : "16 / 10",
      borderRadius: "var(--r-md)",
      overflow: "hidden",
      background: "linear-gradient(150deg, var(--surface-2), var(--ink-850))",
      boxShadow: isFocus ? "var(--shadow-tile-focus)" : "var(--shadow-tile)",
      outline: isFocus ? "3px solid var(--focus-ring)" : "1px solid var(--border-subtle)",
      outlineOffset: isFocus ? 0 : -1
    }
  }, hasArt ? /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gridTemplateRows: "1fr 1fr",
      gap: 2
    }
  }, [0, 1, 2, 3].map(i => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      background: tiles[i] ? `center/cover no-repeat url(${tiles[i]})` : "linear-gradient(150deg, var(--surface-3), var(--surface-1))"
    }
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      background: "var(--scrim-bottom)",
      opacity: 0.9
    }
  })) :
  /*#__PURE__*/
  /* artless folder tile — the common real-playlist case */
  React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("span", {
    "aria-hidden": "true",
    style: {
      position: "absolute",
      right: -14,
      bottom: -18,
      color: "var(--text-primary)",
      opacity: 0.05,
      pointerEvents: "none"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: k.icon,
    size: compact ? 96 : 132,
    strokeWidth: 1.25
  })), /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      top: 12,
      left: 14,
      display: "grid",
      placeItems: "center",
      width: compact ? 30 : 36,
      height: compact ? 30 : 36,
      borderRadius: "var(--r-sm)",
      background: "rgba(59,130,246,0.14)",
      border: "1px solid rgba(59,130,246,0.4)",
      color: "var(--accent)"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: k.icon,
    size: compact ? 16 : 20
  }))), smart && /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      top: 10,
      right: 10,
      display: "inline-flex",
      alignItems: "center",
      height: 20,
      padding: "0 8px",
      borderRadius: "var(--r-pill)",
      background: "rgba(139,92,246,0.2)",
      border: "1px solid rgba(139,92,246,0.5)",
      color: "var(--violet-400)",
      font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)",
      boxShadow: "var(--glow-smart)"
    }
  }, "SMART"), /*#__PURE__*/React.createElement("div", {
    "aria-hidden": "true",
    style: {
      position: "absolute",
      inset: 0,
      overflow: "hidden",
      borderRadius: "inherit",
      pointerEvents: "none"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 0,
      left: 0,
      right: 0,
      height: "55%",
      background: "linear-gradient(180deg, rgba(255,255,255,0.10), rgba(255,255,255,0) 70%)",
      opacity: isFocus ? 0.9 : 0.5,
      transition: "opacity var(--dur-fast)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 0,
      bottom: 0,
      width: "45%",
      left: "-60%",
      transform: isFocus ? "translateX(340%) skewX(-14deg)" : "translateX(0) skewX(-14deg)",
      transition: isFocus ? "transform 720ms var(--ease-out)" : "none",
      background: "linear-gradient(100deg, transparent, rgba(255,255,255,0.16), transparent)"
    }
  })), isFocus && /*#__PURE__*/React.createElement("div", {
    "aria-hidden": "true",
    style: {
      position: "absolute",
      inset: 0,
      borderRadius: "inherit",
      pointerEvents: "none",
      boxShadow: "inset 0 0 0 1px rgba(147,197,253,0.55), inset 0 0 24px rgba(59,130,246,0.28)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      left: 14,
      right: 14,
      bottom: compact ? 10 : 12,
      display: "flex",
      alignItems: "center",
      gap: 10
    }
  }, hasArt && /*#__PURE__*/React.createElement("div", {
    style: {
      width: 34,
      height: 34,
      borderRadius: "var(--r-sm)",
      flex: "0 0 auto",
      background: "var(--surface-overlay)",
      backdropFilter: "blur(8px)",
      border: "1px solid var(--border-default)",
      display: "grid",
      placeItems: "center",
      color: "var(--accent-hover)"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: k.icon,
    size: iconSize
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: `var(--fw-bold) ${compact ? "var(--fs-label)" : "var(--fs-title)"}/1.1 var(--font-display)`,
      color: "var(--text-primary)",
      letterSpacing: "var(--ls-tight)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, name), count != null && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 2,
      font: "var(--text-caption)",
      color: "var(--text-secondary)"
    }
  }, typeof count === "number" ? count.toLocaleString() : count, " ", noun)))));
}
Object.assign(__ds_scope, { CategoryCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/category/CategoryCard.jsx", error: String((e && e.message) || e) }); }

// components/category/CategoryRow.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const KIND = {
  live: {
    icon: "radio",
    unit: "channels"
  },
  tv: {
    icon: "tv",
    unit: "channels"
  },
  movies: {
    icon: "film",
    unit: "movies"
  },
  series: {
    icon: "clapperboard",
    unit: "series"
  },
  guide: {
    icon: "layout-grid",
    unit: "channels"
  },
  catchup: {
    icon: "clock",
    unit: "programs"
  },
  favorites: {
    icon: "heart",
    unit: "items"
  },
  default: {
    icon: "folder",
    unit: "items"
  }
};

/** CategoryRow — compact list row for the category filter column used down the
 *  left of Live TV, Movies, Series and the EPG guide. Kind icon, name, item
 *  count and a chevron. Active row gets a glossy accent-gradient wash, glow,
 *  solid accent icon chip and edge bar; focus/hover adds the accent ring and a
 *  light sweep. Optional SMART tag. */
function CategoryRow({
  name,
  count,
  kind = "default",
  unit,
  smart = false,
  active = false,
  focused = false,
  onClick,
  style,
  ...rest
}) {
  const [hover, setHover] = React.useState(false);
  const isFocus = focused || hover;
  const k = KIND[kind] || KIND.default;
  const noun = unit || k.unit;
  return /*#__PURE__*/React.createElement("button", _extends({
    onClick: onClick,
    onMouseEnter: () => setHover(true),
    onMouseLeave: () => setHover(false),
    style: {
      display: "flex",
      alignItems: "center",
      gap: 14,
      width: "100%",
      height: 56,
      padding: "0 16px",
      border: "none",
      font: "inherit",
      borderRadius: "var(--r-md)",
      cursor: "pointer",
      position: "relative",
      textAlign: "left",
      overflow: "hidden",
      background: active ? "linear-gradient(105deg, rgba(59,130,246,0.30), rgba(59,130,246,0.09) 75%)" : isFocus ? "var(--surface-2)" : "transparent",
      outline: isFocus ? "2px solid var(--focus-ring)" : active ? "1px solid rgba(59,130,246,0.45)" : "1px solid transparent",
      outlineOffset: -1,
      boxShadow: active ? "0 0 22px rgba(59,130,246,0.16), inset 0 1px 0 rgba(255,255,255,0.10)" : "none",
      transition: "var(--tr-color)",
      ...style
    }
  }, rest), active && /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      left: 0,
      top: 12,
      bottom: 12,
      width: 3,
      borderRadius: 3,
      background: "var(--accent)",
      boxShadow: "0 0 10px rgba(59,130,246,0.8)"
    }
  }), /*#__PURE__*/React.createElement("span", {
    "aria-hidden": "true",
    style: {
      position: "absolute",
      top: 0,
      bottom: 0,
      width: "40%",
      left: "-55%",
      pointerEvents: "none",
      transform: isFocus ? "translateX(370%) skewX(-14deg)" : "translateX(0) skewX(-14deg)",
      transition: isFocus ? "transform 640ms var(--ease-out)" : "none",
      background: "linear-gradient(100deg, transparent, rgba(255,255,255,0.10), transparent)"
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      display: "grid",
      placeItems: "center",
      width: 38,
      height: 38,
      borderRadius: "var(--r-sm)",
      flex: "0 0 auto",
      background: active ? "var(--accent)" : "var(--surface-3)",
      color: active ? "var(--accent-fg)" : "var(--text-tertiary)",
      boxShadow: active ? "var(--glow-accent)" : "none",
      transition: "var(--tr-color)"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: k.icon,
    size: 20
  })), /*#__PURE__*/React.createElement("span", {
    style: {
      minWidth: 0,
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-label)",
      color: active ? "var(--text-primary)" : "var(--text-secondary)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, name), smart && /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      height: 16,
      padding: "0 6px",
      borderRadius: "var(--r-pill)",
      background: "rgba(139,92,246,0.18)",
      border: "1px solid rgba(139,92,246,0.45)",
      color: "var(--violet-400)",
      font: "var(--fw-bold) var(--fs-nano, 9px)/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)",
      flex: "0 0 auto"
    }
  }, "SMART"))), count != null && /*#__PURE__*/React.createElement("span", {
    title: `${count} ${noun}`,
    style: {
      flex: "0 0 auto",
      font: "var(--text-mono)",
      color: active ? "var(--text-secondary)" : "var(--text-tertiary)",
      background: active ? "rgba(255,255,255,0.10)" : "transparent",
      padding: active ? "3px 8px" : 0,
      borderRadius: "var(--r-pill)",
      transition: "var(--tr-color)"
    }
  }, typeof count === "number" ? count.toLocaleString() : count), /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "chevron-right",
    size: 18,
    color: "var(--text-quaternary, var(--text-tertiary))",
    style: {
      opacity: isFocus || active ? 1 : 0.5
    }
  }));
}
Object.assign(__ds_scope, { CategoryRow });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/category/CategoryRow.jsx", error: String((e && e.message) || e) }); }

// components/core/IconButton.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** IconButton — square control for a single glyph. Used in player HUD, nav, toolbars.
 *  Variants: solid | glass | ghost. Optional active (accent) state. */
function IconButton({
  children,
  label,
  variant = "ghost",
  size = "md",
  active = false,
  focused = false,
  disabled = false,
  onClick,
  style,
  ...rest
}) {
  const dims = {
    sm: 40,
    md: 52,
    lg: 64
  }[size];
  const [hover, setHover] = React.useState(false);
  const [press, setPress] = React.useState(false);
  const isFocus = focused || hover;
  const variants = {
    solid: {
      background: "var(--surface-2)",
      color: "var(--text-primary)",
      border: "1px solid var(--border-default)"
    },
    glass: {
      background: "var(--surface-glass)",
      color: "#fff",
      border: "1px solid var(--border-default)",
      backdropFilter: "blur(var(--blur-glass))"
    },
    ghost: {
      background: "transparent",
      color: "var(--text-secondary)",
      border: "1px solid transparent"
    }
  }[variant];
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    "aria-label": label,
    title: label,
    disabled: disabled,
    onClick: onClick,
    onMouseEnter: () => setHover(true),
    onMouseLeave: () => {
      setHover(false);
      setPress(false);
    },
    onMouseDown: () => setPress(true),
    onMouseUp: () => setPress(false),
    style: {
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      width: dims,
      height: dims,
      borderRadius: "var(--r-md)",
      cursor: disabled ? "not-allowed" : "pointer",
      opacity: disabled ? 0.4 : 1,
      color: active ? "var(--accent-fg)" : variants.color,
      background: active ? "var(--accent)" : variants.background,
      border: variants.border,
      backdropFilter: variants.backdropFilter,
      transform: press ? "scale(var(--press-scale))" : isFocus ? "scale(1.04)" : "scale(1)",
      boxShadow: isFocus && !disabled ? "var(--focus-glow-tight)" : "none",
      transition: "var(--tr-focus), var(--tr-color)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      width: size === "lg" ? 28 : 24,
      height: size === "lg" ? 28 : 24
    }
  }, children));
}
Object.assign(__ds_scope, { IconButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/IconButton.jsx", error: String((e && e.message) || e) }); }

// components/forms/StepIndicator.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** StepIndicator — the onboarding wizard progress header. steps: string[].
 *  Marks completed (check), current (accent), and upcoming steps with a
 *  connecting track. */
function StepIndicator({
  steps = [],
  current = 0,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      display: "flex",
      alignItems: "center",
      gap: 0,
      ...style
    }
  }, rest), steps.map((s, i) => {
    const done = i < current,
      active = i === current;
    return /*#__PURE__*/React.createElement(React.Fragment, {
      key: i
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 12
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        width: 36,
        height: 36,
        borderRadius: "50%",
        display: "grid",
        placeItems: "center",
        flex: "0 0 auto",
        font: "var(--fw-bold) var(--fs-label)/1 var(--font-body)",
        background: active ? "var(--accent)" : done ? "var(--accent-wash)" : "var(--surface-2)",
        color: active ? "#fff" : done ? "var(--accent-hover)" : "var(--text-tertiary)",
        border: done || active ? "none" : "1px solid var(--border-default)",
        boxShadow: active ? "var(--glow-accent)" : "none"
      }
    }, done ? "✓" : i + 1), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "var(--text-label)",
        color: active ? "var(--text-primary)" : "var(--text-tertiary)",
        whiteSpace: "nowrap"
      }
    }, s)), i < steps.length - 1 && /*#__PURE__*/React.createElement("span", {
      style: {
        flex: 1,
        height: 2,
        margin: "0 16px",
        borderRadius: 2,
        background: i < current ? "var(--accent)" : "var(--border-default)",
        minWidth: 32
      }
    }));
  }));
}
Object.assign(__ds_scope, { StepIndicator });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/StepIndicator.jsx", error: String((e && e.message) || e) }); }

// components/forms/Switch.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** Switch — on/off toggle (theme, parental lock, PiP, autoplay). Accent when on. */
function Switch({
  checked = false,
  onChange,
  disabled = false,
  focused = false,
  style,
  ...rest
}) {
  const [hover, setHover] = React.useState(false);
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    role: "switch",
    "aria-checked": checked,
    disabled: disabled,
    onClick: () => onChange && onChange(!checked),
    onMouseEnter: () => setHover(true),
    onMouseLeave: () => setHover(false),
    style: {
      width: 58,
      height: 34,
      borderRadius: "var(--r-pill)",
      border: "none",
      cursor: disabled ? "not-allowed" : "pointer",
      padding: 4,
      position: "relative",
      opacity: disabled ? 0.5 : 1,
      background: checked ? "var(--accent)" : "var(--surface-3)",
      boxShadow: focused || hover ? "var(--focus-glow-tight)" : "none",
      transition: "background var(--dur-fast), box-shadow var(--dur-fast)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      top: 4,
      left: checked ? 28 : 4,
      width: 26,
      height: 26,
      borderRadius: "50%",
      background: "#fff",
      boxShadow: "0 2px 6px rgba(0,0,0,0.4)",
      transition: "left var(--dur-fast) var(--ease-emph)"
    }
  }));
}
Object.assign(__ds_scope, { Switch });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Switch.jsx", error: String((e && e.message) || e) }); }

// components/forms/TextField.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** TextField — labeled input. `mono` renders the value in JetBrains Mono for
 *  URLs / Xtream params / EPG links so users can verify them character-by-char.
 *  Supports leading icon node, helper/error text, and a focused glow. */
function TextField({
  label,
  value,
  placeholder,
  mono = false,
  type = "text",
  icon = null,
  helper,
  error,
  prefix,
  focused = false,
  onChange,
  style,
  ...rest
}) {
  const [focus, setFocus] = React.useState(false);
  const isFocus = focused || focus;
  return /*#__PURE__*/React.createElement("label", {
    style: {
      display: "block",
      ...style
    }
  }, label && /*#__PURE__*/React.createElement("span", {
    style: {
      display: "block",
      font: "var(--text-label)",
      color: "var(--text-secondary)",
      marginBottom: 8
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      height: 56,
      padding: "0 16px",
      borderRadius: "var(--r-md)",
      background: "var(--surface-1)",
      border: error ? "2px solid var(--danger)" : isFocus ? "2px solid var(--focus-ring)" : "1px solid var(--border-default)",
      boxShadow: isFocus ? "var(--focus-glow-tight)" : "none",
      transition: "var(--tr-focus), var(--tr-color)"
    }
  }, icon && /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      color: "var(--text-tertiary)",
      width: 20,
      height: 20
    }
  }, icon), prefix && /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-mono)",
      color: "var(--text-tertiary)",
      fontSize: 14
    }
  }, prefix), /*#__PURE__*/React.createElement("input", _extends({
    type: type,
    value: value,
    placeholder: placeholder,
    onChange: onChange,
    onFocus: () => setFocus(true),
    onBlur: () => setFocus(false),
    style: {
      flex: 1,
      minWidth: 0,
      background: "none",
      border: "none",
      outline: "none",
      color: "var(--text-primary)",
      font: mono ? "var(--fw-medium) var(--fs-body)/1 var(--font-mono)" : "var(--fw-medium) var(--fs-body)/1 var(--font-body)",
      letterSpacing: mono ? "0.01em" : "0"
    }
  }, rest))), (helper || error) && /*#__PURE__*/React.createElement("span", {
    style: {
      display: "block",
      marginTop: 8,
      font: "var(--text-caption)",
      color: error ? "var(--danger)" : "var(--text-tertiary)"
    }
  }, error || helper));
}
Object.assign(__ds_scope, { TextField });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/TextField.jsx", error: String((e && e.message) || e) }); }

// components/guide/GuideCell.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** GuideCell — one program block in the EPG grid. Width is set by the caller
 *  (proportional to duration). Shows time range + title; marks the live/now
 *  program with an accent edge, and catch-up availability. Long titles
 *  truncate with an ellipsis at rest and marquee-scroll on focus so the full
 *  title is readable in place; pair the grid with a focused-program info bar
 *  (see Guide screen) for full details. */
function GuideCell({
  title,
  time,
  live = false,
  now = false,
  catchup = false,
  progress = 0,
  focused = false,
  width = 240,
  onClick,
  onFocusChange,
  style,
  ...rest
}) {
  const [hover, setHover] = React.useState(false);
  const isFocus = focused || hover;
  const tRef = React.useRef(null);
  const [shift, setShift] = React.useState(0);
  React.useEffect(() => {
    const el = tRef.current;
    if (!el || !el.parentElement) return;
    const d = el.offsetWidth - el.parentElement.clientWidth;
    setShift(isFocus && d > 0 ? d : 0);
  }, [isFocus, title, width]);
  const enter = () => {
    setHover(true);
    onFocusChange && onFocusChange(true);
  };
  const leave = () => {
    setHover(false);
    onFocusChange && onFocusChange(false);
  };
  return /*#__PURE__*/React.createElement("button", _extends({
    onClick: onClick,
    onMouseEnter: enter,
    onMouseLeave: leave,
    "aria-label": `${time} ${title}`,
    style: {
      position: "relative",
      width,
      height: "var(--guide-row-h)",
      flex: "0 0 auto",
      textAlign: "left",
      padding: "0 16px",
      borderRadius: "var(--r-sm)",
      cursor: "pointer",
      overflow: "hidden",
      background: now ? "var(--accent-wash)" : "var(--surface-1)",
      border: isFocus ? "2px solid var(--focus-ring)" : "1px solid var(--border-subtle)",
      boxShadow: isFocus ? "var(--focus-glow-tight)" : "none",
      transform: isFocus ? "scale(1.015)" : "scale(1)",
      transition: "var(--tr-focus), var(--tr-color)",
      zIndex: isFocus ? 2 : 1,
      ...style
    }
  }, rest), now && /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      left: 0,
      top: 0,
      bottom: 0,
      width: 3,
      background: "var(--accent)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-mono)",
      color: now ? "var(--accent-hover)" : "var(--text-tertiary)",
      fontSize: 12
    }
  }, time), live && /*#__PURE__*/React.createElement("span", {
    style: {
      width: 6,
      height: 6,
      borderRadius: "50%",
      background: "var(--live)",
      boxShadow: "var(--glow-live)"
    }
  }), catchup && /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--fw-bold) 10px/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)",
      color: "var(--green-400)"
    }
  }, "\u27F2")), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 4,
      overflow: "hidden",
      whiteSpace: "nowrap",
      textOverflow: shift ? "clip" : "ellipsis",
      font: "var(--fw-semibold) var(--fs-label)/1.15 var(--font-body)",
      color: "var(--text-primary)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    ref: tRef,
    style: {
      display: "inline-block",
      transform: `translateX(${-shift}px)`,
      transition: shift ? `transform ${Math.max(1.4, shift / 36)}s linear 0.55s` : "none"
    }
  }, title)), now && progress > 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      left: 0,
      bottom: 0,
      height: 2,
      width: `${progress}%`,
      background: "var(--accent)"
    }
  }));
}
Object.assign(__ds_scope, { GuideCell });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/guide/GuideCell.jsx", error: String((e && e.message) || e) }); }

// components/guide/StreamHealth.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** StreamHealth — traffic-light indicator for stream reliability. A next-gen
 *  ARE iptv feature: green stable / amber moderate / red poor, with optional
 *  label and bitrate readout. */
function StreamHealth({
  level = "stable",
  label = true,
  bitrate,
  size = "md",
  style,
  ...rest
}) {
  const map = {
    stable: {
      c: "var(--health-stable)",
      t: "Stable"
    },
    moderate: {
      c: "var(--health-moderate)",
      t: "Moderate"
    },
    poor: {
      c: "var(--health-poor)",
      t: "Poor"
    }
  }[level];
  const d = {
    sm: 8,
    md: 11,
    lg: 14
  }[size];
  return /*#__PURE__*/React.createElement("span", _extends({
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 8,
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("span", {
    style: {
      position: "relative",
      display: "inline-flex"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: d,
      height: d,
      borderRadius: "50%",
      background: map.c,
      boxShadow: `0 0 12px ${map.c}`
    }
  }), level === "stable" && /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      inset: -3,
      borderRadius: "50%",
      border: `2px solid ${map.c}`,
      opacity: 0.35
    }
  })), label && /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-caption)",
      color: "var(--text-secondary)",
      fontWeight: 600
    }
  }, map.t), bitrate && /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-mono)",
      color: "var(--text-tertiary)",
      fontSize: 12
    }
  }, bitrate));
}
Object.assign(__ds_scope, { StreamHealth });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/guide/StreamHealth.jsx", error: String((e && e.message) || e) }); }

// components/media/ChannelTile.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** ChannelTile — logo-first live-TV tile. IPTV providers can't reliably serve
 *  stream previews, so the tile leads with the channel logo (mono initials chip
 *  when the playlist ships none) over a subtle glow, plus an in-card info panel:
 *  now-playing program with progress, next up, and quality info
 *  (resolution badge · codec · stream-health dot · catch-up). */
function ChannelTile({
  channel,
  number,
  logo,
  now,
  next,
  progress = 45,
  health = "stable",
  quality,
  codec,
  catchup = false,
  fav = false,
  width = "var(--tile-land-w)",
  focused = false,
  onClick,
  style,
  ...rest
}) {
  const [hover, setHover] = React.useState(false);
  const isFocus = focused || hover;
  const healthColor = {
    stable: "var(--health-stable)",
    moderate: "var(--health-moderate)",
    poor: "var(--health-poor)"
  }[health];
  const healthLabel = {
    stable: "Stable stream",
    moderate: "Unstable stream",
    poor: "Poor stream"
  }[health];
  const initials = (channel || "?").replace(/\s?HD$/i, "").split(" ").slice(0, 2).map(w => w[0]).join("").toUpperCase();
  return /*#__PURE__*/React.createElement("div", _extends({
    onClick: onClick,
    onMouseEnter: () => setHover(true),
    onMouseLeave: () => setHover(false),
    style: {
      width,
      cursor: "pointer",
      flex: "0 0 auto",
      position: "relative",
      zIndex: isFocus ? 2 : 1,
      transform: isFocus ? "scale(var(--focus-scale))" : "scale(1)",
      transition: "var(--tr-focus)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      borderRadius: "var(--r-md)",
      overflow: "hidden",
      background: "linear-gradient(150deg, var(--surface-2), var(--ink-850))",
      boxShadow: isFocus ? "var(--shadow-tile-focus)" : "var(--shadow-tile)",
      outline: isFocus ? "3px solid var(--focus-ring)" : "1px solid var(--border-subtle)",
      outlineOffset: isFocus ? 0 : -1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      aspectRatio: "16 / 8"
    }
  }, /*#__PURE__*/React.createElement("div", {
    "aria-hidden": "true",
    style: {
      position: "absolute",
      inset: 0,
      background: "radial-gradient(90% 130% at 50% 0%, rgba(59,130,246,0.14), transparent 62%)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 10,
      left: 10,
      display: "flex",
      alignItems: "center",
      gap: 6
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      padding: "3px 7px",
      borderRadius: "var(--r-xs)",
      background: "var(--live)",
      color: "#fff",
      font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)"
    }
  }, "LIVE"), catchup && /*#__PURE__*/React.createElement("span", {
    title: "Catch-up available",
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 4,
      padding: "3px 7px",
      borderRadius: "var(--r-xs)",
      background: "var(--surface-overlay)",
      backdropFilter: "blur(8px)",
      border: "1px solid var(--border-default)",
      color: "var(--text-secondary)",
      font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "clock",
    size: 11
  }), "CATCH-UP")), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 10,
      right: 10,
      display: "flex",
      alignItems: "center",
      gap: 8
    }
  }, quality && /*#__PURE__*/React.createElement("span", {
    style: {
      padding: "3px 7px",
      borderRadius: "var(--r-xs)",
      background: "rgba(59,130,246,0.16)",
      border: "1px solid rgba(59,130,246,0.4)",
      color: "var(--accent-hover)",
      font: "var(--fw-bold) var(--fs-micro)/1 var(--font-mono)",
      letterSpacing: "var(--ls-caps)"
    }
  }, quality), /*#__PURE__*/React.createElement("span", {
    title: healthLabel,
    style: {
      width: 9,
      height: 9,
      borderRadius: "50%",
      background: healthColor,
      boxShadow: `0 0 9px ${healthColor}`
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      display: "grid",
      placeItems: "center"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 62,
      height: 62,
      borderRadius: "var(--r-sm)",
      background: "var(--surface-overlay)",
      backdropFilter: "blur(8px)",
      display: "grid",
      placeItems: "center",
      border: "1px solid var(--border-default)",
      boxShadow: isFocus ? "0 6px 22px rgba(0,0,0,0.45), 0 0 0 1px rgba(59,130,246,0.35)" : "0 4px 14px rgba(0,0,0,0.35)",
      font: "var(--fw-bold) 21px/1 var(--font-display)",
      color: "var(--text-primary)",
      transition: "box-shadow var(--dur-fast)"
    }
  }, logo ? /*#__PURE__*/React.createElement("img", {
    src: logo,
    alt: "",
    style: {
      width: 46,
      height: 46,
      objectFit: "contain"
    }
  }) : initials)), fav && /*#__PURE__*/React.createElement("span", {
    title: "Favorite",
    style: {
      position: "absolute",
      bottom: 8,
      right: 11,
      color: "var(--live)",
      display: "inline-flex"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "heart",
    size: 14
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      padding: "12px 12px 11px",
      background: "var(--surface-1)",
      borderTop: "1px solid var(--border-subtle)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: -1,
      left: 0,
      right: 0,
      height: 3,
      background: "rgba(0,0,0,0.5)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: `${Math.max(0, Math.min(100, progress))}%`,
      height: "100%",
      background: "var(--accent)",
      boxShadow: "0 0 8px rgba(59,130,246,0.7)"
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "baseline",
      gap: 8
    }
  }, number && /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-mono)",
      color: "var(--text-tertiary)",
      flex: "0 0 auto"
    }
  }, number), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-tile)",
      color: "var(--text-primary)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis",
      minWidth: 0,
      flex: 1
    }
  }, channel)), now && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 4,
      font: "var(--text-caption)",
      color: "var(--text-secondary)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--accent-hover)"
    }
  }, "Now"), " \xB7 ", now), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 2,
      display: "flex",
      alignItems: "baseline",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-caption)",
      color: "var(--text-tertiary)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis",
      minWidth: 0,
      flex: 1
    }
  }, next ? `Next · ${next}` : "\u00a0"), codec && /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-mono)",
      color: "var(--text-tertiary)",
      flex: "0 0 auto"
    }
  }, codec))), /*#__PURE__*/React.createElement("div", {
    "aria-hidden": "true",
    style: {
      position: "absolute",
      inset: 0,
      pointerEvents: "none",
      overflow: "hidden",
      borderRadius: "inherit"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 0,
      left: 0,
      right: 0,
      height: "38%",
      background: "linear-gradient(180deg, rgba(255,255,255,0.08), rgba(255,255,255,0) 70%)",
      opacity: isFocus ? 1 : 0.55,
      transition: "opacity var(--dur-fast)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 0,
      bottom: 0,
      width: "45%",
      left: "-60%",
      transform: isFocus ? "translateX(340%) skewX(-14deg)" : "translateX(0) skewX(-14deg)",
      transition: isFocus ? "transform 720ms var(--ease-out)" : "none",
      background: "linear-gradient(100deg, transparent, rgba(255,255,255,0.10), transparent)"
    }
  }))));
}
Object.assign(__ds_scope, { ChannelTile });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/media/ChannelTile.jsx", error: String((e && e.message) || e) }); }

// components/media/ContinueCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** ContinueCard — landscape "continue watching" tile with a still image, play
 *  overlay on focus, resume progress bar, and remaining-time label. */
function ContinueCard({
  title,
  meta,
  image,
  progress = 60,
  remaining,
  width = 340,
  focused = false,
  onClick,
  style,
  ...rest
}) {
  const [hover, setHover] = React.useState(false);
  const isFocus = focused || hover;
  return /*#__PURE__*/React.createElement("div", _extends({
    onClick: onClick,
    onMouseEnter: () => setHover(true),
    onMouseLeave: () => setHover(false),
    style: {
      width,
      flex: "0 0 auto",
      cursor: "pointer",
      position: "relative",
      zIndex: isFocus ? 2 : 1,
      transform: isFocus ? "scale(var(--focus-scale))" : "scale(1)",
      transition: "var(--tr-focus)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      aspectRatio: "16 / 9",
      borderRadius: "var(--r-md)",
      overflow: "hidden",
      background: image ? `center/cover no-repeat url(${image})` : "linear-gradient(135deg, var(--surface-3), var(--ink-850))",
      boxShadow: isFocus ? "var(--shadow-tile-focus)" : "var(--shadow-tile)",
      outline: isFocus ? "3px solid var(--focus-ring)" : "1px solid var(--border-subtle)",
      outlineOffset: isFocus ? 0 : -1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      background: "var(--scrim-bottom)",
      opacity: 0.85
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      display: "grid",
      placeItems: "center",
      opacity: isFocus ? 1 : 0,
      transition: "opacity var(--dur-fast)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 56,
      height: 56,
      borderRadius: "50%",
      background: "var(--accent)",
      display: "grid",
      placeItems: "center",
      boxShadow: "var(--glow-accent)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      marginLeft: 3,
      borderStyle: "solid",
      borderWidth: "9px 0 9px 15px",
      borderColor: "transparent transparent transparent #fff"
    }
  }))), remaining && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 10,
      right: 10,
      padding: "3px 8px",
      borderRadius: "var(--r-pill)",
      background: "var(--surface-overlay)",
      backdropFilter: "blur(8px)",
      font: "var(--fw-semibold) var(--fs-micro)/1 var(--font-body)",
      color: "var(--text-primary)"
    }
  }, remaining), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      left: 0,
      right: 0,
      bottom: 0,
      height: 4,
      background: "rgba(0,0,0,0.5)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: `${progress}%`,
      height: "100%",
      background: "var(--accent)"
    }
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 9
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--text-tile)",
      color: "var(--text-primary)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, title), meta && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--text-caption)",
      color: "var(--text-tertiary)",
      marginTop: 2
    }
  }, meta)));
}
Object.assign(__ds_scope, { ContinueCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/media/ContinueCard.jsx", error: String((e && e.message) || e) }); }

// components/media/Hero.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** Hero — full-bleed featured banner for the top of Home / a detail page.
 *  Backdrop image with left + bottom scrims, badges, title, meta, synopsis,
 *  and an action row (pass Button children via `actions`). */
function Hero({
  title,
  kicker,
  meta,
  synopsis,
  image,
  badges = [],
  actions = null,
  height = 520,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      position: "relative",
      height,
      borderRadius: "var(--r-xl)",
      overflow: "hidden",
      background: image ? `center 20%/cover no-repeat url(${image})` : "linear-gradient(120deg, var(--surface-2), var(--ink-900))",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      background: "var(--scrim-left)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      background: "var(--scrim-bottom)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      left: 0,
      bottom: 0,
      padding: "var(--sp-12)",
      maxWidth: 640
    }
  }, kicker && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--fw-bold) var(--fs-caption)/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)",
      textTransform: "uppercase",
      color: "var(--accent-hover)",
      marginBottom: 14
    }
  }, kicker), badges.length > 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      marginBottom: 16
    }
  }, badges), /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "var(--text-hero)",
      color: "#fff",
      letterSpacing: "var(--ls-tight)",
      textShadow: "0 2px 20px rgba(0,0,0,0.5)"
    }
  }, title), meta && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14,
      font: "var(--text-label)",
      color: "var(--ink-100)"
    }
  }, meta), synopsis && /*#__PURE__*/React.createElement("p", {
    style: {
      marginTop: 14,
      font: "var(--text-body)",
      color: "var(--ink-100)",
      lineHeight: "var(--lh-normal)",
      maxWidth: 560,
      display: "-webkit-box",
      WebkitLineClamp: 2,
      WebkitBoxOrient: "vertical",
      overflow: "hidden"
    }
  }, synopsis), actions && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 14,
      marginTop: 26
    }
  }, actions)));
}
Object.assign(__ds_scope, { Hero });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/media/Hero.jsx", error: String((e && e.message) || e) }); }

// components/media/PosterTile.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** PosterTile — portrait VOD tile (movie/series). Focusable with scale + glow.
 *  Poster art via `image` (url). Falls back to an initials chip. Optional badges,
 *  rating, progress bar (for continue-watching), and title/meta below. */
function PosterTile({
  title,
  meta,
  image,
  badges = [],
  rating,
  progress = null,
  width = "var(--tile-poster-w)",
  focused = false,
  onClick,
  style,
  ...rest
}) {
  const [hover, setHover] = React.useState(false);
  const isFocus = focused || hover;
  const initials = (title || "?").split(" ").slice(0, 2).map(w => w[0]).join("").toUpperCase();
  return /*#__PURE__*/React.createElement("div", _extends({
    onClick: onClick,
    onMouseEnter: () => setHover(true),
    onMouseLeave: () => setHover(false),
    style: {
      width,
      cursor: "pointer",
      flex: "0 0 auto",
      transform: isFocus ? "scale(var(--focus-scale))" : "scale(1)",
      transition: "var(--tr-focus)",
      zIndex: isFocus ? 2 : 1,
      position: "relative",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      aspectRatio: "2 / 3",
      borderRadius: "var(--r-md)",
      overflow: "hidden",
      background: image ? `center/cover no-repeat url(${image})` : "linear-gradient(150deg, var(--surface-3), var(--surface-1))",
      boxShadow: isFocus ? "var(--shadow-tile-focus)" : "var(--shadow-tile)",
      outline: isFocus ? "3px solid var(--focus-ring)" : "1px solid var(--border-subtle)",
      outlineOffset: isFocus ? "0px" : "-1px"
    }
  }, !image && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      display: "grid",
      placeItems: "center",
      font: "var(--fw-bold) 44px/1 var(--font-display)",
      color: "var(--text-tertiary)"
    }
  }, initials), badges.length > 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 10,
      left: 10,
      display: "flex",
      gap: 6
    }
  }, badges), rating != null && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 10,
      right: 10,
      display: "flex",
      alignItems: "center",
      gap: 4,
      padding: "3px 8px",
      borderRadius: "var(--r-pill)",
      background: "var(--surface-overlay)",
      backdropFilter: "blur(8px)",
      font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)",
      color: "var(--text-primary)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--amber-400)"
    }
  }, "\u2605"), rating), progress != null && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      left: 0,
      right: 0,
      bottom: 0,
      height: 4,
      background: "rgba(0,0,0,0.5)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: `${progress}%`,
      height: "100%",
      background: "var(--accent)"
    }
  }))), title && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--text-tile)",
      color: "var(--text-primary)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, title), meta && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--text-caption)",
      color: "var(--text-tertiary)",
      marginTop: 2
    }
  }, meta)));
}
Object.assign(__ds_scope, { PosterTile });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/media/PosterTile.jsx", error: String((e && e.message) || e) }); }

// components/media/Rail.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** Rail — a titled horizontal row of tiles (the core Home building block).
 *  Header shows title, optional "smart" tag, and a see-all affordance.
 *  Children are tiles (PosterTile / ChannelTile / any node). */
function Rail({
  title,
  smart = false,
  seeAll = true,
  children,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("section", _extends({
    style: {
      marginBottom: "var(--sp-10)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12,
      padding: `0 var(--safe-x)`,
      marginBottom: "var(--sp-4)"
    }
  }, /*#__PURE__*/React.createElement("h2", {
    style: {
      font: "var(--text-h2)",
      color: "var(--text-primary)",
      letterSpacing: "var(--ls-tight)"
    }
  }, title), smart && /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 5,
      height: 22,
      padding: "0 8px",
      borderRadius: "var(--r-pill)",
      background: "rgba(139,92,246,0.16)",
      border: "1px solid rgba(139,92,246,0.45)",
      color: "var(--violet-400)",
      font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)",
      boxShadow: "var(--glow-smart)"
    }
  }, "SMART"), seeAll && /*#__PURE__*/React.createElement("button", {
    style: {
      marginLeft: "auto",
      display: "inline-flex",
      alignItems: "center",
      gap: 4,
      background: "none",
      border: "none",
      color: "var(--text-tertiary)",
      font: "var(--text-label)",
      cursor: "pointer"
    }
  }, "See all ", /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 18
    }
  }, "\u203A"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: "var(--rail-gap)",
      overflowX: "auto",
      padding: `4px var(--safe-x) 12px`,
      scrollbarWidth: "none"
    }
  }, children));
}
Object.assign(__ds_scope, { Rail });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/media/Rail.jsx", error: String((e && e.message) || e) }); }

// components/navigation/SidebarNav.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** SidebarNav — the left rail nav. Collapsed to icons; expands to labels when
 *  the nav column has focus. items: [{ id, label, icon }]. Highlights `active`. */
function SidebarNav({
  items = [],
  active,
  expanded = false,
  brand = "ARE",
  onSelect,
  style,
  ...rest
}) {
  const [hoverExpand, setHoverExpand] = React.useState(false);
  const open = expanded || hoverExpand;
  return /*#__PURE__*/React.createElement("nav", _extends({
    onMouseEnter: () => setHoverExpand(true),
    onMouseLeave: () => setHoverExpand(false),
    style: {
      width: open ? "var(--sidebar-w-open)" : "var(--sidebar-w)",
      transition: "width var(--dur-base) var(--ease-out)",
      background: "linear-gradient(180deg, var(--surface-1), var(--bg-base))",
      borderRight: "1px solid var(--border-subtle)",
      display: "flex",
      flexDirection: "column",
      padding: "var(--sp-8) 0",
      height: "100%",
      flex: "0 0 auto",
      overflow: "hidden",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12,
      padding: "0 26px",
      marginBottom: "var(--sp-10)",
      height: 44
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 40,
      height: 40,
      borderRadius: "var(--r-sm)",
      background: "var(--accent)",
      display: "grid",
      placeItems: "center",
      font: "var(--fw-bold) 18px/1 var(--font-display)",
      color: "#fff",
      flex: "0 0 auto",
      boxShadow: "var(--glow-accent)"
    }
  }, brand[0]), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--fw-bold) var(--fs-h3)/1 var(--font-display)",
      color: "var(--text-primary)",
      whiteSpace: "nowrap",
      opacity: open ? 1 : 0,
      transition: "opacity var(--dur-fast)"
    }
  }, "ARE ", /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--text-tertiary)",
      fontWeight: 500
    }
  }, "iptv"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 6,
      padding: "0 16px",
      flex: 1
    }
  }, items.map(it => {
    const on = it.id === active;
    return /*#__PURE__*/React.createElement("button", {
      key: it.id,
      onClick: () => onSelect && onSelect(it.id),
      style: {
        display: "flex",
        alignItems: "center",
        gap: 16,
        height: 52,
        padding: "0 20px",
        borderRadius: "var(--r-md)",
        background: on ? "var(--accent-wash)" : "transparent",
        border: "none",
        cursor: "pointer",
        position: "relative",
        color: on ? "var(--accent-hover)" : "var(--text-tertiary)",
        transition: "var(--tr-color)"
      },
      onMouseEnter: e => {
        if (!on) e.currentTarget.style.background = "var(--surface-2)";
      },
      onMouseLeave: e => {
        if (!on) e.currentTarget.style.background = "transparent";
      }
    }, on && /*#__PURE__*/React.createElement("span", {
      style: {
        position: "absolute",
        left: 0,
        top: 12,
        bottom: 12,
        width: 3,
        borderRadius: 3,
        background: "var(--accent)"
      }
    }), /*#__PURE__*/React.createElement(__ds_scope.Icon, {
      name: it.icon,
      size: 26
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "var(--text-label)",
        whiteSpace: "nowrap",
        opacity: open ? 1 : 0,
        transition: "opacity var(--dur-fast)",
        color: on ? "var(--text-primary)" : "var(--text-secondary)"
      }
    }, it.label));
  })));
}
Object.assign(__ds_scope, { SidebarNav });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/SidebarNav.jsx", error: String((e && e.message) || e) }); }

// components/navigation/Tabs.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** Tabs — horizontal category switcher (browse, detail sections). Underline
 *  indicator slides under the active tab. items: [{ id, label }]. */
function Tabs({
  items = [],
  active,
  onSelect,
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      display: "flex",
      gap: 8,
      borderBottom: "1px solid var(--border-subtle)",
      ...style
    }
  }, rest), items.map(it => {
    const on = it.id === active;
    return /*#__PURE__*/React.createElement("button", {
      key: it.id,
      onClick: () => onSelect && onSelect(it.id),
      style: {
        position: "relative",
        background: "none",
        border: "none",
        cursor: "pointer",
        padding: "0 6px 16px",
        font: on ? "var(--fw-bold) var(--fs-h3)/1 var(--font-display)" : "var(--fw-medium) var(--fs-h3)/1 var(--font-display)",
        color: on ? "var(--text-primary)" : "var(--text-tertiary)",
        transition: "var(--tr-color)"
      }
    }, it.label, /*#__PURE__*/React.createElement("span", {
      style: {
        position: "absolute",
        left: 6,
        right: 6,
        bottom: -1,
        height: 3,
        borderRadius: 3,
        background: on ? "var(--accent)" : "transparent",
        boxShadow: on ? "var(--glow-accent)" : "none",
        transition: "background var(--dur-fast)"
      }
    }));
  }));
}
Object.assign(__ds_scope, { Tabs });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/Tabs.jsx", error: String((e && e.message) || e) }); }

// components/overlay/Dialog.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** Dialog — modal sheet on a scrim (confirm remove, parental PIN, add source).
 *  Glass panel, title, body, and an action row. Not a full focus-trap — a
 *  visual recreation for the design system. */
function Dialog({
  open = true,
  title,
  children,
  actions,
  width = 520,
  onClose,
  style,
  ...rest
}) {
  if (!open) return null;
  return /*#__PURE__*/React.createElement("div", {
    onClick: onClose,
    style: {
      position: "absolute",
      inset: 0,
      display: "grid",
      placeItems: "center",
      background: "rgba(6,7,10,0.6)",
      backdropFilter: "blur(var(--blur-scrim))",
      zIndex: 50
    }
  }, /*#__PURE__*/React.createElement("div", _extends({
    onClick: e => e.stopPropagation(),
    role: "dialog",
    "aria-modal": "true",
    style: {
      width,
      maxWidth: "90%",
      background: "var(--surface-2)",
      borderRadius: "var(--r-xl)",
      border: "1px solid var(--border-default)",
      boxShadow: "var(--shadow-xl)",
      padding: "var(--sp-8)",
      ...style
    }
  }, rest), title && /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "var(--text-h2)",
      color: "var(--text-primary)",
      letterSpacing: "var(--ls-tight)",
      marginBottom: "var(--sp-4)"
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--text-body)",
      color: "var(--text-secondary)",
      lineHeight: "var(--lh-normal)"
    }
  }, children), actions && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "flex-end",
      gap: 12,
      marginTop: "var(--sp-8)"
    }
  }, actions)));
}
Object.assign(__ds_scope, { Dialog });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/overlay/Dialog.jsx", error: String((e && e.message) || e) }); }

// components/player/PlayerControls.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/** PlayerControls — glass transport HUD overlaid on live video / VOD.
 *  Shows program title + now/next, a TimeShift-aware seek bar (buffered region
 *  vs live edge), transport buttons, and quick actions (audio, subtitles,
 *  aspect, PiP, multi-view). `live` toggles the LIVE-edge treatment. */
function PlayerControls({
  title,
  subtitle,
  live = true,
  playing = true,
  position = 62,
  buffered = 80,
  elapsed = "20:28",
  total = "20:45",
  channelLogoInitials = "SKY",
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      padding: "var(--sp-6)",
      borderRadius: "var(--r-xl)",
      background: "var(--surface-glass)",
      backdropFilter: "blur(var(--blur-glass))",
      border: "1px solid var(--border-default)",
      boxShadow: "var(--shadow-glass)",
      display: "flex",
      flexDirection: "column",
      gap: "var(--sp-4)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 52,
      height: 52,
      borderRadius: "var(--r-sm)",
      background: "var(--surface-overlay)",
      display: "grid",
      placeItems: "center",
      border: "1px solid var(--border-default)",
      font: "var(--fw-bold) 15px/1 var(--font-display)",
      color: "var(--text-primary)",
      flex: "0 0 auto"
    }
  }, channelLogoInitials), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10
    }
  }, live && /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 5,
      padding: "3px 8px",
      borderRadius: "var(--r-xs)",
      background: "var(--live)",
      color: "#fff",
      font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 6,
      height: 6,
      borderRadius: "50%",
      background: "#fff"
    }
  }), "LIVE"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-h3)",
      color: "var(--text-primary)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, title)), subtitle && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 3,
      font: "var(--text-caption)",
      color: "var(--text-secondary)"
    }
  }, subtitle)), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-mono)",
      color: "var(--text-secondary)",
      fontSize: 14
    }
  }, elapsed, " ", /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--text-tertiary)"
    }
  }, "/ ", total))), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      height: 6,
      borderRadius: 3,
      background: "var(--surface-3)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      left: 0,
      top: 0,
      bottom: 0,
      width: `${buffered}%`,
      background: "var(--border-strong)",
      borderRadius: 3
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      left: 0,
      top: 0,
      bottom: 0,
      width: `${position}%`,
      background: "var(--accent)",
      borderRadius: 3
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      left: `${position}%`,
      top: "50%",
      width: 16,
      height: 16,
      marginLeft: -8,
      marginTop: -8,
      borderRadius: "50%",
      background: "#fff",
      boxShadow: "var(--glow-accent)"
    }
  }), live && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      right: 0,
      top: "50%",
      transform: "translateY(-50%)",
      display: "flex",
      alignItems: "center",
      gap: 5
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 8,
      height: 8,
      borderRadius: "50%",
      background: "var(--live)"
    }
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    label: "Rewind",
    variant: "glass"
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "rewind"
  })), /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    label: playing ? "Pause" : "Play",
    variant: "glass",
    size: "lg",
    active: true
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: playing ? "pause" : "play",
    size: 28
  })), /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    label: "Fast forward",
    variant: "glass"
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "fast-forward"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      width: 1,
      height: 32,
      background: "var(--border-default)",
      margin: "0 6px"
    }
  }), /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    label: "Jump to live",
    variant: "glass"
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "skip-forward"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    label: "Audio track",
    variant: "glass"
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "volume-2"
  })), /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    label: "Subtitles",
    variant: "glass"
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "captions"
  })), /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    label: "Multi-view",
    variant: "glass"
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "columns-2"
  })), /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    label: "Picture in picture",
    variant: "glass"
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "picture-in-picture-2"
  })), /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    label: "Open guide",
    variant: "glass"
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "layout-grid"
  }))));
}
Object.assign(__ds_scope, { PlayerControls });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/player/PlayerControls.jsx", error: String((e && e.message) || e) }); }

// ui_kits/are-tv/app.jsx
try { (() => {
// ARE iptv — app shell: rail nav + screen router + overlays (detail, player, multi-view, onboarding).
function App() {
  const C = window.AREIptvDesignSystem_632b75;
  const {
    SidebarNav,
    IconButton,
    Icon
  } = C;
  const [theme, setThemeState] = React.useState("dark");
  const [booted, setBooted] = React.useState(false);
  const [screen, setScreen] = React.useState("home");
  const [detail, setDetail] = React.useState(null);
  const [player, setPlayer] = React.useState(null);
  const [multi, setMulti] = React.useState(false);
  const setTheme = t => {
    setThemeState(t);
    document.documentElement.setAttribute("data-theme", t);
  };
  React.useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
  }, []);
  const navItems = [{
    id: "home",
    label: "Home",
    icon: "home"
  }, {
    id: "live",
    label: "Live TV",
    icon: "tv"
  }, {
    id: "guide",
    label: "TV Guide",
    icon: "layout-grid"
  }, {
    id: "movies",
    label: "Movies",
    icon: "film"
  }, {
    id: "series",
    label: "Series",
    icon: "clapperboard"
  }, {
    id: "search",
    label: "Search",
    icon: "search"
  }, {
    id: "favorites",
    label: "Favorites",
    icon: "heart"
  }, {
    id: "settings",
    label: "Settings",
    icon: "settings"
  }];
  const openDetail = item => setDetail(item || {});
  const openPlayer = ch => setPlayer(ch || {});
  const D = window.AREDATA;
  const Screen = () => {
    switch (screen) {
      case "home":
        return /*#__PURE__*/React.createElement(window.Home, {
          openDetail: openDetail,
          openPlayer: openPlayer
        });
      case "live":
        return /*#__PURE__*/React.createElement(window.Live, {
          openPlayer: openPlayer
        });
      case "guide":
        return /*#__PURE__*/React.createElement(window.Guide, {
          openPlayer: openPlayer
        });
      case "movies":
        return /*#__PURE__*/React.createElement(window.Browse, {
          key: "movies",
          title: "Movies",
          data: D.movies,
          openDetail: openDetail
        });
      case "series":
        return /*#__PURE__*/React.createElement(window.Browse, {
          key: "series",
          title: "Series",
          data: D.series,
          openDetail: openDetail
        });
      case "search":
        return /*#__PURE__*/React.createElement(window.Search, {
          openDetail: openDetail,
          openPlayer: openPlayer
        });
      case "favorites":
        return /*#__PURE__*/React.createElement(window.Favorites, {
          openDetail: openDetail,
          openPlayer: openPlayer
        });
      case "settings":
        return /*#__PURE__*/React.createElement(window.Settings, {
          theme: theme,
          setTheme: setTheme
        });
      default:
        return null;
    }
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      height: "100vh",
      width: "100%",
      overflow: "hidden",
      background: "var(--bg-base)",
      display: "flex"
    }
  }, /*#__PURE__*/React.createElement(SidebarNav, {
    items: navItems,
    active: screen === "live" ? "live" : screen,
    onSelect: setScreen
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      position: "relative",
      overflowY: "auto",
      overflowX: "hidden"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "sticky",
      top: 0,
      zIndex: 10,
      display: "flex",
      alignItems: "center",
      gap: 12,
      padding: "18px var(--safe-x)",
      background: "linear-gradient(180deg, var(--bg-base) 55%, transparent)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement(IconButton, {
    label: "Multi-view",
    variant: "solid",
    onClick: () => setMulti(true)
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "grid-2x2"
  })), /*#__PURE__*/React.createElement(IconButton, {
    label: "Search",
    variant: "solid",
    onClick: () => setScreen("search")
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "search"
  })), /*#__PURE__*/React.createElement(IconButton, {
    label: "Add playlist",
    variant: "solid",
    onClick: () => setBooted(false)
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "plus"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      width: 44,
      height: 44,
      borderRadius: "50%",
      background: `center/cover url(${D.img("avatar", 100, 100)})`,
      border: "2px solid var(--border-strong)"
    }
  })), /*#__PURE__*/React.createElement(Screen, null)), detail && /*#__PURE__*/React.createElement(window.Detail, {
    item: detail,
    onClose: () => setDetail(null),
    openPlayer: openPlayer
  }), player && /*#__PURE__*/React.createElement(window.LivePlayer, {
    channel: player,
    onClose: () => setPlayer(null)
  }), multi && /*#__PURE__*/React.createElement(window.MultiView, {
    onClose: () => setMulti(false)
  }), !booted && /*#__PURE__*/React.createElement(window.Onboarding, {
    onDone: () => setBooted(true)
  }));
}
window.App = App;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/are-tv/app.jsx", error: String((e && e.message) || e) }); }

// ui_kits/are-tv/data.js
try { (() => {
// ARE iptv — mock data for the UI kit. Real-looking IPTV content; images via picsum.
window.AREDATA = (() => {
  const img = (s, w, h) => `https://picsum.photos/seed/${s}/${w}/${h}`;
  const poster = s => img(s, 320, 480);
  const still = s => img(s, 480, 270);
  const wide = s => img(s, 1280, 640);
  const featured = {
    kicker: "Featured tonight",
    title: "British Grand Prix",
    meta: "LIVE · Sky Sports F1 · 4K HDR · Lap 34 / 52",
    synopsis: "Lights out at Silverstone as the championship battle reaches its halfway point. Full race, pit-wall audio and onboard cameras.",
    image: wide("silverstone-f1"),
    logo: "F1"
  };
  const continueWatching = [{
    title: "The Last of Us",
    meta: "S2 · E4 · 26 min left",
    image: still("lastofus"),
    progress: 45,
    remaining: "26 min left"
  }, {
    title: "Dune: Part Two",
    meta: "1h 22m left",
    image: still("dune2"),
    progress: 38,
    remaining: "1h 22m left"
  }, {
    title: "Slow Horses",
    meta: "S4 · E2 · 12 min left",
    image: still("slowhorses"),
    progress: 78,
    remaining: "12 min left"
  }, {
    title: "Formula 1: Drive to Survive",
    meta: "S6 · E7",
    image: still("dts"),
    progress: 60,
    remaining: "18 min left"
  }, {
    title: "Shōgun",
    meta: "S1 · E9",
    image: still("shogun"),
    progress: 25,
    remaining: "42 min left"
  }];
  const liveNow = [{
    channel: "BBC One HD",
    number: "101",
    now: "The News at Ten",
    next: "Match of the Day",
    progress: 70,
    health: "stable",
    quality: "FHD",
    codec: "H.264",
    catchup: true,
    fav: true
  }, {
    channel: "Sky Sports F1",
    number: "406",
    now: "British Grand Prix",
    next: "Post-race analysis",
    progress: 55,
    health: "stable",
    quality: "FHD",
    codec: "H.265",
    catchup: true,
    fav: true
  }, {
    channel: "ESPN",
    number: "204",
    now: "NBA: Lakers @ Celtics",
    next: "SportsCenter",
    progress: 40,
    health: "moderate",
    quality: "HD",
    codec: "H.264",
    catchup: true
  }, {
    channel: "CNN",
    number: "311",
    now: "Global News Hour",
    next: "Amanpour",
    progress: 62,
    health: "stable",
    quality: "FHD",
    codec: "H.264",
    catchup: true
  }, {
    channel: "Discovery",
    number: "520",
    now: "How It's Made",
    next: "Gold Rush",
    progress: 18,
    health: "poor",
    quality: "SD",
    codec: "H.264"
  }, {
    channel: "Cartoon Network",
    number: "701",
    now: "Adventure Time",
    next: "Regular Show",
    progress: 88,
    health: "stable",
    quality: "HD",
    codec: "H.264"
  }];

  // Channel pool tagged by live category — drives the Live TV screen's category filter.
  const liveChannelPool = [{
    channel: "Sky Sports F1",
    number: "406",
    now: "British Grand Prix",
    next: "Post-race analysis",
    progress: 55,
    health: "stable",
    cat: "Sports",
    quality: "FHD",
    codec: "H.265",
    catchup: true,
    fav: true
  }, {
    channel: "ESPN",
    number: "204",
    now: "NBA: Lakers @ Celtics",
    next: "SportsCenter",
    progress: 40,
    health: "moderate",
    cat: "Sports",
    quality: "HD",
    codec: "H.264",
    catchup: true
  }, {
    channel: "TNT Sports 1",
    number: "410",
    now: "Premier League Live",
    next: "Goals on Sunday",
    progress: 33,
    health: "stable",
    cat: "Sports",
    quality: "FHD",
    codec: "H.265",
    catchup: true
  }, {
    channel: "Eurosport 1",
    number: "412",
    now: "Cycling: Tour Stage 12",
    next: "Snooker",
    progress: 61,
    health: "stable",
    cat: "Sports",
    quality: "HD",
    codec: "H.264"
  }, {
    channel: "BBC One HD",
    number: "101",
    now: "The News at Ten",
    next: "Match of the Day",
    progress: 70,
    health: "stable",
    cat: "Entertainment",
    quality: "FHD",
    codec: "H.264",
    catchup: true,
    fav: true
  }, {
    channel: "ITV1 HD",
    number: "103",
    now: "Coronation Street",
    next: "Drama Premiere",
    progress: 48,
    health: "stable",
    cat: "Entertainment",
    quality: "FHD",
    codec: "H.264",
    catchup: true
  }, {
    channel: "Channel 4 HD",
    number: "104",
    now: "Grand Designs",
    next: "Feature Film",
    progress: 22,
    health: "moderate",
    cat: "Entertainment",
    quality: "HD",
    codec: "H.264",
    catchup: true
  }, {
    channel: "CNN",
    number: "311",
    now: "Global News Hour",
    next: "Amanpour",
    progress: 62,
    health: "stable",
    cat: "News",
    quality: "FHD",
    codec: "H.264",
    catchup: true
  }, {
    channel: "BBC News",
    number: "231",
    now: "Verified Live",
    next: "The Context",
    progress: 15,
    health: "stable",
    cat: "News",
    quality: "HD",
    codec: "H.264",
    catchup: true
  }, {
    channel: "Al Jazeera",
    number: "235",
    now: "Newshour",
    next: "Inside Story",
    progress: 44,
    health: "stable",
    cat: "News",
    quality: "HD",
    codec: "H.264"
  }, {
    channel: "Discovery",
    number: "520",
    now: "How It's Made",
    next: "Gold Rush",
    progress: 18,
    health: "poor",
    cat: "Documentary",
    quality: "SD",
    codec: "H.264"
  }, {
    channel: "National Geographic",
    number: "525",
    now: "Air Crash Investigation",
    next: "Drain the Oceans",
    progress: 71,
    health: "stable",
    cat: "Documentary",
    quality: "FHD",
    codec: "H.265",
    catchup: true
  }, {
    channel: "Cartoon Network",
    number: "701",
    now: "Adventure Time",
    next: "Regular Show",
    progress: 88,
    health: "stable",
    cat: "Kids",
    quality: "HD",
    codec: "H.264"
  }, {
    channel: "Nickelodeon",
    number: "705",
    now: "SpongeBob",
    next: "The Loud House",
    progress: 52,
    health: "stable",
    cat: "Kids",
    quality: "HD",
    codec: "H.264"
  }, {
    channel: "MTV Hits",
    number: "801",
    now: "Top 40 Countdown",
    next: "Pop Throwbacks",
    progress: 30,
    health: "stable",
    cat: "Music",
    quality: "SD",
    codec: "H.264"
  }, {
    channel: "Kerrang! TV",
    number: "805",
    now: "Rock Anthems",
    next: "New Noise",
    progress: 66,
    health: "moderate",
    cat: "Music",
    quality: "SD",
    codec: "H.264"
  }, {
    channel: "Sky Cinema Premiere",
    number: "301",
    now: "Dune: Part Two",
    next: "Oppenheimer",
    progress: 12,
    health: "stable",
    cat: "Movies",
    quality: "FHD",
    codec: "H.265",
    catchup: true,
    fav: true
  }, {
    channel: "TCM",
    number: "315",
    now: "Casablanca",
    next: "The Godfather",
    progress: 80,
    health: "stable",
    cat: "Movies",
    quality: "SD",
    codec: "H.264"
  }, {
    channel: "TF1",
    number: "901",
    now: "Journal de 20h",
    next: "Koh-Lanta",
    progress: 25,
    health: "stable",
    cat: "International",
    quality: "HD",
    codec: "H.265",
    catchup: true
  }, {
    channel: "RAI 1",
    number: "915",
    now: "Telegiornale",
    next: "Serie A Live",
    progress: 58,
    health: "moderate",
    cat: "International",
    quality: "HD",
    codec: "H.264"
  }, {
    channel: "DAZN 4K",
    number: "600",
    now: "UFC Fight Night",
    next: "Boxing Tonight",
    progress: 42,
    health: "stable",
    cat: "4K & UHD",
    quality: "4K",
    codec: "H.265",
    catchup: true,
    fav: true
  }, {
    channel: "Insight UHD",
    number: "608",
    now: "Planet Earth III",
    next: "Blue Planet",
    progress: 19,
    health: "stable",
    cat: "4K & UHD",
    quality: "4K",
    codec: "H.265"
  }];
  const movies = [{
    title: "Dune: Part Two",
    meta: "2024 · Sci-Fi",
    image: poster("dune-two"),
    rating: "9.1",
    badge: "4K"
  }, {
    title: "Oppenheimer",
    meta: "2023 · Drama",
    image: poster("oppenheimer"),
    rating: "8.9",
    badge: "4K"
  }, {
    title: "The Batman",
    meta: "2022 · Action",
    image: poster("thebatman"),
    rating: "8.4"
  }, {
    title: "Poor Things",
    meta: "2023 · Comedy",
    image: poster("poorthings"),
    rating: "8.0",
    badge: "NEW"
  }, {
    title: "Killers of the Flower Moon",
    meta: "2023 · Crime",
    image: poster("kotfm"),
    rating: "7.8"
  }, {
    title: "Everything Everywhere",
    meta: "2022 · Sci-Fi",
    image: poster("eeaao"),
    rating: "8.1"
  }, {
    title: "Past Lives",
    meta: "2023 · Drama",
    image: poster("pastlives"),
    rating: "8.3"
  }, {
    title: "The Holdovers",
    meta: "2023 · Comedy",
    image: poster("holdovers"),
    rating: "8.0"
  }];
  const series = [{
    title: "Shōgun",
    meta: "S1 · Drama",
    image: poster("shogun-s"),
    rating: "9.2",
    badge: "NEW"
  }, {
    title: "The Bear",
    meta: "S3 · Comedy",
    image: poster("thebear"),
    rating: "8.6"
  }, {
    title: "Fallout",
    meta: "S1 · Sci-Fi",
    image: poster("fallout"),
    rating: "8.5",
    badge: "4K"
  }, {
    title: "True Detective",
    meta: "S4 · Crime",
    image: poster("truedetective"),
    rating: "8.3"
  }, {
    title: "Severance",
    meta: "S2 · Thriller",
    image: poster("severance"),
    rating: "8.7"
  }, {
    title: "The Last of Us",
    meta: "S2 · Drama",
    image: poster("tlou-s"),
    rating: "8.8"
  }, {
    title: "Slow Horses",
    meta: "S4 · Thriller",
    image: poster("slowhorses-s"),
    rating: "8.3"
  }, {
    title: "Ripley",
    meta: "S1 · Crime",
    image: poster("ripley"),
    rating: "8.2"
  }];
  const recommended = [{
    title: "MotoGP Highlights",
    meta: "Because you watch F1",
    image: poster("motogp"),
    rating: "—",
    smart: true
  }, {
    title: "Senna",
    meta: "Docuseries",
    image: poster("senna"),
    rating: "8.9",
    smart: true
  }, {
    title: "Ford v Ferrari",
    meta: "2019 · Drama",
    image: poster("fvf"),
    rating: "8.1",
    smart: true
  }, {
    title: "Rush",
    meta: "2013 · Drama",
    image: poster("rush"),
    rating: "8.1",
    smart: true
  }, {
    title: "Gran Turismo",
    meta: "2023 · Action",
    image: poster("gt"),
    rating: "7.2",
    smart: true
  }, {
    title: "Le Mans '66",
    meta: "Docs",
    image: poster("lemans"),
    rating: "7.9",
    smart: true
  }];

  // EPG grid: channels x programs. times are minute-offsets in a 6h window from 18:00.
  const epgChannels = [{
    name: "BBC One HD",
    num: "101",
    logo: "BBC",
    cat: "Entertainment"
  }, {
    name: "ITV1 HD",
    num: "103",
    logo: "ITV",
    cat: "Entertainment"
  }, {
    name: "Channel 4 HD",
    num: "104",
    logo: "C4",
    cat: "Entertainment"
  }, {
    name: "Sky Sports F1",
    num: "406",
    logo: "F1",
    cat: "Sports"
  }, {
    name: "Sky Sports Main",
    num: "401",
    logo: "SKY",
    cat: "Sports"
  }, {
    name: "ESPN",
    num: "204",
    logo: "E",
    cat: "Sports"
  }, {
    name: "CNN",
    num: "311",
    logo: "CNN",
    cat: "News"
  }, {
    name: "Discovery",
    num: "520",
    logo: "DIS",
    cat: "Documentary"
  }, {
    name: "National Geographic",
    num: "525",
    logo: "NG",
    cat: "Documentary"
  }, {
    name: "Cartoon Network",
    num: "701",
    logo: "CN",
    cat: "Kids"
  }];
  const epgRows = [[["18:00", "Regional News", 60, "catchup"], ["19:00", "EastEnders", 30], ["19:30", "The One Show", 30], ["20:00", "The News at Ten", 45, "now"], ["20:45", "Match of the Day Live", 105]], [["18:00", "ITV Evening News", 60, "catchup"], ["19:00", "Emmerdale", 30], ["19:30", "Coronation Street", 30], ["20:00", "Drama Premiere", 90, "now"], ["21:30", "News at Ten", 60]], [["18:00", "The Simpsons", 60, "catchup"], ["19:00", "Hollyoaks", 30], ["19:30", "Grand Designs", 60, "now"], ["20:30", "Feature Film: Arrival", 120]], [["18:00", "F1 Practice 3", 90, "catchup"], ["19:30", "Qualifying Analysis", 30], ["20:00", "British Grand Prix", 150, "now"], ["22:30", "Post-race", 60]], [["18:00", "Premier League Review", 60], ["19:00", "Live Football", 120, "now"], ["21:00", "Gillette Soccer", 60], ["22:00", "Highlights", 60]], [["18:00", "SportsCenter", 60, "catchup"], ["19:00", "NBA Pregame", 30], ["19:30", "NBA: Lakers @ Celtics", 150, "now"], ["22:00", "Post-game", 60]], [["18:00", "Global News Hour", 60, "now"], ["19:00", "Amanpour", 60], ["20:00", "Quest Means Business", 60], ["21:00", "CNN Newsroom", 120]], [["18:00", "How It's Made", 60, "catchup"], ["19:00", "Gold Rush", 60, "now"], ["20:00", "Deadliest Catch", 60], ["21:00", "Expedition Unknown", 120]], [["18:00", "Wild Planet", 90, "catchup"], ["19:30", "Drain the Oceans", 60, "now"], ["20:30", "Air Crash Investigation", 90], ["22:00", "Cosmos", 60]], [["18:00", "Adventure Time", 60, "catchup"], ["19:00", "Regular Show", 30], ["19:30", "Gumball", 30, "now"], ["20:00", "Teen Titans Go!", 60], ["21:00", "We Bare Bears", 60]]];
  const categories = ["All", "Sports", "Movies", "News", "Kids", "Entertainment", "Documentary", "Music", "International"];

  // ---- CATEGORIES: the grouping every content type shares (Live TV, Movies, Series, EPG) ----
  // Real playlists often ship NO artwork per category, so most cats are artless
  // (the CategoryCard renders a clean kind-icon folder). VOD cats borrow real
  // poster art only where matched items actually exist.
  const genreOf = m => (m.meta.split("\u00b7")[1] || "").trim();
  const pics = (list, match) => list.filter(x => genreOf(x).includes(match)).map(x => x.image);
  const anyPics = (list, n) => list.slice(0, n).map(x => x.image);

  // VOD category columns — `match` filters the grid by genre keyword; `count` is display-realistic.
  const movieCats = [{
    name: "All movies",
    kind: "movies",
    count: 642,
    posters: anyPics(movies, 4),
    all: true
  }, {
    name: "Recently added",
    kind: "movies",
    count: 48,
    posters: anyPics(movies.slice(3), 4),
    smart: true,
    recent: true
  }, {
    name: "Action & Adventure",
    kind: "movies",
    count: 128,
    posters: pics(movies, "Action"),
    match: "Action"
  }, {
    name: "Sci-Fi & Fantasy",
    kind: "movies",
    count: 96,
    posters: pics(movies, "Sci-Fi"),
    match: "Sci-Fi"
  }, {
    name: "Drama",
    kind: "movies",
    count: 210,
    posters: pics(movies, "Drama"),
    match: "Drama"
  }, {
    name: "Comedy",
    kind: "movies",
    count: 84,
    posters: pics(movies, "Comedy"),
    match: "Comedy"
  }, {
    name: "Crime & Thriller",
    kind: "movies",
    count: 72,
    posters: pics(movies, "Crime"),
    match: "Crime"
  }, {
    name: "Documentary",
    kind: "movies",
    count: 61,
    match: "Documentary"
  }, {
    name: "Family & Kids",
    kind: "movies",
    count: 118,
    match: "Family"
  }, {
    name: "Horror",
    kind: "movies",
    count: 54,
    match: "Horror"
  }, {
    name: "Romance",
    kind: "movies",
    count: 39,
    match: "Romance"
  }];
  const seriesCats = [{
    name: "All series",
    kind: "series",
    count: 318,
    posters: anyPics(series, 4),
    all: true
  }, {
    name: "Recently added",
    kind: "series",
    count: 22,
    posters: anyPics(series.slice(2), 4),
    smart: true,
    recent: true
  }, {
    name: "Drama",
    kind: "series",
    count: 96,
    posters: pics(series, "Drama"),
    match: "Drama"
  }, {
    name: "Sci-Fi & Fantasy",
    kind: "series",
    count: 41,
    posters: pics(series, "Sci-Fi"),
    match: "Sci-Fi"
  }, {
    name: "Crime",
    kind: "series",
    count: 63,
    posters: pics(series, "Crime"),
    match: "Crime"
  }, {
    name: "Comedy",
    kind: "series",
    count: 38,
    posters: pics(series, "Comedy"),
    match: "Comedy"
  }, {
    name: "Thriller",
    kind: "series",
    count: 57,
    posters: pics(series, "Thriller"),
    match: "Thriller"
  }, {
    name: "Reality & Docs",
    kind: "series",
    count: 44,
    match: "Reality"
  }, {
    name: "Animation",
    kind: "series",
    count: 31,
    match: "Animation"
  }];

  // Live TV category folders — channel groups. Artless (real IPTV groups rarely ship art).
  const liveCats = [{
    name: "All channels",
    kind: "live",
    count: 1240,
    all: true
  }, {
    name: "Sports",
    kind: "live",
    count: 128
  }, {
    name: "News",
    kind: "live",
    count: 64
  }, {
    name: "Entertainment",
    kind: "live",
    count: 312
  }, {
    name: "Kids",
    kind: "live",
    count: 39
  }, {
    name: "Documentary",
    kind: "live",
    count: 57
  }, {
    name: "Movies",
    kind: "live",
    count: 210
  }, {
    name: "Music",
    kind: "live",
    count: 44
  }, {
    name: "International",
    kind: "live",
    count: 486
  }, {
    name: "4K & UHD",
    kind: "live",
    count: 72
  }, {
    name: "Local channels",
    kind: "live",
    count: 28
  }, {
    name: "24/7 & VOD channels",
    kind: "live",
    count: 96
  }];

  // Curated "Browse by category" rail for Home — a mix across content types.
  const browseCats = [{
    name: "Sports",
    kind: "live",
    count: 128
  }, {
    name: "Action & Adventure",
    kind: "movies",
    count: 128,
    posters: pics(movies, "Action")
  }, {
    name: "Drama series",
    kind: "series",
    count: 96,
    posters: pics(series, "Drama")
  }, {
    name: "News",
    kind: "live",
    count: 64
  }, {
    name: "Suggested for you",
    kind: "movies",
    count: 40,
    posters: anyPics(recommended, 4),
    smart: true
  }, {
    name: "Kids",
    kind: "live",
    count: 39
  }, {
    name: "Documentary",
    kind: "guide",
    count: 57
  }, {
    name: "Catch-up",
    kind: "catchup",
    count: 12
  }];
  const detail = {
    title: "Shōgun",
    year: "2024",
    genres: "Drama · History · Adventure",
    seasons: "1 season · 10 episodes",
    rating: "9.2",
    maturity: "18",
    quality: "4K HDR",
    synopsis: "Set in Japan in the year 1600, a power struggle unfolds as Lord Yoshii Toranaga fights for survival against his rivals on the Council of Regents, while an English sailor's arrival upends the balance of power.",
    cast: ["Hiroyuki Sanada", "Cosmo Jarvis", "Anna Sawai", "Tadanobu Asano", "Takehiro Hira"],
    backdrop: wide("shogun-backdrop"),
    poster: poster("shogun-detail"),
    episodes: [{
      n: 1,
      title: "Anjin",
      dur: "58m",
      still: still("shogun-e1"),
      desc: "A European ship runs aground off a fishing village."
    }, {
      n: 2,
      title: "Servants of Two Masters",
      dur: "56m",
      still: still("shogun-e2"),
      desc: "Toranaga navigates the council's hostility."
    }, {
      n: 3,
      title: "Tomorrow Is Tomorrow",
      dur: "52m",
      still: still("shogun-e3"),
      desc: "An escape from Osaka Castle turns deadly."
    }, {
      n: 4,
      title: "The Eightfold Fence",
      dur: "54m",
      still: still("shogun-e4"),
      desc: "Blackthorne adjusts to life in Ajiro."
    }]
  };
  return {
    img,
    poster,
    still,
    wide,
    featured,
    continueWatching,
    liveNow,
    liveChannelPool,
    movies,
    series,
    recommended,
    epgChannels,
    epgRows,
    categories,
    movieCats,
    seriesCats,
    liveCats,
    browseCats,
    genreOf,
    detail
  };
})();
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/are-tv/data.js", error: String((e && e.message) || e) }); }

// ui_kits/are-tv/screens/Browse.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
// Browse — category filter column (CategoryRow) + poster grid. Doubles for Movies / Series.
function Browse({
  title = "Movies",
  data,
  initialCat = 0,
  openDetail
}) {
  const C = window.AREIptvDesignSystem_632b75,
    D = window.AREDATA;
  const {
    CategoryRow,
    PosterTile,
    Badge
  } = C;
  const cats = title === "Series" ? D.seriesCats : D.movieCats;
  const [catIdx, setCatIdx] = React.useState(initialCat);
  React.useEffect(() => {
    setCatIdx(initialCat);
  }, [initialCat]);
  const all = data || [...D.movies, ...D.series];
  const cat = cats[catIdx];
  const items = cat.all || cat.recent ? all : all.filter(m => D.genreOf(m).includes(cat.match));
  const posterBadges = b => b ? [/*#__PURE__*/React.createElement(Badge, {
    key: "b",
    tone: b === "NEW" ? "new" : "quality"
  }, b)] : [];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "26px 0 40px"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "0 var(--safe-x)"
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "var(--text-display)",
      color: "var(--text-primary)",
      letterSpacing: "var(--ls-tight)",
      marginBottom: 22
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 32,
      alignItems: "flex-start"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 300,
      flex: "0 0 auto",
      position: "sticky",
      top: 96,
      display: "flex",
      flexDirection: "column",
      gap: 4
    }
  }, /*#__PURE__*/React.createElement("p", {
    style: {
      font: "var(--fw-bold) 11px/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)",
      textTransform: "uppercase",
      color: "var(--text-tertiary)",
      margin: "0 0 8px",
      padding: "0 16px"
    }
  }, "Categories"), cats.map((c, i) => /*#__PURE__*/React.createElement(CategoryRow, {
    key: c.name,
    name: c.name,
    count: c.count,
    kind: c.kind,
    smart: c.smart,
    active: i === catIdx,
    onClick: () => setCatIdx(i)
  }))), /*#__PURE__*/React.createElement("div", {
    key: catIdx,
    className: "cat-panel",
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "baseline",
      gap: 10,
      marginBottom: 18
    }
  }, /*#__PURE__*/React.createElement("h2", {
    style: {
      font: "var(--text-h2)",
      color: "var(--text-primary)",
      letterSpacing: "var(--ls-tight)"
    }
  }, cat.name), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-mono)",
      color: "var(--text-tertiary)"
    }
  }, cat.count.toLocaleString(), " titles")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))",
      gap: 24
    }
  }, items.map((m, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    className: "cat-tile",
    style: {
      animationDelay: `${i * 30}ms`
    }
  }, /*#__PURE__*/React.createElement(PosterTile, _extends({}, m, {
    width: "100%",
    badges: posterBadges(m.badge),
    onClick: () => openDetail(m)
  })))))))));
}
window.Browse = Browse;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/are-tv/screens/Browse.jsx", error: String((e && e.message) || e) }); }

// ui_kits/are-tv/screens/Detail.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
// Movie/Series detail — blurred backdrop, poster, metadata, cast, episodes.
function Detail({
  item,
  onClose,
  openPlayer
}) {
  const C = window.AREIptvDesignSystem_632b75,
    D = window.AREDATA;
  const {
    Button,
    Badge,
    Tabs,
    Icon,
    IconButton
  } = C;
  const d = D.detail;
  const [tab, setTab] = React.useState("episodes");
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      zIndex: 30,
      background: "var(--bg-base)",
      overflowY: "auto"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 0,
      left: 0,
      right: 0,
      height: 620,
      background: `center 15%/cover no-repeat url(${d.backdrop})`
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 0,
      left: 0,
      right: 0,
      height: 620,
      background: "var(--scrim-bottom)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 0,
      left: 0,
      right: 0,
      height: 620,
      background: "var(--scrim-left)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "24px var(--safe-x) 0"
    }
  }, /*#__PURE__*/React.createElement(IconButton, {
    label: "Back",
    variant: "glass",
    onClick: onClose
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "arrow-left"
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 44,
      padding: "120px var(--safe-x) 40px",
      alignItems: "flex-end"
    }
  }, /*#__PURE__*/React.createElement("img", {
    src: d.poster,
    alt: "",
    style: {
      width: 240,
      aspectRatio: "2/3",
      objectFit: "cover",
      borderRadius: "var(--r-lg)",
      boxShadow: "var(--shadow-xl)",
      flex: "0 0 auto",
      border: "1px solid var(--border-default)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      marginBottom: 16
    }
  }, /*#__PURE__*/React.createElement(Badge, {
    tone: "new"
  }, "NEW SEASON"), /*#__PURE__*/React.createElement(Badge, {
    tone: "quality"
  }, d.quality)), /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "var(--text-hero)",
      color: "#fff",
      letterSpacing: "var(--ls-tight)"
    }
  }, d.title), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 18,
      marginTop: 16,
      font: "var(--text-label)",
      color: "var(--ink-100)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 6
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--amber-400)"
    }
  }, "\u2605"), /*#__PURE__*/React.createElement("b", {
    style: {
      color: "#fff"
    }
  }, d.rating)), /*#__PURE__*/React.createElement("span", null, d.year), /*#__PURE__*/React.createElement("span", {
    style: {
      padding: "1px 8px",
      border: "1px solid var(--border-strong)",
      borderRadius: "var(--r-xs)",
      fontSize: 13
    }
  }, d.maturity), /*#__PURE__*/React.createElement("span", null, d.genres), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--text-tertiary)"
    }
  }, d.seasons)), /*#__PURE__*/React.createElement("p", {
    style: {
      marginTop: 18,
      font: "var(--text-body)",
      color: "var(--ink-100)",
      lineHeight: "var(--lh-normal)",
      maxWidth: 720
    }
  }, d.synopsis), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 14,
      marginTop: 26
    }
  }, /*#__PURE__*/React.createElement(Button, {
    size: "lg",
    icon: /*#__PURE__*/React.createElement(Icon, {
      name: "play",
      size: 22
    }),
    onClick: () => openPlayer({
      channel: d.title
    })
  }, "Play S1 \xB7 E1"), /*#__PURE__*/React.createElement(Button, {
    variant: "secondary",
    size: "lg",
    icon: /*#__PURE__*/React.createElement(Icon, {
      name: "plus",
      size: 20
    })
  }, "My list"), /*#__PURE__*/React.createElement(IconButton, {
    label: "Favorite",
    variant: "solid",
    size: "lg"
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "heart"
  })), /*#__PURE__*/React.createElement(IconButton, {
    label: "Trailer",
    variant: "solid",
    size: "lg"
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "clapperboard"
  }))))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "0 var(--safe-x)"
    }
  }, /*#__PURE__*/React.createElement(Tabs, {
    active: tab,
    onSelect: setTab,
    items: [{
      id: "episodes",
      label: "Episodes"
    }, {
      id: "cast",
      label: "Cast & crew"
    }, {
      id: "related",
      label: "More like this"
    }]
  }), tab === "episodes" && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: 20,
      marginTop: 26
    }
  }, d.episodes.map(ep => /*#__PURE__*/React.createElement("div", {
    key: ep.n,
    onClick: () => openPlayer({
      channel: `${d.title} · E${ep.n}`
    }),
    style: {
      display: "flex",
      gap: 16,
      padding: 12,
      borderRadius: "var(--r-md)",
      background: "var(--surface-1)",
      border: "1px solid var(--border-subtle)",
      cursor: "pointer"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      width: 160,
      aspectRatio: "16/9",
      borderRadius: "var(--r-sm)",
      overflow: "hidden",
      flex: "0 0 auto",
      background: `center/cover url(${ep.still})`
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      display: "grid",
      placeItems: "center",
      background: "rgba(6,7,10,.3)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      marginLeft: 3,
      borderStyle: "solid",
      borderWidth: "8px 0 8px 13px",
      borderColor: "transparent transparent transparent #fff"
    }
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "baseline",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--fw-bold) var(--fs-title)/1.2 var(--font-body)",
      color: "var(--text-primary)"
    }
  }, ep.n, ". ", ep.title), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-caption)",
      color: "var(--text-tertiary)",
      marginLeft: "auto"
    }
  }, ep.dur)), /*#__PURE__*/React.createElement("p", {
    style: {
      marginTop: 6,
      font: "var(--text-caption)",
      color: "var(--text-secondary)",
      lineHeight: "var(--lh-normal)"
    }
  }, ep.desc))))), tab === "cast" && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexWrap: "wrap",
      gap: 14,
      marginTop: 26,
      paddingBottom: 40
    }
  }, d.cast.map((name, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12,
      padding: "10px 18px 10px 10px",
      borderRadius: "var(--r-pill)",
      background: "var(--surface-1)",
      border: "1px solid var(--border-subtle)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 44,
      height: 44,
      borderRadius: "50%",
      background: `center/cover url(${D.img("cast" + i, 100, 100)})`
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-label)",
      color: "var(--text-primary)"
    }
  }, name)))), tab === "related" && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 20,
      marginTop: 26,
      flexWrap: "wrap",
      paddingBottom: 40
    }
  }, D.series.slice(0, 6).map((m, i) => /*#__PURE__*/React.createElement(C.PosterTile, _extends({
    key: i
  }, m, {
    width: 180
  })))))));
}
window.Detail = Detail;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/are-tv/screens/Detail.jsx", error: String((e && e.message) || e) }); }

// ui_kits/are-tv/screens/Favorites.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
// Favorites — multiple favorite lists (channels, movies, sports, kids) + custom
// groups. Smart favorites surfaces frequently-watched automatically.
function Favorites({
  openDetail,
  openPlayer
}) {
  const C = window.AREIptvDesignSystem_632b75,
    D = window.AREDATA;
  const {
    Tabs,
    ChannelTile,
    PosterTile,
    ContinueCard,
    Rail,
    Icon,
    Badge,
    Chip
  } = C;
  const [tab, setTab] = React.useState("channels");
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "26px 0 40px"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "0 var(--safe-x)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 14,
      marginBottom: 24
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "var(--text-display)",
      color: "var(--text-primary)",
      letterSpacing: "var(--ls-tight)"
    }
  }, "Favorites"), /*#__PURE__*/React.createElement(Chip, {
    icon: /*#__PURE__*/React.createElement(Icon, {
      name: "plus",
      size: 16
    }),
    style: {
      marginLeft: "auto"
    }
  }, "New group")), /*#__PURE__*/React.createElement(Tabs, {
    active: tab,
    onSelect: setTab,
    items: [{
      id: "channels",
      label: "Channels"
    }, {
      id: "movies",
      label: "Movies"
    }, {
      id: "sports",
      label: "Sports"
    }, {
      id: "kids",
      label: "Kids"
    }]
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 26
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "0 var(--safe-x) 14px",
      display: "flex",
      alignItems: "center",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "sparkles",
    size: 18,
    color: "var(--violet-400)"
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)",
      textTransform: "uppercase",
      color: "var(--violet-400)"
    }
  }, "Smart favorites"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-caption)",
      color: "var(--text-tertiary)"
    }
  }, "Frequently watched, surfaced automatically")), tab === "channels" && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 20,
      overflow: "hidden",
      padding: "0 var(--safe-x)"
    }
  }, D.liveNow.map((c, i) => /*#__PURE__*/React.createElement(ChannelTile, _extends({
    key: i
  }, c, {
    onClick: () => openPlayer(c)
  })))), tab === "movies" && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fill, minmax(190px,1fr))",
      gap: 24,
      padding: "0 var(--safe-x)"
    }
  }, D.movies.map((m, i) => /*#__PURE__*/React.createElement(PosterTile, _extends({
    key: i
  }, m, {
    width: "100%",
    onClick: () => openDetail(m)
  })))), tab === "sports" && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 20,
      overflow: "hidden",
      padding: "0 var(--safe-x)"
    }
  }, D.liveNow.slice(1, 4).map((c, i) => /*#__PURE__*/React.createElement(ChannelTile, _extends({
    key: i
  }, c, {
    onClick: () => openPlayer(c)
  }))), D.continueWatching.slice(3, 5).map((c, i) => /*#__PURE__*/React.createElement(ContinueCard, _extends({
    key: i
  }, c)))), tab === "kids" && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fill, minmax(190px,1fr))",
      gap: 24,
      padding: "0 var(--safe-x)"
    }
  }, D.series.slice(0, 4).map((m, i) => /*#__PURE__*/React.createElement(PosterTile, _extends({
    key: i
  }, m, {
    width: "100%",
    onClick: () => openDetail(m)
  }))))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 40
    }
  }, /*#__PURE__*/React.createElement(Rail, {
    title: "Weekend sports",
    seeAll: false
  }, D.liveNow.slice(0, 4).map((c, i) => /*#__PURE__*/React.createElement(ChannelTile, _extends({
    key: i
  }, c, {
    onClick: () => openPlayer(c)
  }))))));
}
window.Favorites = Favorites;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/are-tv/screens/Favorites.jsx", error: String((e && e.message) || e) }); }

// ui_kits/are-tv/screens/Guide.jsx
try { (() => {
// Full EPG guide grid — timeline header, channel column, proportional program cells.
// Guide is filterable by channel group (the TiviMate / OTT Navigator pattern:
// the guide always scopes to a group so 1000+-channel playlists stay usable).
function Guide({
  openPlayer
}) {
  const C = window.AREIptvDesignSystem_632b75,
    D = window.AREDATA;
  const {
    GuideCell,
    Chip,
    Icon,
    StreamHealth
  } = C;
  const PX = 3.2; // px per minute
  const times = ["18:00", "18:30", "19:00", "19:30", "20:00", "20:30", "21:00", "21:30", "22:00", "22:30", "23:00", "23:30"];
  const [day, setDay] = React.useState("Today");
  const [grp, setGrp] = React.useState("All");
  const GRP_ICON = {
    All: "layout-grid",
    Entertainment: "tv",
    Sports: "trophy",
    News: "newspaper",
    Documentary: "globe",
    Kids: "baby"
  };
  const groups = ["All", ...Array.from(new Set(D.epgChannels.map(c => c.cat)))];
  const rows = D.epgChannels.map((c, i) => ({
    ...c,
    ri: i
  })).filter(c => grp === "All" || c.cat === grp);
  // focused-program info bar — full details for the cell under focus (10-foot
  // pattern: no tooltips on TV). Sticky: keeps the last focused program.
  const endOf = (t, dur) => {
    const [h, m] = t.split(":").map(Number);
    const e = h * 60 + m + dur;
    return `${String(Math.floor(e / 60) % 24).padStart(2, "0")}:${String(e % 60).padStart(2, "0")}`;
  };
  const [info, setInfo] = React.useState(() => ({
    chan: D.epgChannels[3],
    p: D.epgRows[3].find(p => p[3] === "now")
  }));
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "26px 0 40px"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 16,
      padding: "0 var(--safe-x) 20px"
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "var(--text-display)",
      color: "var(--text-primary)",
      letterSpacing: "var(--ls-tight)"
    }
  }, "TV Guide"), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), ["Yesterday", "Today", "Tomorrow"].map(d => /*#__PURE__*/React.createElement(Chip, {
    key: d,
    selected: d === day,
    onClick: () => setDay(d)
  }, d)), /*#__PURE__*/React.createElement(Chip, {
    icon: /*#__PURE__*/React.createElement(Icon, {
      name: "refresh-cw",
      size: 16
    })
  }, "Refresh EPG")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      padding: "0 var(--safe-x) 18px",
      flexWrap: "wrap"
    }
  }, groups.map(g => {
    const on = g === grp;
    const n = g === "All" ? D.epgChannels.length : D.epgChannels.filter(c => c.cat === g).length;
    return /*#__PURE__*/React.createElement("button", {
      key: g,
      onClick: () => setGrp(g),
      style: {
        display: "inline-flex",
        alignItems: "center",
        gap: 9,
        height: 44,
        padding: "0 18px",
        borderRadius: "var(--r-pill)",
        cursor: "pointer",
        border: "none",
        font: "var(--text-label)",
        transition: "var(--tr-color)",
        background: on ? "var(--accent)" : "var(--surface-2)",
        color: on ? "var(--accent-fg)" : "var(--text-secondary)",
        outline: on ? "1px solid var(--accent)" : "1px solid var(--border-subtle)",
        outlineOffset: -1,
        boxShadow: on ? "var(--glow-accent)" : "none"
      }
    }, /*#__PURE__*/React.createElement(Icon, {
      name: GRP_ICON[g] || "folder",
      size: 18,
      color: on ? "var(--accent-fg)" : "var(--text-tertiary)"
    }), g === "All" ? "All channels" : g, /*#__PURE__*/React.createElement("span", {
      style: {
        font: "var(--fw-bold) var(--fs-micro)/1 var(--font-mono)",
        opacity: 0.75
      }
    }, n));
  })), info && info.p && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 14,
      margin: "0 var(--safe-x) 18px",
      padding: "12px 16px",
      background: "var(--surface-1)",
      border: "1px solid var(--border-subtle)",
      borderRadius: "var(--r-md)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 44,
      height: 44,
      borderRadius: "var(--r-xs)",
      background: "var(--surface-3)",
      display: "grid",
      placeItems: "center",
      font: "var(--fw-bold) 13px/1 var(--font-display)",
      color: "var(--text-primary)",
      flex: "0 0 auto"
    }
  }, info.chan.logo), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0,
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--fw-semibold) var(--fs-title)/1.15 var(--font-display)",
      color: "var(--text-primary)",
      letterSpacing: "var(--ls-tight)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, info.p[1]), info.p[3] === "now" && /*#__PURE__*/React.createElement("span", {
    style: {
      padding: "3px 7px",
      borderRadius: "var(--r-xs)",
      background: "var(--live)",
      color: "#fff",
      flex: "0 0 auto",
      font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)"
    }
  }, "LIVE"), info.p[3] === "catchup" && /*#__PURE__*/React.createElement("span", {
    style: {
      padding: "3px 7px",
      borderRadius: "var(--r-xs)",
      background: "rgba(34,197,94,0.14)",
      border: "1px solid rgba(34,197,94,0.4)",
      color: "var(--green-400)",
      flex: "0 0 auto",
      font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)"
    }
  }, "CATCH-UP")), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 3,
      font: "var(--text-caption)",
      color: "var(--text-secondary)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, info.chan.name, " \xB7 ", /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-mono)"
    }
  }, info.p[0], " \u2013 ", endOf(info.p[0], info.p[2])), " \xB7 ", info.p[2], " min")), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-caption)",
      color: "var(--text-tertiary)",
      flex: "0 0 auto"
    }
  }, "OK \xB7 Watch")), /*#__PURE__*/React.createElement("div", {
    style: {
      overflowX: "auto",
      padding: "0 var(--safe-x)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 12 * 30 * PX + 220
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      position: "sticky",
      top: 0,
      zIndex: 2
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: "var(--guide-chan-w)",
      flex: "0 0 auto"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flex: 1
    }
  }, times.map(t => /*#__PURE__*/React.createElement("div", {
    key: t,
    style: {
      width: 30 * PX,
      flex: "0 0 auto",
      font: "var(--text-mono)",
      fontSize: 13,
      color: "var(--text-tertiary)",
      padding: "0 0 12px 4px",
      borderLeft: "1px solid var(--border-subtle)"
    }
  }, t)))), /*#__PURE__*/React.createElement("div", {
    key: grp,
    className: "cat-panel",
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 8
    }
  }, rows.map(chan => /*#__PURE__*/React.createElement("div", {
    key: chan.ri,
    style: {
      display: "flex",
      gap: 8,
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: "var(--guide-chan-w)",
      flex: "0 0 auto",
      display: "flex",
      alignItems: "center",
      gap: 12,
      padding: "0 12px",
      height: "var(--guide-row-h)",
      background: "var(--surface-1)",
      borderRadius: "var(--r-sm)",
      border: "1px solid var(--border-subtle)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 42,
      height: 42,
      borderRadius: "var(--r-xs)",
      background: "var(--surface-3)",
      display: "grid",
      placeItems: "center",
      font: "var(--fw-bold) 13px/1 var(--font-display)",
      color: "var(--text-primary)",
      flex: "0 0 auto"
    }
  }, chan.logo), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--text-mono)",
      fontSize: 12,
      color: "var(--text-tertiary)"
    }
  }, chan.num), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--fw-semibold) 14px/1.1 var(--font-body)",
      color: "var(--text-primary)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, chan.name))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 6,
      flex: 1
    }
  }, D.epgRows[chan.ri].map((p, ci) => /*#__PURE__*/React.createElement(GuideCell, {
    key: ci,
    time: p[0],
    title: p[1],
    width: p[2] * PX - 6,
    now: p[3] === "now",
    live: p[3] === "now" && chan.ri === 3,
    catchup: p[3] === "catchup",
    progress: p[3] === "now" ? 55 : 0,
    onFocusChange: on => on && setInfo({
      chan,
      p
    }),
    onClick: () => openPlayer(D.liveNow[Math.min(chan.ri, D.liveNow.length - 1)])
  })))))))));
}
window.Guide = Guide;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/are-tv/screens/Guide.jsx", error: String((e && e.message) || e) }); }

// ui_kits/are-tv/screens/Home.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
// Home dashboard — hero + rails (continue watching, live now, movies, series, AI recs)
function Home({
  openDetail,
  openPlayer
}) {
  const C = window.AREIptvDesignSystem_632b75,
    D = window.AREDATA;
  const {
    Hero,
    Rail,
    ContinueCard,
    ChannelTile,
    PosterTile,
    CategoryCard,
    Badge,
    Button,
    Icon
  } = C;
  const posterBadges = b => b ? [/*#__PURE__*/React.createElement(Badge, {
    key: "b",
    tone: b === "NEW" ? "new" : "quality"
  }, b)] : [];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      paddingTop: 28,
      paddingBottom: 40
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "0 var(--safe-x) 12px"
    }
  }, /*#__PURE__*/React.createElement(Hero, _extends({}, D.featured, {
    height: 460,
    badges: [/*#__PURE__*/React.createElement(Badge, {
      key: "l",
      tone: "live",
      glow: true
    }, "LIVE"), /*#__PURE__*/React.createElement(Badge, {
      key: "q",
      tone: "quality"
    }, "4K HDR")],
    actions: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(Button, {
      size: "lg",
      icon: /*#__PURE__*/React.createElement(Icon, {
        name: "play",
        size: 22
      }),
      onClick: () => openPlayer(D.liveNow[1])
    }, "Watch live"), /*#__PURE__*/React.createElement(Button, {
      variant: "secondary",
      size: "lg",
      icon: /*#__PURE__*/React.createElement(Icon, {
        name: "info",
        size: 20
      })
    }, "More info"))
  }))), /*#__PURE__*/React.createElement(Rail, {
    title: "Continue watching",
    seeAll: true
  }, D.continueWatching.map((c, i) => /*#__PURE__*/React.createElement(ContinueCard, _extends({
    key: i
  }, c, {
    onClick: () => openPlayer({
      channel: c.title,
      now: c.meta
    })
  })))), /*#__PURE__*/React.createElement(Rail, {
    title: "Live now",
    seeAll: true
  }, D.liveNow.map((c, i) => /*#__PURE__*/React.createElement(ChannelTile, _extends({
    key: i
  }, c, {
    onClick: () => openPlayer(c)
  })))), /*#__PURE__*/React.createElement(Rail, {
    title: "Browse by category",
    seeAll: true
  }, D.browseCats.map((c, i) => /*#__PURE__*/React.createElement(CategoryCard, _extends({
    key: i
  }, c, {
    width: 272
  })))), /*#__PURE__*/React.createElement(Rail, {
    title: "Recommended for you",
    smart: true,
    seeAll: true
  }, D.recommended.map(({
    smart,
    ...m
  }, i) => /*#__PURE__*/React.createElement(PosterTile, _extends({
    key: i
  }, m, {
    onClick: () => openDetail(m)
  })))), /*#__PURE__*/React.createElement(Rail, {
    title: "Movies",
    seeAll: true
  }, D.movies.map((m, i) => /*#__PURE__*/React.createElement(PosterTile, _extends({
    key: i
  }, m, {
    badges: posterBadges(m.badge),
    onClick: () => openDetail(m)
  })))), /*#__PURE__*/React.createElement(Rail, {
    title: "Series",
    seeAll: true
  }, D.series.map((m, i) => /*#__PURE__*/React.createElement(PosterTile, _extends({
    key: i
  }, m, {
    badges: posterBadges(m.badge),
    onClick: () => openDetail(m)
  })))));
}
window.Home = Home;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/are-tv/screens/Home.jsx", error: String((e && e.message) || e) }); }

// ui_kits/are-tv/screens/Live.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
// Live TV — category filter column (CategoryRow) + channel grid, with an animated
// transition that re-plays when you switch category (panel fade + staggered tiles).
function Live({
  openPlayer
}) {
  const C = window.AREIptvDesignSystem_632b75,
    D = window.AREDATA;
  const {
    CategoryRow,
    ChannelTile,
    Badge,
    Icon
  } = C;
  const [catIdx, setCatIdx] = React.useState(0);
  const cat = D.liveCats[catIdx];
  const channels = cat.all ? D.liveChannelPool : D.liveChannelPool.filter(c => c.cat === cat.name);
  const list = channels.length ? channels : D.liveChannelPool.slice(0, 6);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "26px 0 40px"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "0 var(--safe-x)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "baseline",
      gap: 12,
      marginBottom: 22
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "var(--text-display)",
      color: "var(--text-primary)",
      letterSpacing: "var(--ls-tight)"
    }
  }, "Live TV"), /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 7,
      padding: "5px 10px",
      borderRadius: "var(--r-pill)",
      background: "rgba(239,68,68,0.14)",
      border: "1px solid rgba(239,68,68,0.4)",
      color: "var(--red-400)",
      font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 8,
      height: 8,
      borderRadius: "50%",
      background: "var(--live)",
      boxShadow: "var(--glow-live)"
    }
  }), "ON AIR NOW")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 32,
      alignItems: "flex-start"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 300,
      flex: "0 0 auto",
      position: "sticky",
      top: 96,
      display: "flex",
      flexDirection: "column",
      gap: 4
    }
  }, /*#__PURE__*/React.createElement("p", {
    style: {
      font: "var(--fw-bold) 11px/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)",
      textTransform: "uppercase",
      color: "var(--text-tertiary)",
      margin: "0 0 8px",
      padding: "0 16px"
    }
  }, "Channel groups"), D.liveCats.map((c, i) => /*#__PURE__*/React.createElement(CategoryRow, {
    key: c.name,
    name: c.name,
    count: c.count,
    kind: c.kind,
    smart: c.smart,
    active: i === catIdx,
    onClick: () => setCatIdx(i)
  }))), /*#__PURE__*/React.createElement("div", {
    key: catIdx,
    className: "cat-panel",
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      marginBottom: 18
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      display: "grid",
      placeItems: "center",
      width: 40,
      height: 40,
      borderRadius: "var(--r-sm)",
      background: "var(--surface-2)",
      border: "1px solid var(--border-subtle)",
      color: "var(--accent-hover)"
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "radio",
    size: 22
  })), /*#__PURE__*/React.createElement("h2", {
    style: {
      font: "var(--text-h2)",
      color: "var(--text-primary)",
      letterSpacing: "var(--ls-tight)"
    }
  }, cat.name), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-mono)",
      color: "var(--text-tertiary)"
    }
  }, cat.count.toLocaleString(), " channels")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fill, minmax(250px, 1fr))",
      gap: 18
    }
  }, list.map((c, i) => /*#__PURE__*/React.createElement("div", {
    key: c.number,
    className: "cat-tile",
    style: {
      animationDelay: `${i * 34}ms`
    }
  }, /*#__PURE__*/React.createElement(ChannelTile, _extends({}, c, {
    width: "100%",
    onClick: () => openPlayer(c)
  })))))))));
}
window.Live = Live;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/are-tv/screens/Live.jsx", error: String((e && e.message) || e) }); }

// ui_kits/are-tv/screens/LivePlayer.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
// Live TV player — fullscreen video with the glass HUD, plus a "now playing"
// channel strip that slides up (mini-EPG) over the video.
function LivePlayer({
  channel,
  onClose
}) {
  const C = window.AREIptvDesignSystem_632b75,
    D = window.AREDATA;
  const {
    PlayerControls,
    IconButton,
    Icon,
    ChannelTile
  } = C;
  const [strip, setStrip] = React.useState(false);
  const ch = channel || D.liveNow[1];
  const initials = (ch.channel || "F1").replace(/\s?HD$/i, "").split(" ").slice(0, 2).map(w => w[0]).join("").toUpperCase();
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      background: "#000",
      zIndex: 40,
      overflow: "hidden"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      background: `center/cover no-repeat url(${D.wide("silverstone-live")})`
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      background: "linear-gradient(to top, rgba(6,7,10,.7), rgba(6,7,10,0) 45%)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 24,
      left: 28,
      right: 28,
      display: "flex",
      alignItems: "center",
      gap: 14,
      zIndex: 3
    }
  }, /*#__PURE__*/React.createElement(IconButton, {
    label: "Back",
    variant: "glass",
    onClick: onClose
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "arrow-left"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement(IconButton, {
    label: "Channels",
    variant: "glass",
    onClick: () => setStrip(s => !s)
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "list"
  })), /*#__PURE__*/React.createElement(IconButton, {
    label: "Settings",
    variant: "glass"
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "settings"
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      left: 0,
      right: 0,
      bottom: strip ? 220 : -260,
      transition: "bottom var(--dur-base) var(--ease-out)",
      zIndex: 3,
      padding: "0 28px"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 20,
      overflow: "hidden",
      padding: "10px 0"
    }
  }, D.liveNow.map((c, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      width: 300,
      flex: "0 0 auto"
    }
  }, /*#__PURE__*/React.createElement(ChannelTile, _extends({}, c, {
    width: "100%"
  })))))), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      left: 28,
      right: 28,
      bottom: 24,
      zIndex: 3
    }
  }, /*#__PURE__*/React.createElement(PlayerControls, {
    title: "British Grand Prix",
    subtitle: `${ch.channel || "Sky Sports F1"} · Now · Lap 34 of 52`,
    live: true,
    playing: true,
    position: 62,
    buffered: 84,
    elapsed: "1:24:08",
    total: "2:10:00",
    channelLogoInitials: initials
  })));
}
window.LivePlayer = LivePlayer;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/are-tv/screens/LivePlayer.jsx", error: String((e && e.message) || e) }); }

// ui_kits/are-tv/screens/MultiView.jsx
try { (() => {
// Multi-view — 2 or 4 simultaneous live streams; one is the active (audio) pane.
function MultiView({
  onClose
}) {
  const C = window.AREIptvDesignSystem_632b75,
    D = window.AREDATA;
  const {
    IconButton,
    Icon,
    Badge,
    StreamHealth,
    Chip
  } = C;
  const [count, setCount] = React.useState(4);
  const [active, setActive] = React.useState(0);
  const panes = [{
    title: "British Grand Prix",
    ch: "Sky Sports F1",
    seed: "mv-f1",
    health: "stable"
  }, {
    title: "Lakers @ Celtics",
    ch: "ESPN",
    seed: "mv-nba",
    health: "moderate"
  }, {
    title: "Live Football",
    ch: "Sky Sports Main",
    seed: "mv-football",
    health: "stable"
  }, {
    title: "Global News Hour",
    ch: "CNN",
    seed: "mv-cnn",
    health: "stable"
  }].slice(0, count);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      zIndex: 40,
      background: "#000",
      display: "flex",
      flexDirection: "column"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 14,
      padding: "18px 28px",
      zIndex: 2
    }
  }, /*#__PURE__*/React.createElement(IconButton, {
    label: "Back",
    variant: "glass",
    onClick: onClose
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "arrow-left"
  })), /*#__PURE__*/React.createElement("h2", {
    style: {
      font: "var(--text-h2)",
      color: "#fff"
    }
  }, "Multi-view"), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement(Chip, {
    icon: /*#__PURE__*/React.createElement(Icon, {
      name: "grid-2x2",
      size: 16
    }),
    selected: count === 4,
    onClick: () => setCount(4)
  }, "4-up"), /*#__PURE__*/React.createElement(Chip, {
    icon: /*#__PURE__*/React.createElement(Icon, {
      name: "columns-2",
      size: 16
    }),
    selected: count === 2,
    onClick: () => setCount(2)
  }, "2-up")), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: "grid",
      gap: 10,
      padding: "0 28px 28px",
      gridTemplateColumns: count === 2 ? "1fr 1fr" : "1fr 1fr",
      gridTemplateRows: count === 2 ? "1fr" : "1fr 1fr"
    }
  }, panes.map((p, i) => {
    const on = i === active;
    return /*#__PURE__*/React.createElement("div", {
      key: i,
      onClick: () => setActive(i),
      style: {
        position: "relative",
        borderRadius: "var(--r-md)",
        overflow: "hidden",
        cursor: "pointer",
        background: `center/cover no-repeat url(${D.wide(p.seed)})`,
        outline: on ? "3px solid var(--accent)" : "1px solid var(--border-default)",
        boxShadow: on ? "var(--glow-accent)" : "none"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        position: "absolute",
        inset: 0,
        background: "linear-gradient(to top, rgba(6,7,10,.85), rgba(6,7,10,0) 55%)"
      }
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        position: "absolute",
        top: 12,
        left: 12,
        display: "flex",
        gap: 8
      }
    }, /*#__PURE__*/React.createElement(Badge, {
      tone: "live",
      glow: true
    }, "LIVE"), on && /*#__PURE__*/React.createElement(Badge, {
      tone: "new"
    }, /*#__PURE__*/React.createElement(Icon, {
      name: "volume-2",
      size: 12
    }), " AUDIO")), /*#__PURE__*/React.createElement("div", {
      style: {
        position: "absolute",
        top: 12,
        right: 12
      }
    }, /*#__PURE__*/React.createElement(StreamHealth, {
      level: p.health,
      label: false
    })), /*#__PURE__*/React.createElement("div", {
      style: {
        position: "absolute",
        left: 16,
        bottom: 14
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "var(--text-h3)",
        color: "#fff"
      }
    }, p.title), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "var(--text-caption)",
        color: "var(--ink-100)",
        marginTop: 3
      }
    }, p.ch)));
  })));
}
window.MultiView = MultiView;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/are-tv/screens/MultiView.jsx", error: String((e && e.message) || e) }); }

// ui_kits/are-tv/screens/Onboarding.jsx
try { (() => {
// Onboarding wizard — guided playlist add: Source → Credentials → EPG → Confirm.
// Supports both an M3U/URL playlist and Xtream Codes (host/user/pass) params.
function Onboarding({
  onDone
}) {
  const C = window.AREIptvDesignSystem_632b75,
    D = window.AREDATA;
  const {
    StepIndicator,
    TextField,
    Button,
    Switch,
    Icon,
    Badge
  } = C;
  const steps = ["Source", "Credentials", "EPG", "Confirm"];
  const [step, setStep] = React.useState(0);
  const [source, setSource] = React.useState("xtream"); // "m3u" | "xtream"
  const [epgAuto, setEpgAuto] = React.useState(true);
  const SourceCard = ({
    id,
    icon,
    title,
    desc
  }) => {
    const on = source === id;
    return /*#__PURE__*/React.createElement("button", {
      onClick: () => setSource(id),
      style: {
        textAlign: "left",
        flex: 1,
        padding: 24,
        borderRadius: "var(--r-lg)",
        cursor: "pointer",
        background: on ? "var(--accent-wash)" : "var(--surface-1)",
        border: on ? "2px solid var(--accent)" : "1px solid var(--border-default)",
        boxShadow: on ? "var(--glow-accent)" : "none",
        transition: "var(--tr-color)"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        width: 52,
        height: 52,
        borderRadius: "var(--r-md)",
        background: on ? "var(--accent)" : "var(--surface-3)",
        display: "grid",
        placeItems: "center",
        marginBottom: 16
      }
    }, /*#__PURE__*/React.createElement(Icon, {
      name: icon,
      size: 26,
      color: on ? "#fff" : "var(--text-secondary)"
    })), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "var(--text-h3)",
        color: "var(--text-primary)",
        marginBottom: 6
      }
    }, title), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "var(--text-caption)",
        color: "var(--text-tertiary)",
        lineHeight: "var(--lh-normal)"
      }
    }, desc));
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      zIndex: 60,
      background: "radial-gradient(1200px 600px at 20% -10%, rgba(59,130,246,.18), transparent), var(--bg-base)",
      overflowY: "auto"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      maxWidth: 900,
      margin: "0 auto",
      padding: "56px 40px"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 14,
      marginBottom: 8
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 44,
      height: 44,
      borderRadius: "var(--r-sm)",
      background: "var(--accent)",
      display: "grid",
      placeItems: "center",
      font: "var(--fw-bold) 20px/1 var(--font-display)",
      color: "#fff",
      boxShadow: "var(--glow-accent)"
    }
  }, "A"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--fw-bold) var(--fs-h2)/1 var(--font-display)",
      color: "var(--text-primary)"
    }
  }, "ARE ", /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--text-tertiary)",
      fontWeight: 500
    }
  }, "iptv"))), /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "var(--text-display)",
      color: "var(--text-primary)",
      letterSpacing: "var(--ls-tight)",
      margin: "18px 0 6px"
    }
  }, "Add a playlist"), /*#__PURE__*/React.createElement("p", {
    style: {
      font: "var(--text-body)",
      color: "var(--text-secondary)",
      marginBottom: 36
    }
  }, "Bring your own IPTV subscription. ARE iptv never stores your credentials off-device."), /*#__PURE__*/React.createElement(StepIndicator, {
    current: step,
    steps: steps
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 40,
      minHeight: 260
    }
  }, step === 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 20
    }
  }, /*#__PURE__*/React.createElement(SourceCard, {
    id: "xtream",
    icon: "server",
    title: "Xtream Codes",
    desc: "Enter your portal host, username and password. Best for live TV + VOD with automatic EPG."
  }), /*#__PURE__*/React.createElement(SourceCard, {
    id: "m3u",
    icon: "link",
    title: "M3U / URL playlist",
    desc: "Paste a single M3U or M3U-plus link. Add an XMLTV EPG URL separately if you have one."
  })), step === 1 && /*#__PURE__*/React.createElement("div", {
    style: {
      maxWidth: 640
    }
  }, /*#__PURE__*/React.createElement(TextField, {
    label: "Portal name",
    placeholder: "Living room",
    value: "Living room",
    style: {
      marginBottom: 20
    }
  }), source === "xtream" ? /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(TextField, {
    label: "Server URL",
    mono: true,
    prefix: "http://",
    placeholder: "portal.example.tv:8080",
    icon: /*#__PURE__*/React.createElement(Icon, {
      name: "server",
      size: 18
    }),
    style: {
      marginBottom: 20
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: 20
    }
  }, /*#__PURE__*/React.createElement(TextField, {
    label: "Username",
    mono: true,
    value: "are_user_01"
  }), /*#__PURE__*/React.createElement(TextField, {
    label: "Password",
    mono: true,
    type: "password",
    value: "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022"
  }))) : /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(TextField, {
    label: "M3U playlist URL",
    mono: true,
    prefix: "http://",
    placeholder: "host:8080/get.php?type=m3u_plus&output=ts",
    icon: /*#__PURE__*/React.createElement(Icon, {
      name: "link",
      size: 18
    })
  }))), step === 2 && /*#__PURE__*/React.createElement("div", {
    style: {
      maxWidth: 640
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      padding: "18px 20px",
      borderRadius: "var(--r-md)",
      background: "var(--surface-1)",
      border: "1px solid var(--border-default)",
      marginBottom: 18
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--text-label)",
      color: "var(--text-primary)"
    }
  }, "Auto-match EPG & logos"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--text-caption)",
      color: "var(--text-tertiary)",
      marginTop: 4
    }
  }, "Fetch guide data, channel logos and posters automatically.")), /*#__PURE__*/React.createElement(Switch, {
    checked: epgAuto,
    onChange: setEpgAuto
  })), /*#__PURE__*/React.createElement(TextField, {
    label: "XMLTV EPG URL (optional)",
    mono: true,
    prefix: "http://",
    placeholder: "portal.example.tv/xmltv.php?username=\u2026",
    disabled: epgAuto,
    helper: epgAuto ? "Disabled while auto-match is on." : "Provide your own guide source."
  })), step === 3 && /*#__PURE__*/React.createElement("div", {
    style: {
      maxWidth: 640
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24,
      borderRadius: "var(--r-lg)",
      background: "var(--surface-1)",
      border: "1px solid var(--border-default)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      marginBottom: 20
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "check-circle-2",
    size: 24,
    color: "var(--green-500)"
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-h3)",
      color: "var(--text-primary)"
    }
  }, "Ready to add"), /*#__PURE__*/React.createElement(Badge, {
    tone: "catchup",
    style: {
      marginLeft: "auto"
    }
  }, "842 CHANNELS")), [["Portal", "Living room"], ["Source", source === "xtream" ? "Xtream Codes" : "M3U playlist"], ["Server", "portal.example.tv:8080"], ["EPG", epgAuto ? "Auto-matched" : "Custom XMLTV"], ["VOD", "1,204 movies · 386 series"]].map(([k, v]) => /*#__PURE__*/React.createElement("div", {
    key: k,
    style: {
      display: "flex",
      justifyContent: "space-between",
      padding: "11px 0",
      borderBottom: "1px solid var(--border-subtle)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--text-caption)",
      color: "var(--text-tertiary)"
    }
  }, k), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--fw-medium) var(--fs-caption)/1 var(--font-mono)",
      color: "var(--text-primary)"
    }
  }, v)))))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 14,
      marginTop: 36
    }
  }, step > 0 && /*#__PURE__*/React.createElement(Button, {
    variant: "ghost",
    size: "lg",
    onClick: () => setStep(step - 1)
  }, "Back"), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement(Button, {
    variant: "ghost",
    size: "lg",
    onClick: onDone
  }, "Skip for now"), step < 3 ? /*#__PURE__*/React.createElement(Button, {
    size: "lg",
    trailingIcon: /*#__PURE__*/React.createElement(Icon, {
      name: "chevron-right",
      size: 20
    }),
    onClick: () => setStep(step + 1)
  }, "Continue") : /*#__PURE__*/React.createElement(Button, {
    size: "lg",
    icon: /*#__PURE__*/React.createElement(Icon, {
      name: "check",
      size: 20
    }),
    onClick: onDone
  }, "Add playlist"))));
}
window.Onboarding = Onboarding;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/are-tv/screens/Onboarding.jsx", error: String((e && e.message) || e) }); }

// ui_kits/are-tv/screens/Search.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
// Universal search — one query across live, movies, series, catch-up. Shows a
// simulated keyboard-driven query with typed value and grouped results.
function Search({
  openDetail,
  openPlayer
}) {
  const C = window.AREIptvDesignSystem_632b75,
    D = window.AREDATA;
  const {
    TextField,
    Chip,
    PosterTile,
    ChannelTile,
    Badge,
    Icon
  } = C;
  const [q, setQ] = React.useState("f1");
  const [scope, setScope] = React.useState("All");
  const scopes = ["All", "Live TV", "Movies", "Series", "Catch-up"];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "26px 0 40px"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "0 var(--safe-x)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      maxWidth: 760
    }
  }, /*#__PURE__*/React.createElement(TextField, {
    value: q,
    onChange: e => setQ(e.target.value),
    placeholder: "Search channels, movies, actors\u2026",
    icon: /*#__PURE__*/React.createElement(Icon, {
      name: "search",
      size: 20
    }),
    focused: true
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      margin: "20px 0 30px"
    }
  }, scopes.map(s => /*#__PURE__*/React.createElement(Chip, {
    key: s,
    selected: s === scope,
    onClick: () => setScope(s)
  }, s))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      marginBottom: 16
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)",
      textTransform: "uppercase",
      color: "var(--violet-400)"
    }
  }, "Universal results"), /*#__PURE__*/React.createElement(Badge, {
    tone: "smart"
  }, "AI RANKED")), /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "var(--text-h3)",
      color: "var(--text-secondary)",
      margin: "6px 0 16px"
    }
  }, "Live & catch-up"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 20,
      overflow: "hidden",
      marginBottom: 30
    }
  }, D.liveNow.slice(1, 4).map((c, i) => /*#__PURE__*/React.createElement(ChannelTile, _extends({
    key: i
  }, c, {
    onClick: () => openPlayer(c)
  })))), /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "var(--text-h3)",
      color: "var(--text-secondary)",
      margin: "6px 0 16px"
    }
  }, "Movies & series"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))",
      gap: 22
    }
  }, D.recommended.map((m, i) => /*#__PURE__*/React.createElement(PosterTile, _extends({
    key: i
  }, m, {
    width: "100%",
    onClick: () => openDetail(m)
  }))))));
}
window.Search = Search;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/are-tv/screens/Search.jsx", error: String((e && e.message) || e) }); }

// ui_kits/are-tv/screens/Settings.jsx
try { (() => {
// Settings — playback, appearance (theme), parental, playlists. Wired to app theme.
function Settings({
  theme,
  setTheme
}) {
  const C = window.AREIptvDesignSystem_632b75,
    D = window.AREDATA;
  const {
    Switch,
    Icon,
    Badge,
    Button,
    Chip
  } = C;
  const [ext, setExt] = React.useState("ExoPlayer");
  const Row = ({
    icon,
    title,
    desc,
    control
  }) => /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 16,
      padding: "18px 20px",
      borderBottom: "1px solid var(--border-subtle)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 42,
      height: 42,
      borderRadius: "var(--r-sm)",
      background: "var(--surface-2)",
      display: "grid",
      placeItems: "center",
      flex: "0 0 auto"
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: icon,
    size: 22,
    color: "var(--text-secondary)"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--text-label)",
      color: "var(--text-primary)"
    }
  }, title), desc && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--text-caption)",
      color: "var(--text-tertiary)",
      marginTop: 3
    }
  }, desc)), control);
  const Section = ({
    title,
    children
  }) => /*#__PURE__*/React.createElement("div", {
    style: {
      marginBottom: 34
    }
  }, /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "var(--fw-bold) var(--fs-micro)/1 var(--font-body)",
      letterSpacing: "var(--ls-caps)",
      textTransform: "uppercase",
      color: "var(--text-tertiary)",
      marginBottom: 12
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      background: "var(--surface-1)",
      borderRadius: "var(--r-lg)",
      border: "1px solid var(--border-subtle)",
      overflow: "hidden"
    }
  }, children));
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "26px var(--safe-x) 40px",
      maxWidth: 900
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "var(--text-display)",
      color: "var(--text-primary)",
      letterSpacing: "var(--ls-tight)",
      marginBottom: 30
    }
  }, "Settings"), /*#__PURE__*/React.createElement(Section, {
    title: "Appearance"
  }, /*#__PURE__*/React.createElement(Row, {
    icon: "moon",
    title: "Dark theme",
    desc: "Recommended for lean-back viewing.",
    control: /*#__PURE__*/React.createElement(Switch, {
      checked: theme === "dark",
      onChange: v => setTheme(v ? "dark" : "light")
    })
  }), /*#__PURE__*/React.createElement(Row, {
    icon: "zap",
    title: "Reduce motion",
    desc: "Softer focus animations.",
    control: /*#__PURE__*/React.createElement(Switch, {
      checked: false,
      onChange: () => {}
    })
  })), /*#__PURE__*/React.createElement(Section, {
    title: "Playback"
  }, /*#__PURE__*/React.createElement(Row, {
    icon: "gauge",
    title: "Hardware decoding",
    desc: "H.264 / H.265 / AV1 \xB7 HDR10 \xB7 Dolby Vision",
    control: /*#__PURE__*/React.createElement(Switch, {
      checked: true,
      onChange: () => {}
    })
  }), /*#__PURE__*/React.createElement(Row, {
    icon: "captions",
    title: "Autoplay next episode",
    control: /*#__PURE__*/React.createElement(Switch, {
      checked: true,
      onChange: () => {}
    })
  }), /*#__PURE__*/React.createElement(Row, {
    icon: "picture-in-picture-2",
    title: "Picture-in-picture",
    desc: "Keep playing while you browse.",
    control: /*#__PURE__*/React.createElement(Switch, {
      checked: true,
      onChange: () => {}
    })
  }), /*#__PURE__*/React.createElement(Row, {
    icon: "external-link",
    title: "External player",
    desc: "Hand off to another player per content type.",
    control: /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        gap: 8
      }
    }, ["ExoPlayer", "VLC", "MX"].map(p => /*#__PURE__*/React.createElement(Chip, {
      key: p,
      size: "sm",
      selected: p === ext,
      onClick: () => setExt(p)
    }, p)))
  })), /*#__PURE__*/React.createElement(Section, {
    title: "Parental controls"
  }, /*#__PURE__*/React.createElement(Row, {
    icon: "lock",
    title: "Lock adult categories",
    desc: "Require a PIN to open restricted content.",
    control: /*#__PURE__*/React.createElement(Switch, {
      checked: true,
      onChange: () => {}
    })
  }), /*#__PURE__*/React.createElement(Row, {
    icon: "key-round",
    title: "Change PIN",
    control: /*#__PURE__*/React.createElement(Button, {
      variant: "secondary",
      size: "sm"
    }, "Change")
  })), /*#__PURE__*/React.createElement(Section, {
    title: "Playlists & sync"
  }, /*#__PURE__*/React.createElement(Row, {
    icon: "server",
    title: "Living room \xB7 Xtream",
    desc: "842 channels \xB7 EPG auto-matched",
    control: /*#__PURE__*/React.createElement(Badge, {
      tone: "catchup"
    }, "ACTIVE")
  }), /*#__PURE__*/React.createElement(Row, {
    icon: "plus",
    title: "Add playlist",
    desc: "M3U link or Xtream Codes login.",
    control: /*#__PURE__*/React.createElement(Button, {
      variant: "secondary",
      size: "sm"
    }, "Add")
  }), /*#__PURE__*/React.createElement(Row, {
    icon: "hard-drive-download",
    title: "Backup & restore",
    desc: "Export favorites, history and settings to a file \u2014 no account needed.",
    control: /*#__PURE__*/React.createElement(Button, {
      variant: "secondary",
      size: "sm"
    }, "Export")
  })), /*#__PURE__*/React.createElement(Section, {
    title: "About & support"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 20,
      alignItems: "center",
      padding: 22,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 260
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12,
      marginBottom: 10
    }
  }, /*#__PURE__*/React.createElement("img", {
    src: "../../assets/logo-mark.svg",
    width: 40,
    height: 40,
    alt: "",
    style: {
      borderRadius: "var(--r-sm)"
    }
  }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--fw-bold) var(--fs-title)/1 var(--font-display)",
      color: "var(--text-primary)"
    }
  }, "ARE iptv"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "var(--text-mono)",
      color: "var(--text-tertiary)",
      marginTop: 2
    }
  }, "v2.4.1 \xB7 free, no account"))), /*#__PURE__*/React.createElement("p", {
    style: {
      font: "var(--text-body)",
      color: "var(--text-secondary)",
      margin: 0,
      maxWidth: 440
    }
  }, "ARE iptv is free and always will be \u2014 no sign-up, no subscription, no ads. If it earns a place on your TV, you can chip in to keep it maintained.")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 10,
      minWidth: 220
    }
  }, /*#__PURE__*/React.createElement(Button, {
    size: "lg",
    icon: /*#__PURE__*/React.createElement(Icon, {
      name: "coffee",
      size: 20
    }),
    style: {
      background: "#ffdd00",
      color: "#0a0a0a"
    }
  }, "Buy me a coffee"), /*#__PURE__*/React.createElement(Button, {
    variant: "secondary",
    size: "lg",
    icon: /*#__PURE__*/React.createElement(Icon, {
      name: "heart",
      size: 18
    })
  }, "Donate with PayPal"))), /*#__PURE__*/React.createElement(Row, {
    icon: "star",
    title: "Rate ARE iptv",
    desc: "Leave a review on the store.",
    control: /*#__PURE__*/React.createElement(Button, {
      variant: "secondary",
      size: "sm"
    }, "Rate")
  }), /*#__PURE__*/React.createElement(Row, {
    icon: "shield-check",
    title: "Privacy",
    desc: "Playlists and credentials stay on this device. Nothing is sent to us.",
    control: /*#__PURE__*/React.createElement(Icon, {
      name: "chevron-right",
      size: 18,
      color: "var(--text-tertiary)"
    })
  }), /*#__PURE__*/React.createElement(Row, {
    icon: "file-text",
    title: "Open-source licenses",
    control: /*#__PURE__*/React.createElement(Icon, {
      name: "chevron-right",
      size: 18,
      color: "var(--text-tertiary)"
    })
  })));
}
window.Settings = Settings;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/are-tv/screens/Settings.jsx", error: String((e && e.message) || e) }); }

__ds_ns.CategoryCard = __ds_scope.CategoryCard;

__ds_ns.CategoryRow = __ds_scope.CategoryRow;

__ds_ns.Badge = __ds_scope.Badge;

__ds_ns.Button = __ds_scope.Button;

__ds_ns.Chip = __ds_scope.Chip;

__ds_ns.Icon = __ds_scope.Icon;

__ds_ns.IconButton = __ds_scope.IconButton;

__ds_ns.StepIndicator = __ds_scope.StepIndicator;

__ds_ns.Switch = __ds_scope.Switch;

__ds_ns.TextField = __ds_scope.TextField;

__ds_ns.GuideCell = __ds_scope.GuideCell;

__ds_ns.StreamHealth = __ds_scope.StreamHealth;

__ds_ns.ChannelTile = __ds_scope.ChannelTile;

__ds_ns.ContinueCard = __ds_scope.ContinueCard;

__ds_ns.Hero = __ds_scope.Hero;

__ds_ns.PosterTile = __ds_scope.PosterTile;

__ds_ns.Rail = __ds_scope.Rail;

__ds_ns.SidebarNav = __ds_scope.SidebarNav;

__ds_ns.Tabs = __ds_scope.Tabs;

__ds_ns.Dialog = __ds_scope.Dialog;

__ds_ns.PlayerControls = __ds_scope.PlayerControls;

})();
