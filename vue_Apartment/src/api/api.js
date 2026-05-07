/**
 * 项目的整体API管理
 */

import request from "./request";

export default {
  getApartmentList(params) {
    return request({
      url: "/apartment/list",
      method: "get",
      params,
    });
  },
  addApartment(data) {
    return request({
      url: "/apartment/add",
      method: "post",
      data,
    });
  },
  editApartment(data) {
    return request({
      url: "/apartment/edit",
      method: "put",
      data,
    });
  },
  deleteApartment(params) {
    return request({
      url: "/apartment/delete",
      method: "delete",
      params,
    });
  },
  getTableData() {
    return request({
      url: "/home/getTableData",
      method: "get",
    });
  },
  getChartData() {
    return request({
      url: "/chart/getChartData",
      method: "get",
    });
  },
  getUserData(data) {
    return request({
      url: "/user/getUserData",
      method: "get",
      data,
    });
  },
  deleteUser(data) {
    return request({
      url: "/user/deleteUser",
      method: "get",
      data,
    });
  },
  addUser(data) {
    return request({
      url: "/user/addUser",
      method: "post",
      data,
    });
  },
  editUser(data) {
    return request({
      url: "/user/editUser",
      method: "post",
      data,
    });
  },
  getMenu(params) {
    return request({
      url: "/permission/getMenu",
      method: "post",
      data: params,
    });
  },
  getRoomList(params) {
    return request({
      url: "/room/list",
      method: "get",
      params,
    });
  },
  updateRoom(data) {
    return request({
      url: "/room/update",
      method: "put",
      data,
    });
  },
  deleteRoom(params) {
    return request({
      url: "/room/delete",
      method: "delete",
      params,
    });
  },
  addRoom(data) {
    return request({
      url: "/room/add",
      method: "post",
      data,
    });
  },
  getTenantList(params) {
    return request({
      url: "/permission/tenantList", 
      method: "get",
      params,
    });
  },
  addRoom(data) {
    return request({
      url: "/room/add",
      method: "post",
      data,
    });
  },
  register(data) {
    return request({
      url: "/permission/register",
      method: "post",
      data,
    });
  },
  resetPassword(data) {
    return request({
      url: "/permission/resetPassword",
      method: "post",
      data,
    });
  },
  getApartmentByInviteCode(params) {
    return request({
      url: "/apartment/getByInviteCode",
      method: "get",
      params,
    });
  },
  joinApartment(data) {
    return request({
      url: "/apartment/join",
      method: "post",
      data,
    });
  },
 // 房东账单接口
getLandlordBillList(params) {
  return request({
    url: "/bill/landlord/list",
    method: "get",
    params,
  });
},
addLandlordBill(data) {
  return request({
    url: "/bill/landlord/add",
    method: "post",
    data,
  });
},
editLandlordBill(data) {
  return request({
    url: "/bill/landlord/edit",
    method: "put",
    data,
  });
},
deleteLandlordBill(params) {
  return request({
    url: "/bill/landlord/delete",
    method: "delete",
    params,
  });
},
getLandlordBillDetail(params) {
  return request({
    url: "/bill/landlord/detail/" + params.billId,
    method: "get",
  });
},
getBillPayments(params) {
  return request({
    url: "/bill/landlord/payments",
    method: "get",
    params,
  });
},
// 租客账单接口
getTenantBillList(params) {
  return request({
    url: "/bill/tenant/list",
    method: "get",
    params,
  });
},
payTenantBill(data) {
  return request({
    url: "/bill/tenant/pay",
    method: "post",
    data,
  });
},
// 消息相关接口
publishTask(data) {
  return request({
    url: "/message/publishTask",
    method: "post",
    data,
  });
},
getMessageList(params) {
  return request({
    url: "/message/list",
    method: "get",
    params,
  });
},
getUnreadCount() {
  return request({
    url: "/message/unread/count",
    method: "get",
  });
},
markMessageRead(id) {
  return request({
    url: `/message/read/${id}`,
    method: "put",
  });
},
markAllRead() {
  return request({
    url: "/message/read/all",
    method: "put",
  });
},
deleteMessage(id) {
  return request({
    url: `/message/delete/${id}`,
    method: "delete",
  });
},
// 任务相关接口
getTaskList(params) {
  return request({
    url: "/task/list",
    method: "get",
    params,
  });
},
getTaskDetail(id) {
  return request({
    url: `/task/detail/${id}`,
    method: "get",
  });
},
deleteTask(id) {
  return request({
    url: `/task/delete/${id}`,
    method: "delete",
  });
},
updateTaskStatus(data) {
  return request({
    url: "/task/updateStatus",
    method: "put",
    data,
  });
},
// 任务相关接口
getTaskList(params) {
  return request({
    url: "/task/list",
    method: "get",
    params,
  });
},
getTaskDetail(id) {
  return request({
    url: `/task/detail/${id}`,
    method: "get",
  });
},
updateTaskStatus(data) {
  return request({
    url: "/task/updateStatus",
    method: "put",
    data,
  });
},
deleteTask(id) {
  return request({
    url: `/task/delete/${id}`,
    method: "delete",
  });
},
getMyTasks(params) {
  return request({
    url: "/task/myTasks",
    method: "get",
    params,
  });
},
// 确保这些接口都存在
getMessageList(params) {
  return request({
    url: "/message/list",
    method: "get",
    params,
  });
},
getUnreadCount() {
  return request({
    url: "/message/unread/count",
    method: "get",
  });
},
markMessageRead(id) {
  return request({
    url: `/message/read/${id}`,
    method: "put",
  });
},
markAllRead() {
  return request({
    url: "/message/read/all",
    method: "put",
  });
},
deleteMessage(id) {
  return request({
    url: `/message/delete/${id}`,
    method: "delete",
  });
},
// 任务相关接口（租客事务）
createTask(data) {
  return request({
    url: "/task/create",
    method: "post",
    data,
  });
},
getTaskList(params) {
  return request({
    url: "/task/list",
    method: "get",
    params,
  });
},
getTaskDetail(id) {
  return request({
    url: `/task/detail/${id}`,
    method: "get",
  });
},
updateTaskStatus(data) {
  return request({
    url: "/task/updateStatus",
    method: "put",
    data,
  });
},
deleteTask(id) {
  return request({
    url: `/task/delete/${id}`,
    method: "delete",
  });
},
};
