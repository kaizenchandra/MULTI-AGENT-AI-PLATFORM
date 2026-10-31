package com.kaizen.platform;

import static com.kaizen.platform.Contracts.*;
import java.util.*;
import java.time.Instant;
import com.fasterxml.jackson.core.type.TypeReference;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class WorkflowWorker {
 private final TraceSupport tracing;private final JdbcTemplate db;private final Json json;private final AgentRegistry agents;private final KnowledgeService knowledge;
 private final ModelGateway models;private final ToolGateway tools;private final Audit audit;private final TransactionTemplate tx;private final MeterRegistry metrics;private final boolean enabled;
 private static final Logger log=LoggerFactory.getLogger(WorkflowWorker.class);
 public WorkflowWorker(JdbcTemplate db,Json json,AgentRegistry agents,KnowledgeService knowledge,ModelGateway models,ToolGateway tools,Audit audit,PlatformTransactionManager manager,MeterRegistry metrics,TraceSupport tracing,@Value("${platform.worker.enabled:true}") boolean enabled) {
  this.tracing=tracing;this.db=db;this.json=json;this.agents=agents;this.knowledge=knowledge;this.models=models;this.tools=tools;this.audit=audit;this.metrics=metrics;this.enabled=enabled;
  tx=new TransactionTemplate(manager);tx.setTimeout(25);
 }
 @Scheduled(fixedDelayString="${platform.worker.delay-ms:500}") public void scheduled() {
  if(!enabled)return;
  try { tick(); } catch(Exception e) {metrics.counter("platform.worker.infrastructure_failures").increment();log.error("Workflow polling failed; class={}",e.getClass().getSimpleName());}
 }
 public boolean tick() {return tick(null);}
 public boolean tick(UUID onlyWorkflow) {
  UUID[] selected={null};long[] version={-1};
  try {
   Boolean worked=tx.execute(status->{
    db.execute("SET LOCAL lock_timeout='2s'");db.execute("SET LOCAL statement_timeout='20s'");
    String selection="SELECT * FROM workflows WHERE ((state IN ('CREATED','RUNNING') AND next_attempt_at<=now()) OR (state='WAITING' AND deadline<=now()))";
    var rows=onlyWorkflow==null?db.queryForList(selection+" ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED"):
      db.queryForList(selection+" AND id=? ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED",onlyWorkflow);
    if(rows.isEmpty())return false;var row=rows.getFirst();UUID id=(UUID)row.get("id");selected[0]=id;version[0]=((Number)row.get("version")).longValue();
    String agent=agents.plan(Kind.valueOf((String)row.get("kind"))).steps().get(((Number)row.get("stage")).intValue()).agent();
    try(var span=tracing.agent(row,agent)){try{advance(row);}catch(RuntimeException e){span.failure(e.getClass().getSimpleName());throw e;}}return true;
   });return Boolean.TRUE.equals(worked);
  }catch(Exception e) {
   if(selected[0]==null)throw e;
   handleFailure(selected[0],version[0],e);return true;
  }
 }
 private void advance(Map<String,Object> row) {
  UUID id=(UUID)row.get("id");String tenant=(String)row.get("tenant"),owner=(String)row.get("owner_id");
  var who=new Identity(tenant,owner,Set.of("workflow:write","knowledge:read"));
  State state=State.valueOf((String)row.get("state"));
  if(((java.sql.Timestamp)row.get("deadline")).toInstant().isBefore(Instant.now())) {
   WorkflowService.transition(db,id,state,State.TIMED_OUT);
   db.update("UPDATE tasks SET state='TIMED_OUT' WHERE workflow_id=? AND state NOT IN ('COMPLETED','FAILED')",id);
   db.update("UPDATE approvals SET state='EXPIRED' WHERE workflow_id=? AND state='PENDING'",id);
   audit.record(tenant,owner,id,"WORKFLOW_TIMED_OUT","Deadline exceeded");return;
  }
  if(state==State.CREATED){WorkflowService.transition(db,id,state,State.RUNNING);state=State.RUNNING;}
  int stage=((Number)row.get("stage")).intValue();Kind kind=Kind.valueOf((String)row.get("kind"));var plan=agents.plan(kind);
  if(stage<0||stage>=plan.steps().size())throw new ApiError(422,"Invalid durable stage");
  var step=plan.steps().get(stage);
  if(stage>0 && db.queryForObject("SELECT count(*) FROM tasks WHERE workflow_id=? AND ordinal=? AND state='COMPLETED'",Integer.class,id,stage-1)!=1)throw new ApiError(422,"Unmet dependency");
  db.update("UPDATE tasks SET state='RUNNING',attempt=attempt+1 WHERE workflow_id=? AND ordinal=?",id,stage);
  long start=System.nanoTime();Object output;
  if(stage==0)output=plan;
  else if(kind==Kind.CREATE_CASE) {
   var pending=db.queryForList("SELECT state FROM approvals WHERE workflow_id=? AND tenant=?",id,tenant);
   if(pending.isEmpty()) {
    String action=(String)row.get("action_json");
    db.update("INSERT INTO approvals(id,workflow_id,tenant,action_hash,action_json,requested_by,expires_at) VALUES (?,?,?,?,?,?,?)",UUID.randomUUID(),id,tenant,ToolGateway.hash(action),action,owner,row.get("deadline"));
    WorkflowService.transition(db,id,state,State.WAITING);
    db.update("UPDATE tasks SET state='WAITING' WHERE workflow_id=? AND ordinal=?",id,stage);
    audit.record(tenant,owner,id,"APPROVAL_REQUESTED",ToolGateway.hash(action));return;
   }
   output=tools.createCase(who,id,json.read((String)row.get("action_json"),Action.class));
   db.update("UPDATE workflows SET result_json=? WHERE id=?",json.write(output),id);
  } else if(stage==1) {
   agents.authorizeTool("research","knowledge.search");
   output=knowledge.search(who,(String)row.get("goal"));
   db.update("UPDATE workflows SET context_json=? WHERE id=?",json.write(output),id);
  } else {
   List<Evidence> evidence=json.read((String)row.get("context_json"),new TypeReference<List<Evidence>>(){});
   // Recheck source access before sending retained context to any provider, not only before publication.
   for(var e:evidence)if(!knowledge.authorized(who,new Claim(e.documentId(),e.chunkId(),e.documentVersion(),e.quote())))throw new ApiError(422,"Evidence revoked during execution");
   if(stage==2) {
    var result=models.analyze((String)row.get("goal"),evidence);knowledge.verify(who,evidence,result.answer());
    // Uncertainty is application controlled. Provider text is not published as factual commentary.
    output=new Answer(result.answer().claims(),result.answer().claims().isEmpty()?"No supported evidence; abstained.":"Quoted evidence only. Sources may be incomplete; no additional inference verified.");
    db.update("INSERT INTO usage_records(id,tenant,workflow_id,provider,model,prompt_version,input_tokens,output_tokens,estimated_usd,duration_ms) VALUES (?,?,?,?,?,'grounded-1',?,?,?,?)",UUID.randomUUID(),tenant,id,result.provider(),result.model(),result.inputTokens(),result.outputTokens(),result.estimatedUsd(),(System.nanoTime()-start)/1000000);
    db.update("UPDATE workflows SET result_json=? WHERE id=?",json.write(output),id);
   } else {
    var answer=json.read((String)row.get("result_json"),Answer.class);knowledge.verify(who,evidence,answer);output=answer;
   }
  }
  long duration=(System.nanoTime()-start)/1000000;
  db.update("UPDATE tasks SET state='COMPLETED',output_json=?,duration_ms=? WHERE workflow_id=? AND ordinal=?",json.write(output),duration,id,stage);
  db.update("UPDATE workflows SET stage=stage+1,version=version+1,attempts=0,updated_at=now(),next_attempt_at=now() WHERE id=?",id);
  metrics.timer("platform.agent.duration","agent",step.agent()).record(java.time.Duration.ofMillis(duration));
  audit.record(tenant,owner,id,"TASK_COMPLETED",step.agent()+"@1.0.0 stage="+stage);
  log.info("Task completed workflow={} agent={} stage={} durationMs={}",id,step.agent(),stage,duration);
  if(stage+1==plan.steps().size()) {
   WorkflowService.transition(db,id,state,State.COMPLETED);metrics.counter("platform.workflows.completed","kind",kind.name()).increment();
   audit.record(tenant,owner,id,"WORKFLOW_COMPLETED",kind.name());
  }
 }
 private void handleFailure(UUID id,long version,Exception failure) {
  tx.executeWithoutResult(status->{
   var rows=db.queryForList("SELECT * FROM workflows WHERE id=? AND version=? AND state IN ('CREATED','RUNNING') FOR UPDATE",id,version);
   if(rows.isEmpty())return;var row=rows.getFirst();int attempt=((Number)row.get("attempts")).intValue()+1;
   boolean retry=failure instanceof ModelGateway.TransientModelFailure && attempt<3;
   String reason=failure instanceof ApiError?"POLICY_OR_VALIDATION":failure instanceof ModelGateway.TransientModelFailure?"MODEL_UNAVAILABLE":"EXECUTION_FAILURE";
   db.update("UPDATE workflows SET attempts=?,failure=?,next_attempt_at=now()+(? * interval '1 second'),version=version+1,updated_at=now() WHERE id=?",attempt,reason,(int)Math.pow(2,attempt),id);
   db.update("UPDATE tasks SET attempt=attempt+1,state=? WHERE workflow_id=? AND ordinal=?",retry?"CREATED":"FAILED",id,row.get("stage"));
   if(!retry){WorkflowService.transition(db,id,State.valueOf((String)row.get("state")),State.FAILED);db.update("UPDATE tasks SET state='CANCELLED' WHERE workflow_id=? AND state='CREATED'",id);}
   audit.record((String)row.get("tenant"),(String)row.get("owner_id"),id,retry?"RETRY_SCHEDULED":"WORKFLOW_FAILED",reason);
   metrics.counter("platform.workflow.failures","reason",reason).increment();
   log.warn("Workflow execution failed workflow={} reason={} retry={}",id,reason,retry);
  });
 }
}
