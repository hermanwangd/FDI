const path=require('path');
module.exports=async function(c,s,{operations,record}={}){
if(!operations||typeof record!=='function')throw Error('OPERATOR_CONTEXT_REQUIRED');
const {fs,read,readNamed,save,pin,sha,ws,runtime}=operations;
const {inspectOwnedReadPages,inspectOwnedCaptureCommand}=await import('../role-unit-evidence-v3.mjs');
const key=c.key;
function ownedFile(file){const rel=path.relative(c.cwd,file);if(rel.startsWith('..')||path.isAbsolute(rel))throw Error('path grant');let at=c.cwd;for(const part of rel.split('/')){at=path.join(at,part);if(!fs.existsSync(at))return false;if(fs.lstatSync(at).isSymbolicLink())throw Error('SYMLINK_NOT_READ');}return true;}
function snapshots(){for(const f of [...c.installed,...['.multica/daemon_task_context.json','.multica/project/resources.json'].map(x=>({path:c.cwd+'/'+x,kind:'publicContext'}))]){if(!ownedFile(f.path))continue;const st=fs.statSync(f.path);if(!st.isFile()||st.size>262144)throw Error('NAMED_FILE_SIZE_OR_KIND');const b=fs.readFileSync(f.path),h=sha(b),file='phaseZG-'+key+'-captured-'+sha(f.path).slice(0,12)+'-'+h.slice(0,12)+'.bytes';if(!fs.existsSync(file))fs.writeFileSync(file,b);s.snapshots[f.path]={kind:f.kind,source:f.path,sha256:h,bytes:b.length,at:new Date().toISOString(),file};if(f.kind==='render'){const a=readNamed('phaseZG-coder.stdout');if(!b.includes(Buffer.from(a.instructions)))s.violations.push('ROLE_BODY_NOT_PRESENT');}else if(['assignedSkill','attachment'].includes(f.kind)&&h!==f.sha256)s.violations.push('INSTALLED_ASSIGNED_BYTES_MISMATCH');}}
function requestScope(t,r){const i=t.input||{},file=i.path||i.file_path;
 if(t.tool==='read_file'){if(!c.installed.some(f=>f.path===file)||i.path&&i.file_path&&i.path!==i.file_path)return 'OUTSIDE_NAMED_ABSOLUTE_READ';if(Object.keys(i).some(k=>!['path','file_path','line_offset','n_lines','max_chars'].includes(k))||i.line_offset!=null&&(!Number.isSafeInteger(i.line_offset)||i.line_offset<1)||i.n_lines!=null&&(!Number.isSafeInteger(i.n_lines)||i.n_lines<1||i.n_lines>200)||i.max_chars!=null&&(!Number.isSafeInteger(i.max_chars)||i.max_chars<1||i.max_chars>65536))return 'UNSUPPORTED_READ_PAGE_GRAMMAR';return null;}
 if(t.tool==='write_file'){
   if(i.path&&i.file_path&&i.path!==i.file_path)return 'CONFLICTING_WRITE_PATHS';
   if(![c.cwd+'/loading-reply.md',c.logicalCwd+'/loading-reply.md'].includes(file))return 'NON_COORDINATION_WRITE';
   s.coordinationWrites=(s.coordinationWrites||0)+1;if(s.coordinationWrites>1)return 'EXCESS_COORDINATION_WRITE';
   return /mention:\/\//.test(i.content||'')?'MENTION_DISPATCH_PROHIBITED':null;
 }
 if(t.tool==='todo')return null;
 if(t.tool!=='terminal')return 'UNSUPPORTED_CAPTURE_TOOL';
 const cmd=i.command;if(typeof cmd!=='string'||Object.keys(i).some(k=>!['command','cwd','timeout','timeout_ms'].includes(k))||/[\r\n\x00`$\\~]/.test(cmd))return 'UNSUPPORTED_CAPTURE_SHELL';
 if(i.cwd!=null&&![c.cwd,c.logicalCwd].includes(i.cwd))return 'OUTSIDE_TERMINAL_CWD';
 if(/\/sessions\/|\/daemon\/|\/credentials|\.ssh|\.codex|\.kimi-code|\.aws/.test(cmd))return 'PRIVATE_REQUEST';
 const command=cmd.trim().replace(/^\/opt\/homebrew\/bin\/multica\b/,'multica').replace(/^multica --workspace-id ["']?([0-9a-f-]{36})["']? /,(all,id)=>id===ws?'multica ':'FOREIGN_WORKSPACE ');
 const escaped=c.issue.replace(/[.*+?^${}()|[\]\\]/g,'\\$&');
 if(new RegExp('^multica issue (?:get|runs) '+escaped+' --output json$').test(command))return null;
 if(new RegExp('^multica issue comment list '+escaped+'(?: --full)? --output json$').test(command))return null;
 const normal=inspectOwnedCaptureCommand(cmd,{issue:c.issue,workspace:ws,ownedRoots:[c.cwd,c.logicalCwd]},i.cwd);record('inspectOwnedCaptureCommand',normal,{...r,observedSeq:t.seq,observedCallId:t.call_id});
 if(normal.status==='MATCH_OWNED_CAPTURE_COMMAND'){if(normal.kind==='OWN_IN_PROGRESS_NO_START'){s.progressRequests=(s.progressRequests||0)+1;if(s.progressRequests>1)return 'EXCESS_PROGRESS_STATUS';}return null;}
 if(normal.status==='SCOPE_REJECT')return normal.reason;
 if(normal.reason==='START_NOT_SUPPRESSED')return 'METHOD_START_NOT_SUPPRESSED';
 if(new RegExp('^multica issue update '+escaped+' --status in_review --no-start --output json$').test(command)){s.statusRequests=(s.statusRequests||0)+1;return s.statusRequests===1?null:'EXCESS_STATUS_REQUEST';}
 const prefix='multica issue comment add '+c.issue+' --content-file ';
 if(command.startsWith(prefix)){
   const operand=command.slice(prefix.length).replace(/ --output json$/,'').replace(/^['"]|['"]$/g,'');if(![c.cwd+'/loading-reply.md',c.logicalCwd+'/loading-reply.md'].includes(operand))return 'UNSUPPORTED_OWN_REPLY_PATH';
   s.replyRequests=(s.replyRequests||0)+1;return s.replyRequests===1?null:'EXCESS_REPLY_REQUEST';
 }
 return 'UNSUPPORTED_CAPTURE_TRANSPORT';
}

return {scope:(t,r)=>{const reason=requestScope(t,r);return reason?[reason]:[];},snapshot:snapshots,deliveries:trace=>c.installed.map(f=>{const snap=s.snapshots[f.path];if(!snap)return {path:f.path,status:'PENDING_SOURCE_SNAPSHOT'};return {path:f.path,installed:snap,...inspectOwnedReadPages(trace,{path:f.path,content:fs.readFileSync(snap.file,'utf8'),bytes:snap.bytes,sha256:snap.sha256,at:snap.at},{run:s.subjectRunId,issue:c.issue,ownedPaths:c.installed.map(x=>x.path),startedAt:s.runs[0].started_at,sourceSha256:f.sha256||snap.sha256})};})};
};
