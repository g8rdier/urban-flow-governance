package ufg.app

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.transform.CompileStatic
import grails.plugins.metadata.*
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.scheduling.annotation.EnableScheduling

import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

@CompileStatic
@EnableScheduling
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Bean
    FilterRegistrationBean<Filter> stripCorsOriginHeaderFilter() {
        def bean = new FilterRegistrationBean<Filter>()
        bean.filter = new Filter() {
            void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
                chain.doFilter(req, new HttpServletResponseWrapper((HttpServletResponse) res) {
                    @Override void setHeader(String name, String value) {
                        if (!name.equalsIgnoreCase('Access-Control-Allow-Origin')) super.setHeader(name, value)
                    }
                    @Override void addHeader(String name, String value) {
                        if (!name.equalsIgnoreCase('Access-Control-Allow-Origin')) super.addHeader(name, value)
                    }
                })
            }
            void init(FilterConfig config) {}
            void destroy() {}
        }
        bean.order = Ordered.HIGHEST_PRECEDENCE
        bean.addUrlPatterns('/*')
        bean
    }
}
