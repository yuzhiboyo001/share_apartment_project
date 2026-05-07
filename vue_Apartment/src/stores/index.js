import { defineStore } from "pinia";
import { ref, watch } from "vue";

function initState() {
  return {
    isCollapse: false,
    tags: [
      {
        path: "/home",
        name: "home",
        label: "首页",
        icon: "home",
      },
    ],
    currentMenu: null,
    menuList: [],
    token: "",
    routerList: [],
  };
}
export const useALLDataStore = defineStore("allData", () => {
  const state = ref(initState());

  watch(state, (newObj) => {
    if (!newObj.token) return;
    localStorage.setItem("store", JSON.stringify(newObj));
  },{deep:true});
  function selectMenu(val) {
    if (val.name === "home") {
      state.value.currentMenu = null;
    } else {
      state.value.currentMenu = val;
      let index = state.value.tags.findIndex((item) => item.name === val.name);
      index === -1 ? state.value.tags.push(val) : "";
    }
  }
  function undateTags(tag) {
    let index = state.value.tags.findIndex((item) => item.name === tag.name);
    state.value.tags.splice(index, 1);
  }
  function addMenu(router,type) {
    if(type === "refresh"){
      if(JSON.parse(localStorage.getItem("store"))){
        state.value = JSON.parse(localStorage.getItem("store"));
        //刷新时需要删除所有路由
        state.value.routerList = [];

      }else{
        return;
      }
    }
    const menu = state.value.menuList;
    const module = import.meta.glob("../views/**/*.vue");
    const routeArr = [];
    menu.forEach((item) => {
      if (item.children) {
        item.children.forEach((val) => {
          let url = `../views/${val.url}.vue`;
          val.component = module[url];
          routeArr.push(...item.children);
        });
      } else {
        let url = `../views/${item.url}.vue`;
        item.component = module[url];
        routeArr.push(item);
      }
    });

    routeArr.forEach((item) => {
      state.value.routerList.push(router.addRoute("main", item));
    });
  }
  function updateMenuList(val) {
    state.value.menuList = val;
  }
  function clean(){
  state.value.routerList.forEach((item) => {
    if(item) item();
  });
    state.value = initState();
    //删除我们本地的缓存
    localStorage.removeItem('store');
}
  return {
    state,
    selectMenu,
    undateTags,
    updateMenuList,
    addMenu,
    clean,
  };
});
