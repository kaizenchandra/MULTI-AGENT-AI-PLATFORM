package com.kaizen.platform;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class Contracts {
 private Contracts() {}
 public enum Kind { KNOWLEDGE, CREATE_CASE }
 public enum State { CREATED, RUNNING, WAITING, COMPLETED, FAILED, TIMED_OUT, CANCELLED, REJECTED;
  public boolean terminal() { return Set.of(COMPLETED,FAILED,TIMED_OUT,CANCELLED,REJECTED).contains(this); }
  public void requireTransition(State target) {
   boolean allowed = !terminal() && (target==CANCELLED || target==TIMED_OUT || target==FAILED ||
     this==CREATED && target==RUNNING || this==RUNNING && Set.of(WAITING,COMPLETED).contains(target) ||
     this==WAITING && Set.of(RUNNING,REJECTED).contains(target));
   if (!allowed) throw new IllegalStateException("Invalid workflow transition");
  }
 }
 public record Identity(String tenant, String subject, Set<String> scopes) {
  public void require(String scope) { if (!scopes.contains(scope)) throw new ApiError(403,"Permission denied"); }
 }
 public record Action(@NotBlank @Size(max=200) String title, @NotBlank @Size(max=2000) String description) {}
 public record Submit(@NotNull Kind kind, @NotBlank @Size(max=2000) String goal, @jakarta.validation.Valid Action action) {}
 public record Ingest(@NotBlank @Size(max=200) String title,@NotBlank @Size(max=100000) String text,
                      @Pattern(regexp="PRIVATE|TENANT") @NotNull String visibility) {}
 public record Query(@NotBlank @Size(max=1000) String query) {}
 public record Decision(@NotNull Boolean approve,@NotBlank @Size(max=64) String actionHash) {}
 public record MemoryWrite(@NotNull UUID sourceDocument,@NotBlank @Size(max=2000) String fact) {}
 public record Evidence(UUID documentId,UUID chunkId,int documentVersion,String title,String quote,double score) {}
 public record Claim(UUID documentId,UUID chunkId,int documentVersion,String quote) {}
 public record Answer(List<Claim> claims,String uncertainty) {}
 public record ModelResult(Answer answer,String provider,String model,int inputTokens,int outputTokens,double estimatedUsd) {}
 public record AgentDefinition(String id,String version,String responsibility,List<String> capabilities,List<String> allowedTools,
   String inputSchema,String outputSchema,String modelPolicy,String authorizationScope,int timeoutSeconds,int tokenBudget,
   double costBudget,int maxIterations,int maxAttempts,String failureBehavior) {}
 public record Step(int ordinal,String agent,Integer dependency,List<String> tools,String outputSchema,boolean approvalRequired) {}
 public record Plan(String schemaVersion,Kind kind,List<Step> steps,int maxSteps,int deadlineSeconds) {}
 public static class ApiError extends RuntimeException {
  public final int status;
  public ApiError(int status,String message) { super(message); this.status=status; }
 }
}
