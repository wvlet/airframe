#!/bin/bash

coursier bootstrap -r https://oss.sonatype.org/content/repositories/snapshots org.xerial.sbt.thirdparty:scalafmt-cli_2.12:1.3.0-SNAPSHOT -o scalafmt --standalone --main org.scalafmt.cli.Cli
chmod +x scalafmt
