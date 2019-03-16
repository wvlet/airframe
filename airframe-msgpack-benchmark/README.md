airframe-msgpack-benchmark
===

MessagePack benchmark program based on [JMH](https://openjdk.java.net/projects/code-tools/jmh/).


## How To Build
```
$ ./sbt
> msgpackBenchmark/pack

# To install this program to $HOME/local/bin, run:
> msgpackBenchmark/packInstall
```

### Running the benchmark
```
# In another terminal, run this command. The result will be written to a json file: 
$ ./airframe-msgpack-benchmark/target/pack/bin/airframe-msgpack-benchmark bench -f json 
```

### Usage
```
$ ./airframe-msgpack-benchmark/target/pack/bin/airframe-msgpack-benchmark bench --help
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
- Run `airframe-msgpack-benchmark/bin/airframe-msgpack-benchmark bench`

