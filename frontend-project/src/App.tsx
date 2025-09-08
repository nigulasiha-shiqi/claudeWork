import { useState, useRef, useEffect } from 'react'
import VariableEditor, { type VariableEditorRef } from './components/VariableEditor'
import PreviewEditor, { type PreviewEditorRef } from './components/PreviewEditor'
import type { VariableItem } from './types'

function App() {
  const editorRef = useRef<VariableEditorRef>(null);
  const previewRef = useRef<PreviewEditorRef>(null);
  const [content, setContent] = useState<string>('');
  const [previewContent, setPreviewContent] = useState<string>('');
  const isUpdatingFromPreview = useRef(false);
  const updateTimer = useRef<NodeJS.Timeout | null>(null);

  // 测试用的变量数据
  const variables: VariableItem[] = [
    { key: 'userName', label: '用户名' },
    { key: 'userEmail', label: '用户邮箱' },
    { key: 'currentDate', label: '当前日期' },
    { key: 'companyName', label: '公司名称' },
    { key: 'projectName', label: '项目名称' },
    { key: 'version', label: '版本号' },
    { key: 'department', label: '部门' },
    { key: 'manager', label: '经理' },
  ];

  // 测试用的初始文本
  const initialText = '欢迎您，${userName}！您的邮箱是 ${userEmail}。今天是 ${currentDate}，您在 ${companyName} 的 ${department} 部门工作。';

  // 触发字符配置
  const triggerChars = ['/', '$'];

  // 初始化预览内容
  useEffect(() => {
    // 下方编辑器显示变量格式，所以直接使用初始文本
    setPreviewContent(initialText);
  }, []);

  // 清理定时器
  useEffect(() => {
    return () => {
      if (updateTimer.current) {
        clearTimeout(updateTimer.current);
      }
    };
  }, []);

  // 处理主编辑器内容变化（序列化格式，用于下方预览编辑器显示）
  const handleMainEditorChange = (newContent: string) => {
    // 只有不是从预览编辑器触发的更新才设置预览内容
    if (!isUpdatingFromPreview.current) {
      setPreviewContent(newContent);
    }
  };

  // 处理主编辑器预览内容变化（人类可读格式，暂时不使用）
  const handleMainEditorPreviewChange = (previewContent: string) => {
    // 这个回调现在不使用，因为我们要在下方显示变量格式
  };

  // 处理预览编辑器内容变化（实时同步）
  const handlePreviewChange = (newContent: string) => {
    // 清除之前的定时器
    if (updateTimer.current) {
      clearTimeout(updateTimer.current);
    }
    
    // 标记正在从预览编辑器更新
    isUpdatingFromPreview.current = true;
    
    // 使用防抖，减少更新频率
    updateTimer.current = setTimeout(() => {
      if (editorRef.current) {
        editorRef.current.setContent(newContent);
      }
      
      // 延迟重置标记
      setTimeout(() => {
        isUpdatingFromPreview.current = false;
      }, 50);
    }, 300); // 300ms防抖，在输入体验和实时性之间找平衡
  };

  // 处理预览编辑器失去焦点时的同步（备份方案）
  const handlePreviewBlur = (newContent: string) => {
    // 如果实时同步有问题，这里作为备份
    if (!isUpdatingFromPreview.current) {
      handlePreviewChange(newContent);
    }
  };

  const handleGetContent = () => {
    if (editorRef.current) {
      const content = editorRef.current.getContent();
      setContent(content);
    }
  };

  const handleSetContent = () => {
    if (editorRef.current) {
      const newContent = '这是一个测试：${projectName} 版本 ${version} 由 ${manager} 负责管理。';
      editorRef.current.setContent(newContent);
    }
  };

  const handleClearContent = () => {
    if (editorRef.current) {
      editorRef.current.setContent('');
    }
  };

  const handleTestPopup = () => {
    if (editorRef.current) {
      editorRef.current.testPopup();
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-4xl mx-auto px-4">
        <header className="text-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">
            变量富文本编辑器测试
          </h1>
          <p className="text-gray-600">
            支持变量占位符的 Lexical 编辑器
          </p>
        </header>
        
        <main className="space-y-8">
          {/* 使用说明 */}
          <section className="bg-blue-50 rounded-lg p-6">
            <h3 className="text-lg font-semibold text-blue-800 mb-3">使用说明</h3>
            <ul className="text-blue-700 space-y-1 text-sm">
              <li>• 在编辑器中输入文本，按 <kbd className="bg-blue-200 px-1 rounded">/</kbd> 或 <kbd className="bg-blue-200 px-1 rounded">$</kbd> 键可以弹出变量选择列表</li>
              <li>• 使用 <kbd className="bg-blue-200 px-1 rounded">↑</kbd> <kbd className="bg-blue-200 px-1 rounded">↓</kbd> 键导航，<kbd className="bg-blue-200 px-1 rounded">Enter</kbd> 键选择</li>
              <li>• 支持搜索过滤：按触发字符后继续输入可筛选变量</li>
              <li>• 初始文本中的 <code className="bg-blue-200 px-1 rounded">{'${key}'}</code> 会自动转换为对应的变量标签</li>
            </ul>
          </section>

          {/* 变量编辑器 */}
          <section className="bg-white rounded-lg shadow-sm p-6">
            <h2 className="text-xl font-semibold mb-4">变量编辑器</h2>
            <VariableEditor
              ref={editorRef}
              initialText={initialText}
              variables={variables}
              triggerChars={triggerChars}
              onChange={handleMainEditorChange}
              className="min-h-40"
              placeholder="输入文本，按 / 或 $ 选择变量..."
            />
            
            <div className="mt-4 flex gap-2 flex-wrap">
              <button
                onClick={handleGetContent}
                className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition-colors"
              >
                获取内容
              </button>
              <button
                onClick={handleTestPopup}
                className="px-4 py-2 bg-purple-600 text-white rounded hover:bg-purple-700 transition-colors"
              >
                🧪 测试弹窗
              </button>
              <button
                onClick={handleSetContent}
                className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 transition-colors"
              >
                设置测试内容
              </button>
              <button
                onClick={handleClearContent}
                className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700 transition-colors"
              >
                清空内容
              </button>
            </div>
          </section>

          {/* 变量格式编辑器 */}
          <section className="bg-white rounded-lg shadow-sm p-6">
            <h2 className="text-xl font-semibold mb-4">变量格式编辑器（双向同步）</h2>
            <p className="text-sm text-gray-600 mb-3">
              显示上方编辑器的变量格式（${'${key}'}），也可以直接编辑变量格式内容，停止输入300ms后会同步到上方编辑器
            </p>
            <PreviewEditor
              ref={previewRef}
              variables={variables}
              value={previewContent}
              onChange={handlePreviewChange}
              onBlur={handlePreviewBlur}
              className="min-h-40"
              placeholder="这里显示变量格式内容..."
            />
          </section>

          {/* 可用变量列表 */}
          <section className="bg-white rounded-lg shadow-sm p-6">
            <h2 className="text-xl font-semibold mb-4">可用变量</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
              {variables.map(variable => (
                <div 
                  key={variable.key}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                >
                  <span className="font-medium">{variable.label}</span>
                  <code className="text-sm text-gray-600 bg-gray-200 px-2 py-1 rounded">
                    {'${' + variable.key + '}'}
                  </code>
                </div>
              ))}
            </div>
          </section>

          {/* 输出内容 */}
          {content && (
            <section className="bg-white rounded-lg shadow-sm p-6">
              <h2 className="text-xl font-semibold mb-4">序列化输出</h2>
              <div className="bg-gray-100 p-4 rounded-lg">
                <pre className="text-sm text-gray-800 whitespace-pre-wrap">{content}</pre>
              </div>
              <p className="text-sm text-gray-600 mt-2">
                这是编辑器内容序列化后的结果，变量已转换为 {'${key}'} 格式
              </p>
            </section>
          )}
        </main>
      </div>
    </div>
  )
}

export default App
