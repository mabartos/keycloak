export async function discoverServer(resource: string, signal: AbortSignal) {
  const url = new URL(resource);
  if (
    url.protocol !== "https:" ||
    url.username ||
    url.password ||
    url.search ||
    url.hash
  ) {
    throw new Error("Invalid HTTPS resource URL");
  }
  const wellKnown = url.origin + "/.well-known/oauth-protected-resource";
  for (const endpoint of new Set([
    wellKnown + url.pathname.replace(/^\/$/, ""),
    wellKnown,
  ])) {
    const response = await fetch(endpoint, {
      headers: { Accept: "application/json" },
      credentials: "omit",
      referrerPolicy: "no-referrer",
      redirect: "error",
      signal,
    });
    if (response.status === 404) continue;
    if (!response.ok) throw new Error(response.statusText);
    const metadata = await response.json();
    if (metadata.resource !== resource)
      throw new Error("Resource identifier mismatch");
    const strings = (values: unknown): string[] =>
      Array.isArray(values)
        ? values.filter((v): v is string => typeof v === "string")
        : [];
    return {
      name:
        typeof metadata.resource_name === "string"
          ? metadata.resource_name
          : "",
      issuers: strings(metadata.authorization_servers),
      scopes: strings(metadata.scopes_supported),
    };
  }
  throw new Error("Protected resource metadata unavailable");
}
