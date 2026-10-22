# Shoreline 1.0 — Minecraft 1.21.1 / Mojmap

## 建置

安裝 JDK 21，將 `JAVA_HOME` 指向 JDK 21。首次執行需要網路下載 Gradle、Minecraft 與 Maven 依賴。

Windows：

```bat
gradlew.bat compileJava
```

Linux / macOS：

```sh
chmod +x gradlew
./gradlew compileJava
```

使用 Gradle 8.7、Fabric Loom 1.6.12、`mappings loom.officialMojangMappings()`。

## 原始碼與依賴

- 本體、Discord IPC、Shoreline eventbus 全部由 `src/main/java` 的 774 個 Java 檔案編譯。
- 原 JAR 未作為本體依賴，也未放入此專案。
- 保留原 JAR 的資源與全部 Mixin 設定；開發端 access widener namespace 為 `named`。
- 使用 Fabric Loader 0.16.0、Fabric API 0.102.1+1.21.1、Baritone API 1.2、MixinExtras Fabric 0.4.0。
- Baritone API 是原專案使用的外部 API 依賴；實際遊戲環境仍須安裝相容 Minecraft 1.21.1 的 Baritone 實作。
- Mixin refmap 由 Loom annotation processor 依 Mojmap 原始碼重新產生；原 Yarn refmap 不作為執行期資源沿用。

## 編譯狀態

正式專案的 `compileJava` 在 Loom 配置階段遭環境限制：`java.net.SocketException: Operation not permitted`（Unix domain socket 偵測），未進入 Java 編譯。因此正式 Loom 建置與 Mixin annotation processor 尚未驗證。

為修正反編譯錯誤，另以快取中的 Minecraft、Fabric、Baritone 映射依賴和原 access wideners 執行暫時的離線 Gradle `compileJava`。774 個 Java 檔案全部通過；此離線編譯未執行 annotation processor，不等同正式專案已通過 Loom。

未執行完整 build、遊戲啟動或額外測試。交付內容不包含上述暫時編譯環境、快取或編譯產物。
