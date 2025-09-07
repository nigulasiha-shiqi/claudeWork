import {
  $createParagraphNode,
  $createTextNode,
  $getRoot,
  type EditorState,
  type LexicalEditor
} from 'lexical';
import { $createVariableMentionNode } from '../components/VariableMentionNode';
import type { VariableItem } from '../types';

// 解析初始文本，将 ${key} 转换为变量节点
export function parseInitialText(text: string, variables: VariableItem[]): void {
  const root = $getRoot();
  root.clear();

  if (!text.trim()) {
    const paragraph = $createParagraphNode();
    root.append(paragraph);
    return;
  }

  // 创建变量映射表，便于快速查找
  const variableMap = new Map<string, VariableItem>();
  variables.forEach(variable => {
    variableMap.set(variable.key, variable);
  });

  // 使用正则表达式匹配 ${key} 格式的变量
  const variableRegex = /\$\{([^}]+)\}/g;
  const parts: Array<{ type: 'text' | 'variable'; content: string; variable?: VariableItem }> = [];
  
  let lastIndex = 0;
  let match;

  while ((match = variableRegex.exec(text)) !== null) {
    // 添加变量前的文本
    if (match.index > lastIndex) {
      const textContent = text.slice(lastIndex, match.index);
      if (textContent) {
        parts.push({ type: 'text', content: textContent });
      }
    }

    // 添加变量
    const variableKey = match[1];
    const variable = variableMap.get(variableKey);
    
    if (variable) {
      parts.push({ type: 'variable', content: match[0], variable });
    } else {
      // 如果变量不存在，作为普通文本处理
      parts.push({ type: 'text', content: match[0] });
    }

    lastIndex = match.index + match[0].length;
  }

  // 添加剩余的文本
  if (lastIndex < text.length) {
    const remainingText = text.slice(lastIndex);
    if (remainingText) {
      parts.push({ type: 'text', content: remainingText });
    }
  }

  // 按段落分割内容
  const paragraphs = splitIntoLines(parts);
  
  paragraphs.forEach(paragraphParts => {
    const paragraph = $createParagraphNode();
    
    paragraphParts.forEach(part => {
      if (part.type === 'variable' && part.variable) {
        const variableNode = $createVariableMentionNode(part.variable.key, part.variable.label);
        paragraph.append(variableNode);
      } else {
        const textNode = $createTextNode(part.content);
        paragraph.append(textNode);
      }
    });
    
    root.append(paragraph);
  });
}

// 将内容按行分割
function splitIntoLines(parts: Array<{ type: 'text' | 'variable'; content: string; variable?: VariableItem }>): Array<Array<{ type: 'text' | 'variable'; content: string; variable?: VariableItem }>> {
  const lines: Array<Array<{ type: 'text' | 'variable'; content: string; variable?: VariableItem }>> = [];
  let currentLine: Array<{ type: 'text' | 'variable'; content: string; variable?: VariableItem }> = [];

  parts.forEach(part => {
    if (part.type === 'text' && part.content.includes('\n')) {
      const textParts = part.content.split('\n');
      
      textParts.forEach((textPart, index) => {
        if (textPart) {
          currentLine.push({ type: 'text', content: textPart });
        }
        
        if (index < textParts.length - 1) {
          // 换行
          lines.push(currentLine);
          currentLine = [];
        }
      });
    } else {
      currentLine.push(part);
    }
  });

  if (currentLine.length > 0) {
    lines.push(currentLine);
  }

  // 确保至少有一个段落
  if (lines.length === 0) {
    lines.push([]);
  }

  return lines;
}

// 创建初始编辑器状态
export function createInitialEditorState(editor: LexicalEditor, text: string, variables: VariableItem[]): void {
  editor.update(() => {
    parseInitialText(text, variables);
  });
}