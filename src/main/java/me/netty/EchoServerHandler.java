package me.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import me.model.WorldClockProtocol.*;

import java.util.Calendar;
import java.util.TimeZone;

import static java.util.Calendar.*;

/**
 * Listing 2.4  of <i>Netty in Action</i>
 *
 * @author <a href="mailto:nmaurer@redhat.com">Norman Maurer</a>
 */
@Sharable
public class EchoServerHandler extends
        SimpleChannelInboundHandler<Locations> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
       EchoServer.clientContexts.add(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        EchoServer.clientContexts.remove(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Locations locations) throws Exception {
        for (Location location : locations.getLocationList()) {
            System.out.println(location.getContinent());
            System.out.println(location.getCity());
        }

        long currentTime = System.currentTimeMillis();

        LocalTimes.Builder builder = LocalTimes.newBuilder();
        for (Location l: locations.getLocationList()) {
            TimeZone tz = TimeZone.getTimeZone(toString(l.getContinent()) + '/' + l.getCity());
            Calendar calendar = getInstance(tz);
            calendar.setTimeInMillis(currentTime);

            builder.addLocalTime(LocalTime.newBuilder().
                    setYear(calendar.get(YEAR)).
                    setMonth(calendar.get(MONTH) + 1).
                    setDayOfMonth(calendar.get(DAY_OF_MONTH)).
                    setDayOfWeek(DayOfWeek.valueOf(calendar.get(DAY_OF_WEEK))).
                    setHour(calendar.get(HOUR_OF_DAY)).
                    setMinute(calendar.get(MINUTE)).
                    setSecond(calendar.get(SECOND)).build());
        }

        channelHandlerContext.write(builder.build());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);//.addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
        Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private static String toString(Continent c) {
        return c.name().charAt(0) + c.name().toLowerCase().substring(1);
    }
}
