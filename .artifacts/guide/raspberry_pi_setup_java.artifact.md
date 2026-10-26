# Raspberry Pi 5 サーバー設定ガイド (Python版・AI判定 + Android通知連携)

このガイドでは、Pythonを使用して通知を受け取り、AI（Ollama）で詐欺判定を行い、**ラズパイ5に光や音で警告させる**手順を説明します。Pythonサーバーを採用することで、JVM特有のネットワークバインド問題やタイムアウトを完全に回避し、最も安定した通信を実現します。

## 1. 準備するもの

- Raspberry Pi 5 (Python 3インストール済み)
- Ollama (yutayuma-ai モデルが準備済みであること)
- **LED** (1個) & **抵抗** (220Ω程度) ※物理的な警告用
- 警告音声 (`danger.mp3`)
- スピーカー

## 2. ハードウェアの接続

- **LEDのアノード**: GPIO 18 (物理ピン 12番)
- **LEDのカソード**: GND (物理ピン 6番)

## 3. プログラムの作成 (notification_server.py)

以下のコードをラズパイ5上で `notification_server.py` という名前で保存します。スマホからの通知を受け取ったラズパイ側は**即座に `{"status":"ok"}` の応答を返し（一瞬で通信終了）**、AI（Ollama）の解析処理やLED・音声制御はバックグラウンドスレッドで非同期実行します。

```python
import http.server
import json
import subprocess
import threading
import time

PORT = 5000
LED_PIN = 18

def run_command(*args):
    try:
        subprocess.run(args, check=True)
    except Exception as e:
        print(f"Command error: {e}")

def run_ollama(text):
    print(f"\n[AI 解析中...] プロンプト: {text}")
    try:
        result = subprocess.run(
            ["ollama", "run", "yutayuma-ai", text],
            capture_output=True,
            text=True,
            encoding="utf-8"
        )
        return result.stdout
    except Exception as e:
        print(f"Ollama実行エラー: {e}")
        return ""

def trigger_led():
    try:
        subprocess.run(["pinctrl", str(LED_PIN), "op", "dh"])
        time.sleep(5)
        subprocess.run(["pinctrl", str(LED_PIN), "op", "dl"])
    except Exception as e:
        print(f"LED制御エラー: {e}")

def play_audio(filepath):
    try:
        subprocess.run(["mpg123", "-q", filepath])
    except Exception as e:
        print(f"音声再生エラー: {e}")

class NotificationHandler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path == "/notify":
            content_length = int(self.headers.get('Content-Length', 0))
            body = self.rfile.read(content_length).decode('utf-8')

            title = "Unknown"
            text = "Unknown"
            try:
                data = json.loads(body)
                title = data.get("title", "Unknown")
                text = data.get("text", "Unknown")
            except Exception:
                pass

            print(f"\n--- 通知受信 ---\nTitle: {title}\nText: {text}")

            # 即座に応答を返し、通信を瞬時（一瞬）に完了させる
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Connection", "close")
            self.end_headers()
            self.wfile.write(b'{"status":"ok"}')

            # AI解析や警告処理はバックグラウンドで非同期実行
            if text != "Unknown":
                def background_task():
                    ai_response = run_ollama(text)
                    if "danger" in ai_response.lower():
                        print("⚠️ 詐欺を検出！")
                        threading.Thread(target=trigger_led).start()
                        threading.Thread(target=play_audio, args=("danger.mp3",)).start()

                threading.Thread(target=background_tag := background_task).start()
        else:
            self.send_response(404)
            self.end_headers()

if __name__ == "__main__":
    run_command("pinctrl", str(LED_PIN), "op", "dl")
    server = http.server.HTTPServer(("0.0.0.0", PORT), NotificationHandler)
    print(f"Python Notification Server started on port {PORT} (0.0.0.0)")
    server.serve_forever()
```

## 4. 実行手順

1. ラズパイ5上でサーバーを起動します：
   ```bash
   python3 notification_server.py
   ```
2. Androidアプリ側の設定画面でラズパイのIPアドレスを入力し、「Test Send to RPi」を押し、通信が成功することを確認します。
