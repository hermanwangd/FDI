// Additional delivery-contract check only; old schema and evidence checkers remain unchanged.
export function checkJSONOnlyFinal(output){
 if(typeof output!=='string')return {status:'FAIL',reason:'missing final text'};
 const trimmed=output.trim();
 const fence=/^```(?:json)?\r?\n([\s\S]*?)\r?\n```$/i.exec(trimmed);
 const candidate=fence?fence[1]:trimmed;
 try{const value=JSON.parse(candidate);return {status:value&&typeof value==='object'&&!Array.isArray(value)?'PASS':'FAIL',reason:value&&typeof value==='object'&&!Array.isArray(value)?'one JSON object with no prose outside':'final JSON is not an object',presentation:fence?'fenced JSON only':'raw JSON only'};}catch{return {status:'FAIL',reason:'non-JSON prose or malformed/multiple JSON outside allowed presentation'};}
}
