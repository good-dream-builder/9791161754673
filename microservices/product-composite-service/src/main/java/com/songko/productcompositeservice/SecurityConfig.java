package com.songko.productcompositeservice;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.authorizeExchange()
                // 보호 대상이 아닌 URL에 대한 접근을 허용
                .pathMatchers("/actuator/**").permitAll()
                // 접근 토큰에 있는 OAuth 2.0 스코프를 바탕으로 접근을 허용
                // 스프링 시큐리티로 권한을 검사하는 경우에는 스코프 이름 앞에 SCOPE_ 붙여야 한다
                .pathMatchers(HttpMethod.POST, "/product-composite/**").hasAuthority("SCOPE_product:write")
                .pathMatchers(HttpMethod.DELETE, "/product-composite/**").hasAuthority("SCOPE_product:write")
                .pathMatchers(HttpMethod.GET, "/product-composite/**").hasAuthority("SCOPE_product:read")
                // 다른 모든 URL에 대한 접근은 인증이 필요
                .anyExchange().authenticated()
                .and()
                // JWT로 인코딩된 OAuth 2.0 접근 토큰을 기반으로 인증 및 권한 부여를 수행
                .oauth2ResourceServer().jwt();

        return http.build();
    }
}
