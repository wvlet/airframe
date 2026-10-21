# Migrate the docs site from Docusaurus 1 to Docusaurus 3

## Goals

- Retire Docusaurus 1.14.7 (`website/`). It has been unmaintained since 2021 and pulls in a large tree of vulnerable, unpatched packages (svgo 1, imagemin, `request`, `decompress`, `shell-quote`, etc.). The docs publish job installs that tree on every run without a lockfile.
- Move to Docusaurus 3.10.2 (the current `latest`) with a committed lockfile, so Dependabot can track and patch real dependencies.
- Keep the existing pipeline shape: `docs/*.md` → sbt-mdoc (`airframe-docs/target/mdoc`) → Docusaurus → `gh-pages` branch → https://wvlet.org/airframe/.
- Keep existing public URLs working, such as `/airframe/docs/airframe-di.html`, which READMEs, blog posts and external articles link to.

## Background

- The `docs` sbt project enables `MdocPlugin` and `DocusaurusPlugin`. `.github/workflows/doc.yml` runs `./sbt docs/docusaurusPublishGhpages`, which runs `yarn install` in `website/` and then the v1 `publish-gh-pages` script over SSH using the `GIT_DEPLOY_KEY` secret.
- GitHub Pages is configured as a legacy build from the `gh-pages` branch, root path. There is no CNAME on this repo; `wvlet.org` comes from the org-level Pages domain. `baseUrl` is `/airframe/`.
- v1 used `cleanUrl: true`, which serves both `/docs/foo` and `/docs/foo.html`. Many links in `docs/` and external sites use the `.html` form.
- v1-only pieces that have no direct v3 equivalent:
  - `siteConfig.js` and `sidebars.json`
  - React pages under `pages/en/` (`index.js` is the landing page; `help.js` and `users.js` are unedited templates)
  - `core/Footer.js`
  - Algolia DocSearch v2 config, which uses only an `apiKey`
  - Universal Analytics `gaTrackingId` (`UA-…`); Google shut Universal Analytics down in 2023, so it no longer collects data
- The 30 docs in `docs/` use v1 front matter (`id`, `layout: docs`, `title`). 23 titles contain an unquoted `: `, such as `title: airframe-di: Dependency Injection`. v3's strict YAML parser rejects these.
- sbt-mdoc 2.9.2 supports `docusaurusVersion := DocusaurusVersion.V3`. In that mode it still shells out to **yarn** (`yarn install`, `yarn build`, `yarn deploy` with `USE_SSH=true`).
- #4280 removed the `website` importer that Dependabot had added to the root `pnpm-lock.yaml`. The root lockfile must stay root-only, because the Scala.js CI jobs install from it.

## Design

**Lane: Standard.** This replaces the public docs site's theme, landing page and publish pipeline, so it changes what users see.

1. **New site skeleton in `website/`** (replacing v1 files)
   - `package.json` pins `@docusaurus/core` and `@docusaurus/preset-classic` to `3.10.2`, with React 19. It gets its own `website/pnpm-lock.yaml`.
   - `website/` is not a pnpm workspace member of the root, so the Scala.js lockfile stays untouched.
   - `docusaurus.config.js` sets:
     - `url: https://wvlet.org`, `baseUrl: /airframe/`, organization `wvlet`, project `airframe`
     - `trailingSlash: false`, so pages are emitted as `docs/airframe-di.html` and `.html` URLs keep working on GitHub Pages
     - `docs.path: '../airframe-docs/target/mdoc'` and `routeBasePath: 'docs'`
     - `markdown.format: 'detect'`, so `.md` files are parsed as CommonMark rather than MDX. This avoids breaking on `<`, `{` and HTML in the existing docs.
     - The blog keeps the single existing post.
     - The theme keeps the v1 colors (`#07405b` primary, `#0e859b` secondary) and the favicon.
     - The navbar has Docs, Blog, Release Notes and GitHub. The footer has the copyright line.
   - Port `sidebars.json` to `sidebars.js` with the same four categories (Resources, Framework, Core Modules, Utilities).
   - Port the landing page to `src/pages/index.js`, keeping the hero, tagline and feature blocks. Drop the template-only `help.js` and `users.js`.
   - Move `static/` over as-is. Drop the unused `solarized-dark.css` and the v1 `overwrite.css` after checking what's still needed.
