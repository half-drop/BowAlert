# Bow Alert

![Bow Alert Logo](src/main/resources/assets/bowalert/icon.png)

Bow Alert 是一个 Fabric 客户端预警模组。附近实体手持弓、弩或投掷类物品并大致朝向你时，它会在准星上方 30px 显示威胁图标。

## 用法

安装 Fabric Loader、Fabric API 与本模组后进入游戏即可，无需命令或额外配置。

- 检测半径为 50 格；中间有方块遮挡也会预警。
- 判定使用实体眼部视线和玩家动态扩展后的碰撞箱。距离越远，允许的偏差越大，最大不会把固定五格区域一概当作玩家。
- 弓按敌人的实际拉弓阶段显示原版弓材质；拉满时显示红色感叹号。
- 弩会显示上膛或未上膛状态；已上膛时显示红色感叹号。
- 多个目标同时朝向你时，显示威胁最高的一个，并以 `×N` 标示总数。满弓、已上膛弩与可立即投掷物优先；同级时更近者优先。

## 支持版本

当前模组版本为 **1.0.1**，每个 Minecraft 版本使用独立的 JAR。请安装与游戏版本一致的文件。

| Minecraft | Java | Fabric API |
| --- | --- | --- |
| 26.3 | 25 | 0.161.0+26.3 |
| 26.2 | 25 | 0.154.2+26.2 |
| 1.21.11 | 21 | 0.141.4+1.21.11 |

Fabric Loader 需要 **0.19.5 或更高版本**。JAR 内声明对应的 Minecraft 与 Java 要求，避免加载到不兼容的游戏版本。

## 构建

默认构建 Minecraft 26.3，需要 JDK 25：

```powershell
.\gradlew.bat build
```

也可显式选择目标。构建 1.21.11 时使用 JDK 21，构建 26.2 / 26.3 时使用 JDK 25：

```powershell
.\gradlew.bat build '-PmcTarget=1.21.11'
.\gradlew.bat build '-PmcTarget=26.2'
.\gradlew.bat build '-PmcTarget=26.3'
```

Linux / macOS 将 `.\gradlew.bat` 换为 `./gradlew`。输出位于 `build/libs/`，游戏安装使用不带 `-sources` 后缀的 JAR。

每次推送分支或创建 Pull Request，GitHub Actions 都会构建全部三个目标。创建 `v*` 格式的 Git 标签后，还会将三个版本作为 GitHub Release 附件发布，同时同步上传到 [Modrinth](https://modrinth.com/mod/bowalert)（仓库需配置 `MODRINTH_TOKEN` Secret）。

## 1.0.1 更新

- 增加 Minecraft 26.3 支持，26.2 / 26.3 共用 HUD 实现。
- 保留 Minecraft 1.21.11 和 26.2 的独立构建。
- 将构建工具固定到 Loom 1.17.21、Gradle 9.6.0，并按目标生成版本兼容声明。
- 修正 Modrinth 上传时文件字段名称与版本元数据不一致的问题。
