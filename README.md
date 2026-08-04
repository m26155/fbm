# フィッシング詐欺撲滅隊メール支所
[Releases](https://github.com/m26155/fbm/releases)にAndroid側のアプリを載せています。
## Imformation
通知を読み取ってラズベリーパイに送信、AIで判定し、通知するアプリ。
## User Guide
1. 最初に開いたら通知の読み取り権限を求められるので許可する
2. ラズパイのIPアドレスとポート番号を入力する

たったそれだけです！
## For Developer
### アプリ側(Windows)
1. [Android Studioをダウンロード](https://developer.android.com/studio?hl=ja)
2. 任意の場所でエクスプローラーのアドレスバーにcmdと入力
3. `winget install --id Git.Git -e --source winget`を実行(Gitをインストール)
4. `git clone https://github.com/m26155/fbm.git` を実行(クローン)
5. Android Studioで任意の場所\fbmを開く
6. 実行設定を構成
7. Androidデバイスを接続 
8. Run

ℹ️デバッグapkはこのリポジトリをフォークしてアクションを実行しても作れます

### ラズベリーパイ側
[ラズパイ側説明書](https://github.com/m26155/fbm/blob/master/.artifacts/guide/raspberry_pi_setup_java.artifact.md)
