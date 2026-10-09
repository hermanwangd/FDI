import test from 'node:test';
import assert from 'node:assert/strict';
import {hash,checkUnitEvidence} from './role-unit-evidence-v3.mjs';
const skills=[{id:'s1',name:'skill',enabled:true,content:'skill body'}];
const pin={actor:'actor',model:'model',runtime_id:'runtime',permission_mode:'private',instructions:{sha256:hash('body')},skills:skills.map(x=>({...x,sha256:hash(x.content)}))};
const suite={sourcePins:{qa:pin},prelude:'unit fixture',cases:[{role:'qa',id:'Q1',input:'fixture'}]};
const prompt=suite.prelude+'\n\n'+JSON.stringify({role:'qa',packetRevision:1,items:[{id:'Q1',input:'fixture'}]},null,2);
const fresh=()=>({role:'qa',issueId:'issue',triggerId:'trigger',dispatchedPrompt:prompt,launchAgent:{id:'actor',workspace_id:'0b02adb6-a395-46bd-bd92-6fec14dee20e',runtime_id:'runtime',instructions:'body',model:'model',permission_mode:'private',skills},launchSkills:skills,subjectRun:{id:'run',agent_id:'actor',runtime_id:'runtime',workspace_id:'0b02adb6-a395-46bd-bd92-6fec14dee20e',issue_id:'issue',trigger_comment_id:'trigger',started_at:'2026-10-08T00:00:00Z',status:'completed',error:null,result:{output:'actual body'}},trace:[{seq:1,type:'text',task_id:'run',issue_id:'issue',content:'actual body'}],tailProbe:{since:1,messages:[]}});
test('receipt binding can pass mechanically without claiming semantic/load/component PASS',()=>{const r=checkUnitEvidence(fresh(),suite);assert.equal(r.status,'PASS');assert.equal(r.renderLoad,'UNVERIFIED');assert.equal(r.componentEffects,'NOT_EXERCISED');assert.equal(r.semanticAcceptance,'PENDING_INDEPENDENT_SCORE');});
test('wrong actor/runtime/scope/trigger cannot pass from completed flag',()=>{for(const key of ['agent_id','runtime_id','workspace_id','trigger_comment_id']){const e=fresh();e.subjectRun[key]='other';assert.equal(checkUnitEvidence(e,suite).status,'FAIL');}});
test('changed source/Skill/prompt and missing evidence are rejected',()=>{for(const mutate of [e=>e.launchAgent.instructions+='x',e=>e.launchSkills=[{...skills[0],content:'old'}],e=>e.dispatchedPrompt='rubric leak',e=>e.subjectRun.started_at=null,e=>e.trace=[],e=>e.subjectRun.result={}]){const e=fresh();mutate(e);assert.equal(checkUnitEvidence(e,suite).status,'FAIL');}});
test('real actor tool invocation/trace gap is retained as failed boundary',()=>{for(const mutate of [e=>e.trace.push({seq:2,type:'tool_call',task_id:'run',issue_id:'issue',content:{tool:'Read',input:{path:'history'}}}),e=>e.trace[0].seq=5,e=>e.trace[0].task_id='other']){const e=fresh();mutate(e);assert.equal(checkUnitEvidence(e,suite).status,'FAIL');}});
test('spoofed launch binding, missing tail, wrong assembled text and host diagnostics for wrong actor rejected',()=>{for(const mutate of [e=>e.launchAgent.id='other',e=>e.tailProbe={since:0,messages:[]},e=>e.trace[0].content='truncated',e=>e.trace.push({seq:2,type:'tool_use',tool:'sdk-host_boundary_diagnostics',input:{producer:'SDK-host',boundary:'shared-readonly-preflight'},task_id:'run',issue_id:'issue'})]){const e=fresh();mutate(e);assert.equal(checkUnitEvidence(e,suite).status,'FAIL');}});
test('interim separated by non-text does not corrupt a complete chunked final',()=>{
  const e=fresh();e.trace=[{seq:1,type:'text',task_id:'run',issue_id:'issue',content:'progress'}, {seq:2,type:'thinking',task_id:'run',issue_id:'issue',content:'reasoning'}, {seq:3,type:'text',task_id:'run',issue_id:'issue',content:'actual '}, {seq:4,type:'text',task_id:'run',issue_id:'issue',content:'body'}];e.tailProbe.since=4;
  const r=checkUnitEvidence(e,suite);assert.equal(r.status,'PASS');assert.deepEqual(r.finalExtraction.seqs,[3,4]);assert.equal(r.finalExtraction.interimText[0].seq,1);
});
test('missing chunk, matching interim only, extra final bytes and wrong terminal segment fail',()=>{
  for(const trace of [
    [{type:'text',content:'actual '}],
    [{type:'text',content:'actual body'},{type:'thinking',content:'intermediate'},{type:'text',content:'other final'}],
    [{type:'text',content:'actual bodyextra'}],
    [{type:'text',content:'actual body'},{type:'tool_result',output:'late operation'}]
  ]){const e=fresh();e.trace=trace.map((m,i)=>({...m,seq:i+1,task_id:'run',issue_id:'issue'}));e.tailProbe.since=e.trace.length;assert.equal(checkUnitEvidence(e,suite).status,'FAIL');}
});

