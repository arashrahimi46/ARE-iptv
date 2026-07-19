import * as React from "react";
/** One program block in the EPG grid. Width is set by the caller (∝ duration).
 * Long titles: ellipsis at rest, marquee-scroll on focus. */
export interface GuideCellProps {
  title: string;
  time: string;
  live?: boolean;
  /** Currently-airing program (accent edge + progress). */
  now?: boolean;
  /** Catch-up available for this past program. */
  catchup?: boolean;
  progress?: number;
  focused?: boolean;
  width?: number;
  onClick?: () => void;
  /** Called with true/false as the cell gains/loses focus — drive a focused-program info bar with it. */
  onFocusChange?: (focused: boolean) => void;
}
export declare function GuideCell(props: GuideCellProps): JSX.Element;
