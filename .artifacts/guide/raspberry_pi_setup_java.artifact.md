# Raspberry Pi サーバー設定ガイド (AI判定 + LED通知版)

このガイドでは、通知を受信し、AI（Ollama）で解析を行い、結果を画面表示するとともに、詐欺の場合にLEDを光らせる**堅牢なプログラム**の構築手順を説明します。

## 1. 準備するもの

- Raspberry Pi (Java JDKインストール済み)
- Ollama (yutayuma-ai モデル準備済み)
- LED + 抵抗 (GPIO 18に接続)

## 2. プログラムの作成 (NotificationServer.java)

この最新版では、データの受信漏れを防ぎ、AIの回答を確実に画面に表示するための改善が含まれています。

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
    private static final int LED_PIN = 18;

    public static void main(String[] args) throws IOException {
        int port = 5000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/notify", new NotificationHandler());
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); // 並列処理を有効化

        runCommand("pinctrl", String.valueOf(LED_PIN), "op", "dl");

        System.out.println("Java Server started on port " + port);
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

                // 1. 受信した生のデータを確認（デバッグ用）
                System.out.println("\n[Raw JSON Received]: " + body);

                String title = extractValue(body, "title");
                String message = extractValue(body, "text");

                System.out.println("Parsed Title: " + title);
                System.out.println("Parsed Text: " + message);

                // レスポンスを先に返してスマホ側の接続を完了させる
                String response = "{\"status\":\"success\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }

                // 2. AI処理を非同期（バックグラウンド）で開始
                if (!message.equals("Unknown") && !message.isEmpty()) {
                    new Thread(() -> {
                        String aiResponse = runOllama(message);
                        checkAndAlert(aiResponse);
                    }).start();
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private String extractValue(String json, String key) {
            // エスケープされた文字を考慮した正規表現
            Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"((?:\\\\\"|[^\"])*)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                String val = matcher.group(1);
                // 簡易的なエスケープ解除
                return val.replace("\\\"", "\"").replace("\\n", "\n");
            }
            return "Unknown";
        }

        private String runOllama(String text) {
            System.out.println("\n[Ollama AI 処理開始]");
            StringBuilder response = new StringBuilder();
            // 引数としてテキストを渡す（非対話モード）
            ProcessBuilder pb = new ProcessBuilder("ollama", "run", "yutayuma-ai", text);
            pb.redirectErrorStream(true); // エラー出力も標準出力に統合

            try {
                Process process = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    System.out.println("--- AIの回答 ---");
                    while ((line = reader.readLine()) != null) {
                        // ANSIエスケープコード（装飾やプログレスバー）を除去
                        String cleanLine = line.replaceAll("\\x1B\\[[0-9;]*[a-zA-Z]", "");
                        if (!cleanLine.trim().isEmpty()) {
                            System.out.println(cleanLine);
                            response.append(cleanLine).append(" ");
                        }
                    }
                    System.out.println("----------------");
                }
                int exitCode = process.waitFor();
                if (exitCode != 0) System.err.println("Ollama実行エラー。終了コード: " + exitCode);
            } catch (Exception e) {
                System.err.println("実行失敗: " + e.getMessage());
            }
            return response.toString();
        }

        private void checkAndAlert(String aiResponse) {
            String lowerResponse = aiResponse.toLowerCase();
            if (lowerResponse.contains("詐欺") || lowerResponse.contains("フィッシング") ||
                lowerResponse.contains("scam") || lowerResponse.contains("phishing")) {

                System.out.println("⚠️ 詐欺検出: LED点灯中...");
                triggerLed();
            } else {
                System.out.println("✅ 安全判定");
            }
        }

        private void triggerLed() {
            try {
                runCommand("pinctrl", String.valueOf(LED_PIN), "op", "dh");
                Thread.sleep(5000);
                runCommand("pinctrl", String.valueOf(LED_PIN), "op", "dl");
            } catch (Exception e) {
                System.err.println("LED制御エラー: " + e.getMessage());
            }
        }
    }

    private static void runCommand(String... args) {
        try {
            new ProcessBuilder(args).start().waitFor();
        } catch (Exception e) {
            System.err.println("コマンドエラー: " + e.getMessage());
        }
    }
}
```

## 3. トラブルシューティング

もしAIの回答が表示されない場合は、以下の点を確認してください。

1.  **手動実行テスト**:
    ラズパイのターミナルで `ollama run yutayuma-ai "テストメッセージ"` を直接入力し、回答が返ってくるか確認してください。
2.  **実行権限**:
    `pinctrl` などのコマンドが権限エラーになる場合は、`sudo java NotificationServer` で実行してください。
3.  **モデル名**:
    コード内の `"yutayuma-ai"` が、実際に作成したモデル名と完全に一致しているか確認してください。
