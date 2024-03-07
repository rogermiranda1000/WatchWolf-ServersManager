#!/bin/bash

# default variables
unit=0
integration=0

# parse params
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --unit) unit=1 ;;
        --integration) integration=1 ;;
        
        *) echo "[e] Unknown parameter passed: $1" >&2 ; exit 1 ;;
    esac
    shift
done

if [ $integration -eq 0 ] && [ $unit -eq 0 ]; then
    echo "[e] You must specify at least one type of test to run!"
    exit 1
fi

# util variables
script_path=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
base_path=$(dirname $(dirname "$script_path"))
local_maven_repos_path="$HOME/.m2"

wsl_mode(){ echo "echo 'Hello world'" | powershell.exe >/dev/null 2>&1; return $?; }
get_ip(){ wsl_mode; if [ $? -eq 0 ]; then echo "(Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias Ethernet).IPAddress" | powershell.exe 2>/dev/null | tail -n2 | head -n1; else hostname -I | awk '{print $1}';fi }


# run the tests
if [ $unit -eq 1 ]; then
    # run unit tests
    docker run -it --rm -v "$base_path":/compile -v "$local_maven_repos_path":/root/.m2 maven:3.8.3-openjdk-17 mvn test -DskipTests=false -DskipUTs=false -DskipITs=true -Dmaven.test.redirectTestOutputToFile=true --file '/compile'
    result=$?

    # Convert xml reports into html
    docker run -it --rm -v "$base_path":/compile -v "$local_maven_repos_path":/root/.m2 maven:3.8.3-openjdk-17 mvn surefire-report:report-only --file '/compile'
    docker run -it --rm -v "$base_path":/compile -v "$local_maven_repos_path":/root/.m2 maven:3.8.3-openjdk-17 mvn site -DgenerateReports=false --file '/compile'

    if [ $result -ne 0 ]; then
        echo "[e] Unit tests failed"
    fi
fi 

if [ $integration -eq 1 ]; then
    # run integration tests
    docker run -it --rm -v "$base_path":/compile -v "$local_maven_repos_path":/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock                \
                    -e WSL_MODE=$(wsl_mode ; echo $? | grep -c 0) -e MACHINE_IP=$(get_ip) -e PUBLIC_IP=$(curl ifconfig.me)                          \
                    -e PARENT_PWD="$base_path" -e SERVER_PATH_SHIFT=./ci/debug                                                                    \
                    maven:3.8.3-openjdk-17 mvn failsafe:integration-test failsafe:verify -Dmaven.test.redirectTestOutputToFile=true --file '/compile'
    result=$?

    # Convert xml reports into html
    docker run -it --rm -v "$base_path":/compile -v "$local_maven_repos_path":/root/.m2 maven:3.8.3-openjdk-17 mvn surefire-report:failsafe-report-only --file '/compile'
    docker run -it --rm -v "$base_path":/compile -v "$local_maven_repos_path":/root/.m2 maven:3.8.3-openjdk-17 mvn site -DgenerateReports=false --file '/compile'

    if [ $result -ne 0 ]; then
        echo "[e] Integration tests failed"
    fi
fi

echo "[i] Done"