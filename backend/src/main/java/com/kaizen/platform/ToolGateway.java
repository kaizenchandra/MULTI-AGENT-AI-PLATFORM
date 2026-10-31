package com.kaizen.platform;
import static com.kaizen.platform.Contracts.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ToolGateway {
 private final JdbcTemplate db;private final AgentRegistry agents;private final Audit audit;private final Json json;
 public ToolGateway(JdbcTemplate db,AgentRegistry agents,Audit audit,Json json){this.db=db;this.agents=agents;this.audit=audit;this.json=json;}
 public static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 public List<Map<String,Object>> catalog(){return List.of(Map.of("id","knowledge.search","effect","READ","allowedAgents",List.of("research","verification"),"inputSchema","Query.v1","approvalRequired",false),Map.of("id","case.create","effect","WRITE_LOCAL","allowedAgents",List.of("execution"),"inputSchema","Action.v1","approvalRequired",true));}
 // Must participate in the locked workflow transaction. Local integration and invocation ledger commit atomically.
 @Transactional(propagation=Propagation.MANDATORY)
 public Map<String,Object> createCase(Identity who,UUID workflow,Action action) {
  who.require("workflow:write");agents.authorizeTool("execution","case.create");
  String serialized=json.write(action),hash=hash(serialized);
  var existing=db.queryForList("SELECT action_hash,result_json FROM tool_invocations WHERE tenant=? AND workflow_id=? AND tool='case.create'",who.tenant(),workflow);
  if(!existing.isEmpty()) {
   if(!existing.getFirst().get("action_hash").equals(hash))throw new ApiError(409,"Idempotency action conflict");
   return json.read((String)existing.getFirst().get("result_json"),new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
  }
  Boolean approved=db.queryForObject("""
   SELECT EXISTS(SELECT 1 FROM approvals a JOIN workflows w ON w.id=a.workflow_id AND w.tenant=a.tenant
    WHERE a.tenant=? AND a.workflow_id=? AND a.state='APPROVED' AND a.action_hash=? AND a.action_json=?
    AND a.expires_at>now() AND a.decided_by<>a.requested_by AND w.owner_id=? AND w.state='RUNNING'
    AND w.action_json=a.action_json AND w.deadline>now())
   """,Boolean.class,who.tenant(),workflow,hash,serialized,who.subject());
  if(!Boolean.TRUE.equals(approved))throw new ApiError(403,"Valid exact-action approval required");
  UUID id=UUID.randomUUID();
  db.update("INSERT INTO cases(id,tenant,workflow_id,title,description) VALUES (?,?,?,?,?)",id,who.tenant(),workflow,action.title(),action.description());
  Map<String,Object> result=Map.of("caseId",id,"integration","local-case-store","actionHash",hash);
  db.update("INSERT INTO tool_invocations(id,tenant,workflow_id,tool,action_hash,state,result_json) VALUES (?,?,?,'case.create',?,'COMPLETED',?)",UUID.randomUUID(),who.tenant(),workflow,hash,json.write(result));
  audit.record(who.tenant(),who.subject(),workflow,"TOOL_COMPLETED","case.create "+id);
  return result;
 }
}
