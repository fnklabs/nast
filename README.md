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
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestNoReplyOneClientAsync       thrpt    5      88331.709 ?   20606.279  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestNoReplyOneClientSync        thrpt    5      56757.862 ?   15289.739  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestNoReplySeveralClientsAsync  thrpt    5     210779.299 ?   19012.062  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestNoReplySeveralClientsSync   thrpt    5      98838.145 ?   16417.761  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestReplyOneClientAsync         thrpt    5      51757.211 ?    3714.575  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestReplyOneClientSync          thrpt    5      18566.532 ?    5221.509  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestReplySeveralClientsAsync    thrpt    5     148880.484 ?   56083.507  ops/s
c.f.n.n.benchmark.nast.NastBenchmarkTest.requestReplySeveralClientsSync     thrpt    5      27254.242 ?   13785.883  ops/s
c.f.n.n.benchmark.netty.NettyBenchmarkTest.nettyOneClientAsync              thrpt    5        601.658 ?     350.020  ops/s
c.f.n.n.benchmark.netty.NettyBenchmarkTest.nettyOneClientSync               thrpt    5      77088.877 ?    1541.410  ops/s
c.f.n.n.benchmark.netty.NettyBenchmarkTest.nettySeveralClientAsync          thrpt    5       1835.116 ?    6602.241  ops/s
c.f.n.n.benchmark.netty.NettyBenchmarkTest.nettySeveralClientsSync          thrpt    5      63017.689 ?    2379.109  ops/s
c.f.n.n.io.frame.FrameDecoderBenchmark.decodeFrame                          thrpt    5  115155488.531 ? 4089979.379  ops/s
c.f.n.n.io.frame.FrameDecoderBenchmark.encodeFrame                          thrpt    5   80177198.079 ? 8567425.074  ops/s
```
 