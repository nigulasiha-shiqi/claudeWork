import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext';
import { useEffect, useState, useCallback, useRef } from 'react';
import { createPortal } from 'react-dom';
import {
  $getSelection,
  $isRangeSelection,
  $createTextNode,
  KEY_DOWN_COMMAND,
  COMMAND_PRIORITY_LOW,
  $isTextNode,
} from 'lexical';
import { $createVariableMentionNode } from './VariableMentionNode';
import type { VariableItem } from '../types';

interface EnhancedVariablePluginProps {
  variables: VariableItem[];
  triggerChars?: string[];
}

interface CaretPosition {
  x: number;
  y: number;
}

interface PopupProps {
  isOpen: boolean;
  variables: VariableItem[];
  position: CaretPosition | null;
  searchQuery: string;
  onSelect: (variable: VariableItem) => void;
  onClose: () => void;
  selectedIndex: number;
  onHover: (index: number) => void;
  onSearchChange: (query: string) => void;
}

// 获取 Lexical 编辑器中的光标位置
function getLexicalCaretPosition(editor: any): CaretPosition | null {
  let position: CaretPosition | null = null;
  
  editor.getEditorState().read(() => {
    const selection = $getSelection();
    if ($isRangeSelection(selection)) {
      const domSelection = window.getSelection();
      if (domSelection && domSelection.rangeCount > 0) {
        const range = domSelection.getRangeAt(0);
        
        // 克隆 range 并折叠到光标位置
        const caretRange = range.cloneRange();
        caretRange.collapse(true);
        
        // 创建一个临时的标记元素
        const marker = document.createElement('span');
        marker.style.position = 'absolute';
        marker.style.height = '1px';
        marker.style.width = '1px';
        marker.style.visibility = 'visible'; // 设为可见以便调试
        marker.style.backgroundColor = 'red';
        marker.style.zIndex = '9999';
        marker.textContent = '|';
        
        // 在光标位置插入标记
        try {
          caretRange.insertNode(marker);
          
          // 获取标记元素的位置
          const markerRect = marker.getBoundingClientRect();
          position = {
            x: markerRect.left + window.scrollX,
            y: markerRect.bottom + window.scrollY + 2
          };
          
          console.log('Lexical caret position:', position, 'marker rect:', markerRect);
          
          // 清理标记元素
          setTimeout(() => {
            if (marker.parentNode) {
              marker.parentNode.removeChild(marker);
            }
          }, 100); // 延迟删除以便调试
          
        } catch (error) {
          console.error('Failed to insert marker:', error);
          // 清理标记元素
          if (marker.parentNode) {
            marker.parentNode.removeChild(marker);
          }
        }
      }
    }
  });
  
  return position;
}

function VariablePopup({ isOpen, variables, position, searchQuery, onSelect, onClose, selectedIndex, onHover, onSearchChange }: PopupProps) {
  const popupRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // 过滤变量列表
  const filteredVariables = variables.filter(variable =>
    variable.label.toLowerCase().includes(searchQuery.toLowerCase()) ||
    variable.key.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // 弹窗打开时自动聚焦到搜索框
  useEffect(() => {
    if (isOpen && inputRef.current) {
      // 延迟聚焦，确保弹窗已完全渲染
      setTimeout(() => {
        inputRef.current?.focus();
      }, 100);
    }
  }, [isOpen]);

  // 点击外部关闭
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (popupRef.current && !popupRef.current.contains(event.target as Node)) {
        onClose();
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen, onClose]);

  if (!isOpen || !position) {
    return null;
  }

  return createPortal(
    <div
      ref={popupRef}
      className="absolute bg-white border border-gray-400 rounded-lg shadow-xl max-h-64 min-w-[200px] max-w-[300px] z-[9999]"
      style={{
        left: `${position.x}px`,
        top: `${position.y}px`,
      }}
    >
      {/* 搜索输入框 */}
      <div className="p-3 border-b border-gray-200">
        <input
          ref={inputRef}
          type="text"
          placeholder="搜索变量..."
          value={searchQuery}
          onChange={(e) => {
            onSearchChange(e.target.value);
          }}
          className="w-full px-3 py-2 text-sm border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          autoFocus
        />
      </div>
      
      {/* 搜索结果计数 */}
      {searchQuery && (
        <div className="px-3 py-1 text-xs text-gray-500 bg-gray-50 border-b border-gray-100">
          找到 {filteredVariables.length} 个变量
        </div>
      )}
      
      {/* 变量列表 */}
      <div className="p-2 max-h-40 overflow-y-auto">
        {filteredVariables.length === 0 ? (
          <div className="px-3 py-4 text-center text-sm text-gray-500">
            {searchQuery ? `未找到包含 "${searchQuery}" 的变量` : '暂无变量'}
          </div>
        ) : (
          filteredVariables.map((variable, index) => {
          const isSelected = index === selectedIndex;
          return (
            <div
              key={variable.key}
              className={`px-3 py-2 cursor-pointer text-sm transition-all duration-200 select-none border ${
                isSelected
                  ? 'bg-blue-100 text-blue-800 border-blue-300'
                  : 'text-gray-800 bg-white border-transparent hover:bg-yellow-100 hover:text-blue-700 hover:border-blue-300'
              }`}
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                console.log('Clicked variable:', variable.label);
                onSelect(variable);
              }}
              onMouseEnter={(e) => {
                e.preventDefault();
                e.stopPropagation();
                console.log('Mouse entered item:', index, variable.label);
                onHover(index);
              }}
              onMouseMove={(e) => {
                e.preventDefault();
                e.stopPropagation();
              }}
            >
              <div className="flex items-center justify-between">
                <span className="font-medium truncate">{variable.label}</span>
                <span className={`text-xs ml-2 font-mono shrink-0 ${
                  isSelected
                    ? 'text-blue-600'
                    : 'text-gray-500'
                }`}>
                  ${'{'}
                  {variable.key}
                  {'}'}
                </span>
              </div>
            </div>
          );
          })
        )}
      </div>
    </div>,
    document.body
  );
}

