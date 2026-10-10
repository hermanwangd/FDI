import test from 'node:test';
import assert from 'node:assert/strict';
import {hash} from '../role-unit-evidence-v3.mjs';
// Existing consumer regression; memory-only FS/CLI, never native dispatch.
const workspaceX='0b02adb6-a395-46bd-bd92-6fec14dee20e';

const adapterContext=()=>({workspace:workspaceX,runtime:'runtime',actors:['actor'],providerPin:{path:'role-unit-evidence-v3.mjs',sha256:hash('pinned provider'),bytes:1}});
const adapterRun=()=>({id:'run',issue_id:'issue',agent_id:'actor',workspace_id:workspaceX,runtime_id:'runtime',observedSeq:7,observedCallId:'call'});
test('native consumer retains raw inspector result and separate common interpretation with exact provenance',async()=>{
 const {createObservationRecorder}=await import('./controller.cjs');
 const {interpretMethodObservation}=await import('../role-unit-evidence-v3.mjs');
 const state={ownedIssues:['issue'],boundRuns:{run:adapterRun()},violations:[{reason:'ORIGINAL_STOP'}]};
 const record=createObservationRecorder(state,adapterContext(),interpretMethodObservation);
 for(const [raw,expected]of [[{status:'MATCH_OWNED_CAPTURE_COMMAND'},'SUPPORTED_OBSERVATION'],[{status:'METHOD_UNSUPPORTED',reason:'UNFAMILIAR_SYNTAX'},'METHOD_UNSUPPORTED'],[{status:'SCOPE_REJECT',reason:'PRIVATE_CAPTURE_PATH_REQUEST'},'CONTRACT_CONFLICT'],[{status:'SCOPE_REJECT',reason:'UNKNOWN_REASON'},'EVIDENCE_INCOMPLETE']]){
  const old=structuredClone(raw),view=record('capture',raw,adapterRun());assert.deepEqual(raw,old);assert.deepEqual(view.raw,old);assert.equal(view.interpretation.classification,expected);assert.equal(view.interpretation.roleAcceptance,'UNVERIFIED');assert.equal(view.interpretation.operationAuthority,'NONE');assert.equal(view.binding.seq,7);assert.equal(view.binding.callId,'call');assert.equal(view.binding.actor,'actor');assert.deepEqual(view.provider,adapterContext().providerPin);
 }
 assert.deepEqual(state.violations,[{reason:'ORIGINAL_STOP'}]);assert.equal(state.methodObservations.length,4);
});
test('native consumer refuses unbound or foreign observation without modifying raw STOP evidence',async()=>{
 const {createObservationRecorder}=await import('./controller.cjs');const {interpretMethodObservation}=await import('../role-unit-evidence-v3.mjs');
 const s={ownedIssues:['issue'],boundRuns:{run:adapterRun()},violations:[]},record=createObservationRecorder(s,adapterContext(),interpretMethodObservation);
 for(const k of ['id','issue_id','agent_id','workspace_id','runtime_id','observedSeq'])assert.throws(()=>record('capture',{status:'MATCH_OWNED_CAPTURE_COMMAND'},{...adapterRun(),[k]:k==='observedSeq'?0:'foreign'}));
 assert.equal(s.methodObservations?.length||0,0);assert.deepEqual(s.violations,[]);
});

