package com.kaizen.platform;

import io.micrometer.tracing.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class TraceSupport {
 private final Tracer tracer;
 public TraceSupport(ObjectProvider<Tracer> provider){tracer=provider.getIfAvailable(()->Tracer.NOOP);}
 public record Captured(String traceId,String spanId,boolean sampled) {}
 public Captured capture(){
  Span current=tracer.currentSpan();
  if(current!=null)return new Captured(current.context().traceId(),current.context().spanId(),Boolean.TRUE.equals(current.context().sampled()));
  Span root=tracer.nextSpan().name("workflow.submit").start();
  try{return new Captured(root.context().traceId(),root.context().spanId(),Boolean.TRUE.equals(root.context().sampled()));}finally{root.end();}
 }
 public Scope agent(Map<String,Object> row,String agent){
  var builder=tracer.spanBuilder().name("agent."+agent).tag("workflow.id",row.get("id").toString()).tag("agent.version","1.0.0");
  String trace=(String)row.get("trace_id"),parent=(String)row.get("parent_span_id");
  if(trace!=null&&!trace.isBlank()&&parent!=null&&!parent.isBlank())builder.setParent(tracer.traceContextBuilder().traceId(trace).spanId(parent).sampled((Boolean)row.get("trace_sampled")).build());
  return new Scope(builder.start());
 }
 public Scope child(String name){return new Scope(tracer.nextSpan().name(name).start());}
 public final class Scope implements AutoCloseable {
  private final Span span;private final Tracer.SpanInScope scope;
  private Scope(Span span){this.span=span;scope=tracer.withSpan(span);}
  public void failure(String classification){span.tag("failure.classification",classification);}
  public void close(){scope.close();span.end();}
 }
}
