# AI记账 - 智能记账应用

一款基于本地AI解析的智能记账Android应用。

## 功能特点

- **AI智能记账**: 输入自然语言描述，自动解析金额和分类
- **手动记账**: 传统表单式记账
- **快捷记账**: 一键添加收入/支出
- **记录查看**: 查看所有记账记录，支持删除
- **统计分析**: 月度收支统计和分类饼图

## 获取 APK 安装包

### 方法一：GitHub Actions 自动构建（推荐）

1. 将此项目上传到 GitHub：
   ```bash
   cd AIBookkeeping
   git init
   git add .
   git commit -m "Initial commit"
   git remote add origin https://github.com/你的用户名/AIBookkeeping.git
   git push -u origin main
   ```

2. 打开 GitHub 仓库页面，点击 **Actions** 标签

3. 点击 **Build APK** workflow，然后点击 **Run workflow**

4. 等待构建完成后，在 **Artifacts** 部分下载 `AI-Bookkeeping-APK`

5. 解压后得到 `app-debug.apk`，传到手机安装即可

### 方法二：Android Studio 本地构建

1. 下载安装 [Android Studio](https://developer.android.com/studio)
2. 打开 Android Studio，选择 `Open` 打开 `AIBookkeeping` 文件夹
3. 等待 Gradle 同步完成
4. 点击菜单 `Build > Build Bundle(s) / APK(s) > Build APK(s)`
5. APK 位置: `app/build/outputs/apk/debug/app-debug.apk`

## AI解析示例

| 输入 | 解析结果 |
|------|---------|
| 早餐15 | 餐饮 - 支出 ¥15 |
| 午饭花了30块 | 餐饮 - 支出 ¥30 |
| 打车去公司25 | 交通 - 支出 ¥25 |
| 收到工资5000 | 工资 - 收入 ¥5000 |
| 奶茶12元 | 餐饮 - 支出 ¥12 |
| 电费200 | 居住 - 支出 ¥200 |

## 安装要求

- Android 8.0 (API 26) 或更高版本
- 约 10MB 存储空间

## 技术栈

- Kotlin + Android Jetpack
- Room 数据库
- Material Design 3
- MPAndroidChart 图表