const facts={profileName:'rc10-reviewer-r16-readonly',activeToolNames:['Read','Glob','Grep'],disallowedTools:['select_tools'],subagents:[],effectiveManifest:[{name:'Glob',source:'builtin'},{name:'Grep',source:'builtin'},{name:'Read',source:'builtin'}],model:'model'};
const profilePin={rawSha256:'raw',expectedFingerprint:'independent-fp',expectedPolicyHash:'independent-policy',expectedFacts:facts};
const reviewerSuite={...suite,sourcePins:{reviewer:pin},cases:[{role:'reviewer',id:'Q1',input:'fixture'}]};
const reviewer=()=>{const e=fresh();e.role='reviewer';e.dispatchedPrompt=reviewerSuite.prelude+'\n\n'+JSON.stringify({role:'reviewer',packetRevision:1,items:[{id:'Q1',input:'fixture'}]},null,2);e.profile=e.profileAfter={sha256:'raw'};const names=['Bash','Write','Edit','Agent','AgentSwarm','select_tools','mcp__r16_guard__publish','mcp__r16_guard__delete','mcp__r16_guard__delegate'];const v={...facts,producer:'SDK-host',profileFingerprint:'independent-fp',effectivePolicyHash:'independent-policy',policyHashAfter:'independent-policy',status:'verified',probes:names.map(name=>({name,guardInstalled:true,decision:'policy-denied',fixture:name.startsWith('mcp__')}))};e.trace=[{seq:1,type:'tool_use',tool:'sdk-host_boundary_diagnostics',call_id:'host',input:{producer:'SDK-host',boundary:'shared-readonly-preflight'},task_id:'run',issue_id:'issue'},{seq:2,type:'tool_result',call_id:'host',output:JSON.stringify(v),task_id:'run',issue_id:'issue'},{seq:3,type:'text',content:'actual body',task_id:'run',issue_id:'issue'}];e.tailProbe.since=3;return e;};
test('after source pin must be independently supplied and same-run facts must match',()=>{assert.equal(checkUnitEvidence(reviewer(),reviewerSuite,profilePin).status,'PASS');assert.throws(()=>checkUnitEvidence(reviewer(),reviewerSuite),/explicit independently/);for(const mutate of [v=>v.profileFingerprint='wrong',v=>v.effectiveManifest=[{name:'Bash',source:'builtin'}],v=>v.policyHashAfter='other']){const e=reviewer();const v=JSON.parse(e.trace[1].output);mutate(v);e.trace[1].output=JSON.stringify(v);assert.equal(checkUnitEvidence(e,reviewerSuite,profilePin).status,'FAIL');}});
test('partial actual probes retain profile binding without declaring full policy guard coverage',()=>{const e=reviewer(),v=JSON.parse(e.trace[1].output);v.status='partial';v.probes[0].decision='missing';e.trace[1].output=JSON.stringify(v);const r=checkUnitEvidence(e,reviewerSuite,profilePin);assert.equal(r.status,'PASS');assert.match(r.hostProbeCoverage,/partial does not prove/);v.status='verified';e.trace[1].output=JSON.stringify(v);assert.equal(checkUnitEvidence(e,reviewerSuite,profilePin).status,'FAIL');});
test('profile file drift, missing probe, denied fixture bypass and guard-failed cannot pass',()=>{for(const mutate of [e=>e.profileAfter={sha256:'drift'},e=>{const v=JSON.parse(e.trace[1].output);v.probes.pop();e.trace[1].output=JSON.stringify(v);},e=>{const v=JSON.parse(e.trace[1].output);v.probes.at(-1).decision='missing';v.status='guard-failed';e.trace[1].output=JSON.stringify(v);},e=>{const v=JSON.parse(e.trace[1].output);v.probes[0].guardInstalled=false;v.status='guard-failed';e.trace[1].output=JSON.stringify(v); }]){const e=reviewer();mutate(e);assert.equal(checkUnitEvidence(e,reviewerSuite,profilePin).status,'FAIL');}});

