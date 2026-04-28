package ufg.app

import org.springframework.core.io.ClassPathResource

class SwaggerController {

    static allowedMethods = [spec: 'GET', ui: 'GET']

    def spec() {
        def resource = new ClassPathResource('public/openapi.yaml')
        response.contentType = 'application/yaml'
        response.outputStream << resource.inputStream
        response.outputStream.flush()
    }

    def ui() {
        def resource = new ClassPathResource('public/swagger-ui/index.html')
        response.contentType = 'text/html;charset=UTF-8'
        response.outputStream << resource.inputStream
        response.outputStream.flush()
    }
}
