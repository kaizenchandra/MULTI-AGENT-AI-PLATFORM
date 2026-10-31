package com.kaizen.platform;
import static com.kaizen.platform.Contracts.*;
import java.util.*;
import org.springframework.stereotype.Component;
@Component
public class AgentRegistry {
 private final Map<String,AgentDefinition> agents;
 public AgentRegistry() {
  var map=new LinkedHashMap<String,AgentDefinition>();
  add(map,"planner","Compile approved bounded workflow",List.of(),"workflow:write","Plan.v1");
  add(map,"research","Retrieve authorized evidence",List.of("knowledge.search"),"knowledge:read","Evidence.v1[]");
  add(map,"analysis","Select grounded evidence claims",List.of(),"knowledge:read","Answer.v1");
  add(map,"verification","Validate exact claims and current authorization",List.of("knowledge.search"),"knowledge:read","Answer.v1");
  add(map,"synthesis","Publish verified evidence with uncertainty",List.of(),"knowledge:read","Answer.v1");
  add(map,"execution","Create approved local case",List.of("case.create"),"workflow:write","CaseResult.v1");
  agents=Collections.unmodifiableMap(map);
 }
 private void add(Map<String,AgentDefinition> map,String id,String responsibility,List<String> tools,String scope,String output) {
  map.put(id,new AgentDefinition(id,"1.0.0",responsibility,List.of(id),tools,"TaskEnvelope.v1",output,
   id.equals("analysis")?"configured-provider-or-extractive":"deterministic",scope,20,4096,0.10,1,3,"fail-closed"));
 }
 public Collection<AgentDefinition> list(){ return agents.values(); }
 public AgentDefinition get(String id){ var a=agents.get(id); if(a==null) throw new ApiError(403,"Unknown agent"); return a; }
 public Plan plan(Kind kind) {
  var ids=kind==Kind.KNOWLEDGE?List.of("planner","research","analysis","verification","synthesis"):List.of("planner","execution");
  var steps=new ArrayList<Step>();
  for(int n=0;n<ids.size();n++){var a=get(ids.get(n));steps.add(new Step(n,a.id(),n==0?null:n-1,a.allowedTools(),a.outputSchema(),a.id().equals("execution")));}
  var plan=new Plan("1",kind,List.copyOf(steps),6,600);validate(plan);return plan;
 }
 public void validate(Plan plan) {
  if(plan.steps().isEmpty() || plan.steps().size()>6 || plan.deadlineSeconds()>600) throw new ApiError(422,"Plan budget exceeded");
  for(int n=0;n<plan.steps().size();n++) {
   var s=plan.steps().get(n);var a=get(s.agent());
   if(s.ordinal()!=n || !Objects.equals(s.dependency(),n==0?null:n-1) || !a.allowedTools().containsAll(s.tools()) || s.agent().equals("execution")&&!s.approvalRequired()) throw new ApiError(422,"Invalid delegation plan");
  }
  var expected=plan.kind()==Kind.KNOWLEDGE?List.of("planner","research","analysis","verification","synthesis"):List.of("planner","execution");
  if(!plan.steps().stream().map(Step::agent).toList().equals(expected)) throw new ApiError(422,"Workflow not allowlisted");
 }
 public void authorizeTool(String agent,String tool){ if(!get(agent).allowedTools().contains(tool)) throw new ApiError(403,"Tool delegation denied"); }
}
