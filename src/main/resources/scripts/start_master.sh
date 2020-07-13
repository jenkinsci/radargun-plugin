#!/bin/bash 

# Jenkins RadarGun plugin
# Script for launching RG main

set -x
ssh $1 $2 $3 $4 -c $5 -s $6 -J "$7"
