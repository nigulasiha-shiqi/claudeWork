# 变量富文本编辑器

基于 React + TypeScript + Lexical + Tailwind CSS 4.x 构建的变量占位符富文本编辑器，支持双向同步预览。

## 🎯 核心功能

### 1. 变量编辑器（上方）
- **显示格式**: 标签格式，如 "用户名"、"部门" 等人类可读文本
- **变量选择**: 支持 `/` 和 `$` 触发变量选择弹窗
- **弹窗功能**: 
  - 实时搜索过滤变量
  - 键盘导航（↑↓ 选择，Enter 确认，Esc 取消）
  - 鼠标点击选择
- **变量标签**: 蓝色标签显示，带删除按钮（×）
- **智能定位**: 弹窗准确显示在光标位置

### 2. 变量格式编辑器（下方）
- **显示格式**: 变量格式，如 `${userName}`、`${department}` 等
- **实时同步**: 上方编辑器变化时，下方立即更新
- **防抖同步**: 编辑下方内容时，停止输入 300ms 后同步到上方
- **双向编辑**: 可直接编辑变量格式，支持手动输入 `${key}` 语法

### 3. 变量管理
- **预定义变量**: 用户名、邮箱、日期、公司、项目、版本、部门、经理等
- **变量映射**: key-label 对应关系，如 `userName` → "用户名"
- **格式转换**: 
  - 标签 → 变量: "用户名" → `${userName}`
  - 变量 → 标签: `${userName}` → "用户名"

## 🛠 技术实现

### 核心技术栈
- **React 18** - UI 框架
- **TypeScript** - 类型安全
- **Lexical** - Facebook 开源的富文本编辑器框架
- **Tailwind CSS 4.x** - 样式框架
- **Vite** - 构建工具

### 关键组件架构

#### 1. VariableEditor 主编辑器
- 基于 Lexical 的富文本编辑器
- 自定义 DecoratorNode (VariableMentionNode) 渲染变量标签
- 支持内容序列化和反序列化
- 提供 ref 接口供外部操作

#### 2. EnhancedVariablePlugin 变量插件
- **可配置触发字符**: 支持 `['/','$']` 等任意字符数组
- **智能弹窗定位**: 使用 DOM Range API 精确定位光标
- **键盘事件处理**: 完整的键盘导航支持
- **搜索功能**: 实时过滤变量列表

#### 3. PreviewEditor 预览编辑器
- 轻量级 textarea 实现
- 防抖机制避免频繁更新
- 焦点管理防止光标跳转

#### 4. 数据转换层
```typescript
// 工具函数
convertVariablesToLabels()  // ${userName} → "用户名"
convertLabelsToVariables()  // "用户名" → ${userName}
serializeEditorContent()    // 序列化为变量格式
serializeEditorContentAsLabels() // 序列化为标签格式
```

### 双向同步机制

#### 上方 → 下方 (实时)
```
上方编辑 → onChange → serializeEditorContent() → setPreviewContent()
```

#### 下方 → 上方 (防抖)
```
下方输入 → onChange → 300ms防抖 → setContent() → 更新上方编辑器
```

#### 防抖实现
- 使用 `setTimeout` + `clearTimeout` 实现
- 每次输入重置计时器，停止输入 300ms 后执行更新
- 防止频繁更新导致的性能问题和焦点干扰

## 📝 使用方式

### 基本使用
```tsx
import VariableEditor from './components/VariableEditor'
import PreviewEditor from './components/PreviewEditor'

const variables = [
  { key: 'userName', label: '用户名' },
  { key: 'userEmail', label: '用户邮箱' }
]

function App() {
  const [previewContent, setPreviewContent] = useState('')
  
  return (
    <>
      <VariableEditor 
        variables={variables}
        triggerChars={['/', '$']}
        onChange={setPreviewContent}
        initialText="欢迎您，${userName}！"
      />
      <PreviewEditor 
        variables={variables}
        value={previewContent}
        onBlur={handleSync}
      />
    </>
  )
}
```

### 变量操作
1. **插入变量**: 输入 `/` 或 `$` 触发选择弹窗
2. **搜索变量**: 在弹窗中输入关键词过滤
3. **删除变量**: 点击变量标签右上角的 × 按钮
4. **导航选择**: 使用 ↑↓ 键选择，Enter 确认

### 内容获取
```typescript
// 获取序列化内容 (${key} 格式)
const content = editorRef.current?.getContent()

// 获取人类可读内容 (标签格式)  
const readableContent = editorRef.current?.getReadableContent()
```

## 🔧 配置选项

### VariableEditor Props
```typescript
interface VariableEditorProps {
  initialText?: string          // 初始文本
  variables: VariableItem[]     // 变量列表  
  triggerChars?: string[]       // 触发字符，默认 ['/']
  onChange?: (content: string) => void  // 内容变化回调
  placeholder?: string          // 占位符文本
  className?: string           // 样式类名
}
```

