#!/usr/bin/env python3
"""Real API + Compose backend restart acceptance. No cloud or model calls."""
import json, os, subprocess, time, urllib.request, uuid
from pathlib import Path
root=Path(__file__).resolve().parents[1]
settings=dict(line.strip().split("=",1) for line in (root/".env").read_text().splitlines() if line and not line.startswith("#"))
base=os.environ.get("API_URL","http://127.0.0.1:18080")
def request(path,token,body=None,key=None):
 headers={"Authorization":"Bearer "+token}
 if body is not None: headers["Content-Type"]="application/json"
 if key:headers["Idempotency-Key"]=key
 req=urllib.request.Request(base+"/api/v1"+path,data=None if body is None else json.dumps(body).encode(),headers=headers)
 with urllib.request.urlopen(req,timeout=10) as response:
  data=response.read();return json.loads(data) if data else None
user=settings["LOCAL_USER_TOKEN"];approver=settings["LOCAL_APPROVER_TOKEN"]
key=str(uuid.uuid4());body={"kind":"CREATE_CASE","goal":"Verify durable restart recovery","action":{"title":"Recovery smoke case","description":"Local restart acceptance; approved exact action only."}}
workflow=request("/workflows",user,body,key)["id"]
def wait_state(expected):
 for _ in range(90):
  try:
   result=request("/workflows/"+workflow,user)
   if result["state"]==expected:return result
   if result["state"] in {"FAILED","REJECTED","TIMED_OUT","CANCELLED"}:raise AssertionError(result["state"])
  except (urllib.error.URLError,ConnectionError):pass
  time.sleep(.5)
 raise AssertionError("Timed out waiting for "+expected)
def restart():
 subprocess.run(["docker","compose","kill","-s","SIGKILL","backend"],cwd=root,check=True,stdout=subprocess.DEVNULL)
 subprocess.run(["docker","compose","up","-d","--wait","backend"],cwd=root,check=True,stdout=subprocess.DEVNULL)
wait_state("WAITING");restart();wait_state("WAITING")
approval=next(a for a in request("/approvals",approver) if a["workflow_id"]==workflow)
request("/approvals/"+approval["id"]+"/decision",approver,{"approve":True,"actionHash":approval["action_hash"]})
first=wait_state("COMPLETED");restart();second=wait_state("COMPLETED")
assert first["result"]["caseId"]==second["result"]["caseId"]
assert request("/workflows",user,body,key)["id"]==workflow
cases=[c for c in request("/cases",user) if c["workflow_id"]==workflow]
assert len(cases)==1
print(json.dumps({"workflowId":workflow,"approvalSurvivedRestart":True,"completedResultSurvivedRestart":True,"caseCount":len(cases),"submissionReplayStable":True}))
