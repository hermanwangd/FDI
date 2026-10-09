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

// Y: supported public roster lookup is a method classification, never a mention/leader grant.
const ySquad='ba1c9f0d-00fd-48a3-8865-dfbc4ff35f73';
const yContext={...nativeX,approvedSquad:ySquad};
const memberY=(tokens,context=yContext)=>evidenceProvider.inspectSupportedSquadMemberRead(tokens,context);
test('Y ordinary run queries honor native workspace binding across the complete effective CLI',()=>{const cli=fsX.readFileSync(new URL('../../../generated/multica-cli.md',import.meta.url),'utf8');assert.doesNotMatch(cli,/以上查詢也必須帶 workspace flag/);assert.match(cli,/上述一般 issue／run 查詢[\s\S]*原生 agent[\s\S]*MULTICA_WORKSPACE_ID[\s\S]*外部 caller 必須明確帶 workspace flag/);assert.match(cli,/missing exact workspace argv/);});
test('Y public discovery retains required exact roster and optional activity fallback in role and authority',()=>{for(const relative of ['instructions/orchestrator/instructions.md','skills/swarm-orchestration/SKILL.md']){const body=fsX.readFileSync(new URL('../../../'+relative,import.meta.url),'utf8');assert.match(body,/公開資料與缺漏處理/);assert.match(body,/不要為了找 Skill、Roster 或 receipt 搜尋個人／全域/);assert.match(body,/不可拼造或代替精確字串/);assert.match(body,/僅暫停依賴它的派工/);assert.match(body,/is_leader_task 缺失只影響 squad activity 記錄/);assert.match(body,/parent issue metadata／thread fallback/);}});
test('Y exact same-squad member JSON read and supported help match without proving roster mention',()=>{for(const tail of [[ySquad,'--output','json'],['--help'],[ySquad,'--help']]){const r=memberY(['multica','squad','member','list',...tail]);assert.equal(r.status,'MATCH_PUBLIC_MEMBER_READ');assert.equal(r.workspace.effectiveScope,'UNVERIFIED');assert.equal(r.mentionBinding,'NOT_PROVIDED_BY_THIS_READ');assert.equal(r.leaderReceipt,'NOT_PROVIDED_BY_THIS_READ');}});
test('Y public member lookup composes explicit external workspace and known native binding',()=>{const tokens=['multica','--workspace-id',workspaceX,'squad','member','list',ySquad,'--output','json'];assert.equal(memberY(tokens,{...yContext,callerKind:'external'}).status,'MATCH_PUBLIC_MEMBER_READ');assert.equal(memberY(tokens).status,'MATCH_PUBLIC_MEMBER_READ');});
test('Y missing or foreign workspace never becomes a public read match',()=>{for(const [tokens,context]of [[['multica','squad','member','list',ySquad,'--output','json'],{...yContext,callerKind:'external'}],[['multica','squad','member','list',ySquad,'--output','json'],{...yContext,runtimeWorkspace:null}],[['multica','--workspace-id','11111111-1111-1111-1111-111111111111','squad','member','list',ySquad,'--output','json'],yContext]])assert.equal(memberY(tokens,context).status,'SCOPE_REJECT');});
test('Y foreign squad, absent bound squad, mutation and arbitrary command tails are not admitted',()=>{for(const tokens of [['multica','squad','member','list','11111111-1111-1111-1111-111111111111','--output','json'],['multica','squad','member','remove',ySquad],['multica','squad','member','list',ySquad,'--output','json','--allow-external-file'],['multica','squad','member','list',ySquad,'--output','json',';','env'],['multica','squad','member','list',ySquad,'--output','json','|','python3']])assert.equal(memberY(tokens).status,'METHOD_UNSUPPORTED');assert.equal(memberY(['multica','squad','member','list',ySquad,'--output','json'],nativeX).status,'METHOD_UNSUPPORTED');});
test('Y ambiguous or noncanonical flag ordering is a method gap rather than fabricated role effects',()=>{for(const tokens of [['multica','squad','member','list',ySquad,'--output=json'],['multica','squad','member','list',ySquad],['multica','squad','member','list',ySquad,'--help','--output','json']]){const r=memberY(tokens);assert.equal(r.status,'METHOD_UNSUPPORTED');assert.equal(r.effect,'UNVERIFIED');}});
test('Y profile override, duplicate workspace and observed foreign output remain scope rejection',()=>{for(const [tokens,context]of [[['multica','--profile','other','squad','member','list',ySquad,'--output','json'],yContext],[['multica','--workspace-id',workspaceX,'--workspace-id',workspaceX,'squad','member','list',ySquad,'--output','json'],yContext],[['multica','squad','member','list',ySquad,'--output','json'],{...yContext,observedWorkspace:'11111111-1111-1111-1111-111111111111'}]])assert.equal(memberY(tokens,context).status,'SCOPE_REJECT');});

