## Task 3: P3.3 Candidate Precheck And Explanation

**Goal:** Let admins understand candidate assignment before publishing or starting a broken process.

**Scope:**

- Add a Hunyuan-native candidate precheck service.
- Input should include simple model, optional form schema, optional simulated starter, and optional simulated form data.
- Output one summary row per user task:
  - node key
  - node name
  - candidate resolver type
  - required configuration
  - whether it can be resolved now
  - whether it requires runtime form data
  - failure or warning message
- The service must not call Flowable.
- Frontend can show this in a quiet designer-side panel or publish confirmation surface.

**Candidate strategies to cover:**

- `EMPLOYEE`
- `ROLE`
- `DEPARTMENT_MANAGER`
- `START_EMPLOYEE`
- `START_DEPARTMENT_MANAGER`
- `EMPLOYEE_SELECT_AT_START`

**Likely backend files:**

- New service under `hunyuan-bpm/module/definition` or `hunyuan-bpm/engine/compiler`
- Admin controller endpoint under existing BPM model/definition surface if needed
- New VO for precheck summaries

**Likely frontend files:**

- `hunyuan-design/apps/hunyuan-system/src/api/system/bpm/*.ts`
- `hunyuan-design/apps/hunyuan-system/src/components/bpm/adapters/bpm-process-designer-adapter.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/model/model-editor.vue`
- `hunyuan-design/apps/hunyuan-system/src/views/system/bpm/bpm-modules.test.ts`

**Steps:**

- [ ] Write backend tests for all six candidate resolver types.
- [ ] Implement precheck summary service without touching Flowable.
- [ ] Add admin API only if the frontend needs live precheck; otherwise keep this source-level first.
- [ ] Wire frontend display in a dense, operational UI surface, not a marketing-style explanation block.
- [ ] Run focused backend tests.
- [ ] Run BPM frontend API/module tests and typecheck.
- [ ] Create `docs/superpowers/specs/2026-07-10-bpm-p3-candidate-precheck-acceptance.md`.

**Done when:**

- Admins can see candidate source and missing configuration before runtime failure.
- Precheck is source-tested for all current candidate strategies.
- Flowable remains hidden.

---

