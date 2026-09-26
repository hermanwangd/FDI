# 覆蓋率指令骨架（需按專案實際工具鏈調整後使用）

> 以下是**骨架**，參數（路徑、門檻、報告格式）以各專案工具鏈與 `--help` 為準；
> 執行後把指令與原始輸出交給 Verifier 貼證據，QA 只負責解讀。

## Go

```bash
go test ./... -coverprofile=coverage.out          # 產出 profile
go tool cover -func=coverage.out | tail -n1       # 總覆蓋率（total 行）
go tool cover -html=coverage.out -o coverage.html # 逐檔報告
# 門檻檢查（骨架）：解析 total 行百分比，低於目標即 exit 1
```

## Python（pytest）

```bash
pytest --cov=<package> --cov-report=term-missing --cov-report=xml:coverage.xml
pytest --cov=<package> --cov-fail-under=80        # 門檻內建（數字按計畫目標）
```

## Node.js（nyc）

```bash
nyc --reporter=text --reporter=lcov npm test
nyc check-coverage --lines 80 --branches 80       # 門檻（數字按計畫目標）
```

## 通用紀律

- 覆蓋率數字是**下限守門**，不是品質證明：100% line 也可能漏掉未實作的規格——
  規格覆蓋（每條驗收條件有案例）與 code 覆蓋要分開報告。
- 報告必附：工具版本、指令原文、原始輸出檔位置，供 Verifier 重跑重現。
- 覆蓋率陡降（對 baseline）本身就是缺陷訊號，在測試報告 open findings 揭露。
