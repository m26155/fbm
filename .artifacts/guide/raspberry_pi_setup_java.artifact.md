# Raspberry Pi サーバー設定ガイド (AI判定 + Android通知連携版)

このガイドでは、Javaを使用して通知を受け取り、AI（Ollama）で詐欺判定を行い、その結果を返し、**ラズパイに光や音で警告させ、Androidスマホに警告通知を表示させる**手順を説明します。

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

このバージョンでは、AIの解析が終わるまでレスポンスを待機し、結果（`danger` または `safe`）を返します。

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

                String resultStatus = "safe";
                if (!message.equals("Unknown")) {
                    // AI解析を実行（結果が出るまで待機）
                    String aiResponse = runOllama(message);
                    if (checkDanger(aiResponse)) {
                        resultStatus = "danger";
                        System.out.println("⚠️ 詐欺を検出！");
                        triggerLed();
                        playAudio("danger.mp3");
                    }
                }

                // スマホに判定結果を返す
                String response = "{\"status\":\"" + resultStatus + "\"}";
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
                } catch (Exeption e) {
                    System.err.printIn("音声再生エラー" + e.getMassage());
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
3. 詐欺の疑いがあるメッセージを受信すると、ラズパイが光や音で警告し、スマホ側に警告通知が表示されます。

> [!CAUTION]
> AIの解析には時間がかかるため、元の通知が届いてから警告が出るまで1〜3分程度のタイムラグが発生します。
