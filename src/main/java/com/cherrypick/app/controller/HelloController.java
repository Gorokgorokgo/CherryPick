package com.cherrypick.app.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String hello() {
        return "Hello, Cherrypick!";
    }
    
    @GetMapping("/api/health")
    public String health() {
        return "OK";
    }
}