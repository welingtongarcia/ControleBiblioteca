package br.com.minhagibiteca.dashboard.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping({"/", "/dashboard"})
    String dashboard() {
        return "dashboard/index";
    }
}
