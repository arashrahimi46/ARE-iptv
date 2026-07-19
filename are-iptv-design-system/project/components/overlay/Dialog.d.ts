import * as React from "react";
/** Modal sheet on a scrim (confirm, PIN, add source). */
export interface DialogProps {
  open?: boolean;
  title?: string;
  children?: React.ReactNode;
  actions?: React.ReactNode;
  width?: number;
  onClose?: () => void;
}
export declare function Dialog(props: DialogProps): JSX.Element;
