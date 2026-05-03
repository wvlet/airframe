# Migrate from npm to pnpm to reduce node_modules storage usage

## Context

The repository currently uses `npm` for two narrow purposes:

1. **Scala.js test runs** — `scripts/setup-scalajs.sh` calls `npm install jsdom@27.X`. CI invokes it
   from four jobs in `.github/workflows/test.yml` (Integration Test, Scala.js / Scala 2.12, 2.13,
   and 3) before running `projectJS/test` / `projectIt/test`. The Scala.js JSDOM env requires
   `jsdom` to be discoverable at the working directory.
2. **A docs leftover** — `website/package.json` is a stub left from the original Docusaurus 1.x
   migration. The current docs flow uses sbt-mdoc + `docusaurusPublishGhpages`; nothing in CI
   actually installs from it. We will leave it untouched in this PR (out of scope).

Root `package.json` already declares `jsdom: ^27.0.0`, so `setup-scalajs.sh` is effectively a
"prepare the working dir" step. With multiple worktrees + Scala cross-version CI cache misses,
each install duplicates `node_modules` on disk. Pnpm uses a content-addressable global store and
hard-links files into project `node_modules`, which deduplicates across the entire host —
particularly valuable in CI runners and across local worktrees.

## Goal

Replace npm with pnpm for the only place we actually call npm: the Scala.js setup hook. CI must
keep working; the local developer workflow described in `airframe-rx-widget/README.md` should be
updated to point at pnpm.

## Changes

### 1. `scripts/setup-scalajs.sh`

Replace the single line with a plain `pnpm install`. Because the dependency is already declared
in `package.json`, this consumes the manifest (no need for `pnpm add`). In CI, pnpm defaults to
`--frozen-lockfile`, which is exactly what we want for deterministic installs.

```sh
#!/bin/sh
set -eu
pnpm install
```

### 2. Commit `pnpm-lock.yaml`

Generate and commit `pnpm-lock.yaml` at the repo root. The lockfile is the missing half of the
storage-savings story: with it, CI installs are deterministic and the pnpm content-addressable
store can be cached across runs (`actions/setup-node` `cache: 'pnpm'`).

### 3. `.github/workflows/test.yml`

Each of the four jobs that runs `setup-scalajs.sh` (`test_integration`, `test_js`,
`test_js_2_13`, `test_js_3`) needs pnpm before Node setup so `actions/setup-node` can detect
it and cache the pnpm store. Use `pnpm/action-setup@v4` then `actions/setup-node@v6` with
`cache: 'pnpm'`:

```yaml
- uses: pnpm/action-setup@v4
  with:
    version: 9
- uses: actions/setup-node@v6
  with:
    node-version: '20'
    cache: 'pnpm'
```

### 4. `airframe-rx-widget/README.md`

Update the developer-facing instructions from `npm install ...` to `pnpm install ...`. The
`browser-sync` global install line stays as `npm install -g browser-sync` only if pnpm has
no equivalent — pnpm does have `pnpm add -g browser-sync`, so use that.

### 5. Out of scope (documented, not changed)

- `website/package.json` — unused stub from the original Docusaurus 1.x migration. The current
  docs flow is sbt-mdoc + `docusaurusPublishGhpages`; nothing in CI installs from it.
- `airframe-rx-widget/scripts/update-monaco-facade.sh` — references `scalajs-bundler` paths
  (webpack), not the npm CLI. No change needed.

## Verification

- Run `./scripts/setup-scalajs.sh` locally with pnpm installed; confirm `node_modules/jsdom`
  appears.
- Push branch, ensure `test_integration` and the three `test_js_*` jobs pass on CI.
- Compare the npm vs pnpm `node_modules` sizes locally as a sanity check (expect pnpm to be
  noticeably smaller via hard-linked store).

## Risk

Low. The footprint is one shell line and four CI job patches. Failure mode is "CI can't find
pnpm" or "pnpm install errors on an empty lockfile", both surfaced immediately by the first
CI run on the PR branch.
