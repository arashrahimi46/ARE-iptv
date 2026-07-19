import * as React from "react";
/**
 * Portrait VOD poster tile (movie/series) with TV focus, badges, rating & progress.
 * @startingPoint section="Media" subtitle="Portrait poster tile with focus glow" viewport="700x420"
 */
export interface PosterTileProps {
  title?: string;
  meta?: string;
  /** Poster art URL. Falls back to an initials chip. */
  image?: string;
  /** Array of <Badge> nodes overlaid top-left. */
  badges?: React.ReactNode[];
  /** Rating number shown top-right (e.g. 9.5). */
  rating?: number | string;
  /** 0–100 resume progress; omit to hide the bar. */
  progress?: number | null;
  width?: string | number;
  focused?: boolean;
  onClick?: () => void;
}
export declare function PosterTile(props: PosterTileProps): JSX.Element;
