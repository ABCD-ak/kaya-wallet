package com.kaya.config;

import com.kaya.common.RequestIdFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * Replaces web.xml servlet wiring. Discovered automatically through Spring's
 * {@code SpringServletContainerInitializer}.
 */
public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[]{AppConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[]{WebConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    @Override
    protected String getServletName() {
        return "kaya-dispatcher";
    }

    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);

        // Spring Security is configured in the root context (AppConfig imports SecurityConfig).
        DelegatingFilterProxy securityFilter = new DelegatingFilterProxy("springSecurityFilterChain");

        return new Filter[]{
                encodingFilter,
                new RequestIdFilter(),
                new RequestContextFilter(),
                securityFilter
        };
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        // Leave headroom above the 5MB avatar limit so the controller can return
        // a friendly 413 payload instead of the container aborting the request.
        registration.setMultipartConfig(new MultipartConfigElement(
                "", 6L * 1024 * 1024, 8L * 1024 * 1024, 0));
    }

    @Override
    protected DispatcherServlet createDispatcherServlet(WebApplicationContext servletAppContext) {
        DispatcherServlet dispatcherServlet =
                (DispatcherServlet) super.createDispatcherServlet(servletAppContext);
        // Route unknown paths through GlobalExceptionHandler so 404s keep the JSON envelope.
        dispatcherServlet.setThrowExceptionIfNoHandlerFound(true);
        return dispatcherServlet;
    }
}
