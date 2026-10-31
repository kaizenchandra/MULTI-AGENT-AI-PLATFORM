#!/usr/bin/env python3
"""Small bounded authenticated-read measurement, not an SLO or capacity test."""
import concurrent.futures,json,os,statistics,time,urllib.request
from pathlib import Path
root=Path(__file__).resolve().parents[1]
settings=dict(line.strip().split("=",1) for line in (root/".env").read_text().splitlines() if line and not line.startswith("#"))
base=os.environ.get("API_URL","http://127.0.0.1:18080")
def read(_):
 start=time.perf_counter();req=urllib.request.Request(base+"/api/v1/me",headers={"Authorization":"Bearer "+settings["LOCAL_USER_TOKEN"]})
 with urllib.request.urlopen(req,timeout=10) as response: assert response.status==200;response.read()
 return (time.perf_counter()-start)*1000
start=time.perf_counter()
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool: values=sorted(pool.map(read,range(40)))
print(json.dumps({"operation":"GET /api/v1/me","requests":len(values),"concurrency":4,"p50Ms":round(statistics.median(values),3),"p95Ms":round(values[int(len(values)*.95)-1],3),"elapsedSeconds":round(time.perf_counter()-start,3),"scope":"local read smoke only"}))
