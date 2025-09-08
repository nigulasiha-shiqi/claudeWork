import { useState, useEffect, forwardRef, useImperativeHandle } from 'react';
import type { VariableItem } from '../types';
import { convertVariablesToLabels, convertLabelsToVariables } from '../utils/variableConverter';

export interface PreviewEditorProps {
  variables: VariableItem[];
  value?: string;
  onChange?: (content: string) => void;
  onBlur?: (content: string) => void;
  placeholder?: string;
  className?: string;
}

export interface PreviewEditorRef {
  getValue: () => string;
  setValue: (value: string) => void;
}

const PreviewEditor = forwardRef<PreviewEditorRef, PreviewEditorProps>(
  ({ variables, value = '', onChange, onBlur, placeholder = '预览内容...', className = '' }, ref) => {
    const [content, setContent] = useState('');

    // 当外部value改变时，直接显示变量格式
    useEffect(() => {
      if (value !== undefined) {
        setContent(value);
      }
    }, [value]);

    // 暴露给外部的方法
    useImperativeHandle(ref, () => ({
      getValue: () => {
        // 直接返回当前内容（已经是变量格式）
        return content;
      },
      setValue: (newValue: string) => {
        // 直接设置内容（保持变量格式）
        setContent(newValue);
      },
    }), [content]);

    const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      const newContent = e.target.value;
      setContent(newContent);
      
      if (onChange) {
        // 恢复实时同步
        onChange(newContent);
      }
    };

    const handleBlur = (e: React.FocusEvent<HTMLTextAreaElement>) => {
      const newContent = e.target.value;
      
      if (onBlur) {
        // 只在失去焦点时同步
        onBlur(newContent);
      }
    };

    return (
      <div className={`relative border border-gray-300 rounded-lg bg-gray-50 ${className}`} data-preview-editor>
        <div className="absolute top-2 left-2 text-xs text-gray-500 font-medium">
          变量格式编辑器 (防抖同步)
        </div>
        <textarea
          value={content}
          onChange={handleChange}
          onBlur={handleBlur}
          placeholder={placeholder}
          className="w-full min-h-32 p-4 pt-8 bg-transparent resize-none focus:outline-none focus:bg-white focus:ring-2 focus:ring-blue-500 focus:border-blue-500 rounded-lg transition-colors"
          style={{ fontFamily: 'inherit' }}
        />
        <div className="absolute bottom-2 right-2 text-xs text-gray-400">
          支持输入 ${'${key}'} 格式
        </div>
      </div>
    );
  }
);

PreviewEditor.displayName = 'PreviewEditor';

export default PreviewEditor;