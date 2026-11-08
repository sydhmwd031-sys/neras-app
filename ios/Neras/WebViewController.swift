import UIKit
import WebKit
import AVFoundation

/// Hosts the Neras web app (www/index.html) and gives it native speech, clipboard and link handling.
final class WebViewController: UIViewController, WKNavigationDelegate, WKScriptMessageHandler {

    private var webView: WKWebView!
    private let speech = AVSpeechSynthesizer()
    private let cream = UIColor(red: 251/255, green: 248/255, blue: 242/255, alpha: 1)

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = cream

        // Play sound even when the silent switch is on, mixed politely with other audio
        try? AVAudioSession.sharedInstance().setCategory(.playback, options: [.mixWithOthers])
        try? AVAudioSession.sharedInstance().setActive(true)

        let content = WKUserContentController()
        content.add(self, name: "neras")
        // Same bridge name the Android build uses, so the web code works unchanged
        let bridge = """
        window.AndroidTTS = {
          speak: function(t){ window.webkit.messageHandlers.neras.postMessage({a:'speak', t:String(t)}); },
          stop:  function(){  window.webkit.messageHandlers.neras.postMessage({a:'stop'}); },
          copy:  function(t){ window.webkit.messageHandlers.neras.postMessage({a:'copy', t:String(t)}); }
        };
        """
        content.addUserScript(WKUserScript(source: bridge, injectionTime: .atDocumentStart, forMainFrameOnly: true))

        let config = WKWebViewConfiguration()
        config.userContentController = content
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []
        config.websiteDataStore = .default()   // keeps the child's profile, PIN and play log

        webView = WKWebView(frame: view.bounds, configuration: config)
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        webView.navigationDelegate = self
        webView.isOpaque = false
        webView.backgroundColor = cream
        webView.scrollView.backgroundColor = cream
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.scrollView.bounces = false
        view.addSubview(webView)

        if let url = Bundle.main.url(forResource: "index", withExtension: "html", subdirectory: "www") {
            webView.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
        }
    }

    override var preferredStatusBarStyle: UIStatusBarStyle { .darkContent }

    // MARK: JS bridge
    func userContentController(_ uc: WKUserContentController, didReceive message: WKScriptMessage) {
        guard let body = message.body as? [String: Any], let action = body["a"] as? String else { return }
        let text = body["t"] as? String ?? ""
        switch action {
        case "speak":
            speech.stopSpeaking(at: .immediate)
            let u = AVSpeechUtterance(string: text)
            u.voice = AVSpeechSynthesisVoice(language: "ar-SA") ?? AVSpeechSynthesisVoice(language: "ar")
            u.rate = AVSpeechUtteranceDefaultSpeechRate * 0.85
            speech.speak(u)
        case "stop":
            speech.stopSpeaking(at: .immediate)
        case "copy":
            UIPasteboard.general.string = text
        default:
            break
        }
    }

    // MARK: Links: phone, WhatsApp, email, TikTok and the website open outside the app
    func webView(_ webView: WKWebView, decidePolicyFor action: WKNavigationAction,
                 decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        guard let url = action.request.url else { return decisionHandler(.allow) }
        if url.isFileURL || url.scheme == "about" { return decisionHandler(.allow) }
        UIApplication.shared.open(url)
        decisionHandler(.cancel)
    }

    // Opening a link with target="_blank"
    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration,
                 for action: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        if let url = action.request.url { UIApplication.shared.open(url) }
        return nil
    }
}
