# NUISTin

<image src="https://raw.githubusercontent.com/zhufucdev/NUISTin/main/icon/linux.svg" width="100"/>

[![Package for all platforms](https://github.com/zhufucdev/NUISTin/actions/workflows/package.yml/badge.svg)](https://github.com/zhufucdev/NUISTin/actions/workflows/package.yml)

NUIST校园网自动登录程序

该项目使用 [Compose Multiplatform](https://github.com/JetBrains/compose-jb)
为macOS, Windows 和 Linux构建桌面应用程序

可按需发登录请求，保证网络通畅

## 安装&卸载

从Release中下载对应平台的安装包

### Windows

下载`windows_x64.msi`并打开，跟随向导安装

打开文件管理器，导航到安装路径，默认为`C:\Program Files\NUISTin`

从这里启动，或创建快捷方式

若要卸载NUISTin，打开下载的msi文件，选择卸载，并将目录`%APPDATA%\NUISTin`删除

### macOS

下载`darwin_aarch64.dmg`，若为intel机型，选择`darwin_x64.dmg`，
挂载并拖拽至应用程序文件夹

若要卸载，删除应用程序文件夹下对应的.app文件，并将目录`~/Library/Application Support/NUISTin`删除

### Linux

若是Linux用户，您应该知道怎么做，just in case

```shell
cd ~/Downloads
sudo apt install ./debain_x64.deb
```

若要卸载

```shell
sudo apt purge nuistin
rm ~/.nuistin
```

## REST API

万一你想知道软件是怎么工作的

### 获取内网IP

API: `http://a.nuist.edu.cn/api/v1/ip`

Response:

```json
{
  "code": 200,
  "data": "your ip addr"
}
```

提交登录请求时须得知内网ip，此方法的好处在于操作系统独立

### 登录请求

API: `http://a.nuist.edu.cn/api/v1/login`

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
如果你是开发者，请使用Jetbrains Intellij IDEA管理和维护这个项目

使用Gradle来调试

```shell
./gradlew run
```

为当前平台编译
```shell
./gradlew packageDistributionForCurrentOS
```

鉴于JVM的宗旨是
> Code once, debug everywhere

没有交叉编译选项