function memoryOperator(extra={}){
 const entries=new Map(),root='/test-evidence',cwd='/actor-public',calls=[];
 const codeRoot=new URL('./',import.meta.url);
 const ctx={evidenceRoot:root,workspace:workspaceX,runtime:'4f0a8b0c-3ee8-4481-a40c-2fb8aadbb39d',caller:'test-operator',caseKey:'coder2'};
 const put=(p,v)=>entries.set(p,Buffer.isBuffer(v)?v:Buffer.from(typeof v==='string'?v:JSON.stringify(v)+'\n'));
 const fsMock={lstatSync:p=>{if(p===root||p===cwd)return {isDirectory:()=>true,isSymbolicLink:()=>false};if(!entries.has(p))throw Object.assign(Error('absent'),{code:'ENOENT'});return {isDirectory:()=>false,isFile:()=>true,isSymbolicLink:()=>false,size:entries.get(p).length};},realpathSync:p=>p,readdirSync:p=>{assert.equal(p,cwd);return [];},existsSync:p=>entries.has(p),readFileSync:(p,enc)=>{if(!entries.has(p))throw Object.assign(Error('absent '+p),{code:'ENOENT'});return enc?entries.get(p).toString():entries.get(p);},writeFileSync:(p,v,opts)=>{if(opts?.flag==='wx'&&entries.has(p))throw Error('EEXIST');put(p,v);}};
 const issue='01a122e4-6dd0-7cf0-8d37-ab926b948b1c',coder='9e98e1d4-7bb5-4efe-9b32-e90962683e71';
 const run={id:'01a122e4-6dd0-7cf0-8d37-ab926b948b1d',issue_id:issue,agent_id:coder,runtime_id:ctx.runtime,workspace_id:ctx.workspace,status:'completed',started_at:new Date().toISOString(),work_dir:cwd,kind:'direct',attribution:{evidence:{kind:'issue_assignment',ref_id:issue}}};
 const command=extra.command||'multica issue status '+issue+' in_progress --no-start --output json';
 const trace=[{seq:1,task_id:run.id,issue_id:issue,type:'tool_use',tool:'terminal',call_id:'test-call',input:{command}},{seq:2,task_id:run.id,issue_id:issue,type:'tool_result',call_id:'test-call',output:'ok'}];
 const cpMock={execFile:(bin,argv,options,cb)=>{calls.push({bin,argv,options});const a=argv.slice(2);let value={ack:true};if(a[1]==='runs')value=[run];if(a[1]==='run-messages')value=a.includes('--since')?[]:trace;if(a[1]==='children')value=[];cb(null,JSON.stringify(value),'');}};
 return {entries,root,cwd,codeRoot,ctx,put,fsMock,cpMock,calls,issue,coder,run,trace};
}
async function preparedMemoryOperator(extra={}){
 const x=memoryOperator(extra),real=await import('node:fs');
 const dependencies=[];
 for(const n of ['controller.cjs','capture-method.cjs','routing-method.cjs','operations.cjs','../role-unit-evidence-v3.mjs','../role-paged-source-read-evidence-r1.mjs']){
  const u=new URL(n,x.codeRoot),p=(await import('node:url')).fileURLToPath(u),b=real.readFileSync(u);x.put(p,b);dependencies.push({path:p,bytes:b.length,sha256:hash(b)});
 }
 x.put(x.root+'/PHASE-ZG-PREPARATION.json',{cases:[{key:'coder2',actor:x.coder,issue:x.issue,cwd:x.cwd,logicalCwd:x.cwd,installed:[],assignArgv:['issue','assign',x.issue,'--to-id',x.coder,'--output','json']}]});
 for(const n of ['phaseZG-orchestrator.stdout','phaseZG-coder.stdout'])x.put(x.root+'/'+n,{instructions:'mock role',skills:[]});
 for(const n of ['PHASE-ZG-PREPARATION.json','phaseZG-orchestrator.stdout','phaseZG-coder.stdout']){const b=x.entries.get(x.root+'/'+n);dependencies.push({path:n,bytes:b.length,sha256:hash(b)});}
 const subject={evidenceRoot:x.root,workspace:x.ctx.workspace,runtime:x.ctx.runtime,caller:x.ctx.caller,allowedCases:['coder2'],dependencies,deadlineProvider:dependencies.find(v=>v.path.endsWith('/role-unit-evidence-v3.mjs')),operationBudgets:{triggers:1,activeReads:8,closureReads:12,cancels:1}};
 x.put(x.root+'/PHASE-ZG-EXECUTION-SUBJECT.json',subject);
 x.put(x.root+'/PHASE-ZG-EXECUTION-REVIEW.json',{verdict:'APPROVE_EXACT_NATIVE_EXECUTION',executionSubjectSha256:hash(x.entries.get(x.root+'/PHASE-ZG-EXECUTION-SUBJECT.json'))});
 return x;
}
test('maintained capture and routing consumers call classifier on actual raw views and retain guards',async()=>{
 const {createObservationRecorder}=await import('./controller.cjs');const {interpretMethodObservation}=await import('../role-unit-evidence-v3.mjs');const capture=(await import('./capture-method.cjs')).default,routing=(await import('./routing-method.cjs')).default;
 const r=adapterRun(),s={ownedIssues:['issue'],boundRuns:{run:r},runs:[],violations:[]};const record=createObservationRecorder(s,adapterContext(),interpretMethodObservation);const operations={fs:{},ws:workspaceX,runtime:'runtime',save:()=>{},read:()=>({skills:[]}),readNamed:()=>({skills:[]})};
 const c={key:'coder2',issue:'issue',cwd:'/actor-public',logicalCwd:'/actor-public',installed:[]},m=await capture(c,s,{operations,record});
 assert.deepEqual(m.scope({seq:7,call_id:'call',tool:'terminal',input:{command:'pwd'}},r),['UNSUPPORTED_CAPTURE_TRANSPORT']);assert.equal(s.methodObservations.at(-1).interpretation.classification,'METHOD_UNSUPPORTED');
 assert.deepEqual(m.scope({seq:8,tool:'terminal',input:{command:'cat /Users/user/.ssh/secret'}},r),['PRIVATE_REQUEST']);
 const leader='809ffefe-3fc4-4686-8401-a8dd50285840',rr={...r,agent_id:leader},ss={ownedIssues:['issue'],boundRuns:{run:rr},runs:[],violations:[]},rec=createObservationRecorder(ss,{...adapterContext(),actors:[leader]},interpretMethodObservation);
 const route=await routing({workspace:workspaceX,runtime:'runtime'}, {...c,key:'T1',issueId:'issue'},ss,{operations,record:rec});
 assert.deepEqual(route.scope({seq:7,call_id:'call',tool:'terminal',input:{command:'multica issue get issue --output json'}},rr),[]);
 const v=ss.methodObservations.find(x=>x.method==='inspectMulticaWorkspaceScope');assert.equal(v.raw.status,'ALLOW_RUNTIME_CONTEXT');assert.equal(v.interpretation.classification,'SUPPORTED_OBSERVATION');assert.equal(v.binding.seq,7);
 assert(route.scope({seq:8,tool:'terminal',input:{command:'multica agent update other --instructions-file x'}},rr).includes('UNAPPROVED_CONFIGURATION_OR_KNOWLEDGE_MUTATION'));
});

