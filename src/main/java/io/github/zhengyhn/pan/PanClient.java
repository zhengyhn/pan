package io.github.zhengyhn.pan;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.redis.RedisArrayAggregator;
import io.netty.handler.codec.redis.RedisBulkStringAggregator;
import io.netty.handler.codec.redis.RedisDecoder;
import io.netty.handler.codec.redis.RedisEncoder;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class PanClient {
    private String host;
    private Integer port;
    private ClientOption clientOption;

    public PanClient(ClientOption option) {
        host = option.getHost();
        port = option.getPort();
        clientOption = option;
    }

    public void execute() {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Channel channel = connect(group);
            sendMessage(channel);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    private Channel connect(EventLoopGroup group) throws InterruptedException {
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new RedisDecoder());
                        p.addLast(new RedisBulkStringAggregator());
                        p.addLast(new RedisArrayAggregator());
                        p.addLast(new RedisEncoder());
                        p.addLast(new RedisClientHandler(clientOption));
                    }
                });
        return b.connect(this.host, this.port).sync().channel();
    }

    private void sendMessage(Channel ch) throws IOException, InterruptedException {
        System.out.println("Enter Redis commands (quit to end)");
        ChannelFuture lastWriteFuture = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.print(clientOption.getPromptPrefix());
        for (;;) {
            final String input = in.readLine();
            final String line = input != null ? input.trim() : null;
            if (line == null || "quit".equalsIgnoreCase(line)) { // EOF or "quit"
                ch.close().sync();
                break;
            } else if (line.isEmpty()) { // skip `enter` or `enter` with spaces.
                System.out.print(clientOption.getPromptPrefix());
                continue;
            }
            // Sends the received line to the server.
            lastWriteFuture = ch.writeAndFlush(line);
            lastWriteFuture.addListener(new GenericFutureListener<ChannelFuture>() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        System.err.print("write failed: ");
                        future.cause().printStackTrace(System.err);
                    }
                }
            });
        }
        // Wait until all messages are flushed before closing the channel.
        if (lastWriteFuture != null) {
            lastWriteFuture.sync();
        }
    }
}
