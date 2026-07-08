# BPM Runtime Shell Routing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move BPM employee runtime pages back into the normal admin shell while keeping `start-form` as a hidden shell-internal route.

**Architecture:** Let the four list pages rely on backend-generated menu routes under the normal shell, and keep only `start-form` in static routing with a `BasicLayout` wrapper. Update the route contract tests to reflect that separation.

**Tech Stack:** Vue 3, vue-router, Vben layout routing, Vitest, TypeScript

## Global Constraints

- Keep changes tightly scoped to BPM runtime routing only.
- Prefer existing router and layout patterns over new abstractions.
- Preserve exact backend/frontend menu path contracts.
- Verify with targeted tests, typecheck, and live browser proof.

---

### Task 1: Re-scope static BPM runtime routes

**Files:**
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts`
- Modify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**Interfaces:**
- Consumes: backend menu paths from `v3.35.0.sql`
- Produces: one static shell-wrapped hidden route for `SystemBpmRuntimeStartFormRoute`

- [ ] Remove static ownership for the four menu-backed runtime list routes.
- [ ] Keep `SystemBpmRuntimeStartFormRoute` and wrap it with `BasicLayout`.
- [ ] Update source-route contract tests so only `start-form` is required in static BPM routing, while the four list pages remain required as view files and SQL-backed menu targets.

### Task 2: Verify runtime navigation still targets the hidden route

**Files:**
- Inspect: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/startable-list.vue`
- Inspect: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/my-instance-list.vue`
- Inspect: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/runtime/start-form.vue`

**Interfaces:**
- Consumes: route name `SystemBpmRuntimeStartFormRoute`
- Produces: unchanged query-driven navigation for start and resubmit flows

- [ ] Confirm `startable-list` still pushes by route name with `definitionId`.
- [ ] Confirm `my-instance-list` still pushes by route name with `instanceId`.
- [ ] Confirm `start-form` back navigation and submit completion still return to shell-backed list paths.

### Task 3: Verify contracts and live shell behavior

**Files:**
- Verify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/router/routes/static/bpm.ts`
- Verify: `E:/my-project/hunyuan-pro/hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**Interfaces:**
- Consumes: router changes from Task 1
- Produces: validated shell-internal runtime navigation behavior

- [ ] Run targeted BPM route contract tests.
- [ ] Run frontend typecheck.
- [ ] Verify in the browser that `/system/bpm/runtime/my-instance-list` and `/system/bpm/runtime/start-form?...` both render inside the normal shell.
