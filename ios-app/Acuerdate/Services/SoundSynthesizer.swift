import Foundation
import AVFoundation

enum AlertTone: String, CaseIterable, Identifiable {
    case chime = "chime"
    case celestial = "celestial"
    case sos = "sos"
    case bell = "bell"
    case pulse = "pulse"
    
    var id: String { self.rawValue }
    
    var displayName: String {
        switch self {
        case .chime: return "Chime Clásico 🔔"
        case .celestial: return "Celestial Armónico ✨"
        case .sos: return "Alarma SOS 🚨"
        case .bell: return "Campana Tradicional 🔔"
        case .pulse: return "Pulso Cyberpunk ⚡"
        }
    }
    
    var systemIcon: String {
        switch self {
        case .chime: return "bell.fill"
        case .celestial: return "sparkles"
        case .sos: return "exclamationmark.triangle.fill"
        case .bell: return "alarm.fill"
        case .pulse: return "bolt.horizontal.fill"
        }
    }
}

class SoundSynthesizer {
    static let shared = SoundSynthesizer()
    
    private let sampleRate: Double = 44100.0
    private var audioEngine: AVAudioEngine?
    private var playerNode: AVAudioPlayerNode?
    
    private init() {
        setupAudioEngine()
    }
    
    private func setupAudioEngine() {
        let engine = AVAudioEngine()
        let player = AVAudioPlayerNode()
        engine.attach(player)
        
        let format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 1)!
        engine.connect(player, to: engine.mainMixerNode, format: format)
        
