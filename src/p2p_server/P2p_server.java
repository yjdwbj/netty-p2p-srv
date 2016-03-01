/* To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2p_server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author yjdwbj
 */
public class P2p_server {

    DispatchCenter dc = new DispatchCenter();
    private final static int readerIdleTimeSeconds = 80;
    private final static int writerIdleTimeSeconds = 80;
    private final static int allIdleTimeSeconds = 100;

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        List<ChannelFuture> futures = new ArrayList<>();

        try {
            ServerBootstrap appboot = new ServerBootstrap();
            appboot.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 8192)
                    .childHandler(new AppChildChannelHandler());

            appboot.option(ChannelOption.SO_REUSEADDR, true);
            appboot.option(ChannelOption.TCP_NODELAY, true);
            appboot.childOption(ChannelOption.SO_KEEPALIVE, true);
            appboot.childOption(ChannelOption.SO_RCVBUF, 512);
            appboot.childOption(ChannelOption.SO_SNDBUF, 512);

            ServerBootstrap devboot = new ServerBootstrap();
            devboot.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 8192)
                    .childHandler(new DevChildChannelHandler());

            devboot.option(ChannelOption.SO_REUSEADDR, true);
            devboot.option(ChannelOption.TCP_NODELAY, true);
            devboot.childOption(ChannelOption.SO_KEEPALIVE, true);
            devboot.childOption(ChannelOption.SO_RCVBUF, 512);
            devboot.childOption(ChannelOption.SO_SNDBUF, 512);

            //ChannelFuture f = boostrap.bind(port).sync();
            futures.add(devboot.bind(5560));
            futures.add(appboot.bind(5561));
            for (ChannelFuture f : futures) {
                f.sync();
            }

            for (ChannelFuture f : futures) {
                f.channel().closeFuture().sync();
            }
            // 等待端口，同步等待成功
            //   f.channel().closeFuture().sync();

        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();

        }

    }

    private class DevChildChannelHandler extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel arg0) throws Exception {
            // arg0.pipeline().addLast(new PacketHandler());
            //arg0.pipeline().addLast(new LineBasedFrameDecoder(8192));
            ByteBuf delimiter = Unpooled.copiedBuffer("\r\n\r\n".getBytes());
            //arg0.pipeline().addLast(new LineBasedFrameDecoder(8192));
           // arg0.pipeline().addLast(new IdleStateHandler(readerIdleTimeSeconds,writerIdleTimeSeconds, allIdleTimeSeconds));
            arg0.pipeline().addLast(new DelimiterBasedFrameDecoder(8192, delimiter));
            arg0.pipeline().addLast(new StringDecoder());
            arg0.pipeline().addLast(new DeviceHandler(dc));
        }
    }

    private class AppChildChannelHandler extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel arg0) throws Exception {
            // arg0.pipeline().addLast(new PacketHandler());
            //arg0.pipeline().addLast(new LineBasedFrameDecoder(8192));
            ByteBuf delimiter = Unpooled.copiedBuffer("\r\n\r\n".getBytes());
            //arg0.pipeline().addLast(new LineBasedFrameDecoder(8192));
            //arg0.pipeline().addLast(new IdleStateHandler(readerIdleTimeSeconds,writerIdleTimeSeconds, allIdleTimeSeconds));
            arg0.pipeline().addLast(new DelimiterBasedFrameDecoder(8192, delimiter));
            arg0.pipeline().addLast(new StringDecoder());
            arg0.pipeline().addLast(new AppHandler(dc));

        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        // TODO code application logic here
        new P2p_server().run();

    }

}

