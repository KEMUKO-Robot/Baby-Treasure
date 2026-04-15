# 科梦奇机器人云端API接口

文中 **`{BASE}`** 表示开放平台根地址。

---

## 目录

- [全局约定](#全局约定)
- [接入与 IP 授权](#接入与-ip-授权)
- [获取访问凭证](#获取访问凭证)
- [企业与组织](#企业与组织)
- [企业后台 SSO](#企业后台-sso)
- [人员管理](#人员管理)
- [机器人信息](#机器人信息)
- [地图与点位](#地图与点位)
- [远程控制 Pipe](#远程控制-pipe)
- [QA 问答](#qa-问答)
- [使用数据查询](#使用数据查询)
- [错误码](#错误码)
- [事件与回调](#事件与回调)

---

## 全局约定

### 传输与鉴权头

| 项目     | 说明                                                           |
|--------|--------------------------------------------------------------|
| 协议     | HTTPS                                                        |
| 字符集    | UTF-8                                                        |
| 鉴权     | 除「获取访问凭证」外，业务接口需携带请求头：`Authorization: Bearer <access_token>` |

### 公共响应体（绝大多数接口）

响应 JSON 根字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | 成功：多数接口为 `0`，**豹小秘使用数据查询**（`/proxyopen/dataopen/...`）为 `200`；其它见 [错误码](#错误码) |
| `msg` | string | 错误描述 |
| `req_id` | string | 请求追踪 id（部分接口无此字段） |
| `data` | object | 业务数据，结构见各接口 |

### 区域

多区域部署时，不同区域使用不同 **`{BASE}`**，由交付文档提供。以下示例均写 `{BASE}`，不绑定具体域名。

### 查询外网出口 IP（用于申请白名单）

```bash
curl --location '{BASE}/v1/myip'
```

返回体中可包含当前探测到的公网 IP（示例形式：`IP: 11.22.33.44`）。

---

## 接入与 IP 授权

1. 向 **科梦奇** 申请开通云端 API，获取 `appid`、`secret`，并登记 **调用服务端** 的外网出口 IP 白名单。  
2. `secret` **仅可放在服务端**，禁止写入客户端或前端。  
3. 获取 `access_token` 时会校验出口 IP；业务接口通常不再重复校验（以平台策略为准）。

---

## 获取访问凭证

### 请求

| 项目 | 值 |
| --- | --- |
| 路径 | `/v1/auth/get_token` |
| Method | `GET` |
| 鉴权 | 无 Bearer；**受 IP 白名单限制** |

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `appid` | string | 是 | 科梦奇分配的 appid |
| `secret` | string | 是 | 科梦奇分配的 secret |
| `grant_type` | string | 是 | 固定 `client_credential` |

**请求示例**

```bash
curl --location '{BASE}/v1/auth/get_token?appid=YOUR_APPID&secret=YOUR_SECRET&grant_type=client_credential'
```

### 响应

**`data` 字段**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `access_token` | string | 访问令牌 |
| `expire_in` | int | 有效期（秒），常见约 7200，以实际返回为准 |

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "access_token": "YOUR_ACCESS_TOKEN",
        "expire_in": 7200
    }
}
```

---

## 企业与组织

### 获取企业信息

| 项目 | 值 |
| --- | --- |
| 路径 | `/v1/corp/corp_info` |
| Method | `GET` |

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ov_corpid` | string | 是 | 企业 id |

**响应 `data`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `corp` | object | 企业信息对象，字段见下表 |

**`corp` 对象字段**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `ov_corpid` | string | 企业 id |
| `corp_name` | string | 企业名称 |
| `create_time` | string | 创建时间戳（秒） |
| `update_time` | string | 最后修改时间戳（秒） |

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "corp": {
            "ov_corpid": "kmq.enterprise.example.12345678",
            "corp_name": "示例企业",
            "create_time": "1711966681",
            "update_time": "1711966681"
        }
    }
}
```

---

## 企业后台 SSO

### 免登录到企业后台首页 URL

| 项目 | 值 |
| --- | --- |
| 路径 | `/v1/sso/corp_admin_sso` |
| Method | `GET` |
| 数据授权 | **企业级** |

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `sso_acct_mobile` | string | 是 | 目标企业账号绑定的手机号；免登录即以此手机号对应账号进入 **科梦奇企业后台（接待后台）** |
| `sso_acct_id` | string | 否 | 贵方业务系统用户 id，平台会记录；最长 64 字节 |
| `sso_acct_name` | string | 否 | 贵方用户昵称或姓名；登录后后台展示名；最长 16 字 |
| `sso_acct_avatar_url` | string | 否 | 头像 URL；最长 256 字节 |

**响应 `data`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `redirect_url` | string | 免登跳转 URL（需在规定时间内打开） |
| `expires_in` | string | URL 有效时间（秒），字符串形式的整数 |

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "redirect_url": "https://your-admin.example.com/capi/v1/corp/admin_sso_auth?sso_token=example&target_type=main",
        "expires_in": "60"
    }
}
```

### 其他 SSO 接口

- `/v1/sso/corp_admin_page_sso`  
- `/v1/sso/init_webapi_access`  

---

## 人员管理

### 添加人员

| 项目 | 值 |
| --- | --- |
| 路径 | `/v1/person/add_person` |
| Method | `POST` |
| Content-Type | `multipart/form-data` |

**表单字段**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ov_corpid` | string | 是 | 企业 id |
| `person` | string（JSON） | 是 | 人员信息 JSON，结构见下「人员信息对象」 |
| `skip_check_same` | int | 否 | 是否跳过人脸重复校验：`0` 检测（默认），`1` 跳过；不传人脸照片且不需校验时传 `1` |
| `face_content_1` | file | 否 | 人脸图 1，仅 jpg/jpeg/png |
| `face_content_2` | file | 否 | 人脸图 2 |
| `face_content_3` | file | 否 | 人脸图 3 |

**人员信息对象 `person`（JSON 内字段）**

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `full_name` | string | 是 | 姓名 |
| `gender` | string | 否 | `0` 未知 `1` 男 `2` 女 |
| `extinfo` | string | 否 | 自定义扩展信息，平台原样存储与回传，最长 512 字 |

**响应 `data`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `person_id` | string | 新建人员 id |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/person/add_person' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --form 'ov_corpid=YOUR_OV_CORPID' \
     --form 'person={"full_name":"张三","gender":"1"}' \
     --form 'face_content_1=@/path/to/face1.jpg'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "req_id": "example_req_id",
    "data": {
        "person_id": "person_abc123"
    }
}
```

---

### 修改人员

| 项目 | 值 |
| --- | --- |
| 路径 | `/v1/person/modify_person` |
| Method | `POST` |
| Content-Type | `multipart/form-data` |

**表单字段**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `person_id` | string | 是 | 目标人员 id |
| `person` | string（JSON） | 是 | 人员信息；字段同添加，均为可选更新项 |
| `skip_check_same` | int | 否 | 同添加 |
| `face_content_1`～`3` | file | 否 | 重置人脸照片 |

**响应 `data`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `person_id` | string | 人员 id |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/person/modify_person' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --form 'person_id=person_abc123' \
     --form 'person={"full_name":"李四"}' \
     --form 'skip_check_same=1'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "req_id": "example_req_id",
    "data": {
        "person_id": "person_abc123"
    }
}
```

---

### 删除人员

| 项目 | 值 |
| --- | --- |
| 路径 | `/v1/person/delete_person` |
| Method | `POST` |
| Content-Type | `application/x-www-form-urlencoded` |

**Body 参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `person_id` | string | 是 | 要删除的人员 id |

**响应 `data`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `person_id` | string | 人员 id |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/person/delete_person' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --header 'Content-Type: application/x-www-form-urlencoded' \
     --data-urlencode 'person_id=person_abc123'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "req_id": "example_req_id",
    "data": {
        "person_id": "person_abc123"
    }
}
```

---

### 通过人脸照片搜索人员

| 项目 | 值 |
| --- | --- |
| 路径 | `/v1/person/search_person_by_multi_face` |
| Method | `POST` |
| Content-Type | `multipart/form-data` |

**表单字段**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ov_corpid` | string | 是 | 企业 id |
| `face_content_1` | file | 是 | 第一张人脸照片 |
| `face_content_2` | file | 否 | 第二张 |
| `face_content_3` | file | 否 | 第三张 |

**响应 `data`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `multi_list` | array | 每张请求照片对应一条结果 |
| `multi_list[].index` | string | 与请求中照片字段名对应，如 `face_content_1` |
| `multi_list[].person_list` | array | 该照片匹配到的人员列表 |

**`person_list` 中单个人员**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `person_id` | string | 人员 id |
| `full_name` | string | 姓名 |
| `gender` | string | 性别 |
| `extinfo` | string | 扩展信息 |
| `create_time` | string | 创建时间戳（秒） |
| `update_time` | string | 更新时间戳（秒） |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/person/search_person_by_multi_face' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --form 'ov_corpid=YOUR_OV_CORPID' \
     --form 'face_content_1=@/path/to/face1.jpg'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "req_id": "example_req_id",
    "data": {
        "multi_list": [
            {
                "index": "face_content_1",
                "person_list": [
                    {
                        "person_id": "person_abc123",
                        "full_name": "张三",
                        "gender": "1",
                        "extinfo": "",
                        "create_time": "1711966681",
                        "update_time": "1711966681"
                    }
                ]
            }
        ]
    }
}
```

---

## 机器人信息

### 获取机器人列表

| 项目 | 值 |
| --- | --- |
| 路径 | `/v1/robot/robot_list` |
| Method | `GET` |
| 数据授权 | **企业级** |

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ov_corpid` | string | 是 | 企业 id；多个用英文逗号分隔 |
| `robot_sn` | string | 否 | 若传则只返回指定设备；多个 sn 逗号分隔 |
| `page` | int | 否 | 页码，默认 1 |
| `page_rows` | int | 否 | 每页条数，默认 1 |

**响应 `data`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `total_count` | string | 总条数 |
| `robot_list` | array | 机器人信息对象列表 |

**机器人信息对象 `robot`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `ov_corpid` | string | 所属企业 id |
| `robot_uuid` | string | 机器人 uuid |
| `robot_sn` | string | 机器人 sn |
| `robot_name` | string | 名称 |
| `robot_version` | string | ROM 版本号 |
| `robot_model` | string | 型号 |
| `bind_time` | string | 最后一次绑定到当前企业的时间戳（秒） |
| `expires_time` | string | 租期截止时间（秒），租用设备有效 |
| `online_status` | string | `0` 离线 `1` 在线（依赖心跳，约 15 秒延迟） |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/robot/robot_list?ov_corpid=YOUR_OV_CORPID&page=1&page_rows=20' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "req_id": "example_req_id",
    "data": {
        "total_count": "1",
        "robot_list": [
            {
                "ov_corpid": "kmq.enterprise.example.12345678",
                "robot_uuid": "uuid-example",
                "robot_sn": "ROBOT_SN_001",
                "robot_name": "演示机器人",
                "robot_version": "V9.7.2024041200.1234US",
                "robot_model": "OS-R-DR01S",
                "bind_time": "1711966681",
                "expires_time": "",
                "online_status": "1"
            }
        ]
    }
}
```

---

### 获取机器人详情

| 项目 | 值 |
| --- | --- |
| 路径 | `/v1/robot/robot_info` |
| Method | `GET` |
| 数据授权 | **企业级** |

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `robot_sn` | string | 是 | 机器人 sn |
| `is_report_status` | int | 否 | 是否返回当前状态：`0` 不返回（默认），`1` 返回电量/任务/位置等 |
| `is_report_task_event` | int | 否 | 是否返回任务事件：`0` 不返回（默认），`1` 返回（须配合 `report_task_type`） |
| `report_task_type` | string | 否 | 任务类型，多个英文逗号分隔；仅 `is_report_task_event=1` 时有效。取值与设备业务类型相关，以 ROM 与交付文档为准 |

**响应 `data`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `corp` | object | 所属企业信息（字段同前文「企业信息对象」） |
| `robot` | object | 机器人信息（字段同「机器人列表」中单条） |
| `robot_report_status` | object | 仅 `is_report_status=1` 时返回，见下表 |
| `robot_task_list` | array | 仅 `is_report_task_event=1` 且 `report_task_type` 有效时返回，见下表 |

**`robot_report_status`（当前状态）**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `battery` | object | 电量信息 |
| `battery.battery_rate` | string | 剩余电量百分比 |
| `battery.is_charging` | string | `0` 未充 `1` 充电中 |
| `task_info` | object | 当前任务 |
| `task_info.task_name` | string | 当前任务名称（如「导航中」「待机」等，以设备上报为准） |
| `task_info.last_task_name` | string | 上一任务名称 |
| `location` | object | 位置信息 |
| `location.state` | string | `ready` 定位成功，`get_lost` 丢失 |
| `location.pos_name` | string | 当前点位默认语言名称 |
| `location.pos_all_name` | object | 多语言名称，如 `zh_CN`、`en_US` |
| `location.emergency` | string | `0` 非急停 `1` 急停 |

**`robot_task_list` 中单条任务事件（摘要）**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `task_id` | string | 任务 id |
| `task_type` | string | 任务类型 |
| `event_type` | string | 事件类型（注意 `te__` 双下划线与 `te_` 单下划线区别） |
| `task_time` / `start_time` / `update_time` | string | 时间戳（秒） |
| `task_data` | object | 任务数据，如 `pos_name` 目的点位 |
| `event_list` | array | 事件上报记录，含 `event_type`、`first_time`、`last_time` |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/robot/robot_info?robot_sn=ROBOT_SN_001&is_report_status=1' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

**响应 JSON 示例（节选，`is_report_status=1` 时含状态字段）**

```json
{
    "code": 0,
    "msg": "",
    "req_id": "example_req_id",
    "data": {
        "corp": {
            "ov_corpid": "kmq.enterprise.example.12345678",
            "corp_name": "示例企业",
            "create_time": "1711966681",
            "update_time": "1711966681"
        },
        "robot": {
            "ov_corpid": "kmq.enterprise.example.12345678",
            "robot_uuid": "uuid-example",
            "robot_sn": "ROBOT_SN_001",
            "robot_name": "演示机器人",
            "robot_version": "V9.7.2024041200.1234US",
            "robot_model": "OS-R-DR01S",
            "bind_time": "1711966681",
            "online_status": "1"
        },
        "robot_report_status": {
            "battery": {
                "battery_rate": "85",
                "is_charging": "0",
                "update_time": "1712046236"
            },
            "task_info": {
                "task_name": "导航中",
                "last_task_name": "待机",
                "update_time": "1712046236"
            },
            "location": {
                "state": "ready",
                "pos_name": "前台",
                "pos_all_name": { "zh_CN": "前台", "en_US": "" },
                "emergency": "0",
                "update_time": "1712046236"
            }
        }
    }
}
```

---

## 地图与点位

### 获取企业地图列表

| 项目 | 值 |
| --- | --- |
| 路径 | `/v1/corp/map_list` |
| Method | `GET` |

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ov_corpid` | string | 是 | 企业 id |
| `map_name` | string | 否 | 地图名称完全匹配；不传则不限 |
| `page` | int | 否 | 页码，默认 1 |
| `page_rows` | int | 否 | 每页条数，默认 1 |

**响应 `data`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `total_count` | string | 总条数 |
| `map_list` | array | 每项含 `map` 对象 |

**`map` 对象**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `map_id` | string | 地图 id |
| `map_name` | string | 地图名称 |
| `create_time` | string | 创建时间戳（秒） |
| `update_time` | string | 更新时间戳（秒） |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/corp/map_list?ov_corpid=YOUR_OV_CORPID&page=1&page_rows=10' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "total_count": "1",
        "map_list": [
            {
                "map": {
                    "map_id": "map_001",
                    "map_name": "一楼地图",
                    "create_time": "1547021774",
                    "update_time": "1562222558"
                }
            }
        ]
    }
}
```

---

### 获取企业地图点位列表

| 项目 | 值 |
| --- | --- |
| 路径 | `/v1/corp/map_position_list` |
| Method | `GET` |

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ov_corpid` | string | 是 | 企业 id |
| `map_name` | string | 是 | 地图名称，多个地图用英文逗号分隔 |

**响应 `data.map_position_list[]`**

| 字段 | 说明 |
| --- | --- |
| `map` | 地图信息对象（`map_id`、`map_name`、`create_time`、`update_time`） |
| `position_list` | 点位数组；每项内 `position` 含 `map_name`、`pos_name` |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/corp/map_position_list?ov_corpid=YOUR_OV_CORPID&map_name=一楼地图' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "map_position_list": [
            {
                "map": {
                    "map_id": "map_001",
                    "map_name": "一楼地图",
                    "create_time": "1547021774",
                    "update_time": "1562222558"
                },
                "position_list": [
                    {
                        "position": {
                            "map_name": "一楼地图",
                            "pos_name": "前台"
                        }
                    }
                ]
            }
        ]
    }
}
```

---

### 获取机器人地图点位列表

| 项目 | 值 |
| --- | --- |
| 路径 | `/v1/robot/map/position_list` |
| Method | `GET` |

**Query 参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `robot_sn` | string | 是 | 机器人 sn |
| `pos_name` | string | 否 | 点位名称完全匹配 |
| `lang` | string | 否 | 语言，默认 `zh_CN` |

**响应 `data.position_list[]`**

每项含 `position`：`map_name`、`pos_name`。

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/robot/map/position_list?robot_sn=ROBOT_SN_001&lang=zh_CN' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "position_list": [
            {
                "position": {
                    "map_name": "一楼地图",
                    "pos_name": "前台"
                }
            }
        ]
    }
}
```

---

## 远程控制 Pipe

### 公共说明

- 多数 Pipe 为 `POST`，`Content-Type: application/x-www-form-urlencoded`。  
- 批量下发时常见参数：`robot_sn`（多个 sn 英文逗号分隔）、`is_batch=1`。  
- **响应 `data.result_list`**：数组元素为「指令下发提交结果」，结构如下：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `ret` | string | `0` 成功，非 `0` 失败 |
| `msg` | string | 结果说明 |
| `msg_id` | string | 全局唯一指令消息 id（部分后续查询依赖） |
| `robot` | object | 含 `robot_sn` 等 |

---

### 导航到指定点位

| 路径 | `/v1/robot/pipe/cmd_navigation` |
| Method | POST |

**Body**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `robot_sn` | string | 是 | 多个 sn 逗号分隔 |
| `is_batch` | int | 是 | 固定 `1` |
| `msg_value` | string | 是 | 目标点位名称 |
| `speed` | float | 否 | 导航速度（米/秒） |
| `confirm_timeout` | int | 否 | 端上确认倒计时秒数；`0` 表示不弹窗立即执行 |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/robot/pipe/cmd_navigation' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --header 'Content-Type: application/x-www-form-urlencoded' \
     --data-urlencode 'robot_sn=ROBOT_SN_001' \
     --data-urlencode 'is_batch=1' \
     --data-urlencode 'msg_value=会议室'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "req_id": "example_req_id",
    "data": {
        "result_list": [
            {
                "ret": "0",
                "msg": "",
                "msg_id": "msg_id_example",
                "robot": { "robot_sn": "ROBOT_SN_001" }
            }
        ]
    }
}
```

---

### 取消导航

| 路径 | `/v1/robot/pipe/cmd_navigation_stop` |
| Method | POST |

**Body**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `robot_sn` | string | 是 | 多个 sn 逗号分隔 |
| `is_batch` | int | 是 | `1` |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/robot/pipe/cmd_navigation_stop' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --header 'Content-Type: application/x-www-form-urlencoded' \
     --data-urlencode 'robot_sn=ROBOT_SN_001' \
     --data-urlencode 'is_batch=1'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "result_list": [
            {
                "ret": "0",
                "msg": "",
                "msg_id": "msg_id_example",
                "robot": { "robot_sn": "ROBOT_SN_001" }
            }
        ]
    }
}
```

---

### 执行语音指令

| 路径 | `/v1/robot/pipe/cmd_exec_voice_cmd` |

**Body**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `robot_sn` | string | 是 | 多个 sn 逗号分隔 |
| `is_batch` | int | 是 | `1` |
| `msg_value` | string | 是 | 语音指令文本 |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/robot/pipe/cmd_exec_voice_cmd' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --header 'Content-Type: application/x-www-form-urlencoded' \
     --data-urlencode 'robot_sn=ROBOT_SN_001' \
     --data-urlencode 'is_batch=1' \
     --data-urlencode 'msg_value=带我去会议室'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "result_list": [
            {
                "ret": "0",
                "msg": "",
                "msg_id": "msg_id_example",
                "robot": { "robot_sn": "ROBOT_SN_001" }
            }
        ]
    }
}
```

---

### 播放 TTS

| 路径 | `/v1/robot/pipe/cmd_play_tts` |

**Body**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `robot_sn` | string | 是 | 多个 sn 逗号分隔 |
| `is_batch` | int | 是 | `1` |
| `msg_value` | string | 是 | 要播报的文本 |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/robot/pipe/cmd_play_tts' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --header 'Content-Type: application/x-www-form-urlencoded' \
     --data-urlencode 'robot_sn=ROBOT_SN_001' \
     --data-urlencode 'is_batch=1' \
     --data-urlencode 'msg_value=你好，欢迎光临'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "result_list": [
            {
                "ret": "0",
                "msg": "",
                "msg_id": "msg_id_example",
                "robot": { "robot_sn": "ROBOT_SN_001" }
            }
        ]
    }
}
```

---

### 切换工作模式

| 路径 | `/v1/robot/pipe/cmd_set_task_mode` |

**Body**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `robot_sn` | string | 是 | 多个 sn 逗号分隔 |
| `task_mode` | string | 是 | 工作模式枚举，取值与设备型号与业务场景相关；**不包含**餐饮送餐、递送、回盘等专用模式。常见示例：`attract_customers`（揽客）、`guide_seat`（领位）等，完整列表以科梦奇交付说明为准 |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/robot/pipe/cmd_set_task_mode' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --header 'Content-Type: application/x-www-form-urlencoded' \
     --data-urlencode 'robot_sn=ROBOT_SN_001' \
     --data-urlencode 'task_mode=attract_customers'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "result_list": [
            {
                "ret": "0",
                "msg": "",
                "msg_id": "msg_id_example",
                "robot": { "robot_sn": "ROBOT_SN_001" }
            }
        ]
    }
}
```

