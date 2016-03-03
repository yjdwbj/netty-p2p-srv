/* To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2p_server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandlerAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.GenericFutureListener;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author yjdwbj
 */
public class DeviceHandler extends ChannelInboundHandlerAdapter {

    static final byte[] unkown_cmd = {0x0, 0x19, 0x7b, 0x22, 0x65, 0x72, 0x72, 0x22, 0x3a, 0x20, 0x22, 0x75, 0x6e, 0x6b, 0x6f, 0x77, 0x6e, 0x20, 0x63, 0x6f, 0x6d, 0x6d, 0x61, 0x6e, 0x64, 0x22, 0x7d, 0x0d, 0x0a, 0x0d, 0x0a};  //  {'err':'unkown command'}

    static final byte[] unkown_format = {0x0, 0x30, 0x7b, 0x22, 0x65, 0x72, 0x72, 0x22, 0x3a, 0x20, 0x22, 0x75, 0x6e, 0x6b, 0x6f, 0x77, 0x6e, 0x20, 0x66, 0x6f, 0x72, 0x6d, 0x61, 0x74, 0x22, 0x7d, 0x0d, 0x0a, 0x0d, 0x0a};

    static final byte[] msg_ok = {0x0, 0xf, 0x7b, 0x22, 0x6d, 0x73, 0x67, 0x22, 0x3a, 0x20, 0x22, 0x6f, 0x6b, 0x22, 0x7d, 0xd, 0xa, 0xd, 0xa};

    static final byte[] msg_keep = {0x0, 0xf, 0x7b, 0x22, 0x63, 0x6d, 0x64, 0x22, 0x3a, 0x20, 0x22, 0x6b, 0x65, 0x65, 0x70, 0x22, 0x7d, 0xd, 0xa, 0xd, 0xa};
    static final String CMD = "cmd";
    static final String LOGIN = "login";
    static final String CONN = "conn";
    static final String KEEP = "keep";
    static final String ADDR = "addr";
    static final String AID = "aid";
    static final String UUID = "uuid";
    static final String PWD = "pwd";
    static final String MSG = "msg";

    private final DispatchCenter dc;

