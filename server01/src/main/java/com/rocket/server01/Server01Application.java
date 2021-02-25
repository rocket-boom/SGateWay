package com.rocket.server01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Server01Application {

    public static void main(String[] args) {
        SpringApplication.run(Server01Application.class, args);
    }
    @RequestMapping("api/get")
    public String api(){
        return "server01";
    }
}
