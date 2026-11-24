const path = require('path')

module.exports = {
  publicPath: '/authcenter/',
  outputDir: 'dist',
  assetsDir: 'assets',
  productionSourceMap: false,
  devServer: {
    port: 5180,
    proxy: {
      '/authcenter/api': {
        target: 'http://localhost:9010',
        changeOrigin: true,
        pathRewrite: { '^/authcenter': '' }
      },
      '/authcenter/sso': {
        target: 'http://localhost:9010',
        changeOrigin: true,
        pathRewrite: { '^/authcenter': '' }
      },
      '/casecheck-sso': {
        target: 'http://localhost:9000',
        changeOrigin: true,
        pathRewrite: { '^/casecheck-sso': '' }
      },
      '/sso': {
        target: 'http://localhost:9010',
        changeOrigin: true
      }
    }
  },
  css: {
    loaderOptions: {
      less: {
        javascriptEnabled: true,
        modifyVars: {
          'primary-color': '#389AFE',
          'link-color': '#187DE4',
          'success-color': '#3BB53C',
          'warning-color': '#FC9F35',
          'error-color': '#EB5E6B',
          'font-size-base': '14px',
          'border-radius-base': '2px',
          'border-color-base': '#DCDFE6',
          'layout-body-background': '#F7FBFF',
          'layout-header-height': '60px',
          'layout-header-background': '#FFFFFF',
          'layout-sider-background': '#003970',
          'menu-dark-bg': '#003970',
          'menu-dark-submenu-bg': '#002F5D',
          'menu-dark-item-active-bg': '#4292F7',
          'table-header-bg': '#F6F6F6',
          'table-row-hover-bg': '#E6F3FF',
          'text-color': '#333333',
          'heading-color': '#333333',
          'font-family': '"Source Han Sans CN", "Microsoft YaHei", "PingFang SC", "Hiragino Sans GB", SimSun, Hei, sans-serif'
        }
      }
    }
  },
  chainWebpack(config) {
    config.resolve.alias.set('@', path.resolve(__dirname, 'src'))
  }
}
