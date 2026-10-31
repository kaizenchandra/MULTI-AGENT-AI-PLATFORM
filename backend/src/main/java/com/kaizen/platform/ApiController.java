package com.kaizen.platform;

import static com.kaizen.platform.Contracts.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1") @Validated
public class ApiController {
 private final WorkflowService workflows;private final KnowledgeService knowledge;private final AgentRegistry agents;private final ToolGateway tools;private final ModelGateway models;private final JdbcTemplate db;private final EvaluationService evaluations;
 public ApiController(WorkflowService workflows,KnowledgeService knowledge,AgentRegistry agents,ToolGateway tools,ModelGateway models,JdbcTemplate db,EvaluationService evaluations){this.workflows=workflows;this.knowledge=knowledge;this.agents=agents;this.tools=tools;this.models=models;this.db=db;this.evaluations=evaluations;}
 private Identity who(Authentication a){return SecurityConfig.identity(a);}
 @GetMapping("/me") public Identity me(Authentication a){return who(a);}
 @GetMapping("/agents") public Collection<AgentDefinition> agents(){return agents.list();}
 @GetMapping("/workflow-definitions") public List<Plan> definitions(){return List.of(agents.plan(Kind.KNOWLEDGE),agents.plan(Kind.CREATE_CASE));}
 @GetMapping("/tools") public Object tools(){return tools.catalog();}
 @GetMapping("/models") public Object models(){return models.capabilities();}
 @PostMapping("/workflows") @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
 public Object submit(Authentication a,@Valid @RequestBody Submit request,@RequestHeader("Idempotency-Key") String key){return workflows.submit(who(a),request,key);}
 @GetMapping("/workflows") public Object list(Authentication a,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){return workflows.list(who(a),page);}
 @GetMapping("/workflows/{id}") public Object status(Authentication a,@PathVariable UUID id){return workflows.status(who(a),id);}
 @GetMapping("/workflows/{id}/tasks") public Object tasks(Authentication a,@PathVariable UUID id){return workflows.tasks(who(a),id);}
 @PostMapping("/workflows/{id}/cancel") public void cancel(Authentication a,@PathVariable UUID id){workflows.cancel(who(a),id);}
 @GetMapping("/approvals") public Object approvals(Authentication a,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){return workflows.approvals(who(a),page);}
 @PostMapping("/approvals/{id}/decision") public void decide(Authentication a,@PathVariable UUID id,@Valid @RequestBody Decision decision){workflows.decide(who(a),id,decision);}
 @PostMapping("/documents") public Object ingest(Authentication a,@Valid @RequestBody Ingest request){return Map.of("id",knowledge.ingest(who(a),request));}
 @GetMapping("/documents") public Object documents(Authentication a,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){return knowledge.list(who(a),page);}
 @DeleteMapping("/documents/{id}") public void delete(Authentication a,@PathVariable UUID id){knowledge.delete(who(a),id);}
 @PostMapping("/knowledge/search") public Object search(Authentication a,@Valid @RequestBody Query query){return knowledge.search(who(a),query.query());}
 @PostMapping("/memories") public Object memory(Authentication a,@Valid @RequestBody MemoryWrite request){return Map.of("id",knowledge.remember(who(a),request));}
 @GetMapping("/memories") public Object memories(Authentication a){return knowledge.memories(who(a));}
 @DeleteMapping("/memories/{id}") public void forget(Authentication a,@PathVariable UUID id){knowledge.forget(who(a),id);}
 @GetMapping("/audit") public Object audit(Authentication a,@RequestParam(defaultValue="0") @Min(0) @Max(10000) int page){var i=who(a);i.require("audit:read");return db.queryForList("SELECT sequence,actor,workflow_id,event_type,detail,created_at FROM audit_events WHERE tenant=? ORDER BY sequence DESC LIMIT 50 OFFSET ?",i.tenant(),page*50);}
 @GetMapping("/usage") public Object usage(Authentication a){var i=who(a);return db.queryForList("SELECT u.workflow_id,u.provider,u.model,u.prompt_version,u.input_tokens,u.output_tokens,u.estimated_usd,u.duration_ms,u.created_at FROM usage_records u JOIN workflows w ON w.id=u.workflow_id AND w.tenant=u.tenant WHERE u.tenant=? AND w.owner_id=? ORDER BY u.created_at DESC LIMIT 100",i.tenant(),i.subject());}
 @GetMapping("/cases") public Object cases(Authentication a){var i=who(a);return db.queryForList("SELECT c.id,c.workflow_id,c.title,c.description,c.created_at FROM cases c JOIN workflows w ON w.id=c.workflow_id AND w.tenant=c.tenant WHERE c.tenant=? AND w.owner_id=? ORDER BY c.created_at DESC LIMIT 50",i.tenant(),i.subject());}
 @PostMapping("/evaluations") public Object evaluate(Authentication a){var i=who(a);i.require("workflow:write");return evaluations.run(i);}
 @PostMapping("/evaluations/workflows") public Object evaluateWorkflows(Authentication a){return evaluations.runWorkflows(who(a));}
 @GetMapping("/evaluations") public Object evaluations(Authentication a){var i=who(a);return db.queryForList("SELECT id,dataset_version,result_json,created_at FROM evaluation_runs WHERE tenant=? AND actor=? ORDER BY created_at DESC LIMIT 50",i.tenant(),i.subject());}
}