        do {
            try engine.start()
            self.audioEngine = engine
            self.playerNode = player
        } catch {
            print("Failed to start AVAudioEngine: \(error)")
        }
    }
    
    private func playSamples(_ samples: [Float]) {
        guard let playerNode = playerNode, let audioEngine = audioEngine else { return }
        
        // Ensure engine is running
        if !audioEngine.isRunning {
            do {
                try audioEngine.start()
            } catch {
                print("Failed to restart AVAudioEngine: \(error)")
                return
            }
        }
        
        let format = AVAudioFormat(standardFormatWithSampleRate: sampleRate, channels: 1)!
        guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(samples.count)) else { return }
        buffer.frameLength = AVAudioFrameCount(samples.count)
        
        if let channels = buffer.floatChannelData {
            let channelData = channels[0]
            for i in 0..<samples.count {
                channelData[i] = samples[i]
            }
        }
        
        playerNode.stop()
        playerNode.scheduleBuffer(buffer, at: nil, options: [], completionHandler: nil)
        playerNode.play()
    }
    
    func playChime() {
        let durationMs = 300
        let numSamples = Int(Double(durationMs) * sampleRate / 1000.0)
        var samples = [Float](repeating: 0.0, count: numSamples)
        
        for i in 0..<numSamples {
            let t = Double(i) / sampleRate
            let progress = Double(i) / Double(numSamples)
            let freq = 880.0 + (1320.0 - 880.0) * progress
            let angle = 2.0 * .pi * freq * t
            let envelope = 1.0 - progress
            samples[i] = Float(sin(angle) * 0.4 * envelope)
        }
        
        playSamples(samples)
    }
    
    func playCelestialChord() {
        let durationMs = 1500
        let numSamples = Int(Double(durationMs) * sampleRate / 1000.0)
        var samples = [Float](repeating: 0.0, count: numSamples)
        
        let eMaj7 = [329.63, 415.30, 493.88, 587.33]  // E4, G#4, B4, D#5
        let aMaj7 = [440.00, 554.37, 659.25, 830.61]  // A4, C#5, E5, G#5
        let bMaj7 = [493.88, 587.33, 698.46, 932.33]  // B4, D#5, F#5, A#5
        
        for i in 0..<numSamples {
            let t = Double(i) / sampleRate
            let progress = Double(i) / Double(numSamples)
            
            let chordFreqs: [Double]
            if progress < 0.25 {
                chordFreqs = eMaj7
            } else if progress < 0.55 {
                chordFreqs = aMaj7
            } else {
                chordFreqs = bMaj7
            }
            
            var mix = 0.0
            let vibrato = 1.0 + 0.015 * sin(2.0 * .pi * 6.0 * t)
            
            for freq in chordFreqs {
                mix += sin(2.0 * .pi * freq * vibrato * t)
            }
            mix /= Double(chordFreqs.count)
            
            let envelope: Double
            if progress < 0.05 {
                envelope = progress / 0.05
            } else if progress > 0.7 {
                envelope = 1.0 - (progress - 0.7) / 0.3
            } else {
                envelope = 1.0
            }
            
            samples[i] = Float(mix * 0.35 * envelope)
        }
        
        playSamples(samples)
    }
    
    func playSOSAlarm() {
        let durationMs = 2400
        let numSamples = Int(Double(durationMs) * sampleRate / 1000.0)
        var samples = [Float](repeating: 0.0, count: numSamples)
        
        let dotMs = 100
        let dashMs = 300
        let spaceMs = 100
        
        let segments: [(active: Bool, durationMs: Int)] = [
            (true, dotMs), (false, spaceMs), (true, dotMs), (false, spaceMs), (true, dotMs), (false, spaceMs),
            (true, dashMs), (false, spaceMs), (true, dashMs), (false, spaceMs), (true, dashMs), (false, spaceMs),
            (true, dotMs), (false, spaceMs), (true, dotMs), (false, spaceMs), (true, dotMs)
        ]
        
        var sampleIdx = 0
        for seg in segments {
            let segSamples = Int(Double(seg.durationMs) * sampleRate / 1000.0)
            
            for _ in 0..<segSamples {
                if sampleIdx >= numSamples { break }
                let t = Double(sampleIdx) / sampleRate
                
                if seg.active {
                    let vibratoFreq = 950.0 + 50.0 * sin(2.0 * .pi * 12.0 * t)
                    let angle = 2.0 * .pi * vibratoFreq * t
                    samples[sampleIdx] = Float(sin(angle) * 0.4)
                } else {
                    samples[sampleIdx] = 0.0
                }
                sampleIdx += 1
            }
        }
        
        playSamples(samples)
    }
    
    func playBell() {
        let durationMs = 1500
        let numSamples = Int(Double(durationMs) * sampleRate / 1000.0)
        var samples = [Float](repeating: 0.0, count: numSamples)
        
        // Classic bell frequency components with inharmonic structure
        let freqs = [350.0, 437.5, 525.0, 700.0, 875.0]
        let weights = [0.4, 0.25, 0.15, 0.1, 0.1]
        
        for i in 0..<numSamples {
            let t = Double(i) / sampleRate
            let progress = Double(i) / Double(numSamples)
            
            var mix = 0.0
            for j in 0..<freqs.count {
                // Higher frequencies decay faster to replicate physical bell decay
                let freqDecay = exp(-progress * (3.0 + Double(j) * 2.0))
                mix += sin(2.0 * .pi * freqs[j] * t) * weights[j] * freqDecay
            }
            
            samples[i] = Float(mix * 0.5)
        }
        
        playSamples(samples)
    }
    
    func playCyberPulse() {
        let durationMs = 450
        let numSamples = Int(Double(durationMs) * sampleRate / 1000.0)
        var samples = [Float](repeating: 0.0, count: numSamples)
        
        for i in 0..<numSamples {
            let t = Double(i) / sampleRate
            let progress = Double(i) / Double(numSamples)
            
            // Retro cyberpunk frequency sweep down then rapid sweep up
            let phase = progress < 0.5 ? progress * 2.0 : (1.0 - progress) * 2.0
            let freq = 600.0 + 800.0 * sin(phase * .pi / 2.0)
            
            let angle = 2.0 * .pi * freq * t
            let sineVal = sin(angle)
            let triVal = abs((angle.truncatingRemainder(dividingBy: 2.0 * .pi) / .pi) - 1.0) * 2.0 - 1.0
            let waveMix = 0.6 * sineVal + 0.4 * triVal
            
            // Rapid double pulse envelope
            let pulseEnv = sin(progress * .pi * 2.0)
            let envelope = abs(pulseEnv) * (1.0 - progress)
            
            samples[i] = Float(waveMix * 0.3 * envelope)
        }
        
        playSamples(samples)
    }
    
    func playTone(_ tone: AlertTone) {
        switch tone {
        case .chime:
            playChime()
        case .celestial:
            playCelestialChord()
        case .sos:
            playSOSAlarm()
        case .bell:
            playBell()
        case .pulse:
            playCyberPulse()
        }
    }
    
    func playSelectedTone() {
        let rawTone = UserDefaults.standard.string(forKey: "selectedAlertTone") ?? "chime"
        let tone = AlertTone(rawValue: rawTone) ?? .chime
        playTone(tone)
    }
}
