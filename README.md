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