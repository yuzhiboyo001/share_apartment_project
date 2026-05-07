import MOCK from "mockjs";
import homeApi from "./mockData/home";
import userApi from "./mockData/user";
import menuAPI from "./mockData/permission";
//1.拦截的路径 2.方法 3.制造出的假数据

MOCK.mock(/api\/home\/getTableData/,"get",homeApi.getTableData);
MOCK.mock(/api\/home\/getCountData/,"get",homeApi.getCountData);
MOCK.mock(/api\/home\/getChartData/,"get",homeApi.getChartData);
MOCK.mock(/api\/user\/getUserData/,"get",userApi.getUserList);
MOCK.mock(/api\/user\/deleteUser/,"get",userApi.deleteUser);
MOCK.mock(/api\/user\/addUser/,"post",userApi.createUser);
MOCK.mock(/api\/user\/editUser/,"post",userApi.updateUser);
MOCK.mock(/api\/permission\/getMenu/,"post",menuAPI.getMenu);
