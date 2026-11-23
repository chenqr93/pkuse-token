module.exports = {
  root: true,
  env: {
    node: true,
    browser: true
  },
  extends: ['plugin:vue/essential', 'eslint:recommended'],
  parserOptions: {
    parser: 'babel-eslint'
  },
  rules: {
    'no-console': 'off',
    'no-unused-vars': 'off',
    'no-undef': 'off',
    'no-irregular-whitespace': 'off',
    'no-extra-semi': 'off',
    'vue/no-unused-components': 'off',
    'vue/multi-word-component-names': 'off'
  }
}
