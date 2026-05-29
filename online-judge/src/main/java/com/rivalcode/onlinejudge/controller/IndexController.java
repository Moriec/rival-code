package com.rivalcode.onlinejudge.controller;

import com.rivalcode.starter.service.ServiceInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/index")
public class IndexController {

    private final ServiceInfo serviceInfo;

    public IndexController(ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    @GetMapping
    public String index() {
        return "index";
    }

    @GetMapping("/service")
    public ServiceInfo service() {
        return serviceInfo;
    }
}
