---
id: airframe-canvas
title: "airframe-canvas: Off-Heap Memory Manager"
---

airframe-canvas is a library for managing large off-heap memory (called Canvas) of more than 2G (2^31) size, 
which is the limit of JVM byte arrays.



With airframe-canvas:
- You can save the CPU cost of zero-filing when initializing arrays.
- Off-heap memory canvases are managed outside JVM heap memory, 
so you can allocate a larger memory region than the value specified with `-Xmx` JVM option.
- You can quickly release allocated memory with `Canvas.release`. 
- Even if you forget to release canvases, the allocated memory can be released upon GC. 

airframe-canvas is a successor of [xerial/larray](https://github.com/xerial/larray).


# Usage

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.wvlet.airframe/airframe-canvas_2.12/badge.svg)](http://central.maven.org/maven2/org/wvlet/airframe/airframe-canvas_2.12/)

**build.sbt**
```scala
libraryDependencies += "org.wvlet.airframe" %% "airframe-canvas" % "(version)"
```


`Canvas` object has various factory methods for creating canvas objects
from scratch, or based on existing byte arrays (e.g., Array[Byte], ByteBuffer, etc.)

```scala
import wvlet.airframe.canvas.Canvas

val c = Canvas.newOffHeapCanvas(512)
c.writeInt(0, 10)
val i = c.readInt(0) // 10

// Release the buffer
c.close()
```
