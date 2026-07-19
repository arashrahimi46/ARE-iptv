import * as React from "react";
/**
 * Traffic-light stream-reliability indicator (a next-gen ARE iptv feature).
 * @startingPoint section="Guide" subtitle="Stream-health traffic light" viewport="700x120"
 */
export interface StreamHealthProps {
  level?: "stable" | "moderate" | "poor";
  label?: boolean;
  bitrate?: string;
  size?: "sm" | "md" | "lg";
}
export declare function StreamHealth(props: StreamHealthProps): JSX.Element;
