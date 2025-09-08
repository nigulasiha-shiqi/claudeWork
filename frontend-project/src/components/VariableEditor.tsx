import { useRef, useImperativeHandle, forwardRef, useEffect } from 'react';
import { 
  $getRoot,
  $createParagraphNode,
  $createTextNode,
  $getSelection,
  $isRangeSelection,
  type EditorState,
  type LexicalEditor
} from 'lexical';
import { LexicalComposer } from '@lexical/react/LexicalComposer';
import { RichTextPlugin } from '@lexical/react/LexicalRichTextPlugin';
import { ContentEditable } from '@lexical/react/LexicalContentEditable';
import { HistoryPlugin } from '@lexical/react/LexicalHistoryPlugin';
import { OnChangePlugin } from '@lexical/react/LexicalOnChangePlugin';
import { LexicalErrorBoundary } from '@lexical/react/LexicalErrorBoundary';
import { VariableMentionNode } from './VariableMentionNode';
import type { VariableItem } from '../types';
import EnhancedVariablePlugin from './EnhancedVariablePlugin';
import { createInitialEditorState } from '../utils/textParser';
import { serializeEditorContent, serializeEditorContentAsLabels } from '../utils/contentSerializer';
import { useLexicalComposerContext } from '@lexical/react/LexicalComposerContext';

export interface VariableEditorProps {
  initialText?: string;
  variables: VariableItem[];
  triggerChars?: string[];
  onChange?: (content: string) => void;
  onPreviewChange?: (previewContent: string) => void;
  placeholder?: string;
  className?: string;
}

export interface VariableEditorRef {
  getContent: () => string;
  setContent: (content: string) => void;
  getEditor: () => LexicalEditor | null;
  testPopup: () => void;
}

const theme = {
  paragraph: 'mb-2',
  text: {
    bold: 'font-bold',
    italic: 'italic',
  },
};

function Placeholder({ text }: { text: string }) {
  return (
    <div className="absolute top-4 left-4 text-gray-400 pointer-events-none select-none">
      {text}
    </div>
  );
}

// 编辑器初始化器组件
interface EditorInitializerProps {
  initialText: string;
  variables: VariableItem[];
  editorRef: React.MutableRefObject<LexicalEditor | null>;
  isInitializedRef: React.MutableRefObject<boolean>;
}

function EditorInitializer({ initialText, variables, editorRef, isInitializedRef }: EditorInitializerProps) {
  const [editor] = useLexicalComposerContext();

  useEffect(() => {
    editorRef.current = editor;
    
    // 只在第一次初始化时解析初始文本
    if (!isInitializedRef.current && initialText) {
      createInitialEditorState(editor, initialText, variables);
      isInitializedRef.current = true;
    }
  }, [editor, initialText, variables, editorRef, isInitializedRef]);

  return null;
}

const VariableEditor = forwardRef<VariableEditorRef, VariableEditorProps>(
  ({ initialText = '', variables, triggerChars = ['/'], onChange, onPreviewChange, placeholder = '输入文本，按 / 选择变量...', className = '' }, ref) => {
    const editorRef = useRef<LexicalEditor | null>(null);
    const isInitializedRef = useRef(false);

    const initialConfig = {
      namespace: 'VariableEditor',
      theme,
      onError: (error: Error) => {
        console.error('VariableEditor error:', error);
      },
      nodes: [VariableMentionNode],
      editorState: undefined, // 暂时为空，后续添加初始化解析
    };

    // 暴露给外部的方法
    useImperativeHandle(ref, () => ({
      getContent: () => {
        if (!editorRef.current) return '';
        
        let content = '';
        editorRef.current.read(() => {
          content = serializeEditorContent();
        });
        return content;
      },

      setContent: (content: string) => {
        if (!editorRef.current) return;
        
        createInitialEditorState(editorRef.current, content, variables);
      },

      getEditor: () => editorRef.current,

      testPopup: () => {
        if (!editorRef.current) return;
        
        // 模拟用户按下斜杠键来触发弹窗
        editorRef.current.update(() => {
          const selection = $getSelection();
          if ($isRangeSelection(selection)) {
            // 在当前位置插入斜杠并触发事件
            const textNode = $createTextNode('/');
            selection.insertNodes([textNode]);
          }
        });
      },
    }), []);

    const handleEditorChange = (editorState: EditorState) => {
      if (onChange || onPreviewChange) {
        editorState.read(() => {
          if (onChange) {
            const content = serializeEditorContent();
            onChange(content);
          }
          if (onPreviewChange) {
            const previewContent = serializeEditorContentAsLabels();
            onPreviewChange(previewContent);
          }
        });
      }
    };

    return (
      <div className={`relative border border-gray-300 rounded-lg ${className}`}>
        <LexicalComposer initialConfig={initialConfig}>
          <EditorInitializer 
            initialText={initialText}
            variables={variables}
            editorRef={editorRef}
            isInitializedRef={isInitializedRef}
          />
          <RichTextPlugin
            contentEditable={
              <ContentEditable 
                className="min-h-32 p-4 focus:outline-none resize-none"
              />
            }
            placeholder={<Placeholder text={placeholder} />}
            ErrorBoundary={LexicalErrorBoundary}
          />
          <HistoryPlugin />
          <OnChangePlugin onChange={handleEditorChange} />
          <EnhancedVariablePlugin variables={variables} triggerChars={triggerChars} />
        </LexicalComposer>
      </div>
    );
  }
);

VariableEditor.displayName = 'VariableEditor';

export default VariableEditor;