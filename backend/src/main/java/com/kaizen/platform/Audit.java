package com.kaizen.platform;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component
public class Audit {
 private final JdbcTemplate db;
 public Audit(JdbcTemplate db){this.db=db;}
 public void record(String tenant,String actor,UUID workflow,String type,String detail) {
  db.update("INSERT INTO audit_events(tenant,actor,workflow_id,event_type,detail) VALUES (?,?,?,?,?)",tenant,actor,workflow,type,detail);
 }
}
