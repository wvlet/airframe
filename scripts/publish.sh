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
     # Publish a release version
     RELEASE=true ./sbt "; + projectJVM2_13/publishSigned; + projectJVM2_12/publishSigned; projectJS/publishSigned; sonatypeBundleRelease"
  fi
else
  echo "This not a master branch commit. Skipping the release step"
fi
