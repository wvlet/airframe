#!/bin/bash
set -e

RELEASE=true ./sbt "; +publishSigned; sonatypeReleaseAll"
