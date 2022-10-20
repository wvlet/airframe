[![Gitter Chat][gitter-badge]][gitter-link] [![CI Status][gha-badge]][gha-link] [![codecov](https://codecov.io/gh/wvlet/airframe/branch/master/graph/badge.svg)](https://codecov.io/gh/wvlet/airframe) [![scala-index][sindex-badge]][sindex-link] [![maven central][central-badge]][central-link] [![Scala.js](https://www.scala-js.org/assets/badges/scalajs-1.0.0.svg)](https://www.scala-js.org)

[circleci-badge]: https://circleci.com/gh/wvlet/airframe.svg?style=svg
[circleci-link]: https://circleci.com/gh/wvlet/airframe
[gha-badge]: https://github.com/wvlet/airframe/workflows/CI/badge.svg
[gha-link]: https://github.com/wvlet/airframe/actions?workflow=CI
[gitter-badge]: https://badges.gitter.im/Join%20Chat.svg
[gitter-link]: https://gitter.im/wvlet/airframe?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
[coverall-badge]: https://coveralls.io/repos/github/wvlet/airframe/badge.svg?branch=master
[coverall-link]: https://coveralls.io/github/wvlet/airframe?branch=master
[sindex-badge]: https://index.scala-lang.org/wvlet/airframe/airframe/latest.svg?color=orange
[sindex-link]: https://index.scala-lang.org/wvlet/airframe
[central-badge]: https://img.shields.io/maven-central/v/org.wvlet.airframe/airframe_2.12.svg?label=maven%20central
[central-link]: https://search.maven.org/search?q=g:%22org.wvlet.airframe%22%20AND%20a:%22airframe_2.12%22


<p><img src="https://github.com/wvlet/airframe/raw/master/logos/airframe-badge-light.png" alt="logo" width="300px"></p>

Airframe https://wvlet.org/airframe is a collection of [lightweight building blocks](https://wvlet.org/airframe/docs/) for Scala.

## Resources

- [Airframe Home](https://wvlet.org/airframe/)
- [Documentation](https://wvlet.org/airframe/docs)
- [Release Notes](https://wvlet.org/airframe/docs/release-notes.html)

### Framework

- [Airframe DI: A Dependency Injection Library Tailored to Scala](https://wvlet.org/airframe/docs/airframe.html)
- [AirSpec: A Functional Testing Library](https://wvlet.org/airframe/docs/airspec.html)

- [Airframe RPC: A Framework for Using Scala Both for Frontend and Backend Programming](https://wvlet.org/airframe/docs/airframe-rpc.html)
<p><img src="https://github.com/wvlet/airframe/raw/master/website/static/img/airframe-rpc/rpc-overview.png" alt="rpc" width="800px"></p>

- [Airframe Modules](https://wvlet.org/airframe/docs/index.html)
<p><img src="https://github.com/wvlet/airframe/raw/master/logos/airframe-overview.png" alt="logo" width="800px"></p>


## For Developers

### Dotty (Scala 3)

For developing with Dotty (Scala 3), use DOTTY=true environment variable:
```
$ DOTTY=true ./sbt
> logJVM/test
```

Or use `++ 3` in the sbt console:
```
# Switch to Scala 3
> ++ 3
```

For starting a migration of some project to Scala 3, create a PR that removes `.settings(scala2Only)` from build.sbt to use Scala 3 in the project, and add the project to `projectDotty`. After all tests pass, the PR can be merged.


Here is the list of milestones for Dotty support: [#1077](https://github.com/wvlet/airframe/issues/1077)

### Releasing

For every commit merged to the master branch, a draft of the [release note](https://github.com/wvlet/airframe/releases) will be updated with [release-drafter](https://github.com/release-drafter/release-drafter).

To publish a new version, first, create a new release tag as follows:

```sh
$ git switch master
$ git pull
$ ruby ./scripts/release.rb
```
This step will update docs/release-noteds.md and push a new git tag to the GitHub.
After that, GitHub actions for releading artifacts to Sonatype will be triggered automatically.

Next, edit and publish the draft of [the release note](https://github.com/wvlet/airframe/releases).
If necessary, adjust the version number and target tag. 

Do not create a new tag from GitHub release pages, because it will not trigger the GitHub Actions for the release.

## LICENSE

[Apache v2](https://github.com/wvlet/airframe/blob/master/LICENSE)
