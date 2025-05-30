#!/bin/bash
cd /home/kavia/workspace/code-generation/memorymatch-kotlin-36176-2f64232b/memorymatch_game
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

