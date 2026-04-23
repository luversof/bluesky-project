Components and fragments in this folder

- Purpose: centralize small reusable JTE fragments (components) used across pages.
- Location convention:
  - Shared components: `fragments/components/` (e.g. `filterBadge.jte`, `dateRangeNavBar.jte`)
  - Page-specific fragments: `fragments/<page>/` (e.g. `fragments/tradeList.jte`)

- Usage:
  - Components expose `@param` contracts at the top. Call with the `@template` fully-qualified path.
    Example:
    @template.stock.htmx.fragments.components.filterBadge(selectedAccountId = accountId)

- Rules:
  - Keep business logic in controllers/services; templates should only render/format.
  - Document required params in the component with `@param` and defaults.
  - Use consistent naming (camelCase) for component files.
