#!/bin/bash
set -e

git config --global user.email "leo@xerial.org"
git config --global user.name "Taro L. Saito"
git config --global push.default simple

./sbt docs/publishMicrosite
