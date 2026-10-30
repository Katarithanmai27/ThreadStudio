package com.threadstudio;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
class JwtService {
  private final javax.crypto.SecretKey key;
  JwtService(@Value("${app.jwt-secret}") String secret) {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes");
    key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }
  String create(AppUser u) {
    return Jwts.builder().subject(u.email).claim("role",u.role.name()).claim("id",u.id)
      .issuedAt(new java.util.Date()).expiration(new java.util.Date(System.currentTimeMillis()+86400000))
      .signWith(key).compact();
  }
  io.jsonwebtoken.Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
@Component
class JwtFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  JwtFilter(JwtService jwt) { this.jwt=jwt; }
  @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
    String h=req.getHeader("Authorization");
    if(h!=null && h.startsWith("Bearer ")) {
      try {
        var c=jwt.parse(h.substring(7));
        var auth=new UsernamePasswordAuthenticationToken(c.getSubject(),null,List.of(new SimpleGrantedAuthority("ROLE_"+c.get("role",String.class))));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
      } catch(Exception ignored) { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }
    chain.doFilter(req,res);
  }
}
@Configuration
class SecurityConfig {
  @Bean PasswordEncoder passwordEncoder(){ return new BCryptPasswordEncoder(); }
  @Bean SecurityFilterChain filterChain(HttpSecurity http,JwtFilter jwtFilter) throws Exception {
    return http.csrf(c->c.disable()).cors(c->{}).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(a->a.requestMatchers("/api/auth/**","/error").permitAll()
        .requestMatchers(HttpMethod.GET,"/api/products/**","/api/reviews/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .requestMatchers("/api/designer/**").hasAnyRole("DESIGNER","ADMIN")
        .requestMatchers("/api/orders/**").authenticated()
        .requestMatchers("/api/reviews/**").authenticated()
        .anyRequest().authenticated())
      .addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class).build();
  }
  

@Bean
CorsConfigurationSource corsConfigurationSource(
    @Value("${app.cors-origins}") String origins) {

    CorsConfiguration c = new CorsConfiguration();

    c.setAllowedOrigins(
        java.util.Arrays.stream(origins.split(","))
            .map(String::trim)
            .toList()
    );

    c.setAllowedMethods(List.of(
        "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
    ));

    c.setAllowedHeaders(List.of("*"));
    c.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source =
        new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration("/**", c);

    return source;
}


}
