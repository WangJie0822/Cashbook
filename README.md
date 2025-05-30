# Cashbook

[![Build](https://github.com/WangJie0822/Cashbook/actions/workflows/Build.yaml/badge.svg)](https://github.com/WangJie0822/Cashbook/actions/workflows/Build.yaml)
[![GitHub](https://img.shields.io/github/license/WangJie0822/Cashbook)](http://www.apache.org/licenses/LICENSE-2.0)
[![GitHub release (with filter)](https://img.shields.io/github/v/release/WangJie0822/Cashbook)](https://github.com/WangJie0822/Cashbook/releases/latest)
![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/WangJie0822/Cashbook/total)

## ToDo 列表
- [x] 基本记账功能；
- [x] 资产管理功能；
- [x] 信用卡支持；
- [x] 多账本功能；
- [x] 检查更新功能；
- [x] 账单标签功能；
- [x] 自定义账单分类功能；
- [x] 账单搜索功能；
- [x] 备份与恢复；
- [x] WebDAV 备份支持；
- [x] 统计图表；
- [x] 离线版本；
- [x] 资产详情添加新建记录入口；
- [x] 标签可隐藏（特定时间使用之后不会再使用的标签隐藏，防止选择标签列表太过繁琐）；
- [x] 快捷进入菜单；
- [x] 使用本地图片作为账单背景；
- [x] 记录详情添加跳转资产功能；
- [x] 新增记账可关联图片功能；
- [x] 记录金额显示优化，只显示最终金额；
- [x] proto 拆分，减少设置项修改导致的无意义刷新数据；
- [ ] 导入导出为 csv；
- [ ] 多主题支持；
- [ ] 数据加载速度慢优化；
- [ ] 选择账单类型后，资产选择优化；
- [ ] markdown中链接点击事件；
- [ ] 信用卡账单日提醒及最后还款日提醒；
- [ ] 可报销记录未报销提醒；
- [ ] 修复切换账本后添加记录默认关联其它账本资产；

## 项目说明
一个自用记账本APP，从18年开始，就一直在使用**网易有钱**记账，考虑到个人隐私以及安全性问题，一直都是离线使用，21年年初突然听说**网易有钱**要停止运营了，看到相关信息说是停止运营前可以导出数据转到其他平台使用，因此连上网络准备导出，就这一步操作，让我记录了有三年多的数据全部丢失。之后终止了一段时间的记账，在这期间感觉到非常的没有安全感，感觉一下子对自己的资产没有了感知，这个月花了多少，收入多少，结余多少完全没有了概念，因此觉得记账还是必不可少，也尝试了其它不少记账APP，但都有或多或少的问题，要不就是功能不满足要求，要不就是要收费，而且还是不想把个人记账相关的信息暴露给三方的平台，最终还是决定自己写一个来使用。

因为个人开发，所以没有好的**UI**方面的想法，因此在界面设计以及功能方面借鉴了不少其它APP：

* 网易有钱
* 薄荷记账
* 钱迹
* Expenses

## 使用说明
1. 你可以从项目的发行版本中下载最新的安装包：[Github](https://github.com/WangJie0822/Cashbook/releases) or [Gitee](https://gitee.com/wangjie0822/Cashbook/releases)；

2. 你也可以将项目下载到本地运行，运行项目前请在项目 /gradle 路径下按如下格式添加文件 signing.versions.toml

```toml
[versions]
# 密钥别名
keyAlias = ""
# 别名密码
keyPassword = ""
# 密钥文件路径
storeFile = ""
# 密钥密码
storePassword = ""
```
