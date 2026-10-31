package com.kaizen.platform;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class RetentionJob {
 private final JdbcTemplate db;
 public RetentionJob(JdbcTemplate db){this.db=db;}
 @Scheduled(fixedDelay=3600000) @Transactional
 public void expire(){
  db.update("DELETE FROM memories WHERE expires_at<=now()");
  db.update("DELETE FROM documents WHERE expires_at<=now()");
  db.update("DELETE FROM api_rate_limits WHERE window_start<now()-interval '1 day'");
  // Purge retained payloads while preserving event metadata and approved action fingerprints.
  db.update("UPDATE tasks SET output_json='{}' WHERE deadline<now()-interval '7 days'");
  db.update("UPDATE workflows SET context_json='[]',result_json='{}' WHERE deadline<now()-interval '7 days' AND state IN ('COMPLETED','FAILED','CANCELLED','TIMED_OUT','REJECTED')");
 }
}
