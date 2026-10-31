package com.kaizen.platform;

import static com.kaizen.platform.Contracts.*;
import java.util.*;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {
 private final TraceSupport tracing;private final JdbcTemplate db;private final Json json;private final AgentRegistry agents;private final Audit audit;private final KnowledgeService knowledge;
 public WorkflowService(JdbcTemplate db,Json json,AgentRegistry agents,Audit audit,KnowledgeService knowledge,TraceSupport tracing){this.tracing=tracing;this.db=db;this.json=json;this.agents=agents;this.audit=audit;this.knowledge=knowledge;}
 @Transactional public Map<String,Object> submit(Identity who,Submit request,String key) {
  who.require("workflow:write");
  if(request.kind()==Kind.KNOWLEDGE)who.require("knowledge:read");
  if(key==null || !key.matches("[a-zA-Z0-9_.:-]{1,100}"))throw new ApiError(400,"Valid Idempotency-Key required");
  if((request.kind()==Kind.CREATE_CASE)!=(request.action()!=null))throw new ApiError(422,"Action required only for CREATE_CASE");
  db.queryForObject("SELECT pg_advisory_xact_lock(hashtext(?))::text",String.class,who.tenant());
  String fingerprint=ToolGateway.hash(json.write(request));
  var existing=db.queryForList("SELECT id,request_hash FROM workflows WHERE tenant=? AND owner_id=? AND idempotency_key=?",who.tenant(),who.subject(),key);
  if(!existing.isEmpty()) {
   if(!existing.getFirst().get("request_hash").equals(fingerprint))throw new ApiError(409,"Idempotency key reused with different request");
   return Map.of("id",existing.getFirst().get("id"),"replayed",true);
  }
  if(db.queryForObject("SELECT count(*) FROM workflows WHERE tenant=? AND state IN ('CREATED','RUNNING','WAITING')",Integer.class,who.tenant())>=20)throw new ApiError(429,"Active workflow quota reached");
  if(db.queryForObject("SELECT count(*) FROM workflows WHERE tenant=? AND created_at>now()-interval '1 hour'",Integer.class,who.tenant())>=200)throw new ApiError(429,"Hourly workflow quota reached");
  var plan=agents.plan(request.kind());UUID id=UUID.randomUUID();Instant deadline=Instant.now().plusSeconds(plan.deadlineSeconds());
  db.update("INSERT INTO workflows(id,tenant,owner_id,kind,goal,action_json,request_hash,idempotency_key,state,deadline) VALUES (?,?,?,?,?,?,?,?,'CREATED',?)",id,who.tenant(),who.subject(),request.kind().name(),request.goal(),json.write(request.action()),fingerprint,key,java.sql.Timestamp.from(deadline));
  var trace=tracing.capture();
  db.update("UPDATE workflows SET trace_id=?,parent_span_id=?,trace_sampled=? WHERE id=?",trace.traceId(),trace.spanId(),trace.sampled(),id);
  UUID root=UUID.randomUUID();
  for(var step:plan.steps())db.update("""
   INSERT INTO tasks(id,workflow_id,tenant,parent_id,source_agent,agent,agent_version,ordinal,dependency_ordinal,state,deadline,correlation_id)
   VALUES (?,?,?,?,'supervisor',?,'1.0.0',?,?,'CREATED',?,?)
   """,step.ordinal()==0?root:UUID.randomUUID(),id,who.tenant(),step.ordinal()==0?null:root,step.agent(),step.ordinal(),step.dependency(),java.sql.Timestamp.from(deadline),id);
  audit.record(who.tenant(),who.subject(),id,"WORKFLOW_CREATED",request.kind().name());return Map.of("id",id,"replayed",false);
 }
 public Map<String,Object> owned(Identity who,UUID id) {
  var rows=db.queryForList("SELECT * FROM workflows WHERE id=? AND tenant=? AND owner_id=?",id,who.tenant(),who.subject());
  if(rows.isEmpty())throw new ApiError(404,"Workflow not found");return rows.getFirst();
 }
 public Map<String,Object> status(Identity who,UUID id) {
  var row=owned(who,id);
  Object result=json.read((String)row.get("result_json"),Object.class);
  if(row.get("kind").equals("KNOWLEDGE") && row.get("state").equals("COMPLETED")) {
   who.require("knowledge:read");if(row.get("result_json").equals("{}"))throw new ApiError(410,"Result retention expired");var answer=json.read((String)row.get("result_json"),Answer.class);
   if(answer.claims().stream().anyMatch(c->!knowledge.authorized(who,c)))throw new ApiError(409,"Result evidence revoked; submit a new workflow");
  }
  var out=new LinkedHashMap<String,Object>();
  for(String key:List.of("id","kind","goal","state","stage","version","attempts","created_at","updated_at","deadline","trace_id"))out.put(key,row.get(key));
  out.put("failure",row.get("failure"));out.put("result",result);return out;
 }
 public List<Map<String,Object>> list(Identity who,int page) {
  return db.queryForList("SELECT id,kind,state,stage,attempts,created_at,updated_at,failure FROM workflows WHERE tenant=? AND owner_id=? ORDER BY created_at DESC LIMIT 50 OFFSET ?",who.tenant(),who.subject(),page*50);
 }
 public List<Map<String,Object>> tasks(Identity who,UUID id) {
  owned(who,id);
  // Intermediate payloads remain internal: deleted/revoked evidence cannot leak via task inspection.
  return db.queryForList("SELECT id,parent_id,source_agent,agent,agent_version,schema_version,ordinal,dependency_ordinal,state,attempt,deadline,correlation_id,duration_ms FROM tasks WHERE workflow_id=? AND tenant=? ORDER BY ordinal",id,who.tenant());
 }
 @Transactional public void cancel(Identity who,UUID id) {
  who.require("workflow:write");var rows=db.queryForList("SELECT * FROM workflows WHERE id=? AND tenant=? AND owner_id=? FOR UPDATE",id,who.tenant(),who.subject());
  if(rows.isEmpty())throw new ApiError(404,"Workflow not found");
  State current=State.valueOf((String)rows.getFirst().get("state"));
  if(current.terminal())throw new ApiError(409,"Workflow already terminal");
  transition(db,id,current,State.CANCELLED);
  db.update("UPDATE approvals SET state='CANCELLED' WHERE workflow_id=? AND state='PENDING'",id);
  db.update("UPDATE tasks SET state='CANCELLED' WHERE workflow_id=? AND state IN ('CREATED','RUNNING','WAITING')",id);
  audit.record(who.tenant(),who.subject(),id,"WORKFLOW_CANCELLED","User cancellation");
 }
 public List<Map<String,Object>> approvals(Identity who,int page) {
  who.require("approval:decide");return db.queryForList("SELECT id,workflow_id,action_hash,action_json,state,requested_by,decided_by,expires_at,decided_at FROM approvals WHERE tenant=? ORDER BY created_at DESC LIMIT 50 OFFSET ?",who.tenant(),page*50);
 }
 @Transactional public void decide(Identity who,UUID id,Decision decision) {
  who.require("approval:decide");
  var refs=db.queryForList("SELECT workflow_id FROM approvals WHERE id=? AND tenant=?",id,who.tenant());
  if(refs.isEmpty())throw new ApiError(404,"Approval not found");UUID workflow=(UUID)refs.getFirst().get("workflow_id");
  // Lock order is always workflow then approval, shared with the worker and cancellation path.
  var w=db.queryForMap("SELECT state,deadline FROM workflows WHERE id=? AND tenant=? FOR UPDATE",workflow,who.tenant());
  var a=db.queryForMap("SELECT * FROM approvals WHERE id=? AND tenant=? FOR UPDATE",id,who.tenant());
  if(who.subject().equals(a.get("requested_by")))throw new ApiError(403,"Requester cannot approve own action");
  if(!w.get("state").equals("WAITING") || !a.get("state").equals("PENDING"))throw new ApiError(409,"Approval already resolved or workflow unavailable");
  if(!a.get("action_hash").equals(decision.actionHash()))throw new ApiError(409,"Proposed action hash mismatch");
  boolean expired=((java.sql.Timestamp)a.get("expires_at")).toInstant().isBefore(Instant.now()) || ((java.sql.Timestamp)w.get("deadline")).toInstant().isBefore(Instant.now());
  if(expired)throw new ApiError(409,"Approval expired");
  db.update("UPDATE approvals SET state=?,decided_by=?,decided_at=now() WHERE id=?",decision.approve()?"APPROVED":"REJECTED",who.subject(),id);
  transition(db,workflow,State.WAITING,decision.approve()?State.RUNNING:State.REJECTED);
  if(!decision.approve())db.update("UPDATE tasks SET state='REJECTED' WHERE workflow_id=? AND state IN ('CREATED','WAITING')",workflow);
  audit.record(who.tenant(),who.subject(),workflow,decision.approve()?"ACTION_APPROVED":"ACTION_REJECTED",decision.actionHash());
 }
 static void transition(JdbcTemplate db,UUID id,State from,State to) {
  from.requireTransition(to);
  if(db.update("UPDATE workflows SET state=?,version=version+1,updated_at=now() WHERE id=? AND state=?",to.name(),id,from.name())!=1)throw new ApiError(409,"Concurrent workflow transition");
 }
}
