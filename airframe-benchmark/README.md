airframe-msgpack-benchmark
===

MessagePack benchmark program based on [JMH](https://openjdk.java.net/projects/code-tools/jmh/).



## How To Build
```
$ ./sbt
> benchmark/pack

# To install this program to $HOME/local/bin, run:
> benchmark/packInstall
```


### Running the benchmark
```
$ cd airframe-benchmark
# In another terminal, run this command. The result will be written to a json file: 
$ ./target/pack/bin/airframe-benchmark bench -f json

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

### Usage
```
$ airframe-benchmark bench --help
usage: bench
  Run a benchmark

[global options]
 -h, --help             display help message
 -f [RESULTFORMAT]      Result format: text, csv, scsv, json, latex
 -o [RESULTOUTPUT]      Result output file name
 -mt:[MEASUREMENTTIME]  measurement time (default: 0.1s)
 -wt:[WARMUPTIME]       warmup time (default: 0.1s)
[options]
 -i, --iteration:[ITERATION]     The number of iteration (default: 10)
 -w, --warmup:[WARMUPITERATION]  The number of warm-up iteration (default: 5)
 -f, --fork-count:[FORKCOUNT]    Fork Count (default: 5)
```

### Download

- [Initial version](https://oss.sonatype.org/content/repositories/snapshots/org/wvlet/airframe/airframe-msgpack-benchmark_2.12/19.3.4+12-0a821f46+20190315-1700-SNAPSHOT/airframe-msgpack-benchmark_2.12-19.3.4+12-0a821f46+20190315-1700-SNAPSHOT.tar.gz)
- Download this archive, and unpack it with `tar xvfz`
- Moved to the unpacked folder, then run `./bin/airframe-msgpack-benchmark bench`


# For Developers

## Running Benchmark While Developing Airframe

```
$ ./sbt

# Run JSON benchmark:
> benchmark/testOnly * -- -n json

# Run benchmark for measuring JSON parse time:
> benchmark/testOnly * -- -n json-time

# Run Msgpack benchmark:
> benchmark/testOnly * -- -n msgpack

# Run all JMH benchmarks:
> benchmark/run bench-quick
```
