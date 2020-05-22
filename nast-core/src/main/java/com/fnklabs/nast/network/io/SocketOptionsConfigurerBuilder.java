package com.fnklabs.nast.network.io;

public class SocketOptionsConfigurerBuilder {
    private TcpNoDelayOptionsConfigurer tcpNoDelayConfigurer = new TcpNoDelayOptionsConfigurer(true);
    private KeepAliveOptionsConfigurer keepAliveOptionsConfigurer = new KeepAliveOptionsConfigurer(true);
    private RcvBufferSocketOptionsConfigurer rcvBufferSocketOptionsConfigurer = new RcvBufferSocketOptionsConfigurer(Integer.parseInt(System.getProperty("network.rcv-buf", "1024")));
    private SendBufferSocketOptionsConfigurer sendBufferSocketOptionsConfigurer = new SendBufferSocketOptionsConfigurer(Integer.parseInt(System.getProperty("network.snd-buf", "1024")));

    private SocketOptionsConfigurerBuilder() {}

    public static SocketOptionsConfigurerBuilder builder() {
        return new SocketOptionsConfigurerBuilder();
    }

    public SocketOptionsConfigurerBuilder tcpNoDelay(boolean value) {
        tcpNoDelayConfigurer = new TcpNoDelayOptionsConfigurer(value);

        return this;
    }

    public SocketOptionsConfigurerBuilder keepAlive(boolean value) {
        keepAliveOptionsConfigurer = new KeepAliveOptionsConfigurer(value);

        return this;
    }

    public SocketOptionsConfigurerBuilder rcvBuffer(int value) {
        rcvBufferSocketOptionsConfigurer = new RcvBufferSocketOptionsConfigurer(value);

        return this;
    }

    public SocketOptionsConfigurerBuilder sndBuffer(int value) {
        sendBufferSocketOptionsConfigurer = new SendBufferSocketOptionsConfigurer(value);

        return this;
    }

    public SocketOptionsConfigurer build() {
        tcpNoDelayConfigurer.andThen(keepAliveOptionsConfigurer);
        keepAliveOptionsConfigurer.andThen(rcvBufferSocketOptionsConfigurer);
        rcvBufferSocketOptionsConfigurer.andThen(sendBufferSocketOptionsConfigurer);

        return tcpNoDelayConfigurer;
    }
}
