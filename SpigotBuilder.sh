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

# @param server_version
function get_java_version {
	case `echo "$1" | grep -o -P '^\d+\.\d+'` in # get the first 2 numbers
		"1.19" | "1.18" )
			return 17
			;;
		"1.17" )
			return 16
			;;
		* ) # previous to 1.17
			return 8
			;;
	esac
}

# TODO check if already updated

# @param absolute_copy_path
# @param version (from getAllVersions)
function buildVersion {
	mc_version="$2"
	get_java_version "$mc_version"
	java_version="$?"
	
	pre_cmd=":" # NOP; in Java 8 you need to use 'apt-get', and git is already installed
	if [ $java_version -ne 8 ]; then
		# TODO use an image with git already installed
		pre_cmd="microdnf install git" # in Java 16-17 git is not installed
	fi
	
	cmd="$pre_cmd; mkdir BuildTools; cd BuildTools; curl -z BuildTools.jar -o BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar && java -jar BuildTools.jar --rev $mc_version && cp spigot-$mc_version.jar /Versions/$mc_version.jar"
	sudo docker run -i --rm --entrypoint /bin/sh --name "Spigot_build_$2" -v "$1":/Versions "openjdk:$java_version" <<< "$cmd"
}