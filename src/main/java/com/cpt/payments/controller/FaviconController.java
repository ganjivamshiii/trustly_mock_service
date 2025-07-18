package com.cpt.payments.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FaviconController {

    @RequestMapping("favicon.ico")
    public void favicon() {
        // No content, just suppresses the 404 error
    }
}