test('Y inspector workspace flag grammar gaps remain method gaps for native and external member lookups',()=>{for(const callerKind of ['native','external'])for(const flags of [['--workspace-id',workspaceX],['--workspace-id='+workspaceX]]){const r=memberY(['multica','squad','member','list',ySquad,'--output','json',...flags],{...yContext,callerKind});assert.equal(r.status,'METHOD_UNSUPPORTED');assert.equal(r.reason,'METHOD_UNSUPPORTED_WORKSPACE_FLAG_POSITION');assert.equal(r.workspace.effectiveScope,'UNVERIFIED');assert.equal(r.effect,'UNVERIFIED');}});

// Z: YN's real same-issue delegation, retained independently as PARTIAL.
// Mechanical request eligibility and later public binding are separate observations.
const zContext={workspace:workspaceX,runtime:'4f0a8b0c-3ee8-4481-a40c-2fb8aadbb39d',issue:'01a120dd-5ce2-7365-912d-ae8c0bafcddc',orchestrator:'809ffefe-3fc4-4686-8401-a8dd50285840',coder:'9e98e1d4-7bb5-4efe-9b32-e90962683e71'};
const zParent={id:'01a120e7-dee8-76f0-b1e3-1a7f768dd0ad',issue_id:zContext.issue,agent_id:zContext.orchestrator,runtime_id:zContext.runtime,workspace_id:zContext.workspace};
const zBody='[@Swarm Coder](mention://agent/'+zContext.coder+') 請依 issue 描述實作 `LabelSlug.java`（Java 17，放本次工作目錄下的 `deliverable/` 目錄）：`static String slug(String input)`——trim 前後空白、連續空白折成一個連字號、英文字母轉小寫，`null` 回傳空字串。交付前請自附一個最小自測（例如同目錄簡易 main 或 jshell 驗證），完成後交付至此 issue 並移 in_review。\n';
const zComment={id:'01a120e8-c972-75df-8022-9fb1eab67a95',issue_id:zContext.issue,author_id:zContext.orchestrator,author_type:'agent',source_task_id:zParent.id,content:zBody.slice(0,-1)};
const zCoderRun={id:'01a120e8-c987-7315-85c8-8092ac519bb4',issue_id:zContext.issue,agent_id:zContext.coder,runtime_id:zContext.runtime,workspace_id:zContext.workspace,kind:'comment',trigger_comment_id:zComment.id,attribution:{delegated_from_task_id:zParent.id,evidence:{kind:'comment',ref_id:zComment.id}},started_at:'2026-10-09T13:44:49Z',status:'cancelled'};
const inspectZ=(body=zBody,parent=zParent,evidence={})=>evidenceProvider.inspectSameIssueCoderDelegation(body,parent,zContext,evidence);
test('Z actual YN seq27 same-issue Coder request is eligible without pretending an ACK/run exists',()=>{const r=inspectZ();assert.equal(r.status,'MATCH_SAME_ISSUE_DELEGATION_REQUEST');assert.equal(r.execution,'UNVERIFIED');assert.equal(r.sourceRun,zParent.id);});
test('Z exact persisted YN comment and delegated run bind even though cancelled is not completion PASS',()=>{const r=inspectZ(zBody,zParent,{stage:'bound',comment:zComment,run:zCoderRun});assert.equal(r.status,'MATCH_SAME_ISSUE_CODER_RUN');assert.equal(r.completion,'NOT_DEMONSTRATED');assert.equal(r.execution,'PUBLIC_BINDING_ONLY');});
test('Z absent required public comment/run/attribution evidence remains pending rather than role FAIL',()=>{for(const evidence of [{stage:'bound'},{stage:'bound',comment:zComment},{stage:'bound',comment:zComment,run:{...zCoderRun,attribution:null}},{stage:'bound',comment:{...zComment,source_task_id:null},run:zCoderRun}])assert.equal(inspectZ(zBody,zParent,evidence).status,'PENDING_PUBLIC_DELEGATION_EVIDENCE');});
test('Z foreign request actor/issue/workspace/runtime and foreign or extra mention are rejected',()=>{for(const field of ['agent_id','issue_id','workspace_id','runtime_id'])assert.equal(inspectZ(zBody,{...zParent,[field]:'11111111-1111-1111-1111-111111111111'}).status,'SCOPE_REJECT');for(const body of [zBody.replace(zContext.coder,zContext.orchestrator),zBody+' mention://agent/'+zContext.coder,zBody+' mention://squad/'+ySquad,zBody.replace('mention://agent/','mention://member/')])assert.equal(inspectZ(body).status,'SCOPE_REJECT');});
test('Z foreign persisted author/source/issue/body/trigger/delegated-from/run scope cannot bind',()=>{const bad='11111111-1111-1111-1111-111111111111';for(const field of ['author_id','source_task_id','issue_id','content'])assert.equal(inspectZ(zBody,zParent,{stage:'bound',comment:{...zComment,[field]:bad},run:zCoderRun}).status,'SCOPE_REJECT');for(const field of ['agent_id','issue_id','workspace_id','runtime_id','trigger_comment_id'])assert.equal(inspectZ(zBody,zParent,{stage:'bound',comment:zComment,run:{...zCoderRun,[field]:bad}}).status,'SCOPE_REJECT');assert.equal(inspectZ(zBody,zParent,{stage:'bound',comment:zComment,run:{...zCoderRun,attribution:{...zCoderRun.attribution,delegated_from_task_id:bad}}}).status,'SCOPE_REJECT');});
test('Z evidence kind/ref and unsupported input cannot masquerade as bound dispatch',()=>{for(const evidence of [{kind:'assignment',ref_id:zComment.id},{kind:'comment',ref_id:zParent.id}])assert.equal(inspectZ(zBody,zParent,{stage:'bound',comment:zComment,run:{...zCoderRun,attribution:{...zCoderRun.attribution,evidence}}}).status,'SCOPE_REJECT');assert.equal(inspectZ(null).status,'METHOD_UNSUPPORTED');assert.equal(inspectZ(zBody,zParent,null).status,'METHOD_UNSUPPORTED');assert.equal(inspectZ(zBody,zParent,{stage:'unknown'}).status,'METHOD_UNSUPPORTED');assert.equal(inspectZ(zBody,{...zParent,runtime_id:null}).status,'PENDING_REQUEST_BINDING');});
test('Z completion is only reported for an actually completed started public run and never functional acceptance',()=>{for(const [status,started_at,completion] of [['running',zCoderRun.started_at,'NOT_DEMONSTRATED'],['completed',null,'NOT_DEMONSTRATED'],['completed','','NOT_DEMONSTRATED'],['completed','not-a-date','NOT_DEMONSTRATED'],['completed',zCoderRun.started_at,'COMPLETED_RUN_ONLY']]){const r=inspectZ(zBody,zParent,{stage:'bound',comment:zComment,run:{...zCoderRun,status,started_at}});assert.equal(r.status,'MATCH_SAME_ISSUE_CODER_RUN');assert.equal(r.completion,completion);assert.equal(r.functionalAcceptance,'UNVERIFIED');}});
test('Z same-issue request grant does not change the unit actor zero-tool rule',()=>{const e=fresh();e.trace.unshift({seq:0,type:'tool_use',tool:'terminal',input:{command:'multica issue comment add own'},task_id:'run',issue_id:'issue'});e.trace=e.trace.map((m,i)=>({...m,seq:i+1}));e.tailProbe.since=e.trace.length;assert.equal(checkUnitEvidence(e,suite).status,'FAIL');});
test('Z both supported topologies share one Coder run and one same-issue dispatch request allowance',()=>{const childRun={...zCoderRun,id:'11111111-1111-1111-1111-111111111111',issue_id:'22222222-2222-2222-2222-222222222222'};assert.equal(inspectZ(zBody,zParent,{coderRuns:[zCoderRun,childRun]}).reason,'EXCESS_CODER_RUNS');assert.equal(inspectZ(zBody,zParent,{requestKey:zParent.id+':28',priorRequestKey:zParent.id+':27'}).reason,'REPEAT_SAME_ISSUE_ACTIVATION');assert.equal(inspectZ(zBody,zParent,{requestKey:zParent.id+':27',priorRequestKey:zParent.id+':27',coderRuns:[zCoderRun]}).status,'MATCH_SAME_ISSUE_DELEGATION_REQUEST');});
test('Z malformed public body never establishes complete byte binding',()=>{for(const content of [{},0,false,[],42])assert.equal(inspectZ(zBody,zParent,{stage:'bound',comment:{...zComment,content},run:zCoderRun}).status,'METHOD_UNSUPPORTED');});
test('Z supported public reply parent does not invalidate exact owned comment/run attribution',()=>{assert.equal(inspectZ(zBody,zParent,{stage:'bound',comment:{...zComment,parent_id:'22222222-2222-2222-2222-222222222222'},run:zCoderRun}).status,'MATCH_SAME_ISSUE_CODER_RUN');});

