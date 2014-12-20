package me.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.concurrent.ImmediateEventExecutor;
import me.model.WorldClockProtocol.*;
import me.model.WorldClockProtocol;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Calendar.*;
import static java.util.Calendar.SECOND;

/**
 * Listing 2.3  of <i>Netty in Action</i>
 *
 * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
 */
public class EchoServer {

    private final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
    private final EventLoopGroup group = new NioEventLoopGroup();
    private Channel channel;

    private final int port;

    public static final List<ChannelHandlerContext> clientContexts = new ArrayList<>();

    public EchoServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                            ChannelPipeline p = ch.pipeline();

                            p.addLast(new ProtobufVarint32FrameDecoder());
                            p.addLast(new ProtobufDecoder(WorldClockProtocol.Locations.getDefaultInstance()));

                            p.addLast(new ProtobufVarint32LengthFieldPrepender());
                            p.addLast(new ProtobufEncoder());

                            p.addLast(new EchoServerHandler());
                        }
                    });

            ChannelFuture f = b.bind().sync();
            System.out.println(EchoServer.class.getName() + " started and listen on " + f.channel().localAddress());
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }

    public ChannelFuture start0() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(group)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(port))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                        ChannelPipeline p = ch.pipeline();

                        p.addLast(new ProtobufVarint32FrameDecoder());
                        p.addLast(new ProtobufDecoder(WorldClockProtocol.Locations.getDefaultInstance()));

                        p.addLast(new ProtobufVarint32LengthFieldPrepender());
                        p.addLast(new ProtobufEncoder());

                        p.addLast(new EchoServerHandler());
                    }
                });

        ChannelFuture f = b.bind();
        f.syncUninterruptibly();
        channel = f.channel();
        System.out.println(EchoServer.class.getName() + " started and listen on " + f.channel().localAddress());
        return f;
    }

    public void destroy() {
        if (channel != null) {
            channel.close();
        }
        channelGroup.close();
        group.shutdownGracefully();
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt("9999");
        final EchoServer endpoint = new EchoServer(port);
        ChannelFuture future = endpoint.start0();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                endpoint.destroy();
            }
        });

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                WorldClockProtocol.LocalTimes.Builder builder = WorldClockProtocol.LocalTimes.newBuilder();
                TimeZone tz = TimeZone.getTimeZone(EchoServer.toString(Continent.ASIA) + '/' + "BeiJing");
                Calendar calendar = getInstance(tz);
                calendar.setTimeInMillis(currentTime);

                builder.addLocalTime(WorldClockProtocol.LocalTime.newBuilder().
                        setYear(calendar.get(YEAR)).
                        setMonth(calendar.get(MONTH) + 1).
                        setDayOfMonth(calendar.get(DAY_OF_MONTH)).
                        setDayOfWeek(WorldClockProtocol.DayOfWeek.valueOf(calendar.get(DAY_OF_WEEK))).
                        setHour(calendar.get(HOUR_OF_DAY)).
                        setMinute(calendar.get(MINUTE)).
                        setSecond(calendar.get(SECOND)).build());
                for (ChannelHandlerContext channelHandlerContext : clientContexts) {
                    System.out.println("connected clients = " + clientContexts.size());
                    channelHandlerContext.writeAndFlush(builder.build());
                }
            }
        }, 1, 1, TimeUnit.SECONDS);

        future.channel().closeFuture().syncUninterruptibly();
    }

    private static String toString(Continent c) {
        return c.name().charAt(0) + c.name().toLowerCase().substring(1);
    }
}

