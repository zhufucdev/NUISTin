# NUISTin

NUIST校园网自动登录程序

该项目使用 [Compose Multiplatform](https://github.com/JetBrains/compose-jb)
为macOS, Windows 和 Linux构建桌面应用程序

可按需发登录请求，保证网络通畅

## REST API

### 获取内网IP

API: `http://10.255.255.34/api/v1/ip`

Response:

```json
{
  "code": 200,
  "data": "your ip addr"
}
```

提交登录请求时须得知内网ip，此方法的好处在于操作系统独立

### 登录请求

API: `http://10.255.255.34/api/v1/login`

Body:

```json
{
  "channel": "channel id",
  "username": "usually your phone number",
  "password": "your password",
  "pagesign": "secondauth",
  "ifautologin": "0",
  "usripadd": "your ip addr"
}
```

Response:

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "reauth": false,
    "username": "usually your phone",
    "balance": "0.00",
    "duration": "0",
    "outport": "中国电信/移动/联通",
    "totaltimespan": "0",
    "usripadd": "your ip addr"
  }
}
```

`Body`中`channel`为各运营商的序号，目前如下：

|  序号  |  运营商  |
|:----:|:-----:|
|  2   |  移动   |
|  3   |  电信   |
|  4   |  联通   |

其他都是些bullshit

校园网不支持IPv6和p2p，非常摩登

## 编译&运行

使用Jetbrains Intellij IDEA管理和维护这个项目

使用Gradle来编译

```shell
./gradlew run
```
