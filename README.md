# MaimaiData

MaimaiData是一款为舞萌DX玩家开发的Android App

包含歌曲信息检索，谱面信息查看，rating计算，分数查看等功能

## 最近更新

### v2.6.2 Latest
1. 添加资源文件
2. 修复部分歌曲无法上传到查分器的问题
3. 修复无法获取公众号成绩的问题

### v2.6.0 

1. 适配舞萌DX2025
2. 增加了历史搜索功能
3. 增加了传分功能
   实现方式来自[bakapiano](https://github.com/bakapiano/maimaidx-prober-updater-android)
4. 支持了id搜索歌曲
5. 部分界面布局调整

本版本开始数据由读取json文件修改为本地数据库获取，可能会导致意料之外的问题，请窒息

### v2.5.4
1. 修复打开版本进度崩溃问题
2. 增加歌曲id显示
3. 增加谱师名复制
4. 设置中可开启谱师名搜索和B50图片自定义昵称

### v2.5.3
1. 进度表记忆上次选择的版本和等级
2. 歌曲详情界面显示国服添加版本和谱面类型
3. 歌曲详情界面长按标题和别名可以进行复制
4. 歌曲详情界面点击歌曲封面可查看大图

### v2.5.0
1. 更新主题色为prism
2. 优化搜索布局，支持了歌曲别名和定数等级的搜索
3. 增加了歌曲别名、拟合定数的显示
4. 增加了曲目列表右侧的快速滚动条
5. 增加了账号切换功能
6. 谱面note分布改为图表显示

## 已知问题
标准谱追加的DX谱、DX谱追加的标准谱的日服添加版本缺少可判断的字段，暂时显示为前者添加版本

## 感谢
感谢[Diving-Fish](https://github.com/Diving-Fish/maimaidx-prober)提供的谱面数据

感谢[maimaiDX](https://github.com/Yuri-YuzuChaN/maimaiDX)提供的别名库

传分功能实现方式来自[bakapiano](https://github.com/bakapiano/maimaidx-prober-updater-android)

## 许可证
本项目基于 Apache License 2.0 开源许可证发布。您可以在遵守许可证条款的前提下自由使用、修改和分发本软件。