---

### 查询切换工作模式执行状态

| 路径 | `/v1/robot/pipe/cmd_set_task_mode/status` |
| Method | GET |

**Query**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `msg_id` | string | 是 | 下发「切换工作模式」时返回的 `msg_id` |

**响应 `data`**

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `report_time` | string | 最近一次状态上报时间戳（秒）；未上报为 `0` |
| `report_data` | object | 上报内容 |
| `report_data.task_mode` | string | 当前工作模式 |
| `report_data.set_result` | string | `0` 成功，非 `0` 失败；未上报则可能无此字段 |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/robot/pipe/cmd_set_task_mode/status?msg_id=msg_id_example' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "report_time": "1717075068",
        "report_data": {
            "task_mode": "attract_customers",
            "set_result": "0"
        }
    }
}
```

---

### 休眠 / 唤醒 / 重启 / 关机 / 去充电 / 停止充电

下列接口响应均为 `data.result_list`，元素结构同 [公共说明](#远程控制-pipe) 中的「指令下发提交结果」。

| 路径 | Method | 请求体要点 |
| --- | --- | --- |
| `/v1/robot/pipe/cmd_power_sleep` | POST | `robot_sn`（必填，多 sn 逗号分隔） |
| `/v1/robot/pipe/cmd_power_wakeup` | POST | `robot_sn` |
| `/v1/robot/pipe/cmd_reboot` | POST | `robot_sn`（以交付说明为准，常与休眠同类） |
| `/v1/robot/pipe/cmd_poweroff` | POST | 同上 |
| `/v1/robot/pipe/cmd_go_charging` | POST | `robot_sn` + `is_batch=1` |
| `/v1/robot/pipe/cmd_stop_charging` | POST | `robot_sn` + `is_batch=1` |

**请求 cURL 示例（休眠）**

```bash
curl --location '{BASE}/v1/robot/pipe/cmd_power_sleep' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --header 'Content-Type: application/x-www-form-urlencoded' \
     --data-urlencode 'robot_sn=ROBOT_SN_001'
