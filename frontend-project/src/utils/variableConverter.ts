import type { VariableItem } from '../types';

/**
 * 将变量格式 ${key} 转换为对应的标签文本
 */
export function convertVariablesToLabels(text: string, variables: VariableItem[]): string {
  let result = text;
  
  variables.forEach(variable => {
    const pattern = new RegExp(`\\$\\{${variable.key}\\}`, 'g');
    result = result.replace(pattern, variable.label);
  });
  
  return result;
}

/**
 * 将标签文本转换为变量格式 ${key}
 */
export function convertLabelsToVariables(text: string, variables: VariableItem[]): string {
  let result = text;
  
  variables.forEach(variable => {
    const pattern = new RegExp(variable.label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g');
    result = result.replace(pattern, `\${${variable.key}}`);
  });
  
  return result;
}

/**
 * 检测文本中是否包含变量格式 ${key}
 */
export function hasVariableFormat(text: string): boolean {
  return /\$\{[^}]+\}/.test(text);
}

/**
 * 提取文本中所有的变量格式 ${key}
 */
export function extractVariableKeys(text: string): string[] {
  const matches = text.match(/\$\{([^}]+)\}/g);
  if (!matches) return [];
  
  return matches.map(match => match.slice(2, -1)); // 去掉 ${ 和 }
}