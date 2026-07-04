# Frontend Tree and List Page Standard

## Purpose

This standard defines the default composition rules for pages that combine:

- a left-side tree or category navigator
- a right-side operational list or table panel

In Hunyuan Pro, this is a core system-management pattern and should be treated as a first-class page family rather than a one-off layout.

## Parent Standard

This document inherits from:

- `docs/frontend-foundation-style-standard.md`
- `docs/frontend-list-table-page-standard.md`

If this document conflicts with a lower-level page-local style, this document wins unless the user explicitly approves an exception.

## Reference Implementations

- `hunyuan-design/apps/hunyuan-system/src/views/system/employee/index.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-org-tree.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-table-panel.vue`
- `hunyuan-design/packages/@vben/art-hooks/src/tree/components/art-org-tree/tree.vue`

## Pattern Definition

A tree + list page is a master-detail operational layout where:

- the left area owns structural filtering
- the right area owns search, actions, table operations, and pagination
- both areas stay visible on desktop
- the right area remains the operational focus

## Layout Contract

- Use a two-column layout on desktop.
- Use a single-column stacked layout on mobile.
- The page shell must keep full height with hidden outer overflow.
- The overall page should use `min-h-0` so the tree and table areas can own their own scrolling.

## Desktop Baseline

Observed stable baseline from the employee page:

- left column width: `248px`
- right column width: `minmax(0, 1fr)`
- column gap: `16px`

Rules:

- Start with `248px` for the tree column unless the tree content proves it needs a wider baseline.
- The left column should be structurally narrow and visually secondary.
- The right column should remain the main operational area.

## Mobile Baseline

Observed stable baseline from the employee page:

- single column layout
- top tree block height: `300px`
- bottom operational block: flexible remaining height

Rules:

- On mobile, the tree should move above the list.
- The tree should remain usable without consuming the entire screen.
- Keep the table/list area as the primary scroll owner below the tree.

## Left Tree Panel Rules

- The tree panel should be a bordered white card with `8px` radius.
- Tree header padding should default to `16px 24px`.
- Tree body padding should stay compact; the employee page currently uses `12px`.
- Tree title should use a quiet section-heading style, not a page-heading style.
- Tree nodes should remain compact and scan-friendly.

Observed stable values:

- tree header title: `15px / 22px`, weight `600`
- tree node height: `36px`
- tree node text: `14px / 20px`
- node icon size: `16px`
- node inline gap: `8px`

Rules:

- The current selected tree node should use a low-key filled background rather than a loud accent block.
- Tree rows should not compete visually with primary action buttons.
- Tree scroll should stay inside the card body.

## Right Operational Panel Rules

- The right panel should follow the list/table page standard.
- Search, action toolbar, table, and pagination all remain on the right side.
- The right panel must absorb structural filtering from the tree without duplicating that same hierarchy visually.

Rules:

- The tree is the structural filter.
- The search bar is the attribute filter.
- Do not overload the right panel with duplicate navigation concepts already represented in the tree.

## Interaction Rules

- Clicking a tree node should immediately scope the right-side list.
- The selected tree node and right-side current filter state must remain synchronized.
- Resetting the right-side filters should preserve the current structural tree selection unless the business rule explicitly resets scope.
- Empty tree data and empty right-side table data should each have independent empty-state handling.

## Visual Priority Rules

- The first thing the user should notice is the right-side primary action area.
- The second thing should be the current search and filter state.
- The tree should support orientation, not dominate the page.

This means:

- right-side primary button hierarchy must remain stronger than the tree
- tree highlight states must stay quieter than destructive or primary action states
- the two columns should feel related, but not equally loud

## What Belongs in This Standard

This page family standard should govern:

- left-tree width and density
- two-column responsive behavior
- left/right visual priority
- tree-header, tree-node, and tree-scroll rhythm
- right-panel relationship to structural filtering

This standard should not redefine:

- generic table toolbar button sizes
- generic search-panel control sizes
- generic edit/detail form rules

Those belong to their parent standards.

## First Exemplar

The first normative exemplar for this pattern is:

- `hunyuan-design/apps/hunyuan-system/src/views/system/employee/index.vue`

Why:

- it already combines the left tree, right list, primary actions, search, and dialog form flow in one operational page

## Verification

For meaningful changes to the tree + list pattern:

1. Read this document and the parent standards first.
2. Inspect both the page shell and the left-tree component before editing.
3. Verify that the tree remains structurally secondary to the right operational area.
4. Run `pnpm --dir hunyuan-design -F @vben/web-ele run typecheck` after shared frontend changes.
