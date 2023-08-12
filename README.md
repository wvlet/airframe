[![Gitter Chat][gitter-badge]][gitter-link] [![CI Status][gha-badge]][gha-link] [![codecov](https://codecov.io/gh/wvlet/airframe/branch/main/graph/badge.svg)](https://codecov.io/gh/wvlet/airframe) [![scala-index][sindex-badge]][sindex-link] [![maven central][central-badge]][central-link] [![Scala.js](https://www.scala-js.org/assets/badges/scalajs-1.0.0.svg)](https://www.scala-js.org)

[circleci-badge]: https://circleci.com/gh/wvlet/airframe.svg?style=svg
[circleci-link]: https://circleci.com/gh/wvlet/airframe
[gha-badge]: https://github.com/wvlet/airframe/workflows/CI/badge.svg
[gha-link]: https://github.com/wvlet/airframe/actions?workflow=CI
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/wvlet/airframe?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
[coverall-badge]: https://coveralls.io/repos/github/wvlet/airframe/badge.svg?branch=main
[coverall-link]: https://coveralls.io/github/wvlet/airframe?branch=main
[sindex-badge]: https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange
[sindex-link]: https://index.scala-lang.org/wvlet/airframe
[central-badge]: https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central
[central-link]: https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22


<p><img src="https://github.com/wvlet/airframe/raw/main/logos/airframe-badge-light.png" alt="logo" width="300px"></p>

Airframe https://wvlet.org/airframe is essential building blocks for developing applications in Scala, including logging, object serialization using JSON or MessagePack, dependency injection, http server/client with RPC support, functional testing with AirSpec, etc. 

## Resources

- [Airframe Home](https://wvlet.org/airframe/)
- [Documentation](https://wvlet.org/airframe/docs)
- [Release Notes](https://wvlet.org/airframe/docs/release-notes.html)

### Framework

- [Airframe DI: A Dependency Injection Library Tailored to Scala](https://wvlet.org/airframe/docs/airframe.html)
- [AirSpec: A Functional Testing Library](https://wvlet.org/airframe/docs/airspec.html)

- [Airframe RPC: A Framework for Using Scala Both for Frontend and Backend Programming](https://wvlet.org/airframe/docs/airframe-rpc.html)
<p><img src="https://github.com/wvlet/airframe/raw/main/website/static/img/airframe-rpc/rpc-overview.png" alt="rpc" width="800px"></p>

- [Airframe Modules](https://wvlet.org/airframe/docs/index.html)
<p><img src="https://github.com/wvlet/airframe/raw/main/logos/airframe-overview.png" alt="logo" width="800px"></p>


## For Developers

Airframe uses Scala 3 as the default Scala version. To use Scala 2.x versions, run `++ 2.12` or `++ 2.13` in the sbt console.

### Releasing

For every PR, [release-drafter](https://github.com/release-drafter/release-drafter) will automatically label PRs using the rules defined in [.github/release-drafter.yml](https://github.com/wvlet/airframe/blob/main/.github/release-drafter.yml). 

To publish a new version, first, create a new release tag as follows:

```sh
$ git switch main
$ git pull
$ ruby ./scripts/release.rb
```
This step will update docs/release-noteds.md, push a new git tag to the GitHub, and
create a new [GitHub relese note](https://github.com/wvlet/airframe/releases).
After that, the artifacts will be published to Sonatype (a.k.a. Maven Central). It usually takes 10-30 minutes. 

Note: Do not create a new tag from GitHub release pages, because it will not trigger the GitHub Actions for the release.

### Binary Compatibility

When changing some interfaces, binary compatibility should be checked so as not to break applications using older versions of Airframe. In build.sbt, set AIRFRAME_BINARY_COMPAT_VERSION to the previous version of Airframe as a comparison target. Then run `sbt mimaReportBinaryIssues` to check the binary compatibility.

## LICENSE

[Apache v2](https://github.com/wvlet/airframe/blob/main/LICENSE)
