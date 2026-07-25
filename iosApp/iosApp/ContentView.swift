import SwiftUI
import shared
import LocalAuthentication

// MARK: - Color Palette matching Android Material3 Theme
extension Color {
    static let bgLight = Color(red: 0.953, green: 0.957, blue: 0.976)        // #F3F4F9
    static let cardSurface = Color(red: 0.992, green: 0.984, blue: 1.0)       // #FDFBFF
    static let cardBorder = Color(red: 0.769, green: 0.776, blue: 0.816)        // #C4C6D0
    static let textPrimary = Color(red: 0.102, green: 0.110, blue: 0.118)       // #1A1C1E
    static let textSecondary = Color(red: 0.267, green: 0.278, blue: 0.306)     // #44474E
    static let textMuted = Color(red: 0.455, green: 0.467, blue: 0.498)         // #74777F
    static let accentContainer = Color(red: 0.839, green: 0.890, blue: 1.0)     // #D6E3FF
    static let accentOnContainer = Color(red: 0.0, green: 0.106, blue: 0.243)    // #001B3E
    static let greenCredit = Color(red: 0.067, green: 0.427, blue: 0.204)       // #116D34
    static let redDebit = Color(red: 0.730, green: 0.102, blue: 0.102)          // #BA1A1A
}

// MARK: - Investec Zebra Head SVG Path Parser & Shape
struct ZebraHeadShape: Shape {
    static let pathData = "M95.094 96.041c-1.137-.496-2.296-1.16-3.47-2.014-5.813-4.229-22.447.23-24.061-13.125-11.974 6.156-4.837 38.453-24.406 52.727-13.586-5.523-29.472-20.722-33.616-23.021 8.521-18.422 39.256-81.737 70.456-66.772 3.579-2.274 6.704-3.799 7.599-3.225.699.45.203 8.397.203 10.471 3.768 5.802 13.809 14.65 20.294 23.07-4.477 6.806-7.85 16.432-12.999 21.889z M49.109 127.799c1.274-1.654 2.37-3.416 3.32-5.252-4.623-2.994-9.816-5.199-14.556-8.094-4.438-2.713-7.302-7.479-10.278-11.586-3.295-4.545-5.458-9.813-6.284-15.352-.792 1.397-1.558 2.779-2.292 4.135 1.987 7.355 6.151 13.568 10.998 19.729.414.527.853 1.023 1.304 1.504-3.936-1.354-7.868-2.664-11.765-4.227-3.231-1.293-4.839-3.344-6.216-6.021-.604 1.23-1.17 2.396-1.691 3.488 1.687 2.635 3.833 4.205 7.073 5.559 10.202 4.254 24.052 6.298 30.387 16.117zm21.552-77.13c1.092-.521.957-1.689.289-2.367-.182-.646-.698-1.212-1.471-1.207-1.801.015-7.545.541-9.979-2.984-.998.459-1.987.975-2.975 1.541.276.261.545.539.832.783-.418-.164-.817-.364-1.226-.555-1.025.606-2.047 1.266-3.058 1.979 3.322 1.944 7.247 3.018 11.044 3.155.257.008 4.796.492 6.544-.345zm22.197 13.942c-1.259-3.965-4.178-8.576-8.262-10.097-3.367-1.254-6.62-1.192-10.182-1.053-4.417.175-9.07 1.735-13.4.612-.829-.215-1.451.203-1.75.794-1.009.51-1.165 2.438.264 3.013 13.167 5.287 17.488 19.76 19.134 33.135 1.057.205 2.128.369 3.188.518-1.545-13.016-5.43-26.703-16.313-33.946 3.514-.133 7.07-.918 10.554-1.057 7.55-.299 11.937 3.203 14.1.168 2.777 8.771.6 17.456-1.379 26.063 1.067.313 2.016.717 2.771 1.268.032.021.063.041.095.064 2.115-9.847 4.369-19.272 1.136-29.482zm6.604 8.94c-1.227 1.574.979 3.813 2.217 2.217 1.582-2.039 2.074-4.412 2.02-6.865-1.176-1.317-2.387-2.627-3.602-3.918.724 3.035 1.095 6.337-.635 8.566z M64.382 65.818c-4.576-3.049-10.624-4.295-15.731-6.198-1.985-.74-3.729-1.858-5.271-3.224-.786.823-1.563 1.668-2.333 2.538 2.743 2.231 6.114 3.506 9.486 4.69 6.229 2.188 13.191 3.834 16.986 9.694 2.639 4.07 3.055 9.115 3.025 14.1.865.844 1.876 1.502 2.979 2.027-.056-9.017-.85-18.103-9.141-23.627zm-13.521-16.3c-.822.648-1.641 1.327-2.45 2.035 1.338 1.142 2.778 2.168 4.343 3.034 1.771.98 3.353-1.727 1.582-2.707-1.237-.683-2.392-1.48-3.475-2.362zm28.241 19.531c3.906 7.344 5.192 14.877 5.47 22.861 1.106.16 2.17.336 3.155.563-.247-8.771-1.641-16.965-5.921-25.006-.95-1.785-3.655-.201-2.704 1.582zM51.893 83.143c-1.073-1.707-3.786-.135-2.706 1.582 2.045 3.254 5.115 5.604 7.254 8.783 1.06 1.578 1.982 3.226 2.927 4.857.307-1.535.618-3.016.951-4.438-.604-1.045-1.224-2.08-1.896-3.08-1.892-2.814-4.714-4.818-6.53-7.704zM38.569 99.209c-.942-1.789-3.648-.205-2.706 1.582 2.667 5.064 4.312 10.471 8.688 14.395 2.6 2.328 5.602 4.123 8.536 6.023.486-1.037.944-2.09 1.356-3.162-1.803-1.229-3.645-2.395-5.448-3.524-5.82-3.648-7.414-9.593-10.426-15.314z M53.085 68.717c-1.49-1.357-3.713.854-2.218 2.217 4.63 4.228 10.021 7.945 12.893 13.623.618-.976 1.326-1.813 2.138-2.504-3.171-5.365-8.147-9.076-12.813-13.336zm-2.499 38.853c-3.034-4.355-4.28-9.705-8.604-13.119-1.57-1.238-3.804.965-2.217 2.217 4.779 3.773 6.38 10.387 10.063 15.113 1.471 1.885 3.23 3.447 5.179 4.734.382-1.086.728-2.188 1.048-3.289-2.046-1.517-3.876-3.372-5.469-5.656zM26.572 88.938c1.513 5.199 2.7 10.533 5.239 15.381.938 1.793 3.645.209 2.706-1.58-3.354-6.406-3.41-14.561-7.672-20.361-.453-.613-.961-1.187-1.494-1.73-.595.974-1.175 1.938-1.745 2.904 1.325 1.503 2.357 3.28 2.966 5.386zm8.499-22.68c-.719.95-1.425 1.914-2.123 2.887.99.984 2.012 1.943 3.104 2.854 2.447 3.743 3.836 8.755 5.656 12.513 4.376 9.041 8.648 16.971 15.646 23.697.28-1.229.541-2.449.79-3.662-6.849-7.195-11.04-15.822-15.408-25.138-.631-1.345-1.181-2.634-1.726-3.878.058.031.107.068.165.102 4.024 2.365 8.766 2.963 12.794 5.482 3.881 2.426 5.67 5.909 7.153 9.715.468-1.609.997-3.092 1.622-4.416-.588-1.223-1.26-2.404-2.104-3.508-2.294-2.995-6.031-5.541-9.521-6.9-4.97-1.938-9.163-3.586-13.272-6.52-.764-1.117-1.67-2.189-2.776-3.228zm9.334-1.705c-1.927-.943-3.672-2.099-5.265-3.409-.672.803-1.338 1.613-1.996 2.439 1.723 1.418 3.608 2.66 5.679 3.677 1.81.886 3.399-1.817 1.582-2.707zm-7.46 26.873c1.623 1.211 3.184-1.512 1.582-2.705-4.396-3.28-6.578-7.94-9.409-12.488-.158-.256-.337-.5-.5-.754-.637.977-1.262 1.951-1.876 2.931.706 1.108 1.396 2.231 2.063 3.377 2.259 3.852 4.536 6.949 8.14 9.639z M26.696 84.357c-4.951-2.646-13.815-6.102-10.478-13.584s23.119-27.215 29.24-32.234c9.757-8.002 27.154-10.495 39.145-4.145 14.563 7.714 21.871 21.067 7.48 27.056-17.611-.808-65.387 22.907-65.387 22.907z M51.185 46.619c3.652 2.743 8.396 4.229 12.939 4.395.249.01.465-.029.652-.106 1.881.628 4.135.6 5.883-.237 1.093-.521.959-1.69.291-2.367-.182-.646-.699-1.212-1.473-1.207-1.801.015-3.822.287-5.479-.568-.363-.188-.719-.223-1.038-.155-3.505-1.459-5.913-4.786-8.028-7.791-.963-1.365-1.913-2.738-2.847-4.122-1.031.478-2.024.997-2.978 1.56 2.308 3.773 4.989 7.642 8.252 10.418-1.356-.536-2.663-1.202-3.886-1.988-2.559-1.646-3.971-4.954-5.141-7.943-.91.573-1.786 1.179-2.602 1.829 1.287 3.126 2.797 6.288 5.455 8.282zm1.9 22.098c-1.49-1.357-3.713.854-2.218 2.217.62.565 1.256 1.125 1.895 1.68 1.064-.438 2.145-.877 3.229-1.313-.969-.853-1.945-1.705-2.906-2.584zM34.687 48.931c1.6 2.604 2.412 5.814 4.425 8.155 2.979 3.468 7.207 5.06 11.42 6.539 4.141 1.454 8.598 2.678 12.24 5.047 1.206-.449 2.415-.893 3.616-1.318-.615-.535-1.279-1.051-2.007-1.533-4.576-3.051-10.624-4.297-15.731-6.198-5.725-2.132-9.472-7.332-12.032-12.867-.66.655-1.341 1.333-2.023 2.023.03.049.058.1.092.152zm-7.991 35.426s.387-.191 1.09-.534c-.292-.494-.598-.979-.939-1.446-2.686-3.65-6.963-5.988-11.276-7.501.313 1.927 1.547 3.503 3.172 4.835 1.061.592 2.061 1.262 2.979 2.019 1.724.999 3.517 1.848 4.974 2.627zM42.823 67.26c1.81.887 3.397-1.814 1.582-2.707-5.666-2.779-9.765-7.33-12.966-12.546-.744.777-1.489 1.563-2.229 2.351 3.405 5.364 7.771 10.036 13.613 12.902zm9.929-12.672c1.771.98 3.353-1.727 1.582-2.707-4.732-2.623-8.328-6.835-10.953-11.545-.708.634-1.493 1.358-2.337 2.147 2.856 4.936 6.669 9.313 11.708 12.105zm25.352-14.547c.209 1.989 3.348 2.009 3.135 0-.295-2.761-1.459-5.25-2.23-7.861-1.121-.32-2.262-.587-3.424-.793.637 2.945 2.195 5.637 2.519 8.654z M98.36 46.698c-.488-.913-1.088-1.825-1.785-2.729-4.392 5.063-13.868 7.08-19.039 2.201-2.779-2.625-3.44-6.696-6.68-9.086-2.638-1.94-5.222-3.802-7.699-5.846-1.317.188-2.627.427-3.907.737 3.523 3.029 7.4 5.487 10.854 8.606 3.813 3.449 4.047 8.199 9.141 10.381 6.102 2.617 14.553.571 19.115-4.264zM28.806 81.783c.241.409.482.807.724 1.198.806-.388 1.763-.843 2.854-1.355-1.104-1.755-2.135-3.581-3.265-5.396-2.538-4.071-5.521-7.826-8.82-11.303-.052-.055-.104-.093-.154-.136-.694.896-1.329 1.748-1.883 2.548 4.131 4.393 7.501 9.247 10.544 14.444zm7.246-9.784c1.267 1.938 2.247 4.212 3.153 6.479.835-.377 1.695-.766 2.586-1.158-.266-.605-.521-1.202-.778-1.787.057.031.106.068.164.102.673.396 1.365.729 2.07 1.041 1.465-.646 2.986-1.309 4.561-1.979-3.578-1.453-6.795-2.949-9.957-5.207-.877-1.277-1.924-2.503-3.26-3.682-2.241-1.97-3.383-4.6-5.23-6.856-.693-.849-1.446-1.614-2.234-2.335-.926 1.015-1.829 2.021-2.695 3.016 3.833 4.364 7.233 8.702 11.62 12.366zm29.484-14.412c3.514-.133 7.07-.918 10.554-1.057 5.417-.214 9.198 1.539 11.724 5.113 1.344-.154 2.59-.229 3.727-.209-1.547-2.988-3.924-5.799-6.939-6.921-3.369-1.254-6.621-1.192-10.182-1.053-4.417.175-9.07 1.735-13.4.612-.829-.215-1.451.203-1.75.794-1.009.51-1.165 2.438.264 3.013 4.479 1.798 7.937 4.66 10.608 8.181 1.015-.338 2.019-.664 3.011-.977-2.064-2.895-4.554-5.459-7.617-7.496z M104.638 75.063c1.069-.533 2.184-.908 3.319-1.088C101.465 65.607 91.54 56.84 87.8 51.079c0-2.072.496-10.021-.203-10.471-.29-.188-.822-.151-1.529.063.604 3.479-2.408 8.744-2.408 10.406 3.899 6.015 14.534 15.297 20.978 23.986z M77.78 45.446c.635-.69 1.678-1.294 2.218-1.612-4.229-2.203-8.728-2.455-11.813-2.162 2.548.24 7.123 2.103 9.595 3.774z M67.565 76.646c-16.406 11.223-9.354 41.004-27.055 55.801.891.43 1.772.826 2.646 1.184 19.569-14.271 12.435-46.57 24.405-52.727-.342-.906-.342-2.533.004-4.258z M108.092 74.153c-5.341.782-10.467 6.021-12.411 3.312-.795 2.971-.945 5.9-.579 8.566 7.451 3.246 13.966-.652 17.36-4.996 3.423-4.379.541-10.508-4.37-16.882z M80.918 57.878 A 1.381 1.381 0 1 1 80.917 57.878 Z"

