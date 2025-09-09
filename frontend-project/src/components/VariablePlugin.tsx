import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext';
import { useEffect, useState, useCallback } from 'react';
import {
  $getSelection,
  $isRangeSelection,
  $createTextNode,
  $isTextNode,
  TextNode,
  KEY_DOWN_COMMAND,
  COMMAND_PRIORITY_LOW,
  KEY_ARROW_DOWN_COMMAND,
  KEY_ARROW_UP_COMMAND,
  KEY_ENTER_COMMAND,
  KEY_ESCAPE_COMMAND,
} from 'lexical';
import { $createVariableMentionNode } from './VariableMentionNode';
import type { VariableItem } from '../types';
import VariablePopup, { getCaretPosition } from './VariablePopup';

interface VariablePluginProps {
  variables: VariableItem[];
}

export default function VariablePlugin({ variables }: VariablePluginProps) {
  const [editor] = useLexicalComposerContext();
  const [isPopupOpen, setIsPopupOpen] = useState(false);
  const [popupPosition, setPopupPosition] = useState<{ x: number; y: number } | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);

  // 添加简单的测试弹窗功能
  const testPopup = useCallback(() => {
    console.log('Test popup triggered');
    setPopupPosition({ x: 200, y: 200 });
    setIsPopupOpen(true);
    setSearchQuery('');
    setSelectedIndex(0);
  }, []);

  // 关闭弹窗
  const closePopup = useCallback(() => {
    setIsPopupOpen(false);
    setPopupPosition(null);
    setSearchQuery('');
    setSelectedIndex(0);
  }, []);

  // 打开弹窗
  const openPopup = useCallback(() => {
    console.log('openPopup called');
    editor.getEditorState().read(() => {
      const selection = $getSelection();
      console.log('selection:', selection);
      if ($isRangeSelection(selection)) {
        const domSelection = window.getSelection();
        console.log('domSelection:', domSelection);
        if (domSelection && domSelection.rangeCount > 0) {
          const range = domSelection.getRangeAt(0);
          const rect = range.getBoundingClientRect();
          console.log('rect:', rect);
          
          const position = {
            x: rect.left + window.scrollX,
            y: rect.bottom + window.scrollY
          };
          console.log('setting position:', position);
          
          setPopupPosition(position);
          setIsPopupOpen(true);
          setSearchQuery('');
          setSelectedIndex(0);
        }
      }
    });
  }, [editor]);

  // 插入变量节点
  const insertVariable = useCallback((variable: VariableItem) => {
    editor.update(() => {
      const selection = $getSelection();
      if ($isRangeSelection(selection)) {
        // 如果有搜索查询，先删除查询文本
        if (searchQuery) {
          const anchor = selection.anchor;
          const focus = selection.focus;
          
          // 选中并删除搜索查询文本
          selection.setTextNodeRange(
            anchor.getNode(),
            Math.max(0, anchor.offset - searchQuery.length),
            focus.getNode(),
            focus.offset
          );
          selection.removeText();
        }
        
        // 插入变量节点
        const variableMentionNode = $createVariableMentionNode(variable.key, variable.label);
        selection.insertNodes([variableMentionNode]);
        
        // 在变量后添加一个空格
        const spaceNode = $createTextNode(' ');
        selection.insertNodes([spaceNode]);
      }
    });
    
    closePopup();
  }, [editor, searchQuery, closePopup]);

  // 处理变量选择
  const handleVariableSelect = useCallback((variable: VariableItem) => {
    insertVariable(variable);
  }, [insertVariable]);

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

  // 添加右击事件监听器
  useEffect(() => {
    const handleContextMenu = (event: MouseEvent) => {
      event.preventDefault();
      console.log('Right click detected');
      testPopup();
    };

    const editorElement = editor.getRootElement();
    if (editorElement) {
      editorElement.addEventListener('contextmenu', handleContextMenu);
      return () => {
        editorElement.removeEventListener('contextmenu', handleContextMenu);
      };
    }
  }, [editor, testPopup]);

  useEffect(() => {
    // 键盘事件处理
    const handleKeyDown = (event: KeyboardEvent) => {
      console.log('Key pressed:', event.key, 'isPopupOpen:', isPopupOpen);
      
      // 处理 / 键
      if (event.key === '/' && !isPopupOpen) {
        console.log('Slash key detected, will open popup');
        // 不阻止默认行为，让 / 字符正常插入
        setTimeout(() => {
          testPopup(); // 暂时使用测试弹窗
        }, 10);
        
        return false; // 允许默认的字符插入
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
            // 如果搜索查询为空且按退格，关闭弹窗
            if (!searchQuery) {
              closePopup();
              return false; // 允许正常的退格处理
            }
            break;
            
          default:
            // 处理搜索输入
            if (event.key.length === 1 && !event.ctrlKey && !event.metaKey && !event.altKey) {
              setSearchQuery(prev => prev + event.key);
              setSelectedIndex(0);
            }
            break;
        }
      }
      
      return false;
    };

    // 注册键盘命令
    const unregisterKeyDown = editor.registerCommand(
      KEY_DOWN_COMMAND,
      handleKeyDown,
      COMMAND_PRIORITY_LOW
    );

    return () => {
      unregisterKeyDown();
    };
  }, [editor, isPopupOpen, searchQuery, selectedIndex, openPopup, closePopup, insertVariable, getFilteredVariables]);

  // 监听编辑器变化，处理搜索查询的更新
  useEffect(() => {
    if (!isPopupOpen) return;

    const unregisterUpdate = editor.registerUpdateListener(({ editorState }) => {
      editorState.read(() => {
        const selection = $getSelection();
        if ($isRangeSelection(selection)) {
          const anchorNode = selection.anchor.getNode();
          if ($isTextNode(anchorNode)) {
            const text = anchorNode.getTextContent();
            const offset = selection.anchor.offset;
            
            // 查找最近的 / 字符
            const slashIndex = text.lastIndexOf('/', offset);
            if (slashIndex !== -1) {
              const query = text.slice(slashIndex + 1, offset);
              if (query !== searchQuery) {
                setSearchQuery(query);
                setSelectedIndex(0);
              }
            }
          }
        }
      });
    });

    return () => {
      unregisterUpdate();
    };
  }, [editor, isPopupOpen, searchQuery]);

  return (
    <VariablePopup
      isOpen={isPopupOpen}
      variables={variables}
      position={popupPosition}
      searchQuery={searchQuery}
      onSelect={handleVariableSelect}
      onClose={closePopup}
      selectedIndex={selectedIndex}
      onSelectedIndexChange={setSelectedIndex}
    />
  );
}