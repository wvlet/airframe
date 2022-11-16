---
id: release-notes
layout: docs
title: Release Notes
---

Airframe uses YY.MM.patch versioning scheme, so the version numbers match with the release year and month.   

## 22.11.1

[Release notes](https://github.com/wvlet/airframe/releases/tag/v22.11.1)

## 22.11.0

This version upgrades slf4j to 2.0.x series. No application code change is necessary, but 
if you are using an slf4j binder, the binder needs to be upgraded to the version that support slf4j-api 2.0.x. 
See [slf4j FAQ](https://www.slf4j.org/faq.html#changesInVersion200) for more details.

[Release notes](https://github.com/wvlet/airframe/releases/tag/v22.11.0)

## 22.10.4

[Release notes](https://github.com/wvlet/airframe/releases/tag/v22.10.4)

## 22.10.3

This version upgrades to Scala 3.2.1 for Scala 2.13.10 compatibility through TASTy reader.

[Release notes](https://github.com/wvlet/airframe/releases/tag/v22.10.3)

## 22.10.2

[Release notes](https://github.com/wvlet/airframe/releases/tag/v22.10.2)

## 22.10.1

[Release notes](https://github.com/wvlet/airframe/releases/tag/v22.10.1)

## 22.10.0

Upgrade to Scala 2.13.10 to address [a regression in Scala 2.13.9](https://github.com/scala/scala/pull/10155).
And also, an experimental module airframe-http-netty is added to support REST/RPC server in Scala 3.

- Update fluency-core, fluency-fluentd, ... to 2.7.0 ([#2458](https://github.com/wvlet/airframe/issues/2458)) [[f4982c6f0](https://github.com/wvlet/airframe/commit/f4982c6f0)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.50.0 ([#2485](https://github.com/wvlet/airframe/issues/2485)) [[024fea0b6](https://github.com/wvlet/airframe/commit/024fea0b6)]
- Update protobuf-java to 3.21.8 ([#2484](https://github.com/wvlet/airframe/issues/2484)) [[3ca772199](https://github.com/wvlet/airframe/commit/3ca772199)]
- Upgrade to logback-core 1.3.4 ([#2483](https://github.com/wvlet/airframe/issues/2483)) [[d7263e53a](https://github.com/wvlet/airframe/commit/d7263e53a)]
- Update auth to 2.17.293 ([#2481](https://github.com/wvlet/airframe/issues/2481)) [[e59cf9ad3](https://github.com/wvlet/airframe/commit/e59cf9ad3)]
- Update scala-compiler, scala-library, ... to 2.13.10 ([#2476](https://github.com/wvlet/airframe/issues/2476)) [[e4bc9684a](https://github.com/wvlet/airframe/commit/e4bc9684a)]
- Update sbt-scoverage to 2.0.5 ([#2480](https://github.com/wvlet/airframe/issues/2480)) [[ab681912a](https://github.com/wvlet/airframe/commit/ab681912a)]
- Update scalafmt-core to 3.6.0 ([#2479](https://github.com/wvlet/airframe/issues/2479)) [[833afd613](https://github.com/wvlet/airframe/commit/833afd613)]
- Update sbt-mdoc to 2.3.6 ([#2478](https://github.com/wvlet/airframe/issues/2478)) [[d2809f4a7](https://github.com/wvlet/airframe/commit/d2809f4a7)]
- Update sbt, sbt-dependency-tree, ... to 1.7.2 ([#2477](https://github.com/wvlet/airframe/issues/2477)) [[969aa7b2d](https://github.com/wvlet/airframe/commit/969aa7b2d)]
- Update trino-main to 400 ([#2475](https://github.com/wvlet/airframe/issues/2475)) [[dcd91c32a](https://github.com/wvlet/airframe/commit/dcd91c32a)]
- Update swagger-parser to 2.1.5 ([#2474](https://github.com/wvlet/airframe/issues/2474)) [[46646185b](https://github.com/wvlet/airframe/commit/46646185b)]
- Remove unnecessary scala-steward PR labels ([#2482](https://github.com/wvlet/airframe/issues/2482)) [[c7ca2cf1b](https://github.com/wvlet/airframe/commit/c7ca2cf1b)]
- Update netty-all to 4.1.84.Final ([#2473](https://github.com/wvlet/airframe/issues/2473)) [[6513cd85e](https://github.com/wvlet/airframe/commit/6513cd85e)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.49.2 ([#2472](https://github.com/wvlet/airframe/issues/2472)) [[5c9538aba](https://github.com/wvlet/airframe/commit/5c9538aba)]
- Bump codecov/codecov-action from 1 to 3 ([#2469](https://github.com/wvlet/airframe/issues/2469)) [[73a073146](https://github.com/wvlet/airframe/commit/73a073146)]
- Bump actions/upload-artifact from 1 to 3 ([#2470](https://github.com/wvlet/airframe/issues/2470)) [[0b48cf403](https://github.com/wvlet/airframe/commit/0b48cf403)]
- Enable Dependabot for enabling Github Action updates ([#2468](https://github.com/wvlet/airframe/issues/2468)) [[bdf3c2267](https://github.com/wvlet/airframe/commit/bdf3c2267)]
- Upgrade to checkout@v3, setup-java@v3, setup-node@v3, action-junit-report@v3 ([#2466](https://github.com/wvlet/airframe/issues/2466)) [[0e8312cf8](https://github.com/wvlet/airframe/commit/0e8312cf8)]
- Fix MatchError in excluding pseudo headers of JDK's http client ([#2467](https://github.com/wvlet/airframe/issues/2467)) [[64a5e7626](https://github.com/wvlet/airframe/commit/64a5e7626)]
- Exclude pseudo headers from response of JDK's http client ([#2465](https://github.com/wvlet/airframe/issues/2465)) [[5067e85d3](https://github.com/wvlet/airframe/commit/5067e85d3)]
- Registered MBean doesn't have any attributes ([#2464](https://github.com/wvlet/airframe/issues/2464)) [[c035218a8](https://github.com/wvlet/airframe/commit/c035218a8)]
- airframe-netty: Performance optimziation ([#2462](https://github.com/wvlet/airframe/issues/2462)) [[9514866f8](https://github.com/wvlet/airframe/commit/9514866f8)]
- airframe-http-netty: Add an experimental Netty-backed HTTP server implementation ([#2460](https://github.com/wvlet/airframe/issues/2460)) [[41b2ca7fb](https://github.com/wvlet/airframe/commit/41b2ca7fb)]
- Update trino-main to 398 ([#2457](https://github.com/wvlet/airframe/issues/2457)) [[18c9331c2](https://github.com/wvlet/airframe/commit/18c9331c2)]
- Update protobuf-java to 3.21.7 ([#2456](https://github.com/wvlet/airframe/issues/2456)) [[2c591b1b4](https://github.com/wvlet/airframe/commit/2c591b1b4)]
- Update json4s-jackson to 4.0.6 ([#2455](https://github.com/wvlet/airframe/issues/2455)) [[71f239ac4](https://github.com/wvlet/airframe/commit/71f239ac4)]
- Update airframe-codec, airframe-control, ... to 22.9.3 ([#2453](https://github.com/wvlet/airframe/issues/2453)) [[307eee124](https://github.com/wvlet/airframe/commit/307eee124)]

## 22.9.3

This version includes JDK19 support for Scala 2.13 and several bug fixes. 

- airframe-rpc: Use a short class name when an RPC path prefix is given ([#2452](https://github.com/wvlet/airframe/issues/2452)) [[c73aa46fe](https://github.com/wvlet/airframe/commit/c73aa46fe)]
- airspec: Fixes [#2370](https://github.com/wvlet/airframe/issues/2370). Support running specs extending objects ([#2446](https://github.com/wvlet/airframe/issues/2446)) [[c9352ab76](https://github.com/wvlet/airframe/commit/c9352ab76)]
- airspec: Show exceptions during test class initialization failures ([#2445](https://github.com/wvlet/airframe/issues/2445)) [[375d3e200](https://github.com/wvlet/airframe/commit/375d3e200)]
- Add JDK19 support CI ([#2448](https://github.com/wvlet/airframe/issues/2448)) [[14b663873](https://github.com/wvlet/airframe/commit/14b663873)]
- airframe-http: Add RPCStatus.fromHttpStatus(http status) ([#2444](https://github.com/wvlet/airframe/issues/2444)) [[0ebae131b](https://github.com/wvlet/airframe/commit/0ebae131b)]
- Update sbt-mdoc to 2.3.5 ([#2451](https://github.com/wvlet/airframe/issues/2451)) [[17324b7a3](https://github.com/wvlet/airframe/commit/17324b7a3)]
- Update swagger-parser to 2.1.3 ([#2450](https://github.com/wvlet/airframe/issues/2450)) [[320c551ca](https://github.com/wvlet/airframe/commit/320c551ca)]
- Update AirSpec developer doc [[2573061ec](https://github.com/wvlet/airframe/commit/2573061ec)]
- Update snakeyaml to 1.33 ([#2447](https://github.com/wvlet/airframe/issues/2447)) [[15fea6947](https://github.com/wvlet/airframe/commit/15fea6947)]
- airframe-surface: [#2442](https://github.com/wvlet/airframe/issues/2442) Support special symbols (-, +, =) in quoted params ([#2443](https://github.com/wvlet/airframe/issues/2443)) [[dcbf70948](https://github.com/wvlet/airframe/commit/dcbf70948)]
- Update airframe-rpc.md [[8d3356c0c](https://github.com/wvlet/airframe/commit/8d3356c0c)]
- Update sbt-mdoc to 2.3.4 ([#2441](https://github.com/wvlet/airframe/issues/2441)) [[505e04306](https://github.com/wvlet/airframe/commit/505e04306)]
- Update scala-compiler, scala-library, ... to 2.13.9 ([#2439](https://github.com/wvlet/airframe/issues/2439)) [[fc9e92c64](https://github.com/wvlet/airframe/commit/fc9e92c64)]
- Update trino-main to 397 ([#2438](https://github.com/wvlet/airframe/issues/2438)) [[14513fa90](https://github.com/wvlet/airframe/commit/14513fa90)]
- Update sbt-scoverage to 2.0.4 ([#2433](https://github.com/wvlet/airframe/issues/2433)) [[f86d2689e](https://github.com/wvlet/airframe/commit/f86d2689e)]
- airframe-finagle: Toward Scala 3 support ([#2432](https://github.com/wvlet/airframe/issues/2432)) [[bcc942310](https://github.com/wvlet/airframe/commit/bcc942310)]
- Support Scala 3 cross-build ([#2431](https://github.com/wvlet/airframe/issues/2431)) [[f8c95bfd0](https://github.com/wvlet/airframe/commit/f8c95bfd0)]
- Update sbt-airframe to 22.9.2 ([#2429](https://github.com/wvlet/airframe/issues/2429)) [[bc52a0dcb](https://github.com/wvlet/airframe/commit/bc52a0dcb)]
- Update airframe-codec, airframe-control, ... to 22.9.2 ([#2428](https://github.com/wvlet/airframe/issues/2428)) [[1fd191877](https://github.com/wvlet/airframe/commit/1fd191877)]

## 22.9.2

From this version, AirSpec testing library supports Scala 3 + Scala.js! 

- airspec: Support Scala3 + scala.js ([#2427](https://github.com/wvlet/airframe/issues/2427)) [[3a73e4352](https://github.com/wvlet/airframe/commit/3a73e4352)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.49.1 ([#2425](https://github.com/wvlet/airframe/issues/2425)) [[33ab4afab](https://github.com/wvlet/airframe/commit/33ab4afab)]
- airframe-surface: Scala 3 support without reflection ([#2399](https://github.com/wvlet/airframe/issues/2399)) [[568de2fcf](https://github.com/wvlet/airframe/commit/568de2fcf)]
- Update airframe-codec, airframe-control, ... to 22.9.1 ([#2424](https://github.com/wvlet/airframe/issues/2424)) [[387cefbff](https://github.com/wvlet/airframe/commit/387cefbff)]
- Update scala-compiler, scala-library, ... to 2.12.17 ([#2423](https://github.com/wvlet/airframe/issues/2423)) [[92b6b93aa](https://github.com/wvlet/airframe/commit/92b6b93aa)]
- Remove unnecessary snapshoht step [[93ca3ac40](https://github.com/wvlet/airframe/commit/93ca3ac40)]
- Upgrade to Scala 2.12.17 ([#2422](https://github.com/wvlet/airframe/issues/2422)) [[83c0323cf](https://github.com/wvlet/airframe/commit/83c0323cf)]

## 22.9.1

Upgrade to Scala.js 1.11.0.

- Update sbt-scalajs, scalajs-compiler, ... to 1.11.0 ([#2417](https://github.com/wvlet/airframe/issues/2417)) [[b4e8cb8c8](https://github.com/wvlet/airframe/commit/b4e8cb8c8)]
- Update scalajs-test-interface to 1.11.0 ([#2419](https://github.com/wvlet/airframe/issues/2419)) [[f0cd8583b](https://github.com/wvlet/airframe/commit/f0cd8583b)]
- airframe-sql: Fix QName lookup from catalog ([#2421](https://github.com/wvlet/airframe/issues/2421)) [[2b0f9a056](https://github.com/wvlet/airframe/commit/2b0f9a056)]
- airframe-sql: Add LogicalPlan.traverse ([#2420](https://github.com/wvlet/airframe/issues/2420)) [[fccca7e15](https://github.com/wvlet/airframe/commit/fccca7e15)]
- airframe-sql: Resolve CTE before resolving group indexes ([#2414](https://github.com/wvlet/airframe/issues/2414)) [[346ac97d0](https://github.com/wvlet/airframe/commit/346ac97d0)]
- Update circe-parser to 0.14.3 ([#2415](https://github.com/wvlet/airframe/issues/2415)) [[f0915b751](https://github.com/wvlet/airframe/commit/f0915b751)]
- Update trino-main to 396 ([#2416](https://github.com/wvlet/airframe/issues/2416)) [[8e0c1c204](https://github.com/wvlet/airframe/commit/8e0c1c204)]
- Update scalacheck to 1.17.0 ([#2418](https://github.com/wvlet/airframe/issues/2418)) [[1b873989a](https://github.com/wvlet/airframe/commit/1b873989a)]
- Update sbt-scoverage to 2.0.3 ([#2412](https://github.com/wvlet/airframe/issues/2412)) [[687a6f75a](https://github.com/wvlet/airframe/commit/687a6f75a)]
- Update protobuf-java to 3.21.6 ([#2409](https://github.com/wvlet/airframe/issues/2409)) [[b9bc27c27](https://github.com/wvlet/airframe/commit/b9bc27c27)]

## 22.9.0

Added SQL type resolver to airframe-sql and improved Scala 3 compatibility of airframe-surface. 

- airframe-sql: Resolve aggregation keys properly ([#2406](https://github.com/wvlet/airframe/issues/2406)) [[bfdac25bf](https://github.com/wvlet/airframe/commit/bfdac25bf)]
- airframe-sql: Resolve function inputs ([#2401](https://github.com/wvlet/airframe/issues/2401)) [[7965e3f13](https://github.com/wvlet/airframe/commit/7965e3f13)]
- airframe-sql: Resolve joins ([#2389](https://github.com/wvlet/airframe/issues/2389)) [[f874b8917](https://github.com/wvlet/airframe/commit/f874b8917)]
- airframe-sql: Propagate column metadata ([#2376](https://github.com/wvlet/airframe/issues/2376)) [[d6d36b471](https://github.com/wvlet/airframe/commit/d6d36b471)]
- airframe-ulid: [#2106](https://github.com/wvlet/airframe/issues/2106) Add ULID.ofMillis(unix time milliseconds) ([#2400](https://github.com/wvlet/airframe/issues/2400)) [[23c1b3a65](https://github.com/wvlet/airframe/commit/23c1b3a65)]
- airframe-surface: [#2396](https://github.com/wvlet/airframe/issues/2396) Build EnumSurface without reflection ([#2398](https://github.com/wvlet/airframe/issues/2398)) [[4eab8bd91](https://github.com/wvlet/airframe/commit/4eab8bd91)]
- airframe-surface: Scala3 higher-kind type support ([#2395](https://github.com/wvlet/airframe/issues/2395)) [[a1ce6845e](https://github.com/wvlet/airframe/commit/a1ce6845e)]
- Update snakeyaml to 1.32 ([#2404](https://github.com/wvlet/airframe/issues/2404)) [[d1697b04e](https://github.com/wvlet/airframe/commit/d1697b04e)]
- Update sqlite-jdbc to 3.39.3.0 ([#2403](https://github.com/wvlet/airframe/issues/2403)) [[665556f0f](https://github.com/wvlet/airframe/commit/665556f0f)]
- Update trino-main to 395 ([#2402](https://github.com/wvlet/airframe/issues/2402)) [[8c0ce6f6b](https://github.com/wvlet/airframe/commit/8c0ce6f6b)]
- airspec: Upgrade to Scala 3.2.0 ([#2394](https://github.com/wvlet/airframe/issues/2394)) [[ab39f2427](https://github.com/wvlet/airframe/commit/ab39f2427)]
- Update antlr4, antlr4-runtime to 4.11.1 ([#2392](https://github.com/wvlet/airframe/issues/2392)) [[9f5654ec6](https://github.com/wvlet/airframe/commit/9f5654ec6)]
- Update sbt-scalajs-bundler to 0.21.0 ([#2388](https://github.com/wvlet/airframe/issues/2388)) [[a7b95a91e](https://github.com/wvlet/airframe/commit/a7b95a91e)]
- Update scalajs-dom to 2.3.0 ([#2387](https://github.com/wvlet/airframe/issues/2387)) [[6b40ca536](https://github.com/wvlet/airframe/commit/6b40ca536)]
- Upgrade to Scala 3.2.0 ([#2393](https://github.com/wvlet/airframe/issues/2393)) [[e9ce3466b](https://github.com/wvlet/airframe/commit/e9ce3466b)]
- Update sbt-scoverage to 2.0.2 ([#2362](https://github.com/wvlet/airframe/issues/2362)) [[f9f871120](https://github.com/wvlet/airframe/commit/f9f871120)]
- Update trino-main to 394 ([#2386](https://github.com/wvlet/airframe/issues/2386)) [[43b63cb0d](https://github.com/wvlet/airframe/commit/43b63cb0d)]
- airframe-sql: Resolve CTE query types ([#2385](https://github.com/wvlet/airframe/issues/2385)) [[b5c14034b](https://github.com/wvlet/airframe/commit/b5c14034b)]
- Update snakeyaml to 1.31 ([#2383](https://github.com/wvlet/airframe/issues/2383)) [[87dfef611](https://github.com/wvlet/airframe/commit/87dfef611)]
- Update sqlite-jdbc to 3.39.2.1 ([#2382](https://github.com/wvlet/airframe/issues/2382)) [[a8f417273](https://github.com/wvlet/airframe/commit/a8f417273)]
- Update postgresql to 42.5.0 ([#2381](https://github.com/wvlet/airframe/issues/2381)) [[d70ca2230](https://github.com/wvlet/airframe/commit/d70ca2230)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.49.0 ([#2380](https://github.com/wvlet/airframe/issues/2380)) [[6f250895d](https://github.com/wvlet/airframe/commit/6f250895d)]
- Update airframe-codec, airframe-control, ... to 22.8.0 ([#2377](https://github.com/wvlet/airframe/issues/2377)) [[519af3963](https://github.com/wvlet/airframe/commit/519af3963)]

## 22.8.0

- airframe-rpc: Added [RPCContext](https://wvlet.org/airframe/docs/airframe-rpc#rpccontext) to access the original HTTP request and thread-local storage.
- airframe-rpc: Added RPCStatus.(code).newException to report errors to RPC clients. See also https://wvlet.org/airframe/docs/airframe-rpc for more details.

Other updates: 

- airframe-rpc: Add RPCStatus and RPC client documentation ([#2375](https://github.com/wvlet/airframe/issues/2375)) [[196433a38](https://github.com/wvlet/airframe/commit/196433a38)]
- Update trino-main to 393 ([#2372](https://github.com/wvlet/airframe/issues/2372)) [[090f88e3d](https://github.com/wvlet/airframe/commit/090f88e3d)]
- airframe-grpc: Fix thread local storage ([#2373](https://github.com/wvlet/airframe/issues/2373)) [[216a3099c](https://github.com/wvlet/airframe/commit/216a3099c)]
- Update README.md to fix ([#2374](https://github.com/wvlet/airframe/issues/2374)) [[78fb5686c](https://github.com/wvlet/airframe/commit/78fb5686c)]
- Update postgresql to 42.4.2 ([#2371](https://github.com/wvlet/airframe/issues/2371)) [[0b6136e5c](https://github.com/wvlet/airframe/commit/0b6136e5c)]
- airframe-rpc: [#2368](https://github.com/wvlet/airframe/issues/2368) Add RPCContext to access thread-local storage and http request ([#2369](https://github.com/wvlet/airframe/issues/2369)) [[4f7ac2ceb](https://github.com/wvlet/airframe/commit/4f7ac2ceb)]
- Update swagger-parser to 2.1.2 ([#2367](https://github.com/wvlet/airframe/issues/2367)) [[7127ac2f8](https://github.com/wvlet/airframe/commit/7127ac2f8)]
- airframe-sql: Support generic data types ([#2365](https://github.com/wvlet/airframe/issues/2365)) [[f62047af8](https://github.com/wvlet/airframe/commit/f62047af8)]
- Update scalafmt-core to 3.5.9 ([#2366](https://github.com/wvlet/airframe/issues/2366)) [[bf28f3b10](https://github.com/wvlet/airframe/commit/bf28f3b10)]
- Update protobuf-java to 3.21.5 ([#2364](https://github.com/wvlet/airframe/issues/2364)) [[a6054800b](https://github.com/wvlet/airframe/commit/a6054800b)]
- airframe-sql: Add SQL type name parser ([#2363](https://github.com/wvlet/airframe/issues/2363)) [[40a63bb7a](https://github.com/wvlet/airframe/commit/40a63bb7a)]
- Update hadoop-aws, hadoop-client to 3.3.4 ([#2358](https://github.com/wvlet/airframe/issues/2358)) [[78e6a0f41](https://github.com/wvlet/airframe/commit/78e6a0f41)]
- Update sqlite-jdbc to 3.39.2.0 ([#2357](https://github.com/wvlet/airframe/issues/2357)) [[0422e6106](https://github.com/wvlet/airframe/commit/0422e6106)]
- Update trino-main to 392 ([#2355](https://github.com/wvlet/airframe/issues/2355)) [[93ef71f8f](https://github.com/wvlet/airframe/commit/93ef71f8f)]
- Update postgresql to 42.4.1 ([#2356](https://github.com/wvlet/airframe/issues/2356)) [[404353335](https://github.com/wvlet/airframe/commit/404353335)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.48.1 ([#2354](https://github.com/wvlet/airframe/issues/2354)) [[08791e92c](https://github.com/wvlet/airframe/commit/08791e92c)]
- Update finagle-core, finagle-http, ... to 22.7.0 ([#2349](https://github.com/wvlet/airframe/issues/2349)) [[7c647a08d](https://github.com/wvlet/airframe/commit/7c647a08d)]
- Update scala-collection-compat to 2.8.1 ([#2350](https://github.com/wvlet/airframe/issues/2350)) [[20ce92670](https://github.com/wvlet/airframe/commit/20ce92670)]
- Update sbt-mdoc to 2.3.3 ([#2351](https://github.com/wvlet/airframe/issues/2351)) [[385f9a4a4](https://github.com/wvlet/airframe/commit/385f9a4a4)]
- Update protobuf-java to 3.21.4 ([#2348](https://github.com/wvlet/airframe/issues/2348)) [[1dfb248cd](https://github.com/wvlet/airframe/commit/1dfb248cd)]
- airframe-grpc: Fix NPE in Scala 3 ([#2346](https://github.com/wvlet/airframe/issues/2346)) [[a3f5e1c73](https://github.com/wvlet/airframe/commit/a3f5e1c73)]
- airframe-surface: Use GenericSurface for Scala 3 + Scala.js ([#2345](https://github.com/wvlet/airframe/issues/2345)) [[2fb5f8d22](https://github.com/wvlet/airframe/commit/2fb5f8d22)]
- airframe-surface: Read default parameters in Scala 3  ([#2344](https://github.com/wvlet/airframe/issues/2344)) [[75193e4fd](https://github.com/wvlet/airframe/commit/75193e4fd)]
- Add Scala 3 format check ([#2343](https://github.com/wvlet/airframe/issues/2343)) [[5419e65db](https://github.com/wvlet/airframe/commit/5419e65db)]
- airframe-surface: Read annotation in Scala 3 macros ([#2341](https://github.com/wvlet/airframe/issues/2341)) [[2e045057f](https://github.com/wvlet/airframe/commit/2e045057f)]
- Update/grpc netty shaded 1.48.0 ([#2340](https://github.com/wvlet/airframe/issues/2340)) [[f697078e2](https://github.com/wvlet/airframe/commit/f697078e2)]
- Update protobuf-java to 3.21.3 ([#2335](https://github.com/wvlet/airframe/issues/2335)) [[0cd29c7d6](https://github.com/wvlet/airframe/commit/0cd29c7d6)]
- Update trino-main to 391 ([#2339](https://github.com/wvlet/airframe/issues/2339)) [[3ca0cfef8](https://github.com/wvlet/airframe/commit/3ca0cfef8)]
- Update airframe-http, ... to 22.7.3 ([#2333](https://github.com/wvlet/airframe/issues/2333)) [[da9432d3b](https://github.com/wvlet/airframe/commit/da9432d3b)]
- Upgrade to airframe/airspec 22.7.3 ([#2332](https://github.com/wvlet/airframe/issues/2332)) [[cd6622d4d](https://github.com/wvlet/airframe/commit/cd6622d4d)]
- Update airframe-codec, airframe-control, ... to 22.7.3 ([#2331](https://github.com/wvlet/airframe/issues/2331)) [[188cb691b](https://github.com/wvlet/airframe/commit/188cb691b)]

## 22.7.3

This version removes the absolute source code paths embedded to the compiled binaries when using airframe-log, airframe-di, airspec. This will reduce the generated class file size and protect the privacy of your local folder information.

- Removing full source paths from the generated code  ([#2330](https://github.com/wvlet/airframe/issues/2330)) [[b10c9490e](https://github.com/wvlet/airframe/commit/b10c9490e)]
- Update trino-main to 390 ([#2325](https://github.com/wvlet/airframe/issues/2325)) [[681c82780](https://github.com/wvlet/airframe/commit/681c82780)]
- Use 2.12.x wildcard version in CI ([#2329](https://github.com/wvlet/airframe/issues/2329)) [[1090ccf00](https://github.com/wvlet/airframe/commit/1090ccf00)]
- Update airframe-codec, airframe-control, ... to 22.7.2 ([#2323](https://github.com/wvlet/airframe/issues/2323)) [[7ba1b05ff](https://github.com/wvlet/airframe/commit/7ba1b05ff)]
- Update airframe-codec, airframe-control, ... to 22.7.2 ([#2321](https://github.com/wvlet/airframe/issues/2321)) [[8f90e5079](https://github.com/wvlet/airframe/commit/8f90e5079)]

## 22.7.2

This version recompiles airframe for Java8 target.

- Retarget to JDK8 ([#2320](https://github.com/wvlet/airframe/issues/2320)) [[411f510e0](https://github.com/wvlet/airframe/commit/411f510e0)]
- Update sbt, sbt-dependency-tree, ... to 1.7.1 ([#2317](https://github.com/wvlet/airframe/issues/2317)) [[6dbd501e8](https://github.com/wvlet/airframe/commit/6dbd501e8)]
- Update sbt, sbt-dependency-tree, ... to 1.7.0 ([#2314](https://github.com/wvlet/airframe/issues/2314)) [[713302a98](https://github.com/wvlet/airframe/commit/713302a98)]
- airframe-http: Add a filter for http client logs ([#2315](https://github.com/wvlet/airframe/issues/2315)) [[199f0e950](https://github.com/wvlet/airframe/commit/199f0e950)]
- Update airspec to 22.7.1 ([#2310](https://github.com/wvlet/airframe/issues/2310)) [[c3969131f](https://github.com/wvlet/airframe/commit/c3969131f)]
- Ignore unstable tests ([#2313](https://github.com/wvlet/airframe/issues/2313)) [[9e7fea68f](https://github.com/wvlet/airframe/commit/9e7fea68f)]
- Update scala-collection-compat to 2.8.0 ([#2311](https://github.com/wvlet/airframe/issues/2311)) [[81edbb08f](https://github.com/wvlet/airframe/commit/81edbb08f)]
- Update airframe-codec, airframe-control, ... to 22.7.1 ([#2309](https://github.com/wvlet/airframe/issues/2309)) [[4517db953](https://github.com/wvlet/airframe/commit/4517db953)]
- Update trino-main to 389 ([#2306](https://github.com/wvlet/airframe/issues/2306)) [[04ac827a1](https://github.com/wvlet/airframe/commit/04ac827a1)]
- Use Scala 2.12.16 for testing ([#2308](https://github.com/wvlet/airframe/issues/2308)) [[0a2cdc133](https://github.com/wvlet/airframe/commit/0a2cdc133)]
- Update airframe-codec, airframe-control, ... to 22.7.1 ([#2305](https://github.com/wvlet/airframe/issues/2305)) [[75e3aa13a](https://github.com/wvlet/airframe/commit/75e3aa13a)]

## 22.7.1

This version includes minor updates and upgrades to Scala 3.1.3. 

- airframe-metrics: Use ZoneId for TimeWindow.withTimeZone() instead of ZoneOffset ([#2298](https://github.com/wvlet/airframe/issues/2298)) [[2f0fb0d79](https://github.com/wvlet/airframe/commit/2f0fb0d79)]
- airframe-launcher: Fixes [#2291](https://github.com/wvlet/airframe/issues/2291) support nested objects in command args ([#2297](https://github.com/wvlet/airframe/issues/2297)) [[e6003ef4d](https://github.com/wvlet/airframe/commit/e6003ef4d)]
- Upgrade to Scala 3.1.3 ([#2284](https://github.com/wvlet/airframe/issues/2284)) [[e6c5abddc](https://github.com/wvlet/airframe/commit/e6c5abddc)]

- Use Scala 2.12.16 for sbt-airframe build ([#2304](https://github.com/wvlet/airframe/issues/2304)) [[efc937137](https://github.com/wvlet/airframe/commit/efc937137)]
- Update airframe-codec, airframe-control, ... to 22.7.0 ([#2303](https://github.com/wvlet/airframe/issues/2303)) [[b2afe579c](https://github.com/wvlet/airframe/commit/b2afe579c)]
- Update airframe-codec, airframe-control, ... to 22.6.4 ([#2301](https://github.com/wvlet/airframe/issues/2301)) [[13f6ad130](https://github.com/wvlet/airframe/commit/13f6ad130)]
- Update airspec to 22.7.0 ([#2302](https://github.com/wvlet/airframe/issues/2302)) [[6dc076354](https://github.com/wvlet/airframe/commit/6dc076354)]
- Update airspec, sbt-airframe to 22.7.0 ([#2295](https://github.com/wvlet/airframe/issues/2295)) [[e3512d5ff](https://github.com/wvlet/airframe/commit/e3512d5ff)]

## 22.7.0

This version upgrades Scala.js to 1.10.1, msgpack-core 0.9.3 (JDK17 support).
AirSpec 22.7.0 fixes JUnit test report (test-repot.xml) output. 

- Update fluency-core, fluency-fluentd, ... to 2.6.5 ([#2293](https://github.com/wvlet/airframe/issues/2293)) [[37d990ba5](https://github.com/wvlet/airframe/commit/37d990ba5)]
- airspec: Fix [#2290](https://github.com/wvlet/airframe/issues/2290). Report test spec names to test-report.xml properly ([#2294](https://github.com/wvlet/airframe/issues/2294)) [[6457f9389](https://github.com/wvlet/airframe/commit/6457f9389)]
- Update trino-main to 388 ([#2288](https://github.com/wvlet/airframe/issues/2288)) [[007703355](https://github.com/wvlet/airframe/commit/007703355)]
- Update msgpack-core to 0.9.3 ([#2285](https://github.com/wvlet/airframe/issues/2285)) [[ccb849442](https://github.com/wvlet/airframe/commit/ccb849442)]
- Update scala-js-macrotask-executor to 1.1.0 ([#2283](https://github.com/wvlet/airframe/issues/2283)) [[e9788aec7](https://github.com/wvlet/airframe/commit/e9788aec7)]
- Update scalajs-test-interface to 1.10.1 ([#2281](https://github.com/wvlet/airframe/issues/2281)) [[996b9a8ed](https://github.com/wvlet/airframe/commit/996b9a8ed)]
- Update protobuf-java to 3.21.2 ([#2275](https://github.com/wvlet/airframe/issues/2275)) [[18bcf2007](https://github.com/wvlet/airframe/commit/18bcf2007)]
- openapi: Stabilize unit tests ([#2280](https://github.com/wvlet/airframe/issues/2280)) [[298b370ab](https://github.com/wvlet/airframe/commit/298b370ab)]
- Update sbt-scalajs, scalajs-compiler, ... to 1.10.1 ([#2278](https://github.com/wvlet/airframe/issues/2278)) [[009cd5526](https://github.com/wvlet/airframe/commit/009cd5526)]
- Update sbt-scoverage to 2.0.0 ([#2277](https://github.com/wvlet/airframe/issues/2277)) [[f10a1c6be](https://github.com/wvlet/airframe/commit/f10a1c6be)]
- Update auth to 2.17.217 ([#2271](https://github.com/wvlet/airframe/issues/2271)) [[550cc1406](https://github.com/wvlet/airframe/commit/550cc1406)]
- Update trino-main to 387 ([#2270](https://github.com/wvlet/airframe/issues/2270)) [[9758ec092](https://github.com/wvlet/airframe/commit/9758ec092)]

## 22.6.4

This version has a minor fix for supporting upcoming Scala 2.13.9

- openapi: Avoid recursive cache update to support future Scala 2.13.x ([#2269](https://github.com/wvlet/airframe/issues/2269)) [[8e3c80f74](https://github.com/wvlet/airframe/commit/8e3c80f74)]
- Update auth to 2.17.216 ([#2264](https://github.com/wvlet/airframe/issues/2264)) [[55ba110cf](https://github.com/wvlet/airframe/commit/55ba110cf)]
- Update airspec, sbt-airframe to 22.6.3 ([#2268](https://github.com/wvlet/airframe/issues/2268)) [[a3c111c25](https://github.com/wvlet/airframe/commit/a3c111c25)]
- Update airframe-codec, airframe-control, ... to 22.6.3 ([#2267](https://github.com/wvlet/airframe/issues/2267)) [[9f2defaad](https://github.com/wvlet/airframe/commit/9f2defaad)]

## 22.6.3

This version is a fix for a furture version of Scala 2.13

- airframe-di: Fixes [#2265](https://github.com/wvlet/airframe/issues/2265) Avoid recursive update of ConcurrentHashMap ([#2266](https://github.com/wvlet/airframe/issues/2266)) [[c753611cc](https://github.com/wvlet/airframe/commit/c753611cc)]

## 22.6.2

This version is mostly internal updates.

- airframe-surface: Avoid recursive update of surface cache ([#2261](https://github.com/wvlet/airframe/issues/2261)) [[34918279e](https://github.com/wvlet/airframe/commit/34918279e)]
- Update auth to 2.17.215 ([#2259](https://github.com/wvlet/airframe/issues/2259)) [[e9227e9b6](https://github.com/wvlet/airframe/commit/e9227e9b6)]
- Update spark-sql to 3.3.0 ([#2251](https://github.com/wvlet/airframe/issues/2251)) [[1329010c1](https://github.com/wvlet/airframe/commit/1329010c1)]
- Update trino-main to 386 ([#2253](https://github.com/wvlet/airframe/issues/2253)) [[f42dc2d4d](https://github.com/wvlet/airframe/commit/f42dc2d4d)]
- Update okhttp to 4.10.0 ([#2247](https://github.com/wvlet/airframe/issues/2247)) [[a0b80cdb2](https://github.com/wvlet/airframe/commit/a0b80cdb2)]
- Update swagger-parser to 2.1.1 ([#2256](https://github.com/wvlet/airframe/issues/2256)) [[ac2b0e400](https://github.com/wvlet/airframe/commit/ac2b0e400)]
- Update msgpack-core to 0.9.2 ([#2258](https://github.com/wvlet/airframe/issues/2258)) [[2686c95ae](https://github.com/wvlet/airframe/commit/2686c95ae)]
- Update airframe-http, ... to 22.6.1 ([#2248](https://github.com/wvlet/airframe/issues/2248)) [[67ca7d3b5](https://github.com/wvlet/airframe/commit/67ca7d3b5)]
- Update scala-compiler, scala-library, ... to 2.12.16 ([#2245](https://github.com/wvlet/airframe/issues/2245)) [[3341ec55f](https://github.com/wvlet/airframe/commit/3341ec55f)]
- Fix jdk11 version in CI ([#2244](https://github.com/wvlet/airframe/issues/2244)) [[ea9ffe8d6](https://github.com/wvlet/airframe/commit/ea9ffe8d6)]
- Update postgresql to 42.4.0 ([#2242](https://github.com/wvlet/airframe/issues/2242)) [[ba65b7efc](https://github.com/wvlet/airframe/commit/ba65b7efc)]
- Update auth to 2.17.208 ([#2243](https://github.com/wvlet/airframe/issues/2243)) [[08b82cdbc](https://github.com/wvlet/airframe/commit/08b82cdbc)]
- Update .scala-steward.conf [[dd0a7f74c](https://github.com/wvlet/airframe/commit/dd0a7f74c)]
- Update trino-main to 385 ([#2240](https://github.com/wvlet/airframe/issues/2240)) [[1505ac573](https://github.com/wvlet/airframe/commit/1505ac573)]
- Update scala-ulid to 1.0.24 ([#2238](https://github.com/wvlet/airframe/issues/2238)) [[ddad6cc95](https://github.com/wvlet/airframe/commit/ddad6cc95)]
- Update auth to 2.17.206 ([#2239](https://github.com/wvlet/airframe/issues/2239)) [[c2d0d850b](https://github.com/wvlet/airframe/commit/c2d0d850b)]
- Use JDK17 for regular CI and JDK11 for releases ([#2235](https://github.com/wvlet/airframe/issues/2235)) [[9a046d1dd](https://github.com/wvlet/airframe/commit/9a046d1dd)]
- airframe-http: Add RPCMethod for simplifying RPC client code ([#2234](https://github.com/wvlet/airframe/issues/2234)) [[de0bc2c78](https://github.com/wvlet/airframe/commit/de0bc2c78)]
- Use only sed for dynver computation ([#2233](https://github.com/wvlet/airframe/issues/2233)) [[e33ce183e](https://github.com/wvlet/airframe/commit/e33ce183e)]
- airframe-rpc: Add a new integration test with the new Http RPC client ([#2232](https://github.com/wvlet/airframe/issues/2232)) [[10ae968a7](https://github.com/wvlet/airframe/commit/10ae968a7)]
- airframe-http: Http client interface clean up  ([#2231](https://github.com/wvlet/airframe/issues/2231)) [[439fb5b3b](https://github.com/wvlet/airframe/commit/439fb5b3b)]
- Update trino-main to 384 ([#2229](https://github.com/wvlet/airframe/issues/2229)) [[cc822516d](https://github.com/wvlet/airframe/commit/cc822516d)]
- airframe-http: Add HttpChannel and ClientFilter ([#2230](https://github.com/wvlet/airframe/issues/2230)) [[5613aa3b2](https://github.com/wvlet/airframe/commit/5613aa3b2)]
- Update auth to 2.17.204 ([#2227](https://github.com/wvlet/airframe/issues/2227)) [[a060cd5e4](https://github.com/wvlet/airframe/commit/a060cd5e4)]
- Update antlr4, antlr4-runtime to 4.10.1 ([#2216](https://github.com/wvlet/airframe/issues/2216)) [[a8fa16e53](https://github.com/wvlet/airframe/commit/a8fa16e53)]
- Update airspec to 22.6.1 ([#2226](https://github.com/wvlet/airframe/issues/2226)) [[9f3372768](https://github.com/wvlet/airframe/commit/9f3372768)]
- Update airframe-codec, airframe-control, ... to 22.6.1 ([#2225](https://github.com/wvlet/airframe/issues/2225)) [[c1722cd18](https://github.com/wvlet/airframe/commit/c1722cd18)]

## 22.6.1

This is a bug fix release of airframe-http client.


- airframe-http: Fix deadlock at http client threads ([#2223](https://github.com/wvlet/airframe/issues/2223)) [[a900e0e88](https://github.com/wvlet/airframe/commit/a900e0e88)]

### Internal updates

- Update scalafmt-core to 3.5.8 ([#2219](https://github.com/wvlet/airframe/issues/2219)) [[9c8ff6d1d](https://github.com/wvlet/airframe/commit/9c8ff6d1d)]
- Update auth to 2.17.203 ([#2222](https://github.com/wvlet/airframe/issues/2222)) [[1a7ae2be6](https://github.com/wvlet/airframe/commit/1a7ae2be6)]
- Update parquet-avro, parquet-hadoop to 1.12.3 ([#2217](https://github.com/wvlet/airframe/issues/2217)) [[ba678f5b3](https://github.com/wvlet/airframe/commit/ba678f5b3)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.47.0 ([#2214](https://github.com/wvlet/airframe/issues/2214)) [[ab5b8f35e](https://github.com/wvlet/airframe/commit/ab5b8f35e)]
- Update trino-main to 383 ([#2215](https://github.com/wvlet/airframe/issues/2215)) [[d92939c9b](https://github.com/wvlet/airframe/commit/d92939c9b)]
- Update protobuf-java to 3.21.1 ([#2213](https://github.com/wvlet/airframe/issues/2213)) [[93218a265](https://github.com/wvlet/airframe/commit/93218a265)]

## 22.6.0

This version extends Scala 3 support for additional modules, including config, fluentd, http, jdbc, jmx, launcher, rx-html, sql, parquet. 

From this version, we drop the support of Java 8 for improving the maintainability and for adopting new Java client of JDK11, which can be used without any additional dependencies.
airframe-http added a new standard http client implementation (Http.client.newSync/AsyncClient), which works both in JVM and Scala.js.

### Major changes

- Drop Java8 support and use Java11 http client by default ([#2191](https://github.com/wvlet/airframe/issues/2191)) [[716a3fec5](https://github.com/wvlet/airframe/commit/716a3fec5)]
- airframe-parquet: Scala 3 support ([#2211](https://github.com/wvlet/airframe/issues/2211)) [[8ef953593](https://github.com/wvlet/airframe/commit/8ef953593)]
- airframe-sql/parquet: Scala 3 support ([#2208](https://github.com/wvlet/airframe/issues/2208)) [[90931d9ec](https://github.com/wvlet/airframe/commit/90931d9ec)]
- airframe-jdbc: Scala 3 support ([#2207](https://github.com/wvlet/airframe/issues/2207)) [[306581c34](https://github.com/wvlet/airframe/commit/306581c34)]
- More dotty support ([#2147](https://github.com/wvlet/airframe/issues/2147)) [[83bf966e0](https://github.com/wvlet/airframe/commit/83bf966e0)]
- airframe-http: Add readAs[Resp], call[Req, Resp] to http clients ([#2206](https://github.com/wvlet/airframe/issues/2206)) [[5b20fca6f](https://github.com/wvlet/airframe/commit/5b20fca6f)]
- airframe-config: Scala 3 support ([#2205](https://github.com/wvlet/airframe/issues/2205)) [[410e8d6b2](https://github.com/wvlet/airframe/commit/410e8d6b2)]
- airframe-fluentd: Scala 3 support ([#2204](https://github.com/wvlet/airframe/issues/2204)) [[00e398d53](https://github.com/wvlet/airframe/commit/00e398d53)]
- Upgrade to Scala 3.1.2 for AirSpec ([#2203](https://github.com/wvlet/airframe/issues/2203)) [[c27342346](https://github.com/wvlet/airframe/commit/c27342346)]
- airframe-jmx: Scala 3 support ([#2201](https://github.com/wvlet/airframe/issues/2201)) [[0068ea079](https://github.com/wvlet/airframe/commit/0068ea079)]
- airframe-surface: Do not traverse non-public constructor ([#2199](https://github.com/wvlet/airframe/issues/2199)) [[558e981f2](https://github.com/wvlet/airframe/commit/558e981f2)]
- airframe-http-codegen: Scala 3 support (Fix OpenAPI generator) ([#2198](https://github.com/wvlet/airframe/issues/2198)) [[edec851b1](https://github.com/wvlet/airframe/commit/edec851b1)]
- airframe-launcher: Scala 3 support  ([#2197](https://github.com/wvlet/airframe/issues/2197)) [[20c7cd81a](https://github.com/wvlet/airframe/commit/20c7cd81a)]
- airframe-rx-html: Scala 3 support ([#2196](https://github.com/wvlet/airframe/issues/2196)) [[c8d7f3096](https://github.com/wvlet/airframe/commit/c8d7f3096)]
- airframe-http-codegen: Add 'rpc' client type ([#2195](https://github.com/wvlet/airframe/issues/2195)) [[cda07225d](https://github.com/wvlet/airframe/commit/cda07225d)]
- airframe-grpc: Fix syntax for Scala 3 ([#2193](https://github.com/wvlet/airframe/issues/2193)) [[ee60277a8](https://github.com/wvlet/airframe/commit/ee60277a8)]
- airframe-http: Scala 3 support ([#2192](https://github.com/wvlet/airframe/issues/2192)) [[1b94fa64f](https://github.com/wvlet/airframe/commit/1b94fa64f)]
- airframe-http: Scala 3 support [[58528d1fc](https://github.com/wvlet/airframe/commit/58528d1fc)]
- airframe-http: Add a common Sync/AsyncClient interface for Scala JVM and Scala.js ([#2190](https://github.com/wvlet/airframe/issues/2190)) [[52c0e7e50](https://github.com/wvlet/airframe/commit/52c0e7e50)]
- airframe-http: Add JSHttpAsyncClient ([#2189](https://github.com/wvlet/airframe/issues/2189)) [[f4660c5d8](https://github.com/wvlet/airframe/commit/f4660c5d8)]
- airframe-http: Add sync/async HTTP clients using JDK11 HttpClient ([#2188](https://github.com/wvlet/airframe/issues/2188)) [[653ce3526](https://github.com/wvlet/airframe/commit/653ce3526)]
- airframe-http: Add read/connect timeout settings to HttpClientConfig ([#2187](https://github.com/wvlet/airframe/issues/2187)) [[de9036e56](https://github.com/wvlet/airframe/commit/de9036e56)]

### Internal changes

- Run Scala 3 unit tests on CI ([#2212](https://github.com/wvlet/airframe/issues/2212)) [[8004d8a6a](https://github.com/wvlet/airframe/commit/8004d8a6a)]
- Update sbt-sonatype to 3.9.13 ([#2186](https://github.com/wvlet/airframe/issues/2186)) [[bd5df898a](https://github.com/wvlet/airframe/commit/bd5df898a)]
- Update postgresql to 42.3.6 ([#2184](https://github.com/wvlet/airframe/issues/2184)) [[37d09b1c2](https://github.com/wvlet/airframe/commit/37d09b1c2)]
- Update sbt-airframe to 22.5.0 ([#2185](https://github.com/wvlet/airframe/issues/2185)) [[1e69304aa](https://github.com/wvlet/airframe/commit/1e69304aa)]
- Update airspec to 22.5.0 ([#2183](https://github.com/wvlet/airframe/issues/2183)) [[89ab0953a](https://github.com/wvlet/airframe/commit/89ab0953a)]
- Update auth to 2.17.197 ([#2182](https://github.com/wvlet/airframe/issues/2182)) [[f46c139da](https://github.com/wvlet/airframe/commit/f46c139da)]

## 22.5.0

This version changes the behavior of AirSpec in order to support [async testing](https://wvlet.org/airframe/docs/airspec#async-testing) for tests that return `Future[_]` values. With async testing, you don't need to wait the completion of `Future[_]` objects in your test cases. This is especially useful when you need to await network responses in your Scala and Scala.js test code.  

If you have tests that acquire resources around Future, you may see unexpected execution test order due to this change. To properly acquire and release resources for async testing, consider using newly added methods `Control.withResourceAsync(resource)` and `wvlet.airframe.control.Resource[A].wrapFuture`.

This version also adds `wvlet.airframe.http.RPCStatus` error code, which will be the standard error reporting method in Airframe RPC. In the upcoming Airframe versions, you can use `RPCStatus.newException(...)` to propagate RPC server-side errors into clients in the form of `wvlet.airframe.http.RPCException`.  

### Major updates

- AirSpec: Add async test support ([#2159](https://github.com/wvlet/airframe/issues/2159)) [[8046872d9](https://github.com/wvlet/airframe/commit/8046872d9)]
- airframe-http: [#1559](https://github.com/wvlet/airframe/issues/1559) Use Jitter by default with max retry = 15 in http clients ([#2180](https://github.com/wvlet/airframe/issues/2180)) [[f72605a42](https://github.com/wvlet/airframe/commit/f72605a42)]
- airframe-rx: Add RxVar.stop to send OnCompletion event
- airframe-ulid: Make ULID a regular class and map to string in OpenAPI ([#2155](https://github.com/wvlet/airframe/issues/2155)) [[ea7de71e3](https://github.com/wvlet/airframe/commit/ea7de71e3)]
- airframe-control: Add Resource[R] for releasing resources at ease in AirSpec ([#2162](https://github.com/wvlet/airframe/issues/2162)) [[c7fcfe7d4](https://github.com/wvlet/airframe/commit/c7fcfe7d4)]
- Upgrade to Scala 3.1.2 ([#2161](https://github.com/wvlet/airframe/issues/2161)) [[4ba827f86](https://github.com/wvlet/airframe/commit/4ba827f86)]
- Update scalajs-dom to 2.2.0 ([#2173](https://github.com/wvlet/airframe/issues/2173)) [[17fbf20fc](https://github.com/wvlet/airframe/commit/17fbf20fc)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.46.0 ([#2138](https://github.com/wvlet/airframe/issues/2138)) [[0c6d81e87](https://github.com/wvlet/airframe/commit/0c6d81e87)]
- Update finagle-core, finagle-http, ... to 22.4.0 ([#2133](https://github.com/wvlet/airframe/issues/2133)) [[852f5cf5c](https://github.com/wvlet/airframe/commit/852f5cf5c)]

### Minor updates

- airframe-log: Add Logger.supporessLog, suppressLogAroundFuture
- airframe-control: Add Control.withResourceAsync
- airframe-control: Add Resource.wrapFuture
- airframe-http: Add the default mappings from gRPC code to RPCStatus
- airframe-http: Add @description annotation ([#2157](https://github.com/wvlet/airframe/issues/2157)) [[d43e5b26b](https://github.com/wvlet/airframe/commit/d43e5b26b)]
- airframe-sql: Mark CTAS as Update type ([#2181](https://github.com/wvlet/airframe/issues/2181)) [[c12a7760e](https://github.com/wvlet/airframe/commit/c12a7760e)]
- airframe-grpc: Add GrpcClient for consolidating a logic for handling RPCException ([#2172](https://github.com/wvlet/airframe/issues/2172)) [[4c8f1c24b](https://github.com/wvlet/airframe/commit/4c8f1c24b)]
- airframe-grpc: Add GrpcClient to handle RPCException properly
- Add RPCHttpClient to handle RPCException at client side ([#2165](https://github.com/wvlet/airframe/issues/2165)) [[782b3434f](https://github.com/wvlet/airframe/commit/782b3434f)]
- airframe-http: Add RPCEncoding object for managing application/msgpack, json content types ([#2144](https://github.com/wvlet/airframe/issues/2144)) [[a9a04eb40](https://github.com/wvlet/airframe/commit/a9a04eb40)]
- openapi: Suppress package prefix from tags ([#2158](https://github.com/wvlet/airframe/issues/2158)) [[cc284547f](https://github.com/wvlet/airframe/commit/cc284547f)]
- openapi: Support removing package prefixes from object names  ([#2154](https://github.com/wvlet/airframe/issues/2154)) [[6ffe483ab](https://github.com/wvlet/airframe/commit/6ffe483ab)]
- [#2419](https://github.com/wvlet/airframe/issues/2419): Use optional OpenAPI parameter if a default value is defined ([#2153](https://github.com/wvlet/airframe/issues/2153)) [[ebc7b86f1](https://github.com/wvlet/airframe/commit/ebc7b86f1)]
- airframe-http-finagle: Propagate RPCException to clients ([#2141](https://github.com/wvlet/airframe/issues/2141)) [[e43122068](https://github.com/wvlet/airframe/commit/e43122068)]
- airframe-grpc: Propagate RPCException to clients ([#2139](https://github.com/wvlet/airframe/issues/2139)) [[7f211936b](https://github.com/wvlet/airframe/commit/7f211936b)]
- airframe-rpc: Add RPC status code ([#1973](https://github.com/wvlet/airframe/issues/1973)) [[dccb460de](https://github.com/wvlet/airframe/commit/dccb460de)]
- Update fluency-core, fluency-fluentd, ... to 2.6.4 ([#2137](https://github.com/wvlet/airframe/issues/2137)) [[a4b463e22](https://github.com/wvlet/airframe/commit/a4b463e22)]
- Update postgresql to 42.3.5 ([#2152](https://github.com/wvlet/airframe/issues/2152)) [[7b522f269](https://github.com/wvlet/airframe/commit/7b522f269)]
- Update portable-scala-reflect to 1.1.2 ([#2134](https://github.com/wvlet/airframe/issues/2134)) [[ba4f4efa7](https://github.com/wvlet/airframe/commit/ba4f4efa7)]


### Internal dependency updates

- Update circe-parser to 0.14.2 ([#2177](https://github.com/wvlet/airframe/issues/2177)) [[fc713794b](https://github.com/wvlet/airframe/commit/fc713794b)]
- Update swagger-parser to 2.0.33 ([#2178](https://github.com/wvlet/airframe/issues/2178)) [[e64c35197](https://github.com/wvlet/airframe/commit/e64c35197)]
- Update hadoop-aws, hadoop-client to 3.3.3 ([#2176](https://github.com/wvlet/airframe/issues/2176)) [[954c549f7](https://github.com/wvlet/airframe/commit/954c549f7)]
- Update trino-main to 381 ([#2175](https://github.com/wvlet/airframe/issues/2175)) [[3c4c34757](https://github.com/wvlet/airframe/commit/3c4c34757)]
- Update trino-main to 380 ([#2160](https://github.com/wvlet/airframe/issues/2160)) [[f99e08e49](https://github.com/wvlet/airframe/commit/f99e08e49)]
- Update airframe-rpc.md ([#2156](https://github.com/wvlet/airframe/issues/2156)) [[13af55b84](https://github.com/wvlet/airframe/commit/13af55b84)]
- Update scalafmt-core to 3.5.2 ([#2146](https://github.com/wvlet/airframe/issues/2146)) [[c44e6a647](https://github.com/wvlet/airframe/commit/c44e6a647)]
- Update trino-main to 379 ([#2143](https://github.com/wvlet/airframe/issues/2143)) [[88e810598](https://github.com/wvlet/airframe/commit/88e810598)]
- Update trino-main to 378 ([#2136](https://github.com/wvlet/airframe/issues/2136)) [[27cb12c8b](https://github.com/wvlet/airframe/commit/27cb12c8b)]
- Update protobuf-java to 3.20.1 ([#2135](https://github.com/wvlet/airframe/issues/2135)) [[b365c8002](https://github.com/wvlet/airframe/commit/b365c8002)]
- Update scalafmt-core to 3.5.1 ([#2132](https://github.com/wvlet/airframe/issues/2132)) [[98229b1ed](https://github.com/wvlet/airframe/commit/98229b1ed)]
- Update postgresql to 42.3.4 ([#2129](https://github.com/wvlet/airframe/issues/2129)) [[a106bfa5f](https://github.com/wvlet/airframe/commit/a106bfa5f)]
- Update airspec, sbt-airframe to 22.4.2 ([#2131](https://github.com/wvlet/airframe/issues/2131)) [[90000213b](https://github.com/wvlet/airframe/commit/90000213b)]

## 22.4.2

A minor bug fix release.

- airframe-log: Handle occasional JMX registration failure in JDK17 [#2127](https://github.com/wvlet/airframe/issues/2127) ([#2128](https://github.com/wvlet/airframe/issues/2128)) [[8d724e2e](https://github.com/wvlet/airframe/commit/8d724e2e)]
- Update trino-main to 377 ([#2126](https://github.com/wvlet/airframe/issues/2126)) [[e2bf0386](https://github.com/wvlet/airframe/commit/e2bf0386)]
- Update airspec to 22.4.1 ([#2124](https://github.com/wvlet/airframe/issues/2124)) [[3e0b3224](https://github.com/wvlet/airframe/commit/3e0b3224)]
- Update sbt-airframe to 22.4.1 ([#2123](https://github.com/wvlet/airframe/issues/2123)) [[312b7ab8](https://github.com/wvlet/airframe/commit/312b7ab8)]

## 22.4.1

This version upgrades to [Scala.js 1.10.0](https://www.scala-js.org/news/2022/04/04/announcing-scalajs-1.10.0/) to address a security vulnerability [CVE-2022-28355](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-28355) found in java.util.UUID.randomUUID() implementation of Scala.js. If UUID.randomUUID() is used for generting publicly visible IDs, generated IDs were preditable in the former Scala.js versions.   
Similarily, airframe-ulid for Scala.js is fixed to use SecureRandom to avoid generating predictable ULID values.


- Upgrade to Scala.js 1.10.0 ([#2122](https://github.com/wvlet/airframe/issues/2122)) [[d2ce4725](https://github.com/wvlet/airframe/commit/d2ce4725)]
- airframe-ulid: Use SecureRandom-based generator for Scala.js ([#2121](https://github.com/wvlet/airframe/issues/2121)) [[07936e55](https://github.com/wvlet/airframe/commit/07936e55)]
- Add scala-js-java-securerandom for Scala.js 1.10.0 ([#2117](https://github.com/wvlet/airframe/issues/2117)) [[96f5d37e](https://github.com/wvlet/airframe/commit/96f5d37e)]
- Update airspec to 22.4.0 ([#2120](https://github.com/wvlet/airframe/issues/2120)) [[e1a607d6](https://github.com/wvlet/airframe/commit/e1a607d6)]

## 22.4.0

This is a maintenance release for upgrading internal libraries.

- Remove dependency to UUID.randomUUID()  ([#2118](https://github.com/wvlet/airframe/issues/2118)) [[75f17f24](https://github.com/wvlet/airframe/commit/75f17f24)]
- Update trino-main to 376 ([#2115](https://github.com/wvlet/airframe/issues/2115)) [[39a9bda2](https://github.com/wvlet/airframe/commit/39a9bda2)]
- Update scalacheck to 1.16.0 ([#2116](https://github.com/wvlet/airframe/issues/2116)) [[ef986b63](https://github.com/wvlet/airframe/commit/ef986b63)]
- Update json4s-jackson to 4.0.5 ([#2113](https://github.com/wvlet/airframe/issues/2113)) [[cc6f312e](https://github.com/wvlet/airframe/commit/cc6f312e)]
- Update protobuf-java to 3.20.0 ([#2110](https://github.com/wvlet/airframe/issues/2110)) [[ae8acf37](https://github.com/wvlet/airframe/commit/ae8acf37)]
- Update swagger-parser to 2.0.32 ([#2111](https://github.com/wvlet/airframe/issues/2111)) [[b35d7e29](https://github.com/wvlet/airframe/commit/b35d7e29)]
- Update scalafmt-core to 3.5.0 ([#2109](https://github.com/wvlet/airframe/issues/2109)) [[1d53aa78](https://github.com/wvlet/airframe/commit/1d53aa78)]
- Update sbt-airframe to 22.3.0 ([#2108](https://github.com/wvlet/airframe/issues/2108)) [[9e0210c5](https://github.com/wvlet/airframe/commit/9e0210c5)]
- Update airspec to 22.3.0 ([#2107](https://github.com/wvlet/airframe/issues/2107)) [[10552510](https://github.com/wvlet/airframe/commit/10552510)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.45.1 ([#2105](https://github.com/wvlet/airframe/issues/2105)) [[bc3bd141](https://github.com/wvlet/airframe/commit/bc3bd141)]

## 22.3.0

This version is a maintenance release for dependency updatates:

- Update sbt-mdoc to 2.3.2 ([#2101](https://github.com/wvlet/airframe/issues/2101)) [[7ba56b97](https://github.com/wvlet/airframe/commit/7ba56b97)]
- Update jmh-core, jmh-generator-bytecode, ... to 1.35 ([#2102](https://github.com/wvlet/airframe/issues/2102)) [[78afcd9c](https://github.com/wvlet/airframe/commit/78afcd9c)]
- Update trino-main to 375 ([#2103](https://github.com/wvlet/airframe/issues/2103)) [[346d3bd0](https://github.com/wvlet/airframe/commit/346d3bd0)]
- Update finagle-core, finagle-http, ... to 22.3.0 ([#2104](https://github.com/wvlet/airframe/issues/2104)) [[3bbbc469](https://github.com/wvlet/airframe/commit/3bbbc469)]
- Revert "Update sbt-sonatype to 3.9.12 ([#2082](https://github.com/wvlet/airframe/issues/2082))" ([#2100](https://github.com/wvlet/airframe/issues/2100)) [[e69eb9f5](https://github.com/wvlet/airframe/commit/e69eb9f5)]
- Update scala-collection-compat to 2.7.0 ([#2099](https://github.com/wvlet/airframe/issues/2099)) [[87592f2d](https://github.com/wvlet/airframe/commit/87592f2d)]
- Update sbt-scalajs-crossproject to 1.2.0 ([#2098](https://github.com/wvlet/airframe/issues/2098)) [[dedbd5d4](https://github.com/wvlet/airframe/commit/dedbd5d4)]
- Update trino-main to 374 ([#2097](https://github.com/wvlet/airframe/issues/2097)) [[18eb8b5d](https://github.com/wvlet/airframe/commit/18eb8b5d)]
- Update swagger-parser to 2.0.31 ([#2096](https://github.com/wvlet/airframe/issues/2096)) [[ac6374f0](https://github.com/wvlet/airframe/commit/ac6374f0)]
- Update trino-main to 373 ([#2095](https://github.com/wvlet/airframe/issues/2095)) [[7f8632c6](https://github.com/wvlet/airframe/commit/7f8632c6)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.45.0 ([#2094](https://github.com/wvlet/airframe/issues/2094)) [[54608379](https://github.com/wvlet/airframe/commit/54608379)]
- Update msgpack-core to 0.9.1 ([#2093](https://github.com/wvlet/airframe/issues/2093)) [[61701722](https://github.com/wvlet/airframe/commit/61701722)]
- Update hadoop-aws, hadoop-client to 3.3.2 ([#2091](https://github.com/wvlet/airframe/issues/2091)) [[c071146c](https://github.com/wvlet/airframe/commit/c071146c)]
- Update logback-core to 1.2.11 ([#2092](https://github.com/wvlet/airframe/issues/2092)) [[91e02e26](https://github.com/wvlet/airframe/commit/91e02e26)]
- Update trino-main to 372 ([#2090](https://github.com/wvlet/airframe/issues/2090)) [[d45ceb4e](https://github.com/wvlet/airframe/commit/d45ceb4e)]
- Update sbt-mdoc to 2.3.1 ([#2088](https://github.com/wvlet/airframe/issues/2088)) [[beeefc43](https://github.com/wvlet/airframe/commit/beeefc43)]
- Update finagle-core, finagle-http, ... to 22.2.0 ([#2089](https://github.com/wvlet/airframe/issues/2089)) [[0af461bd](https://github.com/wvlet/airframe/commit/0af461bd)]
- Slow down awssdk update ([#2087](https://github.com/wvlet/airframe/issues/2087)) [[09a66a29](https://github.com/wvlet/airframe/commit/09a66a29)]
- Update sbt-sonatype to 3.9.12 ([#2082](https://github.com/wvlet/airframe/issues/2082)) [[105d1a70](https://github.com/wvlet/airframe/commit/105d1a70)]
- Update scala-parser-combinators to 2.1.1 ([#2084](https://github.com/wvlet/airframe/issues/2084)) [[8cd1e9a3](https://github.com/wvlet/airframe/commit/8cd1e9a3)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.44.1 ([#2080](https://github.com/wvlet/airframe/issues/2080)) [[7357cd76](https://github.com/wvlet/airframe/commit/7357cd76)]
- Update airspec, sbt-airframe to 22.2.0 ([#2081](https://github.com/wvlet/airframe/issues/2081)) [[c78698c0](https://github.com/wvlet/airframe/commit/c78698c0)]

## 22.2.0

This version upgrades Scala.js vertion to 1.9.0, and improves support for Scala 3

- Update sbt-scalajs, scalajs-compiler, ... to 1.9.0 ([#2071](https://github.com/wvlet/airframe/issues/2071)) [[78035500](https://github.com/wvlet/airframe/commit/78035500)]

### Bug Fixes

- airframe-metrics: Support positive base duration in TimeWindow ([#2079](https://github.com/wvlet/airframe/issues/2079)) [[40a31711](https://github.com/wvlet/airframe/commit/40a31711)]
- airframe-surface: Handle case fields as method arguments in Scala 3 ([#2051](https://github.com/wvlet/airframe/issues/2051)) [[93635586](https://github.com/wvlet/airframe/commit/93635586)]
- airframe-surface: Scala 3: Pass all tests on JVM ([#2052](https://github.com/wvlet/airframe/issues/2052)) [[7b0d96cc](https://github.com/wvlet/airframe/commit/7b0d96cc)]

### Internal Updates

- [build] Make it easier on cross-build while working on Scala 3 support ([#2056](https://github.com/wvlet/airframe/issues/2056)) [[74127800](https://github.com/wvlet/airframe/commit/74127800)]
- Use munit temporarily to avoid airspec <-> surface circular reference ([#2049](https://github.com/wvlet/airframe/issues/2049)) [[c8173e5e](https://github.com/wvlet/airframe/commit/c8173e5e)]
- Fix sonatype settings for AirSpec releases [[1c54fc61](https://github.com/wvlet/airframe/commit/1c54fc61)]
- Update sbt-buildinfo to 0.11.0 ([#2077](https://github.com/wvlet/airframe/issues/2077)) [[82946fb6](https://github.com/wvlet/airframe/commit/82946fb6)]
- Update trino-main to 371 ([#2076](https://github.com/wvlet/airframe/issues/2076)) [[cfd4cf13](https://github.com/wvlet/airframe/commit/cfd4cf13)]
- Update postgresql to 42.3.3 ([#2074](https://github.com/wvlet/airframe/issues/2074)) [[d6cd3d16](https://github.com/wvlet/airframe/commit/d6cd3d16)]
- Update scalajs-test-interface to 1.9.0 ([#2072](https://github.com/wvlet/airframe/issues/2072)) [[ca683c6d](https://github.com/wvlet/airframe/commit/ca683c6d)]
- Update scalafmt-core to 3.4.3 ([#2070](https://github.com/wvlet/airframe/issues/2070)) [[3bc98c9e](https://github.com/wvlet/airframe/commit/3bc98c9e)]
- Update sbt-sonatype to 3.9.11 ([#2066](https://github.com/wvlet/airframe/issues/2066)) [[adbb58fb](https://github.com/wvlet/airframe/commit/adbb58fb)]
- Update swagger-parser to 2.0.30 ([#2062](https://github.com/wvlet/airframe/issues/2062)) [[b71f86ca](https://github.com/wvlet/airframe/commit/b71f86ca)]
- Update slf4j-jdk14 to 1.7.36 ([#2064](https://github.com/wvlet/airframe/issues/2064)) [[eed963fe](https://github.com/wvlet/airframe/commit/eed963fe)]
- Update scalafmt-core to 3.4.2 ([#2061](https://github.com/wvlet/airframe/issues/2061)) [[5f439149](https://github.com/wvlet/airframe/commit/5f439149)]
- Update scalafmt-core to 3.4.1 ([#2060](https://github.com/wvlet/airframe/issues/2060)) [[468457be](https://github.com/wvlet/airframe/commit/468457be)]
- Fix Scala deprecations as much as possible ([#2058](https://github.com/wvlet/airframe/issues/2058)) [[06ad0533](https://github.com/wvlet/airframe/commit/06ad0533)]
- Update trino-main to 370 ([#2057](https://github.com/wvlet/airframe/issues/2057)) [[b4e91839](https://github.com/wvlet/airframe/commit/b4e91839)]
- Update auth to 2.17.123 ([#2054](https://github.com/wvlet/airframe/issues/2054)) [[057240bf](https://github.com/wvlet/airframe/commit/057240bf)]
- Upgrade to Scala 3.1.1 ([#2055](https://github.com/wvlet/airframe/issues/2055)) [[8b6abcd8](https://github.com/wvlet/airframe/commit/8b6abcd8)]
- Update fluency-core, fluency-fluentd, ... to 2.6.3 ([#2053](https://github.com/wvlet/airframe/issues/2053)) [[2d4061d6](https://github.com/wvlet/airframe/commit/2d4061d6)]
- Update auth to 2.17.122 ([#2050](https://github.com/wvlet/airframe/issues/2050)) [[a9b5c923](https://github.com/wvlet/airframe/commit/a9b5c923)]
- Update auth to 2.17.121 ([#2041](https://github.com/wvlet/airframe/issues/2041)) [[7de415c1](https://github.com/wvlet/airframe/commit/7de415c1)]
- Update postgresql to 42.3.2 ([#2046](https://github.com/wvlet/airframe/issues/2046)) [[e65bd716](https://github.com/wvlet/airframe/commit/e65bd716)]
- Update sbt-airframe to 22.1.0 [[a1eb5625](https://github.com/wvlet/airframe/commit/a1eb5625)]
- Update sbt, sbt-dependency-tree, ... to 1.6.2 [[00749a97](https://github.com/wvlet/airframe/commit/00749a97)]
- Update airspec to 22.1.0 ([#2045](https://github.com/wvlet/airframe/issues/2045)) [[04ddd95c](https://github.com/wvlet/airframe/commit/04ddd95c)]

## 22.1.0

This version introduces type-safe event handlers (e.g., `onclick -> { e: MouseEvent => ... }`) for airframe-rx-html. Thanks [@exoego](https://github.com/exoego) for the contribution!

- airframe-rx-html: Add type-safe event handler ([#2044](https://github.com/wvlet/airframe/issues/2044)) [[9b23ccc7](https://github.com/wvlet/airframe/commit/9b23ccc7)]

From this version, AirSpec will be built using a sub folder of Airframe project. This is for simplyfing the build definition of Airframe. 

- Build AirSpec using a sub project ([#1992](https://github.com/wvlet/airframe/issues/1992)) [[9994d212](https://github.com/wvlet/airframe/commit/9994d212)]

### Minor fixes

- Share same scalafmt config between airframe and airspec ([#2043](https://github.com/wvlet/airframe/issues/2043)) [[bad8bcc5](https://github.com/wvlet/airframe/commit/bad8bcc5)]
- Bump scalafmt and fix format ([#2038](https://github.com/wvlet/airframe/issues/2038)) [[606b4c1a](https://github.com/wvlet/airframe/commit/606b4c1a)]

### Major dependency updates 

- Update grpc-netty-shaded, grpc-protobuf, ... to 1.44.0 ([#2036](https://github.com/wvlet/airframe/issues/2036)) [[e2bc8dc4](https://github.com/wvlet/airframe/commit/e2bc8dc4)]
- Update fluency-core, fluency-fluentd, ... to 2.6.2 ([#2032](https://github.com/wvlet/airframe/issues/2032)) [[0734f2da](https://github.com/wvlet/airframe/commit/0734f2da)]
- Update finagle-core, finagle-http, ... to 22.1.0 ([#2017](https://github.com/wvlet/airframe/issues/2017)) [[0a6c6b88](https://github.com/wvlet/airframe/commit/0a6c6b88)]
- Update scala-compiler, scala-library, ... to 2.13.8 ([#2014](https://github.com/wvlet/airframe/issues/2014)) [[8595eec7](https://github.com/wvlet/airframe/commit/8595eec7)]
- Update HikariCP to 5.0.1 ([#2007](https://github.com/wvlet/airframe/issues/2007)) [[872b3bbe](https://github.com/wvlet/airframe/commit/872b3bbe)]
- Update scalajs-dom to 2.1.0 ([#1997](https://github.com/wvlet/airframe/issues/1997)) [[e61c8832](https://github.com/wvlet/airframe/commit/e61c8832)]
- Update logback-core to 1.2.10 ([#1989](https://github.com/wvlet/airframe/issues/1989)) [[b473ee02](https://github.com/wvlet/airframe/commit/b473ee02)]
- Update swagger-parser to 2.0.29 ([#1983](https://github.com/wvlet/airframe/issues/1983)) [[ebdaba8d](https://github.com/wvlet/airframe/commit/ebdaba8d)]
- Update slf4j-jdk14 to 1.7.35 ([#2030](https://github.com/wvlet/airframe/issues/2030)) [[9654f1a0](https://github.com/wvlet/airframe/commit/9654f1a0)]


### Internal dependency updates

- Update sbt-mdoc to 2.3.0 ([#2040](https://github.com/wvlet/airframe/issues/2040)) [[bbd801fa](https://github.com/wvlet/airframe/commit/bbd801fa)]
- Update protobuf-java to 3.19.4 ([#2039](https://github.com/wvlet/airframe/issues/2039)) [[8489ce9d](https://github.com/wvlet/airframe/commit/8489ce9d)]
- Update spark-sql to 3.2.1 ([#2034](https://github.com/wvlet/airframe/issues/2034)) [[03ae35da](https://github.com/wvlet/airframe/commit/03ae35da)]
- Update trino-main to 369 ([#2029](https://github.com/wvlet/airframe/issues/2029)) [[fc3d4fd9](https://github.com/wvlet/airframe/commit/fc3d4fd9)]
- Update json4s-jackson to 4.0.4 ([#2027](https://github.com/wvlet/airframe/issues/2027)) [[01ba66e1](https://github.com/wvlet/airframe/commit/01ba66e1)]
- Update slf4j-jdk14 to 1.7.33 ([#2015](https://github.com/wvlet/airframe/issues/2015)) [[7a833b46](https://github.com/wvlet/airframe/commit/7a833b46)]
- Update trino-main to 368 ([#2012](https://github.com/wvlet/airframe/issues/2012)) [[073d07bf](https://github.com/wvlet/airframe/commit/073d07bf)]
- Update protobuf-java to 3.19.3 ([#2009](https://github.com/wvlet/airframe/issues/2009)) [[0b9d4c6e](https://github.com/wvlet/airframe/commit/0b9d4c6e)]
- Update sbt-scoverage to 1.9.3 ([#2010](https://github.com/wvlet/airframe/issues/2010)) [[06856913](https://github.com/wvlet/airframe/commit/06856913)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.43.2 ([#2003](https://github.com/wvlet/airframe/issues/2003)) [[890b9ff7](https://github.com/wvlet/airframe/commit/890b9ff7)]
- Update protobuf-java to 3.19.2 ([#2001](https://github.com/wvlet/airframe/issues/2001)) [[25a0ecc1](https://github.com/wvlet/airframe/commit/25a0ecc1)]
- Update scalafmt-core to 3.3.1 ([#1998](https://github.com/wvlet/airframe/issues/1998)) [[c252839d](https://github.com/wvlet/airframe/commit/c252839d)]
- Update sbt, sbt-dependency-tree, ... to 1.6.1 ([#1996](https://github.com/wvlet/airframe/issues/1996)) [[4437ee28](https://github.com/wvlet/airframe/commit/4437ee28)]
- Update scalafmt-core to 3.3.0 ([#1995](https://github.com/wvlet/airframe/issues/1995)) [[264e1c91](https://github.com/wvlet/airframe/commit/264e1c91)]
- Update sbt, sbt-dependency-tree, ... to 1.6.0 ([#1994](https://github.com/wvlet/airframe/issues/1994)) [[0f0211c0](https://github.com/wvlet/airframe/commit/0f0211c0)]
- Update sbt-scalafmt to 2.4.6 ([#1993](https://github.com/wvlet/airframe/issues/1993)) [[c7ff87c6](https://github.com/wvlet/airframe/commit/c7ff87c6)]
- Update scalafmt-core to 3.2.2 ([#1991](https://github.com/wvlet/airframe/issues/1991)) [[1ac32f06](https://github.com/wvlet/airframe/commit/1ac32f06)]
- Update jmh-core, jmh-generator-bytecode, ... to 1.34 ([#1990](https://github.com/wvlet/airframe/issues/1990)) [[9d96fc18](https://github.com/wvlet/airframe/commit/9d96fc18)]
- Update trino-main to 367 ([#1988](https://github.com/wvlet/airframe/issues/1988)) [[63b25e69](https://github.com/wvlet/airframe/commit/63b25e69)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.43.1 ([#1984](https://github.com/wvlet/airframe/issues/1984)) [[42a7a657](https://github.com/wvlet/airframe/commit/42a7a657)]
- Update sbt, sbt-dependency-tree, ... to 1.5.8 ([#1985](https://github.com/wvlet/airframe/issues/1985)) [[a38971d6](https://github.com/wvlet/airframe/commit/a38971d6)]
- Update logback-core to 1.2.9 ([#1982](https://github.com/wvlet/airframe/issues/1982)) [[c1f97537](https://github.com/wvlet/airframe/commit/c1f97537)]
- Update finagle-core, finagle-http, ... to 21.12.0 ([#1981](https://github.com/wvlet/airframe/issues/1981)) [[e66ab863](https://github.com/wvlet/airframe/commit/e66ab863)]
- Update snakeyaml to 1.30 ([#1979](https://github.com/wvlet/airframe/issues/1979)) [[5a8c615c](https://github.com/wvlet/airframe/commit/5a8c615c)]
- Update sbt, sbt-dependency-tree, ... to 1.5.7 ([#1978](https://github.com/wvlet/airframe/issues/1978)) [[5d62b17c](https://github.com/wvlet/airframe/commit/5d62b17c)]
- airframe-config: Upgrade snake-yaml to 1.29 for CVE-2017-18640([#1977](https://github.com/wvlet/airframe/issues/1977)) [[20fa080b](https://github.com/wvlet/airframe/commit/20fa080b)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.43.0 ([#1975](https://github.com/wvlet/airframe/issues/1975)) [[6502da7b](https://github.com/wvlet/airframe/commit/6502da7b)]
- Update trino-main to 366 ([#1976](https://github.com/wvlet/airframe/issues/1976)) [[eaa4665a](https://github.com/wvlet/airframe/commit/eaa4665a)]
- Update airframe-codec, airframe-control, ... to 21.12.1 ([#1974](https://github.com/wvlet/airframe/issues/1974)) [[e3ea36f6](https://github.com/wvlet/airframe/commit/e3ea36f6)]

## 21.12.1

This release is for upgrading to Scala.js 1.8.0 and using a safer version of logback-core 1.2.8, which removes JNDI/JDBC related codei [LOGBACK-1591](https://jira.qos.ch/browse/LOGBACK-1591). Note that it's a different issue from the log4j vulnerability [CVE-2021-44228](https://nvd.nist.gov/vuln/detail/CVE-2021-44228). logback-core is used inside airframe-log for rotating log files and airframe-log doesn't use any logback configuration file, which can be a target of JNDI lookup security hole.  


- airframe-rx: Add MacroTask executor as a dependency ([#1972](https://github.com/wvlet/airframe/issues/1972)) [[8784b0ab](https://github.com/wvlet/airframe/commit/8784b0ab)]
- Update logback-core to 1.2.8 ([#1971](https://github.com/wvlet/airframe/issues/1971)) [[d6957f48](https://github.com/wvlet/airframe/commit/d6957f48)]
- Use MacrotaskExecutor  ([#1970](https://github.com/wvlet/airframe/issues/1970)) [[3b294ff4](https://github.com/wvlet/airframe/commit/3b294ff4)]
- Update scalajs-test-interface to 1.8.0 ([#1967](https://github.com/wvlet/airframe/issues/1967)) [[badec138](https://github.com/wvlet/airframe/commit/badec138)]
- Fix scalajs-dom related warnings ([#1968](https://github.com/wvlet/airframe/issues/1968)) [[b6467ec0](https://github.com/wvlet/airframe/commit/b6467ec0)]
- Remove unstable ULID tests because System.currentTimeMillis() can be rollbacked occasionally ([#1966](https://github.com/wvlet/airframe/issues/1966)) [[bff25ca7](https://github.com/wvlet/airframe/commit/bff25ca7)]
- Update sbt-scalajs, scalajs-compiler, ... to 1.8.0 ([#1965](https://github.com/wvlet/airframe/issues/1965)) [[edd480d4](https://github.com/wvlet/airframe/commit/edd480d4)]
- Fix 21.11.0 release note [[619c457d](https://github.com/wvlet/airframe/commit/619c457d)]
- Update sbt, sbt-dependency-tree, ... to 1.5.6 ([#1964](https://github.com/wvlet/airframe/issues/1964)) [[79fb0c3b](https://github.com/wvlet/airframe/commit/79fb0c3b)]
- Update trino-main to 365 ([#1958](https://github.com/wvlet/airframe/issues/1958)) [[b4971366](https://github.com/wvlet/airframe/commit/b4971366)]

## 21.12.0

- airframe-parquet: Fix nested Seq/Map writer ([#1952](https://github.com/wvlet/airframe/issues/1952)) [[3b703a14](https://github.com/wvlet/airframe/commit/3b703a14)]
- airframe-parquet: Fix nested object reader ([#1951](https://github.com/wvlet/airframe/issues/1951)) [[66000e04](https://github.com/wvlet/airframe/commit/66000e04)]
- Update auth to 2.17.96 ([#1957](https://github.com/wvlet/airframe/issues/1957)) [[99f5d376](https://github.com/wvlet/airframe/commit/99f5d376)]
- Update scalafmt-core to 3.2.1 ([#1955](https://github.com/wvlet/airframe/issues/1955)) [[e4f746bc](https://github.com/wvlet/airframe/commit/e4f746bc)]
- Update sbt-scalafmt to 2.4.5 ([#1956](https://github.com/wvlet/airframe/issues/1956)) [[e1c872cc](https://github.com/wvlet/airframe/commit/e1c872cc)]
- Update logback-core to 1.2.7 ([#1928](https://github.com/wvlet/airframe/issues/1928)) [[ab186cce](https://github.com/wvlet/airframe/commit/ab186cce)]

## 21.11.0

This version supports nested record read/write of Parquet files and includes major dependency updates (Scala, Scala.js, scalajs-dom, Finagle, okhttp, etc.)

### New Features
- airframe-parquet: Support nested schema ([#1917](https://github.com/wvlet/airframe/issues/1917)) [[d2ab8fad](https://github.com/wvlet/airframe/commit/d2ab8fad)]
- airframe-parquet: Parquet nested record support - M2 ([#1943](https://github.com/wvlet/airframe/issues/1943)) [[8b3c88bd](https://github.com/wvlet/airframe/commit/8b3c88bd)]

### Bug Fixes
- airspec: Fixes [#1845](https://github.com/wvlet/airframe/issues/1845) MISSING_DEPENDENCY for no-arg tests ([#1895](https://github.com/wvlet/airframe/issues/1895)) [[01792ff7](https://github.com/wvlet/airframe/commit/01792ff7)]
- airframe-http: Fixes [#1843](https://github.com/wvlet/airframe/issues/1843): Sanitize class names in Surface and http/RPC logs ([#1894](https://github.com/wvlet/airframe/issues/1894)) [[a1b68688](https://github.com/wvlet/airframe/commit/a1b68688)]
- airframe-http: Scala 3 support prep ([#1760](https://github.com/wvlet/airframe/issues/1760)) [[68322a4b](https://github.com/wvlet/airframe/commit/68322a4b)]
- airframe-http-recorder: Ignore pragma, cache-control header for request match ([#1892](https://github.com/wvlet/airframe/issues/1892)) [[2ac80e6e](https://github.com/wvlet/airframe/commit/2ac80e6e)]
- airframe-finagle: Handle ChannelClosedException sub classes ([#1891](https://github.com/wvlet/airframe/issues/1891)) [[15045eab](https://github.com/wvlet/airframe/commit/15045eab)]

### Major Dependency Updates
- Upgrade to Scala 3.1.0 ([#1882](https://github.com/wvlet/airframe/issues/1882)) [[75517719](https://github.com/wvlet/airframe/commit/75517719)]
- Update scalajs-dom to 2.0.0 ([#1888](https://github.com/wvlet/airframe/issues/1888)) [[819c95e2](https://github.com/wvlet/airframe/commit/819c95e2)]
- Update scalajs-compiler to 1.7.1 ([#1913](https://github.com/wvlet/airframe/issues/1913)) [[69fd17a8](https://github.com/wvlet/airframe/commit/69fd17a8)]
- Update finagle-core, finagle-http, ... to 21.11.0 ([#1944](https://github.com/wvlet/airframe/issues/1944)) [[125bf365](https://github.com/wvlet/airframe/commit/125bf365)]
- Update okhttp to 4.9.3 ([#1939](https://github.com/wvlet/airframe/issues/1939)) [[faf33ec4](https://github.com/wvlet/airframe/commit/faf33ec4)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.42.1 ([#1930](https://github.com/wvlet/airframe/issues/1930)) [[ced33fe4](https://github.com/wvlet/airframe/commit/ced33fe4)]
- Update scala-compiler, scala-library, ... to 2.13.7 ([#1908](https://github.com/wvlet/airframe/issues/1908)) [[e8dead6d](https://github.com/wvlet/airframe/commit/e8dead6d)]

### Internal Dependency Updates
- Update scalafmt-core to 3.2.0 ([#1947](https://github.com/wvlet/airframe/issues/1947)) [[c73981c6](https://github.com/wvlet/airframe/commit/c73981c6)]
- Update sbt-scalafmt to 2.4.4 ([#1936](https://github.com/wvlet/airframe/issues/1936)) [[26398399](https://github.com/wvlet/airframe/commit/26398399)]
- Update scala-collection-compat to 2.6.0 ([#1924](https://github.com/wvlet/airframe/issues/1924)) [[fc910bc9](https://github.com/wvlet/airframe/commit/fc910bc9)]
- Update antlr4, antlr4-runtime to 4.9.3 ([#1922](https://github.com/wvlet/airframe/issues/1922)) [[fbd78ad8](https://github.com/wvlet/airframe/commit/fbd78ad8)]
- [#1824](https://github.com/wvlet/airframe/issues/1824): Manage sample project dependencies with Scala steward ([#1916](https://github.com/wvlet/airframe/issues/1916)) [[1be61391](https://github.com/wvlet/airframe/commit/1be61391)]
- airspec: Add note for `test(...) { (function) }` issue [#1681](https://github.com/wvlet/airframe/issues/1681) ([#1915](https://github.com/wvlet/airframe/issues/1915)) [[6bb846eb](https://github.com/wvlet/airframe/commit/6bb846eb)]
- Update sbt-scoverage to 1.9.2 ([#1911](https://github.com/wvlet/airframe/issues/1911)) [[88905c40](https://github.com/wvlet/airframe/commit/88905c40)]
- Update trino-main to 364 ([#1909](https://github.com/wvlet/airframe/issues/1909)) [[14111818](https://github.com/wvlet/airframe/commit/14111818)]
- Update postgresql to 42.3.1 ([#1904](https://github.com/wvlet/airframe/issues/1904)) [[082689d3](https://github.com/wvlet/airframe/commit/082689d3)]
- Update protobuf-java to 3.19.1 ([#1903](https://github.com/wvlet/airframe/issues/1903)) [[037d6b56](https://github.com/wvlet/airframe/commit/037d6b56)]
- Update rpc examples ([#1901](https://github.com/wvlet/airframe/issues/1901)) [[b68ba769](https://github.com/wvlet/airframe/commit/b68ba769)]
- Add gallery demo ([#1900](https://github.com/wvlet/airframe/issues/1900)) [[b5655fc6](https://github.com/wvlet/airframe/commit/b5655fc6)]
- Update example ([#1899](https://github.com/wvlet/airframe/issues/1899)) [[8a0ac479](https://github.com/wvlet/airframe/commit/8a0ac479)]
- Add action-junit-report ([#1889](https://github.com/wvlet/airframe/issues/1889)) [[66656835](https://github.com/wvlet/airframe/commit/66656835)]
- Update protobuf-java to 3.19.0 ([#1886](https://github.com/wvlet/airframe/issues/1886)) [[f88437cc](https://github.com/wvlet/airframe/commit/f88437cc)]
- Update sbt-mdoc to 2.2.24 ([#1884](https://github.com/wvlet/airframe/issues/1884)) [[f8e6ecd5](https://github.com/wvlet/airframe/commit/f8e6ecd5)]
- Update scalafmt-core to 3.0.7 ([#1885](https://github.com/wvlet/airframe/issues/1885)) [[bd96684f](https://github.com/wvlet/airframe/commit/bd96684f)]
- Update postgresql to 42.3.0 ([#1880](https://github.com/wvlet/airframe/issues/1880)) [[b9a51438](https://github.com/wvlet/airframe/commit/b9a51438)]
- Update airframe-codec, airframe-control, ... to 21.10.0 ([#1877](https://github.com/wvlet/airframe/issues/1877)) [[6bb326b8](https://github.com/wvlet/airframe/commit/6bb326b8)]

## 21.10.0

This is a maintainance release with dependency updates. 
- JDK17 support
- Scala 2.12.15 support
- Scala.js 1.7.1 support 

### Dependency updates

- Update spark-sql to 3.2.0 ([#1874](https://github.com/wvlet/airframe/issues/1874)) [[609081694](https://github.com/wvlet/airframe/commit/609081694)]
- Update parquet-avro, parquet-hadoop to 1.12.2 ([#1866](https://github.com/wvlet/airframe/issues/1866)) [[9b7e8df58](https://github.com/wvlet/airframe/commit/9b7e8df58)]
- Update trino-main to 363 ([#1868](https://github.com/wvlet/airframe/issues/1868)) [[cdd767311](https://github.com/wvlet/airframe/commit/cdd767311)]
- Update sbt-scalajs, scalajs-compiler, ... to 1.7.1 ([#1867](https://github.com/wvlet/airframe/issues/1867)) [[5d58e09a4](https://github.com/wvlet/airframe/commit/5d58e09a4)]
- Update sbt-scoverage to 1.9.1 ([#1872](https://github.com/wvlet/airframe/issues/1872)) [[275e69ecd](https://github.com/wvlet/airframe/commit/275e69ecd)]
- Update protobuf-java to 3.18.1 ([#1864](https://github.com/wvlet/airframe/issues/1864)) [[4b048f224](https://github.com/wvlet/airframe/commit/4b048f224)]
- Update scalafmt-core to 3.0.6 ([#1862](https://github.com/wvlet/airframe/issues/1862)) [[4d75e297e](https://github.com/wvlet/airframe/commit/4d75e297e)]
- Update auth to 2.17.50 ([#1858](https://github.com/wvlet/airframe/issues/1858)) [[a5747205f](https://github.com/wvlet/airframe/commit/a5747205f)]
- Update scala-parallel-collections to 1.0.4 ([#1854](https://github.com/wvlet/airframe/issues/1854)) [[804a01858](https://github.com/wvlet/airframe/commit/804a01858)]
- Update swagger-parser to 2.0.28 ([#1857](https://github.com/wvlet/airframe/issues/1857)) [[1a627d2fe](https://github.com/wvlet/airframe/commit/1a627d2fe)]
- Update finagle-core, finagle-http, ... to 21.9.0 ([#1859](https://github.com/wvlet/airframe/issues/1859)) [[f5aa039dc](https://github.com/wvlet/airframe/commit/f5aa039dc)]
- Update scala-parser-combinators to 2.1.0 ([#1853](https://github.com/wvlet/airframe/issues/1853)) [[19f0689f1](https://github.com/wvlet/airframe/commit/19f0689f1)]
- Update scalafmt-core to 3.0.5 ([#1851](https://github.com/wvlet/airframe/issues/1851)) [[f72f26613](https://github.com/wvlet/airframe/commit/f72f26613)]
- Update postgresql to 42.2.24 ([#1848](https://github.com/wvlet/airframe/issues/1848)) [[d5e055359](https://github.com/wvlet/airframe/commit/d5e055359)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.41.0 ([#1846](https://github.com/wvlet/airframe/issues/1846)) [[0583a8869](https://github.com/wvlet/airframe/commit/0583a8869)]
- Update trino-main to 362 ([#1842](https://github.com/wvlet/airframe/issues/1842)) [[f33e5a716](https://github.com/wvlet/airframe/commit/f33e5a716)]
- Test against JDK17 ([#1841](https://github.com/wvlet/airframe/issues/1841)) [[38b544ca9](https://github.com/wvlet/airframe/commit/38b544ca9)]
- Update scalafmt-core to 3.0.4 ([#1839](https://github.com/wvlet/airframe/issues/1839)) [[969bff0d9](https://github.com/wvlet/airframe/commit/969bff0d9)]
- Update sbt-scoverage to 1.9.0 ([#1836](https://github.com/wvlet/airframe/issues/1836)) [[84a57c362](https://github.com/wvlet/airframe/commit/84a57c362)]
- Update logback-core to 1.2.6 ([#1827](https://github.com/wvlet/airframe/issues/1827)) [[4c429245d](https://github.com/wvlet/airframe/commit/4c429245d)]
- Update parquet-avro, parquet-hadoop to 1.12.1 ([#1834](https://github.com/wvlet/airframe/issues/1834)) [[037b01166](https://github.com/wvlet/airframe/commit/037b01166)]
- Update protobuf-java to 3.18.0 ([#1835](https://github.com/wvlet/airframe/issues/1835)) [[9eed688d3](https://github.com/wvlet/airframe/commit/9eed688d3)]
- Update Scala to 2.12.15 ([#1832](https://github.com/wvlet/airframe/issues/1832)) [[8b97fe6e1](https://github.com/wvlet/airframe/commit/8b97fe6e1)]
- Update scalafmt-core to 3.0.3 ([#1829](https://github.com/wvlet/airframe/issues/1829)) [[274a9c3fc](https://github.com/wvlet/airframe/commit/274a9c3fc)]


### Internal Changes

- sbt-airframe: Build as a sub-folder project ([#1823](https://github.com/wvlet/airframe/issues/1823)) [[bddec856d](https://github.com/wvlet/airframe/commit/bddec856d)]

## 21.9.0

This is mostly a dependency update release.

- airframe-http: Support customizing generated RPC client names ([#1821](https://github.com/wvlet/airframe/issues/1821)) [[f84a4bf14](https://github.com/wvlet/airframe/commit/f84a4bf14)]
- Update examples ([#1793](https://github.com/wvlet/airframe/issues/1793)) [[04e28a42e](https://github.com/wvlet/airframe/commit/04e28a42e)]
- Upgrade to Scala 3.0.1 ([#1794](https://github.com/wvlet/airframe/issues/1794)) [[2eb2bfcbe](https://github.com/wvlet/airframe/commit/2eb2bfcbe)]
- Update scalajs-dom to 1.2.0 ([#1806](https://github.com/wvlet/airframe/issues/1806)) [[7d6840cb7](https://github.com/wvlet/airframe/commit/7d6840cb7)]
- Update sqlite-jdbc to 3.36.0.3 ([#1812](https://github.com/wvlet/airframe/issues/1812)) [[b27f30ce8](https://github.com/wvlet/airframe/commit/b27f30ce8)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.40.1 ([#1802](https://github.com/wvlet/airframe/issues/1802)) [[8da9a7b78](https://github.com/wvlet/airframe/commit/8da9a7b78)]
- airframe-msgpack: Test tryUnpackNil at EOL ([#1787](https://github.com/wvlet/airframe/issues/1787)) [[f0bdf1489](https://github.com/wvlet/airframe/commit/f0bdf1489)]

Internal Dependency Updates:
- Upgrade to scalafmt-core 3.0.2 ([#1822](https://github.com/wvlet/airframe/issues/1822)) [[18bd9c5b8](https://github.com/wvlet/airframe/commit/18bd9c5b8)]
- Update auth to 2.17.34 ([#1819](https://github.com/wvlet/airframe/issues/1819)) [[bbca62f5f](https://github.com/wvlet/airframe/commit/bbca62f5f)]
- Update trino-main to 361 ([#1810](https://github.com/wvlet/airframe/issues/1810)) [[444623f9c](https://github.com/wvlet/airframe/commit/444623f9c)]
- Update sbt-mdoc to 2.2.23 ([#1808](https://github.com/wvlet/airframe/issues/1808)) [[cc5d7428f](https://github.com/wvlet/airframe/commit/cc5d7428f)]
- Update sbt-sonatype to 3.9.10 ([#1804](https://github.com/wvlet/airframe/issues/1804)) [[d5ca4f63c](https://github.com/wvlet/airframe/commit/d5ca4f63c)]

## 21.8.1

[airframe-parquet](https://wvlet.org/airframe/docs/airframe-parquet) now supports AWS S3, writing dynamic records, and reading column statistics.  
airframe-log added ThreadLogFormatter to show the current thread name in the log. This is suited to multi-thread applications.

### New Features

- airframe-parquet: Support S3 ([#1777](https://github.com/wvlet/airframe/issues/1777)) [[ed99dd086](https://github.com/wvlet/airframe/commit/ed99dd086)]
- airframe-parquet: Add record writer ([#1785](https://github.com/wvlet/airframe/issues/1785)) [[555960bf7](https://github.com/wvlet/airframe/commit/555960bf7)]
- airframe-parquet: Read column statistics ([#1784](https://github.com/wvlet/airframe/issues/1784)) [[cdd3f95fd](https://github.com/wvlet/airframe/commit/cdd3f95fd)]
- airframe-log: Support setting the root log level with `_root_` logger name ([#1788](https://github.com/wvlet/airframe/issues/1788)) [[7b44e4e9d](https://github.com/wvlet/airframe/commit/7b44e4e9d)]
- airframe-log: Add ThreadLogFormatter ([#1786](https://github.com/wvlet/airframe/issues/1786)) [[53e3186b1](https://github.com/wvlet/airframe/commit/53e3186b1)]

### Bug Fixes

- airframe-surface: Give up building Surface for difficult types ([#1781](https://github.com/wvlet/airframe/issues/1781)) [[ec8ec7e0a](https://github.com/wvlet/airframe/commit/ec8ec7e0a)]
- airframe-http: Fixes [#1715](https://github.com/wvlet/airframe/issues/1715) for logging ULID RPC arg ([#1780](https://github.com/wvlet/airframe/issues/1780)) [[16a070152](https://github.com/wvlet/airframe/commit/16a070152)]

### Dependency Updates

- Upgrade to Finagle 21.8.0 ([#1778](https://github.com/wvlet/airframe/issues/1778)) [[00c9f27bd](https://github.com/wvlet/airframe/commit/00c9f27bd)]
- Update logback-core to 1.2.5 ([#1768](https://github.com/wvlet/airframe/issues/1768)) [[8d0e2da4e](https://github.com/wvlet/airframe/commit/8d0e2da4e)]
- Update sbt-sonatype to 3.9.9 ([#1790](https://github.com/wvlet/airframe/issues/1790)) [[bb7be3ab1](https://github.com/wvlet/airframe/commit/bb7be3ab1)]
- airframe-control: Remove scala-parser-combinators dependency ([#1789](https://github.com/wvlet/airframe/issues/1789)) [[6afc0a7c0](https://github.com/wvlet/airframe/commit/6afc0a7c0)]
- Update auth to 2.17.19 ([#1783](https://github.com/wvlet/airframe/issues/1783)) [[5f10fef39](https://github.com/wvlet/airframe/commit/5f10fef39)]
- Update jmh-core, jmh-generator-bytecode, ... to 1.33 ([#1775](https://github.com/wvlet/airframe/issues/1775)) [[0928cd435](https://github.com/wvlet/airframe/commit/0928cd435)]
- Fixes [#1752](https://github.com/wvlet/airframe/issues/1752): Simplify build settings ([#1779](https://github.com/wvlet/airframe/issues/1779)) [[851940945](https://github.com/wvlet/airframe/commit/851940945)]

## 21.8.0

Upgraded to [Scala.js 1.7.0](https://www.scala-js.org/news/2021/08/04/announcing-scalajs-1.7.0/), which fixed all of the known bugs in Scala.js 1.6.0.

Minor Fixes:

- airframe-http: Fixes [#1772](https://github.com/wvlet/airframe/issues/1772) Properly decode URL-encoded paths when scanning jar files ([#1773](https://github.com/wvlet/airframe/issues/1773)) [[9d7184df3](https://github.com/wvlet/airframe/commit/9d7184df3)]
- airframe-log: Fix formatStacktrace dropping last line ([#1759](https://github.com/wvlet/airframe/issues/1759)) [[a093436b3](https://github.com/wvlet/airframe/commit/a093436b3)]
- airframe-log: Add a workaround if System.err is replaced for logging ([#1756](https://github.com/wvlet/airframe/issues/1756)) [[3c5eed424](https://github.com/wvlet/airframe/commit/3c5eed424)]

Dependency Updates:

- Update sbt-scalajs, scalajs-compiler, ... to 1.7.0 ([#1771](https://github.com/wvlet/airframe/issues/1771)) [[0eda8c4c2](https://github.com/wvlet/airframe/commit/0eda8c4c2)]
- Update trino-main to 360 ([#1770](https://github.com/wvlet/airframe/issues/1770)) [[152c8bf6b](https://github.com/wvlet/airframe/commit/152c8bf6b)]
- Update json4s-jackson to 4.0.3 ([#1767](https://github.com/wvlet/airframe/issues/1767)) [[32abe607a](https://github.com/wvlet/airframe/commit/32abe607a)]
- Update slf4j-jdk14 to 1.7.32 ([#1764](https://github.com/wvlet/airframe/issues/1764)) [[1b32fec4b](https://github.com/wvlet/airframe/commit/1b32fec4b)]
- Update sbt-mdoc to 2.2.22 ([#1765](https://github.com/wvlet/airframe/issues/1765)) [[253621f34](https://github.com/wvlet/airframe/commit/253621f34)]
- Update HikariCP to 5.0.0 ([#1762](https://github.com/wvlet/airframe/issues/1762)) [[3e09bad26](https://github.com/wvlet/airframe/commit/3e09bad26)]
- Update sbt, sbt-dependency-tree to 1.5.5 ([#1761](https://github.com/wvlet/airframe/issues/1761)) [[38b6dbc32](https://github.com/wvlet/airframe/commit/38b6dbc32)]
- Update scala-collection-compat to 2.5.0 ([#1758](https://github.com/wvlet/airframe/issues/1758)) [[8c2615fb0](https://github.com/wvlet/airframe/commit/8c2615fb0)]
- Update sbt-scalafmt to 2.4.3 ([#1755](https://github.com/wvlet/airframe/issues/1755)) [[a0238ec53](https://github.com/wvlet/airframe/commit/a0238ec53)]
- Update sbt-pack to 0.14 ([#1753](https://github.com/wvlet/airframe/issues/1753)) [[4a5e2fe1e](https://github.com/wvlet/airframe/commit/4a5e2fe1e)]

## 21.7.0

This version adds support for JDK16 and uses Scala.js 1.6.0 for JS projects. 

Dependency updates:

- Update postgresql to 42.2.23 ([#1751](https://github.com/wvlet/airframe/issues/1751)) [[997e93df7](https://github.com/wvlet/airframe/commit/997e93df7)]
- Update sbt-scalajs-crossproject to 1.1.0 ([#1750](https://github.com/wvlet/airframe/issues/1750)) [[b9c6e0939](https://github.com/wvlet/airframe/commit/b9c6e0939)]
- Update slf4j-jdk14 to 1.7.31 ([#1738](https://github.com/wvlet/airframe/issues/1738)) [[a716ef106](https://github.com/wvlet/airframe/commit/a716ef106)]
- Update trino-main to 359 ([#1749](https://github.com/wvlet/airframe/issues/1749)) [[9aacaa958](https://github.com/wvlet/airframe/commit/9aacaa958)]
- Update fluency-core, fluency-fluentd, ... to 2.6.0 ([#1745](https://github.com/wvlet/airframe/issues/1745)) [[09bb1bb92](https://github.com/wvlet/airframe/commit/09bb1bb92)]
- Update sqlite-jdbc to 3.36.0.1 ([#1748](https://github.com/wvlet/airframe/issues/1748)) [[654f0d208](https://github.com/wvlet/airframe/commit/654f0d208)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.39.0 ([#1747](https://github.com/wvlet/airframe/issues/1747)) [[3da52dde2](https://github.com/wvlet/airframe/commit/3da52dde2)]
- Update json4s-jackson to 4.0.1 ([#1746](https://github.com/wvlet/airframe/issues/1746)) [[12c5465a2](https://github.com/wvlet/airframe/commit/12c5465a2)]
- Update swagger-parser to 2.0.27 ([#1744](https://github.com/wvlet/airframe/issues/1744)) [[4a0a708e9](https://github.com/wvlet/airframe/commit/4a0a708e9)]
- Update finagle-core, finagle-http, ... to 21.6.0 ([#1742](https://github.com/wvlet/airframe/issues/1742)) [[b8354e936](https://github.com/wvlet/airframe/commit/b8354e936)]
- JDK16 Support ([#1740](https://github.com/wvlet/airframe/issues/1740)) [[8dabb4757](https://github.com/wvlet/airframe/commit/8dabb4757)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.38.1 ([#1737](https://github.com/wvlet/airframe/issues/1737)) [[d1158ae61](https://github.com/wvlet/airframe/commit/d1158ae61)]
- Update hadoop-client to 3.3.1 ([#1735](https://github.com/wvlet/airframe/issues/1735)) [[b4006b33a](https://github.com/wvlet/airframe/commit/b4006b33a)]
- Update sbt, sbt-dependency-tree to 1.5.4 ([#1734](https://github.com/wvlet/airframe/issues/1734)) [[dc331483f](https://github.com/wvlet/airframe/commit/dc331483f)]
- Update sbt-scalajs, scalajs-library, ... to 1.6.0 ([#1728](https://github.com/wvlet/airframe/issues/1728)) [[0d6910562](https://github.com/wvlet/airframe/commit/0d6910562)]
- Update msgpack-core to 0.9.0 ([#1732](https://github.com/wvlet/airframe/issues/1732)) [[d3a5f47dd](https://github.com/wvlet/airframe/commit/d3a5f47dd)]
- Update snakeyaml to 1.29 ([#1731](https://github.com/wvlet/airframe/issues/1731)) [[e4e19fee7](https://github.com/wvlet/airframe/commit/e4e19fee7)]
- Remove airframe-ulid from the community build ([#1730](https://github.com/wvlet/airframe/issues/1730)) [[ba7285903](https://github.com/wvlet/airframe/commit/ba7285903)]
- Update protobuf-java to 3.17.3 ([#1729](https://github.com/wvlet/airframe/issues/1729)) [[58a9414cd](https://github.com/wvlet/airframe/commit/58a9414cd)]

## 21.6.0

This version is mostly a bug-fix release for Scala 3. To simplify the cross build settings with Scala 3, sbt-airframe becomes a [separate GitHub project](https://github.com/wvlet/sbt-airframe). 

### New Features

- airframe-ulid: Support non-secure random ULID generator ([#1722](https://github.com/wvlet/airframe/issues/1722)) [[0a5524d0d](https://github.com/wvlet/airframe/commit/0a5524d0d)]
- airframe-ulid: Make ULIDGenerator public ([#1721](https://github.com/wvlet/airframe/issues/1721)) [[31c179138](https://github.com/wvlet/airframe/commit/31c179138)]
- airframe-codec: Support reading JSON as an object field ([#1720](https://github.com/wvlet/airframe/issues/1720)) [[dc0f1eccd](https://github.com/wvlet/airframe/commit/dc0f1eccd)]
- airframe-grpc: Show the exception error message in RPC implementations ([#1716](https://github.com/wvlet/airframe/issues/1716)) [[78fcdda1c](https://github.com/wvlet/airframe/commit/78fcdda1c)]

### Minor Fixes

- airframe-http-recorder: Delete duplicated server creation methods ([#1703](https://github.com/wvlet/airframe/issues/1703)) [[8f186064b](https://github.com/wvlet/airframe/commit/8f186064b)]
- airframe-codec: Fix MapCodec to read nil as an empty Map ([#1717](https://github.com/wvlet/airframe/issues/1717)) [[e0fe0a63a](https://github.com/wvlet/airframe/commit/e0fe0a63a)]
- airframe-surface: Resolve type parameters for Scala 3 ([#1700](https://github.com/wvlet/airframe/issues/1700)) [[ae947249c](https://github.com/wvlet/airframe/commit/ae947249c)]
- airframe-codec: Fix tests for Scala 3 ([#1699](https://github.com/wvlet/airframe/issues/1699)) [[28e8df563](https://github.com/wvlet/airframe/commit/28e8df563)]
- airframe-surface: Support building abstract class impl for Scala 3 ([#1697](https://github.com/wvlet/airframe/issues/1697)) [[d5ae6ec4f](https://github.com/wvlet/airframe/commit/d5ae6ec4f)]
- airframe-surface: Support local class surfaces for Scala 3 ([#1695](https://github.com/wvlet/airframe/issues/1695)) [[69538969e](https://github.com/wvlet/airframe/commit/69538969e)]
- airframe-surface: Use compile-time surface for Scala 3 ([#1694](https://github.com/wvlet/airframe/issues/1694)) [[cdf921018](https://github.com/wvlet/airframe/commit/cdf921018)]
- airframe-surface: Support EnumSurface for Scala 3 ([#1693](https://github.com/wvlet/airframe/issues/1693)) [[6be492a4d](https://github.com/wvlet/airframe/commit/6be492a4d)]
- airframe-surface: Surface.ofClass(...) for Scala 3 ([#1692](https://github.com/wvlet/airframe/issues/1692)) [[d8ae38956](https://github.com/wvlet/airframe/commit/d8ae38956)]

### Internal Updates

- Extract sbt-airframe as a separate GitHub project ([#1686](https://github.com/wvlet/airframe/issues/1686)) [[cb238c85e](https://github.com/wvlet/airframe/commit/cb238c85e)]
- internal: Move airframe src to airframe-di ([#1689](https://github.com/wvlet/airframe/issues/1689)) [[41b796d1e](https://github.com/wvlet/airframe/commit/41b796d1e)]
- Update finagle-core, finagle-http, ... to 21.5.0 ([#1718](https://github.com/wvlet/airframe/issues/1718)) [[d647317b7](https://github.com/wvlet/airframe/commit/d647317b7)]
- Update protobuf-java to 3.17.2 ([#1727](https://github.com/wvlet/airframe/issues/1727)) [[793d54818](https://github.com/wvlet/airframe/commit/793d54818)]
- Update scala-ulid to 1.0.13 ([#1684](https://github.com/wvlet/airframe/issues/1684)) [[1408e43b6](https://github.com/wvlet/airframe/commit/1408e43b6)]
- Update trino-main to 358 ([#1726](https://github.com/wvlet/airframe/issues/1726)) [[5e76a977e](https://github.com/wvlet/airframe/commit/5e76a977e)]
- Add sbt-airframe integration test ([#1725](https://github.com/wvlet/airframe/issues/1725)) [[7dc5b345e](https://github.com/wvlet/airframe/commit/7dc5b345e)]
- Update sbt-jmh to 0.4.3 ([#1724](https://github.com/wvlet/airframe/issues/1724)) [[0315fd734](https://github.com/wvlet/airframe/commit/0315fd734)]
- Update sbt, sbt-dependency-tree to 1.5.3 ([#1723](https://github.com/wvlet/airframe/issues/1723)) [[08f6dab4d](https://github.com/wvlet/airframe/commit/08f6dab4d)]
- Update swagger-parser to 2.0.26 ([#1712](https://github.com/wvlet/airframe/issues/1712)) [[04b380219](https://github.com/wvlet/airframe/commit/04b380219)]
- Update sbt-scoverage to 1.8.2 ([#1713](https://github.com/wvlet/airframe/issues/1713)) [[e068e79cf](https://github.com/wvlet/airframe/commit/e068e79cf)]
- Update spark-sql to 3.1.2 ([#1707](https://github.com/wvlet/airframe/issues/1707)) [[f122dcdb8](https://github.com/wvlet/airframe/commit/f122dcdb8)]
- Update scala-compiler, scala-library, ... to 2.12.14 ([#1708](https://github.com/wvlet/airframe/issues/1708)) [[6dc75878a](https://github.com/wvlet/airframe/commit/6dc75878a)]
- Update json4s-jackson to 4.0.0 ([#1701](https://github.com/wvlet/airframe/issues/1701)) [[71d214d3e](https://github.com/wvlet/airframe/commit/71d214d3e)]
- Update jmh-core, jmh-generator-bytecode, ... to 1.32 ([#1706](https://github.com/wvlet/airframe/issues/1706)) [[509de959d](https://github.com/wvlet/airframe/commit/509de959d)]
- Update circe-parser to 0.14.1 ([#1705](https://github.com/wvlet/airframe/issues/1705)) [[3cbb4c1ca](https://github.com/wvlet/airframe/commit/3cbb4c1ca)]
- Update circe-parser to 0.14.0 ([#1702](https://github.com/wvlet/airframe/issues/1702)) [[cb40a85db](https://github.com/wvlet/airframe/commit/cb40a85db)]
- airframe-rx: More test coverage ([#1704](https://github.com/wvlet/airframe/issues/1704)) [[dec3219f5](https://github.com/wvlet/airframe/commit/dec3219f5)]
- Update protobuf-java to 3.17.1 ([#1696](https://github.com/wvlet/airframe/issues/1696)) [[b6815e8a9](https://github.com/wvlet/airframe/commit/b6815e8a9)]
- internal: Use raw AirSpec again to test internal modules ([#1691](https://github.com/wvlet/airframe/issues/1691)) [[aee66648b](https://github.com/wvlet/airframe/commit/aee66648b)]
- Use Scala 2.13 as the default Scala version ([#1690](https://github.com/wvlet/airframe/issues/1690)) [[0cdede246](https://github.com/wvlet/airframe/commit/0cdede246)]
- internal: Use crossBuild.pure setting for source folder location consistency ([#1688](https://github.com/wvlet/airframe/issues/1688)) [[e6a1e55ae](https://github.com/wvlet/airframe/commit/e6a1e55ae)]
- Update trino-main to 357 ([#1685](https://github.com/wvlet/airframe/issues/1685)) [[cf1b10dc6](https://github.com/wvlet/airframe/commit/cf1b10dc6)]
- Ignore frequent scala-ulid updates [[873712528](https://github.com/wvlet/airframe/commit/873712528)]
- Update sbt-scoverage to 1.8.1 ([#1683](https://github.com/wvlet/airframe/issues/1683)) [[72421877a](https://github.com/wvlet/airframe/commit/72421877a)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.38.0 ([#1682](https://github.com/wvlet/airframe/issues/1682)) [[06d5d0497](https://github.com/wvlet/airframe/commit/06d5d0497)]
- Update scala-compiler, scala-library, ... to 2.13.6 ([#1679](https://github.com/wvlet/airframe/issues/1679)) [[c36f70622](https://github.com/wvlet/airframe/commit/c36f70622)]
- Update msgpack-core to 0.8.24 ([#1678](https://github.com/wvlet/airframe/issues/1678)) [[99e91c997](https://github.com/wvlet/airframe/commit/99e91c997)]
- AirSpec: Scala.js + Scala 3.0.0 prep ([#1676](https://github.com/wvlet/airframe/issues/1676)) [[0b6861daa](https://github.com/wvlet/airframe/commit/0b6861daa)]
- Update airspec to 21.5.4 ([#1675](https://github.com/wvlet/airframe/issues/1675)) [[0e4ab52d8](https://github.com/wvlet/airframe/commit/0e4ab52d8)]

## 21.5.4

This is a minor bug fix release. We've found airframe-surface for Scala 3 still has several issues, which may cause errors in Airframe DI.

- airframe-di: Fixes macro-compilation errors by using only inline methods for Scala 3 ([#1673](https://github.com/wvlet/airframe/issues/1673)) [[db2cdf690](https://github.com/wvlet/airframe/commit/db2cdf690)]
- airspec: Show deprecation warning messages for scalaJSSupport usage.
- airframe-json: Deprecate JSON query (path / to).value syntax. Use (path / to).getValue instead becuase .value can be ambiguous in Scala 3. 
- internal: Migrating test codes for Scala 3 ([#1672](https://github.com/wvlet/airframe/issues/1672)) [[76943cfd0](https://github.com/wvlet/airframe/commit/76943cfd0)]

## 21.5.3

AirSpec, a testing library for Scala, now supports Scala 3.0.0! From this version, AirSpec only supports `test(...) { ... }` syntax, and deprecates using public functions as test cases. See also [AirSpec documentation](https://wvlet.org/airframe/docs/airspec).


- AirSpec: Scala 3 support ([#1671](https://github.com/wvlet/airframe/issues/1671)) [[f79c5e5ee](https://github.com/wvlet/airframe/commit/f79c5e5ee)]
- AirSpec: funspec deprecation ([#1664](https://github.com/wvlet/airframe/issues/1664)) [[93d2a60e8](https://github.com/wvlet/airframe/commit/93d2a60e8)]
- airframe-surface: Fix BigInt surface resolver ([#1669](https://github.com/wvlet/airframe/issues/1669)) [[0759856d2](https://github.com/wvlet/airframe/commit/0759856d2)]

## 21.5.2

This is a minor bug fix release for AirSpec and surfce.

- airframe-surface, codec: Add BigInt, BigInteger support ([#1666](https://github.com/wvlet/airframe/issues/1666)) [[8f99f5272](https://github.com/wvlet/airframe/commit/8f99f5272)]
- airframe-surface: Fix bitLength < 2 error ([#1667](https://github.com/wvlet/airframe/issues/1667)) [[d0fe7b1a7](https://github.com/wvlet/airframe/commit/d0fe7b1a7)]
- AirSpec requires scala-reflect for Scala 2.x ([#1665](https://github.com/wvlet/airframe/issues/1665)) [[58c931e86](https://github.com/wvlet/airframe/commit/58c931e86)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.37.1 ([#1652](https://github.com/wvlet/airframe/issues/1652)) [[8341a575d](https://github.com/wvlet/airframe/commit/8341a575d)]

## 21.5.1

### Major Changes

- From this version, Airframe DI for Scala 3.0.0 is available. For Scala 3, Airframe DI only supports constructor injection. We will deprecate in-trait injection support in upcoming versions. For more details, see also [airframe-di migraiton plan](https://github.com/wvlet/airframe/issues/1649).
- AirSpec deprecates function-style test specs and only uses `test("...") { ... }` syntax. This version shows warning messages if you are still using public functions as test cases. This test support will be removed from the next version of AirSpec. This is [a necessary step toward supporting Scala 3](https://github.com/wvlet/airframe/issues/1659).

- airframe-di: Scala 3 support (Phase 3) ([#1655](https://github.com/wvlet/airframe/issues/1655)) [[235025063](https://github.com/wvlet/airframe/commit/235025063)]
- airframe-di: Next generation DI ([#1653](https://github.com/wvlet/airframe/issues/1653)) [[1a5725800](https://github.com/wvlet/airframe/commit/1a5725800)]
- AirSpec: Show deprecation warning message to function spec ([#1662](https://github.com/wvlet/airframe/issues/1662)) [[92193db48](https://github.com/wvlet/airframe/commit/92193db48)]
 
### Internal Changes

- AirSpec: Remove function test syntax ([#1660](https://github.com/wvlet/airframe/issues/1660)) [[3353fb3e6](https://github.com/wvlet/airframe/commit/3353fb3e6)]
- Prepare Scala3 compat layer for AirSpec ([#1658](https://github.com/wvlet/airframe/issues/1658)) [[69ceb7ec2](https://github.com/wvlet/airframe/commit/69ceb7ec2)]
- Update sbt-mdoc to 2.2.21 ([#1650](https://github.com/wvlet/airframe/issues/1650)) [[ca402a6ad](https://github.com/wvlet/airframe/commit/ca402a6ad)]
- Update airspec to 21.5.0 ([#1651](https://github.com/wvlet/airframe/issues/1651)) [[b7bb400bd](https://github.com/wvlet/airframe/commit/b7bb400bd)]
- airframe-http: Remove dependency to airframe-di (Scala 3 support phase 3) ([#1648](https://github.com/wvlet/airframe/issues/1648)) [[8aa6f9c21](https://github.com/wvlet/airframe/commit/8aa6f9c21)]
- airframe-http: Split router as a separate module (Scala 3 support phase 2) ([#1647](https://github.com/wvlet/airframe/issues/1647)) [[cb5e79d85](https://github.com/wvlet/airframe/commit/cb5e79d85)]
- Publish Scala 3 artifact snapshots at CI [[a4080d981](https://github.com/wvlet/airframe/commit/a4080d981)]
- airframe-http: Scala 3 support (phase 1) ([#1643](https://github.com/wvlet/airframe/issues/1643)) [[cf2b37129](https://github.com/wvlet/airframe/commit/cf2b37129)]

## 21.5.0

### New Features

- Added [airframe-parquet](https://wvlet.org/airframe/docs/airframe-parquet), Parquet columnar file reader and writer. So that you can read and write Scala objects at ease in columnar format.
- Scala 3.0.0 support (Dropped 3.0.0-RC3 support).
  - Now airframe-codec also supports Scala 3.0.0.
  - See [Milestone: Scala 3 Support](https://github.com/wvlet/airframe/issues/1077) for the other Scala 3 migraiton work in progress


- airframe-parquet: [#1621](https://github.com/wvlet/airframe/issues/1621) Support is null and is not null ([#1624](https://github.com/wvlet/airframe/issues/1624)) [[a0b4fbd28](https://github.com/wvlet/airframe/commit/a0b4fbd28)]
- airframe-parquet: SQL query support ([#1620](https://github.com/wvlet/airframe/issues/1620)) [[87f79c128](https://github.com/wvlet/airframe/commit/87f79c128)]
- airframe-parquet: Support column projection ([#1617](https://github.com/wvlet/airframe/issues/1617)) [[15878d24a](https://github.com/wvlet/airframe/commit/15878d24a)]
- airframe-parquet: Support for row-group filter ([#1615](https://github.com/wvlet/airframe/issues/1615)) [[8381f44ae](https://github.com/wvlet/airframe/commit/8381f44ae)]
- airframe-parquet: Add Parquet format reader and writer ([#1613](https://github.com/wvlet/airframe/issues/1613)) [[1fb076162](https://github.com/wvlet/airframe/commit/1fb076162)]
- Upgrade to Scala 3.0.0 ([#1639](https://github.com/wvlet/airframe/issues/1639)) [[2e27fd3da](https://github.com/wvlet/airframe/commit/2e27fd3da)]
- airframe-codec: Scala 3 support ([#1640](https://github.com/wvlet/airframe/issues/1640)) [[038fcc9b6](https://github.com/wvlet/airframe/commit/038fcc9b6)]
- airframe-http-recorder: Add methods to dump HttpRecordStore as JSON and YAML ([#1622](https://github.com/wvlet/airframe/issues/1622)) [[9ed313c89](https://github.com/wvlet/airframe/commit/9ed313c89)]
- airframe-http-finagle: Update finagle-core, finagle-http, ... to 21.4.0 ([#1603](https://github.com/wvlet/airframe/issues/1603)) [[9b5c59670](https://github.com/wvlet/airframe/commit/9b5c59670)]

### Dependency Updates

- Update scala-collection-compat to 2.4.4 ([#1645](https://github.com/wvlet/airframe/issues/1645)) [[c8559e99d](https://github.com/wvlet/airframe/commit/c8559e99d)]
- Update scala-parallel-collections to 1.0.3 ([#1646](https://github.com/wvlet/airframe/issues/1646)) [[6612d15e5](https://github.com/wvlet/airframe/commit/6612d15e5)]
- Update msgpack-core to 0.8.23 ([#1632](https://github.com/wvlet/airframe/issues/1632)) [[8bf5d1405](https://github.com/wvlet/airframe/commit/8bf5d1405)]
- Update protobuf-java to 3.17.0 ([#1636](https://github.com/wvlet/airframe/issues/1636)) [[13e626854](https://github.com/wvlet/airframe/commit/13e626854)]
- Update sbt-jmh to 0.4.2 ([#1637](https://github.com/wvlet/airframe/issues/1637)) [[8b66ea7f4](https://github.com/wvlet/airframe/commit/8b66ea7f4)]
- Update jmh-core, jmh-generator-bytecode, ... to 1.31 ([#1634](https://github.com/wvlet/airframe/issues/1634)) [[716de222b](https://github.com/wvlet/airframe/commit/716de222b)]
- Update sbt-scoverage to 1.8.0 ([#1629](https://github.com/wvlet/airframe/issues/1629)) [[93a818fed](https://github.com/wvlet/airframe/commit/93a818fed)]
- Update sbt, sbt-dependency-tree, ... to 1.5.2 ([#1628](https://github.com/wvlet/airframe/issues/1628)) [[4e33b8046](https://github.com/wvlet/airframe/commit/4e33b8046)]
- Update scalacheck to 1.15.4 ([#1611](https://github.com/wvlet/airframe/issues/1611)) [[ff35edc01](https://github.com/wvlet/airframe/commit/ff35edc01)]
- Update trino-main to 356 ([#1605](https://github.com/wvlet/airframe/issues/1605)) [[a0f8fdd9c](https://github.com/wvlet/airframe/commit/a0f8fdd9c)]
- Update scala-ulid to 1.0.7 ([#1600](https://github.com/wvlet/airframe/issues/1600)) [[f226d4448](https://github.com/wvlet/airframe/commit/f226d4448)]
- Upgrade to Scala 2.12.13 ([#1495](https://github.com/wvlet/airframe/issues/1495)) [[f98b6b404](https://github.com/wvlet/airframe/commit/f98b6b404)]
- Update postgresql to 42.2.20 ([#1597](https://github.com/wvlet/airframe/issues/1597)) [[8b3d7a187](https://github.com/wvlet/airframe/commit/8b3d7a187)]
- Update sbt-mdoc to 2.2.20 ([#1593](https://github.com/wvlet/airframe/issues/1593)) [[ad79c2e78](https://github.com/wvlet/airframe/commit/ad79c2e78)]
- Update airspec to 21.4.1 ([#1592](https://github.com/wvlet/airframe/issues/1592)) [[f124b10bc](https://github.com/wvlet/airframe/commit/f124b10bc)]

## 21.4.1

Added Scala 3.0.0-RC3 support. This version has no functional difference with 21.4.0.

Now, the following 9 modules support Scala 3.0.0-RC3:
- airfarme-log
- airframe-surface
- airframe-canvas
- airframe-control
- airframe-metrics
- airframe-msgpack
- airframe-json
- airframe-rx
- airframe-ulid

Changes:
- Add more modules with Scala 3.0.0-RC3 support ([#1591](https://github.com/wvlet/airframe/issues/1591)) [[ca6c1fcd0](https://github.com/wvlet/airframe/commit/ca6c1fcd0)]
- Upgrade to Scala 3.0.0-RC3 ([#1590](https://github.com/wvlet/airframe/issues/1590)) [[ea7fc5600](https://github.com/wvlet/airframe/commit/ea7fc5600)]

Internal changes:
- Deprecate sbt 0.13 syntax ([#1589](https://github.com/wvlet/airframe/issues/1589)) [[c68c40675](https://github.com/wvlet/airframe/commit/c68c40675)]
- Update sbt, sbt-dependency-tree, ... to 1.5.0 ([#1577](https://github.com/wvlet/airframe/issues/1577)) [[2331a5294](https://github.com/wvlet/airframe/commit/2331a5294)]
- Update airspec to 21.4.0 ([#1588](https://github.com/wvlet/airframe/issues/1588)) [[5579dad3b](https://github.com/wvlet/airframe/commit/5579dad3b)]

## 21.4.0

### New Features 

- [airframe-ulid](https://wvlet.org/airframe/docs/airframe-ulid) (ULID generator for Scala) is now an independent module that can be used as a standalone library for Scala and Scala.js. With our optimizaiton effort, airframe-ulid becomes the fastest ULID generator for Scala and can produce more than 5 million ULIDs per second.


- airframe-ulid: Performance optimization ([#1586](https://github.com/wvlet/airframe/issues/1586)) [[28a763371](https://github.com/wvlet/airframe/commit/28a763371)]
- airframe-ulid: Extract ULID as a new module ([#1585](https://github.com/wvlet/airframe/issues/1585)) [[1c8574610](https://github.com/wvlet/airframe/commit/1c8574610)]
- airframe-rx: Add Rx.timer/delay ([#1583](https://github.com/wvlet/airframe/issues/1583)) [[49158a183](https://github.com/wvlet/airframe/commit/49158a183)]
- airframe-rx-widget: Add GoogleAuth.getCurrentUser: Option[GoogleAuthProfile] ([#1576](https://github.com/wvlet/airframe/issues/1576)) [[dfa27cb91](https://github.com/wvlet/airframe/commit/dfa27cb91)]
- airframe-rx: Add Rx.cache.getCurrent ([#1572](https://github.com/wvlet/airframe/issues/1572)) [[6fa646866](https://github.com/wvlet/airframe/commit/6fa646866)]
- airframe-rx: Add Rx.startWith ([#1571](https://github.com/wvlet/airframe/issues/1571)) [[b46be2a94](https://github.com/wvlet/airframe/commit/b46be2a94)]
- airframe-rx: Add RxVar.setException(Throwable) ([#1556](https://github.com/wvlet/airframe/issues/1556)) [[7b360c70d](https://github.com/wvlet/airframe/commit/7b360c70d)]
- airframe-grpc: Use ForkJoinPool with nCPU x 2 max threads by default to handle concurrent requests ([#1557](https://github.com/wvlet/airframe/issues/1557)) [[bc86c269d](https://github.com/wvlet/airframe/commit/bc86c269d)]
- airframe-http: Discourage using HttpRequest[x] adapter in the endpoint definitions [[a015acc87](https://github.com/wvlet/airframe/commit/a015acc87)]
- sbt-airframe: Generate ServiceJSClientRx for Scala.js + RPC + Rx ([#1570](https://github.com/wvlet/airframe/issues/1570)) [[093be4948](https://github.com/wvlet/airframe/commit/093be4948)]


### Dependency Updates

- Update sbt-scalajs, scalajs-compiler, ... to 1.5.1 ([#1569](https://github.com/wvlet/airframe/issues/1569)) [[af51f916a](https://github.com/wvlet/airframe/commit/af51f916a)]
- Update finagle-core, finagle-http, ... to 21.3.0 ([#1562](https://github.com/wvlet/airframe/issues/1562)) [[1b5634a5c](https://github.com/wvlet/airframe/commit/1b5634a5c)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.37.0 ([#1579](https://github.com/wvlet/airframe/issues/1579)) [[e740578a1](https://github.com/wvlet/airframe/commit/e740578a1)]
- Update protobuf-java to 3.15.8 ([#1580](https://github.com/wvlet/airframe/issues/1580)) [[e20a4ea26](https://github.com/wvlet/airframe/commit/e20a4ea26)]
- Update scala-collection-compat to 2.4.3 ([#1563](https://github.com/wvlet/airframe/issues/1563)) [[bb8ba8092](https://github.com/wvlet/airframe/commit/bb8ba8092)]
- Update scala-parallel-collections to 1.0.2 ([#1564](https://github.com/wvlet/airframe/issues/1564)) [[0c92f2b51](https://github.com/wvlet/airframe/commit/0c92f2b51)]

### Bug Fixes

- airframe-http: Add a proper retry wait to JSHttpClient ([#1561](https://github.com/wvlet/airframe/issues/1561)) [[0e0c27690](https://github.com/wvlet/airframe/commit/0e0c27690)]
- airframe-rx: Lazily evaluate Cancelable.merge ([#1582](https://github.com/wvlet/airframe/issues/1582)) [[d4ba91b53](https://github.com/wvlet/airframe/commit/d4ba91b53)]

### Internal Updates

- Update trino-main to 355 ([#1581](https://github.com/wvlet/airframe/issues/1581)) [[cd5ef09f2](https://github.com/wvlet/airframe/commit/cd5ef09f2)]
- Update swagger-parser to 2.0.25 ([#1578](https://github.com/wvlet/airframe/issues/1578)) [[0a5b08027](https://github.com/wvlet/airframe/commit/0a5b08027)]
- rx-html: Add multiple attribute update tests ([#1574](https://github.com/wvlet/airframe/issues/1574)) [[b432ce8f5](https://github.com/wvlet/airframe/commit/b432ce8f5)]
- rx-html: Add tests for attribute updates [#1341](https://github.com/wvlet/airframe/issues/1341) ([#1573](https://github.com/wvlet/airframe/issues/1573)) [[fe26b2b43](https://github.com/wvlet/airframe/commit/fe26b2b43)]
- Update sbt-dotty to 0.5.4 ([#1566](https://github.com/wvlet/airframe/issues/1566)) [[b3c0b5111](https://github.com/wvlet/airframe/commit/b3c0b5111)]
- Add missing args in airframe-codec docs ([#1565](https://github.com/wvlet/airframe/issues/1565)) [[dbc21dfb9](https://github.com/wvlet/airframe/commit/dbc21dfb9)]
- Update sbt-mdoc to 2.2.19 ([#1560](https://github.com/wvlet/airframe/issues/1560)) [[b7582c5ee](https://github.com/wvlet/airframe/commit/b7582c5ee)]
- rx-html: Add an example for avoiding RxStream[Object] resolution ([#1558](https://github.com/wvlet/airframe/issues/1558)) [[8b3e1bdd3](https://github.com/wvlet/airframe/commit/8b3e1bdd3)]
- Update airspec to 21.3.1 ([#1553](https://github.com/wvlet/airframe/issues/1553)) [[31ad55fe9](https://github.com/wvlet/airframe/commit/31ad55fe9)]

## 21.3.1

This is a bug fix release of airframe-http. 

- airframe-http: Fixes [#1546](https://github.com/wvlet/airframe/issues/1546) by adding path prefixes to disambiguate NFA nodes ([#1547](https://github.com/wvlet/airframe/issues/1547)) [[910e20324](https://github.com/wvlet/airframe/commit/910e20324)]
- airframe-http: Fix OpenAPI generator to merge PathItems in the same path ([#1541](https://github.com/wvlet/airframe/issues/1541)) [[f079f8c1d](https://github.com/wvlet/airframe/commit/f079f8c1d)]
- grpc: Update grpc-netty-shaded, grpc-protobuf, ... to 1.36.1 ([#1550](https://github.com/wvlet/airframe/issues/1550)) [[426d27e89](https://github.com/wvlet/airframe/commit/426d27e89)]

### Internal Updates
- Update jmh-core, jmh-generator-bytecode, ... to 1.29 ([#1549](https://github.com/wvlet/airframe/issues/1549)) [[21ddee73d](https://github.com/wvlet/airframe/commit/21ddee73d)]
- Update coursier to 2.0.16 ([#1548](https://github.com/wvlet/airframe/issues/1548)) [[ff1fdea48](https://github.com/wvlet/airframe/commit/ff1fdea48)]
- Update trino-main to 354 ([#1544](https://github.com/wvlet/airframe/issues/1544)) [[454d3cdec](https://github.com/wvlet/airframe/commit/454d3cdec)]
- Update coursier to 2.0.14 ([#1542](https://github.com/wvlet/airframe/issues/1542)) [[f09add84d](https://github.com/wvlet/airframe/commit/f09add84d)]
- Change the default value for the formatType parameter ([#1540](https://github.com/wvlet/airframe/issues/1540)) [[33e076c36](https://github.com/wvlet/airframe/commit/33e076c36)]
- Update sbt-sonatype to 3.9.7 ([#1538](https://github.com/wvlet/airframe/issues/1538)) [[ebb9aad02](https://github.com/wvlet/airframe/commit/ebb9aad02)]
- Update antlr4, antlr4-runtime to 4.9.2 ([#1537](https://github.com/wvlet/airframe/issues/1537)) [[111759b5f](https://github.com/wvlet/airframe/commit/111759b5f)]
- Update protobuf-java to 3.15.6 ([#1536](https://github.com/wvlet/airframe/issues/1536)) [[1c6135ccc](https://github.com/wvlet/airframe/commit/1c6135ccc)]
- Update sbt-sonatype to 3.9.6 ([#1534](https://github.com/wvlet/airframe/issues/1534)) [[b36e55bfd](https://github.com/wvlet/airframe/commit/b36e55bfd)]
- Update airspec to 21.3.0 ([#1533](https://github.com/wvlet/airframe/issues/1533)) [[22f1feb7c](https://github.com/wvlet/airframe/commit/22f1feb7c)]

## 21.3.0

This version enhances airframe-rx with convenient operators: Rx.cache, Rx.andThen(Future). This version also includes various bug fixes for gRPC support.   

Since this version, airframe-log and airframe-surface support Scala 3.0.0-RC1. We dropped support for Scala 3.0.0-M3.

### Enhancement

- airframe-rx: Add Rx.cache ([#1525](https://github.com/wvlet/airframe/issues/1525)) [[799628e5](https://github.com/wvlet/airframe/commit/799628e5)]
- airframe-rx: Cache.expireAfterWrite(...) ([#1529](https://github.com/wvlet/airframe/issues/1529)) [[73dfe139](https://github.com/wvlet/airframe/commit/73dfe139)]
- airframe-rx: Add Ticker for RxCache ([#1532](https://github.com/wvlet/airframe/issues/1532)) [[49c6a8aa](https://github.com/wvlet/airframe/commit/49c6a8aa)]
- airframe-rx: [#1505](https://github.com/wvlet/airframe/issues/1505) Add Rx.andThen(Future[X]) and Rx.future[X] ([#1520](https://github.com/wvlet/airframe/issues/1520)) [[b77f0208](https://github.com/wvlet/airframe/commit/b77f0208)]
- airframe-rx-html: Add beforeRender/beforeUnmount event handler ([#1524](https://github.com/wvlet/airframe/issues/1524)) [[00736358](https://github.com/wvlet/airframe/commit/00736358)]
- airframe-http: Add a default CircuitBreaker for js HTTP client ([#1521](https://github.com/wvlet/airframe/issues/1521)) [[36594388](https://github.com/wvlet/airframe/commit/36594388)]
- Upgrade to Scala 3.0.0-RC1 ([#1487](https://github.com/wvlet/airframe/issues/1487)) [[190bf81b](https://github.com/wvlet/airframe/commit/190bf81b)]
- Upgrade to Scala 2.13.5 ([#1492](https://github.com/wvlet/airframe/issues/1492)) [[f9ed386f](https://github.com/wvlet/airframe/commit/f9ed386f)]

### Bug Fixes

- airframe-http: Fix [#1508](https://github.com/wvlet/airframe/issues/1508) Hide secret RPC parameters from logs ([#1519](https://github.com/wvlet/airframe/issues/1519)) [[7580b4c2](https://github.com/wvlet/airframe/commit/7580b4c2)]
- airframe-surface: Use jdk8 comptible reflection method ([#1527](https://github.com/wvlet/airframe/issues/1527)) [[71c80abb](https://github.com/wvlet/airframe/commit/71c80abb)]
- airframe-http: Fixes [#1509](https://github.com/wvlet/airframe/issues/1509). Resolve full package paths for Alias in generated clients ([#1518](https://github.com/wvlet/airframe/issues/1518)) [[bbe33a89](https://github.com/wvlet/airframe/commit/bbe33a89)]
- airframe-grpc: Fixes [#1494](https://github.com/wvlet/airframe/issues/1494). Support Unit return type ([#1517](https://github.com/wvlet/airframe/issues/1517)) [[e3f00664](https://github.com/wvlet/airframe/commit/e3f00664)]
- airframe-grpc: Fixes [#1511](https://github.com/wvlet/airframe/issues/1511). Option[X] can be None if missing in the arg ([#1515](https://github.com/wvlet/airframe/issues/1515)) [[2b4ebcad](https://github.com/wvlet/airframe/commit/2b4ebcad)]
- airframe-grpc: [#1513](https://github.com/wvlet/airframe/issues/1513) Add awaitTermination for closing channels ([#1516](https://github.com/wvlet/airframe/issues/1516)) [[cb43c300](https://github.com/wvlet/airframe/commit/cb43c300)]
- Remove demo from airframe-rx-widget ([#1501](https://github.com/wvlet/airframe/issues/1501)) [[feaf98f7](https://github.com/wvlet/airframe/commit/feaf98f7)]
- airframe-surface: javax-annotation dependency is unnecessary ([#1502](https://github.com/wvlet/airframe/issues/1502)) [[aa986c13](https://github.com/wvlet/airframe/commit/aa986c13)]

### Dependency Updates

- Update portable-scala-reflect to 1.1.1 ([#1512](https://github.com/wvlet/airframe/issues/1512)) [[10dc4154](https://github.com/wvlet/airframe/commit/10dc4154)]
- Update trino-main to 353 ([#1522](https://github.com/wvlet/airframe/issues/1522)) [[9d06e89a](https://github.com/wvlet/airframe/commit/9d06e89a)]
- Update protobuf-java to 3.15.5 ([#1510](https://github.com/wvlet/airframe/issues/1510)) [[ec7e65be](https://github.com/wvlet/airframe/commit/ec7e65be)]
- Update scala-parallel-collections to 1.0.1 ([#1507](https://github.com/wvlet/airframe/issues/1507)) [[e03d668f](https://github.com/wvlet/airframe/commit/e03d668f)]
- Update HikariCP to 4.0.3 ([#1503](https://github.com/wvlet/airframe/issues/1503)) [[0607344e](https://github.com/wvlet/airframe/commit/0607344e)]
- Update spark-sql to 3.1.1 ([#1499](https://github.com/wvlet/airframe/issues/1499)) [[cf4ed0a5](https://github.com/wvlet/airframe/commit/cf4ed0a5)]
- Update json4s-jackson to 3.6.11 ([#1498](https://github.com/wvlet/airframe/issues/1498)) [[2a995eef](https://github.com/wvlet/airframe/commit/2a995eef)]
- Update jmh-core, jmh-generator-bytecode, ... to 1.28 ([#1496](https://github.com/wvlet/airframe/issues/1496)) [[cdf42947](https://github.com/wvlet/airframe/commit/cdf42947)]
- Update grpc-netty-shaded, grpc-protobuf, ... to 1.36.0 ([#1490](https://github.com/wvlet/airframe/issues/1490)) [[4fffbc66](https://github.com/wvlet/airframe/commit/4fffbc66)]
- Update snakeyaml to 1.28 ([#1489](https://github.com/wvlet/airframe/issues/1489)) [[076d502e](https://github.com/wvlet/airframe/commit/076d502e)]
- Update protobuf-java to 3.15.1 ([#1488](https://github.com/wvlet/airframe/issues/1488)) [[a05095cb](https://github.com/wvlet/airframe/commit/a05095cb)]
- Update scalacheck to 1.15.3 ([#1481](https://github.com/wvlet/airframe/issues/1481)) [[ed34ed0c](https://github.com/wvlet/airframe/commit/ed34ed0c)]
- Update sbt-dotty to 0.5.3 ([#1480](https://github.com/wvlet/airframe/issues/1480)) [[d166a5ad](https://github.com/wvlet/airframe/commit/d166a5ad)]
- Update postgresql to 42.2.19 ([#1485](https://github.com/wvlet/airframe/issues/1485)) [[1e4711fe](https://github.com/wvlet/airframe/commit/1e4711fe)]
- Update sbt-mdoc to 2.2.18 ([#1484](https://github.com/wvlet/airframe/issues/1484)) [[64a51960](https://github.com/wvlet/airframe/commit/64a51960)]
- Update scala-collection-compat to 2.4.2 ([#1482](https://github.com/wvlet/airframe/issues/1482)) [[cf27d819](https://github.com/wvlet/airframe/commit/cf27d819)]
- Update airspec to 21.2.0 ([#1479](https://github.com/wvlet/airframe/issues/1479)) [[12d4843b](https://github.com/wvlet/airframe/commit/12d4843b)]
- Update coursier to 2.0.12 ([#1478](https://github.com/wvlet/airframe/issues/1478)) [[540611f3](https://github.com/wvlet/airframe/commit/540611f3)]

## 21.2.0

### New Features

Airframe 21.2.0 enhances support for gRPC with logging, multiple server factory, and JSON messaging. sbt-airframe has also been updated to support nested API package 
strucutres so that you can build HTTP servers and clients for complex services. airframe-rx-widget added a support for Google Auth and rich text editor of Visual Studio Code (Monaco Editor)
for Scala.js. Since this version, Scala.js 1.5.0 is required for JS projects. 

- Update sbt-scalajs, scalajs-compiler, ... to 1.5.0 ([#1471](https://github.com/wvlet/airframe/issues/1471)) [[84cdbbab0](https://github.com/wvlet/airframe/commit/84cdbbab0)]
- sbt-airframe: Support nested API packages for HTTP client generation ([#1429](https://github.com/wvlet/airframe/issues/1429)) [[07df5a405](https://github.com/wvlet/airframe/commit/07df5a405)]

airframe-grpc:
- airframe-http-grpc: Add gRPC logging ([#1444](https://github.com/wvlet/airframe/issues/1444)) [[7991d6ca0](https://github.com/wvlet/airframe/commit/7991d6ca0)]
- airframe-http-grpc: Add GrpcServerFactory ([#1430](https://github.com/wvlet/airframe/issues/1430)) [[b1d1e4310](https://github.com/wvlet/airframe/commit/b1d1e4310)]
- airframe-http-grpc: Support JSON request/response ([#1445](https://github.com/wvlet/airframe/issues/1445)) [[851487aec](https://github.com/wvlet/airframe/commit/851487aec)]

airframe-http:
- airframe-http: Add a standard filter for HttpMessage.Request/Response ([#1438](https://github.com/wvlet/airframe/issues/1438)) [[270341450](https://github.com/wvlet/airframe/commit/270341450)]
- airframe-http: [#1354](https://github.com/wvlet/airframe/issues/1354) Fix missing slash in Http.client requests ([#1437](https://github.com/wvlet/airframe/issues/1437)) [[245d83000](https://github.com/wvlet/airframe/commit/245d83000)]
- airframe-http: [#1355](https://github.com/wvlet/airframe/issues/1355) Add Http.clientFor(serverAddress) ([#1436](https://github.com/wvlet/airframe/issues/1436)) [[79d761bce](https://github.com/wvlet/airframe/commit/79d761bce)]
- airframe-http: [#1135](https://github.com/wvlet/airframe/issues/1135) Support modifying retryContext ([#1435](https://github.com/wvlet/airframe/issues/1435)) [[8d9a77fca](https://github.com/wvlet/airframe/commit/8d9a77fca)]
- airframe-http: Update finagle-core, finagle-http, ... to 21.2.0 ([#1470](https://github.com/wvlet/airframe/issues/1470)) [[b8acccdc2](https://github.com/wvlet/airframe/commit/b8acccdc2)]

airframe-rx-widget:
- airframe-rx-widget: Add Monaco editor facade ([#1459](https://github.com/wvlet/airframe/issues/1459)) [[ab458acbe](https://github.com/wvlet/airframe/commit/ab458acbe)]
- airframe-rx-widget: Add Google Auth support ([#1450](https://github.com/wvlet/airframe/issues/1450)) [[68c65eb5f](https://github.com/wvlet/airframe/commit/68c65eb5f)]
- airframe-rx-widget: Deprecated bootstrap specific modules ([#1454](https://github.com/wvlet/airframe/issues/1454)) [[dc4d159c7](https://github.com/wvlet/airframe/commit/dc4d159c7)]

### Bug Fixes
- airframe-di: [#586](https://github.com/wvlet/airframe/issues/586) Fixes constructor injection to use Singleton  ([#1439](https://github.com/wvlet/airframe/issues/1439)) [[b98fb9f9a](https://github.com/wvlet/airframe/commit/b98fb9f9a)]
- sbt-airframe: [#1457](https://github.com/wvlet/airframe/issues/1457) Support downloading airframe-http from Sonatype snapshot repo ([#1462](https://github.com/wvlet/airframe/issues/1462)) [[c07bdc4a0](https://github.com/wvlet/airframe/commit/c07bdc4a0)]
- sbt-airframe: Fixes [#1447](https://github.com/wvlet/airframe/issues/1447) Use JSON command line arguments ([#1449](https://github.com/wvlet/airframe/issues/1449)) [[6d4e059ec](https://github.com/wvlet/airframe/commit/6d4e059ec)]
- sbt-airframe: Fix full type name resolution ([#1434](https://github.com/wvlet/airframe/issues/1434)) [[93c3a9f1c](https://github.com/wvlet/airframe/commit/93c3a9f1c)]
- airframe-http: Fixes [#1446](https://github.com/wvlet/airframe/issues/1446). Resolve MIME type of file names with multiple dots ([#1448](https://github.com/wvlet/airframe/issues/1448)) [[9db4128ac](https://github.com/wvlet/airframe/commit/9db4128ac)]
- airframe-rx-widget: Fix GAuth sign-in flow ([#1461](https://github.com/wvlet/airframe/issues/1461)) [[26d32baa6](https://github.com/wvlet/airframe/commit/26d32baa6)]

### Other updates
- internal: Fix the release script to support longer git hash values ([#1477](https://github.com/wvlet/airframe/issues/1477)) [[d8ee440ce](https://github.com/wvlet/airframe/commit/d8ee440ce)]
- Update sbt-pgp to 2.1.2 ([#1476](https://github.com/wvlet/airframe/issues/1476)) [[35d7f62d3](https://github.com/wvlet/airframe/commit/35d7f62d3)]
- Update coursier to 2.0.11 ([#1473](https://github.com/wvlet/airframe/issues/1473)) [[47ff2c9d4](https://github.com/wvlet/airframe/commit/47ff2c9d4)]
- Update HikariCP to 4.0.2 ([#1474](https://github.com/wvlet/airframe/issues/1474)) [[cde6b16c1](https://github.com/wvlet/airframe/commit/cde6b16c1)]
- Update fluency-core, fluency-fluentd, ... to 2.5.1 ([#1475](https://github.com/wvlet/airframe/issues/1475)) [[cfc65cd90](https://github.com/wvlet/airframe/commit/cfc65cd90)]
- Update trino-main to 352 ([#1469](https://github.com/wvlet/airframe/issues/1469)) [[330f5e336](https://github.com/wvlet/airframe/commit/330f5e336)]
- Update sbt-mdoc to 2.2.17 ([#1467](https://github.com/wvlet/airframe/issues/1467)) [[3997cfe10](https://github.com/wvlet/airframe/commit/3997cfe10)]
- Update scala-collection-compat to 2.4.1 ([#1464](https://github.com/wvlet/airframe/issues/1464)) [[ef6028a90](https://github.com/wvlet/airframe/commit/ef6028a90)]
- Update sbt, sbt-dependency-tree, ... to 1.4.7 ([#1465](https://github.com/wvlet/airframe/issues/1465)) [[0cf9ad8b5](https://github.com/wvlet/airframe/commit/0cf9ad8b5)]
- internal: Fix Scala 2.13 related warnings ([#1460](https://github.com/wvlet/airframe/issues/1460)) [[a08011d16](https://github.com/wvlet/airframe/commit/a08011d16)]
- internal: Update sbt-dotty to 0.5.2 ([#1455](https://github.com/wvlet/airframe/issues/1455)) [[f719e2014](https://github.com/wvlet/airframe/commit/f719e2014)]
- internal: Update portable-scala-reflect to 1.1.0 ([#1453](https://github.com/wvlet/airframe/issues/1453)) [[758ed820a](https://github.com/wvlet/airframe/commit/758ed820a)]
- internal: Update airspec to 21.1.1 ([#1428](https://github.com/wvlet/airframe/issues/1428)) [[f57345fad](https://github.com/wvlet/airframe/commit/f57345fad)]
- internal: Upgrade codecov action to v1 ([#1432](https://github.com/wvlet/airframe/issues/1432)) [[65fdc9afb](https://github.com/wvlet/airframe/commit/65fdc9afb)]
- internal: Remove cache from CI ([#1431](https://github.com/wvlet/airframe/issues/1431)) [[5e7b6a3bb](https://github.com/wvlet/airframe/commit/5e7b6a3bb)]

## 21.1.1

This is a minor bug fix release of airframe-grpc for Scala 2.13

-  airframe-grpc: Update grpc-netty-shaded, grpc-protobuf, ... to 1.35.0 ([#1427](https://github.com/wvlet/airframe/issues/1427)) [[feed252](https://github.com/wvlet/airframe/commit/feed252)]
-  sbt-airframe: Fix code gRPC client generator for Scala 2.13 ([#1426](https://github.com/wvlet/airframe/issues/1426)) [[f458180](https://github.com/wvlet/airframe/commit/f458180)]
-  internal: Update antlr4, antlr4-runtime to 4.9.1 ([#1424](https://github.com/wvlet/airframe/issues/1424)) [[3a72a5a](https://github.com/wvlet/airframe/commit/3a72a5a)]
-  internal: Update spark-sql to 3.1.0 ([#1423](https://github.com/wvlet/airframe/issues/1423)) [[a9bd431](https://github.com/wvlet/airframe/commit/a9bd431)]
-  internal: airframe-sql: Migrate from Presto to Trino ([#1421](https://github.com/wvlet/airframe/issues/1421)) [[8d4f7d6](https://github.com/wvlet/airframe/commit/8d4f7d6)]

## 21.1.0

This release fixes a missing dependency issue in 20.12.2:
-  [#1417](https://github.com/wvlet/airframe/issues/1417): Fixes a missing dependency to airframe-di-macros ([#1418](https://github.com/wvlet/airframe/issues/1418)) [[7025fb8](https://github.com/wvlet/airframe/commit/7025fb8)]

## 20.12.2

### Scala 3 support (experimenal)

airframe-log, airframe-surface are experimentally supporting Scala 3.0.0-M3. We are now working on Scala 3 support for Airframe DI. The progress can be tracked at [#1077](https://github.com/wvlet/airframe/issues/1077). We have found one blocking issue that needs a fix at Scala 3 side [#10931](https://github.com/lampepfl/dotty/issues/10931). After this issue is resolved, porting other Airframe modules, including AirSpec, to Scala 3 would not be so difficult for us.  

-  airframe-log: Upgrade to Scala 3.0.0-M3 ([#1398](https://github.com/wvlet/airframe/issues/1398)) [[3b2c795](https://github.com/wvlet/airframe/commit/3b2c795)]
-  airframe-surface: Surface.methodsOf[X] for Scala 3 ([#1403](https://github.com/wvlet/airframe/issues/1403)) [[f26d216](https://github.com/wvlet/airframe/commit/f26d216)]
-  airframe-surface: Support Surface.of[X] for Scala 3 (Dotty) ([#1385](https://github.com/wvlet/airframe/issues/1385)) [[91ecac5](https://github.com/wvlet/airframe/commit/91ecac5)]
-  airframe-di: Split Scala 2 and Scala 3 source folders ([#1405](https://github.com/wvlet/airframe/issues/1405)) [[02ae40e](https://github.com/wvlet/airframe/commit/02ae40e)]
-  airframe-di: Add Scala3 impl base ([#1407](https://github.com/wvlet/airframe/issues/1407)) [[f37f807](https://github.com/wvlet/airframe/commit/f37f807)]
-  Update sbt-dotty to 0.5.1 ([#1399](https://github.com/wvlet/airframe/issues/1399)) [[87dd372](https://github.com/wvlet/airframe/commit/87dd372)]

Other updates:

-  airframe-rpc: Update grpc-netty-shaded, grpc-protobuf, ... to 1.34.1 ([#1394](https://github.com/wvlet/airframe/issues/1394)) [[01615b9](https://github.com/wvlet/airframe/commit/01615b9)]
-  airframe-http-finagle: Update finagle-core, finagle-http, ... to 20.12.0 ([#1389](https://github.com/wvlet/airframe/issues/1389)) [[e4108bd](https://github.com/wvlet/airframe/commit/e4108bd)]
-  airframe-fluentd: Add fluency-treasuredata to the library dependency ([#1404](https://github.com/wvlet/airframe/issues/1404)) [[e5ee9ba](https://github.com/wvlet/airframe/commit/e5ee9ba)]
-  airframe-jdbc: Update sqlite-jdbc to 3.34.0 ([#1390](https://github.com/wvlet/airframe/issues/1390)) [[2849c01](https://github.com/wvlet/airframe/commit/2849c01)]
-  airframe-sql: Update presto-main to 350 ([#1414](https://github.com/wvlet/airframe/issues/1414)) [[d7e2973](https://github.com/wvlet/airframe/commit/d7e2973)]
-  airspec: Update scalacheck to 1.15.2 ([#1397](https://github.com/wvlet/airframe/issues/1397)) [[8056d84](https://github.com/wvlet/airframe/commit/8056d84)]
-  airframe-msgpack: Update msgpack-core to 0.8.22 ([#1401](https://github.com/wvlet/airframe/issues/1401)) [[0b4f744](https://github.com/wvlet/airframe/commit/0b4f744)]
-  Update scala-collection-compat to 2.3.2 ([#1406](https://github.com/wvlet/airframe/issues/1406)) [[ec83785](https://github.com/wvlet/airframe/commit/ec83785)]
-  internal: Upgrade setup-node to v2 ([#1411](https://github.com/wvlet/airframe/issues/1411)) [[fc9373f](https://github.com/wvlet/airframe/commit/fc9373f)]
-  internal: Update coursier to 2.0.8 ([#1408](https://github.com/wvlet/airframe/issues/1408)) [[919c47b](https://github.com/wvlet/airframe/commit/919c47b)]
-  internal: Update sbt, sbt-dependency-tree, ... to 1.4.6 ([#1409](https://github.com/wvlet/airframe/issues/1409)) [[6a575d3](https://github.com/wvlet/airframe/commit/6a575d3)]
-  internal: Update sbt-mdoc to 2.2.14 ([#1396](https://github.com/wvlet/airframe/issues/1396)) [[9232d8a](https://github.com/wvlet/airframe/commit/9232d8a)]
-  internal: Update sbt script for Apple Silicion ([#1395](https://github.com/wvlet/airframe/issues/1395)) [[63d3bae](https://github.com/wvlet/airframe/commit/63d3bae)]
-  internal: Update jmh-core, jmh-generator-bytecode, ... to 1.27 ([#1388](https://github.com/wvlet/airframe/issues/1388)) [[4d65de2](https://github.com/wvlet/airframe/commit/4d65de2)]
-  internal: Update swagger-parser to 2.0.24 ([#1382](https://github.com/wvlet/airframe/issues/1382)) [[b32368d](https://github.com/wvlet/airframe/commit/b32368d)]

## 20.12.1

This version has added an experimental support for Dotty (Scala 3.0.0-M2) for airframe-log. 

-  airframe-log: Dotty support ([#1381](https://github.com/wvlet/airframe/issues/1381)) [[c6cea28](https://github.com/wvlet/airframe/commit/c6cea28)]
   - Changed to eagerly initialize the root logger to use `SourceCodeLogFormatter`. This is a workaround for the Scala3's [new lazy val initialzation logic](https://dotty.epfl.ch/docs/reference/changed-features/lazy-vals-init.html). 
-  Upgrade to Scala 2.13.4 ([#1376](https://github.com/wvlet/airframe/issues/1376)) [[bd13085](https://github.com/wvlet/airframe/commit/bd13085)]
-  Upgrade scalacheck to 1.15.1 ([#1374](https://github.com/wvlet/airframe/issues/1374)) [[93d2fc0](https://github.com/wvlet/airframe/commit/93d2fc0)]

### Internal Changes
-  internal: Update sbt-dotty to 0.4.6 ([#1378](https://github.com/wvlet/airframe/issues/1378)) [[cb49d7b](https://github.com/wvlet/airframe/commit/cb49d7b)]
-  internal: Separate AirSpec build  ([#1380](https://github.com/wvlet/airframe/issues/1380)) [[1b39ece](https://github.com/wvlet/airframe/commit/1b39ece)]
-  internal: Add configuration for testing Scala 3 (Dotty) ([#1377](https://github.com/wvlet/airframe/issues/1377)) [[64feb58](https://github.com/wvlet/airframe/commit/64feb58)]
-  iinternal: Use a different sonatype session name for Scala.js release [[9bac053](https://github.com/wvlet/airframe/commit/9bac053)]

## 20.12.0

This is a minor dependency update release. 

-  airframe-grpc: Update grpc-netty-shaded, grpc-protobuf, ... to 1.34.0 ([#1371](https://github.com/wvlet/airframe/issues/1371)) [[65cd466](https://github.com/wvlet/airframe/commit/65cd466)]
-  sbt-airframe: Embed the service name to OpenAPI tag ([#1362](https://github.com/wvlet/airframe/issues/1362)) [[306bf81](https://github.com/wvlet/airframe/commit/306bf81)]
-  Update scala-collection-compat to 2.3.1 ([#1366](https://github.com/wvlet/airframe/issues/1366)) [[d7392c0](https://github.com/wvlet/airframe/commit/d7392c0)]
-  internal: Update sbt-pgp to 2.0.2 ([#1370](https://github.com/wvlet/airframe/issues/1370)) [[ba279f2](https://github.com/wvlet/airframe/commit/ba279f2)]
-  internal: Update presto-main to 347 ([#1368](https://github.com/wvlet/airframe/issues/1368)) [[3a19419](https://github.com/wvlet/airframe/commit/3a19419)]
-  internal: Update antlr4, antlr4-runtime to 4.9 ([#1365](https://github.com/wvlet/airframe/issues/1365)) [[44fe2db](https://github.com/wvlet/airframe/commit/44fe2db)]
-  internal: Update sbt-mdoc to 2.2.13 ([#1367](https://github.com/wvlet/airframe/issues/1367)) [[c3aecb4](https://github.com/wvlet/airframe/commit/c3aecb4)]
-  internal: Update sbt, sbt-dependency-tree, ... to 1.4.4 ([#1364](https://github.com/wvlet/airframe/issues/1364)) [[9c7adde](https://github.com/wvlet/airframe/commit/9c7adde)]
-  internal: Update scala-collection-compat to 2.3.0 ([#1363](https://github.com/wvlet/airframe/issues/1363)) [[0fd99c8](https://github.com/wvlet/airframe/commit/0fd99c8)]
-  internal: Add a GitHub Action for creating release note links [[927a99c](https://github.com/wvlet/airframe/commit/927a99c)]
-  internal: Update sbt-pgp to 2.1.1 ([#1373](https://github.com/wvlet/airframe/issues/1373)) [[060ff7a](https://github.com/wvlet/airframe/commit/060ff7a)]
-  internal: sbt-pgp 2.1.1 no longer requires gpg 1.x [[f022394](https://github.com/wvlet/airframe/commit/f022394)]

## 20.11.0

From this release, we no longer support Scala 2.11. This is because Spark, the last major libarary using Scala 2.11, has updated to use Scala 2.12 since Spark 3.0.x. 

-  Remove support for Scala 2.11 ([#1347](https://github.com/wvlet/airframe/issues/1347)) [[484587a](https://github.com/wvlet/airframe/commit/484587a)]
-  sbt-airframe: Various fixes of Open API generator ([#1357](https://github.com/wvlet/airframe/issues/1357)) [[f227e1e](https://github.com/wvlet/airframe/commit/f227e1e)]
-  airframe-fluentd: Update fluency-core, fluency-fluentd, ... to 2.5.0 ([#1353](https://github.com/wvlet/airframe/issues/1353)) [[88cf476](https://github.com/wvlet/airframe/commit/88cf476)]
-  airframe-http: Add a method for storing server exceptions to TLS: TLS_KEY_SERVER_EXCEPTION ([#1351](https://github.com/wvlet/airframe/issues/1351)) [[f35b64e](https://github.com/wvlet/airframe/commit/f35b64e)]
-  airframe-jdbc: Remove duplicated close message ([#1343](https://github.com/wvlet/airframe/issues/1343)) [[bb06ec9](https://github.com/wvlet/airframe/commit/bb06ec9)]
-  airframe-json: Improve empty JSON array and object formatting ([#1345](https://github.com/wvlet/airframe/issues/1345)) [[c1de7ed](https://github.com/wvlet/airframe/commit/c1de7ed)]
-  internal: Update coursier to 2.0.7 ([#1360](https://github.com/wvlet/airframe/issues/1360)) [[9a116c1](https://github.com/wvlet/airframe/commit/9a116c1)]
-  internal: Upgrade to setup-scala@v10 ([#1361](https://github.com/wvlet/airframe/issues/1361)) [[91ec6a0](https://github.com/wvlet/airframe/commit/91ec6a0)]
-  internal: Update sbt, sbt-dependency-tree, ... to 1.4.3 ([#1359](https://github.com/wvlet/airframe/issues/1359)) [[568e945](https://github.com/wvlet/airframe/commit/568e945)]
-  internal: Update sbt-scalajs, scalajs-compiler, ... to 1.3.1 ([#1358](https://github.com/wvlet/airframe/issues/1358)) [[2b8787f](https://github.com/wvlet/airframe/commit/2b8787f)]
-  internal: Update protobuf-java to 3.14.0 ([#1356](https://github.com/wvlet/airframe/issues/1356)) [[06c602e](https://github.com/wvlet/airframe/commit/06c602e)]
-  internal: Update presto-main to 346 ([#1350](https://github.com/wvlet/airframe/issues/1350)) [[2a9979e](https://github.com/wvlet/airframe/commit/2a9979e)]
-  internal: Update sbt-mdoc to 2.2.12 ([#1349](https://github.com/wvlet/airframe/issues/1349)) [[549e5a5](https://github.com/wvlet/airframe/commit/549e5a5)]
-  internal: Update swagger-parser to 2.0.23 ([#1338](https://github.com/wvlet/airframe/issues/1338)) [[1c6c191](https://github.com/wvlet/airframe/commit/1c6c191)]
-  internal: Update sbt-sonatype to 3.9.5 ([#1339](https://github.com/wvlet/airframe/issues/1339)) [[2951d0c](https://github.com/wvlet/airframe/commit/2951d0c)]

## 20.10.3

-  airframe-http: Return 400 when invalid JSON data is passed and fix Timestamp json formatting ([#1336](https://github.com/wvlet/airframe/issues/1336)) [[831b71c](https://github.com/wvlet/airframe/commit/831b71c)]
-  airframe-rpc: Update grpc-netty-shaded, grpc-protobuf, ... to 1.33.1 ([#1334](https://github.com/wvlet/airframe/issues/1334)) [[58f4589](https://github.com/wvlet/airframe/commit/58f4589)]
-  airframe-finagle: Update finagle-core to 20.10.0 ([#1333](https://github.com/wvlet/airframe/issues/1333)) [[5cd901b](https://github.com/wvlet/airframe/commit/5cd901b)]
-  internal: Use Airframe 20.10.2 in the demo code [[8938c54](https://github.com/wvlet/airframe/commit/8938c54)]
-  internal: Update presto-main to 345 ([#1331](https://github.com/wvlet/airframe/issues/1331)) [[ddb0c52](https://github.com/wvlet/airframe/commit/ddb0c52)]

## 20.10.2

This version is a bug fix and small maintenance release.

-  airframe-grpc: Fixes [#1329](https://github.com/wvlet/airframe/issues/1329). Use jdk8 compatible stream reader ([#1330](https://github.com/wvlet/airframe/issues/1330)) [[810bda8](https://github.com/wvlet/airframe/commit/810bda8)]
-  airframe-grpc: Update grpc-netty-shaded, grpc-protobuf, ... to 1.33.0 ([#1326](https://github.com/wvlet/airframe/issues/1326)) [[2749d9e](https://github.com/wvlet/airframe/commit/2749d9e)]
-  airframe-rx: Fix Rx.interval ([#1328](https://github.com/wvlet/airframe/issues/1328)) [[91a501d](https://github.com/wvlet/airframe/commit/91a501d)]
-  airframe-rx-html: Add a demo code for airframe-rx-html ([#1283](https://github.com/wvlet/airframe/issues/1283)) [[70dda74](https://github.com/wvlet/airframe/commit/70dda74)]
-  airframe-log: Fixes [#1321](https://github.com/wvlet/airframe/issues/1321). Set the default log level for Scala.js ([#1327](https://github.com/wvlet/airframe/issues/1327)) [[d27748d](https://github.com/wvlet/airframe/commit/d27748d)]
-  airframe-http: Support a Date instance as an argument for withDate method ([#1184](https://github.com/wvlet/airframe/issues/1184)) [[89347f3](https://github.com/wvlet/airframe/commit/89347f3)]
-  airframe-rpc: Add an RPC demo code using Scala + Scala.js ([#1322](https://github.com/wvlet/airframe/issues/1322)) [[e407847](https://github.com/wvlet/airframe/commit/e407847)]
-  internal: Update antlr4, antlr4-runtime to 4.8-1 ([#1325](https://github.com/wvlet/airframe/issues/1325)) [[b89453a](https://github.com/wvlet/airframe/commit/b89453a)]
-  internal: Update sbt-mdoc to 2.2.10 ([#1320](https://github.com/wvlet/airframe/issues/1320)) [[2f69780](https://github.com/wvlet/airframe/commit/2f69780)]
-  Internal: Update sbt, sbt-dependency-tree, ... to 1.4.1 ([#1324](https://github.com/wvlet/airframe/issues/1324)) [[521202d](https://github.com/wvlet/airframe/commit/521202d)]
-  internal: Update scalafmt-core to 2.7.5 ([#1323](https://github.com/wvlet/airframe/issues/1323)) [[22e4ac8](https://github.com/wvlet/airframe/commit/22e4ac8)]

## 20.10.1

-  airframe-http, rx: Update sbt-scalajs, scalajs-compiler, ... to 1.3.0 ([#1316](https://github.com/wvlet/airframe/issues/1316)) [[73a7129](https://github.com/wvlet/airframe/commit/73a7129)]
-  airframe-finagle: Support raw-string and MsgPack responses [[1828afb](https://github.com/wvlet/airframe/commit/1828afb)]
-  airframe-finagle: Update finagle-core, finagle-http, ... to 20.9.0 ([#1298](https://github.com/wvlet/airframe/issues/1298)) [[bf8b1e6](https://github.com/wvlet/airframe/commit/bf8b1e6)]
-  airframe-grpc: Update grpc-netty-shaded, grpc-protobuf, ... to 1.32.2 ([#1314](https://github.com/wvlet/airframe/issues/1314)) [[7e55229](https://github.com/wvlet/airframe/commit/7e55229)]
-  airframe-jdbc: Update postgresql to 42.2.18 ([#1317](https://github.com/wvlet/airframe/issues/1317)) [[f61c3e5](https://github.com/wvlet/airframe/commit/f61c3e5)]
-  internal: Update presto-main to 344 ([#1313](https://github.com/wvlet/airframe/issues/1313)) [[6efd9f5](https://github.com/wvlet/airframe/commit/6efd9f5)]
-  internal: Update jmh-core, jmh-generator-bytecode, ... to 1.26 ([#1311](https://github.com/wvlet/airframe/issues/1311)) [[b59c129](https://github.com/wvlet/airframe/commit/b59c129)]

## 20.10.0

This is a minor maintenance release. Mostly for intenral library upgrades.  

-  sbt-airframe: Support Future/Response response type in OpenAPI generator ([#1308](https://github.com/wvlet/airframe/issues/1308)) [[3ea1e9c](https://github.com/wvlet/airframe/commit/3ea1e9c)]
-  internal: Upgrade to sbt 1.4.0 ([#1306](https://github.com/wvlet/airframe/issues/1306)) [[8b5f373](https://github.com/wvlet/airframe/commit/8b5f373)]
-  internal: Update scalafmt-core to 2.7.3 ([#1302](https://github.com/wvlet/airframe/issues/1302)) [[4618206](https://github.com/wvlet/airframe/commit/4618206)]
-  internal: sbt-airframe: Update swagger-parser to 2.0.22 ([#1304](https://github.com/wvlet/airframe/issues/1304)) [[d6815c1](https://github.com/wvlet/airframe/commit/d6815c1)]
-  internal: Update json4s-jackson to 3.6.10 ([#1301](https://github.com/wvlet/airframe/issues/1301)) [[a0b6360](https://github.com/wvlet/airframe/commit/a0b6360)]
-  internal: Update presto-main to 343 ([#1300](https://github.com/wvlet/airframe/issues/1300)) [[b7b1dfa](https://github.com/wvlet/airframe/commit/b7b1dfa)]
-  internal: Update sbt-pack to 0.13 ([#1297](https://github.com/wvlet/airframe/issues/1297)) [[93a1735](https://github.com/wvlet/airframe/commit/93a1735)]
-  internal: Update scalafmt-core to 2.7.2 ([#1296](https://github.com/wvlet/airframe/issues/1296)) [[53e52cb](https://github.com/wvlet/airframe/commit/53e52cb)]
-  internal: Update sbt-mdoc to 2.2.9 ([#1295](https://github.com/wvlet/airframe/issues/1295)) [[8ee9a6e](https://github.com/wvlet/airframe/commit/8ee9a6e)]

## 20.9.2

This version upgrades Scala.js to 1.2.0 and includes RxOption rendering bug fixes. 

-  Upgraded Scala.js version to 1.2.0 ([#1272](https://github.com/wvlet/airframe/issues/1272)) [[627a4a8](https://github.com/wvlet/airframe/commit/627a4a8)]
-  airframe-rx: Interface hierarchy change: Rx[A], RxOption[A], and RxStream[A] ([#1292](https://github.com/wvlet/airframe/issues/1292)) [[10d9e29](https://github.com/wvlet/airframe/commit/10d9e29)]
-  airframe-rx: Add throttleFirst/Last(sample) ([#1291](https://github.com/wvlet/airframe/issues/1291)) [[4fb0dce](https://github.com/wvlet/airframe/commit/4fb0dce)]
-  airframe-rx: internal: Add RxResult for propergating downstream operator states ([#1294](https://github.com/wvlet/airframe/issues/1294)) [[835dbaf](https://github.com/wvlet/airframe/commit/835dbaf)]
-  airframe-msgpack: Update msgpack-core to 0.8.21 ([#1285](https://github.com/wvlet/airframe/issues/1285)) [[5768da4](https://github.com/wvlet/airframe/commit/5768da4)]
-  airframe-config: Update snakeyaml to 1.27 ([#1282](https://github.com/wvlet/airframe/issues/1282)) [[639162a](https://github.com/wvlet/airframe/commit/639162a)]
-  ariframe-grpc: Update grpc-netty-shaded, grpc-protobuf, ... to 1.32.1 ([#1278](https://github.com/wvlet/airframe/issues/1278)) [[2610ad3](https://github.com/wvlet/airframe/commit/2610ad3)]
-  internal: Update scalafmt-core to 2.7.1 ([#1288](https://github.com/wvlet/airframe/issues/1288)) [[09edd54](https://github.com/wvlet/airframe/commit/09edd54)]
-  internal: Update sbt-antlr4 to 0.8.3 ([#1287](https://github.com/wvlet/airframe/issues/1287)) [[8df7e70](https://github.com/wvlet/airframe/commit/8df7e70)]
-  internal: Update spark-sql to 2.4.7 ([#1286](https://github.com/wvlet/airframe/issues/1286)) [[e66f981](https://github.com/wvlet/airframe/commit/e66f981)]
-  internal: Update scala-collection-compat to 2.2.0 ([#1280](https://github.com/wvlet/airframe/issues/1280)) [[b08b195](https://github.com/wvlet/airframe/commit/b08b195)]
-  internal: Update protobuf-java to 3.12.4 ([#1277](https://github.com/wvlet/airframe/issues/1277)) [[48c958c](https://github.com/wvlet/airframe/commit/48c958c)]
-  internal: Exclude airframe-benchmark from artifacts to resolve protobuf scaladoc generation errors ([#1276](https://github.com/wvlet/airframe/issues/1276)) [[547c1e2](https://github.com/wvlet/airframe/commit/547c1e2)]

## 20.9.1

This release is for enhancing the usability of airframe-rx. 

-  airframe-rx: Make RxOption public and add Rx.join/zip/concat helper methods ([#1275](https://github.com/wvlet/airframe/issues/1275)) [[a13006d](https://github.com/wvlet/airframe/commit/a13006d)]
-  airframe-rx: Add Rx.take(n) and Rx.interval ([#1269](https://github.com/wvlet/airframe/issues/1269)) [[febba6d](https://github.com/wvlet/airframe/commit/febba6d)]
-  airframe-rx: Add RxOption.getOrElse/orElse/transform ([#1268](https://github.com/wvlet/airframe/issues/1268)) [[670f11a](https://github.com/wvlet/airframe/commit/670f11a)]
-  internal: Update presto-main to 341 ([#1274](https://github.com/wvlet/airframe/issues/1274)) [[327bede](https://github.com/wvlet/airframe/commit/327bede)]
-  internal: Upgrade to scalafmt 2.7.0 ([#1271](https://github.com/wvlet/airframe/issues/1271)) [[b4c707c](https://github.com/wvlet/airframe/commit/b4c707c)]
-  internal: Update protobuf-java to 3.13.0 ([#1267](https://github.com/wvlet/airframe/issues/1267)) [[470b69e](https://github.com/wvlet/airframe/commit/470b69e)]
-  internal: Update sbt-mdoc to 2.2.6 ([#1262](https://github.com/wvlet/airframe/issues/1262)) [[a578830](https://github.com/wvlet/airframe/commit/a578830)]
-  internal: Use GIT_DEPLOY_KEY env [[afc49f7](https://github.com/wvlet/airframe/commit/afc49f7)]
-  internal: Fix env var name [[e476dec](https://github.com/wvlet/airframe/commit/e476dec)]
-  internal: Update to sbt-mdoc 2.2.6 and use GIT_DEPLOY_KEY env for doc deployment [[236f6ba](https://github.com/wvlet/airframe/commit/236f6ba)]
-  internal: Add packaging test ([#1264](https://github.com/wvlet/airframe/issues/1264)) [[8ac4707](https://github.com/wvlet/airframe/commit/8ac4707)]
-  internal: Update jmh-core, jmh-generator-bytecode, ... to 1.25.2 ([#1265](https://github.com/wvlet/airframe/issues/1265)) [[35c970a](https://github.com/wvlet/airframe/commit/35c970a)]

## 20.9.0

This version is a minor bug fix release. 

-  airframe-rx: Fix the compilation error when binding RxVar ([#1263](https://github.com/wvlet/airframe/issues/1263)) [[b6c8e0f](https://github.com/wvlet/airframe/commit/b6c8e0f)]
-  airframe-http: Client generator clieanup ([#1254](https://github.com/wvlet/airframe/issues/1254)) [[0b312f2](https://github.com/wvlet/airframe/commit/0b312f2)]
-  airframe-jdbc: Update postgresql to 42.2.16 ([#1252](https://github.com/wvlet/airframe/issues/1252)) [[2e6f9ff](https://github.com/wvlet/airframe/commit/2e6f9ff)]
-  airframe-grpc: Add benchmark for grpc-java ([#1251](https://github.com/wvlet/airframe/issues/1251)) [[8ddf320](https://github.com/wvlet/airframe/commit/8ddf320)]
-  airframe-rpc: Add an RPC project example ([#1250](https://github.com/wvlet/airframe/issues/1250)) [[942f11e](https://github.com/wvlet/airframe/commit/942f11e)]
-  internal: Update sbt-jmh to 0.4.0 ([#1260](https://github.com/wvlet/airframe/issues/1260)) [[d690eb8](https://github.com/wvlet/airframe/commit/d690eb8)]
-  internal: Fix Scala.js CI and upgrade scala-js-dom to 1.1.0  ([#1261](https://github.com/wvlet/airframe/issues/1261)) [[5259c64](https://github.com/wvlet/airframe/commit/5259c64)]
-  internal: Update sbt script ([#1259](https://github.com/wvlet/airframe/issues/1259)) [[b999f14](https://github.com/wvlet/airframe/commit/b999f14)]
-  internal: Update jmh-core, jmh-generator-bytecode, ... to 1.25.1 ([#1256](https://github.com/wvlet/airframe/issues/1256)) [[589a16c](https://github.com/wvlet/airframe/commit/589a16c)]

## 20.8.0

### Major And Breaking Changes

- Airframe RPC now supports HTTP/2 based gRPC backend: [airframe-grpc](https://wvlet.org/airframe/docs/airframe-rpc#airframe-grpc). Our initial benchmark result showed [3x faster rpc/sec](https://github.com/wvlet/airframe/pull/1247) over Finagle backend.  
   -  airframe-grpc: Support gRPC backend ([#1192](https://github.com/wvlet/airframe/issues/1192)) [[3670cc7](https://github.com/wvlet/airframe/commit/3670cc7)]
- [airframe-rx](https://wvlet.org/airframe/docs/airframe-rx) becomes an independent module (`wvlet.airframe.rx.Rx[X]`) for providing ReactiveX streaming interface. This module is used for gRPC-streaming and interctive DOM rendering (airframe-rx-html)
   - The package `wvlet.airframe.http.rx` has been renamed to `wvlet.airframe.rx` ([#1237](https://github.com/wvlet/airframe/issues/1237)) [[99c54b2](https://github.com/wvlet/airframe/commit/99c54b2)]
- airframe-http-rx, airframe-http-widgets are renamed to airframe-rx-http and airframe-rx-widgets respectively ([#1238](https://github.com/wvlet/airframe/issues/1238)) [[b58d40a](https://github.com/wvlet/airframe/commit/b58d40a)]

### New Features

-  airframe-control: Support ULID. [Universally Unique Lexicographically Sortable Identifier](https://github.com/ulid/spec) ([#1223](https://github.com/wvlet/airframe/issues/1223)) [[6e7e7df](https://github.com/wvlet/airframe/commit/6e7e7df)]
-  airframe-codec: Support ListMap codec ([#1217](https://github.com/wvlet/airframe/issues/1217)) [[e96341a](https://github.com/wvlet/airframe/commit/e96341a)]
-  airframe-rx: Represent Rx values with OnNext, OnError, and OnCompletion ([#1235](https://github.com/wvlet/airframe/issues/1235)) [[39a2a92](https://github.com/wvlet/airframe/commit/39a2a92)]
-  airframe-rx: Add RxOption[A]  ([#1214](https://github.com/wvlet/airframe/issues/1214)) [[d08ea45](https://github.com/wvlet/airframe/commit/d08ea45)]
-  airframe-rx: Add Rx.zip(...), Rx.join(...) for merging multiple stream events ([#1216](https://github.com/wvlet/airframe/issues/1216)) [[37196a8](https://github.com/wvlet/airframe/commit/37196a8)]
-  airframe-json: Add JSON.format for pretty printing ([#1225](https://github.com/wvlet/airframe/issues/1225)) [[d01c772](https://github.com/wvlet/airframe/commit/d01c772)]
-  sbt-airframe: Support generating gRPC clients ([#1194](https://github.com/wvlet/airframe/issues/1194)) [[2890331](https://github.com/wvlet/airframe/commit/2890331)]

### Bug fixes

-  sbt-airframe: Limit the scope of finding @RPC/@Endpoint annotaitons ([#1244](https://github.com/wvlet/airframe/issues/1244)) [[202f94b](https://github.com/wvlet/airframe/commit/202f94b)]
-  airfame-codec: Throw an exception for invalid enum strings ([#1220](https://github.com/wvlet/airframe/issues/1220)) [[a55cb68](https://github.com/wvlet/airframe/commit/a55cb68)]
-  airframe-okhttp: Replace the request body content to a byte array in memory. ([#1233](https://github.com/wvlet/airframe/issues/1233)) [[242d3c0](https://github.com/wvlet/airframe/commit/242d3c0)]
-  airframe-okhttp: Shutdown OkHttpClient explicitly ([#1196](https://github.com/wvlet/airframe/issues/1196)) [[5917804](https://github.com/wvlet/airframe/commit/5917804)]
-  airframe-http: Retry on 408 request timeout ([#1218](https://github.com/wvlet/airframe/issues/1218)) [[ceea5b1](https://github.com/wvlet/airframe/commit/ceea5b1)]

### Other changes 

-  Upgrade to Scala 2.12.12, 2.13.3 ([#1199](https://github.com/wvlet/airframe/issues/1199)) [[b07b7ac](https://github.com/wvlet/airframe/commit/b07b7ac)]
-  airframe-http-okhttp: Update okhttp to 3.14.9 ([#1096](https://github.com/wvlet/airframe/issues/1096)) [[372f98d](https://github.com/wvlet/airframe/commit/372f98d)]
-  airframe-grpc: Add a simple finagle/grpc benchmark ([#1247](https://github.com/wvlet/airframe/issues/1247)) [[ba85551](https://github.com/wvlet/airframe/commit/ba85551)]
-  airframe-grpc: Support gRPC server interceptors ([#1246](https://github.com/wvlet/airframe/issues/1246)) [[53db2e0](https://github.com/wvlet/airframe/commit/53db2e0)]
-  airframe-grpc: Add async grpc client for streaming ([#1241](https://github.com/wvlet/airframe/issues/1241)) [[a1660cd](https://github.com/wvlet/airframe/commit/a1660cd)]
-  airframe-grpc: Support client, server, and bidirectional streaming ([#1239](https://github.com/wvlet/airframe/issues/1239)) [[bbfef41](https://github.com/wvlet/airframe/commit/bbfef41)]
-  airframe-grpc: Update grpc-netty-shaded, grpc-stub to 1.31.1 ([#1230](https://github.com/wvlet/airframe/issues/1230)) [[ede91c5](https://github.com/wvlet/airframe/commit/ede91c5)]
-  airframe-grpc: Generate both sync and async clients inside the same class ([#1224](https://github.com/wvlet/airframe/issues/1224)) [[a9bb18b](https://github.com/wvlet/airframe/commit/a9bb18b)]

### Internal Changes

-  airframe-grpc: Update grpc-netty-shaded, grpc-stub to 1.31.0 ([#1197](https://github.com/wvlet/airframe/issues/1197)) [[30eb3b9](https://github.com/wvlet/airframe/commit/30eb3b9)]
-  internal: Update presto-main to 340 ([#1219](https://github.com/wvlet/airframe/issues/1219)) [[9671111](https://github.com/wvlet/airframe/commit/9671111)]
-  Update jmh-core, jmh-generator-bytecode, ... to 1.25 ([#1231](https://github.com/wvlet/airframe/issues/1231)) [[7794143](https://github.com/wvlet/airframe/commit/7794143)]
-  airframe-http: Update swagger-parser to 2.0.21 ([#1193](https://github.com/wvlet/airframe/issues/1193)) [[f9fd30b](https://github.com/wvlet/airframe/commit/f9fd30b)]
-  Update sbt-scalafmt to 2.4.2 ([#1198](https://github.com/wvlet/airframe/issues/1198)) [[3a8660f](https://github.com/wvlet/airframe/commit/3a8660f)]
-  airframe-jdbc: Update postgresql to 42.2.15 ([#1236](https://github.com/wvlet/airframe/issues/1236)) [[3a1bc66](https://github.com/wvlet/airframe/commit/3a1bc66)]
-  airframe-jdbc: Update sqlite-jdbc to 3.32.3.2 ([#1195](https://github.com/wvlet/airframe/issues/1195)) [[07f42e1](https://github.com/wvlet/airframe/commit/07f42e1)]
-  Update scalajs-dom to 1.1.0 ([#1215](https://github.com/wvlet/airframe/issues/1215)) [[d0866a5](https://github.com/wvlet/airframe/commit/d0866a5)]
-  internal: Update sbt-buildinfo to 0.10.0 ([#1222](https://github.com/wvlet/airframe/issues/1222)) [[96e78f3](https://github.com/wvlet/airframe/commit/96e78f3)]

## 20.7.0

This release supports generating Open API schema from Aiframe HTTP/RPC interfaces by using sbt-airframe plugin. 

Changes:
-  airframe-http: Generate Open API schema from Endpoint/RPC interfaces ([#1178](https://github.com/wvlet/airframe/issues/1178)) [[8a55a5f](https://github.com/wvlet/airframe/commit/8a55a5f)]
-  airframe-http: Fix the issue that HTTP response headers except for Content-Type are ignored ([#1183](https://github.com/wvlet/airframe/issues/1183)) [[3f3ff3d](https://github.com/wvlet/airframe/commit/3f3ff3d)]
-  airframe-fluentd: Add convenient factories for initializing fluentd/td loggers ([#1190](https://github.com/wvlet/airframe/issues/1190)) [[60882c2](https://github.com/wvlet/airframe/commit/60882c2)]
-  Update sbt-scalajs, scalajs-compiler, ... to 1.1.1 ([#1173](https://github.com/wvlet/airframe/issues/1173)) [[c509dca](https://github.com/wvlet/airframe/commit/c509dca)]
-  Add Airframe RPC documentation ([#1174](https://github.com/wvlet/airframe/issues/1174)) [[d027772](https://github.com/wvlet/airframe/commit/d027772)]

Internal changes:
-  Update presto-main to 339 ([#1189](https://github.com/wvlet/airframe/issues/1189)) [[7a8a59f](https://github.com/wvlet/airframe/commit/7a8a59f)]
-  Update scalafmt-core to 2.6.3 ([#1181](https://github.com/wvlet/airframe/issues/1181)) [[5c5338a](https://github.com/wvlet/airframe/commit/5c5338a)]
-  Update sbt-sonatype to 3.9.4 ([#1177](https://github.com/wvlet/airframe/issues/1177)) [[d7e9f97](https://github.com/wvlet/airframe/commit/d7e9f97)]
-  Update sbt-dynver to 4.1.1 ([#1172](https://github.com/wvlet/airframe/issues/1172)) [[bcc7f12](https://github.com/wvlet/airframe/commit/bcc7f12)]

## 20.6.2

This is a minor bug fix and improvement release.  

-  airframe-codec: Support Either and Throwable class serde  ([#1163](https://github.com/wvlet/airframe/issues/1163)) [[68998fb](https://github.com/wvlet/airframe/commit/68998fb)]
-  airframe-http-okhttp: Add SocketTimeoutException as a retryable failure. ([#1170](https://github.com/wvlet/airframe/issues/1170)) [[e5cd4b8](https://github.com/wvlet/airframe/commit/e5cd4b8)]
-  airframe-http: Fixes [#1158](https://github.com/wvlet/airframe/issues/1158) Import higherKinds for generated AsyncHttpClient ([#1164](https://github.com/wvlet/airframe/issues/1164)) [[008366e](https://github.com/wvlet/airframe/commit/008366e)]
-  airframe-msgpack: [#1159](https://github.com/wvlet/airframe/issues/1159) Support packTimestamp(unixtime) to use timestamp32 ([#1160](https://github.com/wvlet/airframe/issues/1160)) [[b49a79f](https://github.com/wvlet/airframe/commit/b49a79f)]

## 20.6.1

This release introduced URLConnection-based HTTP client for handy http request testing, and improved RPC request handling to support arbitrary number of RPC function arguments.   

-  airframe-di: Add .afterStart lifecycle hook ([#1153](https://github.com/wvlet/airframe/issues/1153)) [[c6defb2](https://github.com/wvlet/airframe/commit/c6defb2)]
-  airframe-http: [#1131](https://github.com/wvlet/airframe/issues/1131) Add a sync client using URLConnection  ([#1134](https://github.com/wvlet/airframe/issues/1134)) [[b70cdbd](https://github.com/wvlet/airframe/commit/b70cdbd)]
-  airframe-http: [#1100](https://github.com/wvlet/airframe/issues/1100) Support unary and n-ary functions in RPC  ([#1126](https://github.com/wvlet/airframe/issues/1126)) [[0d2f280](https://github.com/wvlet/airframe/commit/0d2f280)]
-  airframe-http: Various http client fixes ([#1142](https://github.com/wvlet/airframe/issues/1142)) [[3d78428](https://github.com/wvlet/airframe/commit/3d78428)]
-  airframe-http: [#1129](https://github.com/wvlet/airframe/issues/1129) Deprecate no-key unary mapping for RPC ([#1143](https://github.com/wvlet/airframe/issues/1143)) [[a2f6b70](https://github.com/wvlet/airframe/commit/a2f6b70)]
-  airframe-http: [#1128](https://github.com/wvlet/airframe/issues/1128) Add Router.verifyRoutes to check duplicated endpoints ([#1137](https://github.com/wvlet/airframe/issues/1137)) [[c6768ed](https://github.com/wvlet/airframe/commit/c6768ed)]
-  airframe-http-rx: [#1117](https://github.com/wvlet/airframe/issues/1117) Add RxVar.forceSet and forceUpdate ([#1144](https://github.com/wvlet/airframe/issues/1144)) [[3b8887c](https://github.com/wvlet/airframe/commit/3b8887c)]
-  airframe-jdbc: Update sqlite-jdbc to 3.32.3 ([#1150](https://github.com/wvlet/airframe/issues/1150)) [[dc00d2c](https://github.com/wvlet/airframe/commit/dc00d2c)]

Internal changes:
-  Support Metals + Bloop build ([#1141](https://github.com/wvlet/airframe/issues/1141)) [[fa3c458](https://github.com/wvlet/airframe/commit/fa3c458)]
-  Update presto-main to 336 ([#1140](https://github.com/wvlet/airframe/issues/1140)) [[9dfefc2](https://github.com/wvlet/airframe/commit/9dfefc2)]
-  Update scalafmt-core to 2.6.0 ([#1139](https://github.com/wvlet/airframe/issues/1139)) [[002e9a1](https://github.com/wvlet/airframe/commit/002e9a1)]
-  Update spark-sql to 2.4.6 ([#1122](https://github.com/wvlet/airframe/issues/1122)) [[97de722](https://github.com/wvlet/airframe/commit/97de722)]
-  Update json4s-jackson to 3.6.9 ([#1138](https://github.com/wvlet/airframe/issues/1138)) [[f8be3e1](https://github.com/wvlet/airframe/commit/f8be3e1)]
-  Update postgresql to 42.2.14 ([#1125](https://github.com/wvlet/airframe/issues/1125)) [[13d89c1](https://github.com/wvlet/airframe/commit/13d89c1)]
-  Upgrade to sbt 1.3.12 with Scala.js build fixes ([#1124](https://github.com/wvlet/airframe/issues/1124)) [[4e7611f](https://github.com/wvlet/airframe/commit/4e7611f)]

## 20.6.0

-  airframe-codec: [#1091](https://github.com/wvlet/airframe/issues/1091) Support complex types in AnyCodec ([#1098](https://github.com/wvlet/airframe/issues/1098)) [[d333515](https://github.com/wvlet/airframe/commit/d333515)]
-  airframe-http: [#1112](https://github.com/wvlet/airframe/issues/1112) Report RPC interface class in the http_access logs ([#1114](https://github.com/wvlet/airframe/issues/1114)) [[cb0edbe](https://github.com/wvlet/airframe/commit/cb0edbe)]
-  airframe-json: Add convenient operations for JSONValues ([#1123](https://github.com/wvlet/airframe/issues/1123)) [[4675f09](https://github.com/wvlet/airframe/commit/4675f09)]
-  airframe-jdbc: Update postgresql to 42.2.13 ([#1121](https://github.com/wvlet/airframe/issues/1121)) [[4bf5cfb](https://github.com/wvlet/airframe/commit/4bf5cfb)]
-  airframe-metrics: Add Count (succinct counter) ([#1109](https://github.com/wvlet/airframe/issues/1109)) [[cb6d659](https://github.com/wvlet/airframe/commit/cb6d659)]


## 20.5.2

Airframe 20.5.2 added [a built-in http/RPC request logging](https://wvlet.org/airframe/docs/airframe-http#access-logs) and fixed several http request handling bugs. We also upgraded to Scala.js 1.1.0, whose binary is compatible with Scala.js 1.0.x.

airframe-codec is enhanced to support mapping from JSON array strings to Array[X] types. 

-  airframe-http: [#399](https://github.com/wvlet/airframe/issues/399) Add a standard JSON request logger ([#1073](https://github.com/wvlet/airframe/issues/1073)) [[22716fb](https://github.com/wvlet/airframe/commit/22716fb)]
-  airframe-http: Add RPC call logging ([#1076](https://github.com/wvlet/airframe/issues/1076)) [[119ada2](https://github.com/wvlet/airframe/commit/119ada2)]
-  airframe-http: Suppress request objects from rpc_args logs ([#1090](https://github.com/wvlet/airframe/issues/1090)) [[cfb0fe0](https://github.com/wvlet/airframe/commit/cfb0fe0)]
-  airframe-http: Retry on Finagle's ChannelClosedException ([#1089](https://github.com/wvlet/airframe/issues/1089)) [[ca4980c](https://github.com/wvlet/airframe/commit/ca4980c)]
-  airframe-http: [#1086](https://github.com/wvlet/airframe/issues/1086) Support query_string mapping for non GET requests ([#1087](https://github.com/wvlet/airframe/issues/1087)) [[2b77ad5](https://github.com/wvlet/airframe/commit/2b77ad5)]
-  airframe-http: Fix query_string parsing of HttpMessage.Request ([#1085](https://github.com/wvlet/airframe/issues/1085)) [[5e6d52f](https://github.com/wvlet/airframe/commit/5e6d52f)]
-  airframe-http: Report exception traces inside RPC/Endpoint methods ([#1081](https://github.com/wvlet/airframe/issues/1081)) [[217bc88](https://github.com/wvlet/airframe/commit/217bc88)]
-  airframe-http: Evaluate request/response body lazily ([#1074](https://github.com/wvlet/airframe/issues/1074)) [[762ee16](https://github.com/wvlet/airframe/commit/762ee16)]
-  airframe-http-rx: Fix EntityRef rendering ([#1075](https://github.com/wvlet/airframe/issues/1075)) [[079ac12](https://github.com/wvlet/airframe/commit/079ac12)]
-  airframe-surface, airframe-codec: [#1092](https://github.com/wvlet/airframe/issues/1092) Support Enum-like classes in Scala.js ([#1099](https://github.com/wvlet/airframe/issues/1099)) [[7ee82d7](https://github.com/wvlet/airframe/commit/7ee82d7)]
-  Update sbt-scalajs, scalajs-compiler, ... to 1.1.0 ([#1088](https://github.com/wvlet/airframe/issues/1088)) [[fa6bc92](https://github.com/wvlet/airframe/commit/fa6bc92)]
-  airframe-codec: [#1083](https://github.com/wvlet/airframe/issues/1083) Support JSON arrays to Array[X] mapping ([#1097](https://github.com/wvlet/airframe/issues/1097)) [[3a2d703](https://github.com/wvlet/airframe/commit/3a2d703)]
-  airframe-codec: [#1093](https://github.com/wvlet/airframe/issues/1093) Add MessageCodec.fromString(String) ([#1095](https://github.com/wvlet/airframe/issues/1095)) [[5cbdcc1](https://github.com/wvlet/airframe/commit/5cbdcc1)]
-  airframe-http-okhttp: Update okhttp to 3.12.12 ([#1094](https://github.com/wvlet/airframe/issues/1094)) [[93fa2eb](https://github.com/wvlet/airframe/commit/93fa2eb)]

## 20.5.1

-  airframe-http: Read error response body and headers properly in Scala.js ([#1069](https://github.com/wvlet/airframe/issues/1069)) [[8936055](https://github.com/wvlet/airframe/commit/8936055)]
-  airframe-di: [#1068](https://github.com/wvlet/airframe/issues/1068) Do not close loggers upon session shutdown ([#1070](https://github.com/wvlet/airframe/issues/1070)) [[af323d2](https://github.com/wvlet/airframe/commit/af323d2)]

## 20.5.0

Now that Scala.js 1.0.x is publicly available, we will drop support for Scala.js 0.6.x since 20.5.0.

-  airframe-http: Fix a bug of JSHttpClient when the port number is empty string ([#1060](https://github.com/wvlet/airframe/issues/1060)) [[6015849](https://github.com/wvlet/airframe/commit/6015849)]
-  airframe-http-finagle: Update finagle-core, finagle-http, ... to 20.4.1 ([#1045](https://github.com/wvlet/airframe/issues/1045)) [[0a52b93](https://github.com/wvlet/airframe/commit/0a52b93)]
-  airframe-http-okhttp: Update okhttp to 3.12.11 ([#1047](https://github.com/wvlet/airframe/issues/1047)) [[35a5008](https://github.com/wvlet/airframe/commit/35a5008)]
-  airframe-jdbc: Update HikariCP to 3.4.3 ([#1044](https://github.com/wvlet/airframe/issues/1044)) [[4a18359](https://github.com/wvlet/airframe/commit/4a18359)]

Minor changes:
-  Upgrade to Scala 2.13.2 ([#1042](https://github.com/wvlet/airframe/issues/1042)) [[3599f05](https://github.com/wvlet/airframe/commit/3599f05)]
-  Update scala-collection-compat to 2.1.6 ([#1040](https://github.com/wvlet/airframe/issues/1040)) [[2ab080d](https://github.com/wvlet/airframe/commit/2ab080d)]

Internal changes:
-  Update scalajs-env-jsdom-nodejs to 1.1.0 ([#1057](https://github.com/wvlet/airframe/issues/1057)) [[d018032](https://github.com/wvlet/airframe/commit/d018032)]
-  Update sqlite-jdbc to 3.31.1 ([#1053](https://github.com/wvlet/airframe/issues/1053)) [[cba770b](https://github.com/wvlet/airframe/commit/cba770b)]
-  Update presto-main to 333 ([#1054](https://github.com/wvlet/airframe/issues/1054)) [[c423478](https://github.com/wvlet/airframe/commit/c423478)]
-  Update json4s-jackson to 3.6.8 ([#1055](https://github.com/wvlet/airframe/issues/1055)) [[d31dd7b](https://github.com/wvlet/airframe/commit/d31dd7b)]

## 20.4.1

-  airframe-http: [#1025](https://github.com/wvlet/airframe/issues/1025) Support custom server exception response ([#1036](https://github.com/wvlet/airframe/issues/1036)) [[d3a872a](https://github.com/wvlet/airframe/commit/d3a872a)]
-  airframe-http-rx: Support Rx.filter ([#1030](https://github.com/wvlet/airframe/issues/1030)) [[b619bfe](https://github.com/wvlet/airframe/commit/b619bfe)]
-  sbt-airframe: Fixes client code generation when companion objects of API classes exist [#1034](https://github.com/wvlet/airframe/issues/1034) ([#1035](https://github.com/wvlet/airframe/issues/1035)) [[abe4167](https://github.com/wvlet/airframe/commit/abe4167)]

Internal changes:
-  Update scala-collection-compat to 2.1.5 ([#1038](https://github.com/wvlet/airframe/issues/1038)) [[1cef7ca](https://github.com/wvlet/airframe/commit/1cef7ca)]
-  Update sbt, scripted-plugin to 1.3.10 ([#1037](https://github.com/wvlet/airframe/issues/1037)) [[13bf484](https://github.com/wvlet/airframe/commit/13bf484)]
-  Update sbt-scalafmt to 2.3.4 ([#1033](https://github.com/wvlet/airframe/issues/1033)) [[d6be5de](https://github.com/wvlet/airframe/commit/d6be5de)]
-  Update presto-main to 332 ([#1031](https://github.com/wvlet/airframe/issues/1031)) [[381b886](https://github.com/wvlet/airframe/commit/381b886)]

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
