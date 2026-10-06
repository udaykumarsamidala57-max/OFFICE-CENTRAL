package com.Inventory.CONTROLLER;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/Home"})
    public String home() {
        return "redirect:/IndentServlet";
    }
}
