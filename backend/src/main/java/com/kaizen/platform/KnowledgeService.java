package com.kaizen.platform;

import static com.kaizen.platform.Contracts.*;
import java.util.*;
import java.util.regex.Pattern;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeService {
 private final JdbcTemplate db; private final Audit audit;
 private static final Pattern WORDS=Pattern.compile("[\\p{L}\\p{N}]{2,}");
 public KnowledgeService(JdbcTemplate db,Audit audit){this.db=db;this.audit=audit;}
 // A reproducible lexical feature embedding, not a learned semantic model. Version hash64-v1.
 public static String embedding(String text) {
  double[] v=new double[64];var matcher=WORDS.matcher(text.toLowerCase(Locale.ROOT));
  while(matcher.find()){int h=matcher.group().hashCode();v[Math.floorMod(h,64)]+=1;}
  double norm=Math.sqrt(Arrays.stream(v).map(x->x*x).sum());
  if(norm==0)v[0]=1;else for(int i=0;i<v.length;i++)v[i]/=norm;
  return Arrays.toString(v);
 }
 @Transactional public UUID ingest(Identity who,Ingest request) {
  who.require("knowledge:write");
  db.queryForObject("SELECT pg_advisory_xact_lock(hashtext(?))::text",String.class,who.tenant());
  if(db.queryForObject("SELECT count(*) FROM documents WHERE tenant=?",Integer.class,who.tenant())>=1000) throw new ApiError(429,"Document quota reached");
  UUID id=UUID.randomUUID();
  db.update("INSERT INTO documents(id,tenant,owner_id,title,body,visibility,expires_at) VALUES (?,?,?,?,?,?,now()+interval '30 days')",id,who.tenant(),who.subject(),request.title(),request.text(),request.visibility());
  int ordinal=0;
  for(String paragraph:request.text().split("\\n\\s*\\n")) {
   for(int offset=0;offset<paragraph.length();offset+=900) {
    String content=paragraph.substring(offset,Math.min(offset+1000,paragraph.length()));
    db.update("INSERT INTO chunks(id,document_id,tenant,ordinal,content,embedding) VALUES (?,?,?,?,?,?::vector)",UUID.randomUUID(),id,who.tenant(),ordinal++,content,embedding(content));
   }
  }
  audit.record(who.tenant(),who.subject(),null,"DOCUMENT_INGESTED",id.toString());return id;
 }
 public List<Evidence> search(Identity who,String query) {
  who.require("knowledge:read");
  return db.query("""
   SELECT d.id AS doc,c.id AS chunk,d.version,d.title,c.content,
    (0.65*ts_rank_cd(c.terms,plainto_tsquery('english',?)) + 0.35*(1-(c.embedding <=> ?::vector))) AS score
   FROM chunks c JOIN documents d ON d.id=c.document_id AND d.tenant=c.tenant
   WHERE d.tenant=? AND (d.owner_id=? OR d.visibility='TENANT') AND d.expires_at>now()
    AND c.terms @@ plainto_tsquery('english',?)
   ORDER BY score DESC,c.id LIMIT 6
   """,(rs,n)->new Evidence(rs.getObject("doc",UUID.class),rs.getObject("chunk",UUID.class),rs.getInt("version"),rs.getString("title"),rs.getString("content"),rs.getDouble("score")),query,embedding(query),who.tenant(),who.subject(),query);
 }
 public boolean authorized(Identity who,Claim claim) {
  return Boolean.TRUE.equals(db.queryForObject("""
   SELECT EXISTS(SELECT 1 FROM documents d JOIN chunks c ON c.document_id=d.id AND c.tenant=d.tenant
    WHERE d.tenant=? AND (d.owner_id=? OR d.visibility='TENANT') AND d.expires_at>now()
    AND d.id=? AND d.version=? AND c.id=? AND position(? in c.content)>0)
   """,Boolean.class,who.tenant(),who.subject(),claim.documentId(),claim.documentVersion(),claim.chunkId(),claim.quote()));
 }
 public void verify(Identity who,List<Evidence> supplied,Answer answer) {
  if(answer==null || answer.claims()==null || answer.claims().size()>6 || answer.uncertainty()==null || answer.uncertainty().length()>500) throw new ApiError(422,"Invalid model output");
  for(Claim claim:answer.claims()) {
   if(claim==null || claim.quote()==null || claim.quote().isBlank() || claim.quote().length()>1000 ||
      supplied.stream().noneMatch(e->e.documentId().equals(claim.documentId()) && e.chunkId().equals(claim.chunkId()) && e.documentVersion()==claim.documentVersion() && e.quote().contains(claim.quote())) || !authorized(who,claim)) throw new ApiError(422,"Unverified evidence claim");
  }
 }
 @Transactional public void delete(Identity who,UUID id) {
  who.require("knowledge:write");
  int n=db.update("DELETE FROM documents WHERE id=? AND tenant=? AND owner_id=?",id,who.tenant(),who.subject());
  if(n==0)throw new ApiError(404,"Document not found");
  audit.record(who.tenant(),who.subject(),null,"DOCUMENT_DELETED",id.toString());
 }
 public List<Map<String,Object>> list(Identity who,int page) {
  who.require("knowledge:read");return db.queryForList("SELECT id,title,visibility,version,created_at,expires_at FROM documents WHERE tenant=? AND (owner_id=? OR visibility='TENANT') AND expires_at>now() ORDER BY created_at DESC LIMIT 50 OFFSET ?",who.tenant(),who.subject(),page*50);
 }
 @Transactional public UUID remember(Identity who,MemoryWrite write) {
  who.require("memory:write");who.require("knowledge:read");
  // Only verbatim, currently authorized source facts can enter durable memory.
  var matches=db.queryForList("SELECT id FROM documents WHERE id=? AND tenant=? AND (owner_id=? OR visibility='TENANT') AND expires_at>now() AND position(? in body)>0",write.sourceDocument(),who.tenant(),who.subject(),write.fact());
  if(matches.isEmpty())throw new ApiError(422,"Memory requires an authorized verbatim source");
  UUID id=UUID.randomUUID();db.update("INSERT INTO memories(id,tenant,owner_id,fact,source_document,expires_at) VALUES (?,?,?,?,?,now()+interval '7 days')",id,who.tenant(),who.subject(),write.fact(),write.sourceDocument());
  audit.record(who.tenant(),who.subject(),null,"MEMORY_WRITTEN",id.toString());return id;
 }
 public List<Map<String,Object>> memories(Identity who) {
  who.require("knowledge:read");return db.queryForList("""
   SELECT m.id,m.fact,m.source_document,m.expires_at FROM memories m JOIN documents d ON d.id=m.source_document
   WHERE m.tenant=? AND m.owner_id=? AND m.expires_at>now() AND d.expires_at>now() AND d.tenant=m.tenant
   AND (d.owner_id=? OR d.visibility='TENANT') ORDER BY m.created_at DESC LIMIT 50
   """,who.tenant(),who.subject(),who.subject());
 }
 @Transactional public void forget(Identity who,UUID id) {
  who.require("memory:write");if(db.update("DELETE FROM memories WHERE id=? AND tenant=? AND owner_id=?",id,who.tenant(),who.subject())==0)throw new ApiError(404,"Memory not found");
  audit.record(who.tenant(),who.subject(),null,"MEMORY_DELETED",id.toString());
 }
}
