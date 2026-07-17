# Raspberry Pi サーバー設定ガイド (Java + Ollama AI版)

このガイドでは、Javaを使用して通知を受け取り、受信したメッセージをラズパイ上のAI（Ollama）に渡して自動解析させる手順を説明します。

## 1. 準備するもの

- Raspberry Pi (Java JDKがインストール済みであること)
- Ollama (AI実行環境)
- インターネット接続環境

## 2. Ollamaのインストールとモデルの準備

ラズパイのターミナルで以下のコマンドを実行して、Ollamaとモデルを準備します。

```bash
# Ollamaのインストール
curl -fsSL https://ollama.com/install.sh | sh

# モデル「yutayuma-ai」の準備（すでに作成済みの前提）
# 作成していない場合は、ベースとなるモデルをプルしてください
ollama pull llama3 # 例
```

## 3. 受信 + AI実行プログラムの作成

任意のディレクトリに `NotificationServer.java` という名前でファイルを作成し、以下のコードを保存してください。

```java
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationServer {
    public static void main(String[] args) throws IOException {
        int port = 5000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/notify", new NotificationHandler());
        server.setExecutor(null);
        System.out.println("Java Server started on port " + port);
        System.out.println("Waiting for notifications to process with Ollama...");
        server.start();
    }

    static class NotificationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body;
                try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                    body = scanner.hasNext() ? scanner.useDelimiter("\\A").next() : "";
                }

                // JSONから "text" フィールドの値を簡易的に抽出
                String message = extractValue(body, "text");
                String title = extractValue(body, "title");

                System.out.println("\n--- 通知を受信しました ---");
                System.out.println("Title: " + title);
                System.out.println("Text: " + message);

                if (!message.equals("Unknown")) {
                    runOllama(message);
                }

                String response = "{\"status\":\"success\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        // 簡易的なJSON値抽出メソッド
        private String extractValue(String json, String key) {
            Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return "Unknown";
        }

        // Ollamaを実行して結果を表示するメソッド
        private void runOllama(String text) {
            System.out.println("\n[Ollama AI 処理中...]");
            ProcessBuilder pb = new ProcessBuilder("ollama", "run", "yutayuma-ai", text);
            pb.redirectErrorStream(true);

            try {
                Process process = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    System.out.println("--- AIの回答 ---");
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                    System.out.println("----------------");
                }
                process.waitFor();
            } catch (Exception e) {
                System.err.println("Ollamaの実行に失敗しました: " + e.getMessage());
            }
        }
    }
}
```

## 4. コンパイルと実行

ラズパイのターミナルで実行します。

### コンパイル
```bash
javac NotificationServer.java
```

### 実行
```bash
java NotificationServer
```

## 5. 動作確認

1. Androidアプリ側で、ラズパイのIPアドレスとポート `5000` を設定して保存します。
2. スマホで通知を受け取ると、ラズパイの画面に以下の順で表示されます。
   - 受信した通知の内容
   - `[Ollama AI 処理中...]` というメッセージ
   - **AI（yutayuma-ai）による判定結果や回答**

> [!CAUTION]
> AIの処理には時間がかかる場合があります。ラズパイのスペックによっては、応答までに数十秒から数分かかることがあるため、ターミナルの表示を待ってください。
