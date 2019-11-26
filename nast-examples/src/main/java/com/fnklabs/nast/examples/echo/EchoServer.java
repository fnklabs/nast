package com.fnklabs.nast.examples.echo;

import com.fnklabs.nast.network.io.ServerChannel;
import com.google.common.net.HostAndPort;

public class EchoServer {
    public static void main(String[] args) throws Exception {
        HostAndPort hostAndPort = HostAndPort.fromString(args.length > 0 ? args[0] : "127.0.0.1:10000");

        ServerChannel serverChannel = new ServerChannel(hostAndPort, new EchoServerChannelHandler(), 1);
    }
}