    func path(in rect: CGRect) -> Path {
        let scaleX = rect.width / 128.0
        let scaleY = rect.height / 128.0
        let scale = min(scaleX, scaleY)
        let dx = (rect.width - 128.0 * scale) / 2.0
        let dy = (rect.height - 128.0 * scale) / 2.0

        let parsed = SVGPathParser.parse(ZebraHeadShape.pathData)
        let transform = CGAffineTransform(translationX: rect.minX + dx, y: rect.minY + dy).scaledBy(x: scale, y: scale)
        return parsed.applying(transform)
    }
}

struct SVGPathParser {
    static func parse(_ svg: String) -> Path {
        var path = Path()
        let scanner = Scanner(string: svg)
        scanner.charactersToBeSkipped = CharacterSet.whitespacesAndNewlines.union(CharacterSet(charactersIn: ","))

        var currentPoint = CGPoint.zero
        var subpathStart = CGPoint.zero
        var lastControlPoint: CGPoint? = nil

        func readDouble() -> Double? {
            return scanner.scanDouble()
        }

        while !scanner.isAtEnd {
            guard let commandChar = scanner.scanCharacter() else { break }
            let isRelative = commandChar.isLowercase
            let cmd = commandChar.uppercased()

            switch cmd {
            case "M":
                guard let x = readDouble(), let y = readDouble() else { break }
                let pt = isRelative ? CGPoint(x: currentPoint.x + x, y: currentPoint.y + y) : CGPoint(x: x, y: y)
                path.move(to: pt)
                currentPoint = pt
                subpathStart = pt
                lastControlPoint = nil

                while let nx = readDouble(), let ny = readDouble() {
                    let npt = isRelative ? CGPoint(x: currentPoint.x + nx, y: currentPoint.y + ny) : CGPoint(x: nx, y: ny)
                    path.addLine(to: npt)
                    currentPoint = npt
                }

            case "L":
                while let x = readDouble(), let y = readDouble() {
                    let pt = isRelative ? CGPoint(x: currentPoint.x + x, y: currentPoint.y + y) : CGPoint(x: x, y: y)
                    path.addLine(to: pt)
                    currentPoint = pt
                    lastControlPoint = nil
                }

            case "C":
                while let x1 = readDouble(), let y1 = readDouble(),
                      let x2 = readDouble(), let y2 = readDouble(),
                      let x = readDouble(), let y = readDouble() {
                    let cp1 = isRelative ? CGPoint(x: currentPoint.x + x1, y: currentPoint.y + y1) : CGPoint(x: x1, y: y1)
                    let cp2 = isRelative ? CGPoint(x: currentPoint.x + x2, y: currentPoint.y + y2) : CGPoint(x: x2, y: y2)
                    let endPt = isRelative ? CGPoint(x: currentPoint.x + x, y: currentPoint.y + y) : CGPoint(x: x, y: y)
                    path.addCurve(to: endPt, control1: cp1, control2: cp2)
                    currentPoint = endPt
                    lastControlPoint = cp2
                }

            case "S":
                while let x2 = readDouble(), let y2 = readDouble(),
                      let x = readDouble(), let y = readDouble() {
                    let cp1 = lastControlPoint != nil ? CGPoint(x: 2 * currentPoint.x - lastControlPoint!.x, y: 2 * currentPoint.y - lastControlPoint!.y) : currentPoint
                    let cp2 = isRelative ? CGPoint(x: currentPoint.x + x2, y: currentPoint.y + y2) : CGPoint(x: x2, y: y2)
                    let endPt = isRelative ? CGPoint(x: currentPoint.x + x, y: currentPoint.y + y) : CGPoint(x: x, y: y)
                    path.addCurve(to: endPt, control1: cp1, control2: cp2)
                    currentPoint = endPt
                    lastControlPoint = cp2
                }

            case "A":
                while let rx = readDouble(), let ry = readDouble(),
                      let _ = readDouble(), let _ = readDouble(),
                      let _ = readDouble(), let x = readDouble(), let y = readDouble() {
                    let endPt = isRelative ? CGPoint(x: currentPoint.x + x, y: currentPoint.y + y) : CGPoint(x: x, y: y)
                    path.addEllipse(in: CGRect(x: endPt.x - rx, y: endPt.y - ry, width: rx * 2, height: ry * 2))
                    currentPoint = endPt
                    lastControlPoint = nil
                }

            case "Z":
                path.closeSubpath()
                currentPoint = subpathStart
                lastControlPoint = nil

            default:
                break
            }
        }
        return path
    }
}

