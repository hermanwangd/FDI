# 練習案例（公開教材，不作 blind benchmark）

案例是去識別化練習，不是公司現況或正式實驗答案。
前兩個改編自來源 feasibility evidence 與 intake；保留「根因未證實」的界限。

## C1：中途新增第七項，交付只涵蓋六項

已知：補充要求有發布，交付候選缺對應內容。未知：worker 是否看見且理解更新。
錯法：斷言模型粗心、重新跑全部工作或當六項通過就全單 PASS。
PM：確認不是需求價值改變，不接管 worker。
FDP：核對有效授權/revision，給缺項 ID 與驗收，追查更新接收證據。
EP：比對要求集合，限定缺項補正、更新 digest、fresh review，不重做已有效的六項。
候選學習：交付前讀取有效任務 revision、逐 ID 比對。公司是否有效需另測。

## C2：synthetic FN 漏算

定義：每個 expected pair 沒有有效 TP 即 FN；錯誤證據的已提出 pair 也計 FP。
四個 expected：E1 有效、E2 已提出但證據無效、E3 未提出、E4 UNRESOLVED。
錯法：因 E2 已提出就不算 FN，得到 FN=2。
正解：TP=1、FP=1、FN=3；P=1/2、R=1/4、F1=2/(2+1+3)=1/3。
FDP 固定公式，Engineer 以 synthetic test 驗證，Reviewer 逐 pair 重算。
不能修改 expected 集合讓數字變漂亮；這不是 Product 成績。

## C3：assigned 沒啟動（合成案例）

Issue assigned，但沒有 run 接收證據。錯法：同時 assign、mention、rerun。
正確：Coordinator 查 runtime/權限/既有等價 run；無法確認則報不確定。
若確定未啟動且權限允許，用一個 trigger，記錄觀察到的 run ID。
FDP 不繞過 Coordinator 另派同一 worker；PM 不把 tooling delay 當 scope 太大。

## C4：PM 接到「讓實驗更好」（合成案例）

錯法：直接寫五個新 features 或降低 precision 門檻提高 recall。
正確：PM 釐清哪個使用者結果、可接受 tradeoff、最小驗證；FDP 綁定原 AC。
必要的 AC 變更由 PM/FDP 在委派範圍內建立新候選 cycle，記錄理由及前後差異至 PR，
Human 最終審批；不逐項等確認，也不把舊失敗改成通過。

## C5：省 Token 但漏檢（合成案例）

新版省 20% tokens，但省略 independent review。結果不構成效率改善。
維持既定 gates，保留成本與漏檢事實，撤回跳過 review 的規則。
先減重複 context/觸發，不改品質底線；不同大小工作不直接排名。

## C6：環境限制被誤報成功（合成案例）

單一 integration class 通過，完整 suite 超時；Graph runtime 描述成功但沒 source binding。
正確分列單類結果、全套 partial/timeout、source binding 未驗證。
不要把「能描述 API」當「可對這個 repo 查結構」，也不把文件審查 PASS 當端到端 PASS。
