# Raspberry Pi サーバー設定ガイド (接続エラー修正版)

通知受信、AI（Ollama）解析、LED通知を行うJavaプログラムの**最終安定版**です。`localhost` の接続エラーを回避し、起動時に自動診断を行う機能を追加しました。

## 1. 準備するもの

- Raspberry Pi (Java JDK 11以上)
- Ollama (yutayuma-ai モデル準備済み)
- LED + 抵抗 (GPIO 18に接続)

## 2. プログラムの作成 (NotificationServer.java)

このバージョンでは、`localhost` ではなく **`127.0.0.1`** を使用し、起動時にOllamaの状態をチェックします。

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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationServer {
    private static final int LED_PIN = 18;
    // localhostではなく明示的にIPv4の127.0.0.1を指定
    private static final String OLLAMA_BASE_URL = "http://127.0.0.1:11434";
    private static final String MODEL_NAME = "yutayuma-ai";

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws IOException {
        // 1. 起動時にOllamaの接続確認を行う
        if (!checkOllamaConnectivity()) {
            System.err.println("\n❌ エラー: Ollama APIに接続できません。");
            System.err.println("以下の点を確認してください：");
            System.err.println("1. 'ollama serve' が実行されているか");
            System.err.println("2. 'curl " + OLLAMA_BASE_URL + "' で応答があるか");
            return; // 接続できない場合は起動しない
        }
        System.out.println("✅ Ollama API への接続を確認しました。");

        int port = 5000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/notify", new NotificationHandler());
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        // LED初期化
        runCommand("pinctrl", String.valueOf(LED_PIN), "op", "dl");

        System.out.println("Java Server started on port " + port);
        System.out.println("Waiting for notifications...");
        server.start();
    }

    private static boolean checkOllamaConnectivity() {
        System.out.println("Ollama API (" + OLLAMA_BASE_URL + ") の状態を確認中...");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_BASE_URL))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("接続テスト失敗: " + e.getMessage());
            return false;
        }
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

                System.out.println("\n[通知受信]: " + body);
                String message = extractValue(body, "text");

                // Android側に即レスポンス
                sendResponse(exchange, "{\"status\":\"success\"}");

                // AI解析を非同期で開始
                if (!message.equals("Unknown") && !message.isEmpty()) {
                    new Thread(() -> {
                        String aiResponse = callOllamaApi(message);
                        checkAndAlert(aiResponse);
                    }).start();
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void sendResponse(HttpExchange exchange, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        private String callOllamaApi(String prompt) {
            System.out.println("\n[Ollama API 解析開始]");
            String jsonRequest = String.format(
                "{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false}",
                MODEL_NAME, prompt.replace("\"", "\\\"").replace("\n", " ")
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_BASE_URL + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(120)) // ラズパイ用に2分まで待機
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String aiText = extractValue(response.body(), "response");
                    System.out.println("--- AIの回答 ---");
                    System.out.println(aiText);
                    System.out.println("----------------");
                    return aiText;
                } else {
                    System.err.println("APIエラー (HTTP " + response.statusCode() + "): " + response.body());
                }
            } catch (Exception e) {
                System.err.println("AI解析通信エラー: " + e.getMessage());
            }
            return "";
        }

        private void checkAndAlert(String aiResponse) {
            if (aiResponse.contains("詐欺") || aiResponse.contains("フィッシング") ||
                aiResponse.contains("scam") || aiResponse.contains("phishing")) {
                System.out.println("⚠️ 詐欺検出！LED点灯開始...");
                triggerLed();
            } else if (!aiResponse.isEmpty()) {
                System.out.println("✅ 安全なメッセージ");
            }
        }

        private void triggerLed() {
            try {
                runCommand("pinctrl", String.valueOf(LED_PIN), "op", "dh");
                Thread.sleep(5000);
                runCommand("pinctrl", String.valueOf(LED_PIN), "op", "dl");
            } catch (Exception e) {
                System.err.println("LED制御失敗: " + e.getMessage());
            }
        }

        private String extractValue(String json, String key) {
            Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"((?:\\\\\"|[^\"])*)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1).replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "");
            }
            return "Unknown";
        }
    }

    private static void runCommand(String... args) {
        try {
            new ProcessBuilder(args).start().waitFor();
        } catch (Exception e) {
            System.err.println("コマンド失敗: " + e.getMessage());
        }
    }
}
```

## 3. 実行手順

1. ラズパイ側で `NotificationServer.java` を上書きします。
2. コンパイルと実行:
   ```bash
   javac NotificationServer.java
   java NotificationServer
   ```

## 4. トラブルシューティング

- **「接続テスト失敗: Connection refused」と出る場合**:
  Ollamaが起動していないか、ポート番号が異なります。`ollama serve` を実行してください。
- **AIの回答が非常に遅い場合**:
  ラズパイ 4 以前のモデルでは解析に時間がかかることがあります。プログラムは最大2分間待ちますが、それ以上かかる場合はより軽量なモデル（`phi3:mini` など）を試してみてください。
