#!/bin/bash

coursier bootstrap com.geirsson:scalafmt-cli_2.12:1.5.1 -r bintray:scalameta/maven -o scalafmt --standalone --main org.scalafmt.cli.Cli
chmod +x scalafmt
