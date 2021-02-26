package com.rocket.simpleGateWay.filter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
/**
 * @author 饶珂
 * @date 2021/2/25 22:44
 */
public class HeaderHttpRequestFilter implements HttpRequestFilter  {
    @Override
    public void filter(FullHttpRequest fullRequest, ChannelHandlerContext ctx) {
            fullRequest.headers().set("header1","headerValue1");
    }
}
