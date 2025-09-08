import { 
  $getRoot, 
  $isTextNode, 
  $isParagraphNode,
  type LexicalNode,
  type ElementNode
} from 'lexical';
import { $isVariableMentionNode } from '../components/VariableMentionNode';

// 将编辑器内容序列化为带有 ${key} 格式的字符串
export function serializeEditorContent(): string {
  const root = $getRoot();
  const paragraphs: string[] = [];
  
  root.getChildren().forEach(child => {
    if ($isParagraphNode(child)) {
      const paragraphText = serializeParagraph(child);
      paragraphs.push(paragraphText);
    } else {
      // 处理其他类型的顶层节点
      const nodeText = serializeNode(child);
      if (nodeText) {
        paragraphs.push(nodeText);
      }
    }
  });
  
  return paragraphs.join('\n');
}

// 序列化段落节点
function serializeParagraph(paragraph: ElementNode): string {
  const parts: string[] = [];
  
  paragraph.getChildren().forEach(child => {
    const nodeText = serializeNode(child);
    if (nodeText) {
      parts.push(nodeText);
    }
  });
  
  return parts.join('');
}

// 序列化单个节点
function serializeNode(node: LexicalNode): string {
  if ($isVariableMentionNode(node)) {
    // 变量节点转换为 ${key} 格式
    return `\${${node.getVariableKey()}}`;
  } else if ($isTextNode(node)) {
    // 文本节点直接返回文本内容
    return node.getTextContent();
  } else if (node.getType() === 'element') {
    // 其他元素节点递归处理子节点
    const element = node as ElementNode;
    const childTexts: string[] = [];
    
    element.getChildren().forEach(child => {
      const childText = serializeNode(child);
      if (childText) {
        childTexts.push(childText);
      }
    });
    
    return childTexts.join('');
  }
  
  return '';
}

// 获取编辑器的纯文本内容（不含变量格式）
export function getPlainTextContent(): string {
  const root = $getRoot();
  return root.getTextContent();
}

// 将编辑器内容序列化为人类可读格式（变量显示为标签）
export function serializeEditorContentAsLabels(): string {
  const root = $getRoot();
  // 直接使用 Lexical 的内置方法获取文本内容
  // 因为 VariableMentionNode 的 getTextContent() 已经返回标签文本了
  return root.getTextContent();
}

// 统计编辑器内容信息
export function getContentStats() {
  const root = $getRoot();
  let textLength = 0;
  let variableCount = 0;
  let paragraphCount = 0;
  
  const traverse = (node: LexicalNode) => {
    if ($isVariableMentionNode(node)) {
      variableCount++;
      textLength += node.getLabel().length;
    } else if ($isTextNode(node)) {
      textLength += node.getTextContent().length;
    } else if ($isParagraphNode(node)) {
      paragraphCount++;
    }
    
    if (node.getType() === 'element') {
      const element = node as ElementNode;
      element.getChildren().forEach(traverse);
    }
  };
  
  root.getChildren().forEach(traverse);
  
  return {
    textLength,
    variableCount,
    paragraphCount: Math.max(paragraphCount, 1), // 至少有一个段落
  };
}