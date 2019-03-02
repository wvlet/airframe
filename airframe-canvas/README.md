Airframe Canvas
===

airframe-canvas is a library for managing large off-heap memory (called Canvas) of more than 2G (2^31) size, 
which is the limit of JVM byte arrays.

With airframe-canvas:
- You can save the CPU cost of zero-filing when initializing arrays.
- Off-heap memory canvases are managed outside JVM heap memory, 
so you can allocate a larger memory region than the value specified with `-Xmx` JVM option.
- You can quickly release allocated memory with `Canvas.release`. 
- Even if you forget to release canvases, the allocated memory can be released upon GC. 


airframe-canvas is a successor of [xerial/larray](https://github.com/xerial/larray).
