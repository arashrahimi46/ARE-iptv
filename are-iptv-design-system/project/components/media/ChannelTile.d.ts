import * as React from "react";
/**
 * Logo-first live-TV tile (no stream preview — IPTV previews are unreliable):
 * channel logo chip over a subtle glow, plus an in-card info panel with the
 * now-playing program + progress, next up, and quality info
 * (resolution badge, codec, stream-health dot, catch-up, favorite).
 * @startingPoint section="Media" subtitle="Live channel tile with now-playing + quality info" viewport="700x360"
 */
export interface ChannelTileProps {
  channel: string;
  number?: string | number;
  logo?: string;
  now?: string;
  next?: string;
  progress?: number;
  health?: "stable" | "moderate" | "poor";
  /** Resolution badge, e.g. "4K" | "FHD" | "HD" | "SD" */
  quality?: string;
  /** Codec shown in the mono meta slot, e.g. "H.265" */
  codec?: string;
  /** Channel supports catch-up / TimeShift */
  catchup?: boolean;
  /** Favorited channel (small heart on the tile) */
  fav?: boolean;
  width?: string | number;
  focused?: boolean;
  onClick?: () => void;
}
export declare function ChannelTile(props: ChannelTileProps): JSX.Element;
