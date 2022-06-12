#!/bin/bash

function readOneByte {
	data=`od -N1 -An -vtu1`
	if [ -z "$data" ]; then
		return 1 # not enought data
	fi
	
	echo -n "$data"
	return 0
}

function readArray {
	msb=`readOneByte`
	err=$?
	lsb=`readOneByte`
	err=$(($err | $?))
	if [ $err -ne 0 ]; then
		return 1
	fi
	
	arr_size=$((($msb << 8) | $lsb))
	if [ $arr_size -gt 0 ]; then
		for (( i=0; i < $arr_size; i++ )); do
			data=`readOneByte`
			err=$?
			if [ $err -ne 0 ]; then
				return 2
			fi
			
			echo -n "$data "
		done
	fi
	
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
	
	# send size
	echo "${#1}" | awk '{printf "%c", and(rshift($1, 8),0xFF)}' # MSB size
	echo "${#1}" | awk '{printf "%c", and($1,0xFF)}' # LSB size
	
	# send array
	echo -n "$1"
	
	return 0
}