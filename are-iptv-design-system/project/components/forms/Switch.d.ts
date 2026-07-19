import * as React from "react";
/** On/off toggle (theme, parental lock, PiP, autoplay). */
export interface SwitchProps {
  checked?: boolean;
  onChange?: (next: boolean) => void;
  disabled?: boolean;
  focused?: boolean;
}
export declare function Switch(props: SwitchProps): JSX.Element;
