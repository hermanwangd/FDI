import fs from 'node:fs';
import crypto from 'node:crypto';
export function checkOutputSchema(output,caseIds){
 const sha256=crypto.createHash('sha256').update(output).digest('hex');
 const blocks=[...output.matchAll(/```json\s*\n([\s\S]*?)\n```/g)];
 if(blocks.length>1)return {status:'FAIL',stage:'JSON_BLOCK_COUNT',blocks:blocks.length,sha256};
 const trimmed=output.trim();
 const afterFirstLine=trimmed.slice(trimmed.indexOf('\n')+1).trim();
 const json=blocks.length===1?blocks[0][1]:trimmed.startsWith('{')?trimmed:afterFirstLine.startsWith('{')?afterFirstLine:null;
 if(json===null)return {status:'FAIL',stage:'JSON_BLOCK_COUNT',blocks:0,sha256};
 let parsed;
 try{parsed=JSON.parse(json);}catch(e){return {status:'FAIL',stage:'JSON_SYNTAX',error:e.message,sha256};}
 const answers=parsed?.answers;
 if(!Array.isArray(answers))return {status:'FAIL',stage:'ANSWERS_ARRAY',sha256};
 const ids=answers.map(x=>x?.id);
 const fields=['id','result','evidenceSource','limitations'];
 const shape=answers.every(a=>a&&typeof a==='object'&&fields.every(k=>typeof a[k]==='string'));
 const exact=ids.length===caseIds.length&&new Set(ids).size===ids.length&&caseIds.every(x=>ids.includes(x));
 return {kind:'REQUESTED_UNIT_OUTPUT_SCHEMA_ONLY',status:shape&&exact?'PASS':'FAIL',stage:'ANSWER_SCHEMA',shape,exactCaseSet:exact,ids,sha256,limits:'Does not grade verdict, independence, first-line format, behavior, full loading or content correctness; never repairs native output'};
}
if(process.argv[1]?.endsWith('/role-unit-output-schema-r1.mjs')){
 const e=JSON.parse(fs.readFileSync(process.argv[2])),s=JSON.parse(fs.readFileSync(process.argv[3]));
 const r=checkOutputSchema(e.subjectRun.result.output,s.cases.filter(c=>c.role===e.role).map(c=>c.id));
 console.log(JSON.stringify(r,null,2));process.exitCode=r.status==='PASS'?0:1;
}
