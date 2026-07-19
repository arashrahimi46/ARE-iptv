import * as React from "react";
/** Renders a Lucide glyph by kebab-case name. Needs the Lucide UMD script on the page. */
export interface IconProps extends React.HTMLAttributes<HTMLSpanElement> {
  /** Lucide icon name, kebab-case (e.g. "tv", "chevron-right", "picture-in-picture-2"). */
  name: string;
  size?: number;
  color?: string;
  strokeWidth?: number;
}
export declare function Icon(props: IconProps): JSX.Element;
