# Nast

* [About](#About)
* [Goals](#Goals)
* [Flow](#Main flow)
* [Examples](#Examples)

## About
Network lib for building async and fast client-serer application. 

**Current project in R&D state**

# Goals

## What doesn't included in project goal

* solve streaming data (video, broadcast and etc)
* sending big files (bigger than Buffer size (chunking is not supported))
* Http server

## What included in project goal

* build api for writing client-server application
* Async client-server application that communicate with small messages (RPC)
* Streaming for ETL 
* Be Faster
* Be Simple/Smaller


## Main flow

```
          |---------------------------------------------|
          | Network IO (EPOLL)       Network IO (EPOLL) |
          |         |                    |              |
          |         v                    |              |
   Nast   |      FrameDecoder       FrameDecoder        | 
  Thread  |         |                    |              |
   Pool   |         |                    |              |
                    v                    |
          |---------------------------------------------|
          |       ChannelHanler (Handle Messages)       |
          |----------------------------------------------
                   |                    ^
                 (on read)            (on write)
                   v                    |
                        
  App     |------------ Outgoung Queue -----------------|
 Thred    
  Pool    
```

## Network layer
Network layer responsible for 
* read/write data from/to network IO
* unpack/pack message (frame encoder/decoder)
* serialization/deserialization messages


## Application layer

Application layer must be responsible for
* processing incoming messages

Application layer must work in additional thread pool and doesn't block network layer
All communication must be over queue 

## Examples

## Install

```gradle
    implementation "com.fnklabs.nast:nast-core:0.3.0"
    implementation "com.fnklabs.nast:nast-examples:0.3.0"
```

* [Chat server](./nast-examples/README.md)

## Benchmarks

* [Source code](./nast-core/src/jmh)

### Env 

| Type | Value |
|-----|-----:|
| CPU| Intel(R) Core(TM) i7-6500U CPU @ 2.50GHz |
| MemTotal |       16281236 kB  | 
| OS | Ubuntu 19.04|

### Result
```
Benchmark                                                                    Mode  Cnt          Score         Error  Units
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestNoReplyOneClientAsync       thrpt    5    156220.082 ?    5125.897  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestNoReplyOneClientSync        thrpt    5     76350.254 ?   34700.297  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestNoReplySeveralClientsAsync  thrpt    5    390154.632 ?   22315.461  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestNoReplySeveralClientsSync   thrpt    5     95425.885 ?    4636.737  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestReplyOneClientAsync         thrpt    5     53029.154 ?     947.294  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestReplyOneClientSync          thrpt    5     15497.326 ?    5131.255  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestReplySeveralClientsAsync    thrpt    5     55128.012 ?    3183.349  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestReplySeveralClientsSync     thrpt    5     22661.871 ?    6610.770  ops/s
c.f.n.n.benchmark.netty.NettyBenchmarkTest.nettyOneClientAsync              thrpt    5       564.536 ?     228.552  ops/s
c.f.n.n.benchmark.netty.NettyBenchmarkTest.nettyOneClientSync               thrpt    5     69050.246 ?    3860.910  ops/s
c.f.n.n.benchmark.netty.NettyBenchmarkTest.nettySeveralClientAsync          thrpt    5       673.917 ?     807.266  ops/s
c.f.n.n.benchmark.netty.NettyBenchmarkTest.nettySeveralClientsSync          thrpt    5     53745.874 ?     346.603  ops/s
c.f.n.n.io.frame.FrameDecoderBenchmark.decodeFrame                          thrpt    5  93941018.662 ? 5792695.546  ops/s
c.f.n.n.io.frame.FrameDecoderBenchmark.encodeFrame                          thrpt    5  76996458.093 ? 6966696.105  ops/s
```
 