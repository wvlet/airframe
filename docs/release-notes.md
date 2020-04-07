---
id: release-notes
layout: docs
title: Release Notes
---

Airframe uses YY.MM.patch versioning scheme, so the version numbers match with the release year and month.   

## 20.4.0

Changes and bug fixes:

-  airframe-http-finagle: Update finagle-core, finagle-http, ... to 20.4.0 ([#1024](https://github.com/wvlet/airframe/issues/1024)) [[fc850c2](https://github.com/wvlet/airframe/commit/fc850c2)]
-  airframe-http: [#1003](https://github.com/wvlet/airframe/issues/1003) Use custom codec for request mapping ([#1018](https://github.com/wvlet/airframe/issues/1018)) [[2d719bc](https://github.com/wvlet/airframe/commit/2d719bc)]
-  airframe-http: Simplify Request/Response adapters ([#1017](https://github.com/wvlet/airframe/issues/1017)) [[5b4ef31](https://github.com/wvlet/airframe/commit/5b4ef31)]
-  airframe-jdbc: Fix port in jdbcUrl ([#1028](https://github.com/wvlet/airframe/issues/1028)) [[9f38aea](https://github.com/wvlet/airframe/commit/9f38aea)]
-  airframe-jdbc: Update postgresql to 42.2.12 ([#1022](https://github.com/wvlet/airframe/issues/1022)) [[1aec50b](https://github.com/wvlet/airframe/commit/1aec50b)]
-  airframe-control: Break execution in Parallel ([#1023](https://github.com/wvlet/airframe/issues/1023)) [[7dc6d9d](https://github.com/wvlet/airframe/commit/7dc6d9d)]
-  airspec: [#1019](https://github.com/wvlet/airframe/issues/1019) Add scala-reflect to the airspec dependency ([#1020](https://github.com/wvlet/airframe/issues/1020)) [[269224a](https://github.com/wvlet/airframe/commit/269224a)]

Internal changes:

-  Update sbt, scripted-plugin to 1.3.9 ([#1021](https://github.com/wvlet/airframe/issues/1021)) [[d5f928d](https://github.com/wvlet/airframe/commit/d5f928d)]
-  Add note for workarounds for using Future with Airframe ([#918](https://github.com/wvlet/airframe/issues/918)) [[a51b9ad](https://github.com/wvlet/airframe/commit/a51b9ad)]
-  Update sbt-sonatype to 3.9.2 ([#1016](https://github.com/wvlet/airframe/issues/1016)) [[8228ce1](https://github.com/wvlet/airframe/commit/8228ce1)]

## 20.3.3

-  airframe-http: [#989](https://github.com/wvlet/airframe/issues/989) Add @RPC Annotation ([#1012](https://github.com/wvlet/airframe/issues/1012)) [[cdd626d](https://github.com/wvlet/airframe/commit/cdd626d)]
-  airframe-surface: Resolve Surfaces properly within sbt classloader [#1014](https://github.com/wvlet/airframe/issues/1014)) [[f6b6cbf](https://github.com/wvlet/airframe/commit/f6b6cbf)]
-  airframe-surface: Fix a rare case when $ symbol is found in Surface.name ([#1013](https://github.com/wvlet/airframe/issues/1013)) [[67d0dfd](https://github.com/wvlet/airframe/commit/67d0dfd)]

## 20.3.2

This release is a hot fix to properly deploy airframe-http-okhttp library to Maven central.

-  airframe-http-okhttp: Fix for Scala 2.11 and release process in build.sbt ([#1008](https://github.com/wvlet/airframe/issues/1008)) [[b63bbe5](https://github.com/wvlet/airframe/commit/b63bbe5)]
-  airframe-http-okhttp: Update okhttp to 3.12.10 ([#1009](https://github.com/wvlet/airframe/issues/1009)) [[ab367a8](https://github.com/wvlet/airframe/commit/ab367a8)]
-  sbt-airframe: Wrap primitive args into Map[String, Any] ([#1010](https://github.com/wvlet/airframe/issues/1010)) [[8b8582a](https://github.com/wvlet/airframe/commit/8b8582a)]

## 20.3.1

Airframe 20.3.1 added several modules for developing Scala.js applictions using airframe-http framework. 

New Modules:
-  airframe-http-rx: A module for building reactive DOM components. Renamed from airframe-rx.
-  airframe-http-widget: An extension of airframe-http-rx for using [Twitter Bootstrap](https://getbootstrap.com/) CSS collections. Renamed from airframe-rx-widget
-  airframe-http-okhttp: A sync HTTP client implementation backed by okhttp3 ([#946](https://github.com/wvlet/airframe/issues/946)) [[038e960](https://github.com/wvlet/airframe/commit/038e960)]
-  sbt-airframe: An sbt-plugin for generating HTTP client code for accesing REST APIs defined in airframe-http ([#943](https://github.com/wvlet/airframe/issues/943)) [[d78781e](https://github.com/wvlet/airframe/commit/d78781e)]

New Features:
-  airframe-control: Support Scala.js [[0a2c90e](https://github.com/wvlet/airframe/commit/0a2c90e)]
-  airframe-http: Support Scala.js for defining Web interfaces as well as Scala JVM [[0e017db](https://github.com/wvlet/airframe/commit/0e017db)]
-  airframe-http: Support adding a custom codec to clients ([#1004](https://github.com/wvlet/airframe/issues/1004)) [[9675669](https://github.com/wvlet/airframe/commit/9675669)]
-  airframe-http: Add cross-platform HttpMessage.Request and Response classes ([#982](https://github.com/wvlet/airframe/issues/982)) [[f55d438](https://github.com/wvlet/airframe/commit/f55d438)]
-  airframe-http-finagle: Update finagle-core, finagle-http, ... to 20.4.0 ([#965](https://github.com/wvlet/airframe/issues/965)) [[111e5ad](https://github.com/wvlet/airframe/commit/111e5ad)]
-  airframe-http-rx: [#993](https://github.com/wvlet/airframe/issues/993) Support Future[X] to Rx[X] conversion ([#996](https://github.com/wvlet/airframe/issues/996)) [[b54bbe7](https://github.com/wvlet/airframe/commit/b54bbe7)]
-  airframe-http-rx: [#961](https://github.com/wvlet/airframe/issues/961) Support add/overwrite of DOM attributes ([#976](https://github.com/wvlet/airframe/issues/976)) [[02b54b2](https://github.com/wvlet/airframe/commit/02b54b2)]
-  airframe-codec: Add Unit codec ([#980](https://github.com/wvlet/airframe/issues/980)) [[58ef3ff](https://github.com/wvlet/airframe/commit/58ef3ff)]
-  airframe-codec: Support UUID and MessageCodec.fromMap(Map[String, Any]) ([#972](https://github.com/wvlet/airframe/issues/972)) [[31eea9a](https://github.com/wvlet/airframe/commit/31eea9a)]

Major changes:
-  airframe-http: Deprecated SimpleHttpRequest/Response in favor of HttpMessage.Request/Response
-  airframe-surface: Moved `@secret` annotation to wvlet.airframe.surface package. ([#962](https://github.com/wvlet/airframe/issues/962)) [[b79783e](https://github.com/wvlet/airframe/commit/b79783e)]

Minor Fixes:
-  airframe-http: Find Endpoint annotations in parent interfaces ([#997](https://github.com/wvlet/airframe/issues/997)) [[8f54793](https://github.com/wvlet/airframe/commit/8f54793)]
-  airframe-http: Support JSHttpClient config filter airframe-http: Fix protocol check [[c7f34d5](https://github.com/wvlet/airframe/commit/c7f34d5)]
-  airframe-http: Support Future[_], F[_] return types in IDL ([#1001](https://github.com/wvlet/airframe/issues/1001)) [[abf0595](https://github.com/wvlet/airframe/commit/abf0595)]
-  airframe-http: [#956](https://github.com/wvlet/airframe/issues/956) Do not generate response body for 204 responses ([#975](https://github.com/wvlet/airframe/issues/975)) [[bb979dd](https://github.com/wvlet/airframe/commit/bb979dd)]
-  ariframe-http-finagle: Use HttpMessage.Response for static content server ([#984](https://github.com/wvlet/airframe/issues/984)) [[2c91a73](https://github.com/wvlet/airframe/commit/2c91a73)]
-  airframe-http-rx: Migrate airframe-rx as a companion module of airframe-http ([#986](https://github.com/wvlet/airframe/issues/986)) [[39e9be3](https://github.com/wvlet/airframe/commit/39e9be3)]
-  airframe-http-rx: Fix flatMap evaluation and improve the test coverage ([#979](https://github.com/wvlet/airframe/issues/979)) [[8ad4e59](https://github.com/wvlet/airframe/commit/8ad4e59)]
-  airframe-http-rx: [#958](https://github.com/wvlet/airframe/issues/958) Support embedding Iterable[X <: RxElement] inside DOM ([#974](https://github.com/wvlet/airframe/issues/974)) [[f224234](https://github.com/wvlet/airframe/commit/f224234)]
-  airframe-di: Improve the error message for abstract types [[de50bbb](https://github.com/wvlet/airframe/commit/de50bbb)]
-  sbt-airframe: Fix import cleanup [[41523f2](https://github.com/wvlet/airframe/commit/41523f2)]
-  sbt-airframe: Cache the previous client generation results ([#1002](https://github.com/wvlet/airframe/issues/1002)) [[a51a9e2](https://github.com/wvlet/airframe/commit/a51a9e2)]
-  sbt-airframe: Use pre-built airframe-http package for generating client code ([#998](https://github.com/wvlet/airframe/issues/998)) [[a893a26](https://github.com/wvlet/airframe/commit/a893a26)]
-  sbt-airframe: Update client code only when necessary ([#985](https://github.com/wvlet/airframe/issues/985)) [[122c49c](https://github.com/wvlet/airframe/commit/122c49c)]
-  sbt-airframe: Use airframeHttpClients := Seq("(api pkg):(type):(target pkg)") format ([#973](https://github.com/wvlet/airframe/issues/973)) [[346b24f](https://github.com/wvlet/airframe/commit/346b24f)]
-  sbt-airframe: Fix a class loader issue  ([#971](https://github.com/wvlet/airframe/issues/971)) [[8917e42](https://github.com/wvlet/airframe/commit/8917e42)]
-  sbt-airframe: Support generating Scala.js client [[16a2551](https://github.com/wvlet/airframe/commit/16a2551)]
-  airframe-control: Remove dependency to airframe-jmx ([#977](https://github.com/wvlet/airframe/issues/977)) [[154f283](https://github.com/wvlet/airframe/commit/154f283)]
-  [#995](https://github.com/wvlet/airframe/issues/995): Embed release notes into git annotated tags [[45b3e53](https://github.com/wvlet/airframe/commit/45b3e53)]

Internal changes:
-  Improve test coverage ([#1006](https://github.com/wvlet/airframe/issues/1006)) [[5920a1b](https://github.com/wvlet/airframe/commit/5920a1b)]
-  Upgrade to Scala.js 1.0.1 ([#994](https://github.com/wvlet/airframe/issues/994)) [[08c675d](https://github.com/wvlet/airframe/commit/08c675d)]
-  Upgrade to Scala 2.12.11 https://github.com/scala/scala/releases/tag/v2.12.11 ([#991](https://github.com/wvlet/airframe/issues/991)) [[39c0eca](https://github.com/wvlet/airframe/commit/39c0eca)]
-  Update presto-main to 331 ([#990](https://github.com/wvlet/airframe/issues/990)) [[d313263](https://github.com/wvlet/airframe/commit/d313263)]
-  Update sbt-mdoc to 2.1.4 ([#988](https://github.com/wvlet/airframe/issues/988)) [[4d28b21](https://github.com/wvlet/airframe/commit/4d28b21)]
-  Update postgresql to 42.2.11 ([#968](https://github.com/wvlet/airframe/issues/968)) [[52430fd](https://github.com/wvlet/airframe/commit/52430fd)]
-  Add settings for publishing sbt-airframe ([#969](https://github.com/wvlet/airframe/issues/969)) [[c4bdb09](https://github.com/wvlet/airframe/commit/c4bdb09)]
-  Upgrade scalafmt binary ([#966](https://github.com/wvlet/airframe/issues/966)) [[6bad48a](https://github.com/wvlet/airframe/commit/6bad48a)]
-  Update fluency-core, fluency-fluentd, ... to 2.4.1 ([#964](https://github.com/wvlet/airframe/issues/964)) [[938e702](https://github.com/wvlet/airframe/commit/938e702)]

## 20.3.0
-  airframe-codec: Support reading Array[AnyRef] in JDBCCodec ([#960](https://github.com/wvlet/airframe/issues/960)) [[d1c1342](https://github.com/wvlet/airframe/commit/d1c1342)]
-  airframe-codec: Support more variations of time strings for InstantCodec ([#955](https://github.com/wvlet/airframe/issues/955)) [[e36c328](https://github.com/wvlet/airframe/commit/e36c328)]
-  airframe-rx: Support embedding raw HTMLElements ([#952](https://github.com/wvlet/airframe/issues/952)) [[70a393e](https://github.com/wvlet/airframe/commit/70a393e)]
-  airframe-metrics: Fix TimeVector tests across leap years [#953](https://github.com/wvlet/airframe/issues/953) ([#954](https://github.com/wvlet/airframe/issues/954)) [[f536f82](https://github.com/wvlet/airframe/commit/f536f82)]

## 20.2.1
-  airframe-di: Fix a compilation error while binding sealed traits [#939](https://github.com/wvlet/airframe/issues/939) ([#940](https://github.com/wvlet/airframe/issues/940)) [[e381bfa](https://github.com/wvlet/airframe/commit/e381bfa)]
-  airframe-http-finagle: [#930](https://github.com/wvlet/airframe/issues/930) Use bridged schduler for sync client ([#937](https://github.com/wvlet/airframe/issues/937)) [[9ffd0d5](https://github.com/wvlet/airframe/commit/9ffd0d5)]
-  Update scalajs-dom to 1.0.0 ([#936](https://github.com/wvlet/airframe/issues/936)) [[89e5bb7](https://github.com/wvlet/airframe/commit/89e5bb7)]
-  Update scalajs-java-logging to 1.0.0 ([#934](https://github.com/wvlet/airframe/issues/934)) [[efcd37a](https://github.com/wvlet/airframe/commit/efcd37a)]
-  Update portable-scala-reflect to 1.0.0 ([#933](https://github.com/wvlet/airframe/issues/933)) [[fb97343](https://github.com/wvlet/airframe/commit/fb97343)]
-  Update scalajs-java-time to 1.0.0 ([#935](https://github.com/wvlet/airframe/issues/935)) [[4ebf554](https://github.com/wvlet/airframe/commit/4ebf554)]

## 20.2.0

Airframe and Airspec now support the first major release of Scala.js 1.0.0, and airframe-http-finagle becomes available for Scala 2.13.

-  Upgrade to Scala.js 1.0.0 ([#926](https://github.com/wvlet/airframe/issues/926)) [[c3bd7c1](https://github.com/wvlet/airframe/commit/c3bd7c1)]
-  airspec: Support js.Objects in shouldBe comparison ([#922](https://github.com/wvlet/airframe/issues/922)) [[fa2d66c](https://github.com/wvlet/airframe/commit/fa2d66c)]
-  airspec: Add AirSpecLauncher for standalone testing ([#921](https://github.com/wvlet/airframe/issues/921)) [[366bca4](https://github.com/wvlet/airframe/commit/366bca4)]
-  airframe-http: Fiangle Scala 2.13 support ([#920](https://github.com/wvlet/airframe/issues/920)) [[03c65af](https://github.com/wvlet/airframe/commit/03c65af)]
-  airframe-http: Update finagle-core, finagle-http, ... to 20.1.0 ([#916](https://github.com/wvlet/airframe/issues/916)) [[755ea01](https://github.com/wvlet/airframe/commit/755ea01)]
-  airframe-di: [#887](https://github.com/wvlet/airframe/issues/887) Support bind only lifecycle hooks ([#919](https://github.com/wvlet/airframe/issues/919)) [[db3648d](https://github.com/wvlet/airframe/commit/db3648d)]
-  airframe-codec: Support Instant codec in Scala.js ([#914](https://github.com/wvlet/airframe/issues/914)) [[9974409](https://github.com/wvlet/airframe/commit/9974409)]
-  airframe-surface: [#913](https://github.com/wvlet/airframe/issues/913): Fixed Surface.of[X] with alias parameters ([#924](https://github.com/wvlet/airframe/issues/924)) [[f30277b](https://github.com/wvlet/airframe/commit/f30277b)]

Internal changes:
-  Update sbt-scalajs-crossproject to 1.0.0 ([#931](https://github.com/wvlet/airframe/issues/931)) [[100fc60](https://github.com/wvlet/airframe/commit/100fc60)]
-  Update postgresql to 42.2.10 ([#929](https://github.com/wvlet/airframe/issues/929)) [[d151605](https://github.com/wvlet/airframe/commit/d151605)]
-  Update sbt to 1.3.8 ([#925](https://github.com/wvlet/airframe/issues/925)) [[547a331](https://github.com/wvlet/airframe/commit/547a331)]
-  Update sbt-scalafmt to 2.3.1 ([#917](https://github.com/wvlet/airframe/issues/917)) [[e7321c1](https://github.com/wvlet/airframe/commit/e7321c1)]

## 20.1.3
Minor bug fix release:
-  airframe-rx-html: Fix Rx node rendering ([#912](https://github.com/wvlet/airframe/issues/912)) [[ec91f15](https://github.com/wvlet/airframe/commit/ec91f15)]
-  airframe-config: various config mapping improvements ([#911](https://github.com/wvlet/airframe/issues/911)) [[a9b9be7](https://github.com/wvlet/airframe/commit/a9b9be7)]

Internal changes:
-  Update jmh-core, jmh-generator-bytecode, ... to 1.23 ([#908](https://github.com/wvlet/airframe/issues/908)) [[64fd0c9](https://github.com/wvlet/airframe/commit/64fd0c9)]
-  Update presto-main to 329 ([#910](https://github.com/wvlet/airframe/issues/910)) [[0711050](https://github.com/wvlet/airframe/commit/0711050)]

## 20.1.2
This release introduces airframe-rx-html for rendering complex DOM elements (html tags, canvas, SVG, etc.) at ease with Scala.js. You can compose your own DOM elements by defining RxElement and RxComponent classes. 

-  airframe-surface: Fix private field access ([#907](https://github.com/wvlet/airframe/issues/907)) [[16866f4](https://github.com/wvlet/airframe/commit/16866f4)]
-  airspec: [#906](https://github.com/wvlet/airframe/issues/906) Show warning if scalaJsSupport is missing ([#909](https://github.com/wvlet/airframe/issues/909)) [[f272e5d](https://github.com/wvlet/airframe/commit/f272e5d)]
-  airframe-rx-html: Rewrite Gallery with airframe-rx-html ([#905](https://github.com/wvlet/airframe/issues/905)) [[657db9e](https://github.com/wvlet/airframe/commit/657db9e)]
-  airframe-rx-html: Add more tags, attrs, and SVG elements ([#904](https://github.com/wvlet/airframe/issues/904)) [[669e71a](https://github.com/wvlet/airframe/commit/669e71a)]
-  airframe-rx: Add wvlet.airframe.rx.html  ([#903](https://github.com/wvlet/airframe/issues/903)) [[2b7e7d6](https://github.com/wvlet/airframe/commit/2b7e7d6)]
-  airframe-control: Add CircuitBreaker.withRecoveryPolicy() ([#900](https://github.com/wvlet/airframe/issues/900)) [[133f8c9](https://github.com/wvlet/airframe/commit/133f8c9)]
-  airframe-di: Add an option to disable implicit instance creation: `Design.noDefaultInstanceInjection` ([#886](https://github.com/wvlet/airframe/issues/886)) [[15b2558](https://github.com/wvlet/airframe/commit/15b2558)]

Internal changes:
- Rename SCALA_JS_VERSION -> SCALAJS_VESRION to be consistent with other Scala.js projects ([#898](https://github.com/wvlet/airframe/issues/898)) [[029f000](https://github.com/wvlet/airframe/commit/029f000)]
-  Update sbt to 1.3.7 ([#897](https://github.com/wvlet/airframe/issues/897)) [[f63b9d7](https://github.com/wvlet/airframe/commit/f63b9d7)]

## 20.1.1
-  airframe-log: Fix warning log message color ([#896](https://github.com/wvlet/airframe/issues/896)) [[a1f6e1d](https://github.com/wvlet/airframe/commit/a1f6e1d)]
-  airframe-rx: Support generic event handlers ([#893](https://github.com/wvlet/airframe/issues/893)) [[774687c](https://github.com/wvlet/airframe/commit/774687c)]
-  airframe-rx-widget: Support embedding Seq of Rx[A], RxElement inside XML literal ([#892](https://github.com/wvlet/airframe/issues/892)) [[7a8fd0c](https://github.com/wvlet/airframe/commit/7a8fd0c)]
-  airframe-rx-widget: Fix mounting point of Rx element ([#891](https://github.com/wvlet/airframe/issues/891)) [[09170d1](https://github.com/wvlet/airframe/commit/09170d1)]

## 20.1.0
Happy new year! In this release, we added new components: airframe-rx and airframe-rx-widget for building reactive web user-interface with Scala.js.
In addition, AirSpec testing framework supports `test("...", design="...")` syntax for easily nesting test cases and modifying designs for each test case. 
airframe-control now has CircuitBreaker to control connections to remote services with fail-fast behavior.   

Changes:
-  airframe-rx-widget: Support RxVar subscription ([#890](https://github.com/wvlet/airframe/issues/890)) [[19b00f4](https://github.com/wvlet/airframe/commit/19b00f4)]
-  airframe-rx-widget: Use the latest nodejs and jsdom ([#889](https://github.com/wvlet/airframe/issues/889)) [[baf4729](https://github.com/wvlet/airframe/commit/baf4729)]
-  airframe-rx-widget: Update scalajs-env-jsdom-nodejs to 1.0.0-RC3 ([#888](https://github.com/wvlet/airframe/issues/888)) [[3345235](https://github.com/wvlet/airframe/commit/3345235)]
-  airframe-rx-widget: Scala.js UI widget collection ([#883](https://github.com/wvlet/airframe/issues/883)) [[00496b0](https://github.com/wvlet/airframe/commit/00496b0)]
-  airspec: Add test(...) syntax ([#876](https://github.com/wvlet/airframe/issues/876)) [[00d8ba9](https://github.com/wvlet/airframe/commit/00d8ba9)]
-  airframe-control: Add CircuitBreaker ([#763](https://github.com/wvlet/airframe/issues/763)) [[e55521b](https://github.com/wvlet/airframe/commit/e55521b)]
-  airframe-http: [#864](https://github.com/wvlet/airframe/issues/864) Support empty strings for /*path endpoint ([#879](https://github.com/wvlet/airframe/issues/879)) [[552e30f](https://github.com/wvlet/airframe/commit/552e30f)]
-  airframe-http: [#865](https://github.com/wvlet/airframe/issues/865) Support multiple static content paths ([#877](https://github.com/wvlet/airframe/issues/877)) [[9a1f50f](https://github.com/wvlet/airframe/commit/9a1f50f)]
-  airframe-http: Fix static content directory path ([#882](https://github.com/wvlet/airframe/issues/882)) [[5412a31](https://github.com/wvlet/airframe/commit/5412a31)]
-  airframe-http: [#863](https://github.com/wvlet/airframe/issues/863) Find inherited endpoints ([#878](https://github.com/wvlet/airframe/issues/878)) [[db7f792](https://github.com/wvlet/airframe/commit/db7f792)]

Intennal changes:
-  Update presto-main to 328 ([#885](https://github.com/wvlet/airframe/issues/885)) [[9add1aa](https://github.com/wvlet/airframe/commit/9add1aa)]
-  Update HikariCP to 3.4.2 ([#884](https://github.com/wvlet/airframe/issues/884)) [[9f3fc0c](https://github.com/wvlet/airframe/commit/9f3fc0c)]
-  Update sbt-mdoc to 2.1.1 ([#881](https://github.com/wvlet/airframe/issues/881)) [[bcb26ea](https://github.com/wvlet/airframe/commit/bcb26ea)]
-  Update sbt to 1.3.6 ([#873](https://github.com/wvlet/airframe/issues/873)) [[20e64f3](https://github.com/wvlet/airframe/commit/20e64f3)]
-  Update sqlite-jdbc to 3.30.1 ([#871](https://github.com/wvlet/airframe/issues/871)) [[0007f44](https://github.com/wvlet/airframe/commit/0007f44)]

## 19.12.4
-  Publish Scala.js binaries for Scala 2.13 ([#870](https://github.com/wvlet/airframe/issues/870)) [[23b94cd](https://github.com/wvlet/airframe/commit/23b94cd)]
-  airframe-jdbc: Add transaction call support ([#861](https://github.com/wvlet/airframe/issues/861)) [[c48154f](https://github.com/wvlet/airframe/commit/c48154f)]
-  airframe-jdbc: Support standalone usage of JDBC connection pools without DI ([#858](https://github.com/wvlet/airframe/issues/858)) [[405e3e0](https://github.com/wvlet/airframe/commit/405e3e0)]
-  airframe-sql: SQL typer ([#494](https://github.com/wvlet/airframe/issues/494)) [[ede9f23](https://github.com/wvlet/airframe/commit/ede9f23)]
-  internal: Update slf4j-jdk14 to 1.7.30 ([#859](https://github.com/wvlet/airframe/issues/859)) [[5af0887](https://github.com/wvlet/airframe/commit/5af0887)]

## 19.12.3
-  Upgrade Scala.js to 1.0.0-RC2 ([#821](https://github.com/wvlet/airframe/issues/821)) [[6b458dc](https://github.com/wvlet/airframe/commit/6b458dc)]
-  Updated the documentation site using Docusaurus ([#851](https://github.com/wvlet/airframe/issues/851)) [[59d090f](https://github.com/wvlet/airframe/commit/59d090f)]
-  airframe-http: Update finagle-core, finagle-http, ... to 19.12.0 ([#855](https://github.com/wvlet/airframe/issues/855)) [[3bcff69](https://github.com/wvlet/airframe/commit/3bcff69)]
-  airframe-jdbc: Update postgresql to 42.2.9 ([#848](https://github.com/wvlet/airframe/issues/848)) [[d3d79fe](https://github.com/wvlet/airframe/commit/d3d79fe)]
-  Note: version 19.12.1, 19.12.2 was discarded due to release failure.  

## 19.12.0
-  airframe-di: Support in-class bind[X] for classes with DISupport trait ([#816](https://github.com/wvlet/airframe/issues/816)) [[c3ce56b](https://github.com/wvlet/airframe/commit/c3ce56b)]
-  airframe-http: Add CORS filter and filter-instance support ([#846](https://github.com/wvlet/airframe/issues/846)) [[58245b2](https://github.com/wvlet/airframe/commit/58245b2)]
-  airframe-http: Support leaf filters without `@Endpoint` mapping ([#844](https://github.com/wvlet/airframe/issues/844)) [[d342530](https://github.com/wvlet/airframe/commit/d342530)]
-  airframe-http: Support StaticContent ([#843](https://github.com/wvlet/airframe/issues/843)) [[d415025](https://github.com/wvlet/airframe/commit/d415025)]
-  airframe-http: Add thread-local context parameter support ([#829](https://github.com/wvlet/airframe/issues/829)) [[25e312e](https://github.com/wvlet/airframe/commit/25e312e)]
-  airframe-http-client: Add raw response methods to HttpClient ([#831](https://github.com/wvlet/airframe/issues/831)) [[6390bb3](https://github.com/wvlet/airframe/commit/6390bb3)]
-  airframe-fluentd: Fix warnings in airframe-fluentd ([#833](https://github.com/wvlet/airframe/issues/833)) [[856a644](https://github.com/wvlet/airframe/commit/856a644)]
-  airframe-codec: Support Base64 encoded binary ([#819](https://github.com/wvlet/airframe/issues/819)) [[f60af4e](https://github.com/wvlet/airframe/commit/f60af4e)]
-  airframe-codec: Rename MessageHolder -> MessageContext ([#817](https://github.com/wvlet/airframe/issues/817)) [[33c10c6](https://github.com/wvlet/airframe/commit/33c10c6)]
-  airframe-launcher: [#523](https://github.com/wvlet/airframe/issues/523) Add coloring to the help message ([#815](https://github.com/wvlet/airframe/issues/815)) [[74eb427](https://github.com/wvlet/airframe/commit/74eb427)]

internal changes:

-  airframe-msgpack: Update msgpack-core to 0.8.20 ([#830](https://github.com/wvlet/airframe/issues/830)) [[2db9289](https://github.com/wvlet/airframe/commit/2db9289)]
-  airframe-http: Move the internal routing code to wvlet.airframe.http.router package ([#824](https://github.com/wvlet/airframe/issues/824)) [[58cbb90](https://github.com/wvlet/airframe/commit/58cbb90)]
-  airframe-http: Simplify HttpFilter hierarchy ([#842](https://github.com/wvlet/airframe/issues/842)) [[cb4b595](https://github.com/wvlet/airframe/commit/cb4b595)]
-  Update sbt-scoverage to 1.6.1 ([#814](https://github.com/wvlet/airframe/issues/814)) [[39ad0e1](https://github.com/wvlet/airframe/commit/39ad0e1)]
-  Update scalajs-java-time to 0.2.6 ([#825](https://github.com/wvlet/airframe/issues/825)) [[6cf66a0](https://github.com/wvlet/airframe/commit/6cf66a0)]
-  Update scalajs-java-logging to 0.1.6 ([#823](https://github.com/wvlet/airframe/issues/823)) [[e8eb636](https://github.com/wvlet/airframe/commit/e8eb636)]
-  Upgrade Scala.js to 0.6.31 ([#820](https://github.com/wvlet/airframe/issues/820)) [[4d259c2](https://github.com/wvlet/airframe/commit/4d259c2)]
-  Update sbt-sonatype to 3.8.1 ([#818](https://github.com/wvlet/airframe/issues/818)) [[0c73d84](https://github.com/wvlet/airframe/commit/0c73d84)]
-  Update sbt to 1.3.4 ([#826](https://github.com/wvlet/airframe/issues/826)) [[7610c99](https://github.com/wvlet/airframe/commit/7610c99)]

## 19.11.2
-  airframe-codec: [#156](https://github.com/wvlet/airframe/issues/156) Suppress null value output for None:Option[X] ([#811](https://github.com/wvlet/airframe/issues/811)) [[6e8f70e](https://github.com/wvlet/airframe/commit/6e8f70e)]
-  internal: Use GitHub Action cache ([#808](https://github.com/wvlet/airframe/issues/808)) [[574b5a9](https://github.com/wvlet/airframe/commit/574b5a9)]

## 19.11.1
-  airframe-di: Add in-trait bindLocal ([#805](https://github.com/wvlet/airframe/issues/805)) [[22ce396](https://github.com/wvlet/airframe/commit/22ce396)]
-  airframe-control: [#767](https://github.com/wvlet/airframe/issues/767) Add bounded backoff retry ([#803](https://github.com/wvlet/airframe/issues/803)) [[730617a](https://github.com/wvlet/airframe/commit/730617a)]
-  airframe-http: [#796](https://github.com/wvlet/airframe/issues/796) Support returning Scala Future ([#802](https://github.com/wvlet/airframe/issues/802)) [[4135a57](https://github.com/wvlet/airframe/commit/4135a57)]
-  airframe-http-finagle: Update finagle-core, finagle-http, ... to 19.11.0 ([#800](https://github.com/wvlet/airframe/issues/800)) [[681dbcd](https://github.com/wvlet/airframe/commit/681dbcd)]

## 19.11.0

**Major changes**:
- airframe-http-finagle now supports builder patterns Finagle.server.withXXX or Finagle.client.withXXX to fully customize HTTP servers and clients. These methods are preferred over using FinagleServerConfig or FinagleClientConfig directly.
- Added Scala.js support for airframe-codec. Now you can build http servers (Scala JVM) and client pages (Scala.js) that interact together through airframe-codec.

Change log:  
-  airframe-http: Support server customization with FinagleServerConfig ([#797](https://github.com/wvlet/airframe/issues/797)) [[7f525d0](https://github.com/wvlet/airframe/commit/7f525d0)]
-  airframe-http: Use 201 (POST/PUT) and 204 (DELETE) status code for empty responses [[b63d4a9](https://github.com/wvlet/airframe/commit/b63d4a9)]
-  airframe-http: Fix the client builder pattern and use no record expiration time by default ([#777](https://github.com/wvlet/airframe/issues/777)) [[8e96d95](https://github.com/wvlet/airframe/commit/8e96d95)]
-  airframe-http-finagle: Update Finagle to 19.10.0 ([#765](https://github.com/wvlet/airframe/issues/765)) [[44d5e9d](https://github.com/wvlet/airframe/commit/44d5e9d)]
-  airframe-config: Airframe DI integration for Config.overrideWithPropertiesFile() ([#798](https://github.com/wvlet/airframe/issues/798)) [[5d3d7ad](https://github.com/wvlet/airframe/commit/5d3d7ad)]
-  airframe-codec: Scala.js support with Pure Scala packer/unpacker ([#789](https://github.com/wvlet/airframe/issues/789)) [[e565fea](https://github.com/wvlet/airframe/commit/e565fea)]
-  airframe-codec: Add @required to mark must-have parameters ([#779](https://github.com/wvlet/airframe/issues/779)) [[b33ce25](https://github.com/wvlet/airframe/commit/b33ce25)]
-  airframe-di: Deprecate runtime provider binding definitions ([#786](https://github.com/wvlet/airframe/issues/786)) [[45aab9b](https://github.com/wvlet/airframe/commit/45aab9b)]
-  airframe-di: Support design.run[A,B](a:A=>B):B ([#785](https://github.com/wvlet/airframe/issues/785)) [[661cd6b](https://github.com/wvlet/airframe/commit/661cd6b)]
-  airframe-jdbc: Do not shutdown ConnectionPoolFactory explicitly ([#773](https://github.com/wvlet/airframe/issues/773)) [[b87b18d](https://github.com/wvlet/airframe/commit/b87b18d)]

internal changes:
-  Update slf4j-jdk14 to 1.7.29 ([#792](https://github.com/wvlet/airframe/issues/792)) [[c34cef4](https://github.com/wvlet/airframe/commit/c34cef4)]
-  Update sbt-scalafmt to 2.2.1 ([#783](https://github.com/wvlet/airframe/issues/783)) [[613daf2](https://github.com/wvlet/airframe/commit/613daf2)]
-  Update scalafmt-core to 2.2.1 ([#780](https://github.com/wvlet/airframe/issues/780)) [[b090e83](https://github.com/wvlet/airframe/commit/b090e83)]
-  Update scalafmt-core to 2.2.0 ([#776](https://github.com/wvlet/airframe/issues/776)) [[627b857](https://github.com/wvlet/airframe/commit/627b857)]
-  Update sbt to 1.3.3 ([#774](https://github.com/wvlet/airframe/issues/774)) [[88890a8](https://github.com/wvlet/airframe/commit/88890a8)]

## 19.10.1
-  airspec: Add inGitHubAction and inCI ([#762](https://github.com/wvlet/airframe/issues/762)) [[0765f11](https://github.com/wvlet/airframe/commit/0765f11)]
-  airframe-control: Add retryableFailure(e).withExtraWaitMillis/Factor ([#761](https://github.com/wvlet/airframe/issues/761)) [[40b7a58](https://github.com/wvlet/airframe/commit/40b7a58)]
-  airframe-http-finagle: Add Finagle.client builder ([#760](https://github.com/wvlet/airframe/issues/760)) [[48f44cb](https://github.com/wvlet/airframe/commit/48f44cb)]
-  airframe-control: Use this.copy for RetryContext ([#759](https://github.com/wvlet/airframe/issues/759)) [[4251c84](https://github.com/wvlet/airframe/commit/4251c84)]
-  airframe-http: Support patch, patchOps, deleteOps methods ([#755](https://github.com/wvlet/airframe/issues/755)) [[7603f16](https://github.com/wvlet/airframe/commit/7603f16)]
-  Internal changes
   -  Add javacOptions to generate Java8 compatible binary ([#764](https://github.com/wvlet/airframe/issues/764)) [[bc399d1](https://github.com/wvlet/airframe/commit/bc399d1)]
   -  Update sbt-microsites to 0.9.7 ([#752](https://github.com/wvlet/airframe/issues/752)) [[a2e2ce5](https://github.com/wvlet/airframe/commit/a2e2ce5)]
   -  Use jdk11 for all builds ([#751](https://github.com/wvlet/airframe/issues/751)) [[2f69218](https://github.com/wvlet/airframe/commit/2f69218)]
-  Enhancement of GitHub Actions configuration
   -  Do not run CI tests if no source code has changed ([#756](https://github.com/wvlet/airframe/issues/756)) [[7d367b9](https://github.com/wvlet/airframe/commit/7d367b9)]
   -  Update test.yml [[bbffdae](https://github.com/wvlet/airframe/commit/bbffdae)]
   -  Enable codecov for PRs ([#753](https://github.com/wvlet/airframe/issues/753)) [[4eee198](https://github.com/wvlet/airframe/commit/4eee198)]
   -  Upload code coverage report [[cedce6b](https://github.com/wvlet/airframe/commit/cedce6b)]
   -  Fix doc update on GitHub Actions ([#750](https://github.com/wvlet/airframe/issues/750)) [[0f75cbc](https://github.com/wvlet/airframe/commit/0f75cbc)]

## 19.10.0
-  This is a maintenance release for migrating to GitHub Atcions. There is no feature change. ([#749](https://github.com/wvlet/airframe/issues/749)) [[74f84b5](https://github.com/wvlet/airframe/commit/74f84b5)]

## 19.9.9
-  This version is only for internal changes. 
-  From this release, airfarme will use GitHub actions. 
-  Update scalafmt-core to 2.1.0 ([#747](https://github.com/wvlet/airframe/issues/747)) [[c851834](https://github.com/wvlet/airframe/commit/c851834)]
-  Migrate to GitHub Actions ([#744](https://github.com/wvlet/airframe/issues/744)) [[f543442](https://github.com/wvlet/airframe/commit/f543442)]
-  Update sbt-scalafmt to 2.0.6 ([#745](https://github.com/wvlet/airframe/issues/745)) [[daa7e8f](https://github.com/wvlet/airframe/commit/daa7e8f)]
-  Update sbt-sonatype to 3.8 ([#746](https://github.com/wvlet/airframe/issues/746)) [[a6e8db0](https://github.com/wvlet/airframe/commit/a6e8db0)]

## 19.9.8
-  airframe-metrics: Add TimeVector.succinctTimeVector ([#736](https://github.com/wvlet/airframe/issues/736)) [[9598d81](https://github.com/wvlet/airframe/commit/9598d81)]
-  airspec: Upgrade to scalacheck 1.14.2 ([#743](https://github.com/wvlet/airframe/issues/743)) [[a14a333](https://github.com/wvlet/airframe/commit/a14a333)]
-  airframe-jdbc: Update HikariCP to 3.4.1 ([#737](https://github.com/wvlet/airframe/issues/737)) [[081bb14](https://github.com/wvlet/airframe/commit/081bb14)]
-  internal changes
   -  Update sbt-microsites to 0.9.6 ([#742](https://github.com/wvlet/airframe/issues/742)) [[587ef94](https://github.com/wvlet/airframe/commit/587ef94)]
   -  Upgrade to sbt 1.3.2 ([#740](https://github.com/wvlet/airframe/issues/740)) [[f73a11d](https://github.com/wvlet/airframe/commit/f73a11d)]
   -  Use jdk11 in Travis by default ([#735](https://github.com/wvlet/airframe/issues/735)) [[00e1c0c](https://github.com/wvlet/airframe/commit/00e1c0c)]
   -  Upgrade to Scala 2.13.1 ([#733](https://github.com/wvlet/airframe/issues/733)) [[220ef74](https://github.com/wvlet/airframe/commit/220ef74)]

## 19.9.7
-  Add support for Scala.js 1.0.0-M8 ([#732](https://github.com/wvlet/airframe/issues/732)) [[48a58e5](https://github.com/wvlet/airframe/commit/48a58e5)]
-  airspec: Show the deatailed reason of MISSING_DEPENDENCY ([#729](https://github.com/wvlet/airframe/issues/729)) [[1c24971](https://github.com/wvlet/airframe/commit/1c24971)]
-  airframe-msgpack: Optimize JSON->MsgPack stream conversion ([#725](https://github.com/wvlet/airframe/issues/725)) [[6a07fb4](https://github.com/wvlet/airframe/commit/6a07fb4)]
-  internal changes: 
   - Refactor DesignOptions using copy method ([#731](https://github.com/wvlet/airframe/issues/731)) [[1f4fb14](https://github.com/wvlet/airframe/commit/1f4fb14)]
   -  Update sbt-scalajs, scalajs-compiler, ... to 0.6.29 ([#730](https://github.com/wvlet/airframe/issues/730)) [[22b5188](https://github.com/wvlet/airframe/commit/22b5188)]
   -  Update HikariCP to 3.4.0 ([#727](https://github.com/wvlet/airframe/issues/727)) [[2e0f2d1](https://github.com/wvlet/airframe/commit/2e0f2d1)]
   -  Update sbt-scalafmt to 2.0.5 ([#728](https://github.com/wvlet/airframe/issues/728)) [[d577f9f](https://github.com/wvlet/airframe/commit/d577f9f)]
   -  Update sbt-pgp to 2.0.0 ([#726](https://github.com/wvlet/airframe/issues/726)) [[f8a2d2d](https://github.com/wvlet/airframe/commit/f8a2d2d)]
   -  airframe-http: refactoring HTTP request mapper ([#724](https://github.com/wvlet/airframe/issues/724)) [[d992e0f](https://github.com/wvlet/airframe/commit/d992e0f)]

## 19.9.6
-  airframe-codc: Fix AnyCodec to proceed cursor when Nil is found ([#722](https://github.com/wvlet/airframe/issues/722)) [[3d82c26](https://github.com/wvlet/airframe/commit/3d82c26)]
-  airframe-msgpack: Add MessagePack.fromJSON(JSONSource) ([#721](https://github.com/wvlet/airframe/issues/721)) [[584ccbf](https://github.com/wvlet/airframe/commit/584ccbf)]
-  airframe-config: Use left-aligned config display ([#719](https://github.com/wvlet/airframe/issues/719)) [[2a20752](https://github.com/wvlet/airframe/commit/2a20752)]
-  internal: Use .cache folder for coursier ([#720](https://github.com/wvlet/airframe/issues/720)) [[cb4fca7](https://github.com/wvlet/airframe/commit/cb4fca7)]

## 19.9.5
-  airspec: Deprecate configure(design) ([#714](https://github.com/wvlet/airframe/issues/714)) [[9277263](https://github.com/wvlet/airframe/commit/9277263)]
-  airspec: Isolate spec loggers ([#718](https://github.com/wvlet/airframe/issues/718)) [[7ea85f8](https://github.com/wvlet/airframe/commit/7ea85f8)]
-  airframe-di: Fix duplicated Closeable hook registration ([#704](https://github.com/wvlet/airframe/issues/704)) [[0ca5da2](https://github.com/wvlet/airframe/commit/0ca5da2)]
-  airframe-config: Add @secret annotation ([#710](https://github.com/wvlet/airframe/issues/710)) [[c82008f](https://github.com/wvlet/airframe/commit/c82008f)]
-  airframe-http-recorder: Add PathOnlyMatcher ([#711](https://github.com/wvlet/airframe/issues/711)) [[e92536c](https://github.com/wvlet/airframe/commit/e92536c)]
-  airframe-http-recorder: Add name config ([#709](https://github.com/wvlet/airframe/issues/709)) [[26d3cba](https://github.com/wvlet/airframe/commit/26d3cba)]
-  airframe-http: Remove redundant Router nesting ([#708](https://github.com/wvlet/airframe/issues/708)) [[cdc410c](https://github.com/wvlet/airframe/commit/cdc410c)]
-  airframe-http-finagle: Update finagle-core, finagle-http, ... to 19.9.0 ([#694](https://github.com/wvlet/airframe/issues/694)) [[c587c86](https://github.com/wvlet/airframe/commit/c587c86)]-  airframe-http-finagle: Add finagleBaseDesign for using FinagleServerFactory ([#707](https://github.com/wvlet/airframe/issues/707)) [[ea8d56b](https://github.com/wvlet/airframe/commit/ea8d56b)]
-  airframe-http-finagle: Fix FinagleServer lifecycle ([#703](https://github.com/wvlet/airframe/issues/703)) [[ce9ef85](https://github.com/wvlet/airframe/commit/ce9ef85)]
-  airframe-jdbc: Update postgresql to 42.2.8 ([#717](https://github.com/wvlet/airframe/issues/717)) [[43e63f7](https://github.com/wvlet/airframe/commit/43e63f7)]
-  Internal Changes:
   -  Upgrade to Scala 2.12.10 ([#715](https://github.com/wvlet/airframe/issues/715)) [[c23d1ce](https://github.com/wvlet/airframe/commit/c23d1ce)]
   -  Update sbt-sonatype to 3.7 ([#700](https://github.com/wvlet/airframe/issues/700)) [[e6e3e5d](https://github.com/wvlet/airframe/commit/e6e3e5d)]
   -  Use JDK11 for code-format task on CI ([#712](https://github.com/wvlet/airframe/issues/712)) [[a4288eb](https://github.com/wvlet/airframe/commit/a4288eb)]

## 19.9.4
-  airframe-http-finagle: Support streaming JSON/MsgPack responses for Reader[X] ([#691](https://github.com/wvlet/airframe/issues/691)) [[4ebe0ec](https://github.com/wvlet/airframe/commit/4ebe0ec)]
-  airframe-http-finagle: Support Reader[Buf] stream response ([#690](https://github.com/wvlet/airframe/issues/690)) [[fb237f7](https://github.com/wvlet/airframe/commit/fb237f7)]

## 19.9.3
-  Deprecate airframe-tablet ([#684](https://github.com/wvlet/airframe/issues/684)) [[6d2bb77](https://github.com/wvlet/airframe/commit/6d2bb77)]
- airframe-jdbc:
  -  Update HikariCP to 3.3.1 ([#683](https://github.com/wvlet/airframe/issues/683)) [[9434e6b](https://github.com/wvlet/airframe/commit/9434e6b)]
  -  Update sqlite-jdbc to 3.28.0 ([#682](https://github.com/wvlet/airframe/issues/682)) [[8d907d6](https://github.com/wvlet/airframe/commit/8d907d6)]
  -  Update postgresql to 42.2.6 ([#673](https://github.com/wvlet/airframe/issues/673)) [[86234f9](https://github.com/wvlet/airframe/commit/86234f9)]
- airframe-fluentd
  -  Update fluency-core, fluency-fluentd, ... to 2.4.0 ([#681](https://github.com/wvlet/airframe/issues/681)) [[bc1e5bc](https://github.com/wvlet/airframe/commit/bc1e5bc)]
- airframe-codec
  -  Update msgpack-core to 0.8.18 ([#671](https://github.com/wvlet/airframe/issues/671)) [[3fbaecc](https://github.com/wvlet/airframe/commit/3fbaecc)]
- airframe-di
 -  Update javax.annotation-api to 1.3.2 ([#669](https://github.com/wvlet/airframe/issues/669)) [[1ca65c4](https://github.com/wvlet/airframe/commit/1ca65c4)]
- airframe-config
  - Use airframe-codec instead of airframe-tablet for mapping Yaml to Objects
  -  Update snakeyaml to 1.25 ([#679](https://github.com/wvlet/airframe/issues/679)) [[81ae0a9](https://github.com/wvlet/airframe/commit/81ae0a9)]
- internal changes:
  -  Upgrade to sbt-sonatype 3.6 [[5bc29f0](https://github.com/wvlet/airframe/commit/5bc29f0)]
  -  Upgrade to sbt 1.3.0 ([#662](https://github.com/wvlet/airframe/issues/662)) [[f6463e9](https://github.com/wvlet/airframe/commit/f6463e9)]
  -  Update scala-collection-compat to 2.1.2 ([#674](https://github.com/wvlet/airframe/issues/674)) [[2f1286d](https://github.com/wvlet/airframe/commit/2f1286d)]
  -  Update sbt-scalajs-crossproject to 0.6.1 ([#672](https://github.com/wvlet/airframe/issues/672)) [[8ff4256](https://github.com/wvlet/airframe/commit/8ff4256)]
  -  Update slf4j-jdk14 to 1.7.28 ([#677](https://github.com/wvlet/airframe/issues/677)) [[6111025](https://github.com/wvlet/airframe/commit/6111025)]

## 19.9.2
-  airframe-di: Fixes [#658](https://github.com/wvlet/airframe/issues/658). Do not remove shutdown hook thread while it is running ([#661](https://github.com/wvlet/airframe/issues/661)) [[60f35d3](https://github.com/wvlet/airframe/commit/60f35d3)]
-  internal changes for Travis CI builds:
   - Add sonatypeDropAll before the release [[5de0594](https://github.com/wvlet/airframe/commit/5de0594)]
   -  [doc] Support doc only release [[25073d2](https://github.com/wvlet/airframe/commit/25073d2)]
   -  Split publish tasks ([#659](https://github.com/wvlet/airframe/issues/659)) [[1d72f22](https://github.com/wvlet/airframe/commit/1d72f22)]

## 19.9.1
-  airframe-di:
   - Fix an issue in 19.9.0 which closes Sessions unexpectedly by calling Session.close().  
   - Move lifecycle related code to wvlet.airframe.lifecycle ([#657](https://github.com/wvlet/airframe/issues/657)) [[a51927c](https://github.com/wvlet/airframe/commit/a51927c)]
   - Deprecate LifeCycle trait as it confuses the owner of the resource when traits are mixed into other classes
-  airframe-http-recorder: Reduce unecessary warning messages for in-memory recorders.
-  internal: Revert to the previous release script [[3f802b7](https://github.com/wvlet/airframe/commit/3f802b7)]

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
 - airframe-tablet: Add play-json based JSONCodec to enable transformation between JSON <-> MessagePack <-> Object.
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
 - Add [airframe-jdbc](airframe-jdbc.md), a reusable JDBC connection pool implementation.  

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
 - Add [airframe-metrics](airframe-metrics.md)

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
