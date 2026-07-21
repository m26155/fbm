# Raspberry Pi サーバー設定ガイド (究極デバッグ版)

通信エラーが解消しない場合、このプログラムを使用してエラーの正体を特定します。

## 1. 準備 (ラズパイのメモリ確保)

ラズパイでAIを動かす際、メモリが不足するとOSが勝手にプロセスを終了させます。実行前に以下のコマンドでメモリ状態を確認してください。

```bash
# メモリ空き容量の確認
free -h

# Ollamaが動いているか確認
ollama ps
```

## 2. プログラムの作成 (NotificationServer.java)

このコードは、エラーが発生した際にその詳細（スタックトレース）をすべて表示します。

```java
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Scanner;

public class NotificationServer {
    private static final int LED_PIN = 18;
    private static final String OLLAMA_URL = "http://127.0.0.1:11434/api/generate";
    private static final String MODEL_NAME = "yutayuma-ai";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public static void main(String[] args) throws IOException {
        System.out.println("--- サーバー起動プロセス開始 ---");

        // 起動時チェック
        try {
            System.out.println("Ollama APIの状態を確認しています...");
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://127.0.0.1:11434")).GET().build();
            httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("✅ Ollama API への接続は正常です。");
        } catch (Exception e) {
            System.err.println("❌ Ollamaに接続できません！ 'ollama serve' が動いているか確認してください。");
            System.err.println("エラー詳細: " + e.getMessage());
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(5000), 0);
        server.createContext("/notify", new NotificationHandler());
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        // LED初期化（失敗しても続行）
        try { new ProcessBuilder("pinctrl", "18", "op", "dl").start(); } catch (Exception e) {}

        System.out.println("🚀 待機中... (Port: 5000)");
        server.start();
    }

    static class NotificationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            InputStream is = exchange.getRequestBody();
            String body;
            try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                body = scanner.hasNext() ? scanner.useDelimiter("\\A").next() : "";
            }

            System.out.println("\n[通知受信]: " + body);

            // 簡易的な値抽出
            String message = "";
            if (body.contains("\"text\":")) {
                int start = body.indexOf("\"text\":\"") + 8;
                int end = body.indexOf("\"", start);
                if (start > 7 && end > start) message = body.substring(start, end);
            }

            // Androidに即レスポンス
            String response = "{\"status\":\"received\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) { os.write(response.getBytes()); }

            if (!message.isEmpty()) {
                String finalMsg = message;
                new Thread(() -> processAi(finalMsg)).start();
            }
        }

        private void processAi(String prompt) {
            System.out.println("\n[Ollama 解析プロセス開始]");
            // JSONの安全な作成（エスケープ強化）
            String safePrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
            String jsonRequest = "{\"model\":\"" + MODEL_NAME + "\",\"prompt\":\"" + safePrompt + "\",\"stream\":false}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                    .timeout(Duration.ofMinutes(10)) // 10分待機
                    .build();

            try {
                System.out.println("APIリクエスト送信中... (最大10分待ちます)");
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    System.out.println("--- AI解析完了 ---");
                    System.out.println(response.body());
                } else {
                    System.err.println("⚠️ Ollamaがエラーを返しました (HTTP " + response.statusCode() + ")");
                    System.err.println("内容: " + response.body());
                }
            } catch (HttpTimeoutException e) {
                System.err.println("❌ タイムアウトエラー: 10分以内にAIが回答しませんでした。ラズパイの負荷が高すぎます。");
            } catch (Exception e) {
                System.err.println("❌ 致命的な通信エラーが発生しました：");
                e.printStackTrace(); // エラーの全容を表示
            }
        }
    }
}
```

## 3. トラブル解消のヒント

このプログラムを実行してもエラーが出る場合は、ターミナルに表示される **`java.net.ConnectException`** や **`HttpTimeoutException`** などの英語のメッセージを教えてください。

1.  **モデルを軽くする**: `llama3` などが重い場合は `ollama run tinyllama` などの軽量モデルに変更して、`MODEL_NAME` を書き換えてみてください。
2.  **スワップメモリの増設**: ラズパイのメモリが足りない場合は、SDカードを一時的なメモリとして使う「スワップ」を増やすことで安定します。
