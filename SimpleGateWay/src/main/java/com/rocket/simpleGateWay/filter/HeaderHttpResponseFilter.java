package com.rocket.simpleGateWay.filter;

import io.netty.handler.codec.http.FullHttpResponse;

/**
 * @author 饶珂
 * @date 2021/2/25 22:46
 */
public class HeaderHttpResponseFilter implements HttpResponseFilter  {
    @Override
    public void filter(FullHttpResponse response) {
        response.headers().set("response1","responseValue");
    }
}
