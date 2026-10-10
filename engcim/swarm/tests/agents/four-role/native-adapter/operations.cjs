'use strict';
const realFs=require('fs'),childProcess=require('child_process'),crypto=require('crypto'),path=require('path');

// Existing external operator, with an explicit subject-bound evidence context.
// Creating/importing this module never invokes Multica.
function createOperations(context,injected={}){
 if(!context||typeof context.evidenceRoot!=='string'||!path.isAbsolute(context.evidenceRoot)||path.resolve(context.evidenceRoot)!==context.evidenceRoot||['workspace','runtime','caller'].some(k=>typeof context[k]!=='string'||!context[k]))throw Error('EXPLICIT_OPERATOR_CONTEXT_REQUIRED');
 if(/(?:^|\/)(?:\.multica|\.ssh|\.aws|\.codex|\.kimi-code|sessions|daemon|credentials)(?:\/|$)/.test(context.evidenceRoot))throw Error('PRIVATE_EVIDENCE_ROOT_PROHIBITED');
 const evidenceRoot=context.evidenceRoot,rawFs=injected.fs||realFs,cp=injected.childProcess||childProcess;
 const rootStat=rawFs.lstatSync(evidenceRoot);
 if(!rootStat.isDirectory()||rootStat.isSymbolicLink()||rawFs.realpathSync(evidenceRoot)!==evidenceRoot)throw Error('PHYSICAL_EVIDENCE_ROOT_REQUIRED');
 const ws=context.workspace,runtime=context.runtime,caller=context.caller,caseKey=context.caseKey;
 function evidencePath(p){
  if(typeof p!=='string')throw Error('INVALID_EVIDENCE_PATH');
  // Named public actor/source paths retain their original guards. Absolute paths
  // inside the evidence root must receive the same symlink check as relative ones.
  if(path.isAbsolute(p)&&p!==evidenceRoot&&!p.startsWith(evidenceRoot+path.sep))return p;
  const resolved=path.resolve(evidenceRoot,p);
  if(resolved!==evidenceRoot&&!resolved.startsWith(evidenceRoot+path.sep))throw Error('EVIDENCE_PATH_ESCAPE');
  let current=evidenceRoot;
  for(const segment of path.relative(evidenceRoot,resolved).split(path.sep).filter(Boolean)){
   current=path.join(current,segment);if(rawFs.existsSync(current)&&rawFs.lstatSync(current).isSymbolicLink())throw Error('EVIDENCE_SYMLINK_PROHIBITED');
  }
  return resolved;
 }
 const pathMethods=new Set(['readFileSync','writeFileSync','existsSync','lstatSync','statSync','readdirSync','realpathSync']);
 const fs=new Proxy(rawFs,{get(target,k){const v=target[k];return typeof v==='function'?(pathMethods.has(k)?(p,...a)=>v.call(target,evidencePath(p),...a):v.bind(target)):v;}});
 const read=p=>JSON.parse(fs.readFileSync(p)),save=(p,x)=>fs.writeFileSync(p,JSON.stringify(x,null,2)+'\n'),sha=x=>crypto.createHash('sha256').update(x).digest('hex');
 const pin=p=>{const b=fs.readFileSync(p);return {path:p,bytes:b.length,sha256:sha(b)};};
 function assertPin(f){
  const resolved=evidencePath(f.path),providerRoot=path.resolve(__dirname,'..');
  if(![evidenceRoot,providerRoot].some(root=>resolved.startsWith(root+path.sep)))throw Error('DEPENDENCY_PATH_OUTSIDE_OWNED_SOURCE');
  if(fs.lstatSync(f.path).isSymbolicLink())throw Error('DEPENDENCY_SYMLINK_PROHIBITED');
  if(fs.realpathSync(f.path)!==resolved)throw Error('DEPENDENCY_PHYSICAL_PATH_REQUIRED');
  const b=fs.readFileSync(f.path);if(b.length!==f.bytes||sha(b)!==f.sha256)throw Error('changed pin '+f.path);return b;
 }
 function assertNamedDependency(a,name){
  const required=path.resolve(evidenceRoot,name),matches=a.dependencies.filter(p=>path.resolve(evidenceRoot,p.path)===required);
  if(matches.length!==1)throw Error('NAMED_DEPENDENCY_PIN_REQUIRED '+name);
  return assertPin(matches[0]);
 }
 function readNamed(name){return JSON.parse(assertNamedDependency(admission(),name));}
 let admittedSubjectSha256=null;
 function admission(){
  const subjectBytes=fs.readFileSync('PHASE-ZG-EXECUTION-SUBJECT.json'),subjectSha256=sha(subjectBytes),a=JSON.parse(subjectBytes),r=read('PHASE-ZG-EXECUTION-REVIEW.json');
  if(admittedSubjectSha256!==null&&subjectSha256!==admittedSubjectSha256)throw Error('ADMITTED_SUBJECT_CHANGED');
  if(r.verdict!=='APPROVE_EXACT_NATIVE_EXECUTION'||r.executionSubjectSha256!==subjectSha256)throw Error('no exact admission');
  if(a.evidenceRoot!==evidenceRoot||a.workspace!==ws||a.runtime!==runtime||a.caller!==caller)throw Error('SUBJECT_CONTEXT_MISMATCH');
  if(!Array.isArray(a.dependencies))throw Error('DEPENDENCY_PINS_REQUIRED');for(const p of a.dependencies)assertPin(p);
  if(caseKey==='unfamiliar'){
   const name='PHASE-ZG-UNFAMILIAR-READINESS.json',f=r.unfamiliarReadiness;
   if(!f||path.resolve(evidenceRoot,f.path)!==path.resolve(evidenceRoot,name))throw Error('READINESS_REVIEW_PIN_REQUIRED');
   const d=JSON.parse(assertPin(f));
   if(d.verdict!=='APPROVE_CURRENT_UNFAMILIAR'||d.executionSubjectSha256!==r.executionSubjectSha256)throw Error('unfamiliar readiness absent');
  }
  admittedSubjectSha256=subjectSha256;return a;
 }
 async function op(label,args,kind='prepReads',caseKey=null,timeoutMs=20000){
  if(!Number.isSafeInteger(timeoutMs)||timeoutMs<1||timeoutMs>20000)throw Error('INVALID_OPERATION_TIME_BOUND');
  if(typeof label!=='string'||!/^[-a-zA-Z0-9_]+$/.test(label)||!Array.isArray(args)||args.some(x=>typeof x!=='string')||args.some(x=>x==='--workspace-id'||x.startsWith('--workspace-id=')))throw Error('INVALID_OPERATION_SCOPE');
  const a=admission();
  const supplementName='PHASE-ZG-DISPATCH-WAIT-PREPARATION-REVIEW.json';
  const supplement=fs.existsSync(supplementName)&&JSON.parse(assertNamedDependency(a,supplementName)).verdict==='APPROVE_PREPARATION_ONLY';
  const limits=supplement?{prepReads:35,prepMutations:15}:{prepReads:32,prepMutations:12},p='PHASE-ZG-BUDGET.json',b=fs.existsSync(p)?read(p):{limits,used:{},localAdapterReads:6};
  const activeLimits=Object.hasOwn(limits,kind)?limits:a.operationBudgets;
  if(!activeLimits||!Object.hasOwn(activeLimits,kind)||!Number.isSafeInteger(activeLimits[kind]))throw Error('unknown budget');
  if(fs.existsSync(label+'.operation.json'))throw Error('operation already attempted');if((b.used[kind]||0)+1>activeLimits[kind])throw Error('budget exhausted '+kind);
  b.used[kind]=(b.used[kind]||0)+1;save(p,b);
  const argv=['--workspace-id',ws,...args],at=new Date().toISOString(),claim={state:'IN_FLIGHT',argv,caller,kind,caseKey,at,timeoutMs};
  fs.writeFileSync(label+'.operation.json',JSON.stringify(claim,null,2)+'\n',{flag:'wx'});
  return await new Promise((resolve,reject)=>cp.execFile('/opt/homebrew/bin/multica',argv,{cwd:evidenceRoot,encoding:'utf8',timeout:timeoutMs,maxBuffer:2097152},(error,stdout,stderr)=>{
   fs.writeFileSync(label+'.stdout',stdout||'');fs.writeFileSync(label+'.stderr',stderr||'');save(label+'.operation.json',{...claim,state:'RETURNED',finishedAt:new Date().toISOString(),exit:error?.code||0,error:error?.message||null});
   if(error)return reject(Error('supported CLI failed '+label+' '+error.code));try{resolve(JSON.parse(stdout));}catch{resolve(stdout);}
  }));
 }
 return {fs,evidenceRoot,op,admission,read,readNamed,save,sha,pin,assertPin,assertNamedDependency,ws,runtime,caller};
}
module.exports={createOperations};
