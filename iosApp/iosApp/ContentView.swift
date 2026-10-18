import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        #if DEBUG
        let debugBuild = true
        #else
        let debugBuild = false
        #endif
        #if INDOOR_EDITOR
        let editorRequested = true
        #else
        let editorRequested = false
        #endif
        return MainViewControllerKt.MainViewController(debugBuild: debugBuild, indoorEditorRequested: editorRequested)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}