import * as React from "react";
/**
 * Glass transport HUD for live/VOD playback, with a TimeShift-aware seek bar.
 * @startingPoint section="Player" subtitle="Glass player HUD with TimeShift bar" viewport="900x260"
 */
export interface PlayerControlsProps {
  title?: string;
  subtitle?: string;
  live?: boolean;
  playing?: boolean;
  /** Playhead position 0–100. */
  position?: number;
  /** Buffered/timeshift extent 0–100. */
  buffered?: number;
  elapsed?: string;
  total?: string;
  channelLogoInitials?: string;
}
export declare function PlayerControls(props: PlayerControlsProps): JSX.Element;
