import UIKit

class HapticFeedbackHelper {
    static let shared = HapticFeedbackHelper()
    
    private init() {}
    
    func vibrateTick() {
        let generator = UIImpactFeedbackGenerator(style: .light)
        generator.prepare()
        generator.impactOccurred()
    }
    
    func vibrateSuccess() {
        let generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(.success)
    }
    
    func vibrateDelete() {
        let generator = UIImpactFeedbackGenerator(style: .heavy)
        generator.prepare()
        generator.impactOccurred()
        
        // Secondary impact for double haptic drop
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
            let secondary = UIImpactFeedbackGenerator(style: .medium)
            secondary.impactOccurred()
        }
    }
    
    func vibrateSOS() {
        let sIntervals = [0.0, 0.2, 0.4]
        let oIntervals = [0.7, 1.0, 1.3]
        let s2Intervals = [1.7, 1.9, 2.1]
        
        // Morse code SOS: . . .   --- --- ---   . . .
        // Short pulses
        for interval in sIntervals {
            DispatchQueue.main.asyncAfter(deadline: .now() + interval) {
                let generator = UIImpactFeedbackGenerator(style: .medium)
                generator.impactOccurred()
            }
        }
        
        // Long pulses (represented by heavy impacts)
        for interval in oIntervals {
            DispatchQueue.main.asyncAfter(deadline: .now() + interval) {
                let generator = UIImpactFeedbackGenerator(style: .heavy)
                generator.impactOccurred()
            }
        }
        
        // Short pulses
        for interval in s2Intervals {
            DispatchQueue.main.asyncAfter(deadline: .now() + interval) {
                let generator = UIImpactFeedbackGenerator(style: .medium)
                generator.impactOccurred()
            }
        }
    }
}
