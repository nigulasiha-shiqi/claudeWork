import { $createParagraphNode, $createTextNode, $getRoot } from 'lexical';
import { LexicalComposer } from '@lexical/react/LexicalComposer';
import { PlainTextPlugin } from '@lexical/react/LexicalPlainTextPlugin';
import { ContentEditable } from '@lexical/react/LexicalContentEditable';
import { HistoryPlugin } from '@lexical/react/LexicalHistoryPlugin';
import { OnChangePlugin } from '@lexical/react/LexicalOnChangePlugin';
import { LexicalErrorBoundary } from '@lexical/react/LexicalErrorBoundary';
import { RichTextPlugin } from '@lexical/react/LexicalRichTextPlugin';
import { HeadingNode } from '@lexical/rich-text';
import type { EditorState } from 'lexical';

const theme = {
  paragraph: 'mb-1',
  heading: {
    h1: 'text-2xl font-bold mb-2',
    h2: 'text-xl font-bold mb-2',
    h3: 'text-lg font-bold mb-2',
  },
};

const initialConfig = {
  namespace: 'MyEditor',
  theme,
  onError: (error: Error) => {
    console.error(error);
  },
  nodes: [HeadingNode],
};

function Placeholder() {
  return <div className="text-gray-500 absolute top-0 left-0 pointer-events-none">Enter some text...</div>;
}

interface EditorProps {
  onChange?: (editorState: EditorState) => void;
}

export default function Editor({ onChange }: EditorProps) {
  return (
    <LexicalComposer initialConfig={initialConfig}>
      <div className="relative">
        <RichTextPlugin
          contentEditable={
            <ContentEditable 
              className="min-h-32 p-4 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" 
            />
          }
          placeholder={<Placeholder />}
          ErrorBoundary={LexicalErrorBoundary}
        />
        <HistoryPlugin />
        {onChange && (
          <OnChangePlugin onChange={onChange} />
        )}
      </div>
    </LexicalComposer>
  );
}