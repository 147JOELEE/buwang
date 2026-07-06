# 聊天文本人格分析 Prompt 设计

> 版本：v1.0 | 最后更新：2026-07-06

---

## 目录

1. [功能概述](#1-功能概述)
2. [预处理规则](#2-预处理规则)
3. [分析 Prompt 模板](#3-分析-prompt-模板)
4. [输出 JSON 格式定义](#4-输出-json-格式定义)
5. [置信度评分机制](#5-置信度评分机制)
6. [分析流程设计](#6-分析流程设计)
7. [质量保障策略](#7-质量保障策略)
8. [设计决策记录](#8-设计决策记录)

---

## 1. 功能概述

### 1.1 功能定位

聊天文本人格分析模块用于从用户的聊天文本中提取 Big Five 人格特征。这是「不忘」App **个性化**能力的基础：

- **用户人格画像**：根据用户的聊天风格自动推荐匹配的角色
- **角色人格验证**：验证用户创建的角色是否符合预期人格
- **对话质量评估**：分析对话中的人格表达质量
- **人格演变追踪**：长期追踪用户的人格变化趋势

### 1.2 设计原则

| 原则 | 说明 |
|------|------|
| 隐私优先 | 分析在本地完成，原始文本不上传 |
| 可解释性 | 每个维度的评分都有文本证据支撑 |
| 置信度透明 | 明确告知用户分析结果的可靠性 |
| 增量更新 | 支持基于新文本更新已有的人格画像 |
| 非诊断性 | 明确标注"仅供娱乐参考，非心理诊断" |

---

## 2. 预处理规则

### 2.1 预处理管道

```
原始聊天记录
    │
    ▼
[Step 1] 格式清洗
    │  去除时间戳、系统消息、元数据
    ▼
[Step 2] 角色分离
    │  区分"用户发言"和"AI回复"
    ▼
[Step 3] 文本量检查
    │  检查是否满足最小分析门槛
    ▼
[Step 4] 噪声过滤
    │  去除纯表情、单字回复、无意义内容
    ▼
[Step 5] 分段与批处理
    │  将长对话分段，每段控制在分析窗口内
    ▼
清洗后的用户文本 → 送入 LLM 分析
```

### 2.2 格式清洗规则

```python
def clean_chat_text(raw_text: str) -> str:
    """
    清洗聊天文本，去除无关内容
    """
    import re

    # 1. 去除时间戳 (多种格式)
    cleaned = re.sub(r'\d{2}:\d{2}(:\d{2})?', '', cleaned)
    cleaned = re.sub(r'\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}', '', cleaned)

    # 2. 去除系统消息
    cleaned = re.sub(r'\[系统\].*?\n', '', cleaned)
    cleaned = re.sub(r'\[System\].*?\n', '', cleaned)

    # 3. 统一角色标记格式
    cleaned = re.sub(r'用户[：:]', '用户：', cleaned)
    cleaned = re.sub(r'AI[：:]', 'AI：', cleaned)
    cleaned = re.sub(r'User[：:]', '用户：', cleaned)
    cleaned = re.sub(r'Assistant[：:]', 'AI：', cleaned)

    # 4. 去除多余空行
    cleaned = re.sub(r'\n{3,}', '\n\n', cleaned)

    return cleaned.strip()
```

### 2.3 角色分离规则

```python
def separate_roles(cleaned_text: str) -> dict:
    """
    将对话文本按角色分离
    """
    user_messages = []
    ai_messages = []

    lines = cleaned_text.split('\n')
    current_role = None
    current_message = []

    for line in lines:
        if line.startswith('用户：') or line.startswith('User:'):
            if current_role == 'user' and current_message:
                user_messages.append(''.join(current_message))
                current_message = []
            current_role = 'user'
            current_message.append(line.split('：', 1)[-1] if '：' in line else line)
        elif line.startswith('AI：') or line.startswith('Assistant:'):
            if current_role == 'ai' and current_message:
                ai_messages.append(''.join(current_message))
                current_message = []
            current_role = 'ai'
            current_message.append(line.split('：', 1)[-1] if '：' in line else line)
        else:
            if current_role:
                current_message.append(line)

    # 处理最后一条消息
    if current_role == 'user' and current_message:
        user_messages.append(''.join(current_message))
    elif current_role == 'ai' and current_message:
        ai_messages.append(''.join(current_message))

    return {
        'user_messages': user_messages,
        'ai_messages': ai_messages,
        'user_message_count': len(user_messages),
        'ai_message_count': len(ai_messages)
    }
```

### 2.4 文本量门槛

分析质量与文本量直接相关。定义以下门槛：

| 门槛级别 | 最少字数 | 最少轮次 | 适用场景 | 置信度上限 |
|----------|----------|----------|----------|-----------|
| 不可分析 | < 100字 | < 5轮 | 文本不足 | N/A |
| 低质量 | 100-500字 | 5-15轮 | 初步参考 | 0.4 |
| 中等质量 | 500-2000字 | 15-50轮 | 基本可靠 | 0.7 |
| 高质量 | 2000-5000字 | 50-150轮 | 较为可靠 | 0.85 |
| 极高质量 | > 5000字 | > 150轮 | 高度可靠 | 0.95 |

```python
def check_quality(user_messages: list[str]) -> dict:
    """
    检查文本质量是否满足分析要求
    """
    total_chars = sum(len(msg) for msg in user_messages)
    total_rounds = len(user_messages)

    if total_chars < 100 or total_rounds < 5:
        return {
            'analyzable': False,
            'quality_level': 'insufficient',
            'max_confidence': 0.0,
            'recommendation': '需要更多对话文本，建议至少积累5轮以上对话后再进行分析'
        }

    if total_chars < 500:
        quality = 'low'
        max_conf = 0.4
    elif total_chars < 2000:
        quality = 'medium'
        max_conf = 0.7
    elif total_chars < 5000:
        quality = 'high'
        max_conf = 0.85
    else:
        quality = 'excellent'
        max_conf = 0.95

    return {
        'analyzable': True,
        'quality_level': quality,
        'total_chars': total_chars,
        'total_rounds': total_rounds,
        'max_confidence': max_conf,
        'recommendation': None
    }
```

### 2.5 噪声过滤

```python
def filter_noise(messages: list[str]) -> list[str]:
    """
    过滤低质量消息
    """
    filtered = []
    for msg in messages:
        # 去除纯表情/符号消息
        if len(msg.strip()) <= 2 and not any(c.isalpha() for c in msg):
            continue
        # 去除纯数字/日期
        if msg.strip().isdigit():
            continue
        # 去除纯标点
        if all(c in '，。！？…、；：""''（）【】《》' for c in msg.strip()):
            continue
        filtered.append(msg)
    return filtered
```

---

## 3. 分析 Prompt 模板

### 3.1 完整分析 Prompt

```
你是一个心理学专家，擅长通过语言行为分析人格特征。请基于以下用户的聊天文本，分析其 Big Five 人格特征。

## 分析框架
请从以下五个维度进行评估，每个维度给出0-100的分数：

1. **开放性 (Openness)**：思维开放程度、好奇心、对新鲜事物的接受度
   - 高分特征：使用丰富的词汇、讨论抽象话题、展现想象力
   - 低分特征：语言平实直接、偏好具体话题、表达务实

2. **尽责性 (Conscientiousness)**：条理性、自律性、对细节的关注
   - 高分特征：表达有条理、关注细节、使用结构化表达
   - 低分特征：表达随性、话题跳跃、不拘泥于形式

3. **外向性 (Extraversion)**：社交活跃度、情绪外放程度
   - 高分特征：话多、使用感叹词、主动分享、热情表达
   - 低分特征：简洁、内敛、被动回应、情绪表达克制

4. **宜人性 (Agreeableness)**：合作性、共情倾向、友善程度
   - 高分特征：使用肯定词、表达共情、回避冲突、温暖友善
   - 低分特征：直接质疑、表达异议、较少情感回应

5. **神经质 (Neuroticism)**：情绪稳定性（反向）、焦虑倾向
   - 高分特征：表达担忧、自我怀疑、负面情绪词汇多
   - 低分特征：情绪稳定、乐观积极、较少负面表达

## 语言风格评估
请额外评估以下语言风格维度：
- 正式度 (0-100)：口语化←→书面化
- 冗长度 (0-100)：简洁←→详细
- 理性度 (0-100)：感性←→理性
- 幽默度 (0-100)：严肃←→幽默
- 温暖度 (0-100)：冷淡←→温暖

## 用户聊天文本
```
{user_chat_text}
```

## 输出要求
请严格按以下 JSON 格式输出分析结果，不要包含任何其他内容：

```json
{
  "big_five": {
    "openness": {
      "score": 0,
      "evidence": ["文本证据1", "文本证据2"],
      "confidence": 0.0
    },
    "conscientiousness": {
      "score": 0,
      "evidence": ["文本证据1", "文本证据2"],
      "confidence": 0.0
    },
    "extraversion": {
      "score": 0,
      "evidence": ["文本证据1", "文本证据2"],
      "confidence": 0.0
    },
    "agreeableness": {
      "score": 0,
      "evidence": ["文本证据1", "文本证据2"],
      "confidence": 0.0
    },
    "neuroticism": {
      "score": 0,
      "evidence": ["文本证据1", "文本证据2"],
      "confidence": 0.0
    }
  },
  "language_style": {
    "formality": 0,
    "verbosity": 0,
    "rationality": 0,
    "humor": 0,
    "warmth": 0
  },
  "summary": "对用户人格特征的综合描述，50-100字",
  "overall_confidence": 0.0,
  "text_quality_assessment": "对分析文本质量的简要评估"
}
```

## 注意事项
- 只分析"用户："标记的文本，忽略"AI："标记的文本
- 基于实际语言行为评分，不要受对话内容主题的过度影响
- 如果某些维度的文本证据不足，降低该维度的置信度
- 综合置信度取各维度置信度的平均值
```

### 3.2 增量更新 Prompt

当需要基于新对话更新已有分析结果时：

```
你是一个心理学专家。之前已经基于用户的聊天文本进行过人格分析，现在有新的对话文本需要整合。

## 之前的分析结果
```json
{previous_analysis_json}
```

## 新增的聊天文本
```
{new_chat_text}
```

## 任务
请基于新增文本，更新之前的人格分析结果。更新规则：
1. 如果新文本提供了某维度的新证据，调整该维度得分
2. 调整幅度与新增文本量成正比（新文本越多，调整越大）
3. 保留之前分析中仍有参考价值的证据
4. 更新综合置信度（更多数据应提升置信度）

输出格式与初次分析相同。
```

---

## 4. 输出 JSON 格式定义

### 4.1 JSON Schema

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "ChatPersonalityAnalysis",
  "type": "object",
  "required": ["big_five", "language_style", "summary", "overall_confidence", "metadata"],
  "properties": {
    "big_five": {
      "type": "object",
      "required": ["openness", "conscientiousness", "extraversion", "agreeableness", "neuroticism"],
      "properties": {
        "openness": {
          "$ref": "#/definitions/DimensionResult"
        },
        "conscientiousness": {
          "$ref": "#/definitions/DimensionResult"
        },
        "extraversion": {
          "$ref": "#/definitions/DimensionResult"
        },
        "agreeableness": {
          "$ref": "#/definitions/DimensionResult"
        },
        "neuroticism": {
          "$ref": "#/definitions/DimensionResult"
        }
      }
    },
    "language_style": {
      "type": "object",
      "required": ["formality", "verbosity", "rationality", "humor", "warmth"],
      "properties": {
        "formality": { "type": "integer", "minimum": 0, "maximum": 100 },
        "verbosity": { "type": "integer", "minimum": 0, "maximum": 100 },
        "rationality": { "type": "integer", "minimum": 0, "maximum": 100 },
        "humor": { "type": "integer", "minimum": 0, "maximum": 100 },
        "warmth": { "type": "integer", "minimum": 0, "maximum": 100 }
      }
    },
    "summary": {
      "type": "string",
      "description": "对用户人格特征的综合描述",
      "maxLength": 200
    },
    "overall_confidence": {
      "type": "number",
      "minimum": 0.0,
      "maximum": 1.0,
      "description": "整体置信度，各维度置信度的加权平均"
    },
    "text_quality_assessment": {
      "type": "string",
      "description": "对分析所用文本质量的评估说明"
    },
    "metadata": {
      "type": "object",
      "required": ["analysis_version", "analysis_timestamp", "text_stats"],
      "properties": {
        "analysis_version": { "type": "string" },
        "analysis_timestamp": { "type": "string", "format": "date-time" },
        "text_stats": {
          "type": "object",
          "properties": {
            "total_chars": { "type": "integer" },
            "total_messages": { "type": "integer" },
            "total_rounds": { "type": "integer" },
            "quality_level": { "type": "string", "enum": ["low", "medium", "high", "excellent"] }
          }
        },
        "model_info": {
          "type": "object",
          "properties": {
            "model_name": { "type": "string" },
            "model_version": { "type": "string" }
          }
        }
      }
    }
  },
  "definitions": {
    "DimensionResult": {
      "type": "object",
      "required": ["score", "evidence", "confidence"],
      "properties": {
        "score": {
          "type": "integer",
          "minimum": 0,
          "maximum": 100,
          "description": "该维度的得分"
        },
        "evidence": {
          "type": "array",
          "items": { "type": "string" },
          "minItems": 1,
          "maxItems": 5,
          "description": "支持该评分的文本证据"
        },
        "confidence": {
          "type": "number",
          "minimum": 0.0,
          "maximum": 1.0,
          "description": "该维度评分的置信度"
        }
      }
    }
  }
}
```

### 4.2 完整输出示例

```json
{
  "big_five": {
    "openness": {
      "score": 72,
      "evidence": [
        "经常讨论哲学和社会议题，如'你觉得AI会取代人类创造力吗'",
        "使用丰富的比喻，如'生活就像一幅未完成的画'",
        "对新鲜话题表现出好奇，主动询问'你最近有看什么有意思的书吗'"
      ],
      "confidence": 0.78
    },
    "conscientiousness": {
      "score": 45,
      "evidence": [
        "回复结构较松散，较少使用分段和编号",
        "偶尔会忘记之前提到的话题",
        "表达随性自然，不过分关注格式"
      ],
      "confidence": 0.65
    },
    "extraversion": {
      "score": 58,
      "evidence": [
        "回复长度适中，偶尔使用感叹号",
        "会主动分享个人经历但不过度",
        "社交互动自然，不过分热情也不冷淡"
      ],
      "confidence": 0.72
    },
    "agreeableness": {
      "score": 80,
      "evidence": [
        "频繁使用'确实'、'你说得对'等肯定语",
        "表达不同意见时使用缓冲语，如'可能是我理解得不对，但...'",
        "对话中展现出较强的共情倾向"
      ],
      "confidence": 0.75
    },
    "neuroticism": {
      "score": 35,
      "evidence": [
        "整体情绪平稳，较少表达负面情绪",
        "偶尔表达轻微担忧，但很快恢复乐观",
        "使用'没事的'、'总会好的'等积极词汇"
      ],
      "confidence": 0.68
    }
  },
  "language_style": {
    "formality": 45,
    "verbosity": 55,
    "rationality": 60,
    "humor": 42,
    "warmth": 78
  },
  "summary": "用户表现出较高的开放性和宜人性，思维活跃且友善包容。尽责性偏低，表达随性自然。情绪整体稳定，偶尔有轻微焦虑但能自我调节。整体呈现温暖、好奇、随和的人格特征。",
  "overall_confidence": 0.72,
  "text_quality_assessment": "基于152条消息、约3200字的对话文本进行分析。文本量充足，涵盖多种话题，分析结果较为可靠。",
  "metadata": {
    "analysis_version": "1.0.0",
    "analysis_timestamp": "2026-07-06T12:00:00Z",
    "text_stats": {
      "total_chars": 3247,
      "total_messages": 152,
      "total_rounds": 76,
      "quality_level": "high"
    },
    "model_info": {
      "model_name": "qwen-plus",
      "model_version": "latest"
    }
  }
}
```

---

## 5. 置信度评分机制

### 5.1 多维置信度体系

置信度由以下因素综合决定：

```
维度置信度 = f(证据数量, 证据一致性, 文本量, 话题多样性)

整体置信度 = Σ(维度置信度 × 维度权重) / 5
```

### 5.2 证据数量因子

| 有效证据数量 | 证据因子 | 说明 |
|-------------|----------|------|
| 0 | 0.0 | 无法评分 |
| 1 | 0.3 | 单一证据，可靠性低 |
| 2 | 0.5 | 两条证据，基本可靠 |
| 3 | 0.7 | 三条证据，较为可靠 |
| 4 | 0.85 | 四条证据，可靠 |
| 5+ | 0.95 | 充分证据，高度可靠 |

### 5.3 证据一致性因子

```python
def calc_consistency_factor(evidences: list[str], score: int) -> float:
    """
    评估证据之间的一致性
    - 如果所有证据都指向同一方向，一致性高
    - 如果证据相互矛盾，一致性低
    """
    # 由 LLM 在分析时同时评估
    # 这里提供计算框架
    if len(evidences) <= 1:
        return 0.5  # 单条证据无法评估一致性

    # 在 LLM 返回结果中，每条 evidence 附带方向标记
    # direction: 1 (高分方向) 或 -1 (低分方向)
    # 一致性 = 同方向证据占比
    pass
```

### 5.4 话题多样性因子

| 话题覆盖 | 多样性因子 | 说明 |
|----------|-----------|------|
| 单一话题 | 0.5 | 仅在一个话题下的表现，可能有偏差 |
| 2-3个话题 | 0.7 | 有一定多样性 |
| 4-6个话题 | 0.85 | 较好的话题覆盖 |
| 7+个话题 | 0.95 | 丰富的话题覆盖 |

### 5.5 综合置信度计算

```python
def calc_dimension_confidence(
    evidence_count: int,
    consistency: float,
    text_quality_factor: float,
    topic_diversity: float
) -> float:
    """
    计算单个维度的置信度
    """
    evidence_factor = min(evidence_count / 5.0, 1.0) * 0.95

    # 加权平均
    confidence = (
        evidence_factor * 0.4 +
        consistency * 0.3 +
        text_quality_factor * 0.2 +
        topic_diversity * 0.1
    )

    return min(confidence, 1.0)

def calc_overall_confidence(dimension_confidences: list[float]) -> float:
    """
    计算整体置信度（各维度置信度的算术平均）
    """
    return sum(dimension_confidences) / len(dimension_confidences)
```

### 5.6 置信度分级展示

| 置信度范围 | 等级 | UI展示建议 |
|-----------|------|-----------|
| 0.8 - 1.0 | 高 | 绿色标识，结果较为可靠 |
| 0.6 - 0.8 | 中 | 黄色标识，结果仅供参考 |
| 0.4 - 0.6 | 低 | 橙色标识，建议积累更多对话 |
| 0.0 - 0.4 | 极低 | 红色标识，当前文本不足以分析 |
| 0.0 | 无数据 | 灰色标识，需要先进行对话 |

---

## 6. 分析流程设计

### 6.1 同步分析流程

```
用户触发分析
    │
    ▼
读取聊天记录 → 预处理 → 质量检查
    │                        │
    │                   不满足门槛
    │                        │
    ▼                        ▼
文本量充足?            返回提示信息
    │                 "需要更多对话"
    ├── 是 ──▶ 构建分析 Prompt
    │              │
    │              ▼
    │         调用 LLM 分析
    │              │
    │              ▼
    │         解析 JSON 结果
    │              │
    │              ▼
    │         验证结果合法性
    │              │
    │              ▼
    │         计算综合置信度
    │              │
    │              ▼
    │         存储分析结果
    │              │
    │              ▼
    │         返回给用户
    │
    └── 否 ──▶ 提示需要更多文本
```

### 6.2 增量分析流程

```
新对话产生
    │
    ▼
检查上次分析时间
    │
    ├── < 24小时 ──▶ 跳过（分析过于频繁）
    │
    └── >= 24小时 ──▶ 检查新增文本量
                        │
                   新增 >= 500字?
                        │
                   是 ──▶ 增量分析 Prompt
                    │         │
                    │         ▼
                    │     LLM 分析
                    │         │
                    │         ▼
                    │     合并结果
                    │         │
                    │         ▼
                    │     更新人格画像
                    │
                   否 ──▶ 累积等待
```

### 6.3 触发方式

| 触发方式 | 说明 |
|----------|------|
| 手动触发 | 用户在设置中点击"分析我的聊天风格" |
| 定时触发 | 每积累 2000 字新对话自动触发（需用户授权） |
| 首次引导 | 新用户完成 10 轮对话后，引导进行人格分析 |
| 角色匹配 | 用户浏览预设角色时，分析对话风格推荐匹配角色 |

---

## 7. 质量保障策略

### 7.1 结果验证规则

```python
def validate_analysis_result(result: dict) -> dict:
    """
    验证 LLM 返回的分析结果是否合法
    """
    errors = []
    warnings = []

    # 1. 分数范围检查
    for dim in ['openness', 'conscientiousness', 'extraversion',
                'agreeableness', 'neuroticism']:
        score = result['big_five'][dim]['score']
        if not (0 <= score <= 100):
            errors.append(f"{dim} 分数 {score} 超出 0-100 范围")

    # 2. 证据检查
    for dim in ['openness', 'conscientiousness', 'extraversion',
                'agreeableness', 'neuroticism']:
        evidence = result['big_five'][dim].get('evidence', [])
        if len(evidence) == 0:
            warnings.append(f"{dim} 缺少文本证据")

    # 3. 置信度检查
    for dim in ['openness', 'conscientiousness', 'extraversion',
                'agreeableness', 'neuroticism']:
        conf = result['big_five'][dim]['confidence']
        if not (0.0 <= conf <= 1.0):
            errors.append(f"{dim} 置信度 {conf} 超出 0-1 范围")

    # 4. 语言风格检查
    for style in ['formality', 'verbosity', 'rationality', 'humor', 'warmth']:
        value = result['language_style'].get(style, -1)
        if not (0 <= value <= 100):
            errors.append(f"语言风格 {style} 值 {value} 超出范围")

    return {
        'valid': len(errors) == 0,
        'errors': errors,
        'warnings': warnings
    }
```

### 7.2 多模型交叉验证（可选）

对于重要分析（如用户人格画像），可使用多模型交叉验证：

```
同一文本 → Model A 分析 → 结果 A
同一文本 → Model B 分析 → 结果 B
同一文本 → Model C 分析 → 结果 C
                │
                ▼
          计算三个结果的标准差
                │
       标准差 < 10? ──▶ 结果可靠，取均值
                │
       标准差 >= 10? ──▶ 标记为低置信度，建议更多数据
```

### 7.3 异常检测

```python
def detect_anomalies(current_analysis: dict, history: list[dict]) -> dict:
    """
    检测分析结果是否异常（与历史趋势不符）
    """
    if len(history) < 2:
        return {'anomaly': False}

    anomalies = []
    for dim in ['openness', 'conscientiousness', 'extraversion',
                'agreeableness', 'neuroticism']:
        current_score = current_analysis['big_five'][dim]['score']
        historical_scores = [h['big_five'][dim]['score'] for h in history]
        avg = sum(historical_scores) / len(historical_scores)
        std = (sum((s - avg) ** 2 for s in historical_scores) / len(historical_scores)) ** 0.5

        # 如果当前分数偏离历史均值超过 2 个标准差
        if std > 0 and abs(current_score - avg) > 2 * std:
            anomalies.append({
                'dimension': dim,
                'current': current_score,
                'historical_avg': round(avg, 1),
                'deviation': round(abs(current_score - avg), 1),
                'severity': 'high' if abs(current_score - avg) > 3 * std else 'medium'
            })

    return {
        'anomaly': len(anomalies) > 0,
        'anomalies': anomalies,
        'recommendation': '建议使用更多文本重新分析' if len(anomalies) > 2 else None
    }
```

---

## 8. 设计决策记录

### D-401: 仅分析用户文本

**决策**：人格分析仅针对用户发言，不包括 AI 回复
**理由**：
- AI 的人格由 System Prompt 控制，分析 AI 文本无意义
- 用户人格分析用于个性化推荐和角色匹配
- 避免 AI 文本污染用户的人格画像

### D-402: 使用 LLM 而非传统 NLP

**决策**：使用 LLM 进行人格分析，而非传统词典法或机器学习分类器
**理由**：
- LLM 能理解语境和语义，传统方法只能做词频统计
- LLM 能提供文本证据，增加可解释性
- 在移动端调用本地或云端 LLM 统一技术栈
- 传统方法（如 LIWC）需要大量标注数据和领域适配

### D-403: 增量分析而非全量重分析

**决策**：新增对话触发增量更新，而非每次全量重分析
**理由**：
- 全量重分析 token 消耗大（历史对话可能很长）
- 增量分析更快，用户体验更好
- 保留历史分析结果，便于追踪人格变化趋势

### D-404: 多维置信度而非单一分数

**决策**：每个维度独立计算置信度，综合置信度取均值
**理由**：
- 不同维度的文本证据量可能不同
- 某些维度可能在对话中表现不明显
- 用户可以了解哪些维度的分析更可靠

### D-405: 本地分析 + 隐私保护

**决策**：分析 Prompt 在本地构建，用户可选择使用本地模型或云端 API
**理由**：
- 聊天文本属于敏感隐私数据
- 「不忘」的核心理念是本地存储
- 用户应能控制自己的数据是否发送到云端
