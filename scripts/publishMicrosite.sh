#!/bin/bash
set -e

git config --global user.email "leo@xerial.org"
git config --global user.name "Taro L. Saito"
git config --global push.default simple

git reset --hard HEAD
git clean -f
git checkout master
git pull origin master
sbt docs/publishMicrosite
