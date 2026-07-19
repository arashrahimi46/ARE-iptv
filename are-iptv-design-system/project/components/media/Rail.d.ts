import * as React from "react";
/** Titled horizontal row of tiles — the core Home building block. */
export interface RailProps {
  title: string;
  /** Show the violet SMART tag (AI-organized rail). */
  smart?: boolean;
  seeAll?: boolean;
  children?: React.ReactNode;
}
export declare function Rail(props: RailProps): JSX.Element;
