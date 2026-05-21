import Foundation
import Speech
import AVFoundation

class SpeechRecognitionService: ObservableObject {
    @Published var isListening = false
    @Published var transcript = ""
    @Published var statusText = "Haz clic en el micrófono para dictar con tu voz"

    private var speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()

    init() {
        speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "es-ES"))
    }

    func startListening() {
        guard !isListening else { return }

        SFSpeechRecognizer.requestAuthorization { [weak self] status in
            guard status == .authorized else {
                DispatchQueue.main.async { self?.statusText = "Permiso de reconocimiento de voz denegado." }
                return
            }
            AVAudioSession.sharedInstance().requestRecordPermission { [weak self] granted in
                DispatchQueue.main.async {
                    guard granted else {
                        self?.statusText = "Permiso de micrófono denegado."
                        return
                    }
                    self?.startRecording()
                }
            }
        }
    }

    func stopListening() {
        audioEngine.stop()
        if audioEngine.inputNode.numberOfInputs > 0 {
            audioEngine.inputNode.removeTap(onBus: 0)
        }
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        recognitionRequest = nil
        recognitionTask = nil
        isListening = false
        statusText = "Transcripción finalizada."
    }

    private func startRecording() {
        recognitionTask?.cancel()
        recognitionTask = nil

        let audioSession = AVAudioSession.sharedInstance()
        do {
            try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            statusText = "Error al configurar el audio."
            return
        }

        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let recognitionRequest else { return }
        recognitionRequest.shouldReportPartialResults = true

        let inputNode = audioEngine.inputNode
        recognitionTask = speechRecognizer?.recognitionTask(with: recognitionRequest) { [weak self] result, error in
            guard let self else { return }
            if let result {
                DispatchQueue.main.async { self.transcript = result.bestTranscription.formattedString }
            }
            if error != nil || result?.isFinal == true {
                DispatchQueue.main.async { self.stopListening() }
            }
        }

        let recordingFormat = inputNode.outputFormat(forBus: 0)
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
            recognitionRequest.append(buffer)
        }

        audioEngine.prepare()
        do {
            try audioEngine.start()
            isListening = true
            statusText = "Escuchando voz en tiempo real..."
        } catch {
            statusText = "Error al iniciar el reconocimiento de voz."
        }
    }
}