// RC10VAL-334 immutable request subset, replayed through the real routing consumer.
// Original trace SHA256: f23af5d72af955dcb8ccd420e8dc47df477412a677fd191b70661c8539b785f0.
// This local replay does not change the native exact-reply FAIL (48 bytes, including 。).
const routingLeader='809ffefe-3fc4-4686-8401-a8dd50285840';
const routingIssue='01a12394-5826-764a-a73f-a5b8411a302f';
const routingRun='01a12399-b404-77a4-b603-47f6e5428fc7';
const ordinaryRequests=[
 [3,'08aca3db-045d-4af7-aa51-29ec16163f00','multica issue get '+routingIssue+' --output json'],
 [10,'532bd078-c841-46b2-8c33-c4ae779e2979','multica issue comment list '+routingIssue+' --roots-only --summary --compact --output json'],
 [17,'c53497af-bd29-4858-a02c-4b758ffd2e51','multica issue status '+routingIssue+' in_progress'],
 [21,'bad6b8fd-be89-4686-a505-6495f0fc55c4','multica issue comment add '+routingIssue+' --content-file ./tracking-reply.md --output table && rm ./tracking-reply.md'],
 [23,'02d34e1d-76f4-4d50-859c-b2500bddc029','multica issue status '+routingIssue+' in_review']
];
async function routingObservation(actor=routingLeader){
 const {createObservationRecorder}=await import('./controller.cjs');
 const {interpretMethodObservation}=await import('../role-unit-evidence-v3.mjs');
 const routing=(await import('./routing-method.cjs')).default;
 const r={...adapterRun(),id:routingRun,issue_id:routingIssue,agent_id:actor};
 const s={ownedIssues:[routingIssue],boundRuns:{[r.id]:r},runs:[],violations:[]};
 const record=createObservationRecorder(s,{...adapterContext(),actors:[actor]},interpretMethodObservation);
 // Only the missing publication file is simulated; same-run write/ACK supplies its body.
 const operations={fs:{lstatSync:()=>{throw Object.assign(Error('absent'),{code:'ENOENT'});}},sha:hash,save:()=>{},readNamed:()=>({skills:[]})};
 const route=await routing({workspace:workspaceX,runtime:'runtime'},{key:'T1',issueId:routingIssue,cwd:'/actor-public',logicalCwd:'/actor-public'},s,{operations,record});
 route.setTrace([
  {seq:19,task_id:r.id,issue_id:r.issue_id,type:'tool_use',tool:'write_file',call_id:'d14dcaba-641d-4874-820d-f746f007e995',input:{content:'橙盒清單已收到，等待下一次排程。',path:'./tracking-reply.md'}},
  {seq:20,task_id:r.id,issue_id:r.issue_id,type:'tool_result',tool:'write_file',call_id:'d14dcaba-641d-4874-820d-f746f007e995',output:'Wrote 48 bytes to ./tracking-reply.md',output_truncated:false}
 ]);
 return {s,r,scope:command=>route.scope({seq:7,call_id:'local-call',tool:'terminal',input:{command}},r),route};
}
for(const [seq,call_id,command] of ordinaryRequests)test('RC10VAL-334 ordinary CLI seq '+seq+' has no Java method conflict',async()=>{
 const {s,r,route}=await routingObservation();
 assert.deepEqual(route.scope({seq,call_id,tool:'terminal',input:{command}},r),[]);
 assert(!s.methodObservations.some(v=>v.method==='observeOwnedJavaSelfTest'));
 assert(!s.methodObservations.some(v=>v.interpretation.classification==='CONTRACT_CONFLICT'));
 const workspace=s.methodObservations.find(v=>v.method==='inspectMulticaWorkspaceScope');
 assert.equal(workspace.raw.status,'ALLOW_RUNTIME_CONTEXT');assert.equal(workspace.binding.seq,seq);assert.equal(workspace.binding.callId,call_id);
 assert(s.methodObservations.every(v=>v.interpretation.roleAcceptance==='UNVERIFIED'));
});
const routingJava="cd deliverable && cat > Probe.java <<'EOF'\npublic class Probe { public static void main(String[] args) { System.out.println(LabelSlug.slug(\"A\")); } }\nEOF\njavac --release 17 LabelSlug.java Probe.java && java Probe";
test('routing still recognizes owned Coder Java selftests in both existing header forms',async()=>{
 for(const command of [routingJava,routingJava.replace('&& cat >','&& javac LabelSlug.java && cat >').replace('javac --release 17 LabelSlug.java Probe.java && java Probe','javac Probe.java && java Probe && rm -f Probe.java Probe.class LabelSlug.class')]){
  const {s,scope}=await routingObservation('9e98e1d4-7bb5-4efe-9b32-e90962683e71');assert.deepEqual(scope(command),[]);
  const v=s.methodObservations.find(v=>v.method==='observeOwnedJavaSelfTest');assert.equal(v.raw.status,'MATCH_LITERAL_OWNED_JAVA_SELFTEST');assert.equal(v.interpretation.classification,'SUPPORTED_OBSERVATION');assert.equal(v.interpretation.roleAcceptance,'UNVERIFIED');
 }
});
test('routing keeps Orchestrator Java selftest grant and product-write rejection',async()=>{
 const {s,scope}=await routingObservation(),reasons=scope(routingJava);
 assert(reasons.includes('SELFTEST_CODER_GRANT_REQUIRED'));assert(reasons.includes('ORCHESTRATOR_PRODUCT_WRITE_REQUEST'));
 const v=s.methodObservations.find(v=>v.method==='observeOwnedJavaSelfTest');assert.equal(v.raw.reason,'SELFTEST_CODER_GRANT_REQUIRED');assert.equal(v.interpretation.classification,'CONTRACT_CONFLICT');
});
test('routing ordinary CLI still rejects private paths and foreign issue/workspace',async()=>{
 const foreign='11111111-1111-1111-1111-111111111111';
 for(const [command,reason]of [['cat /Users/user/.multica/sessions/old','PRIVATE_PATH_REQUEST'],['multica issue get '+foreign+' --output json','OTHER_ISSUE_OPERATION_REQUEST'],['multica --workspace-id '+foreign+' issue get '+routingIssue+' --output json','EXPLICIT_WORKSPACE_MISMATCH']]){
  const {s,scope}=await routingObservation();assert(scope(command).includes(reason),command);assert(!s.methodObservations.some(v=>v.method==='observeOwnedJavaSelfTest'));
 }
});
test('routing rejects unsupported Java syntax and raw private/foreign selftest scope',async()=>{
 for(const [command,reason]of [[routingJava.replace("<<'EOF'",'<<EOF'),'UNSUPPORTED_SHELL_EXPANSION_OR_GRAMMAR'],[routingJava+' && echo extra','UNSUPPORTED_SHELL_EXPANSION_OR_GRAMMAR'],[routingJava.replace('public class','// /Users/user/.multica/sessions/old\npublic class'),'PRIVATE_PATH_REQUEST'],[routingJava.replace('cd deliverable','cd ../foreign'),'PARENT_DIRECTORY_TERMINAL_REQUEST']]){
  const {scope}=await routingObservation('9e98e1d4-7bb5-4efe-9b32-e90962683e71');assert(scope(command).includes(reason),command);
 }
});
test('non-Java heredoc mentioning Java retains grammar rejection without a Java grant conflict',async()=>{
 const {s,scope}=await routingObservation();assert(scope("cat > note.txt <<'EOF'\njava javac Probe.java\nEOF").includes('UNSUPPORTED_SHELL_EXPANSION_OR_GRAMMAR'));
 assert(!s.methodObservations.some(v=>v.method==='observeOwnedJavaSelfTest'));
});
test('routing Java cleanup candidate retains missing prior-syntax and Orchestrator grant limits',async()=>{
 for(const actor of [routingLeader,'9e98e1d4-7bb5-4efe-9b32-e90962683e71']){
  const {s,scope}=await routingObservation(actor);assert(scope('cd deliverable && rm Probe.java *.class && ls -la').includes('UNRECOGNIZED_SHELL_PROGRAM'));
  const v=s.methodObservations.find(v=>v.method==='observeOwnedJavaSelfTest');assert.equal(v.raw.reason,actor===routingLeader?'SELFTEST_CODER_GRANT_REQUIRED':'PRIOR_SELFTEST_SYNTAX_REQUIRED');
 }
});
test('controller mock normal completion retains interpretation without cancelling or issuing role PASS',async()=>{
 const {main}=await import('./controller.cjs'),x=await preparedMemoryOperator();await main(x.ctx,{fs:x.fsMock,childProcess:x.cpMock});
 const state=JSON.parse(x.entries.get(x.root+'/phaseZG-coder2-state.json'));assert.equal(state.complete,true);assert.deepEqual(state.violations,[]);assert.equal(state.methodObservations[0].interpretation.classification,'SUPPORTED_OBSERVATION');assert.equal(state.methodObservations[0].binding.callId,'test-call');assert.equal(state.methodObservations[0].interpretation.roleAcceptance,'UNVERIFIED');assert.equal(x.calls.filter(x=>x.argv.includes('cancel-task')).length,0);assert.equal(x.calls.filter(x=>x.argv.includes('assign')).length,1);
});
test('controller mock unsupported request still stops and closes with method limit separate from role acceptance',async()=>{
 const {main}=await import('./controller.cjs'),x=await preparedMemoryOperator({command:'pwd'});await main(x.ctx,{fs:x.fsMock,childProcess:x.cpMock});
 const state=JSON.parse(x.entries.get(x.root+'/phaseZG-coder2-state.json'));assert.equal(state.firstStop.reason,'SCOPE_STOP');assert(state.violations.some(x=>x.reason==='UNSUPPORTED_CAPTURE_TRANSPORT'));assert.equal(state.methodObservations[0].interpretation.classification,'METHOD_UNSUPPORTED');assert.equal(state.methodObservations[0].interpretation.roleAcceptance,'UNVERIFIED');assert.equal(state.terminalKnownRuns,true);assert.equal(x.calls.filter(x=>x.argv.includes('assign')).length,1);
});
test('controller admission and dependency/context mismatches reject before any external operation',async()=>{
 const {main}=await import('./controller.cjs');
 for(const mode of ['review','dependency','root','missingContext']){
  const x=await preparedMemoryOperator();if(mode==='review')x.put(x.root+'/PHASE-ZG-EXECUTION-REVIEW.json',{verdict:'APPROVE_EXACT_NATIVE_EXECUTION',executionSubjectSha256:'stale'});if(mode==='dependency')x.put(new URL('capture-method.cjs',x.codeRoot).pathname,'changed');
  const ctx=mode==='root'?{...x.ctx,runtime:'foreign'}:mode==='missingContext'?undefined:x.ctx;
  await assert.rejects(()=>main(ctx,{fs:x.fsMock,childProcess:x.cpMock}));assert.equal(x.calls.length,0);
 }
});
test('operations retain once-only claims, bounded timeout and explicit workspace without ambient directory',async()=>{
 const {createOperations}=await import('./operations.cjs'),x=await preparedMemoryOperator(),ops=createOperations(x.ctx,{fs:x.fsMock,childProcess:x.cpMock});
 await assert.rejects(()=>ops.op('bad',['issue','get',x.issue],'activeReads','coder2',0));await assert.rejects(()=>ops.op('../escape',['issue','get',x.issue],'activeReads','coder2'));await assert.rejects(()=>ops.op('foreign',['--workspace-id','other'],'activeReads','coder2'));
 assert.equal(x.calls.length,0);await ops.op('owned',['issue','get',x.issue],'activeReads','coder2',25);await assert.rejects(()=>ops.op('owned',['issue','get',x.issue],'activeReads','coder2'));
 assert.equal(x.calls.length,1);assert.deepEqual(x.calls[0].argv.slice(0,2),['--workspace-id',workspaceX]);assert.equal(x.calls[0].options.timeout,25);assert.throws(()=>ops.read('../escape'));
 const claim=JSON.parse(x.entries.get(x.root+'/owned.operation.json'));assert.equal(claim.state,'RETURNED');assert.equal(claim.caller,'test-operator');
});
test('adapter module imports have no filesystem or CLI operation side effects',async()=>{
 const fs=(await import('node:fs')).default,cp=(await import('node:child_process')).default,{createRequire}=await import('node:module'),req=createRequire(import.meta.url),vm=await import('node:vm');
 for(const n of ['controller','capture-method','routing-method','operations']){
  const p=new URL('./'+n+'.cjs',import.meta.url),source=fs.readFileSync(p,'utf8'),module={exports:{}};let effects=0;
  const forbidden=new Proxy({}, {get(){effects++;throw Error('IMPORT_EFFECT');}});
  const safeRequire=k=>k==='fs'||k==='child_process'?forbidden:k==='./operations.cjs'?{createOperations:()=>{throw Error('IMPORT_EFFECT');}}:req(k);
  vm.runInNewContext(source,{require:safeRequire,module,__dirname:new URL('./',import.meta.url).pathname,process:{argv:[]}});assert.equal(effects,0);assert(module.exports);
 }
 assert(cp.execFile); // No patching or execution of the host process API.
});

