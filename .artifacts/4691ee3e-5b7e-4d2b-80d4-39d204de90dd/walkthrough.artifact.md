# 変更内容：通知データのラズベリーパイ送信機能の実装

通知を読み取り、指定されたラズベリーパイのサーバーにネットワーク経由で送信する機能を実装しました。

## 実施した主な変更

### 1. ネットワーク通信のセットアップ
- `app/build.gradle.kts` に `OkHttp` ライブラリを追加しました。
- `AndroidManifest.xml` に `INTERNET` 権限を追加し、アプリが外部サーバーと通信できるようにしました。

### 2. 設定 UI の追加 ([fragment_first.xml](file:///C:/Users/ytaka/fbm/app/src/main/res/layout/fragment_first.xml))
- アプリのメイン画面に、送信先となるラズベリーパイの URL を入力するフィールドと、設定を保存するボタンを追加しました。

### 3. 設定の保存機能 ([FirstFragment.java](file:///C:/Users/ytaka/fbm/app/src/main/java/com/example/fbm/FirstFragment.java))
- `SharedPreferences` を使用して、入力された URL を端末内に保存し、アプリ再起動後も保持されるようにしました。

### 4. 送信クライアントの作成 ([NetworkClient.java](file:///C:/Users/ytaka/fbm/app/src/main/java/com/example/fbm/NetworkClient.java)) [NEW]
- 通知データ（パッケージ名、タイトル、本文）を JSON 形式で POST 送信する共通クラスを作成しました。

### 5. 通知受信時の送信処理 ([MyNotificationListenerService.java](file:///C:/Users/ytaka/fbm/app/src/main/java/com/example/fbm/MyNotificationListenerService.java))
- 通知を検知した際、保存された URL があれば自動的に `NetworkClient` を介してデータを送信するロジックを統合しました。

## 検証結果

- **ビルド確認**: `app:assembleDebug` を実行し、正常にビルドが完了することを確認しました。
- **UI確認**: 設定画面に入力項目が表示され、保存ボタンが動作することを確認しました。

## 使用方法

1. アプリを起動し、メイン画面の入力欄にラズベリーパイの受信サーバーの URL（例: `http://192.168.1.10:5000/notify`）を入力します。
2. 「Save Configuration」ボタンを押して保存します。
3. Gmail や LINE の通知が届くと、設定した URL に対して通知データが POST 送信されます。

> [!IMPORTANT]
> ラズベリーパイ側で、JSON形式の POST リクエストを受け取れるサーバープログラム（Python の Flask 等）をあらかじめ動作させておいてください。
