<template>
   <div class="tags">
        <el-tag
           v-for="(tag,index) in tags"
           :key="tag.name"
           :closable="tag.name !== 'home'"
           :effect="route.name === tag.name ? 'dark' : 'plain'"
           @click="handleMenu(tag)"
           @close="handleClose(tag,index)"
        >
        {{tag.label}}
        </el-tag>
   </div>
</template>

<script setup>
import {computed, ref}from "vue";
import {useRoute} from "vue-router";
import {useALLDataStore} from '@/stores'
import router from "../router";
const stores = useALLDataStore()
const tags = computed(() => stores.state.tags)
const route = useRoute();
const handleMenu = (tag) => {
   router.push(tag.path);
   stores.selectMenu(tag);
}
const handleClose = (tag,index) => {
    //通过pinia管理的tags数组，删除当前标签
   stores.undateTags(tag)
   //如果点击的关闭的tag 不是对应的当前页面
   if(tag.name !== route.name) return

   if(index === stores.state.tags.length){
        stores.selectMenu(tags.value[index-1])
        router.push(tags.value[index-1].name)
   }else{
        stores.selectMenu(tags.value[index])
        router.push(tags.value[index].name)
   }
}
</script>

<style lang="less" scoped>
.tags{
    margin:20px 0 0 20px;
}
.el-tag{
    margin-right:10px;
}
</style>
