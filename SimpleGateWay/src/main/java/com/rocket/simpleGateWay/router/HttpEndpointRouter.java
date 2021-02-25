package com.rocket.simpleGateWay.router;

import java.util.List;

/**
 * @author 饶珂
 * @date 2021/2/25 23:01
 */
public interface HttpEndpointRouter {
    String route(List<String> endpoints);
}
