import { FlatCompat } from '@eslint/eslintrc';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const compat = new FlatCompat({
    baseDirectory: dirname(fileURLToPath(import.meta.url)),
});

export default [
    ...compat.extends('plugin:@sencha/extjs/recommended'),
    {
        settings: {
            extjs: {
                toolkit: 'modern',
                version: '8.0.0',
            },
        },
        languageOptions: {
            ecmaVersion: 12,
            sourceType: 'module',
        },
        rules: {
            semi: ['error', 'always'],
            quotes: ['error', 'single'],
            '@sencha/extjs/no-deprecated-class-usage': 'off',
        },
    },
];