    public DeviceHandler(DispatchCenter dc) {
        this.dc = dc;
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        // Once session is secured, send a greeting and register the channel to the global channel
        // list so the channel received the messages from others.
        ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(
                new GenericFutureListener<Future<Channel>>() {
            @Override
            public void operationComplete(Future<Channel> future) throws Exception {
                ChannelFuture flag = ctx.writeAndFlush(
                        "Welcome to " + InetAddress.getLocalHost().getHostName() + " secure chat service!\n");
                /*
                if (!flag.isSuccess()) {
                    System.out.println(" dev ssl error " + flag.cause());

                } else {
                    System.out.println("connected ssl");

                }
                 */

            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        // System.out.println("Dev recv msg " + msg);
        Gson json = new Gson();

        int msglen = msg.toString().length();
        String s = msg.toString() + "\r\n";
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        Map<String, String> dict;
        dict = json.fromJson(s.substring(2, msglen), type);

        String cmd = "";
        try {
            cmd = dict.get(CMD).toString();
            //System.out.println("is login " + cmd);
        } catch (NullPointerException e) {
            write_error(ctx, unkown_cmd);
            return;
        }
        if (cmd.compareTo(LOGIN) == 0) {
            // System.out.println("login ok");
            String uuid = "";
            String pwd = "";
            try {
                uuid = dict.get(UUID).toString();
                pwd = dict.get(PWD).toString();
            } catch (NullPointerException ue) {
                write_error(ctx, unkown_format);
                return;
                // ue.printStackTrace();
            }
            /*检查是否重新登录，如果已经登录了，就通知旧的连接，并关闭旧的连接 */
            Object[] oldobj = null;
            try {
                oldobj = dc.getDevChannel(uuid);
            } catch (NullPointerException npe) {
            }

            if (oldobj != null) {
                /* 清除原来的连接  */
                Channel oldch = (Channel) oldobj[1];
                Map<String, String> notifyDict = new HashMap<>();
                notifyDict.put(MSG, "new login from " + ctx.channel().remoteAddress());
                String jsondata = json.toJson(notifyDict);
                ByteBuf sendbuf = oldch.alloc().heapBuffer(jsondata.toString().length() + 4);
                int len = jsondata.length() + 2;
                byte[] lenarry = {0x0, 0x0};
                lenarry[0] = (byte) (0xff00 & len);
                lenarry[1] = (byte) (0xff & len);
                sendbuf.writeBytes(lenarry);
                sendbuf.writeBytes(jsondata.toString().getBytes());
                sendbuf.writeBytes("\r\n\r\n".getBytes());
                ChannelFuture write = oldch.writeAndFlush(sendbuf);
                if (!write.isSuccess()) {
                    System.out.println("notify new login failed: " + write.cause());
                }
                dc.removeDevChannel(uuid);
                write.addListener(ChannelFutureListener.CLOSE);
            }

            dc.bandUserAndChannel(uuid, pwd, ctx.channel());
            ByteBuf sendbuf = ctx.alloc().heapBuffer(msg_ok.length);
            sendbuf.writeBytes(msg_ok);
            // sendbuf.writeBytes(msg_ok);

            ChannelFuture write = ctx.channel().writeAndFlush(sendbuf);
            if (!write.isSuccess()) {

                System.out.println("send login failed: " + write.cause());
            }

        } else if (0 == cmd.compareTo(KEEP)) {
            //  System.out.println("keep ok");
            ByteBuf sendbuf = ctx.alloc().heapBuffer(msg.toString().length() + 4);
            sendbuf.writeBytes(msg.toString().getBytes());
            sendbuf.writeBytes("\r\n\r\n".getBytes());
            ChannelFuture write = ctx.writeAndFlush(sendbuf);

            if (!write.isSuccess()) {
                System.out.println("send keep failed: " + write.cause());
            }

        } else if (0 == cmd.compareTo(CONN)) {

            String aid = "";
            try {
                aid = dict.get(AID);
            } catch (NullPointerException ue) {
                //ue.printStackTrace();
                write_error(ctx, unkown_format);
                return;

            }
            // Promise<Channel> promise = ctx.executor().newPromise();

            // System.out.println("recv dev conn for aid "+aid);
            Channel appChannel = dc.getAppChannel(aid);
            //System.out.println("app dict size : " + dc.getMapSize("app"));

            if (appChannel instanceof Channel) {
                // System.out.println("ready send addr to app!!!!");
                ByteBuf sendbuf = appChannel.alloc().heapBuffer(msg.toString().length() + 4);
                sendbuf.writeBytes(msg.toString().getBytes());
                sendbuf.writeBytes("\r\n\r\n".getBytes());
                ChannelFuture write = appChannel.writeAndFlush(sendbuf);

                /* 
                        if (!write.isSuccess()) {
                            System.out.println("send to app failed: " + write.cause());
                        }
                 */
                dc.removeAppChannel(aid);
                write.addListener(ChannelFutureListener.CLOSE);

            }

        } else {
            //不识别的命令
            write_error(ctx, unkown_cmd);
        }

    }

    private void write_error(ChannelHandlerContext ctx, byte[] arry) {

        ByteBuf sendbuf = ctx.alloc().heapBuffer(arry.length);
        sendbuf.writeBytes(arry);
        ChannelFuture write = ctx.writeAndFlush(arry);
        /*
        if (!write.isSuccess()) {
            System.out.println("send unkown failed: " + write.cause());
        }
         */
        write.addListener(ChannelFutureListener.CLOSE);
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

        ctx.flush();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {
            IdleStateEvent event = (IdleStateEvent) evt;

            // sendbuf.writeBytes(msg_ok);
            if (null != event.state()) {
                switch (event.state()) {
                    case READER_IDLE:
                        System.out.println("dev read idle");
                        break;
                    case WRITER_IDLE:
                        System.out.println("dev write idle");
                        break;
                    case ALL_IDLE:
                        System.out.println("dev all idle");
                        break;
                    default:
                        break;
                }
            }
            ByteBuf sendbuf = ctx.alloc().heapBuffer(msg_keep.length);
            sendbuf.writeBytes(msg_keep);
            ChannelFuture write = ctx.channel().writeAndFlush(sendbuf);
            if (!write.isSuccess()) {

                System.out.println("send login failed: " + write.cause());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
            Throwable cause) {
        System.out.println("dev exception " + cause);

        dc.removeDevChannel(ctx.channel());

        cause.printStackTrace();
        // ctx.close();
    }

}
