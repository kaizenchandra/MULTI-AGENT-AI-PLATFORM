package com.kaizen.platform;

import static com.kaizen.platform.Contracts.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
 @Bean SecurityFilterChain security(HttpSecurity http,
   @Value("${platform.auth.mode}") String mode,@Value("${platform.auth.issuer}") String issuer,
   @Value("${platform.auth.audience}") String audience,@Value("${platform.auth.local-user-token}") String user,
   @Value("${platform.auth.local-approver-token}") String approver) throws Exception {
  http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers("/actuator/health/**").permitAll().anyRequest().authenticated())
   .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
   .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->s.sendError(401)).accessDeniedHandler((r,s,x)->s.sendError(403)));
  if (mode.equals("oidc")) {
   if (issuer.isBlank() || !issuer.startsWith("https://")) throw new IllegalStateException("HTTPS OIDC issuer required");
   NimbusJwtDecoder decoder=JwtDecoders.fromIssuerLocation(issuer);
   decoder.setJwtValidator(validators(issuer,audience));
   http.oauth2ResourceServer(o->o.jwt(j->j.decoder(decoder)));
  } else if (mode.equals("local")) {
   if(user.length()<32 || approver.length()<32 || user.equals(approver)) throw new IllegalStateException("Distinct local tokens of at least 32 characters required");
   http.addFilterBefore(new OncePerRequestFilter() {
    protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws java.io.IOException,ServletException {
     String raw=req.getHeader("Authorization");
     if(raw!=null && raw.startsWith("Bearer ")) {
      String token=raw.substring(7); String who=equal(token,user)?"local-user":equal(token,approver)?"local-approver":null;
      if(who!=null) {
       var scopes=who.equals("local-user")?Set.of("workflow:write","knowledge:write","knowledge:read","memory:write"):Set.of("approval:decide","audit:read");
       var identity=new Identity("local",who,scopes);
       SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(identity,null,scopes.stream().map(SimpleGrantedAuthority::new).toList()));
      }
     }
     chain.doFilter(req,res);
    }
   },BasicAuthenticationFilter.class);
  } else throw new IllegalStateException("Unsupported authentication mode");
  return http.build();
 }
 static OAuth2TokenValidator<Jwt> validators(String issuer,String audience) {
  return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),jwt->jwt.getAudience().contains(audience)?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token")));
 }
 static boolean equal(String a,String b) { return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8)); }
 public static Identity identity(org.springframework.security.core.Authentication auth) {
  if(auth.getPrincipal() instanceof Identity i) return i;
  if(auth.getPrincipal() instanceof Jwt jwt) {
   String tenant=jwt.getClaimAsString("tenant_id");
   if(tenant==null || !tenant.matches("[a-zA-Z0-9_-]{1,100}") || jwt.getSubject()==null || jwt.getSubject().isBlank() || jwt.getSubject().length()>200) throw new ApiError(403,"Invalid identity claims");
   String scope=jwt.getClaimAsString("scope");
   return new Identity(tenant,jwt.getSubject(),scope==null?Set.of():Set.copyOf(Arrays.asList(scope.split(" "))));
  }
  throw new ApiError(401,"Authentication required");
 }
}
