# Frontend UI Standardization Plan

## Purpose

This plan defines which frontend surfaces in Hunyuan Pro should be standardized first, how the standards should be split, and what order we should use for rollout.

The immediate goal is not to restyle one page. The goal is to build a system-level UI baseline that can cover the current app shell, the existing business pages, and the next waves of system modules.

## Karpathy Method

This plan follows the `karpathy-methodology` skill with an `understanding-first` bias:

- First identify the real frontend surface area that exists today.
- Then group those surfaces into standardizable page families.
- Then define the standards by layer, from tokens to components to page patterns.
- Only after the scope is understood should implementation begin.

## Current Frontend Surface Inventory

Based on the current repo, the system frontend already contains these page and interaction families:

### 1. Application Shell

Examples:

- top header and global actions
- sidebar navigation
- breadcrumb and tab navigation

Why it must be standardized:

- Every business page inherits its spacing, height, rhythm, and visual density from the shell.

### 2. Authentication Pages

Evidence:

- `hunyuan-design/apps/hunyuan-system/src/views/_core/authentication/login.vue`
- `.../code-login.vue`
- `.../qrcode-login.vue`
- `.../register.vue`
- `.../forget-password.vue`

Why it must be standardized:

- Authentication is a separate page family with stronger branding and form-focus than ordinary admin pages.

### 3. Fallback and Status Pages

Evidence:

- `hunyuan-design/apps/hunyuan-system/src/views/_core/fallback/*.vue`

Why it must be standardized:

- Empty, error, forbidden, offline, and not-found pages should share one visual and copy baseline.

### 4. Dashboard and Landing-Type System Pages

Evidence:

- `hunyuan-design/apps/hunyuan-system/src/views/system/home/index.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/module-bridge/index.vue`

Why it must be standardized:

- These pages are not ordinary table pages, but they still need consistent spacing, card rhythm, heading scale, and status display.

### 5. Profile and Personal Settings Pages

Evidence:

- `hunyuan-design/apps/hunyuan-system/src/views/_core/profile/index.vue`
- `.../base-setting.vue`
- `.../security-setting.vue`
- `.../password-setting.vue`
- `.../notification-setting.vue`

Why it must be standardized:

- They are form-heavy but not the same as business edit pages.

### 6. List / Search / Table Pages

Evidence:

- `hunyuan-design/apps/hunyuan-system/src/views/system/employee/index.vue`
- `.../components/employee-table-panel.vue`
- `hunyuan-design/apps/web-ele/src/views/demos/table-test.vue`

Why it must be standardized:

- This is the highest-frequency page family in a management system.
- Search bars, action toolbars, table spacing, pagination, and operation columns must stop drifting page by page.

### 7. Tree + List Master-Detail Pages

Evidence:

- `hunyuan-design/apps/hunyuan-system/src/views/system/employee/index.vue`
- `.../components/employee-org-tree.vue`

Why it must be standardized:

- This is a special high-value system pattern: left tree, right list, shared filtering logic, and split-pane density.

### 8. Edit / Detail Pages

Evidence:

- `hunyuan-design/apps/web-ele/src/views/demos/edit-test.vue`
- `hunyuan-design/apps/web-ele/src/views/demos/detail-test.vue`
- Existing standard docs already point to these as references.

Why it must be standardized:

- They need stable page-header, section, form, and detail-display rules.

### 9. Dialog / Drawer / Embedded Form Surfaces

Evidence:

- `hunyuan-design/apps/hunyuan-system/src/views/system/employee/components/employee-form.vue`

Why it must be standardized:

- Many business flows will not open full pages first. They will use dialog and drawer forms.

## What Can Be Included in the Standard

The system-wide standard should be split into these layers.

### Layer A. Design Tokens and Global Rhythm

Should include:

- spacing scale
- radius scale
- control heights
- title and body typography scale
- icon sizes
- border and surface rules
- content width and dense-page rhythm

Reason:

- If this layer is missing, every later component standard becomes fragile.

### Layer B. Basic Interactive Controls

Should include:

- buttons
- icon buttons
- inputs
- selects
- form labels
- switches
- tags
- badges
- pagination controls

Reason:

- These are repeated across every page family.

### Layer C. Shared Business Primitives

Should include:

