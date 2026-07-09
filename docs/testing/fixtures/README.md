# 测试数据源：脱敏备份样本

`Cashbook_Backup_File_sample_desensitized.zip` 是一份**脱敏后的真实结构备份**，用于后续版本的恢复链路 / 统计 / 报销对冲 / 迁移等测试的数据源。

## 用途

在 app「我的 → 备份与恢复 → 数据恢复」中选择此文件恢复，即可得到一套结构真实、规模可观的数据用于手工/真机测试。文件名前缀 `Cashbook_Backup_File_`、后缀 `.zip` 已满足 app 备份识别规则，可直接被恢复列表识别。

## 内容规模（保留的测试价值）

- 记录 6073 条，时间跨度 2021–2026（多年、多月，覆盖统计/翻月/日历分组）。
- 报销对冲簇 160 组（`db_record_with_related`），覆盖 finalAmount 净自付、饼图三口径、报销/退款吸收链路。
- 资产 26 个（含信用卡账单/还款日、余额/额度），账本 2 个，标签 23 个，图片关联 53 行。
- 备份格式：app 原生多 entry zip —— `cashbook.db`（DEFLATE，带兼容旧 app 的 comment）+ `record_images/*.jpg`（STORED）+ `settings.json` + `manifest.json`（formatVersion=2，DB schema version=14）。

## 脱敏范围（已移除的敏感信息）

| 字段 | 处理 |
|---|---|
| `db_asset.name` | 全部泛化为「资产{id}」 |
| `db_asset.open_bank` / `card_no` / `remark` | 全部清空（真实银行/支行、完整卡号、备注） |
| `db_books.name` | 默认账本保留，其余泛化为「账本{id}」 |
| `db_books.description` / `bg_uri` | 清空（bg_uri 曾含真实照片路径+日期） |
| `db_tag.name` | 全部泛化为「标签{id}」（原含人名/地点/事件） |
| `db_record.remark` | 全部清空（1125 条含人名/房租/押金等自由文本） |
| `record_images/*.jpg` | 53 张真实票据照片全部替换为 160 字节占位 JPEG（文件名不变、关联保留） |

**保留（去标识后不再指向个人）**：记录金额与时间、资产余额/额度/账单日、类型分类、各表 id 与关联关系 —— 以维持统计/对冲/迁移测试的真实性。

**settings.json**：仅应用偏好（无 WebDAV 域名/账号/密码等凭据）。

## 已做的核验

- 全表文本列 PII 终扫：无残留银行卡号（≥11 位数字串）、银行/支付渠道名、常见姓氏、`IMG_`/`file:///` 路径。
- DB `PRAGMA integrity_check = ok`，`user_version = 14`。
- zip `testzip` 通过；53 张占位图均为同一 160 字节合法 JPEG。

## 注意

- 记录金额/时间为**真实值（已去标识）**。若后续认为消费金额/时间仍属敏感，可对 `amount`/`record_time` 做进一步扰动后重新打包。
- 首次用于自动化前，建议在模拟器上手工恢复一次做冒烟确认。
