Hello RPC
--- 

An example project that shows how to use RPC in Scala 3.

Build the project:
```
$ sbt
> pack 
```

RPC (Netty Backend) test:
```
# Start a server
$ ./target/pack/bin/greeter-main server
```

```
# In another terminal, send an RPC client
$ ./target/pack/bin/greeter-main client
2023-04-27 10:34:09.510-0700  info [GreeterMain] Received: Hello RPC0!  - (GreeterMain.scala:62)
2023-04-27 10:34:09.527-0700  info [GreeterMain] Received: Hello RPC1!  - (GreeterMain.scala:62)
2023-04-27 10:34:09.531-0700  info [GreeterMain] Received: Hello RPC2!  - (GreeterMain.scala:62)
```