test('preknown MCP fixtures cannot masquerade as missing builtin probes',()=>{for(const fixture of [false,undefined,'false']){const e=reviewer(),v=JSON.parse(e.trace[1].output);for(const p of v.probes.filter(p=>p.name.startsWith('mcp__'))){p.fixture=fixture;p.decision='missing';}v.status='partial';e.trace[1].output=JSON.stringify(v);assert.equal(checkUnitEvidence(e,reviewerSuite,profilePin).status,'FAIL');}});

// Actual R seq9/17/19 syntax: controller parsing, never shell execution.
import * as evidenceProvider from './role-unit-evidence-v3.mjs';
const missionWorkspace='0b02adb6-a395-46bd-bd92-6fec14dee20e';
const ownIssue='01a11ff1-70ca-7ce5-aa0c-c4170cf69b6d';
const actualScopedCommands=[
 'multica --workspace-id "$MISSION_WORKSPACE_ID" issue comment list '+ownIssue+' --roots-only --summary --compact --output json',
 'multica --workspace-id "$MISSION_WORKSPACE_ID" issue comment add '+ownIssue+' --content-file ./reply.md --output json && rm ./reply.md',
 'multica --workspace-id "$MISSION_WORKSPACE_ID" issue status '+ownIssue+' in_review'
];
for(const [i,command] of actualScopedCommands.entries())test('actual R workspace export/newline seq '+[9,17,19][i],()=>{
 const raw='export MISSION_WORKSPACE_ID='+missionWorkspace+'\n'+command;
 const r=evidenceProvider.normalizeKnownMissionWorkspaceCommand(raw,missionWorkspace);
 assert.equal(r.status,'NORMALIZED');assert.equal(r.rawCommand,raw);
 assert.equal(r.normalizedCommand,command.replace('$MISSION_WORKSPACE_ID',missionWorkspace));
});
test('normalization preserves literal command and never repairs missing scope',()=>{
 const raw='multica issue get '+ownIssue+' --output json';
 const r=evidenceProvider.normalizeKnownMissionWorkspaceCommand(raw,missionWorkspace);
 assert.equal(r.status,'UNCHANGED');assert.equal(r.normalizedCommand,raw);assert.doesNotMatch(r.normalizedCommand,/--workspace-id/);
});
test('workspace normalization rejects unknown, missing, empty and mismatched bindings and shell expansion',()=>{
 for(const raw of [
  'MISSION_WORKSPACE_ID="" ; multica issue get '+ownIssue,
  'export MISSION_WORKSPACE_ID=11111111-1111-1111-1111-111111111111\nmultica --workspace-id "$MISSION_WORKSPACE_ID" issue get '+ownIssue,
  'multica --workspace-id "$MISSION_WORKSPACE_ID" issue get '+ownIssue,
  'export OTHER='+missionWorkspace+'\nmultica --workspace-id "$OTHER" issue get '+ownIssue,
  'export MISSION_WORKSPACE_ID='+missionWorkspace+'\nmultica --workspace-id "$(echo '+missionWorkspace+')" issue get '+ownIssue,
  'export MISSION_WORKSPACE_ID='+missionWorkspace+'\nmultica --workspace-id "$MISSION_WORKSPACE_IDsuffix" issue get '+ownIssue,
  'export MISSION_WORKSPACE_ID='+missionWorkspace+'\nMISSION_WORKSPACE_ID=other; multica --workspace-id "$MISSION_WORKSPACE_ID" issue get '+ownIssue,
  'export MISSION_WORKSPACE_ID='+missionWorkspace+'\nmultica --workspace-id "$MISSION_WORKSPACE_ID" issue get '+ownIssue+'\nrm ./reply.md'
 ]){const r=evidenceProvider.normalizeKnownMissionWorkspaceCommand(raw,missionWorkspace);assert.equal(r.status,'UNSUPPORTED',raw);assert.equal(r.rawCommand,raw);assert.equal(r.normalizedCommand,null);}
});
test('single-quoted workspace variable stays literal, not a bound UUID',()=>{
 const raw='export MISSION_WORKSPACE_ID='+missionWorkspace+"\nmultica --workspace-id '$MISSION_WORKSPACE_ID' issue get "+ownIssue;
 const r=evidenceProvider.normalizeKnownMissionWorkspaceCommand(raw,missionWorkspace);
 assert.equal(r.status,'NORMALIZED');assert.match(r.normalizedCommand,/'\$MISSION_WORKSPACE_ID'/);assert.doesNotMatch(r.normalizedCommand,new RegExp(missionWorkspace));
});