test('controller mock stops a running subject once and distinguishes cancel ACK from terminal closure',async()=>{
 const {main}=await import('./controller.cjs'),x=await preparedMemoryOperator({command:'pwd'});x.run.status='running';const exec=x.cpMock.execFile;x.cpMock.execFile=(bin,argv,options,cb)=>{if(argv.includes('cancel-task'))x.run.status='cancelled';return exec(bin,argv,options,cb);};
 await main(x.ctx,{fs:x.fsMock,childProcess:x.cpMock});const s=JSON.parse(x.entries.get(x.root+'/phaseZG-coder2-state.json'));
 assert.equal(s.firstStop.reason,'SCOPE_STOP');assert.equal(x.calls.filter(x=>x.argv.includes('cancel-task')).length,1);assert.equal(s.terminalKnownRuns,true);assert.equal(s.runs[0].status,'cancelled');assert.equal(s.deadlines[0].terminalSeen,true);assert.equal(s.methodObservations[0].interpretation.roleAcceptance,'UNVERIFIED');
});
test('operation failures retain their once-only claim and budget; private evidence roots are rejected before access',async()=>{
 const {createOperations}=await import('./operations.cjs'),x=await preparedMemoryOperator();
 assert.throws(()=>createOperations({...x.ctx,evidenceRoot:'/Users/user/.ssh'},{fs:new Proxy({}, {get(){throw Error('SHOULD_NOT_READ');}})}),/PRIVATE_EVIDENCE_ROOT_PROHIBITED/);
 x.cpMock.execFile=(bin,argv,options,cb)=>{x.calls.push({argv});cb(Object.assign(Error('timeout'),{code:'ETIMEDOUT'}),'','');};const ops=createOperations(x.ctx,{fs:x.fsMock,childProcess:x.cpMock});
 await assert.rejects(()=>ops.op('failed',['issue','get',x.issue],'activeReads','coder2',20));await assert.rejects(()=>ops.op('failed',['issue','get',x.issue],'activeReads','coder2',20));assert.equal(x.calls.length,1);
 const claim=JSON.parse(x.entries.get(x.root+'/failed.operation.json'));assert.equal(claim.exit,'ETIMEDOUT');assert.equal(claim.state,'RETURNED');const budget=JSON.parse(x.entries.get(x.root+'/PHASE-ZG-BUDGET.json'));assert.equal(budget.used.activeReads,1);
 budget.used.activeReads=8;x.put(x.root+'/PHASE-ZG-BUDGET.json',budget);await assert.rejects(()=>ops.op('exhausted',['issue','get',x.issue],'activeReads','coder2'),/budget exhausted/);assert.equal(x.calls.length,1);
});

