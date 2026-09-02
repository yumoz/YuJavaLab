# 代码质量体系

## 概述

项目通过 **Checkstyle**（代码规范）和 **SpotBugs**（静态缺陷检测）两套工具保障代码质量，均由 Maven 插件在构建阶段自动执行。两者均配置为 `failsOnError=false`，即检查失败仅告警、不阻断构建。

## Checkstyle 代码规范

配置文件为根目录下的 [`checkstyle.xml`](../checkstyle.xml)，插件版本 3.3.1，在 `validate` 阶段执行。

### 启用的规则（15 条）

| 规则                     | 类别     | 说明                                   |
| ---------------------- | ------ | ------------------------------------ |
| `AvoidStarImport`      | import | 禁止通配符导入（`import java.util.*`）        |
| `IllegalCatch`         | 编码     | 禁止捕获 `Exception` / `Throwable` 等宽泛异常 |
| `EmptyBlock`           | 编码     | 禁止空代码块                               |
| `MissingSwitchDefault` | 编码     | switch 必须包含 default 分支               |
| `FallThrough`          | 编码     | 禁止 case 落空穿透                         |
| `ModifierOrder`        | 修饰符    | 校验修饰符顺序                              |
| `RedundantModifier`    | 修饰符    | 禁止冗余修饰符                              |
| `UnusedImports`        | import | 检测未使用的 import                        |
| `WhitespaceAround`     | 空白     | 运算符/关键字周围空白                          |
| `GenericWhitespace`    | 空白     | 泛型空白规范                               |
| `MethodParamPad`       | 空白     | 方法参数括号内空白                            |
| `ParenPad`             | 空白     | 圆括号内空白                               |
| `TypecastParenPad`     | 空白     | 类型转换括号空白                             |
| `NoWhitespaceAfter`    | 空白     | 禁止特定字符后空白                            |
| `NoWhitespaceBefore`   | 空白     | 禁止特定字符前空白                            |

### 全局配置

```xml
<module name="Checker">
    <property name="charset" value="UTF-8"/>
    <property name="severity" value="warning"/>
    <property name="fileExtensions" value="java"/>
```

* 检查级别为 `warning`（输出警告但不阻断）。

* 只检查 `.java` 文件。

## SpotBugs 静态缺陷检测

插件版本 4.8.6.6，在 `verify` 阶段执行，排除配置位于 [`config/spotbugs-exclude.xml`](../config/spotbugs-exclude.xml)。

### 排除规则

```xml
<FindBugsFilter>
    <Match>
        <Bug pattern="EI_EXPOSE_REP,EI_EXPOSE_REP2"/>
    </Match>
</FindBugsFilter>
```

排除的 `EI_EXPOSE_REP` / `EI_EXPOSE_REP2`（Expose Internal Representation）是"暴露内部可变引用"类告警——对于本学习 Demo 项目（如返回 `Date` 字段、回填实体），这类告警属于可接受的冗余噪音，故统一排除。

## 非阻塞设计理念

两个插件的 `failsOnError=false` 意味着：静态质量问题**不会导致** **`mvn verify`** **失败**。这一设计适合学习项目，让开发者关注功能性正确性而不被风格/告警阻断构建，同时仍能看到告警列表便于改进。

## 当前状态

在标准 JDK 24 环境（CI）下运行 `mvn verify` 可完整通过。构建输出中发现的一个已知非阻塞告警：

* [`DatabaseInit.init()`](../src/main/java/com/example/util/DatabaseInit.java#L38) 中使用了 `catch (Exception e)`，触发 Checkstyle `IllegalCatch` 规则。该告警级别为 warning，不影响构建。

## 运行命令

```bash
mvn checkstyle:check     # 仅代码规范检查
mvn spotbugs:check       # 仅静态缺陷检测
mvn verify               # 测试 + Checkstyle + SpotBugs 全量验证
```

