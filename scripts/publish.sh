#!/bin/bash
set -e


if [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" -o "$TRAVIS_BRANCH" == "$TRAVIS_TAG" ]; then
  openssl aes-256-cbc -K $encrypted_fa45534951b5_key -iv $encrypted_fa45534951b5_iv -in travis/secrets.tar.enc -out travis/secrets.tar -d
  tar xvf travis/secrets.tar
  if [ -z "$TRAVIS_TAG" ]; then
     # Publish a snapshot
     ./sbt "+publish"
  else
     # Publish a release version
     RELEASE=true ./sbt "; +publishSigned; sonatypeReleaseAll"
  fi
else
  echo "This not a master branch commit. Skipping the release step"
fi