// Actual T seq20 normal shell composition; observation view only.
const publicRoot='/private/tmp/51da4a84-d911-4f2a-8550-bcabc6f60d79';
const publicResource=publicRoot+'/.multica/project/resources.json';
for(const raw of ['cat '+publicResource,'cat '+publicResource+' 2>/dev/null','ls -la '+publicRoot+'; cat '+publicResource+' 2>/dev/null','ls -la "'+publicRoot+'" && cat "'+publicResource+'"'])test('public context actual readonly operand: '+raw,()=>{
 const r=evidenceProvider.observeOwnedPublicContextReads(raw,[publicRoot]);assert.equal(r.rawCommand,raw);assert.equal(r.status,'MASKED');assert.equal(r.maskedPaths.length,1);assert.doesNotMatch(r.observedCommand,/\.multica/);assert.equal(r.maskedPaths[0].resolvedPath,publicResource);
});
test('quoted literal, nested/private target and stdout redirection do not become public reads',()=>{
 for(const raw of ["echo 'cat "+publicResource+"'",'cat '+publicRoot+'/nested/.multica/project/resources.json','cat '+publicRoot+'/.multica/sessions/old','cat '+publicResource+' > '+publicResource,'printf x > '+publicResource,'cat '+publicResource+' 2> '+publicRoot+'/private.log']){const r=evidenceProvider.observeOwnedPublicContextReads(raw,[publicRoot]);assert.equal(r.maskedPaths.length,0,raw);assert.equal(r.observedCommand,raw);}
});
test('readonly mask does not conceal private writes or authorize surrounding programs',()=>{
 for(const raw of ['printf x > '+publicResource+'; cat '+publicResource,'unknown-program; cat '+publicResource]){const r=evidenceProvider.observeOwnedPublicContextReads(raw,[publicRoot]);assert.equal(r.maskedPaths.length,1);assert.equal(r.rawCommand,raw);assert.match(r.observedCommand,/^(?:printf x > .*\.multica|unknown-program)/);}
 assert.equal(evidenceProvider.observeOwnedPublicContextReads('cat "'+publicResource,[publicRoot]).status,'UNSUPPORTED');
});

test('quoted stderr token is an extra operand and comments are unsupported, never masked',()=>{
 for(const suffix of [" '2>/dev/null'",' "2>/dev/null"',' ./other-file']){const raw='cat '+publicResource+suffix,r=evidenceProvider.observeOwnedPublicContextReads(raw,[publicRoot]);assert.equal(r.maskedPaths.length,0);assert.equal(r.observedCommand,raw);}
 const raw='echo ok # ; cat '+publicResource,r=evidenceProvider.observeOwnedPublicContextReads(raw,[publicRoot]);assert.equal(r.status,'UNSUPPORTED');assert.equal(r.maskedPaths.length,0);assert.equal(r.observedCommand,raw);
});

