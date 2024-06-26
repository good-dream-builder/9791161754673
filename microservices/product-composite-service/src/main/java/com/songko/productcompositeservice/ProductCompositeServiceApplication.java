package com.songko.productcompositeservice;

import com.songko.productcompositeservice.services.ProductCompositeIntegration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.client.WebClient;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Collections;
import java.util.LinkedHashMap;

@SpringBootApplication
@ComponentScan("com.songko")
public class ProductCompositeServiceApplication {

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        final WebClient.Builder builder = WebClient.builder();
        return builder;
    }


    public static void main(String[] args) {
        SpringApplication.run(ProductCompositeServiceApplication.class, args);
    }

    @Value("${api.common.version}")
    String apiVersion;
    @Value("${api.common.title}")
    String apiTitle;
    @Value("${api.common.description}")
    String apiDescription;
    @Value("${api.common.termsOfServiceUrl}")
    String apiTermsOfServiceUrl;
    @Value("${api.common.license}")
    String apiLicense;
    @Value("${api.common.licenseUrl}")
    String apiLicenseUrl;
    @Value("${api.common.contact.name}")
    String apiContactName;
    @Value("${api.common.contact.url}")
    String apiContactUrl;
    @Value("${api.common.contact.email}")
    String apiContactEmail;

    /**
     * Will exposed on $HOST:$PORT/swagger-ui.html
     *
     * @return
     */
    @Bean
    public Docket apiDocumentation() {

        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.songko.microservices.composite.product"))
                .paths(PathSelectors.any())
                .build()
                .globalResponseMessage(RequestMethod.POST, Collections.emptyList())
                .globalResponseMessage(RequestMethod.GET, Collections.emptyList())
                .globalResponseMessage(RequestMethod.DELETE, Collections.emptyList())
                .apiInfo(new ApiInfo(
                        apiTitle,
                        apiDescription,
                        apiVersion,
                        apiTermsOfServiceUrl,
                        new Contact(apiContactName, apiContactUrl, apiContactEmail),
                        apiLicense,
                        apiLicenseUrl,
                        Collections.emptyList()
                ));
    }

    @Autowired
    HealthAggregator healthAggregator;

    @Autowired
    ProductCompositeIntegration integration;

    /**
     * ProductCompositeIntegration에 정의된 헬퍼 메서드를 사용해 세 가지 핵심 마이크로서비스의 상태 정보를 등록
     * @return
     */
    @Bean
    ReactiveHealthIndicator coreServices() {

        ReactiveHealthIndicatorRegistry registry = new DefaultReactiveHealthIndicatorRegistry(new LinkedHashMap<>());

        registry.register("product", () -> integration.getProductHealth());
        registry.register("recommendation", () -> integration.getRecommendationHealth());
        registry.register("review", () -> integration.getReviewHealth());

        return new CompositeReactiveHealthIndicator(healthAggregator, registry);
    }
}
