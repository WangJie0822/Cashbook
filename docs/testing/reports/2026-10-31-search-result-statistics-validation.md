# 搜索结果统计与分类分布验证

验证日期：2026-10-31。代码快照以 `git log --grep='按分类键分配颜色避免稀疏ID与零净额占槽'` 定位；后续仅整理文档。最终多维评审另见本地 `.full-review/`，本文不将评审计划写成已通过。

## 功能与回归边界

关键词与筛选结果均显示全量笔数、收入、支出、结余及同一张分类净额分布图。分页和统计共享固定关键词上下文；保留旧备注 LIKE、金额匹配、当前账本与关键词不叠加筛选的规则。

收入/支出采用现有净额，转账仅计算手续费减优惠。分类先做有符号加总，再按绝对值确定扇区；图例保留负号与零净额组。汇总与分类分组来自一条事务查询，标签使用 EXISTS，避免重合标签放大笔数。溢出进入统计错误而非伪装为零；统计错误仍保留可用账单列表。

未更改 schema、金额写入/退款报销算法、首页/Analytics/Budget 口径、依赖、CI 或发布配置。旧三参数关键词接口保留，新增固定上下文重载；全仓 unit 与 lint 编译覆盖调用方及测试替身。分类编辑修复位于另一独立分支，未夹带。

## 执行记录

原始日志与冻结 XML/JSON 位于本地 `.superpowers/sdd/2026-10-31-search-result-statistics/`（以下记作 W，gitignored）。测试文件全集见 `W/test-source-inventory.json`：219 个 unit 源文件、11 个 androidTest 源文件。枚举不代表全部仪器测试已执行。

| 范围 | 实际命令（Windows 使用 gradlew.bat） | 结果与证据 |
|---|---|---|
| 主工程完整 unit | `./gradlew testOnlineDebug testOfflineDebug testDebugUnitTest :core:model:test :lint:test -Proborazzi.test.verify=false --rerun-tasks --console=plain` | BUILD SUCCESSFUL；732 个任务实际执行；212 XML、1551 tests，失败/错误/跳过均0。`W/full-unit-current.log`、`full-unit-current.json` 含逐文件清单与计数 |
| 独立构建逻辑 | `./gradlew -p build-logic check --console=plain` | BUILD SUCCESSFUL；OutputsTest 7 tests，失败/错误/跳过均0。`W/build-logic-check.log`；不混入1551 |
| APK | `./gradlew :app:assembleDevDebug --console=plain` | BUILD SUCCESSFUL；`W/assemble-dev.log`；仅 debug 产物 |
| 格式 | `./gradlew spotlessCheck --init-script gradle/init.gradle.kts --no-configuration-cache --console=plain` | BUILD SUCCESSFUL；`W/spotless-check.log` |
| 依赖及 lint | `./gradlew dependencyGuard :app:lintOnlineRelease :app:lintOfflineRelease :app:lintDevRelease :lint:lint --console=plain` | BUILD SUCCESSFUL；`W/lint-dependency-check.log` |
| 全仓截图校验 | `./gradlew verifyRoborazziDebug --console=plain` | BUILD SUCCESSFUL；`W/full-screenshots.log`。只重录两个相关 records 截图类，未重录其他模块；人工查看关键词浅色、混合暗色、多分类平板及配色修正后样本 |
| data 完整仪器测试 | `ANDROID_SERIAL=emulator-5582 ./gradlew :core:data:connectedDebugAndroidTest --console=plain`（Windows 设置同名环境变量） | BUILD SUCCESSFUL；26 tests，失败/错误/跳过均0；两类分别14/12。`W/full-data-room.log`、`full-data-room.xml` |

DevDebug APK：`app/build/outputs/apk/Dev/debug/app-Dev-debug.apk`，SHA256 `6525AD14C000EF51C1FA25CE131F47417FC60872072ECB23B28ADE27639BCF50`。不是签名 release，也未上传。

## 守卫判别力

初始关键词 Hidden、元数据改名不刷新、同步极端金额转换异常、恢复时过早显示空结果、稀疏分类 ID 颜色碰撞，均保留真实入口的 RED 记录与修改后测试结果。颜色像素探针曾因 forceRedraw 超时作废，随后用接入真实图/图例的共享映射函数重建 RED；不将该超时计为产品缺陷或变异成功。

变异在独立 worktree 执行，原实现树只读，原始副本逐字恢复。每个子进程有硬超时；先跑无变异基线，再交叉检查实际测试名、断言原因、XML、退出码及 BUILD 标记。`W/evidence-index.json` 列出20种变异及各自转红用例。

