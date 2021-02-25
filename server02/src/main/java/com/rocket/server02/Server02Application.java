package com.rocket.server02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Server02Application {

    public static void main(String[] args) {
        SpringApplication.run(Server02Application.class, args);
    }
    @RequestMapping("api/get")
    public String api(){
        return "server02";
    }
}
