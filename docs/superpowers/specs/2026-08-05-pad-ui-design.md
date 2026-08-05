# PAD UI 响应式适配设计

日期：2026-08-05
范围：横屏平板（expanded 窗口尺寸）为主，手机（compact）布局保持现状不动

## 目标

在现有单模块项目中，为横屏平板（≥840dp）提供自适应 UI，不新建项目、不动数据层。手机布局完全保留。

## 断点策略

- 使用 `androidx.compose.material3:material3-adaptive` 库的 `WindowSizeClass`（`currentWindowAdaptiveInfo()`）
- 两档处理：
  - `compact`（<600dp）及 `medium`（600–840dp）：沿用手机布局，不做双栏
  - `expanded`（≥840dp）：启用 PAD 布局
- 新增依赖 `libs.androidx.compose.material3.adaptive`（在现有 Compose BOM 管理下）

## 全局布局（MainScreen）

### 导航

- tablet：左侧 `NavigationRail`，5 个图标垂直排列，**设置图标固定在最底部**（其余 4 个在上方）
- 手机：现状底部 `NavigationBar` 不动
- 导航逻辑（route 切换、`hideBottomBar` 全屏隐藏）不变；tablet 时全屏逻辑隐藏 NavigationRail

### 顶部行（tablet 专用 TopAppBar）

- 最左侧：App logo
- 中间：全局搜索框（点击进入现有 `GLOBAL_SEARCH` 路由）
- 手机不显示此顶栏

## 各页面适配

### 直播页（LiveScreen）

tablet：三栏 `分类 | 频道列表 | 播放器`

- 分类栏约 200dp，频道列表占剩余中间宽度，播放器固定约 1/3 宽度
- 播放器复用现有内嵌 `PlayerView`（`LiveScreen.kt:312` 的 AndroidView 块，同一 `viewModel.player` ExoPlayer 实例）
- 现有播放控制条、EPG 浮层、`onFullscreenChanged`（隐藏导航）逻辑保留
- 手机布局现状不动

### 电影 / 剧集页（MoviesScreen / SeriesScreen）

tablet 布局改为上下两行 + 网格：

- **第一行**（工具栏）：
  1. 分类 Select：单选，选择分类（含"全部"）
  2. 排序 Select：A-Z、最近添加等（新增功能，当前代码无排序）
  3. 海报大小按钮组：小 / 中 / 大三档，直接映射网格列数：小=6 列、中=5 列、大=4 列（写回现有 `contentGridColumns` 偏好，与设置页联动）
  4. 搜索框（现有按名称过滤逻辑复用）
- **第二行**：`LazyVerticalGrid`，tablet 默认列数按海报大小档位（5–6 列）
- 手机布局现状不动（纵向分类侧栏 + 网格）

### 详情页（MovieDetailScreen / SeriesDetailScreen）

tablet：**上下分栏**

- 上方区域：海报 + 元信息 + 简介（横排展示，信息与海报并排）
- 下方区域：剧集列表（系列）/ 相关内容（电影）
- 手机现状（纵向滚动）不动

### 设置页（SettingsScreen）

tablet：**两列布局**

- 第一列：分组导航列表（对应现有 SectionCard 分组）：
  - 常规、播放、首页、启动、数据、锁定、源管理、关于
- 第二列：当前选中分组的设置项，复用现有行组件（`LanguageRow`、`ThemeColorRow`、`PlaybackSpeedRow` 等）与 `SectionCard` 内容块
- 第一列点击切换第二列内容；"常规"为默认选中
- 手机现状（单列滚动）不动

### EPG 页（EpgTimelineScreen）

- tablet：时间轴全宽展开
- 手机现状不动

### 首页（HomeScreen）

- tablet：内容区 `maxWidth` 限宽居中，网格列数随宽度增多（`GridCells.Adaptive` 已具备）
- 手机现状不动

## 组件拆分

- 每个屏的 tablet 分支抽独立私有 composable，与手机分支互不干扰：
  - `TabletScaffold`（NavigationRail + 顶部 logo/搜索行）
  - `LiveTabletPane`（三栏）
  - `ContentToolbar`（分类 Select + 排序 Select + 海报大小 + 搜索，电影/剧集共用）
  - `SettingsTwoPane`（分组导航 + 设置项）
  - 详情页 `DetailTabletLayout`
- 新增 `MovieSortMode`/`SeriesSortMode` 枚举与排序逻辑（UI 层对现有列表 flow 排序，不进数据层）

## 不动部分

- 数据层（repository / DAO / parser）、domain 层、ViewModel 状态逻辑（除排序外）
- PlayerScreen 全屏播放器、PiP、分类锁定、i18n、更新检查
- 导航路由定义（`Routes.kt`）
- 手机端全部布局

## 错误处理与边界

- `WindowSizeClass` 在折叠屏动态变化时（展开/折叠）自动重组，布局即时切换
- m3u 源模式隐藏电影/剧集 tab 的逻辑对 NavigationRail 同样生效（复用 `visibleItems` 过滤）
- 排序对锁定分类隐藏规则无影响（排序在 UI 层过滤之后应用）

## 验证

1. `./gradlew :app:assembleDebug` 构建通过
2. 回归清单（手机模拟器 390x844）：5 个 tab、直播播放/切换、PiP、分类锁定、语言切换、设置页全部功能
3. PAD 验证（平板模拟器 1280x800 横屏）：NavigationRail 5 图标且设置在底部、顶部 logo+搜索、直播三栏播放、电影/剧集工具栏各控件、详情页上下分栏、设置页两列、EPG 全宽