// MARK: - Zebra Badge Icon View matching Android ic_zebra_head container
struct ZebraBadgeView: View {
    var size: CGFloat = 26
    var iconSize: CGFloat = 18

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.35)
                .fill(Color.accentContainer)
                .frame(width: size, height: size)

            ZebraHeadShape()
                .fill(Color.accentOnContainer)
                .frame(width: iconSize, height: iconSize)
        }
    }
}

// MARK: - Main ContentView
struct ContentView: View {
    @State private var isAuthenticated = false
    @State private var selectedTab: NavTab = .home
    @Environment(\.scenePhase) private var scenePhase

    enum NavTab {
        case home
        case chat
    }

    var body: some View {
        ZStack {
            Color.bgLight
                .ignoresSafeArea()

            if isAuthenticated {
                VStack(spacing: 0) {
                    Group {
                        switch selectedTab {
                        case .home:
                            DashboardView()
                        case .chat:
                            ChatView()
                        }
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)

                    // Navigation Suite Bar (Bottom Nav) matching Android NavigationSuiteScaffold
                    CustomBottomNavBar(selectedTab: $selectedTab)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .ignoresSafeArea(.all, edges: .bottom) // Ensure bottom nav reaches the very bottom
            } else {
                LockedSplashScreen {
                    authenticateUser()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .ignoresSafeArea(.all) // Global ignore to ensure edge-to-edge
        .onAppear {
            authenticateUser()
        }
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .background {
                // Lock security screen when leaving app
                isAuthenticated = false
            }
        }
    }

    private func authenticateUser() {
        let context = LAContext()
        var error: NSError?

        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) ||
           context.canEvaluatePolicy(.deviceOwnerAuthentication, error: &error) {
            let reason = "Authenticate using your biometric credential to unlock Private Banking"
            context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason) { success, _ in
                DispatchQueue.main.async {
                    self.isAuthenticated = success
                }
            }
        } else {
            // Fallback for environment without biometrics configured
            DispatchQueue.main.async {
                self.isAuthenticated = true
            }
        }
    }
}

// MARK: - Locked Splash Screen matching Android LockedSplashScreen
struct LockedSplashScreen: View {
    var onUnlockClick: () -> Void