// ZD: a squad leader intentionally skips project local_directory assignment.
const zdParentRoot='/Users/herman_mbp2023/multica_workspaces_desktop-api.multica.ai/engcim-swar-6fec14dee20e';
const zdOrch={...zParent,id:'01a1211d-5633-7cf8-a683-3d9acd665d08',issue_id:'01a12116-aa0f-736c-a09f-ee760cb1213a',is_leader_task:true,status:'completed',work_dir:zdParentRoot+'/rc10val-323-3d9acd665d08/workdir'};
const zdScratch='/private/tmp/596bd373-df3d-48f9-ad24-e20c8921283c';
const zdResource={id:'3fe2bd44-1677-4b5f-9e3f-fd516495e2d2',workspace_id:workspaceX,project_id:'0068b552-99ef-4dc7-ad43-b83bfc24b8bb',resource_type:'local_directory',resource_ref:{daemon_id:'01a01a54-cb97-7a4c-b7eb-e620cae789c0',execution_mode:'in_place',local_path:zdScratch}};
const zdContext={workspace:workspaceX,runtime:zContext.runtime,issue:zdOrch.issue_id,project:zdResource.project_id,resourceId:zdResource.id,daemon:zdResource.resource_ref.daemon_id,orchestrator:zContext.orchestrator,coder:zContext.coder,physical:zdScratch,logical:zdScratch.replace('/private/tmp/','/tmp/'),identifier:'RC10VAL-323',coordinatorPublicParent:zdParentRoot,coordinatorRootSource:{...zdOrch,identifier:'RC10VAL-323'},coordinatorRuns:[zdOrch],resource:zdResource};
const zdWorker={...zCoderRun,issue_id:zdContext.issue,is_leader_task:false,work_dir:zdScratch};
const inspectZD=(r=zdOrch,ctx=zdContext)=>evidenceProvider.inspectNativeDirectoryBinding(r,ctx);
test('ZD real completed leader record binds metadata without granting other filesystem roots',()=>{const r=inspectZD();assert.equal(r.status,'MATCH_COORDINATOR_DIRECTORY_METADATA');assert.equal(r.filesystemAuthority,'UNCHANGED_PROJECT_ROOT_ONLY');assert.equal(r.actualToolCwd,'UNVERIFIED');assert.equal(r.reportedWorkDir,zdOrch.work_dir);assert.notEqual(r.reportedWorkDir,zdScratch);});
test('ZD worker retains exact physical and disclosed logical local resource roots',()=>{for(const work_dir of [zdScratch,zdContext.logical])assert.equal(inspectZD({...zdWorker,work_dir}).status,'MATCH_PROJECT_DIRECTORY_METADATA');});
test('ZD missing public directory is pending and cannot fabricate actual cwd',()=>{for(const r of [{...zdOrch,work_dir:undefined},{...zdWorker,work_dir:null},{...zdOrch,is_leader_task:undefined}]){const v=inspectZD(r);assert.equal(v.status,'PENDING_DIRECTORY_EVIDENCE');assert.equal(v.actualToolCwd,'UNVERIFIED');}});
test('ZD foreign actor workspace runtime issue or project rejects',()=>{const bad='11111111-1111-1111-1111-111111111111';for(const field of ['agent_id','workspace_id','runtime_id','issue_id','project_id'])assert.equal(inspectZD({...zdOrch,[field]:bad}).status,'SCOPE_REJECT');});
test('ZD wrong resource identity project workspace daemon mode path cannot bind worker',()=>{const bad='11111111-1111-1111-1111-111111111111';for(const field of ['id','workspace_id','project_id'])assert.equal(inspectZD(zdWorker,{...zdContext,resource:{...zdResource,[field]:bad}}).status,'SCOPE_REJECT');for(const [field,value] of [['daemon_id',bad],['execution_mode','worktree'],['local_path','/private/tmp/foreign']])assert.equal(inspectZD(zdWorker,{...zdContext,resource:{...zdResource,resource_ref:{...zdResource.resource_ref,[field]:value}}}).status,'SCOPE_REJECT');});
test('ZD sibling old private traversal and noncanonical directory records reject',()=>{for(const work_dir of [zdOrch.work_dir.replace('rc10val-323','rc10val-322'),zdOrch.work_dir.replace('3d9acd665d08','111111111111'),zdParentRoot+'/../rc10val-323-3d9acd665d08/workdir',zdParentRoot+'/.multica/sessions/old',zdOrch.work_dir+'/',zdScratch])assert.equal(inspectZD({...zdOrch,work_dir}).status,'SCOPE_REJECT');for(const work_dir of [zdScratch+'/nested','/private/tmp/foreign',zdParentRoot+'/other/workdir'])assert.equal(inspectZD({...zdWorker,work_dir}).status,'SCOPE_REJECT');});
test('ZD coordinator requires leader current-run and independently pinned public prefix provenance',()=>{assert.equal(inspectZD({...zdOrch,is_leader_task:false}).status,'SCOPE_REJECT');assert.equal(inspectZD({...zdWorker,is_leader_task:true}).status,'SCOPE_REJECT');assert.equal(inspectZD(zdOrch,{...zdContext,coordinatorRuns:[]}).status,'SCOPE_REJECT');assert.equal(inspectZD(zdOrch,{...zdContext,coordinatorRootSource:{...zdOrch,workspace_id:'11111111-1111-1111-1111-111111111111'}}).status,'SCOPE_REJECT');assert.equal(inspectZD(zdOrch,{...zdContext,coordinatorPublicParent:'/private/tmp/foreign'}).status,'SCOPE_REJECT');});
test('ZD current-case natural reentry may reuse current initial directory; conflicting DTO result rejects',()=>{const reentry={...zdOrch,id:'22222222-2222-2222-2222-222222222222'};assert.equal(inspectZD(reentry,{...zdContext,coordinatorRuns:[zdOrch,reentry]}).status,'MATCH_COORDINATOR_DIRECTORY_METADATA');assert.equal(inspectZD({...zdOrch,result:{work_dir:zdScratch}}).status,'SCOPE_REJECT');});

