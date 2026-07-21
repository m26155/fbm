# Raspberry Pi サーバー設定ガイド (API連携版)

このガイドでは、通知を受信し、ラズパイ上のOllamaの**HTTP API**を直接叩いてAI解析を行い、詐欺の場合にLEDを光らせる**最も安定したプログラム**の構築手順を説明します。

## 1. 準備するもの

- Raspberry Pi (Java JDK 11以上インストール済み)
- Ollama (yutayuma-ai モデル準備済み)
- LED + 抵抗 (GPIO 18に接続)

## 2. Ollama APIの動作確認

プログラムを動かす前に、ラズパイのターミナルでAPIが反応するか確認してください。

```bash
curl http://localhost:11434/api/tags
```
モデル一覧のJSONが返ってくれば準備完了です。

## 3. プログラムの作成 (NotificationServer.java)

このバージョンでは、コマンド実行（CLI）ではなくAPI経由で通信するため、出力が途切れたり文字化けしたりする問題が解消されています。

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
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME = "yutayuma-ai";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws IOException {
        int port = 5000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/notify", new NotificationHandler());
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        // 初期化
        runCommand("pinctrl", String.valueOf(LED_PIN), "op", "dl");

        System.out.println("Java Server started on port " + port);
        System.out.println("Ollama API Target: " + OLLAMA_URL);
        System.out.println("Waiting for notifications...");
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

                System.out.println("\n[通知受信]: " + body);

                String message = extractValue(body, "text");

                // Android側に即レスポンス
                String response = "{\"status\":\"success\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }

                // AI解析をバックグラウンドで開始
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

        private String callOllamaApi(String prompt) {
            System.out.println("\n[Ollama API リクエスト送信中...]");

            // JSONボディを手動作成 (stream: false で一括受信)
            String jsonRequest = String.format(
                "{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false}",
                MODEL_NAME, prompt.replace("\"", "\\\"")
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(60)) // AIの思考時間に余裕を持たせる
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    // レスポンスから "response" フィールドを抽出
                    String aiText = extractValue(response.body(), "response");
                    System.out.println("--- AIの回答 ---");
                    System.out.println(aiText);
                    System.out.println("----------------");
                    return aiText;
                } else {
                    System.err.println("APIエラー (Code: " + response.statusCode() + "): " + response.body());
                }
            } catch (Exception e) {
                System.err.println("通信エラー: " + e.getMessage());
            }
            return "";
        }

        private void checkAndAlert(String aiResponse) {
            if (aiResponse.contains("詐欺") || aiResponse.contains("フィッシング") ||
                aiResponse.contains("scam") || aiResponse.contains("phishing")) {
                System.out.println("⚠️ 詐欺検出！LED点灯");
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

## 4. トラブルシューティング

1.  **AIの回答が空の場合**:
    ラズパイで `curl -X POST http://localhost:11434/api/generate -d '{"model":"yutayuma-ai","prompt":"hello","stream":false}'` を実行し、JSONが返るか確認してください。
2.  **Javaのバージョン**:
    このコードは Java 11 以降の `HttpClient` を使用しています。`java -version` で 11 以上であることを確認してください。
3.  **モデル名**:
    プログラム内の `MODEL_NAME` が正しいか確認してください。
