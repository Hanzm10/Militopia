---
phase: 04
slug: unit-roster-combat
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-27
---

# Phase 04 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (via libGDX test harness) + Mockito |
| **Config file** | `core/build.gradle` — testImplementation dependencies |
| **Quick run command** | `./gradlew :core:test --tests "com.militopia.*CombatRosterTest*"` |
| **Full suite command** | `./gradlew :core:test` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :core:test --tests "com.militopia.*CombatRosterTest*"`
- **After every plan wave:** Run `./gradlew :core:test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** ~15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 01 | 1 | WRAITH cleanup | grep | `grep -r "WRAITH" core/src/main/java/ \| wc -l` → 0 | ✅ | ⬜ pending |
| 04-01-02 | 01 | 1 | Cost guard test | unit | `./gradlew :core:test --tests "*CombatRosterTest*"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `core/src/test/java/com/militopia/CombatRosterTest.java` — stubs for cost consistency and summonable count (created by Task 04-01-02)

*Existing infrastructure covers all other phase requirements.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| None | — | — | — |

*All phase behaviors have automated verification.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
