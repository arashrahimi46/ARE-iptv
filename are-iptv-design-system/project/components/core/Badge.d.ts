import * as React from "react";
/** Tiny wide-caps status overline (LIVE, 4K, NEW, CATCH-UP, SMART). */
export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  children?: React.ReactNode;
  tone?: "live" | "new" | "quality" | "catchup" | "smart" | "neutral";
  glow?: boolean;
}
export declare function Badge(props: BadgeProps): JSX.Element;
