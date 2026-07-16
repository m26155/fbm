# 実装プラン：通知をラズベリーパイに送信する

現在の通知リスナー機能を拡張し、傍受した通知データ（パッケージ名、タイトル、本文）をネットワーク経由でラズベリーパイに送信することを目的とします。

## ユーザーによる確認が必要な事項

> [!IMPORTANT]
> このプランは、ラズベリーパイ側でHTTP POSTリクエストを受信できるWebサーバーが動作していることを前提としています。アプリ内でラズベリーパイのIPアドレスとポートを指定する必要があります。

> [!NOTE]
> ネットワークリクエストを効率的に処理するため、`OkHttp`ライブラリを追加します。

## 変更案

### ビルド設定

#### [修正] [build.gradle.kts](file:///C:/Users/ytaka/fbm/app/build.gradle.kts) および [libs.versions.toml](file:///C:/Users/ytaka/fbm/gradle/libs.versions.toml)
- HTTPリクエストを容易にするため、`okhttp`の依存関係を追加します。

### マニフェスト

#### [修正] [AndroidManifest.xml](file:///C:/Users/ytaka/fbm/app/src/main/AndroidManifest.xml)
- アプリがネットワーク経由でデータを送信できるように、`INTERNET`パーミッションを追加します。

### UIと設定

#### [修正] [fragment_first.xml](file:///C:/Users/ytaka/fbm/app/src/main/res/layout/fragment_first.xml)
- ラズベリーパイのIPアドレス/URL入力用の`EditText`を追加します。
- 設定を保存するための「保存」ボタンを追加します。

#### [修正] [FirstFragment.java](file:///C:/Users/ytaka/fbm/app/src/main/java/com/example/fbm/FirstFragment.java)
- `SharedPreferences`を使用して、ラズベリーパイのURLを保存・読み込みするロジックを実装します。

### ネットワークロジック

#### [新規] [NetworkClient.java](file:///C:/Users/ytaka/fbm/app/src/main/java/com/example/fbm/NetworkClient.java)
- 通知データをJSON形式でHTTP POSTリクエストとして送信するユーティリティクラスを作成します。

### サービスの統合

#### [修正] [MyNotificationListenerService.java](file:///C:/Users/ytaka/fbm/app/src/main/java/com/example/fbm/MyNotificationListenerService.java)
- `onNotificationPosted`メソッドを更新し、保存されたURLを取得して`NetworkClient`を使用して通知データを送信するようにします。

## 検証プラン

### 自動テスト
- 新しい依存関係とクラスを追加した後、コードが正常にコンパイルされることを確認します。
- 通知受信時にネットワークリクエストが開始されていることをログで確認します。

### 手動検証
- ユーザーがアプリでラズベリーパイのURLを入力し、保存します。
- 通知をトリガーします（例：テスト用のGmailやLINEメッセージを送信）。
- ラズベリーパイ側でデータが受信されていることを確認します。
