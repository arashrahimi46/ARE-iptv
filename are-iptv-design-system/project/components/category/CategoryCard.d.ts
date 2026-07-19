import * as React from "react";
/**
 * Folder-style browse tile for an IPTV category — the grouping every content
 * type shares (Live TV, Movies, Series, EPG). 2×2 artwork mosaic, name, count,
 * kind icon, optional SMART tag. TV-focusable (scale + accent glow).
 * @startingPoint section="Category" subtitle="Folder tile with poster mosaic & count" viewport="760x420"
 */
export interface CategoryCardProps {
  /** Category name, e.g. "Sports", "Action & Adventure". */
  name?: string;
  /** Item count in the category (number is locale-formatted). */
  count?: number | string;
  /** Content type — sets the fallback icon and count noun. */
  kind?: "live" | "tv" | "movies" | "series" | "guide" | "catchup" | "favorites" | "default";
  /** Up to 4 preview artwork URLs (poster/logo) for the mosaic. */
  posters?: string[];
  /** Override the count noun (defaults per kind, e.g. "channels", "movies"). */
  unit?: string;
  /** AI auto-organized group — shows a violet SMART tag. */
  smart?: boolean;
  /** Denser tile (16/7, smaller label) for large category walls. */
  compact?: boolean;
  width?: string | number;
  focused?: boolean;
  onClick?: () => void;
}
export declare function CategoryCard(props: CategoryCardProps): JSX.Element;
