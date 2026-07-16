# Raspberry Pi サーバー設定ガイド

このガイドでは、Androidアプリ「フィッシング詐欺撲滅隊メール支所」からの通知を受け取るための、Raspberry Pi側のサーバー構築手順を説明します。

## 1. 準備するもの

- Raspberry Pi (OSがインストール済みであること)
- Python 3
- インターネット接続環境

## 2. 必要なライブラリのインストール

通知を受け取るための軽量なWebサーバーフレームワーク「Flask」を使用します。Raspberry Piのターミナルで以下のコマンドを実行してください。

```bash
pip install flask
```

## 3. 受信用プログラムの作成

任意のディレクトリに `server.py` という名前でファイルを作成し、以下のコードを貼り付けて保存してください。

```python
from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/notify', methods=['POST'])
def notify():
    # Androidアプリから送信されたJSONデータを取得
    data = request.json

    if not data:
        return jsonify({"status": "error", "message": "No data received"}), 400

    package_name = data.get('package', 'Unknown')
    title = data.get('title', 'No Title')
    text = data.get('text', 'No Text')

    print(f"--- 通知を受信しました ---")
    print(f"アプリ: {package_name}")
    print(f"タイトル: {title}")
    print(f"内容: {text}")
    print(f"--------------------------")

    # ここにAI判定ロジックや追加の処理を記述できます

    return jsonify({"status": "success", "message": "Notification received"}), 200

if __name__ == '__main__':
    # 0.0.0.0 で待機することでネットワーク内の他デバイスからアクセス可能にします
    # ポート番号はAndroidアプリの設定と合わせる必要があります（デフォルト: 5000）
    app.run(host='0.0.0.0', port=5000)
```

## 4. サーバーの起動

ターミナルで以下のコマンドを実行してサーバーを起動します。

```bash
python server.py
```

起動すると、以下のようなメッセージが表示されます。
`* Running on http://192.168.x.x:5000/ (Press CTRL+C to quit)`

## 5. Androidアプリの設定

1.  Raspberry PiのIPアドレス（上記メッセージに表示されたもの）を確認します。
2.  Androidアプリを起動し、設定画面で以下の通り入力します。
    - **IP Address:** `192.168.x.x` (確認したIP)
    - **Port:** `5000`
3.  「Save Configuration」をタップして保存します。

## 6. 動作確認

GmailやLINEにテスト通知を送信してみてください。Raspberry Piのターミナルに通知内容が表示されれば成功です！

> [!TIP]
> サーバーをバックグラウンドで動かし続けたい場合は、`nohup python server.py &` を使用するか、システムサービスとして登録することをお勧めします。
