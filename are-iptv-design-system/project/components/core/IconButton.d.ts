import * as React from "react";
/** Square single-glyph control for player HUD, nav, and toolbars. */
export interface IconButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  children?: React.ReactNode;
  label: string;
  variant?: "solid" | "glass" | "ghost";
  size?: "sm" | "md" | "lg";
  active?: boolean;
  focused?: boolean;
  disabled?: boolean;
}
export declare function IconButton(props: IconButtonProps): JSX.Element;
