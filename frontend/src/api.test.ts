import {describe,it,expect,vi,afterEach} from 'vitest';
import {api,ApiFailure,submitOptions} from './api';
afterEach(()=>vi.unstubAllGlobals());
describe('API client',()=>{
 it('sends credentials only in authorization and preserves idempotency',async()=>{const fetcher=vi.fn().mockResolvedValue(new Response('{"id":"one"}',{status:202}));vi.stubGlobal('fetch',fetcher);await api('secret','/workflows',submitOptions({kind:'KNOWLEDGE'},'stable'));const init=fetcher.mock.calls[0][1];expect(init.headers.get('Authorization')).toBe('Bearer secret');expect(init.headers.get('Idempotency-Key')).toBe('stable');expect(fetcher.mock.calls[0][0]).not.toContain('secret')});
 it('surfaces server validation errors',async()=>{vi.stubGlobal('fetch',vi.fn().mockResolvedValue(new Response('{"detail":"Action mismatch"}',{status:409})));await expect(api('x','/approvals')).rejects.toThrow('Action mismatch')});
 it('handles empty success bodies',async()=>{vi.stubGlobal('fetch',vi.fn().mockResolvedValue(new Response(null,{status:204})));expect(await api('x','/cancel',{method:'POST'})).toBeUndefined()});
 it('classifies expired sessions',async()=>{vi.stubGlobal('fetch',vi.fn().mockResolvedValue(new Response(null,{status:401})));await expect(api('x','/me')).rejects.toBeInstanceOf(ApiFailure)});
});
