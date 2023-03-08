#!/bin/bash

source ./SpigotBuilder.sh

function getAllPaperVersions {
	# 'https://api.papermc.io/v2/projects/paper/' contains a JSON with all the MC versions
	curl -s https://api.papermc.io/v2/projects/paper/ | jq -c '.versions | .[]' | grep -o -P '(?<=")1\.\d+(\.\d+)?(?=")' | sort --reverse --version-sort --field-separator=.
}

# @param absolute_copy_path
# @param version (from getAllVersions)
function buildPaperVersion {
	mc_version="$2"
	get_java_version "$mc_version"
	java_version="$?"
	
	# 'https://api.papermc.io/v2/projects/paper/versions/<mc_version>/builds/' contains all the builds from that version
	# It returns a JSON with the last element being the most recent. We'll want the build number and the file name to get the download link.
	base_path="https://api.papermc.io/v2/projects/paper/versions/$mc_version/builds/"
	download_path=`curl -s "$base_path" | jq --raw-output '.builds[-1] | .build,.downloads.application.name' | awk -v base="$base_path" '{build=$0; getline; print base build "/downloads/" $0;}'`
	
	pre_cmd=":" # NOP; in Java 8 you need to use 'apt-get', and wget is already installed
	if [ $java_version -ne 8 ]; then
		# TODO use an image with wget already installed
		pre_cmd="microdnf install wget" # in Java 16-17 wget is not installed
	fi
	
	# Paper gets built on the first server execution; we'll start the server, and then stop it and copy the compiled server
	server_file="paper-$mc_version.jar"
	cmd="$pre_cmd; mkdir BuildTools; cd BuildTools; wget -O $server_file $download_path && echo 'eula=true' > eula.txt && echo 'stop' | java -jar $server_file nogui && cp $server_file /Versions/$mc_version.jar"
	sudo docker run -i --rm --detach --name "Paper_build_$mc_version" -v "$1":/Versions "openjdk:$java_version" /bin/bash -c "$cmd"
}
