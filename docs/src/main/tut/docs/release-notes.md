---
layout: docs
title: Release Notes
---

# Release Notes

## 19.9.0

-  airframe-di: [#651](https://github.com/wvlet/airframe/issues/651) Support design-time lifecycle hooks ([#655](https://github.com/wvlet/airframe/issues/655)) [[de526a5](https://github.com/wvlet/airframe/commit/de526a5)]
-  airframe-di: Ensure running all shutdown hooks ([#653](https://github.com/wvlet/airframe/issues/653)) [[56cf14a](https://github.com/wvlet/airframe/commit/56cf14a)]
-  airframe-di: Automatically register shutdown hooks for Closeable resources ([#645](https://github.com/wvlet/airframe/issues/645)) [[937342b](https://github.com/wvlet/airframe/commit/937342b)]
-  ~airframe-di: Add interfaces to define LifeCycle hooks~ ([#656](https://github.com/wvlet/airframe/issues/656)) [[cf161ec](https://github.com/wvlet/airframe/commit/cf161ec)]
-  airframe-http: Fix Option[X] arg binding ([#642](https://github.com/wvlet/airframe/issues/642)) [[9e9e509](https://github.com/wvlet/airframe/commit/9e9e509)]
-  airframe-http-finagle: Upgrade to Finagle 19.8.0 ([#652](https://github.com/wvlet/airframe/issues/652)) [[e30d400](https://github.com/wvlet/airframe/commit/e30d400)]
-  airframe-msgpack: Add a stream json-msgpack converter ([#643](https://github.com/wvlet/airframe/issues/643)) [[b20fd4e](https://github.com/wvlet/airframe/commit/b20fd4e)]
- internal changes:
  -  Release only Scala 2.12 projects for snapshots [[b2a8d92](https://github.com/wvlet/airframe/commit/b2a8d92)]
  -  Upgrade to sbt 1.3.0-RC5 ([#647](https://github.com/wvlet/airframe/issues/647)) [[ff4d209](https://github.com/wvlet/airframe/commit/ff4d209)]
  -  Add note on airspec and aiframe-log integration [[61af200](https://github.com/wvlet/airframe/commit/61af200)]
  -  Use scalafmt 2.0.1 syntax ([#654](https://github.com/wvlet/airframe/issues/654)) [[46eb9a1](https://github.com/wvlet/airframe/commit/46eb9a1)]

## 19.8.10
-  airspec: synchonize logger cleanup to stabilize tests [[121ef76](https://github.com/wvlet/airframe/commit/121ef76)]
-  airframe-http-recorder: Avoid unnecessary db update [[6353233](https://github.com/wvlet/airframe/commit/6353233)]
-  airframe-http-client: use a consistent header customization order [#638](https://github.com/wvlet/airframe/issues/638) ([#639](https://github.com/wvlet/airframe/issues/639)) [[576b9c9](https://github.com/wvlet/airframe/commit/576b9c9)]
-  airframe-http-finagle: Make FinagleServer start/stop idempotent ([#637](https://github.com/wvlet/airframe/issues/637)) [[bc39b75](https://github.com/wvlet/airframe/commit/bc39b75)]
-  airframe-surface: Support inner classes ([#635](https://github.com/wvlet/airframe/issues/635)) [[112a3f9](https://github.com/wvlet/airframe/commit/112a3f9)]
-  airframe-di: Fix [#632](https://github.com/wvlet/airframe/issues/632) to run inherited lifecycle hooks ([#633](https://github.com/wvlet/airframe/issues/633)) [[c9ccafe](https://github.com/wvlet/airframe/commit/c9ccafe)]
- internal changes:
  -  Use a black theme for source code [[c40b9c7](https://github.com/wvlet/airframe/commit/c40b9c7)]
  -  Update AirSpec doc [[7d67f5e](https://github.com/wvlet/airframe/commit/7d67f5e)]
  -  Skip benchmark test on Travis CI [[a55acb5](https://github.com/wvlet/airframe/commit/a55acb5)]
  -  Fix Fluency tests ([#636](https://github.com/wvlet/airframe/issues/636)) [[7df550b](https://github.com/wvlet/airframe/commit/7df550b)]

## 19.8.9
-  airspec: Support AirSpec.design, localDesign ([#631](https://github.com/wvlet/airframe/issues/631)) [[dfd7a86](https://github.com/wvlet/airframe/commit/dfd7a86)]
   - This also fixes an issue in finding test methods defined in sub classes.
-  internal: Run tests in parallel ([#629](https://github.com/wvlet/airframe/issues/629)) [[779f80d](https://github.com/wvlet/airframe/commit/779f80d)]
-  internal: Upgrade to sbt 1.3.0-RC4 ([#628](https://github.com/wvlet/airframe/issues/628)) [[5a0807b](https://github.com/wvlet/airframe/commit/5a0807b)]

## 19.8.8
-  airframe-http-recorder: Fix exclude flag behavior ([#627](https://github.com/wvlet/airframe/issues/627)) [[c73d8a7](https://github.com/wvlet/airframe/commit/c73d8a7)]
-  airframe-http-client: Add HOST header if missing ([#626](https://github.com/wvlet/airframe/issues/626)) [[7d1c91e](https://github.com/wvlet/airframe/commit/7d1c91e)]
-  airframe-di: Fix design option override ([#625](https://github.com/wvlet/airframe/issues/625)) [[576fab7](https://github.com/wvlet/airframe/commit/576fab7)]
-  airspec: Support skipping the whole spec ([#624](https://github.com/wvlet/airframe/issues/624)) [[f7d5f59](https://github.com/wvlet/airframe/commit/f7d5f59)]
-  airframe-http-finagle: Add sendRaw(request) ([#623](https://github.com/wvlet/airframe/issues/623)) [[c54da5e](https://github.com/wvlet/airframe/commit/c54da5e)]
-  airspec: Add inCicleCI ([#622](https://github.com/wvlet/airframe/issues/622)) [[972f9bf](https://github.com/wvlet/airframe/commit/972f9bf)]

## 19.8.7
-  airframe-http-recorder: Store records to .airframe folder ([#619](https://github.com/wvlet/airframe/issues/619)) [[e22b5ed](https://github.com/wvlet/airframe/commit/e22b5ed)]
-  airframe-http-recorder: Support custom http request matchers ([#618](https://github.com/wvlet/airframe/issues/618)) [[37aea23](https://github.com/wvlet/airframe/commit/37aea23)]
-  Upgrade to sbt-dyvner 4.0.0 ([#617](https://github.com/wvlet/airframe/issues/617)) [[02fee65](https://github.com/wvlet/airframe/commit/02fee65)]
-  Upgrade scalafmt to 2.0.1 ([#616](https://github.com/wvlet/airframe/issues/616)) [[4112e95](https://github.com/wvlet/airframe/commit/4112e95)]
-  airspec: Add airspec-light project settings ([#615](https://github.com/wvlet/airframe/issues/615)) [[34e56f6](https://github.com/wvlet/airframe/commit/34e56f6)]

## 19.8.6
-  airspec
   -  Fix shouldNotBe null error message [[c869db1](https://github.com/wvlet/airframe/commit/c869db1)]
   -  [#612](https://github.com/wvlet/airframe/issues/612): Fixes java.lang.ClassNotFoundException: scala.reflect.api.Trees ([#613](https://github.com/wvlet/airframe/issues/613)) [[06b70d8](https://github.com/wvlet/airframe/commit/06b70d8)]
   -  [#610](https://github.com/wvlet/airframe/issues/610): Avoid registering JVM shutdown hooks in airspec sessions ([#611](https://github.com/wvlet/airframe/issues/611)) [[44b84d3](https://github.com/wvlet/airframe/commit/44b84d3)]
-  internal: Upgrade to sbt 1.3.0-RC3 with turbo mode ([#614](https://github.com/wvlet/airframe/issues/614)) [[59d9d48](https://github.com/wvlet/airframe/commit/59d9d48)]

## 19.8.5
-  airspec: Fix log message duplication issue ([#609](https://github.com/wvlet/airframe/issues/609)) [[4536123](https://github.com/wvlet/airframe/commit/4536123)]
-  airspec: Support shouldBe null ([#607](https://github.com/wvlet/airframe/issues/607)) [[78c87dd](https://github.com/wvlet/airframe/commit/78c87dd)]
-  airframe-log: Handle InstanceAlreadyExistsException at logger JMX mbeam registration ([#608](https://github.com/wvlet/airframe/issues/608)) [[bd13110](https://github.com/wvlet/airframe/commit/bd13110)]

## 19.8.4
- AirSpec is renamed to a simple package `wvlet.airspec`.
  -  airspec: Rename to wvlet.airspec ([#605](https://github.com/wvlet/airframe/issues/605)) [[6d6ff91](https://github.com/wvlet/airframe/commit/6d6ff91)]
  -  airspec: Add optional PropertyBased testing [[9fbe8ca](https://github.com/wvlet/airframe/commit/9fbe8ca)]
  -  airspec: [#597](https://github.com/wvlet/airframe/issues/597): Add AirSpecContext to run nested tests ([#602](https://github.com/wvlet/airframe/issues/602)) [[a41211a](https://github.com/wvlet/airframe/commit/a41211a)]
  -  Make airspec an all-in-jar ([#601](https://github.com/wvlet/airframe/issues/601)) [[18d3970](https://github.com/wvlet/airframe/commit/18d3970)]
  -  airspec: Add assertEquals(a, b, delta) [[2ca677a](https://github.com/wvlet/airframe/commit/2ca677a)]
  -  airspec: Add inTravisCI checker [[9302a70](https://github.com/wvlet/airframe/commit/9302a70)]
  -  airspec: Return Nothing for asserts throwing exceptions [[7fb68e2](https://github.com/wvlet/airframe/commit/7fb68e2)]
  -  Migrated all airframe module tests from ScalaTest to AirSpec
- airframe-surface: disable createObjectFactry for abstract classes in Scala.js [[45b22ac](https://github.com/wvlet/airframe/commit/45b22ac)]

## 19.8.3
- AirSpec:
  -  airspec is now a standalone project without any cyclic dependencies to airframe module ([#592](https://github.com/wvlet/airframe/issues/592)) [[d0db028](https://github.com/wvlet/airframe/commit/d0db028)]
  -  Support shouldBe matchers ([#590](https://github.com/wvlet/airframe/issues/590)) [[0caf159](https://github.com/wvlet/airframe/commit/0caf159)]
  -  Always use ANSI color logs for Travis CI ([#596](https://github.com/wvlet/airframe/issues/596)) [[1c791fd](https://github.com/wvlet/airframe/commit/1c791fd)]
-  internal chaange: airframe-control: Use AirSpec for test cases ([#594](https://github.com/wvlet/airframe/issues/594)) [[d08c603](https://github.com/wvlet/airframe/commit/d08c603)]
-  Upgrade to Scala.js 0.6.28 ([#593](https://github.com/wvlet/airframe/issues/593)) [[00e88ab](https://github.com/wvlet/airframe/commit/00e88ab)]

## 19.8.2
-  airspec: Use airframe-log for AirSpec logging for improving the log messages ([#584](https://github.com/wvlet/airframe/issues/584)) [[857ae2c](https://github.com/wvlet/airframe/commit/857ae2c)]
- internal: Migrate test cases of airframe-log/surface to airspec

## 19.8.1
-  **AirSpec**: Added a new function-based testing library for Scala and Scala.js [#580](https://github.com/wvlet/airframe/pull/580)
   - source code: https://github.com/wvlet/airframe/tree/master/airspec
-  airframe-scalatest: Rename airframe-spec to airframe-scalatest [[51f8922](https://github.com/wvlet/airframe/commit/51f8922)]
-  airframe-surfaace: Support symbolic method names in Scala.js [[57d67fe](https://github.com/wvlet/airframe/commit/57d67fe)]
-  airframe-surface: Sort methodSurface based on the source code order [[bdf1a86](https://github.com/wvlet/airframe/commit/bdf1a86)]
-  Handle ClassNotFound issue of sbt-1.3.x by cleaning-up all custom LogHandlers [[e70ab0d](https://github.com/wvlet/airframe/commit/e70ab0d)]

## 19.8.0
-  airframe-http-recorder: Support binary requests and responses ([#579](https://github.com/wvlet/airframe/issues/579)) [[a0c5894](https://github.com/wvlet/airframe/commit/a0c5894)]
-  airframe-http-recorder: Support custom recording  ([#578](https://github.com/wvlet/airframe/issues/578)) [[d9b8ed2](https://github.com/wvlet/airframe/commit/d9b8ed2)]
-  airframe-json: Throw IntegerOverflow exceptions for too big numbers ([#575](https://github.com/wvlet/airframe/issues/575)) [[0e541ee](https://github.com/wvlet/airframe/commit/0e541ee)]
-  airframe-benchmark: Aggregate benchmark programs (msgpack-benchmark, json-benchmark) into one project ([#573](https://github.com/wvlet/airframe/issues/573)) [[67656ba](https://github.com/wvlet/airframe/commit/67656ba)]
-  [#475](https://github.com/wvlet/airframe/issues/475): Use Scala 2.13 compatible collection syntaxes ([#572](https://github.com/wvlet/airframe/issues/572)) [[6f20a16](https://github.com/wvlet/airframe/commit/6f20a16)]
-  airframe-json: Use exhaustive match for JSONValue.value ([#571](https://github.com/wvlet/airframe/issues/571)) [[f138d74](https://github.com/wvlet/airframe/commit/f138d74)]
-  airframe-http: Support Router.add[Filter], andThen[Filter] ([#570](https://github.com/wvlet/airframe/issues/570)) [[8335c39](https://github.com/wvlet/airframe/commit/8335c39)]
-  airframe-http: Wrap exceptions with Future for each filter and context ([#569](https://github.com/wvlet/airframe/issues/569)) [[1324385](https://github.com/wvlet/airframe/commit/1324385)]

## 19.7.6
-  airframe-http-recorder: [#559](https://github.com/wvlet/airframe/issues/559) Support expiration ([#567](https://github.com/wvlet/airframe/issues/567)) [[32a0224](https://github.com/wvlet/airframe/commit/32a0224)]
-  airframe-http: Allow setting FinagleServer name ([#566](https://github.com/wvlet/airframe/issues/566)) [[3f810f1](https://github.com/wvlet/airframe/commit/3f810f1)]
-  airframe-http: Return the last failed response with HttpClientException ([#565](https://github.com/wvlet/airframe/issues/565)) [[2099555](https://github.com/wvlet/airframe/commit/2099555)]
-  airframe-http-recorder: Exclude unnecessary request headers from records ([#563](https://github.com/wvlet/airframe/issues/563)) [[2246316](https://github.com/wvlet/airframe/commit/2246316)]
-  airframe-http-recorder: Exclude Date header from request hash ([#562](https://github.com/wvlet/airframe/issues/562)) [[8b75221](https://github.com/wvlet/airframe/commit/8b75221)]
-  airframe-http-recorder: Use case-insensitive request hashing ([#561](https://github.com/wvlet/airframe/issues/561)) [[20467a6](https://github.com/wvlet/airframe/commit/20467a6)]
-  airframe-http-recorder: Add createRecorderProxy ([#557](https://github.com/wvlet/airframe/issues/557)) [[bbba4db](https://github.com/wvlet/airframe/commit/bbba4db)]
-  Rename airframe-macros to airframe-di-macros ([#560](https://github.com/wvlet/airframe/issues/560)) [[9a6e871](https://github.com/wvlet/airframe/commit/9a6e871)]

## 19.7.5
-  airframe-http: Support reading raw response as is ([#553](https://github.com/wvlet/airframe/issues/553)) [[1ccedae](https://github.com/wvlet/airframe/commit/1ccedae)]
-  airframe-http-client: Add getResource for GET with query strings ([#552](https://github.com/wvlet/airframe/issues/552)) [[fd5849c](https://github.com/wvlet/airframe/commit/fd5849c)]
-  airframe-http-client: Support customizing http requests [#550](https://github.com/wvlet/airframe/issues/550) ([#551](https://github.com/wvlet/airframe/issues/551)) [[1542beb](https://github.com/wvlet/airframe/commit/1542beb)]
-  airframe-http: Add HttpFilter support to Router ([#540](https://github.com/wvlet/airframe/issues/540)) [[1cb7096](https://github.com/wvlet/airframe/commit/1cb7096)]

## 19.7.4
-  airframe-log: GraalVM support ([#549](https://github.com/wvlet/airframe/issues/549)) [[df8b408](https://github.com/wvlet/airframe/commit/df8b408)]
-  airframe-http: Fix over routing in airframe-http ([#547](https://github.com/wvlet/airframe/issues/547)) [[4eb6245](https://github.com/wvlet/airframe/commit/4eb6245)]
-  airframe-config: Allow reading config files from classpath, and add more examples ([#544](https://github.com/wvlet/airframe/issues/544)) [[697b04e](https://github.com/wvlet/airframe/commit/697b04e)]
-  airframe-json: Reuse StringBuilder [#381](https://github.com/wvlet/airframe/issues/381) ([#546](https://github.com/wvlet/airframe/issues/546)) [[04bd01f](https://github.com/wvlet/airframe/commit/04bd01f)]

## 19.7.3
-  airframe-metrics: Add TimeWindow.howMany(unit), minutesDiff, etc. ([#545](https://github.com/wvlet/airframe/issues/545)) [[77f7be9](https://github.com/wvlet/airframe/commit/77f7be9)]

## 19.7.2
-  Use map type MessagePack for ObjectCodec ([#541](https://github.com/wvlet/airframe/issues/541)) [[0ffbcf1](https://github.com/wvlet/airframe/commit/0ffbcf1)]
-  Fixed compile errors ([#543](https://github.com/wvlet/airframe/issues/543)) [[e480e94](https://github.com/wvlet/airframe/commit/e480e94)]
-  Fix typo `%%` -> `%` ([#542](https://github.com/wvlet/airframe/issues/542)) [[0f6d1f9](https://github.com/wvlet/airframe/commit/0f6d1f9)]
-  airframe-json: Seq[JSONValue].value ([#539](https://github.com/wvlet/airframe/issues/539)) [[b86f1f8](https://github.com/wvlet/airframe/commit/b86f1f8)]
-  airframe-json: DSL for JSON extraction ([#538](https://github.com/wvlet/airframe/issues/538)) [[21a6204](https://github.com/wvlet/airframe/commit/21a6204)]
-  Workaround for sbt 1.3.0-RC2 class loader issue for JMX test ([#537](https://github.com/wvlet/airframe/issues/537)) [[a031ea7](https://github.com/wvlet/airframe/commit/a031ea7)]
-  AnyCodec ([#536](https://github.com/wvlet/airframe/issues/536)) [[5cb1944](https://github.com/wvlet/airframe/commit/5cb1944)]

## 19.7.1
-  airframe-http: Fix routing when common token is present in the path ([#535](https://github.com/wvlet/airframe/issues/535)) [[abdea65](https://github.com/wvlet/airframe/commit/abdea65)]
-  Add example of programmable http-recorder ([#532](https://github.com/wvlet/airframe/issues/532)) [[cbab375](https://github.com/wvlet/airframe/commit/cbab375)]
-  fix type annotation syntax ([#534](https://github.com/wvlet/airframe/issues/534)) [[1b3daa1](https://github.com/wvlet/airframe/commit/1b3daa1)]
-  remove unused string interpolations ([#533](https://github.com/wvlet/airframe/issues/533)) [[a2267ac](https://github.com/wvlet/airframe/commit/a2267ac)]
-  airframe-http-finagle: Upgrade to Finagle 19.6.0 to support chunked reader [[61a0316](https://github.com/wvlet/airframe/commit/61a0316)]
-  airframe-http-client: Add requestFilter [[c32ba1e](https://github.com/wvlet/airframe/commit/c32ba1e)]
-  airframe-di: Show binding line numbers ([#530](https://github.com/wvlet/airframe/issues/530)) [[a136907](https://github.com/wvlet/airframe/commit/a136907)]

## 19.7.0
-  airframe-http-recorder: Programmable HTTP mock server ([#529](https://github.com/wvlet/airframe/issues/529)) [[f33cdba](https://github.com/wvlet/airframe/commit/f33cdba)]
  -  HTTP support in http-recorder ([#527](https://github.com/wvlet/airframe/issues/527)) [[4365522](https://github.com/wvlet/airframe/commit/4365522)]
-  airframe-control: Add Retry.retryable/nonRetryableFailure method ([#524](https://github.com/wvlet/airframe/issues/524)) [[4a7744b](https://github.com/wvlet/airframe/commit/4a7744b)]
-  airframe-codec: Add convenient methods to convert JSON <-> Scala object ([#519](https://github.com/wvlet/airframe/issues/519)) [[0c6bd55](https://github.com/wvlet/airframe/commit/0c6bd55)]
-  airframe-codec: [#513](https://github.com/wvlet/airframe/issues/513): Handle empty inputs at unpackJson and unpackMsgPack ([#514](https://github.com/wvlet/airframe/issues/514)) [[99d9cc6](https://github.com/wvlet/airframe/commit/99d9cc6)]
-  internal: Upgrade to sbt-1.3.0-RC2 ([#510](https://github.com/wvlet/airframe/issues/510)) [[3583430](https://github.com/wvlet/airframe/commit/3583430)]

## 19.6.1
-  Upgrade to Scala 2.13.0 ([#508](https://github.com/wvlet/airframe/issues/508)) [[8f6729d](https://github.com/wvlet/airframe/commit/8f6729d)]
-  airframe-fluentd: Bump to Fluency 2.3.2 ([#507](https://github.com/wvlet/airframe/issues/507)) [[1339ec5](https://github.com/wvlet/airframe/commit/1339ec5)]

## 19.6.0
-  surface: Use context class loader to fix [sbt/sbt#4760](https://github.com/sbt/sbt/issues/4760) ([#506](https://github.com/wvlet/airframe/issues/506)) [[f5d9dd8](https://github.com/wvlet/airframe/commit/f5d9dd8)]
-  metrics: Support exact start dates in TimeWindow ([#505](https://github.com/wvlet/airframe/issues/505)) [[23a91b9](https://github.com/wvlet/airframe/commit/23a91b9)]

## 19.5.2
-  Upgrade to Scala 2.13.0-RC3 ([#503](https://github.com/wvlet/airframe/issues/503)) [[4c54d79](https://github.com/wvlet/airframe/commit/4c54d79)]
-  Add more airframe examples ([#502](https://github.com/wvlet/airframe/issues/502)) [[2b4b06a](https://github.com/wvlet/airframe/commit/2b4b06a)]

## 19.5.1
-  Support Scala 2.13.0-RC2 ([#500](https://github.com/wvlet/airframe/issues/500)) [[7c3b329](https://github.com/wvlet/airframe/commit/7c3b329)]
-  Recover Scala 2.11 build as Spark 2.4.x still uses this Scala version for a while. ([#498](https://github.com/wvlet/airframe/issues/498)) [[30561c7](https://github.com/wvlet/airframe/commit/30561c7)]

## 19.5.0

-  The first release of the new Reiwa era! 
-  From this version, we will no longer support Scala 2.11. This is because Spark 2.4.2 now uses Scala 2.12 by default, and Spark has been the only reason for us to keep maintaining Airframe releases for Scala 2.11. ([#496](https://github.com/wvlet/airframe/issues/496)) [[4e37be8](https://github.com/wvlet/airframe/commit/4e37be8)]
-  Add more [Airframe DI code examples](https://github.com/wvlet/airframe/tree/master/examples/src/main/scala/wvlet/airframe/examples/di) ([#495](https://github.com/wvlet/airframe/issues/495)) [[8d3b311](https://github.com/wvlet/airframe/commit/8d3b311)]

## 19.4.2
-  airframe-http: Add HttpClient ([#482](https://github.com/wvlet/airframe/issues/482)) [[4bb9463](https://github.com/wvlet/airframe/commit/4bb9463)]
-  airframe-control: Simplified Retry interface so that it can be used as an independent module. 

## 19.4.1
-  Add Scala 2.13.0-RC1 support. ([#487](https://github.com/wvlet/airframe/issues/487)) [[3eb68a8](https://github.com/wvlet/airframe/commit/3eb68a8)]
-  airframe: Support DI tracing and coverage stats ([#486](https://github.com/wvlet/airframe/issues/486)) [[143c629](https://github.com/wvlet/airframe/commit/143c629)]
-  airframe-control: Add trace logs and JMX monitoring to Parallel ([#481](https://github.com/wvlet/airframe/issues/481)) [[29141a5](https://github.com/wvlet/airframe/commit/29141a5)]
-  airframe-codec: Support JSONValue, Json (raw json string)  ([#484](https://github.com/wvlet/airframe/issues/484)) [[70022a4](https://github.com/wvlet/airframe/commit/70022a4)]
-  Add airframe code [examples ([#485](https://github.com/wvlet/airframe/issues/485)) [[2e585b9](https://github.com/wvlet/airframe/commit/2e585b9)]
-  internal: Replace fluentd-standalone to MockFluentd ([#488](https://github.com/wvlet/airframe/issues/488)) [[fd7d075](https://github.com/wvlet/airframe/commit/fd7d075)]

## 19.4.0
-  airframe-http: Fix shared http path prefix match ([#483](https://github.com/wvlet/airframe/issues/483)) [[a0cb0b0](https://github.com/wvlet/airframe/commit/a0cb0b0)]
-  airframe-http: NFA-based fast HTTP route mapping ([#469](https://github.com/wvlet/airframe/issues/469)) [[27d2193](https://github.com/wvlet/airframe/commit/27d2193)]
-  airframe-jmx: Fix JMX methods extraction ([#480](https://github.com/wvlet/airframe/issues/480)) [[6c9e628](https://github.com/wvlet/airframe/commit/6c9e628)]
-  airframe-codec: Make hasNext idempotent as a workaround for https://github.com/scala/bug/issues/11453 ([#471](https://github.com/wvlet/airframe/issues/471)) [[6c542eb](https://github.com/wvlet/airframe/commit/6c542eb)]
-  internal: Remove Stream[X] usage for Scala 2.13.0 compatibility ([#477](https://github.com/wvlet/airframe/issues/477)) [[d92426f](https://github.com/wvlet/airframe/commit/d92426f)]

## 19.3.7
-  airframe-jmx: Add the default JMXAgent([#465](https://github.com/wvlet/airframe/issues/465)) [[f9937e5](https://github.com/wvlet/airframe/commit/f9937e5)]
-  airframe-jmx: Fix the registration of Scala-specific class names ([#461](https://github.com/wvlet/airframe/issues/461)) [[f853bf0](https://github.com/wvlet/airframe/commit/f853bf0)]
-  airframe-http: Remove the unnecessary finatra dependency ([#464](https://github.com/wvlet/airframe/issues/464)) [[681b006](https://github.com/wvlet/airframe/commit/681b006)]

## 19.3.6
-  airframe-json: Add JSON.parseAny(json) ([#458](https://github.com/wvlet/airframe/issues/458)) [[bce0589](https://github.com/wvlet/airframe/commit/bce0589)]
-  airframe-http: Support Router.add[X].add[Y]... syntax ([#456](https://github.com/wvlet/airframe/issues/456)) [[0ec6430](https://github.com/wvlet/airframe/commit/0ec6430)]

## 19.3.5
-  airframe-http: Accept: application/x-msgpack response support ([#448](https://github.com/wvlet/airframe/issues/448)) [[ccb55b7](https://github.com/wvlet/airframe/commit/ccb55b7)]
-  airframe-http: Support non-JSON content body ([#446](https://github.com/wvlet/airframe/issues/446)) [[f351916](https://github.com/wvlet/airframe/commit/f351916)]
-  airframe-http: Show the cause of an exception upon an internal server error ([#447](https://github.com/wvlet/airframe/issues/447)) [[52a6b02](https://github.com/wvlet/airframe/commit/52a6b02)]
-  airframe-http-finagle: Upgrade to Finagle 19.2.0 ([#442](https://github.com/wvlet/airframe/issues/442)) [[f6d3941](https://github.com/wvlet/airframe/commit/f6d3941)]
-  airframe-codec: Support MsgPack type (= Array[Byte]) codec ([#441](https://github.com/wvlet/airframe/issues/441)) [[5923aac](https://github.com/wvlet/airframe/commit/5923aac)]
-  airframe-codec: [#437](https://github.com/wvlet/airframe/issues/437) Case-insensitive enum mapping ([#439](https://github.com/wvlet/airframe/issues/439)) [[5b26e5a](https://github.com/wvlet/airframe/commit/5b26e5a)]
-  airframe-jdbc: Upgrade sqlite-jdbc to 3.27.2 for upsert support ([#445](https://github.com/wvlet/airframe/issues/445)) [[b7f3a57](https://github.com/wvlet/airframe/commit/b7f3a57)]
-  airframe-surface: Add Zero.register(surface, A) ([#440](https://github.com/wvlet/airframe/issues/440)) [[f16002f](https://github.com/wvlet/airframe/commit/f16002f)]
-  airframe-launcher: Fixes [#435](https://github.com/wvlet/airframe/issues/435). Hide the default command from the help message ([#438](https://github.com/wvlet/airframe/issues/438)) [[bcdd154](https://github.com/wvlet/airframe/commit/bcdd154)]
-  airframe-msgpack: Add a msgpack benchmark using JMH ([#437](https://github.com/wvlet/airframe/issues/437)) [[e5f8526](https://github.com/wvlet/airframe/commit/e5f8526)]

## 19.3.4
-  airframe-http-finagle: Fixes [#432](https://github.com/wvlet/airframe/issues/432): Router should be given from FinagleServerConfig ([#433](https://github.com/wvlet/airframe/issues/433)) [[a6a0687](https://github.com/wvlet/airframe/commit/a6a0687)]
-  airframe-sql: Embed table names into query signatures ([#431](https://github.com/wvlet/airframe/issues/431)) [[37e8f7a](https://github.com/wvlet/airframe/commit/37e8f7a)]
-  airframe-control: Implicit classes for parallel map syntax sugar ([#429](https://github.com/wvlet/airframe/issues/429)) [[09dd9d8](https://github.com/wvlet/airframe/commit/09dd9d8)]

## 19.3.3
-  airframe-http-finagle: Support adding tracing to FinagleServer ([#430](https://github.com/wvlet/airframe/issues/430)) [[1ab7281](https://github.com/wvlet/airframe/commit/1ab7281)]
   - Since this version `Router` needs to be included in `FinagleServerConfig(port, router)` instead of using `bind[Router]`
-  airframe-fluentd: Upgrade to Fluency 2.1.0 and disable SSL by default ([#428](https://github.com/wvlet/airframe/issues/428)) [[bb3498e](https://github.com/wvlet/airframe/commit/bb3498e)]
-  airframe-sql: A new module for SQL parsier and logical query plan models

## 19.3.2
-  airframe: Fix compilation error of bind[X].toInstance when using scope-local objects ([#423](https://github.com/wvlet/airframe/issues/423)) [[4840a93](https://github.com/wvlet/airframe/commit/4840a93)]
-  airframe-json: Fix StackOverflowError when parsing large arrays of JSONObjects ([#421](https://github.com/wvlet/airframe/issues/421)) [[1ef5a02](https://github.com/wvlet/airframe/commit/1ef5a02)]

## 19.3.1
-  airframe-http: Allow more flexible http path routing ([#418](https://github.com/wvlet/airframe/issues/418)) [[76b7995](https://github.com/wvlet/airframe/commit/76b7995)]
-  Add airframe overview illustration [[3cbdb45](https://github.com/wvlet/airframe/commit/3cbdb45)]
-  Add README for modules ([#416](https://github.com/wvlet/airframe/issues/416)) [[93e1b89](https://github.com/wvlet/airframe/commit/93e1b89)]
-  Move launcher annotations to Java src folder to reduce compiler warnings [[ce58f6e](https://github.com/wvlet/airframe/commit/ce58f6e)]

## 19.3.0
-  [#413](https://github.com/wvlet/airframe/issues/413): Add a workaround for bind[X].toInstance(...) compilation error [[9d0da31](https://github.com/wvlet/airframe/commit/9d0da31)]
-  Fixes [#373](https://github.com/wvlet/airframe/issues/373) Resolve build[X] compilation error in Scala 2.12 [[31c929c](https://github.com/wvlet/airframe/commit/31c929c)]
-  Support reading String as JSON array or map ([#411](https://github.com/wvlet/airframe/issues/411)) [[b5e4956](https://github.com/wvlet/airframe/commit/b5e4956)]
-  Add ResultSetCodec.toJsonSeq ([#409](https://github.com/wvlet/airframe/issues/409)) [[61c9627](https://github.com/wvlet/airframe/commit/61c9627)]
-  airframe-control: Add a simple library Parallel for parallel execution ([#406](https://github.com/wvlet/airframe/issues/406)) [[e04166e](https://github.com/wvlet/airframe/commit/e04166e)]

## 19.2.1
-  airframe-fluentd: Fluency 2.0 based Fluentd interface ([#404](https://github.com/wvlet/airframe/issues/404)) [[bd9d75e](https://github.com/wvlet/airframe/commit/bd9d75e)]
-  airframe-codec: Add MessageCodec.toJson ([#405](https://github.com/wvlet/airframe/issues/405)) [[8fbdcab](https://github.com/wvlet/airframe/commit/8fbdcab)]

## 19.2.0
- From this version, we will use `YY.MM.(patch)` version numbers. 
-  Add codecs for airframe-metrics ([#403](https://github.com/wvlet/airframe/issues/403)) [[112d2f6](https://github.com/wvlet/airframe/commit/112d2f6)]
-  Add MessageCodec.unpackJson ([#402](https://github.com/wvlet/airframe/issues/402)) [[cc0f9b3](https://github.com/wvlet/airframe/commit/cc0f9b3)]
-  Add Design.empty ([#398](https://github.com/wvlet/airframe/issues/398)) [[bbeaa06](https://github.com/wvlet/airframe/commit/bbeaa06)]
-  Use @command(isDefault=true) instead of @defaultCommand ([#397](https://github.com/wvlet/airframe/issues/397)) [[55439e5](https://github.com/wvlet/airframe/commit/55439e5)]
-  Add translucent background logo [[e2b0ac2](https://github.com/wvlet/airframe/commit/e2b0ac2)]
-  Add codecs for converting JDBC ResultSets to msgpack ([#391](https://github.com/wvlet/airframe/issues/391)) [[9d0f29b](https://github.com/wvlet/airframe/commit/9d0f29b)]
-  Add ServerAddress for passing host and port data ([#390](https://github.com/wvlet/airframe/issues/390)) [[3b7e083](https://github.com/wvlet/airframe/commit/3b7e083)]
-  Upgrade to scala.js 0.26 ([#388](https://github.com/wvlet/airframe/issues/388)) [[a415c71](https://github.com/wvlet/airframe/commit/a415c71)]
-  Add airframe-http-recorder ([#380](https://github.com/wvlet/airframe/issues/380)) [[d34702f](https://github.com/wvlet/airframe/commit/d34702f)]
-  Upgrade Finagle to 19.1.0 ([#379](https://github.com/wvlet/airframe/issues/379)) [[a211073](https://github.com/wvlet/airframe/commit/a211073)]
-  Add airframe-canvas, an off-heap memory buffer library ([#367](https://github.com/wvlet/airframe/issues/367)) [[e55a3f4](https://github.com/wvlet/airframe/commit/e55a3f4)]

## 0.80
-  Add ArrayJSONCodec to parse JSON array string ([#375](https://github.com/wvlet/airframe/issues/375)) [[8c624a7](https://github.com/wvlet/airframe/commit/8c624a7)]
-  Get ConfigHolders directly from Design ([#374](https://github.com/wvlet/airframe/issues/374)) [[da4d3ba](https://github.com/wvlet/airframe/commit/da4d3ba)]
-  Not ignore resource closing exception in withResource ([#371](https://github.com/wvlet/airframe/issues/371)) [[129868a](https://github.com/wvlet/airframe/commit/129868a)]
-  airframe-stream is moved to wvlet/msgframe ([#365](https://github.com/wvlet/airframe/issues/365)) [[d150ae8](https://github.com/wvlet/airframe/commit/d150ae8)]
-  Fixes [#362](https://github.com/wvlet/airframe/issues/362): Set Option[Boolean] with --(option) in airframe-launcher ([#364](https://github.com/wvlet/airframe/issues/364)) [[f383c5e](https://github.com/wvlet/airframe/commit/f383c5e)]
-  Change airframe-control to a Scala JVM project ([#363](https://github.com/wvlet/airframe/issues/363)) [[5d6632d](https://github.com/wvlet/airframe/commit/5d6632d)]
-  Set the code coverage test threshold to 80% overall, 5% diff ([#361](https://github.com/wvlet/airframe/issues/361)) [[0519d4b](https://github.com/wvlet/airframe/commit/0519d4b)]

## 0.79
-  Enhance loan pattern for AutoCloseable ([#359](https://github.com/wvlet/airframe/issues/359)) [[f971934](https://github.com/wvlet/airframe/commit/f971934)]
-  Add name conversion functionallity to CName ([#360](https://github.com/wvlet/airframe/issues/360)) [[366caca](https://github.com/wvlet/airframe/commit/366caca)]
-  Allows using bindFactory outside of wvlet.airframe package ([#358](https://github.com/wvlet/airframe/issues/358)) [[d8836e4](https://github.com/wvlet/airframe/commit/d8836e4)]
-  Upgrade to Scala 2.11.12 ([#356](https://github.com/wvlet/airframe/issues/356)) [[bc6b172](https://github.com/wvlet/airframe/commit/bc6b172)]

## 0.78
-  Add close() to Unpacker interface ([#348](https://github.com/wvlet/airframe/issues/348)) [[88c3749](https://github.com/wvlet/airframe/commit/88c3749)]

## 0.77
-  airframe-launcher: Fix array argument mapping ([#342](https://github.com/wvlet/airframe/issues/342)) [[00145b6](https://github.com/wvlet/airframe/commit/00145b6)]
-  Upgrade to Scala 2.12.8 ([#341](https://github.com/wvlet/airframe/issues/341)) [[b5b6f9e](https://github.com/wvlet/airframe/commit/b5b6f9e)]
- internal: Enhance SQL parser

## 0.76
[airframe DI]
- Support creating [child sessions](https://wvlet.org/airframe/docs/#child-sessions) with `session.withChildSession`. [#321](https://github.com/wvlet/airframe/issues/321) 
- Improve Design.+ performance by checking Design duplicates right before building a new Session ([#334](https://github.com/wvlet/airframe/issues/334)) [[3e13659](https://github.com/wvlet/airframe/commit/3e13659)]
- Add session id to logs [[0cbd9cc](https://github.com/wvlet/airframe/commit/0cbd9cc)]
- Improved the debug log message by showing session hierarchy and injection behavior [[05c014d](https://github.com/wvlet/airframe/commit/05c014d)]
- [internal] Do not register shutdown hooks for child sessions to avoid closing loggers [[a6460cd](https://github.com/wvlet/airframe/commit/a6460cd)]

[airframe-launcher]
- [bug] Fix in getting the default values of method arguments. ([#339](https://github.com/wvlet/airframe/issues/339)) [[438f2cc](https://github.com/wvlet/airframe/commit/438f2cc)]

[airframe-log]
- Show logs during shutdown hooks by using a custom LogManager so as not to close logger during shutdown hooks [[4dffce2](https://github.com/wvlet/airframe/commit/4dffce2)]
- Show  millisec in log timestamps ([#335](https://github.com/wvlet/airframe/issues/335)) [[7b6ec05](https://github.com/wvlet/airframe/commit/7b6ec05)]

[misc.]
- [airframe-config] [#268](https://github.com/wvlet/airframe/issues/268): Allow hyphens in key names ([#333](https://github.com/wvlet/airframe/issues/333)) [[c237b84](https://github.com/wvlet/airframe/commit/c237b84)]
- [airframe-jdbc] [#133](https://github.com/wvlet/airframe/issues/133): Add DbConfig helper ([#332](https://github.com/wvlet/airframe/issues/332)) [[d274448](https://github.com/wvlet/airframe/commit/d274448)]
- [airframe-codec] BinaryValue encoding was missing ([#331](https://github.com/wvlet/airframe/issues/331)) [[4eb8899](https://github.com/wvlet/airframe/commit/4eb8899)]

## 0.75
- airframe-di: Fix a bug the default values in a constructor are registered as singletons [[40d97ca](https://github.com/wvlet/airframe/commit/40d97ca)]
- airframe-control: Allow passing retry context ([#327](https://github.com/wvlet/airframe/issues/327)) [[afb34b9](https://github.com/wvlet/airframe/commit/afb34b9)]
- [internal] Upgrade to sbt 1.2.7 ([#325](https://github.com/wvlet/airframe/issues/325)) [[b6ebaf1](https://github.com/wvlet/airframe/commit/b6ebaf1)]

## 0.74

- Airframe now has its brand logo!
<p>
<img src="https://github.com/wvlet/airframe/raw/master/logos/airframe_icon_small.png" alt="logo" width="150px">
</p>

-  airframe-launcher: Fix sub method command help message [[3b22d74](https://github.com/wvlet/airframe/commit/3b22d74)]
-  Deprecate in-trait bindInstance[X] ([#317](https://github.com/wvlet/airframe/issues/317)) [[1f15aef](https://github.com/wvlet/airframe/commit/1f15aef)]
-  Deprecate in-trait bindSingleton[X] now that bind[X] binds singletons by default ([#300](https://github.com/wvlet/airframe/issues/300)) [[fe75bb0](https://github.com/wvlet/airframe/commit/fe75bb0)]
-  airframe-http: Register a trait factory when Route.of[A] is called ([#307](https://github.com/wvlet/airframe/issues/307)) [[2cf505b](https://github.com/wvlet/airframe/commit/2cf505b)]
-  airframe-surface: Rename surface.of[X] -> Surface.of[X] ([#306](https://github.com/wvlet/airframe/issues/306)) [[cea2cbb](https://github.com/wvlet/airframe/commit/cea2cbb)]
-  airframe-codec is now a pure-Scala SPI ([#302](https://github.com/wvlet/airframe/issues/302)) [[7677d26](https://github.com/wvlet/airframe/commit/7677d26)]
  -  airframe-codec: Scala.js support  ([#303](https://github.com/wvlet/airframe/issues/303)) [[ff7bd97](https://github.com/wvlet/airframe/commit/ff7bd97)]
- Internal changes: 
   -  airframe-macros is now a pure Scala project ([#319](https://github.com/wvlet/airframe/issues/319)) [[43ec5d5](https://github.com/wvlet/airframe/commit/43ec5d5)]
   -  Add an aggregated project for Scala community-build ([#316](https://github.com/wvlet/airframe/issues/316)) [[fa78beb](https://github.com/wvlet/airframe/commit/fa78beb)]
   -  Upgrade to sbt-microsites 0.7.26 [[40f0b7f](https://github.com/wvlet/airframe/commit/40f0b7f)]
   -  Fix twitter icon card [[0217f54](https://github.com/wvlet/airframe/commit/0217f54)]
   -  Update logos [[e295e8e](https://github.com/wvlet/airframe/commit/e295e8e)]
   -  Add a workaround: Surface.of[A] throws InternalError on REPL ([#308](https://github.com/wvlet/airframe/issues/308)) [[1be3796](https://github.com/wvlet/airframe/commit/1be3796)]

## 0.73
-  airfarme-opts is redesigned as [airframe-launcher](https://wvlet.org/airframe/docs/airframe-launcher.html) Option parser v2 ([#295](https://github.com/wvlet/airframe/issues/295)) [[a270128](https://github.com/wvlet/airframe/commit/a270128)]
-  Support netsted offset in time window ([#296](https://github.com/wvlet/airframe/issues/296)) [[0c6e218](https://github.com/wvlet/airframe/commit/0c6e218)]
-  Add untruncate notation `)` to offset of time window [[3d479be](https://github.com/wvlet/airframe/commit/3d479be)]
-  Add wvlet.airframe.log.init for using default log configurations ([#291](https://github.com/wvlet/airframe/issues/291)) [[a08d15d](https://github.com/wvlet/airframe/commit/a08d15d)]
-  Use fluentd-standalone 1.2.6.1, which supports Scala 2.13.0-M5 ([#289](https://github.com/wvlet/airframe/issues/289)) [[ed7012b](https://github.com/wvlet/airframe/commit/ed7012b)]

## 0.72
-  Support Scala 2.13.0-M5 ([#232](https://github.com/wvlet/airframe/issues/232)) [[c31a4da](https://github.com/wvlet/airframe/commit/c31a4da)]
   - Notice: Due to a [bug](https://github.com/scala/bug/issues/11192) of Scala 2.13.0-M5, serialization of Design objects may not work when using sbt with Scala 2.13.0-M5. Using `fork in Test := true` is a workaround.

## 0.71
-  Added [airframe-fluentd](https://wvlet.org/airframe/docs/airframe-fluentd.html) for sending object-based metrics to Fluentd. ([#286](https://github.com/wvlet/airframe/issues/286)) [[28a70c3](https://github.com/wvlet/airframe/commit/28a70c3)]

## 0.70
-  airframe-control: Add a shell command launcher ([#287](https://github.com/wvlet/airframe/issues/287)) [[1339caf](https://github.com/wvlet/airframe/commit/1339caf)]
-  Upgrade to sbt 1.2.6 ([#285](https://github.com/wvlet/airframe/issues/285)) [[614dba6](https://github.com/wvlet/airframe/commit/614dba6)]
-  Fix typos in docs

## 0.69
-  surface: [#270](https://github.com/wvlet/airframe/issues/270): Move wvlet.surface -> wvlet.airframe.surface ([#272](https://github.com/wvlet/airframe/issues/272)) [[5ca3143](https://github.com/wvlet/airframe/commit/5ca3143)]
-  docs: Add note on scalafmt [[6867642](https://github.com/wvlet/airframe/commit/6867642)]
-  docs: Add [airframe-opts](https://wvlet.org/airframe/docs/airframe-launcher.html) docs [[dd4ecb4](https://github.com/wvlet/airframe/commit/dd4ecb4)]
-  internal: Enable using ctrl+c in sbt console [[143053b](https://github.com/wvlet/airframe/commit/143053b)]

## 0.68
-  Support JDK11 ([#265](https://github.com/wvlet/airframe/issues/265)) [[7e8799d](https://github.com/wvlet/airframe/commit/7e8799d)]
-  Use openjdk because we will mostly use this free JDK versions ([#266](https://github.com/wvlet/airframe/issues/266)) [[fd7f641](https://github.com/wvlet/airframe/commit/fd7f641)]
-  Upgrade to Scala 2.12.7 for Travis builds [[ce10f55](https://github.com/wvlet/airframe/commit/ce10f55)]
-  Remove snapshot repo resolver to speedup dependency download ([#267](https://github.com/wvlet/airframe/issues/267)) [[247fa28](https://github.com/wvlet/airframe/commit/247fa28)]

## 0.67
-  Add bindFactory[A => B] ([#262](https://github.com/wvlet/airframe/issues/262)) [[a38375f](https://github.com/wvlet/airframe/commit/a38375f)]
  - Usage: [Factory Binding](https://wvlet.org/airframe/docs/use-cases.html#factory-binding)
  - Simplified the implementation of AirframeSession
- Fixes typo, misspelled methods, etc. Thanks [Kazuhiro Sera](https://github.com/seratch)  
   - Fix [#256](https://github.com/wvlet/airframe/issues/256) by limiting the visibility of Design.blanc ([#259](https://github.com/wvlet/airframe/issues/259)) [[f0ce25a](https://github.com/wvlet/airframe/commit/f0ce25a)]
   - Fix typo, misspelling words, unnecessary imports ([#258](https://github.com/wvlet/airframe/issues/258)) [[b22f4cf](https://github.com/wvlet/airframe/commit/b22f4cf)]
   - Fix typo in http,finagle modules ([#254](https://github.com/wvlet/airframe/issues/254)) [[b008335](https://github.com/wvlet/airframe/commit/b008335)]
   - Use the deprecated annotation with proper attributes ([#257](https://github.com/wvlet/airframe/issues/257)) [[0843f79](https://github.com/wvlet/airframe/commit/0843f79)]
-  Bump sbt from 1.2.1 to 1.2.3 ([#255](https://github.com/wvlet/airframe/issues/255)) [[428a284](https://github.com/wvlet/airframe/commit/428a284)]
-  Add links to Medium blogs [[4d13933](https://github.com/wvlet/airframe/commit/4d13933)]

## 0.66
- Add Finagle startup helper ([#252](https://github.com/wvlet/airframe/issues/252)) [[d3b4767](https://github.com/wvlet/airframe/commit/d3b4767)]
  - Add airframe-http-finagle examples ([#254](https://github.com/wvlet/airframe/issues/253)) [[4698ba5](https://github.com/wvlet/airframe/commit/4698ba5)]
- [#249](https://github.com/wvlet/airframe/issues/249): Do not create factories for path-dependent types ([#250](https://github.com/wvlet/airframe/issues/250)) [[d2433bb](https://github.com/wvlet/airframe/commit/d2433bb)]

## 0.65
- Seamless integration with airframe-config and Design: https://wvlet.org/airframe/docs/airframe-config.html#using-with-airframe
  - `import wvlet.airframe.config._` to use newly added binding methods: `bindConfig[X]`, `bindConfigFromYaml[X](yamlFile)`, etc. ([#248](https://github.com/wvlet/airframe/issues/248)) [[379332b](https://github.com/wvlet/airframe/commit/379332b)]
  - Deprecated airframe-bootstrap
- Allow setting log levels using wvlet.logger JMX MBean ([#247](https://github.com/wvlet/airframe/issues/247)) [[962d6c2](https://github.com/wvlet/airframe/commit/962d6c2)]

## 0.64
- Support higher kinded types more naturally ([#244](https://github.com/wvlet/airframe/issues/244)) [[e21baef](https://github.com/wvlet/airframe/commit/e21baef)]

## 0.63
- Support higher-kinded type bindings. ([#242](https://github.com/wvlet/airframe/issues/242)) [[cfe35ed](https://github.com/wvlet/airframe/commit/cfe35ed)]
- Upgrade to scala.js 0.6.25 ([#243](https://github.com/wvlet/airframe/issues/243)) [[fc91387](https://github.com/wvlet/airframe/commit/fc91387)]
- Change withoutLifeCycleLogging -> noLifeCycleLogging [[501b88b](https://github.com/wvlet/airframe/commit/501b88b)]

## 0.62
- Add unpackBytes for compatibility ([#240](https://github.com/wvlet/airframe/issues/240)) [[ecd8367](https://github.com/wvlet/airframe/commit/ecd8367)]
- Truncate to duration unit when the exact date is given to the offset ([#239](https://github.com/wvlet/airframe/issues/239)) [[68d497e](https://github.com/wvlet/airframe/commit/68d497e)]

## 0.61
- Support bind[Design] to reference the original design ([#237](https://github.com/wvlet/airframe/issues/237)) [[7d362c9](https://github.com/wvlet/airframe/commit/7d362c9)]

## 0.60
- Added Design.withoutLifeCycleLogging, Design.withProductionMode, Design.withLazyMode
  - [#233](https://github.com/wvlet/airframe/issues/233): Allow disabling LifeCycleLogger ([#234](https://github.com/wvlet/airframe/issues/234)) [[4d2c80a](https://github.com/wvlet/airframe/commit/4d2c80a)]
- Support Future[X] return type in http-finagle ([#230](https://github.com/wvlet/airframe/issues/230)) [[3642cc6](https://github.com/wvlet/airframe/commit/3642cc6)]
- Upgrade to scalafmt 1.5.1 ([#231](https://github.com/wvlet/airframe/issues/231)) [[da84852](https://github.com/wvlet/airframe/commit/da84852)]

## 0.59
- Do not escape forward slashes in JSONStrings ([#229](https://github.com/wvlet/airframe/issues/229)) [[66f77f6](https://github.com/wvlet/airframe/commit/66f77f6)]
- Fix a bug when reading None in OptionCodec ([#228](https://github.com/wvlet/airframe/issues/228)) [[cda0d36](https://github.com/wvlet/airframe/commit/cda0d36)]

## 0.58
- use canonical names for arguments of method surface on MethodCallBuilder ([#227](https://github.com/wvlet/airframe/issues/227)) [[33cad91](https://github.com/wvlet/airframe/commit/33cad91)]

## 0.57
- Move wvlet.config to wvlet.airframe.config ([#226](https://github.com/wvlet/airframe/issues/226)) [[e103a89](https://github.com/wvlet/airframe/commit/e103a89)]
- Allow setting object codec factory for generating natural JSON responses ([#224](https://github.com/wvlet/airframe/issues/224)) [[0b42960](https://github.com/wvlet/airframe/commit/0b42960)]
- Use private[this] to optimize json parsing ([#225](https://github.com/wvlet/airframe/issues/225)) [[8432af8](https://github.com/wvlet/airframe/commit/8432af8)]

## 0.56
- airframe-http-finagle enhancement ([#223](https://github.com/wvlet/airframe/issues/223)) [[6d8b361](https://github.com/wvlet/airframe/commit/6d8b361)]
- Support quarter (q) duration ([#220](https://github.com/wvlet/airframe/issues/220)) [[fa56875](https://github.com/wvlet/airframe/commit/fa56875)]

## 0.55
- airframe-http: Add rest mapping ([#207](https://github.com/wvlet/airframe/issues/207)) [[43bcb0a](https://github.com/wvlet/airframe/commit/43bcb0a)]
  - Support HTTPRequest binding, request body to custom class binding
- airframe-json performance optimization ([#214](https://github.com/wvlet/airframe/issues/214)) [[7b83bea](https://github.com/wvlet/airframe/commit/7b83bea)]
  - Optimize JSON object builder ([#216](https://github.com/wvlet/airframe/issues/216)) [[ac3ba86](https://github.com/wvlet/airframe/commit/ac3ba86)]
- Drop jdk9 build for time saving [[dc96c00](https://github.com/wvlet/airframe/commit/dc96c00)]

## 0.54
- Add airframe-json. A fast JSON parser for Scala and Scala.js. This also supports JSON pull parsing.
  - Optimize number parser ([#213](https://github.com/wvlet/airframe/issues/213)) ([fd89e62](https://github.com/wvlet/airframe/commit/fd89e62))
  - JSON parse performance improvement ([#212](https://github.com/wvlet/airframe/issues/212)) ([897ca38](https://github.com/wvlet/airframe/commit/897ca38))
  - Use airframe-json for JSONCodec ([#211](https://github.com/wvlet/airframe/issues/211)) ([61b9ba7](https://github.com/wvlet/airframe/commit/61b9ba7))
  - Add airframe-json ([#208](https://github.com/wvlet/airframe/issues/208)) ([6aab1fb](https://github.com/wvlet/airframe/commit/6aab1fb))
- Add release automation script ([eeb4d2b](https://github.com/wvlet/airframe/commit/eeb4d2b))

## 0.53
- Publish Scala 2.12 only jvm projects ([97550b2](https://github.com/wvlet/airframe/commit/97550b2))
- Add Simple HTTP REST framework ([#206](https://github.com/wvlet/airframe/issues/206)) ([1467934](https://github.com/wvlet/airframe/commit/1467934))
- Bump sbt version to 1.2.1, also upgrading sbt plugins ([#205](https://github.com/wvlet/airframe/issues/205)) ([e5d2e1a](https://github.com/wvlet/airframe/commit/e5d2e1a))
- Upgrade to sbt 1.2.0 ([#202](https://github.com/wvlet/airframe/issues/202)) ([382eda6](https://github.com/wvlet/airframe/commit/382eda6))
- Upgrade sbt plugins ([#201](https://github.com/wvlet/airframe/issues/201)) ([7bfe82b](https://github.com/wvlet/airframe/commit/7bfe82b))
- [#198](https://github.com/wvlet/airframe/issues/198): Show global options as well to help messages ([#199](https://github.com/wvlet/airframe/issues/199)) ([16c2b66](https://github.com/wvlet/airframe/commit/16c2b66))

## 0.52
 - Support Scala 2.13.0-M4

## 0.51
 - airframe-log: Add Logger.clearAllHandlers (#195)

## 0.50
 - airframe-control: Add a code for retrying code execution
 - airframe-stream: Add a simple SQL parser/interpreter for stream processing https://github.com/wvlet/airframe/projects/1

## 0.49
 - __Important__: `bind[X]`, `bind[X].to[Y]`, etc. now use singleton bindings by default. This is because most of the use cases expect binding
 the same objects to traits. If you need to explicitly instantiate objects for each binding, use `bindInstance[X]` or `bind[X].toInstanceOf[Y]`.
 - Add a handy method to start a session and build an object: `Design.build[A]{ a => ... }`.
 - Support eager singleton initialization with `Design.buildProduction[A]` and `Design.withProductionSession`.
 - (internal) Upgrade to sbt 1.1.6, Scala.js 0.6.23, scalajs-java-logging 0.1.4

## 0.48
 - Java 9/10 support
 - airframe-msgpack. Fixes a bug in encoding Timestamp (Instant) values
 - airframe DI: Enhanced warning/error messages upon missing dependencies.

## 0.47
 - airframe-msgpack: Add a pure Scala [MessagePack](https://msgpack.org) implementation

## 0.46
 - Add airframe-bootstrap to define application modules that can be configured with airframe-config and DI
 - Upgrade to Scala 2.12.6

## 0.45
 - airframe-codec: Support `List[X]` coding
 - airframe-surface: Support `Parameter.getDefaultValue` and `Parameter.get(obj)` in Scala.js  

## 0.44
 - airframe-metrics: Require minus sign to represent last time ranges (e.g., -7d, -1h, etc.)
 - airframe: Enable `bind[Session]` binding to retrieve the current session

## 0.43
 - airframe-log: Set a default log handler (JSConsoleLogHandler) for Scala.js for convenience

## 0.42
 - airframe-log: Add an option to totally disable logging code generation.

## 0.41
 - Fix a singleton binding bug: If `bind[A].toSingletonOf[B]` is used and A is non-abstract trait, A was used instead of B.

## 0.40
 - airframe-launcher: Fixes [#147](https://github.com/wvlet/airframe/issues/147) when reading the default values of nested options

## 0.39
 - Add a unified scaladoc for all modules

## 0.38
 - airframe-tablet: Using json4s-native instead of play-json for Scala 2.13.0-M3 support

## 0.37
 - [airframe-tablet](airframe-tablet.html) Add play-json based JSONCodec to enable transformation between JSON <-> MessagePack <-> Object.
 - Temporarily remove Scala 2.13.0-M2 build. Now preparing 2.13.0-M3 build in [#143](https://github.com/wvlet/airframe/pull/143)

## 0.36
 - Fixes Logger.scanLoglevels to use given log level files appropriately [#141](https://github.com/wvlet/airframe/pull/141)

## 0.35
 - Minor fixes to project structures and build scripts.

## 0.34
 - No major change in terms of features, API.
 - Airframe now uses [sbt-dnyver](https://github.com/dwijnand/sbt-dynver) for auto versioning.
 - For each commit in the master branch, you can find a snapshot version from https://oss.sonatype.org/content/repositories/snapshots/org/wvlet/airframe/
   - For example, [airframe-log_2.12/0.33.1+12-682c4e07-SNAPSHOT](https://oss.sonatype.org/content/repositories/snapshots/org/wvlet/airframe/airframe-log_2.12/0.33.1+12-682c4e07-SNAPSHOT/), means that this version is based on the version `0.33.1` and `12` commits away from the previous version tag, and using git revision `682c4e07`.
 - The release versions will be like `0.34` as usual and available from Maven central.

## 0.33
 - Add [airframe-jdbc](airframe-jdbc.html), a reusable JDBC connection pool implementation.  

## 0.32
 - Fix ObjectCodec for map type value [#131](https://github.com/wvlet/airframe/pull/131)

## 0.31
 - Support recursive types

## 0.30
 - *airframe-metrics*: Add support for Scala.js (DataSize, ElapsedTime)
 - Add ElapsedTime for succinct duration representation

## 0.29
 - Add airframe-codec, a [MessgePack](https://msgpack.org)-based schema-on-read data transcoder.

## 0.28
 - Add support for Scala 2.13.0-M2

## 0.27
 - aiframe-config: Fixes nested case class binding [#108](https://github.com/wvlet/airframe/issues/110)
 - Dropped support of Scala.js + Scala 2.11 combination. Scala.js libraries will support only Scala 2.12 (or higher) in upcoming releases

## 0.26
 - Add an initial version of [airframe-tablet](https://github.com/wvlet/airframe/tree/master/tablet), a [MessagePack](https://msgpack.org) based data transcoder

## 0.25
 - **airframe-log** Upgrade the internal log rotation library to logback-core 1.2.3
 - Upgrade to Scala 2.12.4 and sbt 1.0.3. 

## 0.24
 - Add [airframe-metrics](airframe-metrics.html)

## 0.23
 - Moved to `org.wvlet.airframe` organization because Airframe will have more modules in future.
 - Added airframe-spec (a common library for testing airframe modules)

## 0.22
 - Add Logger.clearAllHandlers to clean-up existing log handlers set by third-party libraries
 - Apply scalafmt code style 

## 0.21
 - Add [airframe-launcher](https://github.com/wvlet/airframe/tree/master/airframe-opts) command line parser.

## 0.20
 - Migrated [airframe-log](https://github.com/wvlet/airframe/tree/master/airframe-log) from [wvlet-log](https://github.com/wvlet/log) because 
these are often commonly used.

## 0.19
 - Add [airframe-jmx](https://github.com/wvlet/airframe/tree/master/airframe-jmx) to expose object parameters through JMX

## 0.18
 - Add beforeShutdown, onInject lifecycle hooks
 - Support `@PostConstruct` and `@PreDestroy` JSR-250 annotations (Scala.js is not supported)
 - Fix to call onInit, onStart only once for singletons

## 0.17
 - Upgrade to Scala 2.12.3, Scala.js 0.6.19
 - Add Design.add
 - Fix a bug, onStart lifecycle hook is not called when a session is already started
 - Fix to use default constructor argument values for constructor bindings
   - Note: Scala.js does not support default parameter binding yet
 
## 0.16
 - Support reading Map type values in YamlReader (airframe config)

## 0.15
 - Surface will be generated by using runtime-type information. This improves the compilation speed in Scala JVM projects.
   - Scala.js version still uses compile-time surface generation
 - Interface change: `Surface.of[X]` to `surface.of[X]`.
   - This is for providing different implementations of Surface for ScalaJVM and Scala.js.
 - Fixed an issue when tagged type is used inside constructor parameters.
 - Fixed an issue that lifecycle hooks are wrongly called when debug level logging is used.
 - Merged wvlet-config as airframe-config. This module is useful for creating configuration objects from YAML file.

## 0.14
 - [Surface](https://github.com/wvlet/airframe/tree/master/surface) is now a part of Airframe
 - Added RuntimeSurface for the convenience of creating Surface from runtime-type information (reflect.universe.Type)
 - Added ObjectBuilder to build objects from Surface information

## 0.13
 - Add tagged binding support

## 0.12
 - Airframe is now refrection-free.
   - Using a reflection free [Surface](https://github.com/wvlet/surface) instead of [ObjectSchema](https://github.com/wvlet/object-schema)
   - Cyclic dependencies now can be found at compile-time thanks to Surface
 - Initial Scala.js support
 - Split Travis build process for the matrix of Scala 2.12/2.11, ScalaJVM/JS
 - Add shortcut for life cycle events: bind[X].onInit/onStart/onShutdown
 - Deprecated tagged type binding. Instead you can use type alias bind[(type alias)].
 - Deprecated @PreDestroy, @PostConstruct lifecycle binding because it complicates the order of life cycle hooks when mixed in
 - Deprecated passing Session as a class parameter or method return value. 
 - Simplified Scala macro codes

## 0.10
 - Session now implements AutoClosable
 - Add Design.withSession{ session => ... } to close Session at ease 

## 0.9
 - Add Scala 2.12 support

## 0.8
 - Split the project into two sub projects: airframe and airframe-macro. However, you only need airframe.jar for using Airframe.
 - Improved the performance by replacing run-time code generation to compile-time code generation #25
 - Upgrade to wvlet-log 1.0 
 - Upgrade wvlet-obj 0.26 (support type aliases)
 - Allow binding to type aliases

## 0.7
 - Fix lifecycle management of objects generated by bindSingleton

## 0.6
 - Add bindSingleton[X]

## 0.5 
 - Fix a bug in FILO order init/shutdown when objects are singleton
 - Add toProvider/toSingletonProvider/toEagerSingletonProvider
 - Add Design.remove[X]

## 0.4 
 - Improved binding performance
 - Fix FIFO lifecycle hook executor
 - Improved injection logging

## 0.3 
 - Support bind(ObjectType).toXXX 

## 0.2
 - Add lifecycle manager
 - Reorganize Session, Design classes
 - Test coverage improvement
 - Deprecated Design.build[X]. Use Design.newSession.build[X]

## 0.1
 - Migrated from wvlet-inject
