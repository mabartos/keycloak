import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { goToRealm } from "../utils/sidebar.ts";

test("register an agent and a server, grant access, and find its allowed callers", async ({
  page,
}) => {
  // eslint-disable-next-line playwright/no-skipped-test -- The page requires the experimental AI agents feature.
  test.skip(
    !(await adminClient.isFeatureEnabled("AI_AGENTS")),
    "Requires the AI agents feature",
  );
  const realm = `agents-${uuid()}`;
  const resource = "https://tickets.example.test/mcp";
  await adminClient.createRealm(realm);
  try {
    await adminClient.createClient({
      realm,
      clientId: "assistant",
      name: "Support assistant",
      protocol: "openid-connect",
    });
    await login(page);
    await goToRealm(page, realm);
    await page.getByTestId("nav-item-ai-agents").click();

    const dialog = page.getByRole("dialog");
    const save = async () => {
      await dialog.getByRole("button", { name: "Save", exact: true }).click();
      await expect(dialog).toHaveCount(0);
    };
    const tabs = page.getByRole("tab");
    await expect(tabs).toHaveText([/Agents/, /MCP servers/, /Connections/]);
    await expect(tabs.first()).toHaveAttribute("aria-selected", "true");
    await expect(
      page.getByRole("heading", { name: "No registered agents" }),
    ).toBeInViewport();
    await expect(
      page
        .getByTestId("empty-state")
        .getByRole("button", { name: "Register agent" }),
    ).toBeInViewport();
    const picker = page.getByRole("dialog", { name: "Select client" });
    const chooseRow = async (clientId: string) => {
      await dialog
        .getByRole("row")
        .filter({
          has: page.getByText(clientId, { exact: true }),
        })
        .getByRole("radio")
        .check();
    };
    const selectEndpoint = async (id: string) => {
      await chooseRow(id);
      await dialog.getByRole("button", { name: "Select", exact: true }).click();
    };
    await page.getByRole("tab", { name: /Connections/ }).click();
    await page.getByRole("button", { name: "Create connection" }).click();
    await dialog.getByRole("button", { name: "Choose calling client" }).click();
    await selectEndpoint("assistant");
    await dialog.getByRole("button", { name: "Choose destination" }).click();
    await selectEndpoint("account");
    await expect(
      dialog.getByRole("button", { name: /Choose calling client/ }),
    ).toContainText("Support assistant");
    await expect(
      dialog.getByRole("button", { name: /Choose destination/ }),
    ).toContainText("OpenID Connect client");
    await dialog.getByRole("button", { name: /Choose destination/ }).click();
    await chooseRow("admin-cli");
    await dialog.getByRole("button", { name: "Cancel" }).click();
    await expect(
      dialog.getByRole("button", { name: /Choose destination/ }),
    ).toContainText("account");
    const modes = dialog.getByRole("radiogroup", { name: "Access mode" });
    await expect(modes.getByRole("radio")).toHaveCount(3);
    await expect(
      modes.getByRole("radio", { name: "Interactive sign-in" }),
    ).toBeChecked();
    await expect(
      modes.getByRole("radio", { name: "Enterprise access (ID-JAG)" }),
    ).toBeDisabled();
    const modeWarning = modes.getByRole("button", {
      name: "Why is Enterprise access (ID-JAG) unavailable?",
    });
    const reason = page.getByRole("tooltip").filter({
      hasText: "Choose a registered MCP resource",
    });
    await expect(reason).toBeHidden();
    await modeWarning.hover();
    await expect(reason).toBeVisible();
    await modeWarning.focus();
    await page.keyboard.press("Escape");
    await expect(reason).toBeHidden();
    await expect(dialog).toBeVisible();
    await save();
    await expect(page.getByRole("tab", { name: /Connections/ })).toContainText(
      "1",
    );
    await page.getByRole("tab", { name: /^Agents/ }).click();
    await page
      .getByRole("button", { name: "Register agent", exact: true })
      .click();
    await expect(dialog.getByLabel("Registration method")).toHaveValue("new");
    const create = dialog.getByRole("button", {
      name: "Create client",
      exact: true,
    });
    const redirectUri = "https://calendar.example.test/callback";
    await expect(create).toBeDisabled();
    await dialog
      .getByRole("textbox", { name: /^Client ID\b/ })
      .fill("assistant");
    await dialog
      .getByRole("textbox", { name: /^Redirect URI\b/ })
      .fill(redirectUri);
    await create.click();
    await expect(page.getByText(/Could not create client/)).toBeVisible();
    await expect(
      dialog.getByRole("textbox", { name: /^Redirect URI\b/ }),
    ).toHaveValue(redirectUri);
    await dialog
      .getByRole("textbox", { name: /^Client ID\b/ })
      .fill("calendar-assistant");
    await create.click();
    await expect(dialog).toHaveCount(0);
    await expect(page.getByRole("tab", { name: /^Agents/ })).toHaveAttribute(
      "aria-selected",
      "true",
    );
    await expect(page.getByRole("tab", { name: /^Agents/ })).toContainText("1");
    expect(
      await adminClient.getClient("calendar-assistant", realm),
    ).toMatchObject({
      publicClient: true,
      protocol: "openid-connect",
      standardFlowEnabled: true,
      directAccessGrantsEnabled: false,
      implicitFlowEnabled: false,
      serviceAccountsEnabled: false,
      attributes: { "ai.agent": "true", "pkce.code.challenge.method": "S256" },
      redirectUris: [redirectUri],
    });
    await page
      .getByRole("button", { name: "Register agent", exact: true })
      .click();
    await dialog.getByLabel("Registration method").selectOption("local");
    await dialog.getByRole("button", { name: "Select client" }).click();
    await expect(picker.locator("thead th")).toHaveText([
      "Select",
      "Client ID",
      "Name",
    ]);
    await chooseRow("assistant");
    await picker.getByRole("button", { name: "Add", exact: true }).click();
    await expect(
      dialog.getByRole("textbox", { name: /^Client ID\b/ }),
    ).toHaveValue("assistant");
    await save();

    await page.getByRole("tab", { name: /MCP servers/ }).click();
    await page
      .getByRole("button", { name: "Register MCP server", exact: true })
      .click();
    await dialog.getByRole("textbox", { name: /^Name\b/ }).fill("Tickets");
    await dialog.getByLabel("Resource URL").fill(resource);
    const scopes = dialog.getByRole("combobox", { name: "Advertised scopes" });
    await scopes.fill("profile");
    await page.getByRole("option", { name: "profile", exact: true }).click();
    await scopes.fill("tickets.read");
    await page.getByRole("option", { name: "Add “tickets.read”" }).click();
    await save();

    await page.getByRole("tab", { name: /Connections/ }).click();
    await page
      .getByRole("button", { name: "Create connection", exact: true })
      .click();
    await dialog.getByRole("button", { name: "Choose calling client" }).click();
    await expect(
      dialog.getByRole("tab", { name: "Agents", exact: true }),
    ).toHaveAttribute("aria-selected", "true");
    await chooseRow("assistant");
    await dialog.getByRole("tab", { name: "Realm clients" }).click();
    await expect(
      dialog.getByRole("button", { name: "Select", exact: true }),
    ).toBeDisabled();
    await dialog.getByRole("tab", { name: "Agents", exact: true }).click();
    await selectEndpoint("assistant");
    await expect(
      dialog.getByRole("button", { name: /Choose calling client/ }),
    ).toContainText("AI agent");
    await dialog.getByRole("button", { name: "Choose destination" }).click();
    await selectEndpoint(resource);
    await expect(
      dialog.getByRole("button", { name: /Choose destination/ }),
    ).toContainText("MCP server");
    const allowed = dialog.getByRole("combobox", {
      name: "Allowed token scopes",
    });
    await allowed.fill("tickets");
    await expect(
      dialog.getByRole("option", { name: "tickets.read", exact: true }),
    ).toBeVisible();
    await allowed.fill("openid");
    await dialog.getByRole("option", { name: /openid/ }).click();
    await allowed.fill("profile");
    await dialog.getByRole("option", { name: "profile", exact: true }).click();
    await save();

    await page.getByRole("tab", { name: /MCP servers/ }).click();
    await page.getByRole("button", { name: "Allowed callers (1)" }).click();
    await expect(
      page.getByRole("tab", { name: /Connections/ }),
    ).toHaveAttribute("aria-selected", "true");
    await expect(
      page.getByText("Support assistant", { exact: true }),
    ).toBeVisible();
    await expect(
      page.getByText("openid profile", { exact: true }),
    ).toBeVisible();

    await page.getByRole("tab", { name: /^Agents/ }).click();
    await page
      .getByRole("button", { name: "Register agent", exact: true })
      .click();
    await dialog.getByLabel("Registration method").selectOption("cimd");
    await dialog
      .getByRole("textbox", { name: /^Name\b/ })
      .fill("Desktop assistant");
    const metadataUrl = "https://clients.example.test/desktop.json";
    await dialog.getByLabel("Client metadata URL").fill(metadataUrl);
    await save();
    await expect(page.getByText(metadataUrl, { exact: true })).toBeVisible();
  } finally {
    await adminClient.deleteRealm(realm);
  }
});