    var body: some View {
        ZStack {
            Color.bgLight.ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                ZebraBadgeView(size: 96, iconSize: 64)

                Spacer().frame(height: 24)

                Text("Investec Private Banking")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(Color.textPrimary)

                Spacer().frame(height: 8)

                Text("Security Verification Required")
                    .font(.system(size: 14))
                    .foregroundColor(Color.textMuted)

                Spacer().frame(height: 48)

                Button(action: onUnlockClick) {
                    HStack(spacing: 8) {
                        Image(systemName: "lock.fill")
                            .font(.system(size: 16))
                        Text("Unlock with Biometrics")
                            .font(.system(size: 16, weight: .semibold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 14)
                    .background(Color.accentOnContainer)
                    .cornerRadius(24)
                }

                Spacer()
            }
        }
        .ignoresSafeArea()
    }
}

// MARK: - Custom Bottom Nav Bar matching Android NavigationSuiteScaffold
struct CustomBottomNavBar: View {
    @Binding var selectedTab: ContentView.NavTab

    var body: some View {
        HStack {
            Spacer()

            // Home Tab
            Button(action: { selectedTab = .home }) {
                VStack(spacing: 4) {
                    ZStack {
                        Capsule()
                            .fill(selectedTab == .home ? Color.accentContainer : Color.clear)
                            .frame(width: 60, height: 32)
                        Image(systemName: "house.fill")
                            .font(.system(size: 18))
                            .foregroundColor(selectedTab == .home ? Color.accentOnContainer : Color.textSecondary)
                    }
                    Text("Home")
                        .font(.system(size: 12, weight: selectedTab == .home ? .bold : .regular))
                        .foregroundColor(selectedTab == .home ? Color.accentOnContainer : Color.textSecondary)
                }
            }

            Spacer()

            // Chat Tab
            Button(action: { selectedTab = .chat }) {
                VStack(spacing: 4) {
                    ZStack {
                        Capsule()
                            .fill(selectedTab == .chat ? Color.accentContainer : Color.clear)
                            .frame(width: 60, height: 32)
                        Image(systemName: "message.fill")
                            .font(.system(size: 18))
                            .foregroundColor(selectedTab == .chat ? Color.accentOnContainer : Color.textSecondary)
                    }
                    Text("Chat")
                        .font(.system(size: 12, weight: selectedTab == .chat ? .bold : .regular))
                        .foregroundColor(selectedTab == .chat ? Color.accentOnContainer : Color.textSecondary)
                }
            }

            Spacer()
        }
        .padding(.top, 8)
        .padding(.bottom, 24) // Extra padding for home indicator area when ignoring safe area
        .background(Color.bgLight)
        .overlay(
            Rectangle()
                .frame(height: 1)
                .foregroundColor(Color.cardBorder.opacity(0.3)),
            alignment: .top
        )
    }
}

// MARK: - Profile Helper Struct
struct ProfileItem: Hashable, Identifiable {
    let id: String
    let name: String
}

// MARK: - Dashboard View matching Android DashboardScreen
struct DashboardView: View {
    @State private var accounts: [BankAccountEntity] = []
    @State private var selectedAccount: BankAccountEntity?
    @State private var selectedProfileId: String? = nil
    @State private var transactions: [TransactionEntity] = []
    @State private var isRefreshing = false
    @State private var syncMessage: String? = nil

    @State private var useSandbox = true
    @State private var clientId = ""
    @State private var clientSecret = ""
    @State private var apiKey = ""
    @State private var showSettings = false
    @State private var showProfileDropdown = false

    private let repository = IOSBankRepositoryFactory.shared.create()

    // Extract unique profiles list from accounts
    private var profiles: [ProfileItem] {
        var seen = Set<String>()
        var list: [ProfileItem] = []
        for acc in accounts {
            if !seen.contains(acc.profileId) {
                seen.insert(acc.profileId)
                list.append(ProfileItem(id: acc.profileId, name: acc.profileName))
            }
        }
        return list
    }

    // Filter accounts belonging to active profile
    private var activeProfileId: String? {
        if let id = selectedProfileId, profiles.contains(where: { $0.id == id }) {
            return id
        }
        return profiles.first?.id
    }

    private var filteredAccounts: [BankAccountEntity] {
        if let pid = activeProfileId {
            return accounts.filter { $0.profileId == pid }
        }
        return accounts
    }

    private var activeAccount: BankAccountEntity? {
        if let sel = selectedAccount, filteredAccounts.contains(where: { $0.accountId == sel.accountId }) {
            return sel
        }
        return filteredAccounts.first
    }

