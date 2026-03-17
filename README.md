![LibreSpeed-Android Logo](https://github.com/adolfintel/speedtest-android/blob/master/.github/Readme-Logo.png?raw=true)
 
# LibreSpeed Android (电视端本地测速)

这是一个基于 [LibreSpeed Android](https://github.com/librespeed/speedtest-android) 模板开发的修改版本。

**本项目的主要初衷是为了方便在电视机 (Android TV) 上进行本地网络测速**。为此对原版进行了简单的二次开发，取消了硬编码服务器列表的限制，改为支持在 UI 界面中通过遥控器或键盘直接输入本地测速服务器的 IP 和端口。

## 功能特性
* **电视端优化**：支持 Android TV 界面适配，方便遥控器操作。
* **手动配置服务器**：无需修改代码，直接在 App 中输入自建服务器 IP 和端口。
* **本地测速优化**：专为测试内网带宽、局域网 Wi-Fi 性能而设计。
* **Docker 联动**：完美匹配轻量级 Docker 测速服务端。
* **核心指标**：支持下载、上传、延迟 (Ping) 及抖动 (Jitter) 测试。

![Screenshot](https://github.com/librespeed/speedtest-android/blob/master/.github/screenshots.png?raw=true)

## 快速开始 (自建服务端)

推荐在 NAS 或个人电脑上使用 Docker 部署轻量化的 Go 语言版服务端：

```bash
docker run -d --name speedtest -p 8989:8989 minimages/librespeed-speedtest-go
```

**使用步骤：**
1. 在电视上打开本应用。
2. 输入服务器的 **IP 地址** (例如: `192.168.1.100`)。
3. 输入 **端口** (默认为 `8989`)。
4. 点击 **开始 (Start)** 即可开始测速。
