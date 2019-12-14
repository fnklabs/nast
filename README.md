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
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestNoReplyOneClientAsync       thrpt    5     177903.669 ?   27812.458  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestNoReplyOneClientSync        thrpt    5      82151.287 ?   29805.368  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestNoReplySeveralClientsAsync  thrpt    5     418135.949 ?   54902.496  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestNoReplySeveralClientsSync   thrpt    5     115883.253 ?    6651.186  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestReplyOneClientAsync         thrpt    5      57362.136 ?    4252.927  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestReplyOneClientSync          thrpt    5      18914.895 ?    3982.760  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestReplySeveralClientsAsync    thrpt    5      57162.433 ?    1683.561  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestReplySeveralClientsSync     thrpt    5      28397.235 ?    8466.794  ops/s
c.f.n.n.benchmark.netty.NettyBenchmarkTest.nettyOneClientAsync              thrpt    5       2613.133 ?   17661.476  ops/s
c.f.n.n.benchmark.netty.NettyBenchmarkTest.nettyOneClientSync               thrpt    5      77079.563 ?    2843.972  ops/s
c.f.n.n.benchmark.netty.NettyBenchmarkTest.nettySeveralClientAsync          thrpt    5       1285.504 ?    4832.195  ops/s
c.f.n.n.benchmark.netty.NettyBenchmarkTest.nettySeveralClientsSync          thrpt    5      62151.877 ?    4401.618  ops/s
c.f.n.n.io.frame.FrameDecoderBenchmark.decodeFrame                          thrpt    5  115980965.748 ? 6763388.787  ops/s
c.f.n.n.io.frame.FrameDecoderBenchmark.encodeFrame                          thrpt    5   81307632.342 ? 2243395.352  ops/s
```
 