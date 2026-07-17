# Walkthrough - Java + Ollama AI Integration

Raspberry Pi側のJavaサーバーを更新し、通知を受信した際に自動的にAIモデル（Ollama）を実行してメッセージを解析するようにしました。

## 変更点

### ガイドの更新
- **[raspberry_pi_setup_java.artifact.md](file:///C:/Users/ytaka/fbm/.artifacts/36b13006-0d03-4e01-ac75-c2685ce1f18e/raspberry_pi_setup_java.artifact.md)** を更新しました。
    - **Ollamaのインストール手順:** ラズパイでAIを動かすためのセットアップ方法を追記しました。
    - **Javaコードの刷新:** 受信したJSONデータからメッセージ内容を抽出し、`ProcessBuilder` を使って `ollama run yutayuma-ai` をコマンドラインから実行するロジックを実装しました。
    - **AI応答の表示:** AIが生成したテキストをリアルタイムでコンソールに出力する処理を追加しました。

## 使い方

1. ラズパイ側で新しい `NotificationServer.java` を作成・上書きします。
2. `javac NotificationServer.java` でコンパイルし、`java NotificationServer` で実行します。
3. Androidアプリから通知が届くと、ラズパイの画面上で自動的にAIによる解析が始まります。

## 注意事項
- ラズパイのスペックによりAIの応答速度が異なります。
- `yutayuma-ai` というモデルがラズパイに作成されている必要があります。
