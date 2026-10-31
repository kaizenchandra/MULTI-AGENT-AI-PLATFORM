package com.kaizen.platform;
import static com.kaizen.platform.Contracts.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class PolicyTest {
 private final AgentRegistry agents=new AgentRegistry();
 @Test void boundedPlans(){for(var kind:Kind.values()){var plan=agents.plan(kind);agents.validate(plan);assertTrue(plan.steps().size()<=6);}}
 @Test void terminalStatesCannotResume(){for(var s:List.of(State.COMPLETED,State.CANCELLED,State.TIMED_OUT,State.REJECTED,State.FAILED))assertThrows(IllegalStateException.class,()->s.requireTransition(State.RUNNING));}
 @Test void waitingRequiresExplicitResume(){assertDoesNotThrow(()->State.WAITING.requireTransition(State.RUNNING));assertThrows(IllegalStateException.class,()->State.WAITING.requireTransition(State.COMPLETED));}
 @Test void readAgentCannotWrite(){assertThrows(ApiError.class,()->agents.authorizeTool("research","case.create"));assertThrows(ApiError.class,()->agents.authorizeTool("execution","shell"));}
 @Test void cyclicAndEscalatingPlansDenied(){
  assertThrows(ApiError.class,()->agents.validate(new Plan("1",Kind.CREATE_CASE,List.of(new Step(0,"execution",0,List.of("case.create"),"CaseResult.v1",false)),6,600)));
  assertThrows(ApiError.class,()->agents.validate(new Plan("1",Kind.KNOWLEDGE,List.of(new Step(0,"research",null,List.of("case.create"),"Answer.v1",false)),6,600)));
 }
 @Test void scopedIdentityDeniesUnknownPermission(){assertThrows(ApiError.class,()->new Identity("a","u",Set.of("knowledge:read")).require("workflow:write"));}
 @Test void lexicalEmbeddingsStableAndFinite(){assertEquals(KnowledgeService.embedding("hello world"),KnowledgeService.embedding("hello world"));assertFalse(KnowledgeService.embedding(" ").contains("NaN"));assertNotEquals(KnowledgeService.embedding("alpha"),KnowledgeService.embedding("beta"));}
 @Test void actionMaterialParametersBound(){assertNotEquals(ToolGateway.hash("{title:a,body:b}"),ToolGateway.hash("{title:a,body:c}"));}
}
