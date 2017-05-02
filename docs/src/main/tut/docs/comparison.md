---
layout: docs
title: Other DI Frameworks
---

# Comparison with the other DI frameworks

There are two types of dependency injection approaches; runtime and compile-time (static) DIs.

## Run-time Dependency Injection

- [Google Guice](https://github.com/google/guice) is a popular run-time dependency injection libraries in Java, which is also used in [Presto](https://github.com/prestodb/presto) to construct a distributed SQL engine consisting of hundreds of classes. Guice itself does not manage the lifecycle of objects and binding configurations, so Presto team at Facebook has developed [airlift-bootstrap](https://github.com/airlift/airlift/tree/master/bootstrap/src/main/java/io/airlift/bootstrap) and [airlift- configuration](https://github.com/airlift/airlift/tree/master/configuration/src/main/java/io/airlift/configuration) libraries to extend Guice's functionality.
   - One of the disadvantages of Guice is it requires constructor annotation like `@Inject`. This is less convenient if you are using third-party libraries, which cannot add such annotations. So you often need to write a provider binding modules to use third-party classes.
   - Airframe has provider bindings in `bind { d1: D1 => new X(d1) }` syntax so that you can directly call the constructor of third-party classes. No need to implement object binding modules.

- [Scaldi](https://github.com/scaldi/scaldi) is an early adaptor of Guice like DI for Scala and has implemented all of the major functionalities of Guice. However it requires extending your class with Scaldi `Module`. Airframe is simplifying it so that you only need to use `bind[X]` without extending any trait.

- [Spring IoC container](https://docs.spring.io/spring/docs/current/spring-framework-reference/html/beans.html) Spring framework popularized the notion of [Inversion of Control](https://martinfowler.com/articles/injection.html), which is now simply called DI. Spring uses XML based configuration, which is less programmer-friendly, but it has been useful to manage ordering of object initializations outside the program.

- [Weld](http://weld.cdi-spec.org/) is a reference implementation of [Contexts & Dependency Injection for Java (CDP)](http://cdi-spec.org/) specifications for managing object life cycle and DI for Java EE applications. Weld, for example, has HTTP request scoped object life cycle, annotations for describing how to inject dependencies, etc.

## Compile-time Dependency Injection

- [MacWire](https://github.com/adamw/macwire) is a compile-time dependency injection library for Scala using `wire[A]` syntax. MacWire ensures all binding types are available at compile time, so if some dependency is missing, it will be shown as a compile error. That is a major advantage of MacWire. On the other hand it sacrifices dynamic binding; For example, we cannot switch the implementation of `wire[A]` to `class AImpl(d1:D1, d2:D2, d3:D3) extends A`, because we cannot statically resolve dependencies to AImpl, D1, D2, and D3 at compile time.

- [Dagger2](https://github.com/google/dagger) is also a compile-time dependency injection library for Java and Android. Google needed binding hundreds of modules, but Guice only resolves these dependencies at runtime, so binding failures can be found later when the application is running. To resolve this, Dagger2 tries to generate dependency injection code at compile time. [This document](https://google.github.io/dagger/users-guide) is a good read to understand the background of why compile-time DI was necessary.

Both of MacWire and Dagger2 requires all of the dependencies should be found in the same scope. There are pros and cons in this approach; A complex example is [Guardian](https://github.com/guardian/frontend)'s [frontend code](https://github.com/guardian/frontend/blob/06b94f88593e68682fb2a03c6d878947f8472d44/admin/app/controllers/AdminControllers.scala), which lists 30 dependencies, including transitive dependenceis, in a single trait to resolve dependencies at compile time. In runtime DI, we only need to write direct dependencies.

## Summary

- Compile-time dependency injection:
  - [pros] Can validate the presence of dependencies at compile time.
  - [pros] Fast since all binding codes are generated at compile time.
  - [cons] Less flexible (e.g., No dynamic type binding)
  - [cons] Need to enumerate all dependencies in the same scope (lengthy code).

- Run-time dependency injection
  - [pros] Allows dynamic type binding.
  - [pros] Simpler code. Only need to bind direct dependencies.
  - [cons] Missed binding founds as a runtime error

Airframe belongs to a runtime dependency injection library, and resolves several short-comings of Google Guice (lack of lifecycle manager, difficulty of binding third-party objects, etc.). We also have implemented Scala-friendly DI syntax in Airframe. For the performance reason, Airframe uses Scala macros to generate binding code as much as possible (except dynamic type binding, which cannot be found at compile-time). To use Airframe, you don't need to understand the whole concept of DI and features in the existing DI frameworks. Just `bind`-`design`-`build` objects. That is all you need to know!