- search panel
- table toolbar
- table panel
- action groups
- status tags
- org tree containers
- attachment and upload surfaces

Reason:

- These are where business-page drift usually starts.

### Layer D. Page Pattern Standards

Should include:

- authentication pages
- fallback/status pages
- dashboard/landing pages
- list/search/table pages
- tree + list pages
- edit pages
- detail pages
- profile/settings pages
- dialog/drawer forms

Reason:

- Different page families need different composition rules even when they share the same base controls.

### Layer E. State and Feedback Standards

Should include:

- loading states
- empty states
- disabled states
- validation states
- confirm/delete danger flows
- success/warning/error feedback placement

Reason:

- Visual consistency is not just spacing. It also includes what the system feels like when data is loading, missing, or dangerous.

## Recommended Standardization Scope

### Must Include in Phase 1

- global spacing and typography baseline
- button and control height baseline
- list/search/table page standard
- tree + list standard
- dialog/drawer form standard

Why:

- These directly affect the core system-management workflow and the pages already in active use.

### Must Include in Phase 2

- authentication page standard
- profile/settings page standard
- fallback/status page standard

Why:

- These are system-wide but lower-frequency than operational business pages.

### Must Include in Phase 3

- dashboard/home page standard
- module-bridge or informational landing surfaces
- advanced content cards and mixed information layouts

Why:

- These matter, but the business list and form flows should stabilize first.

## Proposed Deliverables

The standards should become durable repo artifacts, not chat-only guidance.

### Core Docs

- `docs/frontend-foundation-style-standard.md`
- `docs/frontend-list-table-page-standard.md`
- `docs/frontend-tree-list-page-standard.md`
- `docs/frontend-dialog-drawer-form-standard.md`
- `docs/frontend-auth-page-standard.md`
- `docs/frontend-profile-page-standard.md`
- `docs/frontend-status-fallback-page-standard.md`

### Current Planning Progress

Completed in the repo:

- `docs/frontend-ui-standardization-plan.md`
- `docs/frontend-foundation-style-standard.md`
- `docs/frontend-list-table-page-standard.md`
- `docs/frontend-tree-list-page-standard.md`
- `docs/frontend-dialog-drawer-form-standard.md`
- `docs/frontend-auth-page-standard.md`
- `docs/frontend-profile-page-standard.md`
- `docs/frontend-status-fallback-page-standard.md`

Still to be planned in the same family:

- system-home / informational landing-page standard
- cross-page state and feedback standard if we decide it needs its own doc

### Shared Implementation Targets

- `@vben/art-hooks/common`
- `@vben/art-hooks/table`
- edit/detail shared components
- system-specific layout wrappers only when shared primitives are not enough

## Rollout Order

### Step 1. Freeze the Foundation Baseline

Define:

- spacing scale
- radius scale
- control heights
- typography scale
- icon size scale

Output:

- one foundation doc

### Step 2. Lock High-Frequency Operational Patterns

Define:

- list/search/table standard
- tree + list standard
- dialog/drawer form standard

Output:

- three pattern docs
- one first business exemplar: employee management

### Step 3. Lock Secondary System Patterns

Define:

- auth
- profile
- fallback/status

Output:

- three more pattern docs

### Step 4. Normalize Informational Pages

Define:

- home/dashboard
- bridge/info/landing cards

Output:

- final informational-page standard

## First Business Exemplar

The first implementation exemplar should remain:

- `hunyuan-design/apps/hunyuan-system/src/views/system/employee/index.vue`

Reason:

- It already combines several core system patterns in one place:
  - search bar
  - primary action toolbar
  - right-side utility toolbar
  - table
  - left org tree
  - dialog form

If employee management is standardized well, it can anchor the first operational family.

## What We Should Decide Before Implementation

Before broad implementation starts, we should explicitly confirm:

1. the global spacing scale
2. the control height scale
3. the page-family breakdown
4. the rollout order
5. which docs are normative and which pages are exemplars

## Recommended Next Move

Do not implement all pages at once.

Start with planning artifacts in this order:

1. `frontend-foundation-style-standard.md`
2. `frontend-tree-list-page-standard.md`
3. refine `frontend-list-table-page-standard.md`
4. `frontend-dialog-drawer-form-standard.md`

Then apply those four standards to employee management as the first full-system exemplar.
