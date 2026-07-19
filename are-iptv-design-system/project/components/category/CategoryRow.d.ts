import * as React from "react";
/**
 * Compact category list row for the filter column down the left of Live TV,
 * Movies, Series and the EPG guide. Kind icon, name, count, chevron. Active =
 * accent wash + edge bar; focus (D-pad) lifts the surface with the accent ring.
 * @startingPoint section="Category" subtitle="Category filter list row" viewport="420x360"
 */
export interface CategoryRowProps {
  /** Category name. */
  name?: string;
  /** Item count (number is locale-formatted). */
  count?: number | string;
  /** Content type — sets the icon and count noun. */
  kind?: "live" | "tv" | "movies" | "series" | "guide" | "catchup" | "favorites" | "default";
  /** Override the count noun. */
  unit?: string;
  /** AI auto-organized group — shows a violet SMART tag. */
  smart?: boolean;
  /** Selected/current category. */
  active?: boolean;
  focused?: boolean;
  onClick?: () => void;
}
export declare function CategoryRow(props: CategoryRowProps): JSX.Element;
