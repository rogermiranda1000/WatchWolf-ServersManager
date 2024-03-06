#!/bin/bash

script_path=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
base_path=$(dirname $(dirname "$script_path"))
unit_tests_path="$base_path/src/test/java"
integration_tests_path="$base_path/src/integration-test/java"

# are unit tests following naming standard?
warning_files=`find "$unit_tests_path" -type f ! -name '*Should.java'` # every java file that don't end with "Should"
if [ ! -z "$warning_files" ]; then
    echo "[w] Some files are not following the unit test naming convention. Any unit test that don't end with 'Should' won't run."
    echo "[v] Files that don't follow the convention:"
    echo "$warning_files"
    exit 1
fi

# are integration tests following naming standard?
warning_files=`find "$integration_tests_path" -type f ! -name 'IT*.java'` # every java file that don't start with "IT"
if [ ! -z "$warning_files" ]; then
    echo "[w] Some files are not following the system test naming convention. Any system test that don't start with 'IT' won't run."
    echo "[v] Files that don't follow the convention:"
    echo "$warning_files"
    exit 1
fi

echo "[i] All done"