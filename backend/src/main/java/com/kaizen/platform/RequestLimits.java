package com.kaizen.platform;

import static com.kaizen.platform.Contracts.*;
import java.io.*;
import java.lang.reflect.Type;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

@Configuration
public class RequestLimits implements WebMvcConfigurer {
 private final JdbcTemplate db;
 public RequestLimits(JdbcTemplate db){this.db=db;}
 @Override public void addInterceptors(InterceptorRegistry registry){
  registry.addInterceptor(new org.springframework.web.servlet.HandlerInterceptor(){
   public boolean preHandle(jakarta.servlet.http.HttpServletRequest req,jakarta.servlet.http.HttpServletResponse res,Object handler){
    var who=SecurityConfig.identity(SecurityContextHolder.getContext().getAuthentication());
    var result=db.queryForList("""
     INSERT INTO api_rate_limits(tenant,subject,window_start,requests) VALUES (?,?,now(),1)
     ON CONFLICT(tenant,subject) DO UPDATE SET
      requests=CASE WHEN api_rate_limits.window_start<=now()-interval '1 minute' THEN 1 ELSE api_rate_limits.requests+1 END,
      window_start=CASE WHEN api_rate_limits.window_start<=now()-interval '1 minute' THEN now() ELSE api_rate_limits.window_start END
     WHERE api_rate_limits.window_start<=now()-interval '1 minute' OR api_rate_limits.requests<300 RETURNING requests
     """,who.tenant(),who.subject());
    if(result.isEmpty()){res.setHeader("Retry-After","60");throw new ApiError(429,"API rate limit exceeded");}return true;
   }
  }).addPathPatterns("/api/v1/**");
 }
 @ControllerAdvice
 public static class BoundedBody extends RequestBodyAdviceAdapter {
  public boolean supports(MethodParameter p,Type t,Class<? extends HttpMessageConverter<?>> c){return true;}
  public HttpInputMessage beforeBodyRead(HttpInputMessage message,MethodParameter p,Type t,Class<? extends HttpMessageConverter<?>> c) throws IOException {
   byte[] bytes=message.getBody().readNBytes(1048577);
   if(bytes.length>1048576)throw new ApiError(413,"Request body exceeds 1 MiB");
   return new HttpInputMessage(){public InputStream getBody(){return new ByteArrayInputStream(bytes);}public HttpHeaders getHeaders(){return message.getHeaders();}};
  }
 }
}
