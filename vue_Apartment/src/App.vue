<template>
  <router-view></router-view>
  <ChatFloat />
</template>

<script setup>
import ChatFloat from './components/ChatFloat.vue'

</script>

<style scoped>
#app {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>