2. **Docs content fixes (`docs/*.md`)**: quote titles that contain `:` and drop `layout: docs`. Leave the content unchanged otherwise; any MDX or link fixes are only the ones the build reports.
3. **Build and publish**
   - In `build.sbt`, keep `MdocPlugin` and drop `DocusaurusPlugin` from the `docs` project.
   - In `doc.yml`:
     - `actions/setup-node` (Node 24) + `pnpm/action-setup` (v9)
     - `./sbt docs/mdoc`
     - `pnpm --dir website install --frozen-lockfile`, then `pnpm --dir website build`
     - `actions/upload-pages-artifact` + `actions/deploy-pages`, with `pages: write` and `id-token: write` scoped to the deploy job and the `github-pages` environment
   - The `GIT_DEPLOY_KEY` SSH push and the Git identity setup go away.
   - **Pages setting switch**: the repository's Pages source moves from "Deploy from a branch (`gh-pages`)" to "GitHub Actions" (`gh api -X PUT repos/wvlet/airframe/pages -f build_type=workflow`).
     - This happens right after the PR merges. Then a manual run of the Doc workflow deploys the site.
     - The `gh-pages` branch stays untouched as a rollback: switch the source back to it.
     - `GIT_DEPLOY_KEY` can be deleted once the new deploy is confirmed.
   - Add a CI build check: on PRs that touch `docs/**` or `website/**`, run the same build steps without deploying, so broken links or front matter fail before merge. Docusaurus `onBrokenLinks: 'throw'`.
4. **Search and analytics**
   - **Search:** use Algolia DocSearch v3 through the classic theme's `themeConfig.algolia` (`appId`, search-only `apiKey`, `indexName`). These are public search-only credentials, so they can go in the config file.
     - The DocSearch crawler config must target the new v3 markup, so the index needs a re-crawl after the first deploy.
     - Until that re-crawl runs, search results may be stale or empty.
   - **Analytics:** drop the dead `UA-` Google Analytics tag.
5. **Verification**
   - `pnpm build` passes with no broken links.
   - Open a local `pnpm serve` and spot-check the landing page, a few docs and `.html` URLs (`/airframe/docs/airframe-di.html`, `/airframe/docs/release-notes.html`), the blog, and search.
   - The first real deploy happens on merge to `main`; the doc workflow runs on `website/**` changes.

## Alternatives and Why Not?

- **Keep `DocusaurusPlugin` with `DocusaurusVersion.V3`**: this is the smallest diff in `build.sbt`, but it hard-codes **yarn**. That means adding a `yarn.lock` and a second package manager alongside the repo's pnpm setup. It also runs `yarn deploy` (`docusaurus deploy`), which needs the SSH deploy-key script baked into the plugin. Running the build in the workflow is explicit and lockfile-backed.
- **Keep pushing to `gh-pages` with the SSH deploy key**: this avoids a settings change. However, it keeps a long-lived write key in secrets and a generated-build branch in the repository. Deploying through Pages from Actions uses short-lived OIDC tokens instead.
- **Move to another generator (mdoc + MkDocs, Starlight, etc.)**: this is a larger content and theme rewrite for no clear gain. Docusaurus 3 is the natural successor and keeps the sidebar and blog model.
- **Docusaurus 4 canary**: it isn't released yet. Use 3.10.x now; the upgrade from 3 to 4 should be incremental.
- **Local search (`@easyops-cn/docusaurus-search-local`)**: it needs no account, but its relevance is weaker and it adds build time. Algolia was already in use.
