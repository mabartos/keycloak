import {
  Card,
  CardBody,
  CardTitle,
  Gallery,
  Text,
  TextVariants,
} from "@patternfly/react-core";
import {
  CodeIcon,
  MobileAltIcon,
  ServerIcon,
  WindowMaximizeIcon,
} from "@patternfly/react-icons";
import { Controller, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { FormFields } from "../ClientDetails";

export type ClientType = "api" | "web-app" | "spa" | "native" | "custom";

type ClientTypeCardProps = {
  type: ClientType;
  title: string;
  description: string;
  icon: React.ReactNode;
  isSelected: boolean;
  onClick: () => void;
};

const ClientTypeCard = ({
  type,
  title,
  description,
  icon,
  isSelected,
  onClick,
}: ClientTypeCardProps) => {
  return (
    <Card
      isClickable
      isSelectable
      isSelected={isSelected}
      onClick={onClick}
      data-testid={`client-type-${type}`}
      style={{ cursor: "pointer" }}
    >
      <CardTitle>
        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
          {icon}
          <span>{title}</span>
        </div>
      </CardTitle>
      <CardBody>
        <Text component={TextVariants.small}>{description}</Text>
      </CardBody>
    </Card>
  );
};

export const ClientTypeSelector = () => {
  const { t } = useTranslation();
  const { control } = useFormContext<FormFields>();

  const clientTypes: Array<{
    type: ClientType;
    title: string;
    description: string;
    icon: React.ReactNode;
  }> = [
    {
      type: "api",
      title: t("clientTypeApi"),
      description: t("clientTypeApiDescription"),
      icon: <ServerIcon />,
    },
    {
      type: "web-app",
      title: t("clientTypeWebApp"),
      description: t("clientTypeWebAppDescription"),
      icon: <WindowMaximizeIcon />,
    },
    {
      type: "spa",
      title: t("clientTypeSpa"),
      description: t("clientTypeSpaDescription"),
      icon: <CodeIcon />,
    },
    {
      type: "native",
      title: t("clientTypeNative"),
      description: t("clientTypeNativeDescription"),
      icon: <MobileAltIcon />,
    },
  ];

  return (
    <>
      <Text component={TextVariants.p} className="pf-v5-u-mb-md">
        {t("clientTypeSelectorHelp")}
      </Text>
      <Controller
        name="clientType"
        control={control}
        defaultValue="custom"
        render={({ field }) => (
          <Gallery hasGutter minWidths={{ default: "200px" }}>
            {clientTypes.map((clientType) => (
              <ClientTypeCard
                key={clientType.type}
                type={clientType.type}
                title={clientType.title}
                description={clientType.description}
                icon={clientType.icon}
                isSelected={field.value === clientType.type}
                onClick={() => field.onChange(clientType.type)}
              />
            ))}
          </Gallery>
        )}
      />
    </>
  );
};
