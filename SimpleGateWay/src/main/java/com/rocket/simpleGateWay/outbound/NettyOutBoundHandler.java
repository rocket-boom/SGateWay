package com.rocket.simpleGateWay.outbound;

import com.rocket.simpleGateWay.filter.HeaderHttpResponseFilter;
import com.rocket.simpleGateWay.filter.HttpRequestFilter;
import com.rocket.simpleGateWay.filter.HttpResponseFilter;
import com.rocket.simpleGateWay.router.HttpEndpointRouter;
import com.rocket.simpleGateWay.router.RandomHttpEndpointRouter;
import com.rocket.simpleGateWay.util.NamedThreadFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;

import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import org.apache.http.conn.HttpInetSocketAddress;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.protocol.Pipelined;
import org.apache.http.util.EntityUtils;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author 饶珂
 * @date 2021/2/26 10:00
 */
public class NettyOutBoundHandler {
    private ExecutorService proxyService;
    private List<String> backendUrls;
    private static Bootstrap httpclient;
    private static Map<Channel,Thread> threadMap = new ConcurrentHashMap<>();
     ChannelHandlerContext proxyHandlerContext;
     static Map<ChannelHandlerContext,Object> resMap = new ConcurrentHashMap();
    static {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            httpclient = new Bootstrap();
            httpclient
                    .group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new HttpResponseDecoder())
                                    .addLast(new HttpRequestEncoder())
                                    .addLast(new NettyClientOutBoundHandler());
                        }
                    });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    HttpResponseFilter filter = new HeaderHttpResponseFilter();
    HttpEndpointRouter router = new RandomHttpEndpointRouter();
    public NettyOutBoundHandler(List<String> backendUrls){
        this.backendUrls = backendUrls.stream().map(this::formatUrl).collect(Collectors.toList());

        System.out.println("Netty开启服务");

        int cores = Runtime.getRuntime().availableProcessors();
        long keepAliveTime = 1000;
        int queueSize = 2048;
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        proxyService = new ThreadPoolExecutor(cores, cores,
                keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize),
                new NamedThreadFactory("proxyService"), handler);

    }
    private String formatUrl(String backend) {
        return backend.endsWith("/")?backend.substring(0,backend.length()-1):backend;
    }
    public void handle(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx, HttpRequestFilter filter) {
        this.proxyHandlerContext = ctx;
        String backendUrl = router.route(this.backendUrls);
        final String url = backendUrl + fullRequest.uri();
        filter.filter(fullRequest, ctx);
        proxyService.submit(()->fetchGet(fullRequest, ctx, url));
    }
    private void fetchGet(final FullHttpRequest inbound, final ChannelHandlerContext ctx, final String url) {
        try{
            URL uri = new URL(url);
            // TODO: 2021/2/26 无法处理连接，目测是没有连接上具体的后端路由即 api/xxx/xxx 没有连接成功 待补 
            ChannelFuture channelFuture = httpclient.connect(uri.getHost(),uri.getPort()).sync();
            threadMap.put(channelFuture.channel(),Thread.currentThread());
            channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isDone() || future.isSuccess()){
                    System.out.println("已经连接");
                }

            }
        }).sync();
            LockSupport.park();
            //防止意外唤醒
            while(true){
                Channel channel = channelFuture.channel();
                if(resMap.containsKey(channel)&&resMap.get(channel) != null){
                    proxyHandlerContext.writeAndFlush(resMap.get(channel));
                    resMap.remove(channel);
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    final static class NettyClientOutBoundHandler extends SimpleChannelInboundHandler<DefaultFullHttpRequest> {
        private HttpResponseFilter filter = new HeaderHttpResponseFilter();
        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, DefaultFullHttpRequest httpRequest) throws Exception {
            DefaultFullHttpResponse response = null;
            ByteBuf body = httpRequest.content();
            try{
                response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(body));
                response.headers().set("Content-Type", "application/json");
                response.headers().setInt("Content-Length", Integer.parseInt(httpRequest.headers().get("Content-Length")));
                filter.filter(response);
            }catch (Exception e){
                e.printStackTrace();
                response = new DefaultFullHttpResponse(HTTP_1_1, NO_CONTENT);
            }finally {
                resMap.put(channelHandlerContext,response);
                LockSupport.unpark(threadMap.get(channelHandlerContext.channel()));
            }
        }
    }
}