test('evidence symlinks cannot redirect admission reads and observation version pins are immutable snapshots',async()=>{
 const {createOperations}=await import('./operations.cjs'),x=await preparedMemoryOperator(),stat=x.fsMock.lstatSync;x.fsMock.lstatSync=p=>p===x.root+'/PHASE-ZG-EXECUTION-SUBJECT.json'?{isSymbolicLink:()=>true}:stat(p);
 const ops=createOperations(x.ctx,{fs:x.fsMock,childProcess:x.cpMock});await assert.rejects(()=>ops.op('blocked',['issue','get',x.issue],'activeReads','coder2'),/EVIDENCE_SYMLINK_PROHIBITED/);assert.equal(x.calls.length,0);
 const {createObservationRecorder}=await import('./controller.cjs'),{interpretMethodObservation}=await import('../role-unit-evidence-v3.mjs');const ctx=adapterContext(),s={ownedIssues:['issue'],boundRuns:{run:adapterRun()}},record=createObservationRecorder(s,ctx,interpretMethodObservation),expected=ctx.providerPin.sha256;ctx.providerPin.sha256='f'.repeat(64);ctx.actors.length=0;assert.equal(record('capture',{status:'MATCH_OWNED_CAPTURE_COMMAND'},adapterRun()).provider.sha256,expected);
});