// Actual U head -50 early stop is retained separately; these test the corrected native contract.
import * as workspaceProviderX from './role-unit-evidence-v3.mjs';
import fsX from 'node:fs';
const workspaceX='0b02adb6-a395-46bd-bd92-6fec14dee20e';
const nativeX={expectedWorkspace:workspaceX,callerKind:'native',runtimeWorkspace:workspaceX};
const inspectX=(tokens,context=nativeX)=>workspaceProviderX.inspectMulticaWorkspaceScope(tokens,context);
test('native runtime scope accepts actual U own-read head50 and ordinary command variants without inventing env proof',()=>{for(const suffix of [[],['2>&1','|','head','-50'],['2>&1','|','head','-100'],['2>&1','|','head','-7']]){const r=inspectX(['multica','issue','get','01a1203e-945a-7594-a904-5d9927212d79','--output','json',...suffix]);assert.equal(r.status,'ALLOW_RUNTIME_CONTEXT');assert.equal(r.effectiveScope,'UNVERIFIED');assert.equal(r.explicitWorkspace,null);}});
test('native matching explicit split or equals flag preserves arguments',()=>{for(const tokens of [['multica','--workspace-id',workspaceX,'issue','get','own'],['multica','--workspace-id='+workspaceX,'issue','get','own']]){const r=inspectX(tokens);assert.equal(r.status,'ALLOW_EXPLICIT_SCOPE');assert.deepEqual(r.args,['issue','get','own']);}});
test('external calls require explicit workspace even if supplied runtime context matches',()=>{const context={...nativeX,callerKind:'external'};assert.equal(inspectX(['multica','issue','get','own'],context).status,'REJECT');assert.equal(inspectX(['multica','--workspace-id',workspaceX,'issue','get','own'],context).status,'ALLOW_EXPLICIT_SCOPE');});
test('missing or foreign runtime, explicit or actual workspace cannot pass',()=>{const foreign='11111111-1111-1111-1111-111111111111';for(const [tokens,context]of [[['multica','issue','get','own'],{...nativeX,runtimeWorkspace:null}],[['multica','issue','get','own'],{...nativeX,runtimeWorkspace:foreign}],[['multica','--workspace-id',foreign,'issue','get','own'],nativeX],[['multica','issue','get','own'],{...nativeX,observedWorkspace:foreign}]])assert.equal(inspectX(tokens,context).status,'REJECT');});
test('duplicate flags, profile/server overrides, shell env prefixes and unbound variable flags remain rejected',()=>{for(const tokens of [['multica','--workspace-id',workspaceX,'--workspace-id='+workspaceX,'issue','get','own'],['multica','--profile','other','issue','get','own'],['multica','issue','get','own','--server-url=other'],['env','MULTICA_WORKSPACE_ID=other','multica','issue','get','own'],['multica','--workspace-id','$MISSION_WORKSPACE_ID','issue','get','own']])assert.equal(inspectX(tokens).status,'REJECT');});
test('observed same-workspace output remains separate from metadata expectation',()=>{assert.equal(inspectX(['multica','issue','get','own'],{...nativeX,observedWorkspace:workspaceX}).effectiveScope,'OBSERVED_MATCH');});
test('owning native instructions and both assigned Skills use runtime binding rather than forbid it',()=>{const module=new URL('../../../',import.meta.url);const role=fsX.readFileSync(new URL('instructions/orchestrator/instructions.md',module),'utf8').split('\n\n')[0];const orch=fsX.readFileSync(new URL('skills/swarm-orchestration/SKILL.md',module),'utf8').split('## 一、')[0];const cli=fsX.readFileSync(new URL('generated/multica-cli.md',module),'utf8').split('## Issue 操作')[0];for(const body of [role,orch,cli]){assert.match(body,/MULTICA_WORKSPACE_ID/);assert.doesNotMatch(body,/never inherit|不要依賴預設 workspace 或繼承的環境變數|Every .*invocation/);}});

test('external option-value workspace text is not an actual global flag',()=>{assert.equal(inspectX(['multica','issue','comment','add','own','--body','--workspace-id='+workspaceX],{...nativeX,callerKind:'external'}).status,'REJECT');});
test('workspace text after end-of-options cannot establish external scope',()=>{assert.equal(inspectX(['multica','issue','get','own','--','--workspace-id='+workspaceX],{...nativeX,callerKind:'external'}).status,'REJECT');});
