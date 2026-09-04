# JOYZL Backdoor

为客户端程序提供局域网内的远程参数配置、文件接收、脚本执行支持；
基于 UDP 实现，因此只能支持局域网；

## 主要功能

* 接收普通文本消息(ASCII)以回车或换行结束；
* 接收维护端推送文件包然后由客户端逐块下载，采用慢速以减少对客户端资源占用；
* 接收维护的推送的脚本文件名，客户端执行指定位置脚本。

## 推送文件包

文件包只能是 .zip 或 .tar.gz 压缩包，分别用于 Windows 和 Linux 程序更新；

## 脚本执行

支持 Windows PowerSell(.ps1) 脚本文件；支持 Linux 的 bash Shell(.sh) 脚本文件。

注意：此功能存在一定风险，可能被恶意利用执行恶意脚本。

---
[www.joyzl.com](http://www.joyzl.com)

本软件根据 Apache License 2.0 开源，详见 LICENSE 文件。
