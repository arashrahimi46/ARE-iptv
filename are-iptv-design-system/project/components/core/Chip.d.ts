import * as React from "react";
/** Filter / category pill with a toggled selected state. */
export interface ChipProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  children?: React.ReactNode;
  selected?: boolean;
  focused?: boolean;
  icon?: React.ReactNode;
  /** CSS color for a leading status dot. */
  dot?: string;
  size?: "sm" | "md";
}
export declare function Chip(props: ChipProps): JSX.Element;
