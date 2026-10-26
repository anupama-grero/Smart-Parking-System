package com.smartparking.backend.config;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RequestMatcher gateTokenApiRequest = request ->
                request.getRequestURI().startsWith(request.getContextPath() + "/api/gates/")
                        && request.getHeader("X-Gate-Control-Token") != null;

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/favicon*").permitAll()
                        .requestMatchers("/api/gates/status", "/api/parking/status", "/api/parking/reverse-warning",
                                "/api/system/health", "/api/sensors/**", "/api/parking/occupancy/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/gates/entry/status").permitAll()
                        .requestMatchers("/security-guard", "/admin").hasRole("SECURITY_GUARD")
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf.ignoringRequestMatchers(gateTokenApiRequest,
                        request -> request.getMethod().equals("POST")
                                && request.getRequestURI().equals(request.getContextPath() + "/api/gates/entry/status")))
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/security-guard", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${security.guard.username:guard}") String username,
            @Value("${security.guard.password:}") String password) {
        boolean credentialsConfigured = !username.isBlank() && !password.isBlank();
        String encodedPassword = passwordEncoder.encode(
                credentialsConfigured ? password : UUID.randomUUID().toString());
        UserDetails guard = User.withUsername(username.isBlank() ? "guard" : username)
                .password(encodedPassword)
                .roles("SECURITY_GUARD")
                .disabled(!credentialsConfigured)
                .build();
        return new InMemoryUserDetailsManager(guard);
    }
}
