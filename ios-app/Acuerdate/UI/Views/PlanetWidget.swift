import SwiftUI

struct PlanetWidget: View {
    let state: PlanetState
    
    @State private var isFloating = false
    @State private var rotationAngle: Double = 0.0
    @State private var dangerPulse: Double = 0.3
    
    var body: some View {
        VStack(spacing: 4) {
            // Main Planet visual container
            ZStack {
                let radius: CGFloat = 26.0 // corresponding to radius * 0.28f on 90dp canvas
                
                // Color configuration
                let config = colors(for: state)
                
                // 1. Glowing background aura
                if config.glowActive {
                    Circle()
                        .fill(RadialGradient(
                            colors: [config.glowColor, .clear],
                            center: .center,
                            startRadius: 0,
                            endRadius: radius * 1.7
                        ))
                        .frame(width: radius * 3.4, height: radius * 3.4)
                }
                
                // 2. Outer atmospheric ring (halo outline)
                if state != .inerte {
                    Circle()
                        .stroke(config.mainColor.opacity(0.3), lineWidth: 2)
                        .frame(width: radius * 2.24, height: radius * 2.24)
                }
                
                // 3. Tilted orbital rings with perspective
                Circle()
                    .stroke(config.ringColor, lineWidth: 3)
                    .scaleEffect(x: 1.6, y: 0.32)
                    .frame(width: radius * 2.5, height: radius * 2.5)
                    .rotationEffect(.degrees(-18))
                    .rotationEffect(.degrees(rotationAngle))
                
                // 4. Shaded 3D sphere
                Circle()
                    .fill(RadialGradient(
                        colors: config.planetColors,
                        center: .init(x: 0.35, y: 0.35),
                        startRadius: 0,
                        endRadius: radius * 2.0
                    ))
                    .frame(width: radius * 2.0, height: radius * 2.0)
                    .shadow(color: config.glowColor.opacity(0.3), radius: 6, x: 0, y: 4)
                
                // 5. Glossy highlight overlay
                Circle()
                    .fill(RadialGradient(
                        colors: [Color.white.opacity(0.3), .clear],
                        center: .init(x: 0.25, y: 0.25),
                        startRadius: 0,
                        endRadius: radius * 1.1
                    ))
                    .frame(width: radius * 2.0, height: radius * 2.0)
            }
            .frame(width: 90, height: 90)
            .offset(y: isFloating ? -6 : 6)
            
            // Planet name/icon status
            Text(statusName(for: state))
                .font(.system(size: 11, weight: .medium, design: .rounded))
                .foregroundColor(statusColor(for: state))
        }
        .onAppear {
            // Float animation
            withAnimation(.easeInOut(duration: 2.2).repeatForever(autoreverses: true)) {
                isFloating = true
            }
            // Ring rotation
            withAnimation(.linear(duration: 12.0).repeatForever(autoreverses: false)) {
                rotationAngle = 360.0
            }
            // Danger alert pulsing
            if state == .peligro {
                withAnimation(.easeInOut(duration: 0.35).repeatForever(autoreverses: true)) {
                    dangerPulse = 1.0
                }
            }
        }
    }
    
    // Config struct for colors
    private struct ColorConfig {
        let planetColors: [Color]
        let ringColor: Color
        let glowColor: Color
        let glowActive: Bool
        let mainColor: Color
    }
    
    private func colors(for state: PlanetState) -> ColorConfig {
        switch state {
        case .radiante:
            return ColorConfig(
                planetColors: [Color(hex: "FBBF24"), Color(hex: "F59E0B"), Color(hex: "DC2626")],
                ringColor: Color(hex: "FBBF24").opacity(0.7),
                glowColor: Color(hex: "FBBF24").opacity(0.35),
                glowActive: true,
                mainColor: Color(hex: "FBBF24")
            )
        case .estable:
            return ColorConfig(
                planetColors: [Color(hex: "34D399"), Color(hex: "10B981"), Color(hex: "047857")],
                ringColor: Color(hex: "00F0FF").opacity(0.7),
                glowColor: Color(hex: "10B981").opacity(0.35),
                glowActive: true,
                mainColor: Color(hex: "34D399")
            )
        case .inerte:
            return ColorConfig(
                planetColors: [Color(hex: "9CA3AF"), Color(hex: "4B5563"), Color(hex: "1F2937")],
                ringColor: Color(hex: "9CA3AF").opacity(0.2),
                glowColor: .clear,
                glowActive: false,
                mainColor: Color(hex: "9CA3AF")
            )
        case .peligro:
            return ColorConfig(
                planetColors: [Color(hex: "EF4444"), Color(hex: "DC2626"), Color(hex: "7F1D1D")],
                ringColor: Color(hex: "EF4444").opacity(0.8 * dangerPulse),
                glowColor: Color(hex: "EF4444").opacity(0.6 * dangerPulse),
                glowActive: true,
                mainColor: Color(hex: "EF4444")
            )
        }
    }
    
    private func statusName(for state: PlanetState) -> String {
        switch state {
        case .radiante: return "Radiante 🌞"
        case .estable: return "Estable 🌿"
        case .inerte: return "Inerte 🌑"
        case .peligro: return "¡Peligro! 🌋"
        }
    }
    
    private func statusColor(for state: PlanetState) -> Color {
        switch state {
        case .radiante: return Color(hex: "FBBF24")
        case .estable: return Color(hex: "34D399")
        case .inerte: return Color(hex: "9CA3AF")
        case .peligro: return Color(hex: "EF4444")
        }
    }
}

// Color Hex initializer helper
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8 * 17) & 0xff, (int >> 4 & 0xff) * 17, (int & 0xf) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, (int >> 8) & 0xff, int & 0xff)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, (int >> 16) & 0xff, (int >> 8) & 0xff, int & 0xff)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255.0,
            green: Double(g) / 255.0,
            blue: Double(b) / 255.0,
            opacity: Double(a) / 255.0
        )
    }
}
