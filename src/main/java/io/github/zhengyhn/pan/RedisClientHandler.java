package io.github.zhengyhn.pan;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.redis.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class RedisClientHandler extends ChannelDuplexHandler {
    private ClientOption clientOption;
    public RedisClientHandler(ClientOption option) {
        super();
        clientOption = option;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        String[] commands = ((String) msg).split("\\s+");
        List<RedisMessage> children = new ArrayList<RedisMessage>(commands.length);
        for (String cmdString : commands) {
            children.add(new FullBulkStringRedisMessage(ByteBufUtil.writeUtf8(ctx.alloc(), cmdString)));
        }
        RedisMessage request = new ArrayRedisMessage(children);
        ctx.write(request, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        RedisMessage redisMessage = (RedisMessage) msg;
        printlnAggregatedRedisResponse(redisMessage);
        ReferenceCountUtil.release(redisMessage);
        System.out.print(clientOption.getPromptPrefix());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("exceptionCaught: ");
        cause.printStackTrace(System.err);
    }

    private static void printlnAggregatedRedisResponse(RedisMessage msg) {
        if (msg instanceof SimpleStringRedisMessage) {
            System.out.println(((SimpleStringRedisMessage) msg).content());
        } else if (msg instanceof ErrorRedisMessage) {
            System.out.println(((ErrorRedisMessage) msg).content());
        } else if (msg instanceof IntegerRedisMessage) {
            System.out.println(((IntegerRedisMessage) msg).value());
        } else if (msg instanceof FullBulkStringRedisMessage) {
            System.out.println(getString((FullBulkStringRedisMessage) msg));
        } else if (msg instanceof ArrayRedisMessage) {
            List<RedisMessage> children = ((ArrayRedisMessage) msg).children();
            if (children.size() <= 0) {
                System.out.println("(empty list or set)");
            } else {
                for (RedisMessage child : children) {
                    printlnAggregatedRedisResponse(child);
                }
            }
        } else {
            throw new CodecException("unknown message type: " + msg);
        }
    }

    private static String getString(FullBulkStringRedisMessage msg) {
        if (msg.isNull()) {
            return "(null)";
        }
        return msg.content().toString(CharsetUtil.UTF_8);
    }
}
