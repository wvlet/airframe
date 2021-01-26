#!/bin/bash

# This script is based on https://github.com/scalameta/metabrowse/blob/master/bin/update-monaco-facade.sh

# Bash strict mode
set -euo pipefail
ORIG_IFS="$IFS"
IFS=$'\n\t'

set -x

ROOT_DIR="$(git rev-parse --show-toplevel)"
PROJECT_DIR=airframe-rx-widget
TARGET_PACKAGE_DIR=wvlet/airframe/rx/widget/editor
TS_IMPORTER_DIR="$ROOT_DIR/project/ts-importer"
NODE_MODULES_DIR="$ROOT_DIR/${PROJECT_DIR}/target/scala-2.12/scalajs-bundler/main/node_modules"
NPM_MONACO_D_TS="$ROOT_DIR/${PROJECT_DIR}/target/scala-2.12/scalajs-bundler/main/node_modules/monaco-editor/monaco.d.ts"
MONACO_D_TS="$TS_IMPORTER_DIR/monaco.d.ts"
MONACO_SCALA="$ROOT_DIR/${PROJECT_DIR}/src/main/scala/${TARGET_PACKAGE_DIR}/Monaco.scala"

if [[ ! -e "$NPM_MONACO_D_TS" ]]; then
  (cd "$ROOT_DIR" && sbt -batch clean widgetJS/compile:npmUpdate)
fi

SCALAJS_TS_IMPORTER=https://github.com/sjrd/scala-js-ts-importer.git
if [[ ! -d "$TS_IMPORTER_DIR" ]]; then
  git clone $SCALAJS_TS_IMPORTER "$TS_IMPORTER_DIR"
fi

# Curated list of edits required for scala-js-ts-importer to parse monaco.d.ts
sed \
   -e 's/): [^ ]* is [^; ]*;/): boolean;/' \
   -e 's/: void | /: /' \
   -e '/^declare namespace monaco.languages.\(typescript\|html\|css\|json\) {$/,/^}$/d' \
   -e 's/ const enum / enum /' \
   -e 's/ get / readonly /' \
   -e 's/setSelections(selections: readonly ISelection\[\]): void//;' \
   -e '/ type \(FindEditorOptionsKeyById\|\ComputedEditorOptionValue\).*/d' \
   -e 's/Latest = ESNext,/Latest = 99,/' \
   -e '/findRenameLocations(fileName: string, /d' \
   -e 's/IEditorOption<EditorOption.\w+,/IEditorOption<Int,/' \
   -e 's/Readonly<Required<\(.*\)>>/\1/ \
   < "$NPM_MONACO_D_TS" \
   > "$MONACO_D_TS"

(cd "$TS_IMPORTER_DIR" && sbt -batch "run $MONACO_D_TS $MONACO_SCALA")