    var body: some View {
        GeometryReader { geometry in
            VStack(spacing: 0) {
                // Top App Bar matching Android TopAppBar
                HStack {
                    HStack(spacing: 10) {
                        ZebraBadgeView(size: 26, iconSize: 18)

                        Text("Investec Private Banking")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(Color.textPrimary)
                    }

                    Spacer()

                    // Refresh Button
                    Button(action: { syncData() }) {
                        if isRefreshing {
                            ProgressView()
                                .scaleEffect(0.8)
                        } else {
                            Image(systemName: "arrow.clockwise")
                                .font(.system(size: 18))
                                .foregroundColor(Color.textSecondary)
                        }
                    }
                    .disabled(isRefreshing)
                    .padding(.trailing, 12)

                    // Settings Button
                    Button(action: {
                        withAnimation(.spring()) {
                            showSettings.toggle()
                        }
                    }) {
                        Image(systemName: "gearshape.fill")
                            .font(.system(size: 18))
                            .foregroundColor(showSettings ? Color.accentOnContainer : Color.textSecondary)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, max(geometry.safeAreaInsets.top, 20))
                .padding(.bottom, 12)
                .background(Color.bgLight)

                ScrollView {
                    VStack(spacing: 16) {
                        // Credentials Settings Form matching Android CredentialsSettingsForm
                    if showSettings {
                        CredentialsSettingsFormView(
                            useSandbox: $useSandbox,
                            clientId: $clientId,
                            clientSecret: $clientSecret,
                            apiKey: $apiKey,
                            onSave: {
                                saveSettings()
                                withAnimation { showSettings = false }
                            },
                            onCancel: {
                                withAnimation { showSettings = false }
                            }
                        )
                        .transition(.move(edge: .top).combined(with: .opacity))
                    }

                    // Notification banner matching Android Snackbar
                    if let msg = syncMessage {
                        HStack {
                            Text(msg)
                               .font(.system(size: 13, weight: .medium))
                               .foregroundColor(Color.accentOnContainer)
                            Spacer()
                            Button(action: { syncMessage = nil }) {
                                Image(systemName: "xmark")
                                   .font(.system(size: 12))
                                   .foregroundColor(Color.textSecondary)
                            }
                        }
                        .padding(12)
                        .background(Color.accentContainer)
                        .cornerRadius(8)
                    }

                    if accounts.isEmpty && !isRefreshing {
                        // Empty State View matching Android EmptyStateView
                        EmptyStateView(
                            onRefresh: { syncData() },
                            onOpenSettings: { withAnimation { showSettings = true } }
                        )
                    } else {
                        // Profile Selection Dropdown matching Android Profile Dropdown Menu
                        if !profiles.isEmpty {
                            VStack(spacing: 0) {
                                Button(action: {
                                    withAnimation { showProfileDropdown.toggle() }
                                }) {
                                    HStack {
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text("Select Profile")
                                                .font(.system(size: 11, weight: .bold))
                                                .foregroundColor(Color.textMuted)

                                            Text(profiles.first(where: { $0.id == activeProfileId })?.name ?? "Default Profile")
                                                .font(.system(size: 15, weight: .bold))
                                                .foregroundColor(Color.textPrimary)
                                        }

                                        Spacer()

                                        Image(systemName: showProfileDropdown ? "chevron.up" : "chevron.down")
                                            .font(.system(size: 14, weight: .bold))
                                            .foregroundColor(Color.textSecondary)
                                    }
                                    .padding(16)
                                    .background(Color.cardSurface)
                                    .cornerRadius(12)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(Color.cardBorder, lineWidth: 1)
                                    )
                                }
                                .buttonStyle(PlainButtonStyle())

                                if showProfileDropdown {
                                    VStack(spacing: 0) {
                                        ForEach(profiles) { prof in
                                            Button(action: {
                                                selectedProfileId = prof.id
                                                selectedAccount = nil
                                                withAnimation { showProfileDropdown = false }
                                                if let firstAcc = accounts.first(where: { $0.profileId == prof.id }) {
                                                    fetchTransactions(accountId: firstAcc.accountId)
                                                }
                                            }) {
                                                HStack {
                                                    Text(prof.name)
                                                        .font(.system(size: 14, weight: prof.id == activeProfileId ? .bold : .regular))
                                                        .foregroundColor(Color.textPrimary)
                                                    Spacer()
                                                    if prof.id == activeProfileId {
                                                        Image(systemName: "checkmark")
                                                            .font(.system(size: 12, weight: .bold))
                                                            .foregroundColor(Color.accentOnContainer)
                                                    }
                                                }
                                                .padding(.horizontal, 16)
                                                .padding(.vertical, 12)
                                                .background(Color.cardSurface)
                                            }
                                            .buttonStyle(PlainButtonStyle())

                                            if prof != profiles.last {
                                                Divider()
                                                    .background(Color.cardBorder.opacity(0.4))
                                            }
                                        }
                                    }
                                    .cornerRadius(12)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(Color.cardBorder, lineWidth: 1)
                                    )
                                    .padding(.top, 4)
                                    .transition(.move(edge: .top).combined(with: .opacity))
                                }
                            }
                        }

                        // Accounts Section Header matching Android "Your Accounts"
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Your Accounts")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(Color.textSecondary)

                            // Horizontal Bank Accounts List matching BankAccountGlossyCard
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 12) {
                                    ForEach(filteredAccounts, id: \.accountId) { account in
                                        BankAccountGlossyCard(
                                            account: account,
                                            isSelected: activeAccount?.accountId == account.accountId,
                                            onSelect: {
                                                selectedAccount = account
                                                fetchTransactions(accountId: account.accountId)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Balance Detailed Metrics Card for activeAccount matching Android BalanceDetailedMetricsCard
                        if let currentActive = activeAccount {
                            BalanceDetailedMetricsCard(account: currentActive)

                            // Recent Transactions Header with KYC status pill matching Android
                            VStack(alignment: .leading, spacing: 12) {
                                HStack {
                                    Text("RECENT TRANSACTIONS")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(Color.textSecondary)

                                    Spacer()

                                    Text(currentActive.kycCompliant ? "KYC COMPLIANT" : "KYC PENDING")
                                        .font(.system(size: 8, weight: .bold))
                                        .foregroundColor(currentActive.kycCompliant ? Color.greenCredit : Color.redDebit)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 2)
                                        .background(currentActive.kycCompliant ? Color(red: 0.91, green: 0.96, blue: 0.91) : Color(red: 1.0, green: 0.92, blue: 0.93))
                                        .cornerRadius(20)
                                }
                                .padding(.horizontal, 16)
                                .padding(.vertical, 10)
                                .background(Color(red: 0.945, green: 0.953, blue: 0.976))
                                .cornerRadius(8)

                                // Transactions List matching Android TransactionRow
                                if transactions.isEmpty {
                                    VStack(spacing: 8) {
                                        Image(systemName: "info.circle")
                                            .font(.system(size: 32))
                                            .foregroundColor(Color.textMuted)

                                        Text("No recent transaction postings found for this account.")
                                            .font(.system(size: 12))
                                            .foregroundColor(Color.textSecondary)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 24)
                                    .background(Color.cardSurface)
                                    .cornerRadius(20)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 20)
                                            .stroke(Color.cardBorder, lineWidth: 1)
                                    )
                                } else {
                                    VStack(spacing: 8) {
                                        ForEach(transactions, id: \.id) { tx in
                                            TransactionRowView(
                                                tx: tx,
                                                currencySymbol: getCurrencySymbol(currentActive.currency)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .padding(16)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .onAppear {
            setupRepository()
            loadCachedData()
        }
    }

    private func setupRepository() {
        repository.onSyncCompleted = {
            self.loadCachedData()
        }
    }

    private func loadCachedData() {
        repository.getAccounts { result, _ in
            if let accountsList = result {
                self.accounts = accountsList
                if self.selectedAccount == nil {
                    self.selectedAccount = accountsList.first
                }
                if let active = self.activeAccount {
                    self.fetchTransactions(accountId: active.accountId)
                }
            }
        }

        repository.useSandbox { result, _ in
            if let useSandboxVal = result {
                self.useSandbox = useSandboxVal.boolValue
            }
        }
        self.clientId = repository.getClientId()
        self.clientSecret = repository.getClientSecret()
        self.apiKey = repository.getApiKey()
    }

    private func fetchTransactions(accountId: String) {
        repository.getLastFiveTransactions(accountId: accountId) { result, _ in
            if let txList = result {
                self.transactions = txList
            }
        }
    }

    private func syncData() {
        isRefreshing = true
        syncMessage = "Synchronizing account details..."
        repository.syncData { _, error in
            isRefreshing = false
            if let err = error {
                syncMessage = "Sync failed: \(err.localizedDescription)"
            } else {
                syncMessage = "Sync complete"
                self.loadCachedData()
            }
        }
    }

    private func saveSettings() {
        repository.setUseSandbox(use: useSandbox) { _ in
            self.repository.setClientId(clientId: clientId)
            self.repository.setClientSecret(secret: clientSecret)
            self.repository.setApiKey(apiKey: apiKey)
            self.syncData()
        }
    }
}

// MARK: - Bank Account Glossy Card matching Android BankAccountGlossyCard
struct BankAccountGlossyCard: View {
    let account: BankAccountEntity
    let isSelected: Bool
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            ZStack(alignment: .topTrailing) {
                VStack(alignment: .leading, spacing: 0) {
                    // Top Section
                    VStack(alignment: .leading, spacing: 2) {
                        Text(account.productName.isEmpty ? "PRIVATE BANKING" : account.productName.uppercased())
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(isSelected ? Color.accentOnContainer.opacity(0.8) : Color.textSecondary)

                        Text(account.accountName)
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(isSelected ? Color.accentOnContainer : Color.textPrimary)
                            .lineLimit(1)

                        Text(maskAccountNumber(account.accountNumber))
                            .font(.system(size: 11, design: .monospaced))
                            .foregroundColor(isSelected ? Color.accentOnContainer.opacity(0.7) : Color.textMuted)
                    }

                    Spacer()

                    // Bottom Section
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Available Balance")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundColor(isSelected ? Color.accentOnContainer.opacity(0.8) : Color.textSecondary)

                        Text("\(getCurrencySymbol(account.currency)) \(formatNumber(account.availableBalance))")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(isSelected ? Color.accentOnContainer : Color.textPrimary)
                    }
                }
                .padding(16)
                .frame(width: 240, height: 155, alignment: .leading)

                if isSelected {
                    Circle()
                        .fill(Color.accentOnContainer)
                        .frame(width: 8, height: 8)
                        .padding(12)
                }
            }
            .background(isSelected ? Color.accentContainer : Color.cardSurface)
            .cornerRadius(24)
            .overlay(
                RoundedRectangle(cornerRadius: 24)
                    .stroke(isSelected ? Color.accentOnContainer : Color.cardBorder, lineWidth: isSelected ? 2 : 1)
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Balance Detailed Metrics Card matching Android BalanceDetailedMetricsCard
struct BalanceDetailedMetricsCard: View {
    let account: BankAccountEntity

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // Header with Zebra icon badge and account details
            HStack {
                HStack(spacing: 10) {
                    ZebraBadgeView(size: 36, iconSize: 18)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(account.accountName)
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(Color.textPrimary)

                        Text("**** " + String(account.accountNumber.suffix(4)))
                            .font(.system(size: 12))
                            .foregroundColor(Color.textSecondary)
                    }
                }

                Spacer()

                Image(systemName: "lock.fill")
                    .font(.system(size: 16))
                    .foregroundColor(Color.textSecondary)
            }

            Spacer().frame(height: 16)

            // Large Available Balance Display
            VStack(alignment: .leading, spacing: 2) {
                Text("AVAILABLE BALANCE")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(Color.textSecondary)

                Text("\(getCurrencySymbol(account.currency)) \(formatNumber(account.availableBalance))")
                    .font(.system(size: 32, weight: .bold))
                    .foregroundColor(Color.textPrimary)
            }

            Spacer().frame(height: 14)
            Rectangle()
                .frame(height: 1)
                .foregroundColor(Color(red: 0.882, green: 0.886, blue: 0.914))
            Spacer().frame(height: 14)

            // Sub details (Current/Ledger Balance)
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Ledger Current Balance")
                        .font(.system(size: 12))
                        .foregroundColor(Color.textSecondary)

                    Text("\(getCurrencySymbol(account.currency)) \(formatNumber(account.currentBalance))")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color.textPrimary)
                }

                Spacer()

                Rectangle()
                    .frame(width: 1, height: 36)
                    .foregroundColor(Color(red: 0.882, green: 0.886, blue: 0.914))

                Spacer()

                VStack(alignment: .trailing, spacing: 2) {
                    Text("Currency Type")
                        .font(.system(size: 12))
                        .foregroundColor(Color.textSecondary)

                    Text(account.currency.uppercased())
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(Color.textPrimary)
                }
            }

            Spacer().frame(height: 12)
            Rectangle()
                .frame(height: 1)
                .foregroundColor(Color(red: 0.882, green: 0.886, blue: 0.914))
            Spacer().frame(height: 12)

            // Footer info
            HStack {
                Text("Ref: \(account.referenceName.isEmpty ? "Investec Personal" : account.referenceName)")
                    .font(.system(size: 11))
                    .foregroundColor(Color.textMuted)

                Spacer()

                Text("Synced: \(formatTimestamp(account.lastUpdated))")
                    .font(.system(size: 11))
                    .foregroundColor(Color.textMuted)
            }
        }
        .padding(20)
        .background(Color.cardSurface)
        .cornerRadius(28)
        .overlay(
            RoundedRectangle(cornerRadius: 28)
                .stroke(Color.cardBorder, lineWidth: 1)
        )
    }
}

// MARK: - Transaction Row Component matching Android TransactionRow
struct TransactionRowView: View {
    let tx: TransactionEntity
    let currencySymbol: String

    var isCredit: Bool {
        return tx.type.uppercased() == "CREDIT"
    }

    var iconName: String {
        let desc = tx.description_.uppercased()
        if desc.contains("APPLE") || desc.contains("STORE") {
            return "cart.fill"
        } else if desc.contains("COFFEE") || desc.contains("CAFE") {
            return "heart.fill"
        } else if desc.contains("SALARY") || desc.contains("DEPOSIT") || desc.contains("PAYMENT") {
            return "checkmark"
        } else if desc.contains("UTILITY") || desc.contains("ELEC") || desc.contains("POWER") {
            return "star.fill"
        } else if desc.contains("RESTAURANT") || desc.contains("GRILL") || desc.contains("FOOD") {
            return "person.fill"
        } else if isCredit {
            return "plus"
        } else {
            return "chevron.down"
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            // White circular icon container with crisp border matching Android
            ZStack {
                Circle()
                    .fill(Color.white)
                    .frame(width: 38, height: 38)
                    .overlay(Circle().stroke(Color.cardBorder, lineWidth: 1))

                Image(systemName: iconName)
                    .font(.system(size: 16))
                    .foregroundColor(Color.textSecondary)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(tx.description_.trimmingCharacters(in: .whitespaces).uppercased())
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(Color.textPrimary)
                    .lineLimit(1)

                HStack(spacing: 6) {
                    Text(tx.transactionType)
                        .font(.system(size: 11))
                        .foregroundColor(Color.textMuted)

                    Text(tx.status)
                        .font(.system(size: 8, weight: .bold))
                        .foregroundColor(tx.status == "POSTED" ? Color.textSecondary : Color.accentOnContainer)
                        .padding(.horizontal, 4)
                        .padding(.vertical, 1)
                        .background(tx.status == "POSTED" ? Color(red: 0.945, green: 0.953, blue: 0.976) : Color.accentContainer)
                        .cornerRadius(4)
                }
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 2) {
                Text("\(isCredit ? "+" : "-")\(currencySymbol)\(formatNumber(tx.amount))")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(isCredit ? Color.greenCredit : Color.redDebit)

                Text(formatDate(tx.postingDate ?? tx.transactionDate ?? ""))
                    .font(.system(size: 10))
                    .foregroundColor(Color.textMuted)
            }
        }
        .padding(12)
        .background(Color.cardSurface)
        .cornerRadius(20)
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(Color(red: 0.882, green: 0.886, blue: 0.914), lineWidth: 1)
        )
    }
}

// MARK: - Credentials Settings Form matching Android CredentialsSettingsForm
struct CredentialsSettingsFormView: View {
    @Binding var useSandbox: Bool
    @Binding var clientId: String
    @Binding var clientSecret: String
    @Binding var apiKey: String
    var onSave: () -> Void
    var onCancel: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Secure API Connection Settings")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Color.textPrimary)

            Text("By default, the app uses standard, fully open-access Investec API Sandboxes so you can browse, review balances, and test immediate refresh integrations instantly.")
                .font(.system(size: 12))
                .foregroundColor(Color.textSecondary)

            // Sandbox mode toggle
            Toggle(isOn: $useSandbox) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Developer Sandbox Mode")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(Color.textPrimary)
                    Text("Deploys read-only mock banks")
                        .font(.system(size: 11))
                        .foregroundColor(Color.textSecondary)
                }
            }
            .tint(Color.accentOnContainer)

            if !useSandbox {
                VStack(alignment: .leading, spacing: 10) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Client ID")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(Color.textSecondary)
                        TextField("Client ID", text: $clientId)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Text("Client Secret")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(Color.textSecondary)
                        SecureField("Client Secret", text: $clientSecret)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Text("API Key")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(Color.textSecondary)
                        TextField("x-api-key", text: $apiKey)
                            .textFieldStyle(RoundedBorderTextFieldStyle())
                    }
                }
            } else {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Active Sandbox Credentials:")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(Color.textSecondary)

                    Text("Client ID: yAxzQRFX97vO...")
                        .font(.system(size: 12, design: .monospaced))
                        .foregroundColor(Color.textPrimary)

                    Text("Secret: **** (Active Personal sandbox)")
                        .font(.system(size: 12, design: .monospaced))
                        .foregroundColor(Color.textPrimary)

                    Text("BaseUrl: https://openapisandbox.investec.com")
                        .font(.system(size: 12, weight: .bold, design: .monospaced))
                        .foregroundColor(Color.greenCredit)
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(red: 0.945, green: 0.953, blue: 0.976))
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.cardBorder, lineWidth: 1)
                )
            }

            // Buttons actions matching Android
            HStack {
                Spacer()

                Button(action: onCancel) {
                    Text("Cancel")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(Color.textSecondary)
                }

                Spacer().frame(width: 12)

                Button(action: onSave) {
                    Text("Apply & Sync")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(Color.accentOnContainer)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .background(Color.accentContainer)
                        .cornerRadius(20)
                }
            }
        }
        .padding(20)
        .background(Color.cardSurface)
        .cornerRadius(24)
        .overlay(
            RoundedRectangle(cornerRadius: 24)
                .stroke(Color.cardBorder, lineWidth: 1)
        )
    }
}