```

**响应 JSON 示例（与多数 Pipe 一致，均为 `result_list`）**

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "result_list": [
            {
                "ret": "0",
                "msg": "",
                "msg_id": "msg_id_example",
                "robot": { "robot_sn": "ROBOT_SN_001" }
            }
        ]
    }
}
```

---

## QA 问答

企业级与设备级均使用 JSON；**成功时 `code` 一般为 `0`**（若与公共章节不一致，以实际联调为准）。

### 添加企业级 QA

| 路径 | `/v1/qa/add_corp_qa` |
| Method | POST |
| Content-Type | `application/json; charset=utf-8` |

**Body（根字段）**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ov_corpid` | string | 是 | 企业 id |
| `lang` | string | 是 | 如 `zh_CN`、`en_US` |
| `qa` | object | 是 | 问答内容，见下 |

**`qa` 对象**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `question` | string[] | 是 | 问题列表，每项不超过 25 字 |
| `keyword` | string | 否 | 关键字，多个用英文分号分隔，总长不超过 25 字 |
| `answer` | string[] | 是 | 文本答案列表，每项不超过 500 字 |
| `image_info` | object[] | 否 | 图片答案，最多 10 张；每项含 `src`、`width`、`height` |
| `video_info` | object[] | 否 | 视频答案，通常仅 1 个；与图片答案 **不宜同时** 使用；含 `src`、`backgroundImage`、`imageWidth`、`imageHeight` |

**响应 `data`**

| 字段 | 说明 |
| --- | --- |
| `qapair_id` | 问答条目 id |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/qa/add_corp_qa' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --header 'Content-Type: application/json; charset=utf-8' \
     --data '{"ov_corpid":"YOUR_OV_CORPID","lang":"zh_CN","qa":{"question":["营业时间"],"answer":["周一至周五 9:00-18:00"]}}'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "qapair_id": "qa_pair_001"
    }
}
```

