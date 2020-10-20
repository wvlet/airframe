Hello RPC
--- 

Compile:
```
$ sbt
> pack 
```

RPC (Finagle Backend):
```
$ ./target/pack/bin/greeter-main finagleServer

# In another terminal
$ ./target/pack/bin/greeter-main finagleClient
```

RPC (gRPC Backend):
```
$ ./target/pack/bin/greeter-main grpcServer

# In another terminal
$ ./target/pack/bin/greeter-main grpcClient
```

