airframe-benchmark
===

Airframe benchmark program based on [JMH](https://openjdk.java.net/projects/code-tools/jmh/).

This module has two objectives:
- Measure the machine throughput to see CPU performance.
- Provide a guideline to eliminate overheads in airframe-json/msgpack modules 
by comparing them with the standard msgpack/json processing libraries. 

### Download

- Download the latest tar.gz package from [here](https://oss.sonatype.org/content/repositories/snapshots/org/wvlet/airframe/airframe-benchmark_2.12/)
- Download this archive, and unpack it with `tar xvfz`
- Move to the unpacked folder, then run `./bin/airframe-benchmark bench (msgpack or json)`

### Running the benchmark
```
$ cd airframe-benchmark

# Run benchmark quickly
$ ./bin/airframe-benchmark bench-quick (msgpack or json)

# Run MessagePack benchmark
$ ./bin/airframe-benchmark bench msgpack

# Run JSON benchmark
$ ./bin/airframe-benchmark bench json

# Run Airframe HTTP/RPC benchmark
$ ./bin/airframe-benchmark bench http

# Run MessagePack benchmaark and write the results to a json file
# Run MessagePack benchmark
$ ./bin/airframe-benchmark bench msgpack -f json
...

# Run complete. Total time: 00:00:31

REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
experiments, perform baseline and negative tests that provide experimental control, make sure
the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
Do not assume the numbers tell you what you want them to tell.

Benchmark                             Mode  Cnt       Score       Error  Units
AirframeMsgpack.unpack               thrpt   50  379603.260 ± 13352.047  ops/s
AirframeMsgpack.unpack:booleanArray  thrpt   50  171991.362 ±  6673.264  ops/s
AirframeMsgpack.unpack:floatArray    thrpt   50  134988.640 ±  3069.660  ops/s
AirframeMsgpack.unpack:intArray      thrpt   50   72623.258 ±  7732.559  ops/s
MessagePackJava.unpack               thrpt   50  463493.986 ± 15380.526  ops/s
MessagePackJava.unpack:booleanArray  thrpt   50  166202.338 ± 13253.571  ops/s
MessagePackJava.unpack:floatArray    thrpt   50  176145.186 ±  4517.170  ops/s
MessagePackJava.unpack:intArray      thrpt   50  121146.462 ±  3870.203  ops/s
PackBenchmark.packInt                thrpt   50  105945.238 ±  2251.499  ops/s
```

### General Usage
```
$ airframe-benchmark bench --help
usage: bench [targetPackage]
  Run a benchmark

[global options]
 -h, --help         display help message
 -f [RESULTFORMAT]  Result format: text, csv, scsv, json, latex
 -o [RESULTOUTPUT]  Result output file name
 -wt:[WARMUPTIME]   warmup time (default: 0.1s)
[options]
 -i, --iteration:[ITERATION]     The number of iteration (default: 10)
 -w, --warmup:[WARMUPITERATION]  The number of warm-up iteration (default: 3)
 -mt:[MEASUREMENTTIME]           measurement time (default: 1s)
```


# For Developers

## Running THe Benchmark While Developing Airframe

```
$ ./sbt

# Run JSON benchmark:
> benchmark/run bench-quick json

# Run JSON-MsgPack conversion benchmark:
> benchmark/run bench-quick json_stream

# Run benchmark for measuring JSON parse time:
> benchmark/run json-perf

# Run Msgpack benchmark:
> benchmark/run bench-quick msgpack

# Run all JMH benchmarks:
> benchmark/run bench-quick
```

To see more stable performance characteristics, use `bench` command, instead of `bench-quick`.

## Building The Benchmark Runner
```
$ ./sbt
> benchmark/pack

# To install this program to $HOME/local/bin, run:
> benchmark/packInstall
```
