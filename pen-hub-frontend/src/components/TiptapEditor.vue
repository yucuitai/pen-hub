<template>
  <div class="tiptap-editor">
    <div class="editor-toolbar" v-if="editor">
      <button @click="editor.chain().focus().toggleBold().run()" :class="{ active: editor.isActive('bold') }" title="粗体">B</button>
      <button @click="editor.chain().focus().toggleItalic().run()" :class="{ active: editor.isActive('italic') }" title="斜体">I</button>
      <button @click="editor.chain().focus().toggleHeading({ level: 2 }).run()" :class="{ active: editor.isActive('heading', { level: 2 }) }" title="标题">H2</button>
      <button @click="editor.chain().focus().toggleHeading({ level: 3 }).run()" :class="{ active: editor.isActive('heading', { level: 3 }) }" title="标题">H3</button>
      <button @click="editor.chain().focus().toggleBulletList().run()" :class="{ active: editor.isActive('bulletList') }" title="列表">•</button>
      <button @click="editor.chain().focus().toggleOrderedList().run()" :class="{ active: editor.isActive('orderedList') }" title="有序列表">1.</button>
      <button @click="editor.chain().focus().toggleCodeBlock().run()" :class="{ active: editor.isActive('codeBlock') }" title="代码块">&lt;/&gt;</button>
      <button @click="editor.chain().focus().setHorizontalRule().run()" title="分割线">—</button>
    </div>
    <EditorContent :editor="editor" class="editor-content" />
  </div>
</template>

<script setup lang="ts">
import { watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'

interface Props {
  modelValue: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit,
    Image.configure({ inline: true }),
    Link.configure({ openOnClick: false }),
  ],
  onUpdate: ({ editor }) => {
    emit('update:modelValue', editor.getHTML())
  },
})

watch(() => props.modelValue, (val) => {
  if (editor.value && val !== editor.value.getHTML()) {
    editor.value.commands.setContent(val, { emitUpdate: false })
  }
})

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<style scoped>
.tiptap-editor {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  gap: 4px;
  padding: 8px 12px;
  background: var(--color-background-secondary);
  border-bottom: 1px solid var(--color-border);
  flex-wrap: wrap;
}

.editor-toolbar button {
  width: 32px;
  height: 32px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.editor-toolbar button:hover {
  background: var(--color-background-tertiary);
  color: var(--color-text);
}

.editor-toolbar button.active {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.editor-content {
  min-height: 400px;
  padding: 16px;
  background: var(--color-background);
}

.editor-content :deep(.tiptap) {
  outline: none;
  min-height: 380px;
}

.editor-content :deep(.tiptap h2) {
  font-size: 20px;
  font-weight: 700;
  margin: 16px 0 8px;
  color: var(--color-text);
}

.editor-content :deep(.tiptap h3) {
  font-size: 17px;
  font-weight: 600;
  margin: 12px 0 6px;
  color: var(--color-text);
}

.editor-content :deep(.tiptap p) {
  margin: 8px 0;
  line-height: 1.8;
  color: var(--color-text);
}

.editor-content :deep(.tiptap ul),
.editor-content :deep(.tiptap ol) {
  padding-left: 24px;
  margin: 8px 0;
}

.editor-content :deep(.tiptap code) {
  background: var(--color-background-tertiary);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.editor-content :deep(.tiptap pre) {
  background: var(--color-background-secondary);
  padding: 12px 16px;
  border-radius: 8px;
  overflow-x: auto;
}

.editor-content :deep(.tiptap hr) {
  border: none;
  border-top: 1px solid var(--color-border);
  margin: 16px 0;
}

.editor-content :deep(.tiptap img) {
  max-width: 100%;
  border-radius: 8px;
}

.editor-content :deep(.tiptap a) {
  color: var(--color-primary);
  text-decoration: underline;
}
</style>
