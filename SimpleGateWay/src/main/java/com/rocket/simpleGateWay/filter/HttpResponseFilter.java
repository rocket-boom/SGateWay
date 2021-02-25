package com.rocket.simpleGateWay.filter;

import io.netty.handler.codec.http.FullHttpResponse;

/**
 * @author 饶珂
 * @date 2021/2/25 22:47
 */
public interface HttpResponseFilter {
    void filter(FullHttpResponse response);
}
