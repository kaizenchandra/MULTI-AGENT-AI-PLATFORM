package com.kaizen.platform;
import static com.kaizen.platform.Contracts.*;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import jakarta.validation.ConstraintViolationException;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(ApiError.class) ResponseEntity<ProblemDetail> api(ApiError e){return response(e.status,e.getMessage());}
 @ExceptionHandler({MethodArgumentNotValidException.class,HandlerMethodValidationException.class,ConstraintViolationException.class,HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.web.bind.MissingRequestHeaderException.class})
 ResponseEntity<ProblemDetail> invalid(Exception e){return response(400,"Invalid request");}
 @ExceptionHandler(Exception.class) ResponseEntity<ProblemDetail> unexpected(Exception e){org.slf4j.LoggerFactory.getLogger(ApiErrors.class).error("API failure class={}",e.getClass().getSimpleName());return response(500,"Internal error; retry with the same idempotency key if appropriate");}
 private ResponseEntity<ProblemDetail> response(int status,String message){return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status),message));}
}
