<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      <p class="copyright">
        <a href="https://www.codefather.cn" target="_blank" rel="noopener noreferrer" class="author-link">
          本系统旨在为网文作者提供覆盖创作全链路的AI辅助服务，从灵感激发到正文产出，实现高效、高质量的内容生产。
        </a>
      </p>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts">
// 无需额外的响应式数据
</script>

<style scoped>
.footer {
  background: var(--color-background);
  text-align: center;
  padding: 24px 20px;
  margin-top: 0;
  border-top: 1px solid var(--color-border);
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
}

.copyright {
  margin: 0;
  color: var(--color-text-muted);
  /* 文字颜色为 muted 颜色 */
  font-size: 13px;
}

.author-link {
  color: var(--color-text-muted);
  transition: color var(--transition-fast);
  /* 文字颜色过渡效果 */
}

.author-link:hover {
  color: var(--color-text);
}
</style>
