package com.system.simaf.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            // 401 sem o cabeçalho WWW-Authenticate: o navegador não abre a janela padrão
            // de senha, e a tela login.html assume o controle.
            .httpBasic(b -> b.authenticationEntryPoint(
                    (req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
            .authorizeHttpRequests(a -> a
                // As páginas são públicas (não têm dados). Os dados ficam atrás do login.
                .requestMatchers("/", "/*.html", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error", "/auth/registrar", "/login.html").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}