### 修改 / 删除企业级 QA

- `POST /v1/qa/modify_corp_qa`：Body 含 `ov_corpid`、`lang`、`qapair_id`、`qa`（结构同添加）。  
- `POST /v1/qa/delete_corp_qa`：Body 含 `ov_corpid`、`lang`、`qapair_id`。  
- 响应 `data` 常含 `qapair_id`。

**删除请求 cURL 示例**

```bash
curl --location '{BASE}/v1/qa/delete_corp_qa' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --header 'Content-Type: application/json; charset=utf-8' \
     --data '{"ov_corpid":"YOUR_OV_CORPID","lang":"zh_CN","qapair_id":"qa_pair_001"}'
```

**删除响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "qapair_id": "qa_pair_001"
    }
}
```

### 获取企业级 QA 列表

| 路径 | `/v1/qa/corp_qa_list` | GET |

**Query**

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `ov_corpid` | 是 | 企业 id |
| `lang` | 是 | 语言 |
| `question` | 否 | 问题搜索 |
| `keyword` | 否 | 关键字搜索 |
| `answer` | 否 | 答案搜索 |
| `page` / `page_rows` | 否 | 分页，默认每页 20 |

**响应 `data`**

| 字段 | 说明 |
| --- | --- |
| `total_count` | 总条数 |
| `qa_list` | 列表项含 `qapair_id`、`question`、`keyword`、`answer`、`image_info`、`video_info` |

**请求 cURL 示例**

```bash
curl --location '{BASE}/v1/qa/corp_qa_list?ov_corpid=YOUR_OV_CORPID&lang=zh_CN&page=1&page_rows=20' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

