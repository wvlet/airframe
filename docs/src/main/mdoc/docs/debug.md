---
layout: docs
title: Debugging DI
---

## Debugging DI

To check the runtime behavior of Airframe's dependency injection, set the log level of `wvlet.airframe` to `debug` or `trace`:

**src/main/resources/log.properties**
```
wvlet.airframe=debug
```

While debugging the code in your test cases, you can also use `log-test.properties` file:
**src/test/resources/log-test.properties**
```
wvlet.airframe=debug
```
See [airframe-log configuration](https://github.com/wvlet/airframe/blob/master/log/README.md#configuring-log-levels) for the details of log level configurations.


Then you will see the log messages that show the object bindings and injection activities:
```
2016-12-29 22:23:17-0800 debug [Design] Add binding: ProviderBinding(DependencyFactory(PlaneType,List(),wvlet.airframe.LazyF0@442b0f),true,true)  - (Design.scala:43)
2016-12-29 22:23:17-0800 debug [Design] Add binding: ProviderBinding(DependencyFactory(Metric,List(),wvlet.airframe.LazyF0@1595a8db),true,true)  - (Design.scala:43)
2016-12-29 22:23:17-0800 debug [Design] Add binding: ClassBinding(Engine,GasolineEngine)  - (Design.scala:43)
2016-12-29 22:23:17-0800 debug [Design] Add binding: ProviderBinding(DependencyFactory(PlaneType,List(),wvlet.airframe.LazyF0@b24c12d8),true,true)  - (Design.scala:43)
2016-12-29 22:23:17-0800 debug [Design] Add binding: ClassBinding(Engine,SolarHybridEngine)  - (Design.scala:43)
2016-12-29 22:23:17-0800 debug [SessionBuilder] Creating a new session: session:7bf38868  - (SessionBuilder.scala:48)
2016-12-29 22:23:17-0800 debug [SessionImpl] [session:7bf38868] Initializing  - (SessionImpl.scala:48)
2016-12-29 22:23:17-0800 debug [SessionImpl] [session:7bf38868] Completed the initialization  - (SessionImpl.scala:55)
2016-12-29 22:23:17-0800 debug [SessionImpl] Get or update dependency [AirPlane]  - (SessionImpl.scala:80)
2016-12-29 22:23:17-0800 debug [SessionImpl] Get dependency [wvlet.obj.tag.@@[example.Example.Wing,example.Example.Left]]  - (SessionImpl.scala:60)
2016-12-29 22:23:17-0800 debug [SessionImpl] Get dependency [wvlet.obj.tag.@@[example.Example.Wing,example.Example.Right]]  - (SessionImpl.scala:60)
2016-12-29 22:23:17-0800 debug [SessionImpl] Get dependency [example.Example.Engine]  - (SessionImpl.scala:60)
2016-12-29 22:23:17-0800 debug [SessionImpl] Get or update dependency [Fuel]  - (SessionImpl.scala:80)
2016-12-29 22:23:17-0800 debug [SessionImpl] Get dependency [example.Example.PlaneType]  - (SessionImpl.scala:60)
2016-12-29 22:23:17-0800 debug [SessionImpl] Get dependency [example.Example.Metric]  - (SessionImpl.scala:60)
```