// MARK: - Empty State View matching Android EmptyStateView
struct EmptyStateView: View {
    var onRefresh: () -> Void
    var onOpenSettings: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Spacer().frame(height: 32)
            
            ZStack {
                Circle()
                    .fill(Color.accentContainer)
                    .frame(width: 80, height: 80)

                Image(systemName: "info.circle.fill")
                    .font(.system(size: 40))
                    .foregroundColor(Color.accentOnContainer)
            }

            Spacer().frame(height: 16)

            Text("Secure Bank Cache Empty")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(Color.textPrimary)

            Spacer().frame(height: 8)

            Text("No local bank summaries exist yet. Let's sync with the Investec personal account sandbox server to load dynamic demo profiles, cash streams, and recent postings.")
                .font(.system(size: 13))
                .foregroundColor(Color.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 16)

            Spacer().frame(height: 24)

            HStack(spacing: 12) {
                Button(action: onOpenSettings) {
                    HStack(spacing: 6) {
                        Image(systemName: "gearshape.fill")
                            .font(.system(size: 14))
                        Text("API Setup")
                            .font(.system(size: 14, weight: .semibold))
                    }
                    .foregroundColor(Color.textPrimary)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color(red: 0.882, green: 0.886, blue: 0.914))
                    .cornerRadius(20)
                }