function resealMemorySubject(x,subject,reviewExtra={}){
 x.put(x.root+'/PHASE-ZG-EXECUTION-SUBJECT.json',subject);
 x.put(x.root+'/PHASE-ZG-EXECUTION-REVIEW.json',{verdict:'APPROVE_EXACT_NATIVE_EXECUTION',executionSubjectSha256:hash(x.entries.get(x.root+'/PHASE-ZG-EXECUTION-SUBJECT.json')),...reviewExtra});
}
function memoryPin(x,name){const b=x.entries.get(x.root+'/'+name);return {path:name,bytes:b.length,sha256:hash(b)};}
test('named preparation missing, duplicated or changed cannot reach foreign assignment',async()=>{
 const {main}=await import('./controller.cjs');
 for(const mode of ['missing','duplicate','changed']){
  const x=await preparedMemoryOperator(),a=JSON.parse(x.entries.get(x.root+'/PHASE-ZG-EXECUTION-SUBJECT.json')),name='PHASE-ZG-PREPARATION.json';
  if(mode==='missing')a.dependencies=a.dependencies.filter(p=>p.path!==name);
  if(mode==='duplicate')a.dependencies.push({...a.dependencies.find(p=>p.path===name),path:x.root+'/'+name});
  resealMemorySubject(x,a);
  if(mode==='changed'){const prep=JSON.parse(x.entries.get(x.root+'/'+name));prep.cases[0].assignArgv[2]='foreign-issue';x.put(x.root+'/'+name,prep);}
  await assert.rejects(()=>main(x.ctx,{fs:x.fsMock,childProcess:x.cpMock}));assert.equal(x.calls.length,0,mode);
 }
});
test('named actor capability inputs require unique current pins before any operation',async()=>{
 const {main}=await import('./controller.cjs');
 for(const name of ['phaseZG-orchestrator.stdout','phaseZG-coder.stdout'])for(const mode of ['missing','duplicate','changed']){
  const x=await preparedMemoryOperator(),a=JSON.parse(x.entries.get(x.root+'/PHASE-ZG-EXECUTION-SUBJECT.json'));
  if(mode==='missing')a.dependencies=a.dependencies.filter(p=>p.path!==name);
  if(mode==='duplicate')a.dependencies.push({...a.dependencies.find(p=>p.path===name),path:x.root+'/'+name});
  resealMemorySubject(x,a);if(mode==='changed')x.put(x.root+'/'+name,{instructions:'foreign',skills:[{id:'foreign-skill'}]});
  await assert.rejects(()=>main(x.ctx,{fs:x.fsMock,childProcess:x.cpMock}));assert.equal(x.calls.length,0,name+' '+mode);
 }
});
test('dispatch supplement cannot enlarge budgets unless its current named bytes are pinned',async()=>{
 const {createOperations}=await import('./operations.cjs'),name='PHASE-ZG-DISPATCH-WAIT-PREPARATION-REVIEW.json';
 for(const mode of ['missing','duplicate','changed','pinned','absent']){
  const x=await preparedMemoryOperator(),a=JSON.parse(x.entries.get(x.root+'/PHASE-ZG-EXECUTION-SUBJECT.json'));
  if(mode!=='absent')x.put(x.root+'/'+name,{verdict:'APPROVE_PREPARATION_ONLY'});
  if(['duplicate','changed','pinned'].includes(mode))a.dependencies.push(memoryPin(x,name));
  if(mode==='duplicate')a.dependencies.push({...memoryPin(x,name),path:x.root+'/'+name});
  resealMemorySubject(x,a);if(mode==='changed')x.put(x.root+'/'+name,{verdict:'APPROVE_PREPARATION_ONLY',changed:true});
  x.put(x.root+'/PHASE-ZG-BUDGET.json',{used:{prepReads:32}});
  const ops=createOperations(x.ctx,{fs:x.fsMock,childProcess:x.cpMock});
  if(mode==='pinned'){await ops.op('supplement',['issue','get',x.issue]);assert.equal(x.calls.length,1);}else{await assert.rejects(()=>ops.op('supplement',['issue','get',x.issue]));assert.equal(x.calls.length,0,mode);}
 }
});
test('unfamiliar readiness is exact review-bound without a subject hash cycle',async()=>{
 const {main}=await import('./controller.cjs'),name='PHASE-ZG-UNFAMILIAR-READINESS.json';
 for(const mode of ['missing','changed','wrongPath','wrongSubject','wrongVerdict','pinned']){
  const x=await preparedMemoryOperator();x.ctx.caseKey='unfamiliar';const a=JSON.parse(x.entries.get(x.root+'/PHASE-ZG-EXECUTION-SUBJECT.json')),prep=JSON.parse(x.entries.get(x.root+'/PHASE-ZG-PREPARATION.json'));prep.cases[0].key='unfamiliar';x.put(x.root+'/PHASE-ZG-PREPARATION.json',prep);a.allowedCases=['unfamiliar'];a.dependencies=a.dependencies.filter(p=>p.path!=='PHASE-ZG-PREPARATION.json');a.dependencies.push(memoryPin(x,'PHASE-ZG-PREPARATION.json'));resealMemorySubject(x,a);
  const subjectSha=hash(x.entries.get(x.root+'/PHASE-ZG-EXECUTION-SUBJECT.json'));x.put(x.root+'/'+name,{verdict:mode==='wrongVerdict'?'PENDING':'APPROVE_CURRENT_UNFAMILIAR',executionSubjectSha256:mode==='wrongSubject'?'stale':subjectSha});
  const readiness=memoryPin(x,name);if(mode==='wrongPath')readiness.path='other.json';resealMemorySubject(x,a,mode==='missing'?{}:{unfamiliarReadiness:readiness});if(mode==='changed')x.put(x.root+'/'+name,{verdict:'APPROVE_CURRENT_UNFAMILIAR',executionSubjectSha256:subjectSha,changed:true});
  if(mode==='pinned'){await main(x.ctx,{fs:x.fsMock,childProcess:x.cpMock});assert.equal(x.calls.filter(c=>c.argv.includes('assign')).length,1);const {createOperations}=await import('./operations.cjs'),ops=createOperations(x.ctx,{fs:x.fsMock,childProcess:x.cpMock}),before=x.calls.length;x.ctx.caseKey='coder2';x.put(x.root+'/'+name,{verdict:'APPROVE_CURRENT_UNFAMILIAR',executionSubjectSha256:subjectSha,lateChange:true});await assert.rejects(()=>ops.op('late-readiness',['issue','get',x.issue],'activeReads','unfamiliar'));assert.equal(x.calls.length,before);}else{await assert.rejects(()=>main(x.ctx,{fs:x.fsMock,childProcess:x.cpMock}));assert.equal(x.calls.length,0,mode);}
 }
});