**响应 JSON 示例**

```json
{
    "code": 0,
    "msg": "",
    "data": {
        "total_count": "1",
        "qa_list": [
            {
                "qapair_id": "qa_pair_001",
                "question": ["营业时间"],
                "keyword": ["营业;时间"],
                "answer": ["周一至周五 9:00-18:00"]
            }
        ]
    }
}
```

### 设备级 QA

- 添加：`POST /v1/qa/add_robot_qa`，在 Body 中增加 `robot_sn` 为 **字符串数组**。  
- 修改：`POST /v1/qa/modify_robot_qa`。  
- 删除：`POST /v1/qa/delete_robot_qa`（参数以企业级删除为参考，部分版本需带 `robot_sn`，以联调为准）。  
- 列表：`GET /v1/qa/robot_qa_list`，Query 含 `ov_corpid`、`robot_sn`、`lang` 等。

**设备级添加请求 cURL 示例**

```bash
curl --location '{BASE}/v1/qa/add_robot_qa' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --header 'Content-Type: application/json; charset=utf-8' \
     --data '{"ov_corpid":"YOUR_OV_CORPID","robot_sn":["ROBOT_SN_001"],"lang":"zh_CN","qa":{"question":["设备问答"],"answer":["这是回答"]}}'
```

**设备级列表请求 cURL 示例**

