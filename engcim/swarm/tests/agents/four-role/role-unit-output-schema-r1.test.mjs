import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {checkOutputSchema} from './role-unit-output-schema-r1.mjs';
const wrap=x=>'判定：N/A（流程）\n```json\n'+JSON.stringify(x)+'\n```';
const answer={id:'a',result:'String with "quote" and newline\n.',evidenceSource:'fixture',limitations:'not executed'};
test('valid encoded strings and exact independent item set',()=>assert.equal(checkOutputSchema(wrap({answers:[answer]}),['a']).status,'PASS'));
test('requested JSON does not require an invented fenced-block format',()=>{
 const plain=JSON.stringify({answers:[answer]});
 assert.equal(checkOutputSchema(plain,['a']).status,'PASS');
 assert.equal(checkOutputSchema('判定：N/A（流程）\n'+plain,['a']).status,'PASS');
});
test('duplicates, omitted/extra cases and non-string result rejected',()=>{
 for(const [a,ids]of [[[answer,answer],['a']],[[answer],['a','b']],[[answer,{...answer,id:'b'}],['a']],[[{...answer,result:42}],['a']]])assert.equal(checkOutputSchema(wrap({answers:a}),ids).status,'FAIL');
});
test('ambiguous/missing JSON blocks rejected without guessing',()=>{
 assert.equal(checkOutputSchema('No structured answers',['a']).stage,'JSON_BLOCK_COUNT');
 assert.equal(checkOutputSchema(wrap({answers:[answer]})+'\n'+wrap({answers:[answer]}),['a']).stage,'JSON_BLOCK_COUNT');
});
test('retained actual before supplemental is parseable; first r4 after syntax failure remains failure',()=>{
 const before=fs.readFileSync(new URL('fixtures/native-reviewer-common-cli-before-supplement.native-result.md',import.meta.url),'utf8');
 const after=fs.readFileSync(new URL('fixtures/native-reviewer-common-cli-after-supplement.native-result.md',import.meta.url),'utf8');
 const suite=JSON.parse(fs.readFileSync(new URL('fixtures/REVIEWER-COMMON-CLI-after-supplement-SUITE.json',import.meta.url)));
 const ids=suite.cases.filter(c=>c.role==='reviewer').map(c=>c.id);
 assert.equal(checkOutputSchema(before,ids).status,'PASS');
 assert.equal(checkOutputSchema(after,ids).stage,'JSON_SYNTAX');
});