export default function EnhancedVariablePlugin({ variables, triggerChars = ['/'] }: EnhancedVariablePluginProps) {
  const [editor] = useLexicalComposerContext();
  const [isPopupOpen, setIsPopupOpen] = useState(false);
  const [popupPosition, setPopupPosition] = useState<CaretPosition | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [isTriggeredByTriggerChar, setIsTriggeredByTriggerChar] = useState(false);
  const [triggerChar, setTriggerChar] = useState<string>('');

  const closePopup = useCallback(() => {
    setIsPopupOpen(false);
    setPopupPosition(null);
    setSearchQuery('');
    setSelectedIndex(0);
    setIsTriggeredByTriggerChar(false);
    setTriggerChar('');
  }, []);

  const openPopup = useCallback((triggeredByChar = false, char = '') => {
    console.log('openPopup called, triggeredByChar:', triggeredByChar, 'char:', char);
    
    // 使用 setTimeout 确保斜杠字符已插入
    setTimeout(() => {
      const position = getLexicalCaretPosition(editor);
      console.log('Got position:', position);
      
      if (position) {
        setPopupPosition(position);
        setIsPopupOpen(true);
        setSearchQuery('');
        setSelectedIndex(0);
        setIsTriggeredByTriggerChar(triggeredByChar);
        setTriggerChar(char);
      } else {
        console.warn('Could not get caret position');
      }
    }, triggeredByChar ? 50 : 10); // 触发字符时延迟更长
  }, [editor]);

  const insertVariable = useCallback((variable: VariableItem) => {
    editor.update(() => {
      const selection = $getSelection();
      if ($isRangeSelection(selection)) {
        // 如果是触发字符触发的，需要删除触发字符
        if (isTriggeredByTriggerChar) {
          const anchorNode = selection.anchor.getNode();
          if ($isTextNode(anchorNode)) {
            const text = anchorNode.getTextContent();
            const offset = selection.anchor.offset;
            
            // 查找最近的触发字符
            const triggerIndex = text.lastIndexOf(triggerChar, offset);
            if (triggerIndex !== -1) {
              // 选中从触发字符到当前位置的文本并删除
              selection.setTextNodeRange(
                anchorNode,
                triggerIndex,
                anchorNode,
                offset
              );
              selection.removeText();
            }
          }
        }
        
        // 插入变量节点
        const variableMentionNode = $createVariableMentionNode(variable.key, variable.label);
        selection.insertNodes([variableMentionNode]);
        
        // 在变量后添加空格
        const spaceNode = $createTextNode(' ');
        selection.insertNodes([spaceNode]);
      }
    });
    
    closePopup();
  }, [editor, isTriggeredByTriggerChar, triggerChar, closePopup]);

  const handleVariableHover = useCallback((index: number) => {
    console.log('Mouse hover on index:', index);
    setSelectedIndex(index);
  }, []);

  const handleSearchChange = useCallback((query: string) => {
    setSearchQuery(query);
    setSelectedIndex(0);
  }, []);

  // 获取过滤后的变量列表
  const getFilteredVariables = useCallback(() => {
    if (!searchQuery.trim()) {
      return variables;
    }
    return variables.filter(variable =>
      variable.label.toLowerCase().includes(searchQuery.toLowerCase()) ||
      variable.key.toLowerCase().includes(searchQuery.toLowerCase())
    );
  }, [variables, searchQuery]);

  // 键盘事件处理
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      // 处理触发字符
      if (triggerChars.includes(event.key) && !isPopupOpen) {
        setTimeout(() => {
          openPopup(true, event.key);
        }, 10);
        return false;
      }
      
      // 弹窗打开时的键盘导航
      if (isPopupOpen) {
        const filteredVars = getFilteredVariables();
        
        switch (event.key) {
          case 'ArrowDown':
            event.preventDefault();
            setSelectedIndex(prev => (prev + 1) % filteredVars.length);
            return true;
            
          case 'ArrowUp':
            event.preventDefault();
            setSelectedIndex(prev => (prev - 1 + filteredVars.length) % filteredVars.length);
            return true;
            
          case 'Enter':
            event.preventDefault();
            if (filteredVars[selectedIndex]) {
              insertVariable(filteredVars[selectedIndex]);
            }
            return true;
            
          case 'Escape':
            event.preventDefault();
            closePopup();
            return true;
            
          case 'Backspace':
            // 处理退格键更新搜索查询
            if (searchQuery.length === 0) {
              closePopup();
              return false;
            }
            setSearchQuery(prev => prev.slice(0, -1));
            setSelectedIndex(0);
            break;
            
          default:
            // 让输入框正常处理字符输入
            break;
        }
      }
      
      return false;
    };

    const unregister = editor.registerCommand(
      KEY_DOWN_COMMAND,
      handleKeyDown,
      COMMAND_PRIORITY_LOW
    );

    return unregister;
  }, [editor, isPopupOpen, searchQuery, selectedIndex, openPopup, closePopup, insertVariable, getFilteredVariables]);

  // 监听编辑器内容变化来更新搜索查询
  useEffect(() => {
    if (!isPopupOpen || !isTriggeredByTriggerChar) return;

    const unregisterUpdate = editor.registerUpdateListener(({ editorState }) => {
      editorState.read(() => {
        const selection = $getSelection();
        if ($isRangeSelection(selection)) {
          const anchorNode = selection.anchor.getNode();
          if ($isTextNode(anchorNode)) {
            const text = anchorNode.getTextContent();
            const offset = selection.anchor.offset;
            
            // 查找最近的触发字符
            const triggerIndex = text.lastIndexOf(triggerChar, offset);
            if (triggerIndex !== -1) {
              const query = text.slice(triggerIndex + 1, offset);
              if (query !== searchQuery) {
                setSearchQuery(query);
                setSelectedIndex(0);
              }
            } else if (searchQuery.length > 0) {
              // 如果找不到触发字符但有搜索查询，清空搜索查询
              setSearchQuery('');
              setSelectedIndex(0);
            }
          }
        }
      });
    });

    return unregisterUpdate;
  }, [editor, isPopupOpen, isTriggeredByTriggerChar, triggerChar, searchQuery]);

  // 添加右击功能
  useEffect(() => {
    const handleContextMenu = (event: MouseEvent) => {
      event.preventDefault();
      
      // 获取点击位置的光标坐标
      const position = {
        x: event.clientX + window.scrollX,
        y: event.clientY + window.scrollY
      };
      
      setPopupPosition(position);
      setIsPopupOpen(true);
      setSearchQuery('');
      setSelectedIndex(0);
      setIsTriggeredByTriggerChar(false);
      setTriggerChar('');
    };

    const editorElement = editor.getRootElement();
    if (editorElement) {
      editorElement.addEventListener('contextmenu', handleContextMenu);
      return () => {
        editorElement.removeEventListener('contextmenu', handleContextMenu);
      };
    }
  }, [editor]);

  return (
    <>
      <VariablePopup
        isOpen={isPopupOpen}
        variables={variables}
        position={popupPosition}
        searchQuery={searchQuery}
        onSelect={insertVariable}
        onClose={closePopup}
        selectedIndex={selectedIndex}
        onHover={handleVariableHover}
        onSearchChange={handleSearchChange}
      />
      
      
      {/* 调试：显示光标位置指示器 */}
      {popupPosition && import.meta.env.DEV && (
        <div
          className="fixed w-2 h-2 bg-red-500 rounded-full z-[10001] pointer-events-none"
          style={{
            left: popupPosition.x - 4,
            top: popupPosition.y - 4,
          }}
        />
      )}
      
      {/* 调试：显示当前选中的索引 */}
      {isPopupOpen && import.meta.env.DEV && (
        <div className="fixed top-20 right-6 z-[10000] bg-green-100 border border-green-400 p-3 rounded text-sm">
          <div>选中索引: <span className="font-bold text-green-800">{selectedIndex}</span></div>
          <div>总数: {variables.length}</div>
          {selectedIndex < variables.length && (
            <div>当前: <span className="font-bold text-blue-600">{variables[selectedIndex]?.label}</span></div>
          )}
        </div>
      )}
    </>
  );
}