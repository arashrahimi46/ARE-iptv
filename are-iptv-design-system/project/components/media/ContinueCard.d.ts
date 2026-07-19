import * as React from "react";
/** Landscape continue-watching tile with resume progress and play-on-focus. */
export interface ContinueCardProps {
  title: string;
  meta?: string;
  image?: string;
  progress?: number;
  remaining?: string;
  width?: number;
  focused?: boolean;
  onClick?: () => void;
}
export declare function ContinueCard(props: ContinueCardProps): JSX.Element;