```bash
curl --location '{BASE}/v1/qa/robot_qa_list?ov_corpid=YOUR_OV_CORPID&robot_sn=ROBOT_SN_001&lang=zh_CN' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

---

## 使用数据查询

查询 产品线机器人使用数据。下列路径均相对于 `{BASE}`，**Method 均为 `POST`**，`Content-Type: application/x-www-form-urlencoded`，需 `Authorization: Bearer <access_token>`。数据维度为 **企业级授权**。

**日期参数 `compare`（必填）**

| 写法 | 含义 |
| --- | --- |
| `YYYY-MM-DD` | 单日 |
| `YYYY-MM-DD,YYYY-MM-DD` | 起止区间（含首尾） |

未特别声明时，时区为北京时间。

**公共 Body 参数（各子接口在基础上可增加专有字段）**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `ov_corpid` | string | 是 | 企业 id |
| `compare` | string | 是 | 见上表 |
| `product_line` | string | 否 | 机器人型号，传入则只统计该型号 |
| `devices` | string | 否 | 机器人 SN，多个英文逗号分隔 |
| `pageIndex` | int | 否 | 明细分页页码，从 1 开始，默认 1 |
| `pageSize` | int | 否 | 每页条数，默认 10，建议不超过 50 |

**响应**：本组接口成功时根字段 **`code` 为 `200`**（与文档其它多数接口的 `code: 0` 不同）。`data` 内常见 `list` 数组及 `pagination`（`pageIndex`、`pageSize`、`total`）。列表项常含 `count_time`、`version`、`device_id`、`device_name`、`enterprise_id`、`enterprise_name` 等。

### 接口列表

| 分类 | 路径 | 名称 | 数据更新特点 |
| --- | --- | --- | --- |
| 功能指标 | `/proxyopen/dataopen/flow_list` | 客流和招揽汇总 | 准实时；豹小秘 V6.4 以上不支持客流，见字段 `person_type` |
| 功能指标 | `/proxyopen/dataopen/wakeup_detail_list` | 唤醒汇总 | 准实时 |
| 功能指标 | `/proxyopen/dataopen/machine_interaction_list` | 交互汇总 | 准实时 |
| 功能指标 | `/proxyopen/dataopen/machine_guide_introduction_list` | 导览汇总 | 准实时 |
| 功能指标 | `/proxyopen/dataopen/machine_patrol_introduction_list` | 巡逻汇总 | 每日约 6 点前更新 |
| 功能指标 | `/proxyopen/dataopen/machine_guide_list` | 引领汇总 | 准实时 |
| 功能指标 | `/proxyopen/dataopen/machine_ask_list` | 问路汇总 | 准实时 |
| 功能指标 | `/proxyopen/dataopen/machine_reception_introduction_list` | 来访接待汇总 | 准实时 |
| 功能指标 | `/proxyopen/dataopen/machine_dialog_list` | 语音对话次数汇总 | 准实时 |
| 功能指标 | `/proxyopen/dataopen/percent_dialog_mode_count` | 语音对话领域汇总 | 每日约 6 点前更新 |
| 功能指标 | `/proxyopen/dataopen/voice_list` | 语音对话明细 | 准实时 |
| 问答分析 | `/proxyopen/dataopen/entity_words_cloud_open` | 关键词词云汇总 | 每日约 6 点前更新；最多约 100 条，不分页 |
| 问答分析 | `/proxyopen/dataopen/percent_interaction_mode_count` | 不同交互类型占比汇总 | 每日约 6 点前更新 |
| 用户画像 | `/proxyopen/dataopen/crowd_analysis_overview` | 用户画像占比汇总 | 每日约 9 点前更新 |

### 客流和招揽汇总

| 项目 | 值 |
| --- | --- |
| 路径 | `/proxyopen/dataopen/flow_list` |

**Body 附加参数**

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `person_type` | string | 是 | `flow`：客流；`advance`：招揽 |

**响应 `data`（摘要）**

| 字段 | 说明 |
| --- | --- |
| `list` | 汇总列表 |
| `list[].person_num` | 当日客流或招揽汇总值 |
| `pagination` | 分页信息 |

**请求 cURL 示例**

```bash
curl --location '{BASE}/proxyopen/dataopen/flow_list' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --header 'Content-Type: application/x-www-form-urlencoded' \
     --data-urlencode 'ov_corpid=YOUR_OV_CORPID' \
     --data-urlencode 'compare=2024-02-01,2024-02-03' \
     --data-urlencode 'person_type=flow'
