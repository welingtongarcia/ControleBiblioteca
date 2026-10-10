package br.com.minhagibiteca.dashboard.presentation;

import br.com.minhagibiteca.auth.application.CurrentAuthenticatedUser;
import br.com.minhagibiteca.dashboard.application.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final DashboardService dashboard;
    private final CurrentAuthenticatedUser user;
    private final br.com.minhagibiteca.collection.application.ReadingService reading;
    private final br.com.minhagibiteca.dashboard.application.HomeService home;
    public DashboardController(DashboardService dashboard, CurrentAuthenticatedUser user, br.com.minhagibiteca.collection.application.ReadingService reading,
                               br.com.minhagibiteca.dashboard.application.HomeService home) {
        this.dashboard = dashboard; this.user = user; this.reading = reading; this.home = home;
    }

    @GetMapping({"/", "/dashboard"})
    String dashboard(Model model) {
        model.addAttribute("showcase", home.showcase(user.id()));
        model.addAttribute("bannerSummary", dashboard.summary(user.id()));
        model.addAttribute("bannerReading", reading.overall(user.id()));
        return "dashboard/home";
    }

    @GetMapping("/visao-geral")
    String overview(Model model) {
        model.addAttribute("summary", dashboard.summary(user.id()));
        model.addAttribute("readingProgress", reading.overall(user.id()));
        return "dashboard/index";
    }
}
