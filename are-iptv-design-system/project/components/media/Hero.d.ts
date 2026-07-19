import * as React from "react";
/**
 * Full-bleed featured banner with scrims, badges, synopsis and an action row.
 * @startingPoint section="Media" subtitle="Featured hero banner" viewport="900x520"
 */
export interface HeroProps {
  title: string;
  kicker?: string;
  meta?: string;
  synopsis?: string;
  image?: string;
  badges?: React.ReactNode[];
  /** Action row nodes (e.g. <Button>Play</Button>). */
  actions?: React.ReactNode;
  height?: number;
}
export declare function Hero(props: HeroProps): JSX.Element;
