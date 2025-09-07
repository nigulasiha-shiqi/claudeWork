import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext';
import { useEffect, useState, useCallback } from 'react';
import {
  $getSelection,
  $isRangeSelection,
  $createTextNode,
  KEY_DOWN_COMMAND,
  COMMAND_PRIORITY_LOW,
} from 'lexical';
import { $createVariableMentionNode } from './VariableMentionNode';
import type { VariableItem } from '../types';

interface SimpleVariablePluginProps {
  variables: VariableItem[];
}

interface PopupProps {
  isOpen: boolean;
  variables: VariableItem[];
  onSelect: (variable: VariableItem) => void;
  onClose: () => void;
}

function SimplePopup({ isOpen, variables, onSelect, onClose }: PopupProps) {
  console.log('SimplePopup render:', isOpen, variables.length);
  
  if (!isOpen) return null;

  // 使用 Portal 渲染到 body
  return (
    <div
      className="fixed inset-0 z-[99999] flex items-center justify-center bg-black/50 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        className="bg-white border-4 border-red-500 rounded-lg shadow-2xl max-h-96 overflow-y-auto min-w-[400px] max-w-[500px]"
        onClick={(e) => e.stopPropagation()}
      >
      <div className="p-4">
        <div className="text-lg font-bold text-red-600 mb-3">🔥 选择变量：({variables.length} 个)</div>
        <div className="space-y-1">
          {variables.map((variable) => (
            <div
              key={variable.key}
              className="px-3 py-3 cursor-pointer rounded border border-gray-200 hover:bg-blue-50 hover:border-blue-300 flex items-center justify-between transition-colors"
              onClick={() => onSelect(variable)}
            >
              <span className="font-medium">{variable.label}</span>
              <code className="text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded">
                ${'{'}
                {variable.key}
                {'}'}
              </code>
            </div>
          ))}
        </div>
        <div className="mt-3 pt-3 border-t border-gray-100">
          <button
            onClick={onClose}
            className="w-full px-3 py-2 text-sm text-gray-600 bg-gray-100 rounded hover:bg-gray-200"
          >
            取消
          </button>
        </div>
      </div>
      </div>
    </div>
  );
}

export default function SimpleVariablePlugin({ variables }: SimpleVariablePluginProps) {
  const [editor] = useLexicalComposerContext();
  const [isPopupOpen, setIsPopupOpen] = useState(false);

  const openPopup = useCallback(() => {
    console.log('Opening popup with variables:', variables.length);
    setIsPopupOpen(true);
  }, [variables.length]);

  const closePopup = useCallback(() => {
    console.log('Closing popup');
    setIsPopupOpen(false);
  }, []);

  const insertVariable = useCallback((variable: VariableItem) => {
    console.log('Inserting variable:', variable);
    editor.update(() => {
      const selection = $getSelection();
      if ($isRangeSelection(selection)) {
        // 插入变量节点
        const variableMentionNode = $createVariableMentionNode(variable.key, variable.label);
        selection.insertNodes([variableMentionNode]);
        
        // 在变量后添加一个空格
        const spaceNode = $createTextNode(' ');
        selection.insertNodes([spaceNode]);
      }
    });
    closePopup();
  }, [editor, closePopup]);

  // 键盘事件监听
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      console.log('Key pressed:', event.key);
      
      if (event.key === '/' && !isPopupOpen) {
        console.log('Slash detected, opening popup');
        // 延迟一点打开弹窗，让斜杠字符先插入
        setTimeout(() => {
          openPopup();
        }, 50);
        return false;
      }
      
      if (event.key === 'Escape' && isPopupOpen) {
        closePopup();
        return true;
      }
      
      return false;
    };

    const unregister = editor.registerCommand(
      KEY_DOWN_COMMAND,
      handleKeyDown,
      COMMAND_PRIORITY_LOW
    );

    return unregister;
  }, [editor, isPopupOpen, openPopup, closePopup]);

  // 右击事件监听
  useEffect(() => {
    const handleRightClick = (event: MouseEvent) => {
      event.preventDefault();
      console.log('Right click detected');
      openPopup();
    };

    const editorElement = editor.getRootElement();
    if (editorElement) {
      editorElement.addEventListener('contextmenu', handleRightClick);
      return () => {
        editorElement.removeEventListener('contextmenu', handleRightClick);
      };
    }
  }, [editor, openPopup]);

  return (
    <div>
      <SimplePopup
        isOpen={isPopupOpen}
        variables={variables}
        onSelect={insertVariable}
        onClose={closePopup}
      />
      
      {/* 添加一个测试按钮 */}
      <button
        onClick={openPopup}
        className="fixed top-4 right-4 z-[10000] px-4 py-2 bg-red-500 hover:bg-red-600 text-white font-bold text-sm rounded-lg shadow-lg border-2 border-white"
      >
        🧪 测试弹窗
      </button>
      
      {/* 调试信息 */}
      <div className="fixed top-20 right-4 z-[10000] bg-yellow-100 border border-yellow-400 p-2 rounded text-xs">
        弹窗状态: {isPopupOpen ? '✅ 打开' : '❌ 关闭'} | 变量数: {variables.length}
      </div>
      
      {/* 如果弹窗应该显示但看不见，显示紧急弹窗 */}
      {isPopupOpen && (
        <div className="fixed top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-[99999] bg-red-600 text-white p-8 rounded-xl shadow-2xl border-4 border-yellow-400">
          <div className="text-center">
            <div className="text-2xl font-bold mb-4">🚨 紧急弹窗测试 🚨</div>
            <div className="text-lg mb-4">如果你看到这个，说明弹窗能正常显示</div>
            <div className="space-y-2">
              {variables.slice(0, 3).map((variable) => (
                <button
                  key={variable.key}
                  onClick={() => insertVariable(variable)}
                  className="block w-full px-4 py-2 bg-white text-red-600 rounded hover:bg-gray-100"
                >
                  {variable.label} (${variable.key})
                </button>
              ))}
            </div>
            <button
              onClick={closePopup}
              className="mt-4 px-4 py-2 bg-yellow-400 text-black rounded font-bold"
            >
              关闭测试弹窗
            </button>
          </div>
        </div>
      )}
    </div>
  );
}