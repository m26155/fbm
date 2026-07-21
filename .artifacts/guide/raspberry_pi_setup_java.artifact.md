# Raspberry Pi サーバー設定ガイド (AI判定 + LED通知版)

このガイドでは、Javaを使用して通知を受け取り、AI（Ollama）で詐欺判定を行い、**詐欺と判定された場合に物理LEDを光らせる**手順を説明します。

## 1. 準備するもの

- Raspberry Pi (Java JDKインストール済み)
- Ollama (yutayuma-ai モデルが準備済みであること)
- **LED** (1個)
- **抵抗** (220Ω〜330Ω程度、1個)
- ジャンパー線

## 2. ハードウェアの接続

ラズパイの電源を切った状態で、以下のように接続してください。

- **LEDのアノード（長い方の足）**: 抵抗を介して **GPIO 18** (物理ピン 12番) に接続
- **LEDのカソード（短い方の足）**: **GND** (物理ピン 6番など) に接続

## 3. Ollamaの準備

```bash
# Ollamaのインストール
curl -fsSL https://ollama.com/install.sh | sh

# モデルの確認
ollama list
```

## 4. プログラムの作成 (NotificationServer.java)

以下のコードは、AIの回答の中に「詐欺の可能性が高い」という言葉が含まれているかチェックし、含まれている場合にLEDを5秒間点灯させます。

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
    private static final int LED_PIN = 18; // BCM番号

    public static void main(String[] args) throws IOException {
        int port = 5000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/notify", new NotificationHandler());
        server.setExecutor(null);

        // 初期状態としてLEDをオフに設定
        runCommand("pinctrl", String.valueOf(LED_PIN), "op", "dl");

        System.out.println("Java Server started on port " + port);
        System.out.println("LED Alert System ready (GPIO " + LED_PIN + ")");
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

                if (!message.equals("Unknown")) {
                    String aiResponse = runOllama(message);
                    checkAndAlert(aiResponse);
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

        private String extractValue(String json, String key) {
            Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(json);
            return matcher.find() ? matcher.group(1) : "Unknown";
        }

        private String runOllama(String text) {
            System.out.println("\n[AI 解析中...]");
            StringBuilder response = new StringBuilder();
            ProcessBuilder pb = new ProcessBuilder("ollama", "run", "yutayuma-ai", text);
            pb.redirectErrorStream(true);

            try {
                Process process = pb.start();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    System.out.println("--- AIの回答 ---");
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                        response.append(line).append(" ");
                    }
                    System.out.println("----------------");
                }
                process.waitFor();
            } catch (Exception e) {
                System.err.println("Ollama実行エラー: " + e.getMessage());
            }
            return response.toString();
        }

        private void checkAndAlert(String aiResponse) {
            String lowerResponse = aiResponse.toLowerCase();
            // 詐欺を疑うキーワードが含まれているかチェック
            if (lowerResponse.contains("詐欺の可能性が高い")) {

                System.out.println("⚠️ 詐欺を検出！LEDを点灯します。");
                triggerLed();
            } else {
                System.out.println("✅ 安全なメッセージと判断されました。");
            }
        }

        private void triggerLed() {
            new Thread(() -> {
                try {
                    // LEDオン (Drive High)
                    runCommand("pinctrl", String.valueOf(LED_PIN), "op", "dh");
                    Thread.sleep(5000); // 5秒間点灯
                    // LEDオフ (Drive Low)
                    runCommand("pinctrl", String.valueOf(LED_PIN), "op", "dl");
                } catch (Exception e) {
                    System.err.println("LED制御エラー: " + e.getMessage());
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

## 5. 実行手順

1. ラズパイ上で `NotificationServer.java` をコンパイルします。
   ```bash
   javac NotificationServer.java
   ```
2. サーバーを起動します。
   ```bash
   java NotificationServer
   ```

## 6. 動作確認

Androidアプリから詐欺を模したメッセージを送信し、AIが詐欺と判断した場合に**物理的なLEDが5秒間光る**ことを確認してください。
