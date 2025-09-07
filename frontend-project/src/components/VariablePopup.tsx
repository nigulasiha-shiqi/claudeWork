import { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import type { VariableItem } from '../types';

export interface VariablePopupProps {
  isOpen: boolean;
  variables: VariableItem[];
  position: { x: number; y: number } | null;
  searchQuery: string;
  onSelect: (variable: VariableItem) => void;
  onClose: () => void;
  selectedIndex: number;
  onSelectedIndexChange: (index: number) => void;
}

export default function VariablePopup({
  isOpen,
  variables,
  position,
  searchQuery,
  onSelect,
  onClose,
  selectedIndex,
  onSelectedIndexChange,
}: VariablePopupProps) {
  const [filteredVariables, setFilteredVariables] = useState<VariableItem[]>([]);
  const popupRef = useRef<HTMLDivElement>(null);

  console.log('VariablePopup render:', { isOpen, position, variables: variables.length });

  // 过滤变量列表
  useEffect(() => {
    if (!searchQuery.trim()) {
      setFilteredVariables(variables);
    } else {
      const filtered = variables.filter(variable =>
        variable.label.toLowerCase().includes(searchQuery.toLowerCase()) ||
        variable.key.toLowerCase().includes(searchQuery.toLowerCase())
      );
      setFilteredVariables(filtered);
    }
  }, [variables, searchQuery]);

  // 重置选中索引当过滤结果变化时
  useEffect(() => {
    if (selectedIndex >= filteredVariables.length) {
      onSelectedIndexChange(0);
    }
  }, [filteredVariables.length, selectedIndex, onSelectedIndexChange]);

  // 点击外部关闭弹窗
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

  if (!isOpen || !position || filteredVariables.length === 0) {
    return null;
  }

  const handleItemClick = (variable: VariableItem) => {
    onSelect(variable);
  };

  return createPortal(
    <div
      ref={popupRef}
      className="fixed z-50 bg-white border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-y-auto min-w-[200px] max-w-[300px]"
      style={{
        top: position.y + 20,
        left: position.x,
      }}
    >
      <div className="p-2">
        <div className="text-xs text-gray-500 px-2 py-1 border-b border-gray-100">
          选择变量 ({filteredVariables.length})
        </div>
        <div className="mt-1">
          {filteredVariables.map((variable, index) => (
            <div
              key={variable.key}
              className={`px-3 py-2 cursor-pointer rounded text-sm flex items-center justify-between transition-colors ${
                index === selectedIndex
                  ? 'bg-blue-50 text-blue-700'
                  : 'hover:bg-gray-50'
              }`}
              onClick={() => handleItemClick(variable)}
              onMouseEnter={() => onSelectedIndexChange(index)}
            >
              <span className="flex-1 truncate">{variable.label}</span>
              <span className="text-xs text-gray-400 ml-2 font-mono">
                ${'{'}
                {variable.key}
                {'}'}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>,
    document.body
  );
}

// 工具函数：获取光标位置
export function getCaretPosition(element: HTMLElement): { x: number; y: number } | null {
  const selection = window.getSelection();
  if (!selection || selection.rangeCount === 0) return null;

  const range = selection.getRangeAt(0);
  const rect = range.getBoundingClientRect();
  
  if (rect.width === 0 && rect.height === 0) {
    // 如果范围矩形为空，尝试使用插入临时元素的方法
    const tempElement = document.createElement('span');
    tempElement.style.position = 'absolute';
    tempElement.style.visibility = 'hidden';
    range.insertNode(tempElement);
    
    const tempRect = tempElement.getBoundingClientRect();
    tempElement.remove();
    
    return {
      x: tempRect.left + window.scrollX,
      y: tempRect.top + window.scrollY
    };
  }

  return {
    x: rect.left + window.scrollX,
    y: rect.top + window.scrollY
  };
}

// 工具函数：检查元素是否在视口内
export function isElementInViewport(element: HTMLElement): boolean {
  const rect = element.getBoundingClientRect();
  return (
    rect.top >= 0 &&
    rect.left >= 0 &&
    rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
    rect.right <= (window.innerWidth || document.documentElement.clientWidth)
  );
}