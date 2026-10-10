package br.com.minhagibiteca.shared.presentation;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class NavigationConfiguration implements WebMvcConfigurer {
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView view) {
                if (!request.getMethod().equals("POST") || request.getParameter("returnTo") == null || view == null || view.getViewName() == null || !view.getViewName().startsWith("redirect:/")) return;
                var context = NavigationContext.from(request);
                if (request.getRequestURI().equals(request.getContextPath() + "/edicoes")) {
                    view.setViewName("redirect:" + context.withReturn(view.getViewName().substring("redirect:".length())));
                    return;
                }
                if (!context.key().equals("dashboard")) view.setViewName("redirect:" + context.redirect(view.getViewName().substring("redirect:".length())));
            }
        });
    }
}
