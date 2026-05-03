#!/bin/sh
set -eu

# If node_modules exists but was not created by pnpm (e.g. from an older npm-based
# checkout), remove it so `pnpm install` can proceed without an interactive prompt.
if [ -d node_modules ] && [ ! -f node_modules/.modules.yaml ]; then
  rm -rf node_modules
fi

pnpm install
