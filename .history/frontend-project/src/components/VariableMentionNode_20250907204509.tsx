import { DecoratorNode, type SerializedLexicalNode, type Spread, type NodeKey, $getNodeByKey } from 'lexical';
import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext';
import type { VariableItem } from '../types';

export interface SerializedVariableMentionNode extends Spread<{
  variableKey: string;
  label: string;
}, SerializedLexicalNode> {
  type: 'variable-mention';
  version: 1;
}

export class VariableMentionNode extends DecoratorNode<JSX.Element> {
  __variableKey: string;
  __label: string;

  static getType(): string {
    return 'variable-mention';
  }

  static clone(node: VariableMentionNode): VariableMentionNode {
    return new VariableMentionNode(node.__variableKey, node.__label, node.__key);
  }

  constructor(variableKey: string, label: string, key?: NodeKey) {
    super(key);
    this.__variableKey = variableKey;
    this.__label = label;
  }

  getVariableKey(): string {
    return this.__variableKey;
  }

  getLabel(): string {
    return this.__label;
  }

  createDOM(): HTMLElement {
    const span = document.createElement('span');
    span.style.backgroundColor = '#e3f2fd';
    span.style.color = '#1976d2';
    span.style.padding = '2px 6px';
    span.style.borderRadius = '4px';
    span.style.margin = '0 2px';
    span.style.display = 'inline-block';
    span.style.fontWeight = '500';
    span.setAttribute('data-lexical-variable-mention', 'true');
    return span;
  }

  updateDOM(): false {
    return false;
  }

  setTextContent(text: string): void {
    const dom = this.getLatest().__dom;
    if (dom !== null) {
      dom.textContent = text;
    }
  }

  static importJSON(serializedNode: SerializedVariableMentionNode): VariableMentionNode {
    const { variableKey, label } = serializedNode;
    return $createVariableMentionNode(variableKey, label);
  }

  exportJSON(): SerializedVariableMentionNode {
    return {
      type: 'variable-mention',
      version: 1,
      variableKey: this.__variableKey,
      label: this.__label,
    };
  }

  getTextContent(): string {
    return this.__label;
  }

  isInline(): true {
    return true;
  }

  decorate(): JSX.Element {
    const nodeKey = this.__key;
    const label = this.__label;
    
    function VariableMentionComponent() {
      const [editor] = useLexicalComposerContext();
      
      const handleDelete = (e: React.MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();
        
        editor.update(() => {
          const node = $getNodeByKey(nodeKey);
          if (node) {
            node.remove();
          }
        });
      };
      
      return (
        <span
          className="relative inline-block text-blue-700 px-2 py-1 rounded mx-1 font-medium"
          contentEditable={false}
          data-lexical-variable-mention="true"
        >
          {label}
          <button
            onClick={handleDelete}
            className="absolute -top-3 -right-2 translate-x-1/2 -translate-y-1/2 w-4 h-4 bg-red-500 hover:bg-red-600 text-white text-xs rounded-full flex items-center justify-center transition-colors duration-200"
            title="删除变量"
          >
            ×
          </button>
        </span>
      );
    }
    
    return <VariableMentionComponent />;
  }
}

export function $createVariableMentionNode(variableKey: string, label: string): VariableMentionNode {
  return new VariableMentionNode(variableKey, label);
}

export function $isVariableMentionNode(node: any): node is VariableMentionNode {
  return node instanceof VariableMentionNode;
}