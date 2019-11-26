package com.fnklabs.nast.examples.echo;

import com.fnklabs.nast.network.io.ClientChannel;
import com.google.common.net.HostAndPort;

import java.nio.ByteBuffer;
import java.util.Scanner;

public class EchoClient {
    public static void main(String[] args) throws Exception {
        HostAndPort hostAndPort = HostAndPort.fromString(args.length > 0 ? args[0] : "127.0.0.1:10000");

        try (Scanner scanner = new Scanner(System.in);
             ClientChannel clientChannel = new ClientChannel(hostAndPort, new ClientChannelHandler());
        ) {
            String inputMessage = null;
            do {
                inputMessage = scanner.next();

                clientChannel.send(ByteBuffer.wrap(inputMessage.getBytes()));
            } while (!inputMessage.equals("exit"));
        }


    }
}
