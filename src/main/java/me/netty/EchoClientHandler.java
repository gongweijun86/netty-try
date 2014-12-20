package me.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import me.model.WorldClockProtocol;
import me.model.WorldClockProtocol.*;

import java.util.Formatter;


/**
 * Listing 2.6 of <i>Netty in Action</i>
 *
 * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
 */
@Sharable
public class EchoClientHandler extends
        SimpleChannelInboundHandler<LocalTimes> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        Locations.Builder builder = Locations.newBuilder();

        builder.addLocation(Location.newBuilder().
                setContinent(Continent.ASIA).
                setCity("BeiJing").build());

        channel.writeAndFlush(builder.build());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, LocalTimes localTimes) throws Exception {
        for (LocalTime lt : localTimes.getLocalTimeList()) {
            String s = new Formatter().format(
                    "%4d-%02d-%02d %02d:%02d:%02d %s",
                    lt.getYear(),
                    lt.getMonth(),
                    lt.getDayOfMonth(),
                    lt.getHour(),
                    lt.getMinute(),
                    lt.getSecond(),
                    lt.getDayOfWeek().name()).toString();
            System.out.println(s);
        }
    }
}
