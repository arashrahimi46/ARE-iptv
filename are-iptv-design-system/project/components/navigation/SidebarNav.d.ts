import * as React from "react";
/** Left rail nav: collapses to icons, expands to labels. */
export interface NavItem { id: string; label: string; icon: string; }
export interface SidebarNavProps {
  items: NavItem[];
  active?: string;
  /** Force the expanded (labels visible) state. */
  expanded?: boolean;
  brand?: string;
  onSelect?: (id: string) => void;
}
export declare function SidebarNav(props: SidebarNavProps): JSX.Element;