- 第一快照（`git log --grep='元数据就绪前保留查询加载状态'`）19/19 KILLED：`W/mutations/results.json`。
- 配色修正后重新执行5项 UI 相关变异，5/5 KILLED：`W/mutations-final-ui/results.json`。其 `status=PARTIAL` 表示仅请求5项；`requested_all_killed=true`。前后合并为20种，不是24种，也不声称全20项在同一快照运行。
- 其余模型/VM/SQL目标在两快照间逐字相同，独立比较清单见 `evidence-index.json`。两轮均 `source_untouched=true`，变异副本已恢复。
- 覆盖精确旧VM/页面回退、错误账本、关键词元数据刷新断线、迟到成功/失败、溢出/符号、第一页截断、abs移位、本金混入、负值截断、多标签 JOIN 重复、小分类丢失、旧哈希颜色及统计错误遮蔽列表。

变异执行曾因 UTP XML 无 failure type/message 而停止；核对实际 expected/but-was 断言后调整解析器，不虚构异常类型。另一次 DEVICE_PROVIDER_CONFIG_ERROR/adb version 检测失败，0 tests，未计 KILLED；保留失败原文，重新核对设备后新基线与剩余用例实际成功。更深环境根因未定位。执行事件及原报告均保留。

## 原生 APK 回放

Android14 专用 AVD 从真实搜索、筛选、详情与编辑入口操作。151笔各1元支出、100元收入、手续费2元/优惠5元的1000元转账及免费转账，共154笔关键词命中；另有零贡献记录及另一账本的同词同标签记录。

| 场景 | 实际结果 |
|---|---|
| 关键词全量 | 154笔；收入100、支出148、结余−48；分布基数254，超过首屏分页仍计全量 |
| 仅负净额 | 数字关键词1000命中1笔；支出−3、结余3、基数3，金额保留负号 |
| 零贡献/无匹配 | 分别为1笔/0笔，收支为0；显示不同分布空态 |
| 极端输入后恢复 | 输入 `1e2147483647` 后普通 `qastats` 查询仍得到154笔及正确金额；不以PID存活替代功能恢复验证 |
| 单标签/关键词互斥 | 标签A为153笔；关键词查询恢复154笔，清空关键词再恢复153笔 |
| 隐藏标签、多选去重 | 可选择“QATagB（已隐藏）”，与A并集154笔，不重复累加共同标签账单 |
| 图例与折叠 | 选中分类圆心151元、59.45%；取消恢复基数254；折叠前后汇总相同 |
| 编辑后刷新 | 真实详情→编辑→计算器→保存，将1元支出改2元；筛选和关键词均更新为154笔、收入100、支出149、结余−49、基数255 |

`W/native/search-results.json` 保留4组查询通过结果，`filter-results.json` 保留8组恢复/筛选/交互通过结果，`edit-results.json` 保留3组编辑刷新结果；每项包含原始 UI 层级证据路径。`keyword-final.png` 已人工查看：同图含三种收支类型，图/图例颜色对应，转账−3元小扇区保留。`database-result.json` 独立比较156笔记录的ID、分类、账本、金额、净额、手续费、优惠及备注：只有目标账单的金额与净额从100分变200分，其他记录相同。

工具执行限制也保留：Android CLI 一次安装/启动最终返回1，之后独立核对前台Activity和实际UI；层级采集曾返回null root，补取SDK UIA；一次UIA命令退出137，未计通过，后续普通查询实际恢复。测试脚本标签预期键误写为tagA而非tag_a，修正脚本后从已保存界面继续；不将脚本异常算产品失败。DoKit浮标曾遮挡返回，移开浮标后继续操作，未改产品代码。

## 评审与限制

方案四维评审无可报告问题。Task1 因作为后续接口前提单独评审，发现1条 Medium：金额转换在UI事件同步执行。通过实际 ViewModel 重现后移入注入 IO，共享 Deferred，转换失败分别进入分页/统计错误边界，普通查询可恢复。只有控制器定向复核，未重复扩展评审层数。原始报告见 `W/task1-review.md`。

本地仅使用 Android14 专用模拟器及合成账本；不操作物理设备、真实用户账本或外部账户。未执行 Android15/天玑9300 真机、其他模块全量仪器测试、云端 CI、release、推送或合并。用户选择本次不归档 Vault/Memory，亦未登记自动归档文档。
