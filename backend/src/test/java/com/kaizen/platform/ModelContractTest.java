package com.kaizen.platform;
import static com.kaizen.platform.Contracts.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
class ModelContractTest {
 final ObjectMapper mapper=new ObjectMapper().enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
 ModelGateway gateway(ProviderTransport transport){return new ModelGateway("openai","test-key","operator-selected-model",0.01,0.02,mapper,transport);}
 ProviderTransport.Response response(int status,String body){return new ProviderTransport.Response(status,body.getBytes(StandardCharsets.UTF_8));}
 @Test void sendsVersionedBoundedJsonAndAccountsReportedUsage() throws Exception {
  var gateway=gateway((key,body)->{
   assertEquals("test-key",key);assertTrue(body.contains("json_object"));assertTrue(body.contains("max_completion_tokens"));assertTrue(body.contains("grounded-1"));
   return response(200,"{\"model\":\"actual-version\",\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"{\\\"claims\\\":[],\\\"uncertainty\\\":\\\"No evidence\\\"}\"}}],\"usage\":{\"prompt_tokens\":30,\"completion_tokens\":10}}");
  });
  var r=gateway.analyze("question",List.of());assertEquals(30,r.inputTokens());assertEquals("actual-version",r.model());assertTrue(r.estimatedUsd()>0);
 }
 @Test void retryableAndPermanentErrorsClassified(){assertThrows(ModelGateway.TransientModelFailure.class,()->gateway((k,b)->response(429,"private provider details")).analyze("x",List.of()));assertThrows(ApiError.class,()->gateway((k,b)->response(401,"credential detail")).analyze("x",List.of()));}
 @Test void malformedAndOversizedOutputsFailClosed(){assertThrows(ApiError.class,()->gateway((k,b)->response(200,"{} ")).analyze("x",List.of()));assertThrows(ApiError.class,()->gateway((k,b)->new ProviderTransport.Response(200,new byte[65537])).analyze("x",List.of()));}
 @Test void expensiveConfigurationCannotSpendBeyondBoundedRetryBudget(){var g=new ModelGateway("openai","key","model",100,100,mapper,(k,b)->{fail("Transport must not execute over budget");return null;});assertThrows(ApiError.class,()->g.analyze("x",List.of()));}
}
