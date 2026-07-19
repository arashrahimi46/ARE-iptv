import * as React from "react";

/**
 * Primary action button, built for D-pad focus (glow ring + scale).
 * @startingPoint section="Core" subtitle="Action button with TV focus states" viewport="700x150"
 */
export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  children?: React.ReactNode;
  variant?: "primary" | "secondary" | "ghost" | "danger";
  size?: "sm" | "md" | "lg";
  /** Leading icon node (e.g. a Lucide glyph). */
  icon?: React.ReactNode;
  /** Trailing icon node. */
  trailingIcon?: React.ReactNode;
  full?: boolean;
  disabled?: boolean;
  /** Force the focused/glow state (for previews or controlled focus). */
  focused?: boolean;
}
export declare function Button(props: ButtonProps): JSX.Element;
