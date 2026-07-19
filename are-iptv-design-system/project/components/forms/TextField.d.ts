import * as React from "react";
/**
 * Labeled text input. `mono` renders the value in monospace for URLs / Xtream params.
 * @startingPoint section="Forms" subtitle="Text input with mono param variant" viewport="700x150"
 */
export interface TextFieldProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, "prefix"> {
  label?: string;
  value?: string;
  placeholder?: string;
  /** Monospace value — use for URLs, usernames, ports, EPG links. */
  mono?: boolean;
  icon?: React.ReactNode;
  helper?: string;
  error?: string;
  /** Static mono prefix inside the field (e.g. "http://"). */
  prefix?: string;
  focused?: boolean;
}
export declare function TextField(props: TextFieldProps): JSX.Element;
