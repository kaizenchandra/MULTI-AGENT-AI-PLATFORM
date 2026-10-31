package com.kaizen.platform;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.*;
import org.springframework.stereotype.Component;

@FunctionalInterface
public interface ProviderTransport {
 record Response(int status,byte[] body) {}
 Response post(String key,String body);
}

@Component
final class OpenAiTransport implements ProviderTransport {
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
 private final TraceSupport tracing;
 OpenAiTransport(TraceSupport tracing){this.tracing=tracing;}
 public Response post(String key,String body){
  try(var span=tracing.child("model.openai")){
   var request=HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/chat/completions")).timeout(Duration.ofSeconds(15))
    .header("Authorization","Bearer "+key).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
   var pending=client.sendAsync(request,info->new BoundedResponse(65536));
   try {var response=pending.get(15,TimeUnit.SECONDS);return new Response(response.statusCode(),response.body());}
   catch(TimeoutException e){pending.cancel(true);throw new ModelGateway.TransientModelFailure();}
   catch(InterruptedException e){pending.cancel(true);Thread.currentThread().interrupt();throw new ModelGateway.TransientModelFailure();}
   catch(ExecutionException e){if(e.getCause() instanceof Contracts.ApiError a)throw a;throw new ModelGateway.TransientModelFailure();}
  }
 }
}
