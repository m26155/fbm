# Raspberry Pi 5 サーバー設定ガイド (AI判定 + Android通知連携版)

このガイドでは、Javaを使用して通知を受け取り、AI（Ollama）で詐欺判定を行い、**ラズパイ5に光や音で警告させる**手順を説明します。

## 1. 準備するもの

- Raspberry Pi 5 (Java JDKインストール済み)
- Ollama (yutayuma-ai モデルが準備済みであること)
- **LED** (1個) & **抵抗** (220Ω程度) ※物理的な警告用
- 警告音声(ここではdanger.mp3という名前にする)
- スピーカー

## 2. ハードウェアの接続

- **LEDのアノード**: GPIO 18 (物理ピン 12番)
- **LEDのカソード**: GND (物理ピン 6番)

## 3. プログラムの作成 (NotificationServer.java)

コネクションプールの再利用による接続エラー（`Failed to connect`）を防ぐため、HTTPレスポンスに `Connection: close` ヘッダーを付与し、毎回フレッシュなTCPコネクションを確立するように設計しています。また、AI解析（Ollama）はシングルスレッドのキュー（順次実行）で処理し、ラズパイ5の過負荷を防ぎます。

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationServer {
    private static final int LED_PIN = 18;
    // Ollamaの同時実行によるラズパイ5の過負荷を防ぐため、AI処理専用の単一スレッドキュー
    private static final ExecutorService aiExecutor = Executors.newSingleThreadExecutor();

    public static void main(String[] args) throws IOException {
        int port = 5000;
        // 0.0.0.0を指定してIPv4で全インターフェースに明示的にバインド
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/notify", new NotificationHandler());
        server.setExecutor(Executors.newCachedThreadPool());

        runCommand("pinctrl", String.valueOf(LED_PIN), "op", "dl");

        System.out.println("Java Server started on port " + port + " (0.0.0.0)");
        System.out.println("Waiting for notifications and analyzing with AI...");
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

                String title = extractValue(body, "title");
                String message = extractValue(body, "text");

                System.out.println("\n--- 通知受信 ---");
                System.out.println("Title: " + title);
                System.out.println("Text: " + message);

                // 即座に応答を返し、Connection: closeを設定して接続の再利用バグを防ぐ
                String response = "{\"status\":\"ok\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Connection", "close");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                exchange.close();

                // AI解析はキュー（aiExecutor）に入れて1件ずつ順番に処理
                if (!message.equals("Unknown")) {
                    aiExecutor.submit(() -> {
                        String aiResponse = runOllama(message);
                        if (checkDanger(aiResponse)) {
                            System.out.println("⚠️ 詐欺を検出！");
                            triggerLed();
                            playAudio("danger.mp3");
                        }
                    });
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        }

        private String extractValue(String json, String key) {
            Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(json);
            return matcher.find() ? matcher.group(1) : "Unknown";
        }

        private String runOllama(String text) {
            System.out.println("\n[AI 解析中...] プロンプト: " + text);
            StringBuilder response = new StringBuilder();
            ProcessBuilder pb = new ProcessBuilder("ollama", "run", "yutayuma-ai", text);
            pb.redirectErrorStream(true);

            try {
                Process process = pb.start();
                process.getOutputStream().close();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                        response.append(line).append(" ");
                    }
                }
                process.waitFor();
            } catch (Exception e) {
                System.err.println("Ollama実行エラー: " + e.getMessage());
            }
            return response.toString();
        }

        private boolean checkDanger(String aiResponse) {
            return aiResponse.replaceAll("[^a-zA-Z]", "").toLowerCase().contains("danger");
        }

        private void triggerLed() {
            new Thread(() -> {
                try {
                    runCommand("pinctrl", String.valueOf(LED_PIN), "op", "dh");
                    Thread.sleep(5000);
                    runCommand("pinctrl", String.valueOf(LED_PIN), "op", "dl");
                } catch (Exception e) {
                    System.err.println("LED制御エラー: " + e.getMessage());
                }
            }).start();
        }

        private void playAudio(String filepath) {
            new Thread(() -> {
                try {
                    runCommand("mpg123", "-q", filepath);
                } catch (Exception e) {
                    System.err.println("音声再生エラー: " + e.getMessage());
                }
            }).start();
        }
    }

    private static void runCommand(String... args) {
        try {
            new ProcessBuilder(args).start().waitFor();
        } catch (Exception e) {
            System.err.println("コマンド実行エラー: " + e.getMessage());
        }
    }
}
```

## 4. 実行手順

1. ラズパイ5上で `NotificationServer.java` を更新・再コンパイルし実行します。
   ```bash
   javac NotificationServer.java
   java NotificationServer
   ```
2. Androidアプリ側の設定画面で「Test Send to RPi」を押し、通信が成功するか確認します。
