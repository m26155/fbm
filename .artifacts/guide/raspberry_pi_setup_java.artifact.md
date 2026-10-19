# Raspberry Pi サーバー設定ガイド (AI判定 + Android通知連携版)

このガイドでは、Javaを使用して通知を受け取り、AI（Ollama）で詐欺判定を行い、**ラズパイに光や音で警告させる**手順を説明します。

## 1. 準備するもの

- Raspberry Pi (Java JDKインストール済み)
- Ollama (yutayuma-ai モデルが準備済みであること)
- **LED** (1個) & **抵抗** (220Ω程度) ※物理的な警告用
- 警告音声(ここではdanger.mp3という名前にする)
- スピーカー

## 2. ハードウェアの接続

- **LEDのアノード**: GPIO 18 (物理ピン 12番)
- **LEDのカソード**: GND (物理ピン 6番)

## 3. プログラムの作成 (NotificationServer.java)

通信を瞬時に完了させタイムアウトを防ぐため、スマホからの通知を受け取ったラズパイ側は**即座に応答を返し（一瞬で通信終了）**、AI（Ollama）の重い解析処理やLED・音声制御は**バックグラウンドスレッドで非同期実行**します。これにより、タイムアウト時間を変更することなく通信失敗を防ぎます。

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
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        runCommand("pinctrl", String.valueOf(LED_PIN), "op", "dl");

        System.out.println("Java Server started on port " + port);
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

                // 通信を瞬時に完了させるため、即座に応答を返し、AI解析・警告処理は別スレッドで非同期実行
                String response = "{\"status\":\"ok\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }

                // バックグラウンドでAI解析とハードウェア制御を実行
                if (!message.equals("Unknown")) {
                    new Thread(() -> {
                        String aiResponse = runOllama(message);
                        if (checkDanger(aiResponse)) {
                            System.out.println("⚠️ 詐欺を検出！");
                            triggerLed();
                            playAudio("danger.mp3");
                        }
                    }).start();
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
            // AIの回答に "danger" が含まれているかチェック
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

1. ラズパイ上で `NotificationServer.java` をコンパイル・実行します。
2. Androidアプリを起動し、通知権限を許可します。
3. 通知を受信すると、スマホとの通信が一瞬で完了し、ラズパイ側でバックグラウンドにてAI解析が行われ、危険な場合に光や音で警告します。

> [!NOTE]
> 通信自体は一瞬で完了するため、タイムアウト時間を延長する必要はありません。
