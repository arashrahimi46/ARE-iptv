import * as React from "react";
/** Horizontal category tabs with a sliding underline. */
export interface TabItem { id: string; label: string; }
export interface TabsProps {
  items: TabItem[];
  active?: string;
  onSelect?: (id: string) => void;
}
export declare function Tabs(props: TabsProps): JSX.Element;