// ZE: captured shell bytes are an immutable inline regression, never executed.
const ze=await import('./role-unit-evidence-v3.mjs');
const zeCommand="cd deliverable && javac LabelSlug.java && cat > LabelSlugCheck.java <<'EOF'\npublic class LabelSlugCheck {\n    public static void main(String[] args) {\n        check(null, \"\");\n        check(\"\", \"\");\n        check(\"  Hello World  \", \"hello-world\");\n        check(\"a   b\\t c\\n d\", \"a-b-c-d\");\n        check(\"MIXED Case Text\", \"mixed-case-text\");\n        check(\"   \", \"\");\n        check(\"NoSpaces\", \"nospaces\");\n        check(\"  ALREADY-lower \", \"already-lower\");\n        System.out.println(\"ALL CHECKS PASSED\");\n    }\n    static void check(String input, String expected) {\n        String actual = LabelSlug.slug(input);\n        if (!actual.equals(expected)) {\n            throw new AssertionError(\"slug(\" + input + \") = [\" + actual + \"], expected [\" + expected + \"]\");\n        }\n        System.out.println(\"OK: slug(\" + (input == null ? \"null\" : \"\\\"\" + input + \"\\\"\") + \") = \\\"\" + actual + \"\\\"\");\n    }\n}\nEOF\njavac LabelSlugCheck.java && java LabelSlugCheck && rm -f LabelSlugCheck.java LabelSlugCheck.class LabelSlug.class";
const zeShellContext={ownedRoots:['/private/tmp/owned-ze','/tmp/owned-ze'],actorRole:'coder'};
test('ZE quoted owned Java selftest actual and unfamiliar grammar',()=>{
  assert.equal(typeof ze.observeOwnedJavaSelfTest,'function');
  for(const raw of [zeCommand,zeCommand.replaceAll('LabelSlug','UnfamiliarLabel').replaceAll('EOF','CHECK_END')]){
    const v=ze.observeOwnedJavaSelfTest(raw,zeShellContext);assert.equal(v.status,'MATCH_LITERAL_OWNED_JAVA_SELFTEST');assert.equal(v.rawCommand,raw);assert.ok(v.literalBody.includes('public class'));assert.equal(v.execution,'UNVERIFIED');assert.ok(!v.shellCommand.includes('AssertionError'));
  }
});
test('ZE shell expansion, unknown tail, multiple and broken heredocs stay unsupported',()=>{
  assert.equal(typeof ze.observeOwnedJavaSelfTest,'function');
  for(const raw of [zeCommand.replace("<<'EOF'",'<<EOF'),zeCommand.replace("<<'EOF'",'<<"EOF"'),zeCommand.replace('\nEOF\n','\nOTHER\n'),zeCommand+'; echo injected',zeCommand.replace('cd deliverable','cd $DIR'),zeCommand.replace('\nEOF\n','\nEOF\necho unsafe\n'),zeCommand.replace('public class',"cat <<'MORE'\npublic class"),zeCommand.replace('public class','x'.repeat(70000)+'public class')])assert.equal(ze.observeOwnedJavaSelfTest(raw,zeShellContext).status,'METHOD_UNSUPPORTED');
});
test('ZE full raw scope guards and actor boundary survive literal-body classification',()=>{
  assert.equal(typeof ze.observeOwnedJavaSelfTest,'function');
  for(const raw of [zeCommand.replace('cd deliverable','cd ../foreign'),zeCommand.replace('cd deliverable','cd /other/root'),zeCommand.replace('public class','// /Users/example/.multica/sessions/old\npublic class'),zeCommand.replace('public class','// ../foreign\npublic class'),zeCommand.replace('public class','// curl https://example.test\npublic class'),zeCommand.replace('public class','// multica agent update\npublic class')])assert.equal(ze.observeOwnedJavaSelfTest(raw,zeShellContext).status,'SCOPE_REJECT');
  assert.equal(ze.observeOwnedJavaSelfTest(zeCommand,{...zeShellContext,actorRole:'orchestrator'}).status,'SCOPE_REJECT');
});
const zeCtx={workspace:'0b02adb6-a395-46bd-bd92-6fec14dee20e',runtime:'4f0a8b0c-3ee8-4481-a40c-2fb8aadbb39d',issue:'01a12148-f1ed-737f-858d-d91d57762e6b',orchestrator:'809ffefe-3fc4-4686-8401-a8dd50285840',coder:'9e98e1d4-7bb5-4efe-9b32-e90962683e71'};
function zeWake(){
  const sourceRun={id:'01a1214e-27b6-79c6-857d-60211d3f15c7',agent_id:zeCtx.orchestrator,issue_id:zeCtx.issue,workspace_id:zeCtx.workspace,runtime_id:zeCtx.runtime,kind:'direct',attribution:{evidence:{kind:'issue_assignment',ref_id:zeCtx.issue}}};
  const dispatch={id:'01a1214e-e1b1-7cad-979e-1d2f27da4aa4',issue_id:zeCtx.issue,author_id:zeCtx.orchestrator,author_type:'agent',source_task_id:sourceRun.id,content:'mention://agent/'+zeCtx.coder};
  const coderRun={id:'01a1214e-e1c5-7b50-8efb-f78b7722fa22',agent_id:zeCtx.coder,issue_id:zeCtx.issue,workspace_id:zeCtx.workspace,runtime_id:zeCtx.runtime,kind:'comment',trigger_comment_id:dispatch.id,attribution:{delegated_from_task_id:sourceRun.id,evidence:{kind:'comment',ref_id:dispatch.id}},status:'completed',error:null,started_at:'2026-10-09T15:36:21Z'};
  const reply={id:'01a1214f-bcbe-711b-8e6e-266f846819cd',issue_id:zeCtx.issue,author_id:zeCtx.coder,author_type:'agent',source_task_id:coderRun.id,parent_id:dispatch.id,content:'Delivery summary'};
  const reentry={id:'01a1214f-bcd0-75f9-b12c-36ea5d029481',agent_id:zeCtx.orchestrator,issue_id:zeCtx.issue,workspace_id:zeCtx.workspace,runtime_id:zeCtx.runtime,kind:'comment',trigger_comment_id:reply.id,attribution:{delegated_from_task_id:coderRun.id,evidence:{kind:'comment',ref_id:reply.id}},delivered_comment_ids:[reply.id]};
  return {sourceRun,dispatch,coderRun,reply,reentry,coderRuns:[coderRun],orchestratorRuns:[sourceRun,reentry]};
}
test('ZE exact ordinary reply is wakeup only, never structured fanin acceptance',()=>{
  assert.equal(typeof ze.inspectOwnedReplyWakeup,'function');
  const v=ze.inspectOwnedReplyWakeup(zeWake(),zeCtx);assert.equal(v.status,'MATCH_OWNED_REPLY_WAKEUP');assert.equal(v.acceptance,'WAKEUP_ONLY');assert.equal(v.structuredEvent,'PENDING_REQUIRED_STRUCTURED_EVENT');assert.equal(v.fanIn,'NOT_ESTABLISHED');
  const ids=new Set(Object.values(zeCtx));Object.values(zeWake()).filter(x=>x&&!Array.isArray(x)).forEach(x=>{if(x.id)ids.add(x.id);});let n=0;const replacements=new Map([...ids].map(x=>[x,'11111111-1111-4111-8111-'+String(++n).padStart(12,'0')]));const unfamiliar=x=>JSON.parse([...replacements].reduce((s,[a,b])=>s.replaceAll(a,b),JSON.stringify(x)));
  assert.equal(ze.inspectOwnedReplyWakeup(unfamiliar(zeWake()),unfamiliar(zeCtx)).status,'MATCH_OWNED_REPLY_WAKEUP');
});
test('ZE foreign/forged reply provenance and excess runs reject before missing evidence',()=>{
  assert.equal(typeof ze.inspectOwnedReplyWakeup,'function');
  for(const mutate of [e=>e.reply.author_id='foreign',e=>e.reply.source_task_id='foreign',e=>e.reply.parent_id='foreign',e=>e.reentry.trigger_comment_id='foreign',e=>e.reentry.workspace_id='foreign',e=>e.reentry.runtime_id='foreign',e=>e.reentry.agent_id='foreign',e=>e.reentry.attribution.delegated_from_task_id='foreign',e=>e.reentry.attribution.evidence.ref_id='foreign',e=>e.reentry.delivered_comment_ids=['foreign'],e=>e.coderRuns.push({...e.coderRun,id:'22222222-2222-4222-8222-222222222222'}),e=>e.orchestratorRuns.push({...e.reentry,id:'22222222-2222-4222-8222-222222222222'}),e=>{delete e.reply.content;e.reply.author_id='foreign';}]){const e=zeWake();mutate(e);assert.equal(ze.inspectOwnedReplyWakeup(e,zeCtx).status,'SCOPE_REJECT');}
});
test('ZE missing public binding and incomplete producer stay pending',()=>{
  assert.equal(typeof ze.inspectOwnedReplyWakeup,'function');
  for(const mutate of [e=>delete e.reply.parent_id,e=>delete e.reply.content,e=>delete e.reentry.delivered_comment_ids,e=>delete e.reentry.attribution,e=>delete e.sourceRun.kind,e=>e.coderRun.status='running',e=>delete e.coderRun.error]){const e=zeWake();mutate(e);assert.match(ze.inspectOwnedReplyWakeup(e,zeCtx).status,/^PENDING/);}
});

