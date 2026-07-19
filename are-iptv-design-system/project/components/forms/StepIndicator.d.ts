import * as React from "react";
/** Onboarding wizard progress header. */
export interface StepIndicatorProps {
  steps: string[];
  current?: number;
}
export declare function StepIndicator(props: StepIndicatorProps): JSX.Element;