### 变量数据结构
```typescript
interface VariableItem {
  key: string    // 变量键，如 'userName'
  label: string  // 显示标签，如 '用户名'
}
```

### 触发字符配置
```typescript
// 支持多个触发字符
triggerChars={['/', '$', '@']}

// 单个触发字符
triggerChars={['/']}
```

## 🎨 样式定制

### Tailwind CSS 4.x 配置
使用最新的 CSS-first 配置方式：
```typescript
// vite.config.ts
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),  // 使用 @tailwindcss/vite 插件
  ],
})
```

### 自定义主题
```css
/* src/index.css */
@import "tailwindcss";

/* 变量标签样式 */
.variable-mention {
  @apply bg-blue-100 text-blue-700 px-2 py-0.5 rounded;
}

/* 弹窗样式 */
.variable-popup {
  @apply bg-white border border-gray-300 rounded-lg shadow-xl;
}
```

## 🔍 核心特性解析

### 1. 光标定位算法
使用 DOM Range API 精确计算光标位置：
```typescript
function getLexicalCaretPosition(editor): CaretPosition {
  // 1. 获取 Lexical 选区
  // 2. 转换为 DOM Range  
  // 3. 插入临时标记元素
  // 4. 获取标记元素坐标
  // 5. 清理标记元素
  return { x, y }
}
```

### 2. 变量节点实现
```typescript
class VariableMentionNode extends DecoratorNode {
  // 渲染为 React 组件
  decorate(): JSX.Element {
    return <VariableTag label={this.label} onDelete={this.handleDelete} />
  }
  
  // 序列化为变量格式
  getTextContent(): string {
    return this.label  // 返回标签用于显示
  }
}
```

### 3. 防抖同步机制
```typescript
const handlePreviewChange = (newContent: string) => {
  // 清除之前的定时器
  if (updateTimer.current) {
    clearTimeout(updateTimer.current)
  }
  
  // 设置新的防抖定时器
  updateTimer.current = setTimeout(() => {
    editorRef.current?.setContent(newContent)
  }, 300) // 300ms 防抖延迟
}
```

## 🐛 问题解决

### 1. 光标跳转问题
**问题**: 编辑下方编辑器时光标跳到上方  
**解决**: 使用防抖机制 + 焦点检测，避免在用户输入时更新主编辑器

### 2. 重复内容问题  
**问题**: 预览编辑器显示重复的变量标签  
**解决**: 修复序列化函数，避免二次转换已经是标签格式的内容

### 3. Tailwind CSS 4.x 配置
**问题**: Vite 中 Tailwind 样式不生效  
**解决**: 使用 `@tailwindcss/vite` 插件替代传统配置方式

### 4. 触发字符扩展
**问题**: 只支持 `/` 触发变量选择  
**解决**: 重构为可配置数组，支持任意字符组合 `['/','$']`

## 📊 性能优化

### 1. 防抖优化
- 300ms 防抖延迟，减少不必要的更新
- 避免每次按键都触发编辑器重新渲染

### 2. 事件优化
- 使用 `useCallback` 缓存事件处理函数
- 合理使用 `useEffect` 依赖数组

### 3. 渲染优化
- DecoratorNode 复用，避免重复创建组件
- Portal 渲染弹窗，避免影响主编辑器性能

## 🚀 快速开始

### 安装依赖
```bash
npm install
# 或
pnpm install
```

### 启动开发服务器
```bash
npm run dev
# 或  
pnpm dev
```

### 构建生产版本
```bash
npm run build
# 或
pnpm build
```

## 📁 项目结构
```
src/
├── components/
│   ├── VariableEditor.tsx           # 主编辑器组件
│   ├── EnhancedVariablePlugin.tsx   # 变量插件
│   ├── VariableMentionNode.tsx      # 变量节点定义
│   └── PreviewEditor.tsx            # 预览编辑器
├── utils/
│   ├── variableConverter.ts         # 格式转换工具
│   ├── textParser.ts               # 文本解析工具
│   └── contentSerializer.ts        # 内容序列化工具
├── types/
│   └── index.ts                    # 类型定义
└── App.tsx                         # 主应用组件
```

## 📄 更新日志

### v1.0.0 (最新)
- ✅ 实现基础变量编辑器功能
- ✅ 支持多触发字符配置 `['/','$']`
- ✅ 添加双向同步预览编辑器  
- ✅ 实现防抖同步机制 (300ms)
- ✅ 解决光标跳转问题
- ✅ 修复内容重复显示问题
- ✅ 配置 Tailwind CSS 4.x 支持
- ✅ 完善变量搜索和键盘导航
- ✅ 优化弹窗定位算法
- ✅ 添加变量删除功能

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📞 支持

如有问题或建议，请通过以下方式联系：
- 创建 Issue
- 发送邮件
- 参与讨论

---

**注**: 本项目持续维护和更新中，欢迎反馈和建议！