import fs from 'node:fs';import path from 'node:path';
export function checkFreshBody(bodyPath,publicCwd,pinnedRealCwd){
 if(!path.isAbsolute(bodyPath)||path.dirname(bodyPath)!==publicCwd)throw Error('body outside exact public cwd');
 const real=fs.realpathSync(publicCwd);if(real!==pinnedRealCwd||!fs.statSync(publicCwd).isDirectory())throw Error('existing public cwd realpath/type drift');
 try{fs.lstatSync(bodyPath);}catch(e){if(e.code==='ENOENT')return {status:'PASS',entry:'ABSENT_LSTAT_ENOENT',publicCwd,realCwd:real};throw e;}
 throw Error('body already has an entry, including dangling symlink; no overwrite');
}
export function publicRunCwdBinding(run,publicCwd){const actual=run?.work_dir??run?.result?.work_dir??null;return {status:actual===null?'UNVERIFIED':actual===publicCwd?'MATCH':'MISMATCH',actual,expected:publicCwd,limits:'Project resource alone is configuration; missing public run cwd is not assumed. No private cwd reads or alternate directory.'};}