```

**响应 JSON 示例**

```json
{
    "code": 200,
    "msg": "",
    "data": {
        "list": [
            {
                "enterprise_id": "kmq.enterprise.example.12345678",
                "device_id": "ROBOT_SN_001",
                "device_name": "豹小秘-1",
                "count_time": "2024-02-01",
                "version": "V6.5",
                "person_num": "578",
                "enterprise_name": "示例企业"
            }
        ],
        "pagination": {
            "pageIndex": 1,
            "pageSize": 10,
            "total": 1
        }
    }
}
```

### 唤醒汇总

| 项目 | 值 |
| --- | --- |
| 路径 | `/proxyopen/dataopen/wakeup_detail_list` |

仅使用上文「公共 Body 参数」表，无额外必填字段。

**响应 `data.list[]` 常见字段**

| 字段 | 说明 |
| --- | --- |
| `num` | 当日唤醒次数 |

**请求 cURL 示例**

```bash
curl --location '{BASE}/proxyopen/dataopen/wakeup_detail_list' \
     --header 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
     --header 'Content-Type: application/x-www-form-urlencoded' \
     --data-urlencode 'ov_corpid=YOUR_OV_CORPID' \
     --data-urlencode 'compare=2024-02-01'
```

其余子接口均为 **同一 Method、同一公共参数规则**，路径见本节「接口列表」表；各接口 `data.list` 字段名不同，以联调返回为准。

---

## 错误码

| code / 信息关键词 | 说明 |
| --- | --- |
| `200` | **使用数据查询**等 `/proxyopen/dataopen/...` 接口成功时根字段 `code` 为 `200` |
| `0` | 多数其它业务接口成功时为 `0` |
| `101002` 等 | `appid` / `secret` / `grant_type` 等缺失或非法 |
| `101009` | `appid` 或 `secret` 未正确传递 |
| — | `access_token` 与 `appid` 不匹配，或 token 格式错误 |
| — | 接口未授权；企业/代理商 id 越权 → 联系 **科梦奇** 开通 |
| `101025` | 外网 IP 未在白名单；`msg` 中可能含探测到的 IP |
| `101024` | `access_token` 过期，请重新获取 |

---

## 事件与回调

向 **科梦奇** 登记回调 URL 后，平台可主动推送 **任务状态**、**设备预警** 等。

**任务回调**：多为 `POST`，支持 `application/x-www-form-urlencoded` 与 `multipart/form-data`；常见字段含 `open_appid`、`open_code`（回调鉴权暗码）、`robot_sn`、`task_id`、`task_type`、`event_type`、`task_data`、`event_data` 等；超时约 5 秒，顺序不保证。

**预警回调**：多为 `POST`，`Content-Type: application/json`；需在管理后台配置策略后生效。

**任务回调请求示例（平台 POST 到贵方 URL，字段以实际配置为准）**

```bash
curl --location 'https://your-domain.com/your_callback' \
     --form 'open_appid=YOUR_APPID' \
     --form 'open_code=YOUR_CALLBACK_SECRET' \
     --form 'robot_sn=ROBOT_SN_001' \
     --form 'ov_corpid=YOUR_OV_CORPID' \
     --form 'task_id=task_example_id' \
     --form 'task_type=example_task_type' \
     --form 'event_type=te__start' \
     --form 'task_data={}' \
     --form 'event_data={}'
```

**预警回调请求 JSON 示例（节选）**

```json
{
    "open_appid": "YOUR_APPID",
    "open_code": "YOUR_CALLBACK_SECRET",
    "ov_corpid": "YOUR_OV_CORPID",
    "alarm_type": "2",
    "robot_list": [
        {
            "robot_sn": "ROBOT_SN_001",
            "robot_name": "演示机器人",
            "abnormal_info": { "battery_rate": 5 }
        }
    ],
    "alarm_config": { "battery_rate": 10 }
}
```

贵方应返回 HTTP **200** 表示接收成功。
