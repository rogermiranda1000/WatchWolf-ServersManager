#!/bin/bash

# From https://www.spigotmc.org/wiki/buildtools
function getAllVersions {
	echo "1.19"
	echo "1.18.2"
	echo "1.18.1"
	echo "1.18"
	echo "1.17.1"
	echo "1.17"
	echo "1.16.5"
	echo "1.16.4"
	echo "1.16.3"
	echo "1.16.2"
	echo "1.16.1"
	echo "1.15.2"
	echo "1.15.1"
	echo "1.15"
	echo "1.14.4"
	echo "1.14.3"
	echo "1.14.2"
	echo "1.14.1"
	echo "1.14"
	echo "1.13.2"
	echo "1.13.1"
	echo "1.13"
	echo "1.12.2"
	echo "1.12.1"
	echo "1.12"
	echo "1.11.2"
	echo "1.11.1"
	echo "1.11"
	echo "1.10.2"
	echo "1.9.4"
	echo "1.9.2"
	echo "1.9"
	echo "1.8.8"
	echo "1.8.3"
	echo "1.8"
}

# TODO check if already updated

# @param absolute_copy_path
# @param version (from getAllVersions)
function buildVersion {
	cmd="mkdir BuildTools; cd BuildTools; microdnf install git && curl -z BuildTools.jar -o BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar && java -jar BuildTools.jar --rev $2 && cp spigot-$2.jar /Versions/$2.jar"
	sudo docker run -i --rm --entrypoint /bin/sh --name "Spigot_build_$2" -v "$1":/Versions openjdk:17 <<< "$cmd"
}