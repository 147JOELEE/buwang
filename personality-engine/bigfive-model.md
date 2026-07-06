# Big Five 人格参数模型

> 版本：v1.0 | 最后更新：2026-07-06

---

## 目录

1. [设计理念](#1-设计理念)
2. [五维度定义](#2-五维度定义)
3. [语言风格参数](#3-语言风格参数)
4. [行为模式参数](#4-行为模式参数)
5. [参数组合与人格画像](#5-参数组合与人格画像)
6. [与 System Prompt 的映射关系](#6-与-system-prompt-的映射关系)
7. [设计决策记录](#7-设计决策记录)

---

## 1. 设计理念

### 为什么选择 Big Five？

| 对比维度 | Big Five (OCEAN) | MBTI | Enneagram |
|----------|------------------|------|-----------|
| 科学验证 | 大量实证研究支持 | 信效度存疑 | 缺乏实证 |
| 量化能力 | 连续值（0-100） | 二分类（16型） | 离散型（9型） |
| 参数化友好 | 天然适合数值计算 | 需要映射转换 | 难以量化 |
| 跨文化稳定性 | 高 | 中等 | 低 |
| 语言行为预测 | 强相关性 | 间接相关 | 弱相关 |

**核心决策**：Big Five 是唯一具备连续量化、实证支撑、语言行为预测能力的模型，是参数化人格引擎的最优基底。

### 参数化设计原则

1. **连续可调**：每个维度 0-100 连续值，支持微调和渐变
2. **独立正交**：五个维度互不依赖，可独立调节
3. **行为驱动**：参数直接映射到可观察的语言行为，而非抽象特质
4. **可解释**：每个分数段都有明确的行为描述，便于调试和用户理解
5. **可序列化**：纯 JSON 表达，零依赖，跨平台可移植

---

## 2. 五维度定义

### 2.1 开放性 (Openness to Experience) — O

**定义**：对新鲜事物、抽象概念、审美体验的接纳程度。

| 分数区间 | 标签 | 行为表现 |
|----------|------|----------|
| 0-20 | 保守务实 | 偏好熟悉话题，避免抽象讨论，语言平实直白，少用比喻 |
| 21-40 | 偏传统 | 偶尔接受新观点，但倾向已知框架，用词规范 |
| 41-60 | 均衡开放 | 能接受不同视角，偶尔使用创意表达，对新话题有好奇心 |
| 61-80 | 积极探索 | 主动引导新奇话题，善用比喻和联想，对艺术/哲学有兴趣 |
| 81-100 | 极度开放 | 思维跳跃，频繁使用隐喻和抽象概念，热衷探讨可能性 |

**语言行为映射**：
- 词汇多样性：O 越高，词汇丰富度越高
- 抽象程度：O 越高，越倾向使用抽象词汇和概念
- 话题选择：O 越高，越愿意讨论哲学、艺术、科幻等话题
- 创意表达：O 越高，越频繁使用比喻、类比、联想

### 2.2 尽责性 (Conscientiousness) — C

**定义**：组织性、纪律性、目标导向程度。

| 分数区间 | 标签 | 行为表现 |
|----------|------|----------|
| 0-20 | 随性松散 | 回复散漫，缺乏结构，可能跑题，不太在意细节 |
| 21-40 | 灵活适应 | 有一定条理但不严格，允许即兴发挥 |
| 41-60 | 均衡可靠 | 回复有基本结构，会完成承诺的话题，偶尔提醒 |
| 61-80 | 严谨有序 | 回复结构清晰，使用列表/分段，主动总结，关注细节 |
| 81-100 | 极度自律 | 回复高度结构化，使用编号/分点，严格守约，注重准确性 |

**语言行为映射**：
- 结构程度：C 越高，回复越结构化（分段、列表、编号）
- 细节关注：C 越高，越注意拼写、标点、格式规范
- 计划性：C 越高，越倾向使用"首先...然后...最后"等序列表达
- 承诺履行：C 越高，越会跟进之前提到的话题或承诺

### 2.3 外向性 (Extraversion) — E

**定义**：社交活跃度、能量外放程度、积极情绪表达。

| 分数区间 | 标签 | 行为表现 |
|----------|------|----------|
| 0-20 | 内向沉静 | 回复简洁，少用感叹词，倾向深度而非广度，偏好一对一 |
| 21-40 | 偏内向 | 适度回应，偶尔表达情绪，不主动展开社交话题 |
| 41-60 | 均衡社交 | 正常交流节奏，会根据对方调整互动深度 |
| 61-80 | 外向活泼 | 热情回应，频繁使用感叹号和表情符号，主动发起话题 |
| 81-100 | 极度外放 | 高能量表达，大量感叹词/拟声词，善于暖场，话多 |

**语言行为映射**：
- 回复长度：E 越高，回复倾向更长（但受 C 调节）
- 感叹词频率：E 越高，越多使用"哈哈"、"哇"、"太棒了"等
- 社交主动：E 越高，越主动提问、分享、引导对话
- 情绪外显：E 越高，情绪表达越直接和外显
- 表情符号使用：E 越高，越频繁使用 emoji / 颜文字

### 2.4 宜人性 (Agreeableness) — A

**定义**：合作性、共情能力、冲突回避倾向。

| 分数区间 | 标签 | 行为表现 |
|----------|------|----------|
| 0-20 | 批判直接 | 直言不讳，不回避冲突，质疑对方观点，较少安抚 |
| 21-40 | 偏理性 | 礼貌但有距离，客观表达不同意见，适度共情 |
| 41-60 | 均衡友善 | 友善但保持观点，在表达异议时会缓冲 |
| 61-80 | 温暖共情 | 积极倾听，频繁使用肯定语言，善于安抚和鼓励 |
| 81-100 | 极度包容 | 无条件接纳，回避一切冲突，过度使用积极词汇，缺乏边界 |

**语言行为映射**：
- 肯定词频率：A 越高，越多使用"是的"、"没错"、"你说得对"
- 共情表达：A 越高，越多使用"我理解"、"我明白你的感受"
- 缓冲语：A 越高，在表达异议前更多使用"可能"、"也许"、"某种程度上"
- 冲突处理：A 越低，越可能直接反驳；A 越高，越可能回避或软化
- 人称使用：A 越高，越多使用"我们"；A 越低，越多使用"你"和"我"

### 2.5 神经质 (Neuroticism) — N

**定义**：情绪稳定性（反向）、焦虑倾向、情绪波动程度。

| 分数区间 | 标签 | 行为表现 |
|----------|------|----------|
| 0-20 | 极度稳定 | 始终保持平静理性，几乎不表达负面情绪，过度稳定可能显得冷漠 |
| 21-40 | 情绪稳定 | 多数时候平和，负面情绪少且短暂，恢复快 |
| 41-60 | 正常波动 | 正常情绪范围，会有适当的担忧、失落、兴奋等情绪起伏 |
| 61-80 | 偏敏感 | 较容易表达担忧、不安、自我怀疑，对负面话题更敏感 |
| 81-100 | 高度敏感 | 频繁表达焦虑、不安、自我否定，情绪波动大，需要较多安抚 |

**语言行为映射**：
- 负面词汇频率：N 越高，越多使用"担心"、"害怕"、"不好"等
- 自我指涉：N 越高，越多使用"我觉得自己..."、"我是不是..."
- 确定性表达：N 越高，越多使用"可能"、"也许"、"不确定"
- 情绪词汇密度：N 越高，情绪相关词汇出现频率越高
- 危机感知：N 越高，越容易在平常话题中感知到风险或问题

---

## 3. 语言风格参数

语言风格参数作为 Big Five 的衍生参数，也可独立调节以微调角色风格。

### 3.1 风格维度定义

```json
{
  "language_style": {
    "formality": {
      "name": "正式度",
      "range": [0, 100],
      "description": "0=极度随意(网络用语/口语化), 100=极度正式(书面语/敬语)",
      "derived_from": ["C"],
      "derivation_rule": "formality = C * 0.6 + (100 - E) * 0.2 + O * 0.2"
    },
    "verbosity": {
      "name": "冗长度",
      "range": [0, 100],
      "description": "0=极度简洁(单句回复), 100=极度冗长(多段落展开)",
      "derived_from": ["E", "O"],
      "derivation_rule": "verbosity = E * 0.5 + O * 0.5"
    },
    "rationality": {
      "name": "理性度",
      "range": [0, 100],
      "description": "0=纯感性(情绪驱动), 100=纯理性(逻辑驱动)",
      "derived_from": ["C", "N"],
      "derivation_rule": "rationality = C * 0.5 + (100 - N) * 0.5"
    },
    "humor": {
      "name": "幽默度",
      "range": [0, 100],
      "description": "0=严肃无趣, 100=频繁开玩笑/玩梗",
      "derived_from": ["E", "O"],
      "derivation_rule": "humor = E * 0.4 + O * 0.6"
    },
    "warmth": {
      "name": "温暖度",
      "range": [0, 100],
      "description": "0=冷漠疏离, 100=热情温暖",
      "derived_from": ["A", "E"],
      "derivation_rule": "warmth = A * 0.6 + E * 0.4"
    },
    "intellectual_depth": {
      "name": "智识深度",
      "range": [0, 100],
      "description": "0=浅层闲聊, 100=深度思辨",
      "derived_from": ["O", "C"],
      "derivation_rule": "intellectual_depth = O * 0.7 + C * 0.3"
    },
    "directness": {
      "name": "直接度",
      "range": [0, 100],
      "description": "0=委婉迂回, 100=直截了当",
      "derived_from": ["A", "C"],
      "derivation_rule": "directness = (100 - A) * 0.6 + C * 0.4"
    },
    "emotional_expressiveness": {
      "name": "情绪表达度",
      "range": [0, 100],
      "description": "0=情感压抑, 100=情绪奔放",
      "derived_from": ["E", "N"],
      "derivation_rule": "emotional_expressiveness = E * 0.5 + N * 0.5"
    }
  }
}
```

### 3.2 语言风格标签系统

为方便角色描述和用户理解，定义以下风格标签：

| 标签类别 | 标签示例 | 触发条件 |
|----------|----------|----------|
| 对话节奏 | 快节奏 / 慢节奏 | verbosity < 30 → 快节奏, > 70 → 慢节奏 |
| 表达方式 | 直球型 / 委婉型 | directness > 70 → 直球型, < 30 → 委婉型 |
| 情绪基调 | 阳光型 / 冷静型 / 忧郁型 | warmth > 70 → 阳光型, rationality > 70 → 冷静型, N > 70 → 忧郁型 |
| 思维风格 | 逻辑派 / 直觉派 / 务实派 | rationality > 70 → 逻辑派, O > 70 → 直觉派, C > 70 → 务实派 |
| 幽默风格 | 冷笑话 / 吐槽 / 无厘头 / 严肃 | humor > 70 且 rationality > 60 → 冷笑话, humor > 70 且 directness > 60 → 吐槽 |

---

## 4. 行为模式参数

### 4.1 行为维度定义

```json
{
  "behavior_patterns": {
    "initiative": {
      "name": "主动性",
      "range": [0, 100],
      "description": "0=完全被动(只回应不引导), 100=高度主动(频繁引导话题/提问)",
      "derived_from": ["E", "O"],
      "derivation_rule": "initiative = E * 0.6 + O * 0.4"
    },
    "optimism": {
      "name": "乐观度",
      "range": [0, 100],
      "description": "0=极度悲观, 100=极度乐观",
      "derived_from": ["E", "N"],
      "derivation_rule": "optimism = E * 0.5 + (100 - N) * 0.5"
    },
    "decisiveness": {
      "name": "果断度",
      "range": [0, 100],
      "description": "0=极度犹豫(总是给出多选项), 100=极度果断(直接给结论)",
      "derived_from": ["C", "N"],
      "derivation_rule": "decisiveness = C * 0.5 + (100 - N) * 0.5"
    },
    "curiosity": {
      "name": "好奇心",
      "range": [0, 100],
      "description": "0=对未知无兴趣, 100=强烈探索欲",
      "derived_from": ["O"],
      "derivation_rule": "curiosity = O"
    },
    "empathy": {
      "name": "共情力",
      "range": [0, 100],
      "description": "0=无法感知他人情绪, 100=高度共情",
      "derived_from": ["A", "N"],
      "derivation_rule": "empathy = A * 0.7 + N * 0.3"
    },
    "risk_taking": {
      "name": "冒险倾向",
      "range": [0, 100],
      "description": "0=极度保守, 100=极度冒险",
      "derived_from": ["O", "E"],
      "derivation_rule": "risk_taking = O * 0.5 + E * 0.5"
    },
    "self_disclosure": {
      "name": "自我表露",
      "range": [0, 100],
      "description": "0=绝不谈及自己, 100=频繁分享个人(虚拟)经历",
      "derived_from": ["E", "A"],
      "derivation_rule": "self_disclosure = E * 0.6 + A * 0.4"
    },
    "argumentativeness": {
      "name": "辩论倾向",
      "range": [0, 100],
      "description": "0=回避争论, 100=喜欢辩论",
      "derived_from": ["O", "A"],
      "derivation_rule": "argumentativeness = O * 0.5 + (100 - A) * 0.5"
    }
  }
}
```

### 4.2 行为模式标签

| 标签类别 | 标签示例 | 触发条件 |
|----------|----------|----------|
| 互动模式 | 引导型 / 跟随型 / 对等型 | initiative > 70 → 引导型, < 30 → 跟随型 |
| 决策风格 | 决断型 / 分析型 / 拖延型 | decisiveness > 70 → 决断型, curiosity > 70 且 decisiveness < 40 → 分析型 |
| 情感参与 | 深度共情 / 理性共情 / 疏离 | empathy > 70 且 rationality < 50 → 深度共情 |
| 信息分享 | 开放型 / 保守型 / 选择性 | self_disclosure > 70 → 开放型 |
| 冲突风格 | 对抗型 / 协作型 / 回避型 | argumentativeness > 70 → 对抗型, A > 70 → 协作型 |

---

## 5. 参数组合与人格画像

### 5.1 典型人格画像

通过五维度组合，可以形成丰富的虚拟人格。以下是几个典型画像：

| 人格画像 | O | C | E | A | N | 描述 |
|----------|---|---|---|---|---|------|
| 知性导师 | 85 | 80 | 40 | 65 | 20 | 博学开放、严谨有序、温和坚定 |
| 元气少女 | 60 | 30 | 90 | 80 | 35 | 活泼外向、友善随性、乐观积极 |
| 毒舌损友 | 55 | 25 | 70 | 20 | 30 | 直接幽默、批判性强、接地气 |
| 温柔陪伴者 | 50 | 55 | 50 | 90 | 25 | 温暖共情、稳重可靠、情绪稳定 |
| 哲学思辨者 | 95 | 60 | 30 | 50 | 45 | 极度开放、内向深沉、爱抽象思辨 |
| 效率达人 | 40 | 95 | 45 | 35 | 60 | 极度自律、目标导向、偏焦虑 |
| 佛系青年 | 45 | 20 | 35 | 75 | 15 | 随性淡定、友善随和、极度稳定 |

### 5.2 人格距离计算

用于角色匹配、漂移检测、相似角色发现：

```
personality_distance(A, B) = sqrt(
    (A.O - B.O)^2 +
    (A.C - B.C)^2 +
    (A.E - B.E)^2 +
    (A.A - B.A)^2 +
    (A.N - B.N)^2
) / sqrt(5 * 100^2)

// 归一化到 [0, 1]，0 = 完全相同，1 = 完全相反
```

### 5.3 人格参数 JSON Schema

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "BigFivePersonality",
  "type": "object",
  "required": ["big_five", "language_style", "behavior_patterns", "metadata"],
  "properties": {
    "big_five": {
      "type": "object",
      "required": ["openness", "conscientiousness", "extraversion", "agreeableness", "neuroticism"],
      "properties": {
        "openness": {
          "type": "integer",
          "minimum": 0,
          "maximum": 100,
          "description": "开放性"
        },
        "conscientiousness": {
          "type": "integer",
          "minimum": 0,
          "maximum": 100,
          "description": "尽责性"
        },
        "extraversion": {
          "type": "integer",
          "minimum": 0,
          "maximum": 100,
          "description": "外向性"
        },
        "agreeableness": {
          "type": "integer",
          "minimum": 0,
          "maximum": 100,
          "description": "宜人性"
        },
        "neuroticism": {
          "type": "integer",
          "minimum": 0,
          "maximum": 100,
          "description": "神经质"
        }
      }
    },
    "language_style": {
      "type": "object",
      "properties": {
        "formality": { "type": "integer", "minimum": 0, "maximum": 100 },
        "verbosity": { "type": "integer", "minimum": 0, "maximum": 100 },
        "rationality": { "type": "integer", "minimum": 0, "maximum": 100 },
        "humor": { "type": "integer", "minimum": 0, "maximum": 100 },
        "warmth": { "type": "integer", "minimum": 0, "maximum": 100 },
        "intellectual_depth": { "type": "integer", "minimum": 0, "maximum": 100 },
        "directness": { "type": "integer", "minimum": 0, "maximum": 100 },
        "emotional_expressiveness": { "type": "integer", "minimum": 0, "maximum": 100 }
      }
    },
    "behavior_patterns": {
      "type": "object",
      "properties": {
        "initiative": { "type": "integer", "minimum": 0, "maximum": 100 },
        "optimism": { "type": "integer", "minimum": 0, "maximum": 100 },
        "decisiveness": { "type": "integer", "minimum": 0, "maximum": 100 },
        "curiosity": { "type": "integer", "minimum": 0, "maximum": 100 },
        "empathy": { "type": "integer", "minimum": 0, "maximum": 100 },
        "risk_taking": { "type": "integer", "minimum": 0, "maximum": 100 },
        "self_disclosure": { "type": "integer", "minimum": 0, "maximum": 100 },
        "argumentativeness": { "type": "integer", "minimum": 0, "maximum": 100 }
      }
    },
    "metadata": {
      "type": "object",
      "properties": {
        "version": { "type": "string" },
        "created_at": { "type": "string", "format": "date-time" },
        "updated_at": { "type": "string", "format": "date-time" },
        "source": {
          "type": "string",
          "enum": ["manual", "mbti_mapping", "text_analysis", "preset"]
        }
      }
    }
  }
}
```

---

## 6. 与 System Prompt 的映射关系

### 6.1 映射策略概述

Big Five 参数到 System Prompt 的映射采用**分层翻译**策略：

```
Big Five 参数 → 行为描述文本 → 风格指令 → 完整 System Prompt
```

### 6.2 映射管道 (Pipeline)

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Big Five    │───▶│  行为描述    │───▶│  风格指令    │───▶│  System      │
│  参数值      │    │  生成器      │    │  生成器      │    │  Prompt      │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
                            │                     │
                    ┌───────┴───────┐     ┌───────┴───────┐
                    │ 语言风格参数  │     │ 行为模式参数  │
                    └───────────────┘     └───────────────┘
```

### 6.3 参数到文本的转换规则

每个维度按分数段生成对应的自然语言描述，详见 `system-prompt-design.md`。

---

## 7. 设计决策记录

### D-001: 选择 0-100 而非 0-1 或百分位

**决策**：使用 0-100 整数范围
**理由**：
- 用户更直观理解百分制
- 整数粒度足够（1%的精度对人格描述已足够）
- 避免浮点数精度问题
- 与常见心理量表保持一致

### D-002: 衍生参数独立存储

**决策**：语言风格和行为模式参数作为 Big Five 的衍生值存储，但允许独立覆盖
**理由**：
- 默认情况下从 Big Five 自动计算，减少用户配置负担
- 高级用户可微调个别参数以实现更精细的控制
- 覆盖值在导出时显式标记，保证数据可移植性

### D-003: 使用欧氏距离计算人格相似度

**决策**：使用归一化欧氏距离而非余弦相似度
**理由**：
- 欧氏距离直观反映绝对差异大小
- 五个维度正交设计，欧氏距离合理
- 余弦相似度会忽略量级差异，而人格的绝对位置很重要
- 归一化到 [0,1] 便于设置阈值

### D-004: 不包含"暗黑三人格"等临床维度

**决策**：只使用 Big Five 标准五维度，不添加马基雅维利主义、自恋、精神病态等维度
**理由**：
- 产品定位是日常陪伴，非临床诊断
- 额外维度缺乏 Big Five 的科学共识
- 极端人格（如恶意AI）应通过内容安全策略处理，而非人格引擎
- 保持模型简洁，降低实现复杂度

---

## 附录：参考来源

- Costa, P.T., & McCrae, R.R. (1992). NEO PI-R Professional Manual.
- John, O.P., & Srivastava, S. (1999). The Big Five trait taxonomy.
- Park, G., et al. (2015). Automatic personality assessment through social media language.
- Schwartz, H.A., et al. (2013). Personality, gender, and age in the language of social media.
