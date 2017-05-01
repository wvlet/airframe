---
layout: docs
title: Life Cycle Management
---

## Life Cycle Management

Server-side applications often require object initializtaion, server start, and shutdown hooks.
Airframe has a built-in object life cycle manager to implement these hooks:

```scala
// Your server application
trait Server {
  def init() {}
  def start() {}
  def stop() {}
}

// When binding an object, you can define life cycle hooks to the injected object:
trait MyServerService {
  val service = bind[Server].withLifeCycle(
    init = { _.init },    // Called when the object is initialized (called only once for singleton)
    start = { _.start },  // Called when sesion.start is called
    shutdown = { _.stop } // Called when session.shutdown is called
  )
}
```

You can also use onInit/onStart/onShutdown methods:
```
bind[X]
  .onInit { x => ... }
  .onStart { x => ... }
  .onShutdown { x => ... }
```