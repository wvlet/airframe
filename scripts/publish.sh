#!/bin/bash
set -e

if [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" -o "$TRAVIS_BRANCH" == "$TRAVIS_TAG" ]; then
  PUBLIC_IP=$(curl https://api.ipify.org)
  echo "Public IP Address of this machine: ${PUBLIC_IP}"
  openssl aes-256-cbc -K $encrypted_fa45534951b5_key -iv $encrypted_fa45534951b5_iv -in travis/secrets.tar.enc -out travis/secrets.tar -d
  tar xvf travis/secrets.tar
  if [ -z "$TRAVIS_TAG" ]; then
     # Publish a snapshot
     ./sbt "publishSnapshots"
  else
     # Used for sbt-pgp
     gpg --import ./travis/local.pubring.asc
     gpg --import ./travis/local.secring.asc
     case "$PROJECT" in
       publish)
         # Publish a release version
         ./sbt "; + projectJVM2_13/publishSigned; + projectJVM2_12/publishSigned; sonatypeBundleRelease"
         ;;
       publish-js)
         # Publish a release version
         SCALAJS_VERSION=0.6.31 ./sbt "projectJS/publishSigned"
         SCALAJS_VERSION=1.0.0-RC2 ./sbt "; projectJS/publishSigned; sonatypeBundleRelease"
         ;;
     esac
  fi
else
  echo "This not a master branch commit. Skipping the release step"
fi
