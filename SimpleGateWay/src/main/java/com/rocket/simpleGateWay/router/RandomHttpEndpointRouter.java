package com.rocket.simpleGateWay.router;

import java.util.List;
import java.util.Random;

/**
 * @author 饶珂
 * @date 2021/2/25 23:01
 */
public class RandomHttpEndpointRouter implements HttpEndpointRouter {
    @Override
    public String route(List<String> urls) {
        int size = urls.size();
        Random random = new Random(System.currentTimeMillis());
        return urls.get(random.nextInt(size));
    }
}