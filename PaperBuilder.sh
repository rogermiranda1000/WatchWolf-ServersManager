#!/bin/bash

function getAllVersions {
	# 'https://hub.spigotmc.org/versions' contains all the version files
	curl -s https://api.papermc.io/v2/projects/paper/ | jq -c '.versions | .[]' | grep -o -P '(?<=")1\.\d+(\.\d+)?(?=")' | sort --reverse --version-sort --field-separator=.
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

# @param absolute_copy_path
# @param version (from getAllVersions)
function buildVersion {
	mc_version="$2"
	get_java_version "$mc_version"
	java_version="$?"
	
	base_path="https://api.papermc.io/v2/projects/paper/versions/$mc_version/builds/"
	download_path=`curl -s "$base_path" | jq --raw-output '.builds[-1] | .build,.downloads.application.name' | awk -v base="$base_path" '{build=$0; getline; print base build "/downloads/" $0;}'`
	
	cmd="mkdir BuildTools; cd BuildTools; wget $download_path && java  && cp spigot-$mc_version.jar /Versions/$mc_version.jar"
	sudo docker run -i --rm --detach --name "Paper_build_$mc_version" -v "$1":/Versions "openjdk:$java_version" /bin/bash -c "$cmd"
}
