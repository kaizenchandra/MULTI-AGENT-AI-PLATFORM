export type Identity={tenant:string;subject:string;scopes:string[]};
export type Claim={documentId:string;chunkId:string;documentVersion:number;quote:string};
export type Workflow={id:string;kind:string;state:string;stage:number;attempts:number;created_at:string;failure?:string;goal?:string;result?:{claims?:Claim[];uncertainty?:string;caseId?:string;integration?:string}};
export type Task={id:string;agent:string;agent_version:string;state:string;ordinal:number;duration_ms:number;dependency_ordinal:number|null};
export type Agent={id:string;version:string;responsibility:string;allowedTools:string[];tokenBudget:number;costBudget:number;timeoutSeconds:number;authorizationScope:string};
export type Approval={id:string;workflow_id:string;action_hash:string;action_json:string;state:string;requested_by:string;decided_by?:string;expires_at:string};
export type Document={id:string;title:string;visibility:string;version:number;created_at:string};
export type Evidence={documentId:string;chunkId:string;documentVersion:number;title:string;quote:string;score:number};
export class ApiFailure extends Error {constructor(public status:number,message:string){super(message)}}
export async function api<T>(token:string,path:string,options:RequestInit={}):Promise<T>{
 const headers=new Headers(options.headers);headers.set('Authorization',`Bearer ${token}`);if(options.body)headers.set('Content-Type','application/json');
 const response=await fetch(`/api/v1${path}`,{...options,headers,cache:'no-store'});
 if(!response.ok){let message=`Request failed (${response.status})`;try{const body=await response.json();if(body.detail)message=body.detail;}catch{ /* Status remains actionable if server has no JSON body. */ }
  throw new ApiFailure(response.status,response.status===401?'Session expired or token invalid. Sign in again.':response.status===403?'Your identity does not have permission for this action.':message);
 }
 const text=await response.text();return text?JSON.parse(text) as T:undefined as T;
}
export function submitOptions(body:unknown,key:string=crypto.randomUUID()):RequestInit{return {method:'POST',headers:{'Idempotency-Key':key},body:JSON.stringify(body)}}
