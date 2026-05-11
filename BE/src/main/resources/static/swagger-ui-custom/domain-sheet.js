(function () {
  const STATE = {
    isReady: false,
    domains: [],
  };

  const SELECTORS = {
    trigger: "eum-swagger-domain-trigger",
    backdrop: "eum-swagger-domain-backdrop",
    sheet: "eum-swagger-domain-sheet",
    chip: "eum-swagger-domain-chip",
  };

  const contextPath = window.location.pathname.replace(/\/swagger-ui\/.*/, "");
  const apiDocsUrl = `${contextPath}/v3/api-docs`;

  const createElement = (tagName, className, textContent) => {
    const element = document.createElement(tagName);
    if (className) {
      element.className = className;
    }
    if (textContent) {
      element.textContent = textContent;
    }
    return element;
  };

  const unique = (values) => Array.from(new Set(values));

  const extractDomains = (openApi) => {
    const operationCounts = {};
    Object.values(openApi.paths || {}).forEach((pathItem) => {
      Object.values(pathItem || {}).forEach((operation) => {
        if (!operation || !Array.isArray(operation.tags)) {
          return;
        }
        operation.tags.forEach((tag) => {
          operationCounts[tag] = (operationCounts[tag] || 0) + 1;
        });
      });
    });

    const orderedTagNames = unique([
      ...(openApi.tags || []).map((tag) => tag.name),
      ...Object.keys(operationCounts),
    ]);

    return orderedTagNames
      .filter((name) => operationCounts[name] > 0)
      .map((name) => ({
        name,
        count: operationCounts[name],
      }));
  };

  const findTagSection = (domainName) => {
    const sections = Array.from(document.querySelectorAll(".opblock-tag-section"));
    return sections.find((section) => {
      const tagHeading = section.querySelector(".opblock-tag");
      return tagHeading && tagHeading.textContent.trim().startsWith(domainName);
    });
  };

  const scrollToDomain = (domainName) => {
    const section = findTagSection(domainName);
    if (!section) {
      return;
    }
    section.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  const openSheet = () => {
    document.querySelector(`.${SELECTORS.backdrop}`)?.classList.add("is-open");
    document.querySelector(`.${SELECTORS.sheet}`)?.classList.add("is-open");
  };

  const closeSheet = () => {
    document.querySelector(`.${SELECTORS.backdrop}`)?.classList.remove("is-open");
    document.querySelector(`.${SELECTORS.sheet}`)?.classList.remove("is-open");
  };

  const renderDomainButtons = (grid) => {
    grid.replaceChildren();
    STATE.domains.forEach((domain) => {
      const chip = createElement("button", SELECTORS.chip);
      chip.type = "button";
      chip.setAttribute("aria-label", `${domain.name} 도메인으로 이동`);

      const name = createElement("span", "eum-swagger-domain-chip__name", domain.name);
      const count = createElement("span", "eum-swagger-domain-chip__count", String(domain.count));
      chip.append(name, count);
      chip.addEventListener("click", () => {
        closeSheet();
        window.setTimeout(() => scrollToDomain(domain.name), 120);
      });
      grid.appendChild(chip);
    });
  };

  const renderSheet = () => {
    if (document.querySelector(`.${SELECTORS.trigger}`)) {
      return;
    }

    const trigger = createElement("button", SELECTORS.trigger, "도메인 보기");
    trigger.type = "button";
    trigger.setAttribute("aria-haspopup", "dialog");
    trigger.addEventListener("click", openSheet);

    const backdrop = createElement("div", SELECTORS.backdrop);
    backdrop.addEventListener("click", closeSheet);

    const sheet = createElement("section", SELECTORS.sheet);
    sheet.setAttribute("role", "dialog");
    sheet.setAttribute("aria-modal", "true");
    sheet.setAttribute("aria-labelledby", "eum-swagger-domain-sheet-title");

    const handle = createElement("div", "eum-swagger-domain-sheet__handle");
    const header = createElement("div", "eum-swagger-domain-sheet__header");
    const headerText = createElement("div");
    const title = createElement("h2", "eum-swagger-domain-sheet__title", "API 도메인");
    title.id = "eum-swagger-domain-sheet-title";
    const description = createElement(
      "p",
      "eum-swagger-domain-sheet__description",
      "도메인을 선택하면 해당 Swagger 섹션으로 이동합니다.",
    );
    const closeButton = createElement("button", "eum-swagger-domain-close", "x");
    closeButton.type = "button";
    closeButton.setAttribute("aria-label", "도메인 바텀시트 닫기");
    closeButton.addEventListener("click", closeSheet);
    headerText.append(title, description);
    header.append(headerText, closeButton);

    const grid = createElement("div", "eum-swagger-domain-grid");
    renderDomainButtons(grid);
    sheet.append(handle, header, grid);
    document.body.append(trigger, backdrop, sheet);
  };

  const loadDomains = async () => {
    const response = await fetch(apiDocsUrl, { headers: { Accept: "application/json" } });
    if (!response.ok) {
      throw new Error(`OpenAPI document request failed: ${response.status}`);
    }
    STATE.domains = extractDomains(await response.json());
    STATE.isReady = true;
  };

  const boot = async () => {
    try {
      await loadDomains();
      if (STATE.domains.length > 0) {
        renderSheet();
      }
    } catch (error) {
      console.warn("[swagger-domain-sheet] failed to initialize", error);
    }
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