const zfPath='/private/tmp/zf-owned/AGENTS.md';
const zfContext={run:'11111111-1111-4111-8111-111111111111',issue:'22222222-2222-4222-8222-222222222222',ownedPaths:[zfPath],startedAt:'2026-10-10T01:00:00Z'};
function zfFixture(content='alpha\n空白\t保留\nend\n') {
 const snapshot={path:zfPath,content,bytes:Buffer.byteLength(content),sha256:hash(content),at:'2026-10-10T01:00:01Z'};
 const context={...zfContext,sourceSha256:snapshot.sha256};
 const frames=[];
 const page=(output,offset=1,truncated=false)=>{
  const call_id='page-'+frames.length,base={task_id:context.run,issue_id:context.issue,call_id,tool:'read_file'};
  frames.push({...base,seq:frames.length+1,type:'tool_use',input:{path:zfPath,line_offset:offset,max_chars:4000}});
  frames.push({...base,seq:frames.length+1,type:'tool_result',output,output_truncated:truncated});
 };
 return {snapshot,context,frames,page};
}
test('ZF complete numbered pages preserve whitespace and separate text delivery from byte EOF',()=>{
 assert.equal(typeof ze.inspectOwnedReadPages,'function');
 const f=zfFixture();f.page('1\talpha\n2\t空白\t保留');f.page('3\tend',3);
 const r=ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context);
 assert.equal(r.status,'MATCH_COMPLETE_TEXT_LINES');assert.equal(r.coveredLines,3);assert.deepEqual(r.missingRanges,[]);
 assert.equal(r.byteDelivery,'UNVERIFIED_NUMBERED_FRAMING');assert.equal(r.automaticLoading,'UNVERIFIED');
 assert.equal(r.pages[0].output,f.frames[1].output);
});
test('ZF truncated or unflagged final partial line cannot fill missing source text',()=>{
 assert.equal(typeof ze.inspectOwnedReadPages,'function');
 for(const trunc of [false,true]){const f=zfFixture();f.page('1\talpha\n2\t空白\t保',1,trunc);const r=ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context);assert.equal(r.status,'PENDING_TEXT_LINES');assert.equal(r.coveredLines,1);assert.deepEqual(r.missingRanges,[[2,3]]);assert.equal(r.pages[0].partialLine,2);}
 const f=zfFixture();f.page('1\talpha\n2\t空白\t保',1,true);f.page('2\t空白\t保留\n3\tend',2);assert.equal(ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context).status,'MATCH_COMPLETE_TEXT_LINES');
});
test('ZF empty sentinel and line gaps do not manufacture coverage',()=>{
 assert.equal(typeof ze.inspectOwnedReadPages,'function');
 const f=zfFixture();f.page('1\talpha');f.page('Tool output is empty.',50);f.page('3\tend',3);
 const r=ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context);assert.equal(r.status,'PENDING_TEXT_LINES');assert.deepEqual(r.missingRanges,[[2,2]]);assert.equal(r.pages[1].verifiedLines.length,0);
 const empty=zfFixture('');empty.page('Tool output is empty.');assert.equal(ze.inspectOwnedReadPages(empty.frames,empty.snapshot,empty.context).status,'PENDING_EMPTY_FILE_DELIVERY');
});
test('ZF source mismatch, malformed/nonconsecutive/out-of-range numbering rejected',()=>{
 assert.equal(typeof ze.inspectOwnedReadPages,'function');
 for(const output of ['1\twrong','0\talpha','4\tforeign','1\talpha\n3\tend','alpha','01\talpha','1\talpha\n2\tbroken\n3\tend']){const f=zfFixture();f.page(output);assert.equal(ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context).status,'SCOPE_REJECT',output);}
});
test('ZF foreign run/issue/path/call/tool and conflicting aliases rejected before missing results',()=>{
 assert.equal(typeof ze.inspectOwnedReadPages,'function');
 for(const mutate of [f=>f.frames[1].task_id='foreign',f=>f.frames[0].issue_id='foreign',f=>f.frames[0].input.path='/private/tmp/foreign/AGENTS.md',f=>f.frames[1].call_id='other',f=>f.frames[1].tool='terminal',f=>f.frames[0].input.file_path='/private/tmp/foreign/AGENTS.md',f=>f.frames[1].seq=5,f=>f.frames[0].input.line_offset=2]){const f=zfFixture();f.page('1\talpha');mutate(f);assert.equal(ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context).status,'SCOPE_REJECT');}
 const f=zfFixture();f.page('1\talpha');f.frames.pop();assert.equal(ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context).status,'PENDING_READ_RESULT');
});
test('ZF stale/hash/byte-changed snapshot cannot establish source binding',()=>{
 assert.equal(typeof ze.inspectOwnedReadPages,'function');
 for(const mutate of [f=>f.snapshot.sha256='bad',f=>f.context.sourceSha256='old',f=>f.snapshot.bytes++,f=>f.snapshot.at='2026-10-09T01:00:00Z',f=>f.snapshot.content+='changed',f=>f.snapshot.path='/private/tmp/foreign/AGENTS.md']){const f=zfFixture();f.page('1\talpha');mutate(f);assert.equal(ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context).status,'SCOPE_REJECT');}
});
test('ZF duplicate call/result and incompatible overlapping content fail without conflating ordinary re-read',()=>{
 assert.equal(typeof ze.inspectOwnedReadPages,'function');
 const f=zfFixture();f.page('1\talpha');f.page('1\talpha\n2\t空白\t保留\n3\tend');assert.equal(ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context).status,'MATCH_COMPLETE_TEXT_LINES');
 for(const mutate of [x=>x.frames[2].call_id=x.frames[0].call_id,x=>x.frames[3].call_id=x.frames[1].call_id,x=>x.frames[3].output='1\tALPHA']){const x=zfFixture();x.page('1\talpha');x.page('1\talpha');mutate(x);assert.equal(ze.inspectOwnedReadPages(x.frames,x.snapshot,x.context).status,'SCOPE_REJECT');}
});
test('ZF unfamiliar source, no terminal newline, CR and sentinel-looking source preserve exact text',()=>{
 assert.equal(typeof ze.inspectOwnedReadPages,'function');
 for(const content of ['unfamiliar\n\nlast','a\r\nb\r\n','Tool output is empty.']){const f=zfFixture(content),lines=content.endsWith('\n')?content.slice(0,-1).split('\n'):content.split('\n');f.page(lines.map((s,i)=>(i+1)+'\t'+s).join('\n'));assert.equal(ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context).status,'MATCH_COMPLETE_TEXT_LINES');}
});
test('ZF exact own no-start status and wrapper comment query are observation only',()=>{
 assert.equal(typeof ze.inspectOwnedCaptureCommand,'function');
 const c={issue:zfContext.issue,workspace:zeCtx.workspace,ownedRoots:['/private/tmp/zf-owned']};
 for(const cmd of ['multica issue status '+c.issue+' in_progress --no-start','multica issue status '+c.issue+' in_progress --no-start --output json','multica issue comment list '+c.issue+' --roots-only --summary --compact --output json']){const r=ze.inspectOwnedCaptureCommand(cmd,c);assert.equal(r.status,'MATCH_OWNED_CAPTURE_COMMAND');assert.equal(r.authority,'OBSERVATION_ONLY');assert.equal(r.gateAcceptance,'NOT_ESTABLISHED');}
 const old=ze.inspectOwnedCaptureCommand('multica issue status '+c.issue+' in_progress',c);assert.equal(old.status,'METHOD_UNSUPPORTED');assert.equal(old.reason,'START_NOT_SUPPRESSED');
});
test('ZF public redaction cannot prove source equality or become a role scope defect',()=>{
 const f=zfFixture();f.page('1\t[REDACTED CREDENTIAL]\n2\t空白\t保留\n3\tend');
 const r=ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context);assert.equal(r.status,'PENDING_PUBLIC_REDACTION');assert.equal(r.byteDelivery,'UNVERIFIED_NUMBERED_FRAMING');assert.equal(r.pages[0].redactedLines[0],1);
});
test('ZF existing n_lines pagination stays exact and bounded',()=>{
 const f=zfFixture();f.page('1\talpha\n2\t空白\t保留');f.frames[0].input.n_lines=2;f.page('3\tend',3);f.frames[2].input.n_lines=1;assert.equal(ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context).status,'MATCH_COMPLETE_TEXT_LINES');
 for(const n of [0,1,201,'2']){const x=zfFixture();x.page('1\talpha\n2\t空白\t保留');x.frames[0].input.n_lines=n;assert.equal(ze.inspectOwnedReadPages(x.frames,x.snapshot,x.context).status,'SCOPE_REJECT');}
});
test('ZF flagged truncation in final line prefix or framing LF remains pending, never a role scope defect',()=>{
 for(const output of ['1\talpha\n2','1\talpha\n']){const f=zfFixture();f.page(output,1,true);const r=ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context);assert.equal(r.status,'PENDING_PUBLIC_FRAMING');assert.equal(r.coveredLines,1);assert.equal(r.pages[0].output,output);}
 for(const output of ['1\talpha\n2','1\talpha\n','1\talpha\nforeign']){const f=zfFixture();f.page(output,1,false);assert.equal(ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context).status,'SCOPE_REJECT');}
 const f=zfFixture();f.page('1\talpha\n2\t空白\t保留\n3\tend\n',1,true);assert.equal(ze.inspectOwnedReadPages(f.frames,f.snapshot,f.context).status,'PENDING_PUBLIC_FRAMING');
 const recovered=zfFixture();recovered.page('1\talpha\n2',1,true);recovered.page('1\talpha\n2\t空白\t保留\n3\tend');assert.equal(ze.inspectOwnedReadPages(recovered.frames,recovered.snapshot,recovered.context).status,'MATCH_COMPLETE_TEXT_LINES');
});
test('ZF foreign workspace/issue/private/cwd and arbitrary status/shell remain rejected or unsupported',()=>{
 assert.equal(typeof ze.inspectOwnedCaptureCommand,'function');
 const c={issue:zfContext.issue,workspace:zeCtx.workspace,ownedRoots:['/private/tmp/zf-owned']};
 const own='multica issue status '+c.issue+' in_progress --no-start';
 for(const cmd of [own.replace(c.issue,zeCtx.issue),'multica --workspace-id '+zeCtx.issue+' issue status '+c.issue+' in_progress --no-start',own+'; echo injected',own+' --content-file /Users/user/.multica/sessions/old'])assert.equal(ze.inspectOwnedCaptureCommand(cmd,c).status,'SCOPE_REJECT');
 assert.equal(ze.inspectOwnedCaptureCommand(own,c,'/private/tmp/foreign').status,'SCOPE_REJECT');
 for(const cmd of [own.replace('in_progress','done'),own+' --no-start',own.replace('status','update'),'echo harmless'])assert.equal(ze.inspectOwnedCaptureCommand(cmd,c).status,'METHOD_UNSUPPORTED');
});
