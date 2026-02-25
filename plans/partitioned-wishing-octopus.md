# Handle PrematureChannelClosureException as benign I/O error

## Context

In production, when a client disconnects while the server is still aggregating an HTTP request (e.g., during a large POST body upload), Netty's `HttpObjectAggregator` throws:

```
io.netty.handler.codec.PrematureChannelClosureException: Channel closed while still aggregating message
```

This is a normal, benign client disconnection scenario (same category as "Connection reset" or `ClosedChannelException`), but it's currently logged at WARN level because `isBenignIOException()` doesn't recognize it.

**Root cause**: `PrematureChannelClosureException` extends `CodecException` extends `Exception` — it is NOT an `IOException` or `ClosedChannelException`, and it doesn't wrap one in its cause chain. So the current cause-chain traversal in `isBenignIOException()` never matches it.

## Changes

### 1. Add `PrematureChannelClosureException` to `isBenignIOException()`

**File**: `airframe-http-netty/src/main/scala/wvlet/airframe/http/netty/NettyRequestHandler.scala`

Add a case match in the `loop` function inside `isBenignIOException()`:

```scala
case _: io.netty.handler.codec.PrematureChannelClosureException =>
  true
```

This should be added before the `ClosedChannelException` case for readability (Netty-specific exceptions first, then JDK exceptions).

### 2. Add unit tests for `isBenignIOException()`

**File**: `airframe-http-netty/src/test/scala/wvlet/airframe/http/netty/BenignIOExceptionTest.scala`

Add test cases:
- `PrematureChannelClosureException` directly → benign
- `ClosedChannelException` directly → benign
- `IOException("Connection reset")` → benign
- `IOException("Broken pipe")` → benign
- `RuntimeException` wrapping `PrematureChannelClosureException` → benign (cause chain)
- `RuntimeException("some other error")` → NOT benign
- `null` cause → NOT benign

## Verification

```bash
# Compile
./sbt "httpNettyJVM/compile"

# Run the new test
./sbt 'httpNettyJVM/testOnly *BenignIOExceptionTest'

# Format
./sbt "httpNettyJVM/scalafmtAll"
```