                Button(action: onRefresh) {
                    HStack(spacing: 6) {
                        Image(systemName: "arrow.clockwise")
                            .font(.system(size: 14))
                        Text("Sync Sandbox Now")
                            .font(.system(size: 14, weight: .semibold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(Color.accentOnContainer)
                    .cornerRadius(20)
                }
            }

            Spacer().frame(height: 32)
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(Color.bgLight)
    }
}

// MARK: - Chat View matching Android ChatScreen ("Alex" AI Financial Assistant)
struct ChatView: View {
    @State private var messages: [Message] = [
        Message(text: "Hello! I'm Alex, your Investec AI Financial Assistant. How can I help with your accounts or transactions today?", isUser: false)
    ]
    @State private var inputText = ""

    struct Message: Identifiable {
        let id = UUID()
        let text: String
        let isUser: Bool
        var isSystem: Bool = false
    }

    var body: some View {
        GeometryReader { geometry in
            VStack(spacing: 0) {
                // Top App Bar matching Android TopAppBar in ChatScreen
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Color.accentContainer)
                            .frame(width: 36, height: 36)

                        Text("A")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(Color.accentOnContainer)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Alex")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(Color.textPrimary)

                        Text("AI Financial Assistant")
                            .font(.system(size: 11))
                            .foregroundColor(Color.textMuted)
                    }

                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.top, max(geometry.safeAreaInsets.top, 20))
                .padding(.bottom, 12)
                .background(Color.bgLight)

