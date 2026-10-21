# Airframe website

The documentation site at https://wvlet.org/airframe/, built with [Docusaurus 3](https://docusaurus.io/).
Doc pages come from `../docs`, compiled by mdoc into `../airframe-docs/target/mdoc`.

```sh
# From the repository root: compile docs/*.md (re-run after editing docs)
./sbt docs/mdoc

# From website/
pnpm install
pnpm start   # dev server at http://localhost:3000/airframe/
pnpm build   # static site in build/
```

The `Doc` GitHub Actions workflow builds the site on pull requests and deploys it to GitHub Pages on pushes to `main`.
