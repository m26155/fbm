# Raspberry Pi サーバー設定ガイド (Java版)

このガイドでは、Javaを使用して、Androidアプリからの通知を受け取るためのサーバーをRaspberry Pi上に構築する手順を説明します。

## 1. 準備するもの

- Raspberry Pi (Java JDKがインストール済みであること)
- インターネット接続環境

## 2. Javaの確認

ラズパイのターミナルで以下のコマンドを実行し、Javaがインストールされているか確認してください。

```bash
java -version
```
インストールされていない場合は、`sudo apt update && sudo apt install default-jdk` でインストールできます。

## 3. 受信用プログラムの作成

任意のディレクトリに `NotificationServer.java` という名前でファイルを作成し、以下のコードを貼り付けて保存してください。

```java
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class NotificationServer {
    public static void main(String[] args) throws IOException {
        // Androidアプリの設定と合わせるポート番号（デフォルト: 5000）
        int port = 5000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // /notify エンドポイントを作成
        server.createContext("/notify", new NotificationHandler());
        server.setExecutor(null); // デフォルトのエグゼキュータを使用

        System.out.println("Java Server started on port " + port);
        System.out.println("Waiting for notifications...");
        server.start();
    }

    static class NotificationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // POSTメソッドのみ許可
            if ("POST".equals(exchange.getRequestMethod())) {
                // リクエストボディ（JSON）の読み取り
                InputStream is = exchange.getRequestBody();
                String body;
                try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                    body = scanner.useDelimiter("\\A").next();
                }

                // 受信内容の表示
                System.out.println("\n--- 通知を受信しました ---");
                System.out.println(body);
                System.out.println("--------------------------");

                // レスポンスの送信
                String response = "{\"status\":\"success\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                // POST以外は 405 Method Not Allowed
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }
}
```

## 4. コンパイルと実行

ラズパイのターミナルで以下の手順を実行します。

### コンパイル
```bash
javac NotificationServer.java
```

### 実行
```bash
java NotificationServer
```

起動すると `Java Server started on port 5000` と表示されます。

## 5. Androidアプリの設定

1.  ラズパイのIPアドレスを確認します（`hostname -I` コマンドなどで確認可能）。
2.  Androidアプリの設定画面で、確認したIPアドレスとポート番号（5000）を入力して保存します。

## 6. 動作確認

スマホでGmailやLINEの通知を受け取ると、ラズパイのターミナルに受信したJSONデータが表示されます。

> [!NOTE]
> このコードは標準JDKのみを使用しているため、外部ライブラリ（GsonやJacksonなど）を使わずにJSON全体を文字列として出力します。本格的にデータを解析する場合は、ライブラリの導入を検討してください。