                // Message Thread
                ScrollViewReader { proxy in
                    ScrollView {
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(messages.filter { !$0.isSystem && !$0.text.trimmingCharacters(in: .whitespaces).isEmpty }) { msg in
                                HStack {
                                    if msg.isUser { Spacer() }

                                    Text(msg.text)
                                        .font(.system(size: 14))
                                        .padding(12)
                                        .foregroundColor(msg.isUser ? Color.accentOnContainer : Color.textPrimary)
                                        .background(msg.isUser ? Color.accentContainer : Color.cardSurface)
                                        .cornerRadius(16)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 16)
                                                .stroke(msg.isUser ? Color.clear : Color.cardBorder, lineWidth: 1)
                                        )

                                    if !msg.isUser { Spacer() }
                                }
                                .id(msg.id)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.bgLight)
                    .onChange(of: messages.count) { _ in
                        if let lastId = messages.last?.id {
                            withAnimation {
                                proxy.scrollTo(lastId, anchor: .bottom)
                            }
                        }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

                // Input Bar matching Android OutlinedTextField and Send Button
                HStack(spacing: 8) {
                    TextField("Type a message...", text: $inputText)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        .background(Color.cardSurface)
                        .cornerRadius(24)
                        .overlay(
                            RoundedRectangle(cornerRadius: 24)
                                .stroke(Color.cardBorder, lineWidth: 1)
                        )

                    Button(action: {
                        let trimmed = inputText.trimmingCharacters(in: .whitespaces)
                        if !trimmed.isEmpty {
                            let textToSend = trimmed
                            inputText = ""
                            sendMessage(textToSend)
                        }
                    }) {
                        Text("Send")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(Color.textPrimary)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 12)
                            .background(Color.accentContainer)
                            .cornerRadius(24)
                            .overlay(
                                RoundedRectangle(cornerRadius: 24)
                                    .stroke(Color.accentOnContainer, lineWidth: 1)
                            )
                    }
                    .disabled(inputText.trimmingCharacters(in: .whitespaces).isEmpty)
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 12)
                .background(Color.bgLight)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func sendMessage(_ text: String) {
        messages.append(Message(text: text, isUser: true))

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) {
            let reply = "I've received your query about '\(text)'. As your Investec AI assistant, I can assist with tracking account balances, transaction histories, or managing configuration settings."
            messages.append(Message(text: reply, isUser: false))
        }
    }
}

// MARK: - Value Formatting Helpers matching Android
func getCurrencySymbol(_ isoCode: String) -> String {
    switch isoCode.uppercased() {
    case "ZAR": return "R"
    case "USD": return "$"
    case "EUR": return "€"
    case "GBP": return "£"
    default: return isoCode
    }
}

func maskAccountNumber(_ accNum: String) -> String {
    if accNum.count < 5 { return accNum }
    return "****" + String(accNum.suffix(4))
}

func formatDate(_ rawDate: String) -> String {
    if rawDate.isEmpty { return "" }
    let dfInput = DateFormatter()
    dfInput.locale = Locale(identifier: "en_US_POSIX")
    dfInput.dateFormat = "yyyy-MM-dd"
    if let date = dfInput.date(from: rawDate) {
        let dfOutput = DateFormatter()
        dfOutput.locale = Locale(identifier: "en_US")
        dfOutput.dateFormat = "dd MMM yyyy"
        return dfOutput.string(from: date)
    }
    dfInput.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
    if let date = dfInput.date(from: rawDate) {
        let dfOutput = DateFormatter()
        dfOutput.locale = Locale(identifier: "en_US")
        dfOutput.dateFormat = "dd MMM yyyy"
        return dfOutput.string(from: date)
    }
    return rawDate
}

func formatNumber(_ number: Double) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return formatter.string(from: NSNumber(value: number)) ?? String(format: "%.2f", number)
}

func formatTimestamp(_ timestamp: Int64) -> String {
    let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000.0)
    let formatter = DateFormatter()
    formatter.dateFormat = "HH:mm:ss, dd MMM yyyy"
    return formatter.string(from: date)
}
