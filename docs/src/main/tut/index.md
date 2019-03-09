---
layout: home
section: "home"
title: Lightweight Building Blocks for Scala
technologies:
 - first: ["Scala", "Airframe is completely written in Scala"]
 - second: ["SBT", "Airframe uses SBT and other sbt plugins to generate microsites easily"]
---
# Airframe

<center>
<p><img src="https://github.com/wvlet/airframe/raw/master/logos/airframe-overview.png" alt="logo" width="800px"></p>
</center>

Airframe is a collection of [lightweight building blocks](docs/index.html) for kick starting your Scala applicaiton development.


## Resources
- [Documentation](docs)
- [Source Code (GitHub)](https://github.com/wvlet/airframe)

## Blog Articles
- [Airframe HTTP: Building Low-Friction Web Services Over Finagle](https://medium.com/@taroleo/airframe-http-a-minimalist-approach-for-building-web-services-in-scala-743ba41af7f)
  - airframe-http, airframe-http-finagle
- [Demystifying Dependency Injection with Airframe](https://medium.com/@taroleo/demystifying-dependency-injection-with-airframe-9b637034a78a)
  - airframe dependency injection
- [Airframe Log: A Modern Logging Library for Scala](https://medium.com/@taroleo/airframe-log-a-modern-logging-library-for-scala-56fbc2f950bc)
  - airframe-log
- [3 Tips For Maintaining Your Scala Projects](https://medium.com/@taroleo/3-tips-for-maintaining-your-scala-projects-e54a2feea9c4)
  - Tips on how we are maintaining Airframe.

### In Japanese
- [Airframe Meetup #1: Scala開発に役立つ5つのデザインパターンを紹介](https://medium.com/airframe/airframe-meetup-72d6db13182e)
- [AirframeによるScalaプログラミング：「何ができるか」から「何を効果的に忘れられるか」を考える](https://medium.com/airframe/e9e0f7fc983a)
- [Introdution of Airframe in Japanese (日本語)](https://medium.com/@taroleo/airframe-c5d044a97ec)

## Presentations
- [Airframe Meetup #1. 2018-10-23 @ Arm Treasure Data (Tokyo Office)](https://www.slideshare.net/taroleo/airframe-meetup-1-20181023-arm-treasure-data-tokyo-office)
- [Airframe: Lightweight Building-Blocks for Scala @ TD Tech Talk at Tokyo, 2018](https://www.slideshare.net/taroleo/airframe-lightweight-building-blocks-for-scala-td-tech-talk-20181014)


## Related sbt-plugins

- [sbt-pack](https://github.com/xerial/sbt-pack)
  - A sbt plugin for creating distributable Scala packages.
  - Packaging jar dependencies into a folder 
  - You can also [build a docker image of your program](https://github.com/xerial/sbt-pack#building-a-docker-image-file-with-sbt-pack).
  
- [sbt-sonatype](https://github.com/xerial/sbt-sonatype)
  - A sbt plugin for publishing Scala/Java projects to the Maven central.
  - Enables [a single command release](https://github.com/xerial/sbt-sonatype#using-with-sbt-release-plugin) of your project.


