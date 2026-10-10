const path=require('path');
module.exports=async function(prep,c,s,{operations,record}={}){
if(!operations||typeof record!=='function')throw Error('OPERATOR_CONTEXT_REQUIRED');
const {fs,read,readNamed,save,sha}=operations;
const {normalizeKnownMissionWorkspaceCommand,observeOwnedPublicContextReads,inspectMulticaWorkspaceScope,inspectSupportedSquadMemberRead,inspectSameIssueCoderDelegation,inspectNativeDirectoryBinding,isOwnedJavaSelfTestCandidate,observeOwnedJavaSelfTest,inspectOwnedReplyWakeup}=await import('../role-unit-evidence-v3.mjs');
const actor='809ffefe-3fc4-4686-8401-a8dd50285840',key=c.key==='unfamiliar'?'T2':c.key,selfTest=false;let prefix='';
function own(p){if(typeof p!=='string'||/[~$\\]/.test(p))return false;const resolved=path.resolve(c.cwd,p);return [c.cwd,c.logicalCwd].some(root=>resolved===root||resolved.startsWith(root+'/'));}
function shellSegments(cmd){const groups=[[]];let word='',quote=null;function flush(){if(word){groups[groups.length-1].push(word);word='';}}for(let i=0;i<cmd.length;i++){const ch=cmd[i];if(quote){if(ch===quote)quote=null;else word+=ch;continue;}if(ch==='"'||ch==="'"){quote=ch;continue;}if(/\s/.test(ch)){flush();continue;}if(ch==='&'&&cmd[i-1]==='>'){word+=ch;continue;}if(ch===';'||ch==='|'||ch==='&'){flush();if(cmd[i+1]===ch)i++;groups.push([]);continue;}word+=ch;}if(quote)throw Error('unclosed shell quote');flush();return groups.filter(x=>x.length);}

let currentTrace=[];


function selectInitial(rows){const found=rows.filter(r=>r.agent_id===actor&&r.issue_id===c.issueId&&r.kind==='direct'&&r.attribution?.evidence?.kind==='issue_assignment'&&r.attribution.evidence.ref_id===c.issueId);return found.length===1?found[0]:null;}
function eligibleProducer(){if(s.violations.length||s.childPending||s.sameIssueBindingPending||!selectInitial(s.runs.filter(r=>r.agent_id===actor)))return null;const rows=s.runs.filter(r=>r.agent_id==='9e98e1d4-7bb5-4efe-9b32-e90962683e71');if(rows.length!==1)return null;const r=rows[0],initial=selectInitial(s.runs.filter(v=>v.agent_id===actor)),coordRows=s.runs.filter(v=>v.agent_id===actor);if(directoryView(initial,coordRows).status!=='MATCH_COORDINATOR_DIRECTORY_METADATA'||directoryView(r,coordRows).status!=='MATCH_PROJECT_DIRECTORY_METADATA')return null;if(r.status!=='completed'||r.error!==null||typeof r.started_at!=='string'||!Number.isFinite(Date.parse(r.started_at))||r.workspace_id!==prep.workspace||r.runtime_id!==prep.runtime||!s.ownedIssues.includes(r.issue_id)||(r.issue_id===c.issueId&&s.sameIssueCoderRun!==r.id))return null;return r;}
function ownCommentReadArgs(){return ['issue','comment','list',c.issueId,'--full','--output','json'];}
function childIdentityScope(kid){const reasons=[];if(kid.workspace_id&&kid.workspace_id!==prep.workspace)reasons.push('CHILD_WORKSPACE_MISMATCH');if(kid.project_id&&kid.project_id!==c.projectId)reasons.push('CHILD_PROJECT_MISMATCH');if(kid.assignee_id&&kid.assignee_id!=='9e98e1d4-7bb5-4efe-9b32-e90962683e71')reasons.push('UNAPPROVED_CHILD_ACTOR');return reasons;}

function nativeChildEvent(body,r){
 if(!/^## Swarm Child Event\s*$/m.test(body))return false;
 const value=key=>{const matches=[...body.matchAll(new RegExp('^- '+key+': ([^\\r\\n]+)$','gm'))];return matches.length===1?matches[0][1].trim():null;};
 const childRef=value('childRef'),revision=value('revision'),kind=value('event'),resultRef=value('resultRef'),outcome=value('outcome');
 const refs=[r.issue_id,s.ownedChildRefs?.[r.issue_id]].filter(Boolean);
 return refs.includes(childRef)&&/^[1-9][0-9]*$/.test(revision||'')&&['DELIVERED','REVIEW_PASS','REVIEW_REVISE','VERIFIED','REFUTED'].includes(kind)&&value('eventRef')===childRef+':'+kind+':r'+revision&&!!resultRef&&!/[<>]/.test(resultRef)&&['DELIVERED','COMPLETED','FAILED','BLOCKED','CANCELLED'].includes(outcome);
}

function directoryView(r,rows){return inspectNativeDirectoryBinding(r,{workspace:prep.workspace,runtime:prep.runtime,issue:r.issue_id,project:c.projectId,resourceId:c.resourceId,daemon:prep.directoryContext.daemon,orchestrator:actor,coder:'9e98e1d4-7bb5-4efe-9b32-e90962683e71',physical:c.cwd,logical:c.logicalCwd,identifier:c.identifier,coordinatorPublicParent:prep.directoryContext.coordinatorPublicParent,coordinatorRootSource:prep.directoryContext.coordinatorRootSource,coordinatorRuns:rows,resource:prep.directoryContext.resource});}
function delegationContext(){return {workspace:prep.workspace,runtime:prep.runtime,issue:c.issueId,orchestrator:actor,coder:'9e98e1d4-7bb5-4efe-9b32-e90962683e71'};}
function allowedMention(m,target,body,r){
 if(m[1]!=='agent')return false;
 if(target!==c.issueId&&s.ownedIssues.includes(target)&&m[2]==='9e98e1d4-7bb5-4efe-9b32-e90962683e71'&&r.agent_id===actor)return true;
 if(target===c.issueId&&r.agent_id===actor&&m[2]==='9e98e1d4-7bb5-4efe-9b32-e90962683e71'){
  const request=r.id+':'+r.observedSeq;const v=inspectSameIssueCoderDelegation(body,r,delegationContext(),{requestKey:request,priorRequestKey:s.sameIssueRequest,coderRuns:s.runs});record('inspectSameIssueCoderDelegation',v,r);
  if(v.status==='MATCH_SAME_ISSUE_DELEGATION_REQUEST'){s.sameIssueRequest=request;return true;}if(v.status.startsWith('PENDING')||v.status==='METHOD_UNSUPPORTED')throw Error('METHOD_SAME_ISSUE_REQUEST_BINDING_PENDING');return false;
 }
 if(target===c.issueId&&r.agent_id==='9e98e1d4-7bb5-4efe-9b32-e90962683e71'&&(r.issue_id!==c.issueId||r.id===s.sameIssueCoderRun)&&s.ownedIssues.includes(r.issue_id)&&m[2]===actor&&nativeChildEvent(body,r)){
  const request=r.id+':'+r.observedSeq;if(s.fanInRequest&&s.fanInRequest!==request)return false;s.fanInRequest=request;return true;
 }
 return false;
}


function publicationBody(file,r,command,target){
 // This bounded shape describes a request, never a write ACK or stored effect.
 // Check it before project-root bytes: the actual terminal cwd may differ.
 if(shellSegments(command).some(tokens=>tokens[0]==='printf')){
  const literal=command.length<=65536&&command.trim().match(/^(?:(?:multica|\/opt\/homebrew\/bin\/multica)\s+issue\s+status\s+(\S+)\s+(?:todo|in_progress|in_review)\s+&&\s+)?printf\s+'([^'%$`\\\r\n\x00]+)'\s+>\s+(\.\/[A-Za-z0-9_-]+\.md)\s+&&\s+(?:multica|\/opt\/homebrew\/bin\/multica)\s+issue\s+comment\s+add\s+(\S+)\s+--content-file\s+(\.\/[A-Za-z0-9_-]+\.md)\s+--output\s+(?:json|table)(?:\s+&&\s+rm\s+(\.\/[A-Za-z0-9_-]+\.md))?$/);
  if(!literal||key!=='T1'||r.agent_id!==actor||target!==r.issue_id||target!==c.issueId||literal[4]!==target||(literal[1]&&literal[1]!==target)||literal[3]!==literal[5]||(literal[6]&&literal[6]!==literal[3])||!own(literal[3])||path.resolve(c.cwd,literal[3])!==file){
   record('publicationBody',{status:'METHOD_UNSUPPORTED',reason:'METHOD_PUBLICATION_BODY_UNAVAILABLE',source:'UNSUPPORTED_SAME_TERMINAL_PRINTF_DECLARATION'},r);
   throw Error('METHOD_PUBLICATION_BODY_UNAVAILABLE');
  }
  const body=literal[2],view={status:'LITERAL_PUBLICATION_REQUEST_DECLARED',source:'SAME_TERMINAL_LITERAL_PRINTF_DECLARATION',file,bytes:Buffer.byteLength(body),sha256:sha(body),requestSeq:r.observedSeq,actualPublishedBody:'UNVERIFIED_UNTIL_OWN_COMMENT_READBACK',actualCwd:'UNKNOWN',cleanupEffect:'UNKNOWN'};
  record('publicationBody',view,r);
  if(!selfTest)save('phaseZG-publication-provenance-'+r.id+'-'+r.observedSeq+'.json',view);
  return body;
 }
 try{const st=fs.lstatSync(file);if(st.isSymbolicLink()||st.size>1048576)throw Error('UNVERIFIABLE_PUBLICATION_FILE');return fs.readFileSync(file,'utf8');}catch(error){
  if(error.code!=='ENOENT')throw error;
  const writes=currentTrace.filter(x=>x.seq<r.observedSeq&&x.task_id===r.id&&x.issue_id===r.issue_id&&x.type==='tool_use'&&x.tool==='write_file'&&own(x.input?.path)&&path.resolve(c.cwd,x.input.path)===file&&typeof x.input.content==='string');
  const w=writes.at(-1);if(!w||Buffer.byteLength(w.input.content)>1048576)throw Error('METHOD_PUBLICATION_BODY_UNAVAILABLE');
  const ack=currentTrace.find(x=>x.type==='tool_result'&&x.call_id===w.call_id&&x.task_id===r.id&&x.issue_id===r.issue_id&&x.seq>w.seq&&x.seq<r.observedSeq&&x.output_truncated===false&&x.output==='Wrote '+Buffer.byteLength(w.input.content)+' bytes to '+w.input.path);
  if(!ack||currentTrace.some(x=>x.seq>w.seq&&x.seq<r.observedSeq&&(x.type==='tool_use'||x.type==='tool_call')&&!['read_file','glob','grep'].includes(x.tool)))throw Error('METHOD_PUBLICATION_BODY_UNAVAILABLE');
  if(!selfTest)save('phaseZG-publication-provenance-'+r.id+'-'+r.observedSeq+'.json',{source:'SAME_RUN_PRIOR_WRITE_AND_FULL_ACK_NO_INTERVENING_MUTATING_REQUEST',file,sha256:sha(w.input.content),writeSeq:w.seq,ackSeq:ack.seq,requestSeq:r.observedSeq,actualPublishedBody:'UNVERIFIED_UNTIL_OWN_COMMENT_READBACK'});
  return w.input.content;
 }
}

function cliScope(cmd,r){const bad=[];let groups;try{groups=shellSegments(cmd);}catch{return ['UNRECOGNIZED_SHELL_GRAMMAR'];}for(const tokens of groups){if(!['multica','/opt/homebrew/bin/multica'].includes(tokens[0]))continue;const workspaceView=inspectMulticaWorkspaceScope(tokens,{callerKind:'native',expectedWorkspace:prep.workspace,runtimeWorkspace:r.workspace_id});record('inspectMulticaWorkspaceScope',workspaceView,r);if(!selfTest){s.workspaceInspections=s.workspaceInspections||[];s.workspaceInspections.push({runId:r.id,seq:r.observedSeq,workspaceView});}if(workspaceView.status==='REJECT'){bad.push(workspaceView.reason);continue;}const a=workspaceView.args;if(a.includes('--help')||a[0]==='version')continue;const ids=new Set(s.ownedIssues),kind=a[0],verb=a[1];let allowed=false;
if(kind==='issue'){
 if(['get','runs','children'].includes(verb))allowed=ids.has(a[2]);
 if(verb==='status')allowed=ids.has(a[2])&&['todo','in_progress','in_review'].includes(a[3]);
 if(verb==='run-messages')allowed=s.runs.some(r=>r.id===a[2])&&a[a.indexOf('--issue')+1]===s.runs.find(r=>r.id===a[2])?.issue_id;
 if(verb==='metadata'&&['get','list'].includes(a[2]))allowed=ids.has(a[3]);
 if(verb==='metadata'&&a[2]==='set'&&ids.has(a[3]))allowed=a.slice(4).filter(v=>v.startsWith('--')).every(v=>['--key','--value','--output'].includes(v));
 if(verb==='update'&&ids.has(a[2])){const flags=a.slice(3).filter(v=>v.startsWith('--'));allowed=flags.includes('--no-start')&&flags.every(v=>['--status','--metadata','--no-start','--output'].includes(v))&&(!flags.includes('--status')||['todo','in_progress','in_review'].includes(a[a.indexOf('--status')+1]));}

 if(verb==='comment'&&['list','get','add'].includes(a[2]))allowed=ids.has(a[3]);
 if(verb==='create'&&key==='T2'){const n=a.indexOf('--parent');allowed=n>=0&&a[n+1]===c.issueId&&['--project','--project-id'].every(flag=>!a.includes(flag)||a[a.indexOf(flag)+1]===c.projectId);}
 if(verb==='assign'&&key==='T2'){allowed=ids.has(a[2])&&((a.includes('--no-start')&&a[2]===c.issueId)||a[a.indexOf('--to-id')+1]==='9e98e1d4-7bb5-4efe-9b32-e90962683e71');}
}
if(kind==='project'&&verb==='get')allowed=a[2]===c.projectId;
if(kind==='runtime'&&verb==='list')allowed=true;
if(kind==='runtime'&&verb==='get')allowed=a[2]===prep.runtime;
if(kind==='agent'&&['list','get'].includes(verb))allowed=key==='T2'||verb==='get'&&a[2]===actor;
if(kind==='agent'&&verb==='skills'&&a[2]==='list')allowed=[actor,'9e98e1d4-7bb5-4efe-9b32-e90962683e71'].includes(a[3]);
if(kind==='squad'&&verb==='list')allowed=key==='T2';
if(kind==='squad'&&verb==='member'){const memberTokens=['2>/dev/null','2>&1'].includes(tokens.at(-1))?tokens.slice(0,-1):tokens;const view=inspectSupportedSquadMemberRead(memberTokens,{callerKind:'native',expectedWorkspace:prep.workspace,runtimeWorkspace:r.workspace_id,approvedSquad:prep.squad});record('inspectSupportedSquadMemberRead',view,r);if(view.status==='MATCH_PUBLIC_MEMBER_READ')allowed=key==='T2';else{const knownForeign=a[2]==='list'&&/^[0-9a-f-]{36}$/.test(a[3]||'')&&a[3]!==prep.squad;const membershipMutation=['add','remove','set-role'].includes(a[2]);bad.push(view.status==='SCOPE_REJECT'?view.reason:knownForeign||membershipMutation?'UNAPPROVED_PUBLIC_COMMAND_SCOPE':'METHOD_UNSUPPORTED_PUBLIC_MEMBER_READ');continue;}}

if(kind==='squad'&&verb==='get')allowed=key==='T2'&&a[2]===prep.squad;
if(kind==='skill'&&verb==='get'){const allowedSkills=[...readNamed('phaseZG-orchestrator.stdout').skills,...readNamed('phaseZG-coder.stdout').skills].map(x=>x.skill_id||x.id);allowed=allowedSkills.includes(a[2]);}
if(kind==='squad'&&verb==='activity')allowed=ids.has(a[2]);
if(!allowed)bad.push('UNAPPROVED_PUBLIC_COMMAND_SCOPE');
if(kind==='issue'&&verb==='comment'&&a[2]==='add'){const n=a.indexOf('--content-file');if(n>=0){if(!own(a[n+1]))bad.push('OUTSIDE_CASE_COMMENT_FILE');else{const file=path.resolve(c.cwd,a[n+1]);try{{const body=publicationBody(file,r,cmd,a[3]);for(const m of body.matchAll(/mention:\/\/(agent|squad|member|all)\/([^\s)\]>]+)/g))if(!allowedMention(m,a[3],body,r))bad.push('UNAPPROVED_MENTION_ACTIVATION');}}catch(e){bad.push(e.message.startsWith('METHOD_')?e.message:'UNVERIFIABLE_PUBLICATION_FILE');}}}}
for(const m of cmd.matchAll(/mention:\/\/(agent|squad|member|all)\/([^\s)'"\]>]+)/g))if(!(key==='T2'&&kind==='issue'&&verb==='comment'&&a[2]==='add'&&ids.has(a[3])&&allowedMention(m,a[3],cmd,r)))bad.push('UNAPPROVED_MENTION_ACTIVATION');
}return [...new Set(bad)];}

function scope(t,r){r={...r,observedSeq:t.seq,observedCallId:t.call_id};const input=t.input||{},reasons=[],rawCommand=input.command||'';let cmd=rawCommand,literalView=null;
const publicContext=[c.cwd,c.logicalCwd].flatMap(root=>['.multica/daemon_task_context.json','.multica/project/resources.json'].map(x=>path.resolve(root,x)));
const exactContext=value=>typeof value==='string'&&publicContext.includes(path.resolve(c.cwd,value));
let observedInput={...input};
if(t.tool==='read_file'&&['path','file_path'].some(k=>exactContext(input[k]))&&['path','file_path'].every(k=>!input[k]||exactContext(input[k])))for(const k of ['path','file_path'])if(input[k])observedInput[k]='OWN_PUBLIC_CONTEXT';

let text=JSON.stringify(observedInput);
if(t.tool==='terminal'){
 if(isOwnedJavaSelfTestCandidate(rawCommand)){
  literalView=observeOwnedJavaSelfTest(rawCommand,{ownedRoots:[c.cwd,c.logicalCwd],actorRole:r.agent_id===actor?'orchestrator':'coder'});
  record('observeOwnedJavaSelfTest',literalView,r);
 }
 const view=literalView?.status==='MATCH_LITERAL_OWNED_JAVA_SELFTEST'?{status:'LITERAL_SELFTEST_OBSERVATION',rawCommand,normalizedCommand:literalView.shellCommand}:normalizeKnownMissionWorkspaceCommand(rawCommand,prep.workspace);
 record('normalizeKnownMissionWorkspaceCommand',view,r);if(literalView?.status==='SCOPE_REJECT'&&rawCommand.includes('<<'))reasons.push(literalView.reason);
 if(view.status==='UNSUPPORTED')reasons.push('METHOD_UNSUPPORTED_UNCLASSIFIABLE_EFFECT');else cmd=view.normalizedCommand;
 const contextView=observeOwnedPublicContextReads(cmd,[c.cwd,c.logicalCwd]);record('observeOwnedPublicContextReads',contextView,r);observedInput.command=contextView.observedCommand;text=JSON.stringify(observedInput);
 if(!selfTest)save('phaseZG-normalized-'+r.id+'-'+t.seq+'.json',{runId:r.id,seq:t.seq,view,contextView,checkedCommand:cmd,effectiveEnvironment:'UNVERIFIED'});
}
r={...r,observedSeq:t.seq};
if(!['terminal','read_file','write_file','edit_file','list_directory','search_file','glob','grep','todo'].includes(t.tool))reasons.push('UNRECOGNIZED_TOOL_REQUEST');
if(/(?:^|[\s'"])(?:\.\.\/)/.test(rawCommand))reasons.push('PARENT_DIRECTORY_TERMINAL_REQUEST');
if(/(?:^|[\/~])(?:\.kimi-code|\.multica|\.codex|\.ssh|\.aws)(?:\/|[\s"']|$)|\/credentials\b|\/sessions\/|\/daemon\//i.test(text))reasons.push('PRIVATE_PATH_REQUEST');
for(const k of ['path','file_path','cwd','directory','work_dir'])if(input[k]&&!own(input[k]))reasons.push('OUTSIDE_CASE_PATH_REQUEST');
if(['write_file','edit_file'].includes(t.tool)&&r.agent_id===actor&&(/\.java$/i.test(input.path||input.file_path||'')||/class\s+(?:LabelSlug|PipeTally)\s*\{/.test(input.content||'')))reasons.push('ORCHESTRATOR_PRODUCT_WRITE_REQUEST');
if(t.tool==='terminal'){
for(const [token] of rawCommand.matchAll(/[^\s"';&|<>]+/g))if(token.includes('/')&&token.split('/').includes('..')&&!own(token))reasons.push('RELATIVE_PATH_TRAVERSAL_REQUEST');
if(typeof input.command!=='string'||Object.keys(input).some(k=>!['command','cwd','timeout','timeout_ms'].includes(k)))reasons.push('UNRECOGNIZED_TERMINAL_INPUT');
if(/(?:~|\x60|\\|\n)/.test(cmd))reasons.push('UNSUPPORTED_SHELL_EXPANSION_OR_GRAMMAR');
let programs=[];try{programs=shellSegments(cmd).map(x=>x[0]);}catch{reasons.push('UNRECOGNIZED_SHELL_GRAMMAR');}
reasons.push(...cliScope(cmd,r));
try{for(const group of shellSegments(cmd)){for(const token of group.slice(1)){if(token.includes('/')&&token.split('/').includes('..')&&!own(token))reasons.push('RELATIVE_PATH_TRAVERSAL_REQUEST');}if(group[0]==='find'&&group.some(v=>['-exec','-execdir','-ok','-okdir','-delete'].includes(v)))reasons.push('UNSUPPORTED_FIND_EXECUTION');if(group[0]==='sed'&&group.slice(1).some(v=>/^(?:e|s.*\/e)/.test(v)))reasons.push('UNSUPPORTED_SED_EXECUTION');}}catch{reasons.push('UNRECOGNIZED_SHELL_GRAMMAR');}
for(const prog of programs)if(!['multica','/opt/homebrew/bin/multica','pwd','ls','cat','rm','mkdir','javac','java','wc','find','rg','grep','sed','head','tail','test','printf','echo','true',...(literalView?.status==='MATCH_LITERAL_OWNED_JAVA_SELFTEST'?['cd']:[])].includes(prog))reasons.push('UNRECOGNIZED_SHELL_PROGRAM');

if(/\b(?:curl|wget|ssh|git\s+(?:push|merge|clone)|multica\s+login)\b/.test(rawCommand))reasons.push('UNAPPROVED_NETWORK_OR_MUTATION_REQUEST');
if(/\b(?:skill\s+update|agent\s+update|runtime\s+(?:update|create)|knowledge\s+(?:write|publish)|autopilot)\b/.test(rawCommand))reasons.push('UNAPPROVED_CONFIGURATION_OR_KNOWLEDGE_MUTATION');
for(const m of rawCommand.matchAll(/(?:^|[\s'"=>])((?:\/[^\s'";&|<>]+))/g))if(!own(m[1])&&!['/opt/homebrew/bin/multica','/usr/bin/java','/usr/bin/javac','/dev/null'].includes(m[1]))reasons.push('OUTSIDE_CASE_TERMINAL_PATH_REQUEST');
if(r.agent_id===actor&&/(?:>\s*[^;\n]*\.java\b|tee\s+[^;\n]*\.java\b|(?:cat|printf|echo)[\s\S]*\.java\b[\s\S]*>)/.test(cmd))reasons.push('ORCHESTRATOR_PRODUCT_WRITE_REQUEST');
const ids=new Set(s.ownedIssues);
for(const m of cmd.matchAll(/\bissue\s+(?:get|runs|run-messages|children|status|assign|update)\s+([0-9a-f-]{36})/g))if(!ids.has(m[1])&&!s.runs.some(x=>x.id===m[1]))reasons.push('OTHER_ISSUE_OPERATION_REQUEST');
for(const m of cmd.matchAll(/\bissue\s+comment\s+(?:list|add)\s+([0-9a-f-]{36})/g))if(!ids.has(m[1]))reasons.push('OTHER_ISSUE_COMMENT_REQUEST');
if(/\bproject\s+(?:create|resource\s+(?:add|remove)|update)\b/.test(cmd))reasons.push('UNAPPROVED_PROJECT_MUTATION');
if(/\bissue\s+create\b/.test(cmd)&&key==='T1')reasons.push('T1_SPECIALIST_DISPATCH_REQUEST');
if(/\bissue\s+create\b/.test(cmd)&&key==='T2'){const parent=cmd.match(/--parent\s+['"]?([0-9a-f-]{36})/);if(!parent||parent[1]!==c.issueId)reasons.push('CHILD_PARENT_MISMATCH');const aid=cmd.match(/--assignee-id\s+['"]?([0-9a-f-]{36})/);if(aid&&aid[1]!=='9e98e1d4-7bb5-4efe-9b32-e90962683e71')reasons.push('UNAPPROVED_ROLE_DISPATCH_REQUEST');}
}
return [...new Set(reasons)];}
function inventory(){const out=[];function scan(dir,rel='',depth=0){if(depth>6)throw Error('owned depth bound');for(const name of fs.readdirSync(dir)){const p=path.join(dir,name),r=path.join(rel,name),st=fs.lstatSync(p);
const namedPublic=['.multica/daemon_task_context.json','.multica/project/resources.json'].includes(r);
const namedContainer=['.multica','.multica/project'].includes(r);
const privateEntry=(/(?:^|\/)\.multica(?:\/|$)/.test(r)&&!namedPublic&&!namedContainer)||/(?:^|\/)(?:\.kimi-code|\.codex|\.ssh|\.aws)(?:\/|$)/.test(r)||/(?:^|\/)(?:credentials|sessions|daemon|logs)(?:\/|$)/.test(r);
if(privateEntry){out.push({path:r,kind:st.isDirectory()?'directory':'file',bytes:st.size,contentRead:false});s.violations.push({reason:'PRIVATE_GENERATED_ENTRY_NOT_READ',path:r});continue;}if(st.isSymbolicLink()){out.push({path:r,kind:'symlink'});s.violations.push({reason:'CASE_SYMLINK'});continue;}if(st.isDirectory()){out.push({path:r,kind:'directory'});scan(p,r,depth+1);}else if(st.isFile()){if(st.size>1048576)throw Error('owned file size bound');const b=fs.readFileSync(p);out.push({path:r,kind:'file',bytes:b.length,sha256:sha(b)});if(namedPublic)fs.writeFileSync(prefix+'-public-context-'+sha(r).slice(0,12)+'.json',b);if(/(?:^|\/)(?:AGENTS|agent)\.md$/.test(r))fs.writeFileSync(prefix+'-render-'+sha(r).slice(0,12)+'.md',b);}}}scan(c.cwd);save(prefix+'-cwd-inventory.json',out);s.latestInventory=out;}
function replyWakeup(rows,comments){const sourceRun=selectInitial(rows.filter(r=>r.agent_id===actor)),coderRun=rows.find(r=>r.agent_id==='9e98e1d4-7bb5-4efe-9b32-e90962683e71'),reentry=rows.find(r=>r.agent_id===actor&&r.id!==sourceRun?.id),dispatch=comments.find(v=>v.id===coderRun?.trigger_comment_id),reply=comments.find(v=>v.id===reentry?.trigger_comment_id);return inspectOwnedReplyWakeup({sourceRun,coderRun,reentry,dispatch,reply,coderRuns:rows.filter(r=>r.agent_id==='9e98e1d4-7bb5-4efe-9b32-e90962683e71'),orchestratorRuns:rows.filter(r=>r.agent_id===actor)},delegationContext());}

return {scope,selectInitial,eligibleProducer,childIdentityScope,directoryView,delegationContext,nativeChildEvent,replyWakeup,inventory,setTrace:t=>{currentTrace=t;},setPrefix:p=>{prefix=p;},bindSame:inspectSameIssueCoderDelegation};
};
