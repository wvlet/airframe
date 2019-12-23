---
id: comparison
layout: docs
title: DI Framework Comparison
---

There are two types of dependency injection approaches; **runtime** and **compile-time** (static) DIs.

## Run-time Dependency Injection

- [Google Guice](https://github.com/google/guice) is a popular run-time dependency injection libraries in Java, which is also used in [Presto](https://github.com/prestodb/presto) to construct a distributed SQL engine consisting of hundreds of classes. Guice itself does not manage the lifecycle of objects and binding configurations, so Presto team at Facebook has developed [airlift-bootstrap](https://github.com/airlift/airlift/tree/master/bootstrap/src/main/java/io/airlift/bootstrap) and [airlift- configuration](https://github.com/airlift/airlift/tree/master/configuration/src/main/java/io/airlift/configuration) libraries to extend Guice's functionality.
   - One of the disadvantages of Guice is it requires constructor annotation like `@Inject`. This is less convenient if you are using third-party libraries, which cannot add such annotations. So you often need to write many provider binding modules to use third-party classes.
   - Airframe has provider bindings in `bind { d1: D1 => new X(d1) }` syntax so that you can directly call the constructor of third-party classes. No need to implement object binding modules.
    - [Guice Bootstrap](https://github.com/embulk/guice-bootstrap) is an extension of Guice to support life-cycle management using `@PostConstruct` and `@PreDestroy` annotations.

- [Scaldi](https://github.com/scaldi/scaldi) is an early adaptor of Guice like DI for Scala and has implemented all of the major functionalities of Guice. However it requires extending your class with Scaldi `Module`. Airframe is simplifying it so that you only need to use `bind[X]` without extending any trait.

- [Grafter](https://github.com/zalando/grafter) is a DI library focusing on constructor injections for Scala.

- [Spring IoC container](https://docs.spring.io/spring/docs/current/spring-framework-reference/html/beans.html) Spring framework popularized the notion of [Inversion of Control](https://martinfowler.com/articles/injection.html), which is now simply called DI. Spring uses XML based configuration, which is less programmer-friendly, but it has been useful to manage ordering of object initializations outside the program. Note: Spring framework has stopped supporting Scala.

- [Weld](http://weld.cdi-spec.org/) is a reference implementation of [Contexts & Dependency Injection for Java (CDP)](http://cdi-spec.org/) specifications for managing object life cycle and DI for Java EE applications. Weld, for example, has HTTP request scoped object life cycle, annotations for describing how to inject dependencies, etc.

## Compile-time Dependency Injection

- [MacWire](https://github.com/adamw/macwire) is a compile-time dependency injection library for Scala using `wire[A]` syntax.
MacWire ensures all binding types are available at compile time, so if some dependency is missing, it will be shown as a compile error. That is a major advantage of MacWire. On the other hand it sacrifices dynamic binding; For example, we cannot switch the implementation of `wire[A]` to `class AImpl(d1:D1, d2:D2, d3:D3) extends A`, because we cannot statically resolve dependencies from A to D1, D2, and D3 at compile time. And also for implementing lifecycle management like `onStart` and `onShutdown` hooks, MacWire needs to implement [interceptors](https://github.com/adamw/macwire#interceptors). This requires [Javassist](http://jboss-javassist.github.io/javassist/) library and doesn't work in Scala.js.

- [Dagger2](https://github.com/google/dagger) is also a compile-time dependency injection library for Java and Android. Google needed binding hundreds of modules, but Guice only resolves these dependencies at runtime, so binding failures can be found later when the application is running. To resolve this, Dagger2 tries to generate dependency injection code at compile time. [This document](https://google.github.io/dagger/users-guide) is a good read to understand the background of why compile-time DI was necessary.

Both of MacWire and Dagger2 requires all of the dependencies should be found in the same scope. There are pros and cons in this approach; A complex example is [Guardian](https://github.com/guardian/frontend)'s [frontend code](https://github.com/guardian/frontend/blob/06b94f88593e68682fb2a03c6d878947f8472d44/admin/app/controllers/AdminControllers.scala), which lists 30 dependencies, including transitive dependencies, in a single trait to resolve dependencies at compile time. In runtime DI, we only need to write direct dependencies.

## Pure-Scala Approaches

There are several pure-Scala approaches for DI. [Cake Pattern](https://www.cakesolutions.net/teamblogs/2011/12/19/cake-pattern-in-depth) was born around 2010 and introduced the notion of type abstraction and cake composition. But generally speaking cake pattern adds substantial complexity to your program and have many pitfalls as described in the following blog post:
- [Cake antipattern](https://kubuszok.com/2018/cake-antipattern/)

[Reader Monad](https://medium.com/@AyacheKhettar/using-cat-data-reader-monad-d70269fc451f) is another design pattern in Scala, which cascades dependency passing using nested functions. But it has some performance overhead and it makes the scope of
dependencies ambiguous.

[Dependency Injection in Functional Programming](https://gist.github.com/gvolpe/1454db0ed9476ed0189dcc016fd758aa) is one of the best practices of pure-Scala DI, which doesn't rely on any framework. To manage lifecycle of objects, this approach needs to use IO Monad library like [Cats Effect](https://typelevel.org/cats-effect/).

## Feature Matrix of DI Frameworks

The chart below shows major features supported in selected DI frameworks. For comparison, pure-Scala approach is also added. Key questions in choosing a DI framework (or not using it) would be as follows:

- __Do you need auto-wiring__?
  - If passing dependency objects between your classes is not so cumbersome, pure-Scala approach will fit. If you need to wire hundreds of objects including configuration objects and service modules, DI frameworks will reduce the amount of hand-written code.
- __Which do you need most? Compile-time dependency check or dynamic-type binding?__
  - If you have rarely used modules and it is difficult to write exhaustive tests for checking the presence of all possible dependencies, compile-time dependency check will protect you from missing dependency errors at runtime.
  - If you need to switch the behavior of some modules according to the environment (e.g., test, production, or specialized environment, etc.), runtime DI is a natural choice.
- __Do you need object life-cycle management support?__
  - If you need to lazily initialize objects (e.g., for testing), but want to eagerly initialize all objects for production, Airframe has rich support of life-cycle management.
  - If you have several services that need to be properly started/closed, having life cycle hooks (onStart/onShutdown, etc.) will be convenient.

| Feature    | [Airframe](https://github.com/wvlet/airframe) | [Google Guice](https://github.com/google/guice)| [MacWire](https://github.com/adamw/macwire) |  [Pure Scala](https://gist.github.com/gvolpe/1454db0ed9476ed0189dcc016fd758aa)   |
|-----------------------|:---------:|:--------------------------------:|:-------:|:----------------:|
| Auto-wiring           |   ✓     |    ✓   |    ✓   |   (Manual wiring) |
| Compile-time dependency check    |    |   |  ✓  |  ✓  |
| Dynamic-type binding  |   ✓    | ✓   |     |  ✓ (using [implicit parameters](https://gist.github.com/gvolpe/1454db0ed9476ed0189dcc016fd758aa#the-fp-way-2))    |
| [Constructor injection](airframe.md#bind) |   ✓    | ✓ (Require `@Inject` annotation)  | ✓     | ✓ (manual argument passing)  |
| [In-trait injection](airframe.md#bind) (mix-in support)  |   ✓    |    (Java has no trait)    | ✓       | ✓ (manual override)  |
| [Life-cycle management](airframe.md#life-cycle) (On start/inject/shutdown hooks) |   ✓    | (Need an extension like [airlift](https://github.com/airlift/airlift/tree/master/bootstrap/src/main/java/io/airlift/bootstrap)) | limited (inject interceptor using reflection)| (Need to use IO Monad library like [Cats Effect](https://typelevel.org/cats-effect/)) |
| [Lazy/eager initialization switch](airframe.md#life-cycle)  |  ✓     |  ✓ (with [Stage](https://github.com/google/guice/wiki/Bootstrap))    |  (lazy only) |  (lazy only)  |
| [Multi-bindings](airframe.md#multi-binding) |✓ (Just Scala) | ✓ |✓ (Just Scala) |✓ (Just Scala)
| [Tagged type/alias bindings](airframe.md#advanced-binding-types)   |✓ | limited (Need to define new annotations) |✓|✓ (manual binding) |
| [Generic type bindings](http://wvlet.org/airframe/docs/bindings.html#generic-type-binding) | ✓ |  (Type erasure) |  | ✓ (manual binding) |
| Provider bindings | ✓ | ✓ (Need to define special provider classes) | ✓ (wireWith) | limited (Need to use `implicits`) |
| Scala.js support | ✓ |    | limited (reflection-based interceptor cannot be used) | ✓ |


## Summary

- Compile-time dependency injection:
  - libraries: Macwire, Dagger2, etc.
  - **pros**: Can validate the presence of dependencies at compile time.
  - **cons**: Less flexible (e.g., No dynamic type binding)
  - **cons**: Need to enumerate all dependencies in the same scope (lengthy code).
  - **cons**: Hard to implement life cycle management (e.g., onStart, onShutdown, etc.).

- Run-time dependency injection
  - libraries: Airframe, Google Guice, etc.
  - **pros**: Allows dynamic type binding.
  - **pros**: Simpler binding codes. Only need to bind direct dependencies.
  - **pros**: Can customize object life cycle events through session (Airframe)or inject event handler (Guice).
  - **cons**: Missed binding founds as a runtime error.

- Pure-Scala approach
  - **pros**: It's just Scala! No special extension is required.
  - **pros**: Since all objects are manually wired, missing dependencies will be reported as compile errors.
  - **cons**: Requires manual binding and overrides of classes.
  - **cons**: Need to think about how to pass explicit/implicit parameters to classes and traits.

