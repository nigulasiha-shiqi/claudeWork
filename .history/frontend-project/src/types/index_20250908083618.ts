export interface AppConfig {
  theme: 'light' | 'dark';
  language: string;
}

export interface User {
  id: string;
  name: string;
  email: string;
}

export type Theme = 'light' | 'dark' | 'system';

export interface VariableItem {
  key: string;
  label: string;
}