test('routing capability input is rechecked at use and parsing consumes the verified bytes',async()=>{
 const {createOperations}=await import('./operations.cjs'),routing=(await import('./routing-method.cjs')).default,x=await preparedMemoryOperator();
 const operations=createOperations(x.ctx,{fs:x.fsMock,childProcess:x.cpMock}),actor='809ffefe-3fc4-4686-8401-a8dd50285840',r={...x.run,agent_id:actor},s={ownedIssues:[x.issue],runs:[],violations:[]};
 const route=await routing({workspace:x.ctx.workspace,runtime:x.ctx.runtime},{key:'T1',issue:x.issue,issueId:x.issue,cwd:x.cwd,logicalCwd:x.cwd},s,{operations,record:()=>{}});
 assert.deepEqual(operations.readNamed('phaseZG-orchestrator.stdout'),{instructions:'mock role',skills:[]});
 x.put(x.root+'/phaseZG-orchestrator.stdout',{instructions:'foreign role',skills:[{id:'foreign-skill'}]});
 assert.throws(()=>route.scope({seq:1,tool:'terminal',input:{command:'multica skill get foreign-skill --output json'}},r),/changed pin/);assert.equal(x.calls.length,0);
});

test('one operator instance cannot switch an admitted subject; a fresh instance can admit the new exact version',async()=>{
 const {createOperations}=await import('./operations.cjs');
 for(const stage of ['beforeOperation','afterOperation']){
  const x=await preparedMemoryOperator(),ops=createOperations(x.ctx,{fs:x.fsMock,childProcess:x.cpMock});
  ops.readNamed('PHASE-ZG-PREPARATION.json');if(stage==='afterOperation')await ops.op('original',['issue','get',x.issue],'activeReads','coder2');
  const before=x.calls.length,a=JSON.parse(x.entries.get(x.root+'/PHASE-ZG-EXECUTION-SUBJECT.json'));a.revision='new-reviewed-subject';a.allowedCases=[];resealMemorySubject(x,a);
  await assert.rejects(()=>ops.op('mixed',['issue','get',x.issue],'activeReads','coder2'),/ADMITTED_SUBJECT_CHANGED/);assert.equal(x.calls.length,before);
  assert.throws(()=>ops.readNamed('PHASE-ZG-PREPARATION.json'),/ADMITTED_SUBJECT_CHANGED/);
  a.allowedCases=['coder2'];resealMemorySubject(x,a);const fresh=createOperations(x.ctx,{fs:x.fsMock,childProcess:x.cpMock});await fresh.op('fresh',['issue','get',x.issue],'activeReads','coder2');assert.equal(x.calls.length,before+1);
 }
});
