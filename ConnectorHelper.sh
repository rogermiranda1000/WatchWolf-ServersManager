#!/bin/bash

function readOneByte {
	data=`od -N1 -An -vtu1`
	if [ -z "$data" ]; then
		return 1 # not enought data
	fi
	
	echo -n "$data"
	return 0
}

function readShort {
	lsb=`readOneByte`
	err=$?
	msb=`readOneByte`
	err=$(($err | $?))
	echo $((($msb << 8) | $lsb))
	return $err
}

function readArray {
	arr_size=`readShort`
	err=$?
	if [ $err -ne 0 ]; then
		return 1
	fi
	
	for (( i=0; i < $arr_size; i++ )); do
		data=`readOneByte`
		err=$?
		if [ $err -ne 0 ]; then
			return 2
		fi
		
		echo -n "$data "
	done
	
	return 0
}

function readString {
	data=`readArray`
	err=$?
	if [ $err -ne 0 ]; then
		return 1
	fi
	
	echo "$data" | awk '{ for(i = 1; i <= NF; i++) printf("%c",$i) }'
	
	return 0
}

# @param string
function sendString {
	if [ $# -ne 1 ]; then
		return 1 # string not passed
	fi
	
	# | sed -z 's/\n/\\n/g' | sed -E -z 's/\\n((\s{2,})|(\t\s*))/\\n\\t/g' # also replace \n and \t
	text=`echo -n "$1" | head -c 65535` # trim if >(2^16-1)
	
	# send size
	echo "${#text}" | LC_CTYPE=C awk '{printf "%c%c", and(rshift(int($1), 8),0xFF), and(int($1),0xFF)}' # MSB size - LSB size
	
	# send array
	echo -n "$text"
	
	return 0
}