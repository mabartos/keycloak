import { FormGroup, FormGroupProps } from "@patternfly/react-core";
import { PropsWithChildren, ReactNode } from "react";
import { FieldError, FieldValues, Merge } from "react-hook-form";
import { FormErrorText } from "./FormErrorText";
import { HelpItem } from "./HelpItem";

export type FieldProps<T extends FieldValues = FieldValues> = {
  id?: string | undefined;
  label?: string;
  name: string;
  labelIcon?: string | ReactNode;
  isRecommendation?: boolean;
  error?: FieldError | Merge<FieldError, T>;
  isRequired: boolean;
};

type FormLabelProps = FieldProps & Omit<FormGroupProps, "label" | "labelIcon">;

export const FormLabel = ({
  id,
  name,
  label,
  labelIcon,
  isRecommendation,
  error,
  children,
  ...rest
}: PropsWithChildren<FormLabelProps>) => (
  <FormGroup
    label={label || name}
    fieldId={id || name}
    labelIcon={
      labelIcon ? (
        <HelpItem
          helpText={labelIcon}
          fieldLabelId={id || name}
          isRecommendation={isRecommendation}
        />
      ) : undefined
    }
    {...rest}
  >
    {children}
    {error && (
      <FormErrorText data-testid={`${name}-helper`} message={error.message} />
    )}
  </FormGroup>
);
