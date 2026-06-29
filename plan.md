# Kế hoạch dự án: Biến điện thoại Android thành Microphone cho Windows

## 1. Mục tiêu

Xây dựng hệ thống gồm 2 thành phần:
- **App Android**: ghi âm từ mic điện thoại, gửi qua mạng (LAN/Wi-Fi) tới máy Windows.
- **App Windows**: nhận luồng âm thanh, phát ra một thiết bị audio ảo (Virtual Audio Cable) để các phần mềm khác (Zoom, Discord, OBS, Teams...) nhận diện như một microphone bình thường.

Yêu cầu phi chức năng quan trọng nhất: **độ trễ thấp** (mục tiêu < 150ms end-to-end) và **ổn định** (không bị giật, ngắt khi mạng dao động nhẹ).

---

## 2. Kiến trúc tổng thể

```
[Android: AudioRecord] -> [Encode (PCM hoặc Opus)] -> [UDP Socket]
        --(LAN/Wi-Fi hoặc USB-tether)-->
[Windows: UDP Listener] -> [Decode] -> [NAudio WaveOut] -> [VB-Audio "CABLE Input"]
        -->
[CABLE Output được chọn làm Microphone trong Zoom/Discord/OBS...]
```

**Quan trọng**: Windows app KHÔNG tự tạo driver microphone (cần ký số, rất phức tạp). Thay vào đó dùng **VB-Audio Virtual Cable** (free, có sẵn) làm cầu nối. App Windows chỉ đóng vai trò "phát nhạc" vào input của cable đó.

---

## 3. Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Android app | Kotlin, Android Studio, `AudioRecord` API |
| Truyền dữ liệu | UDP socket (raw PCM hoặc nén Opus qua thư viện `concentus`) |
| Windows app | C# (.NET 8), thư viện **NAudio** |
| Audio routing trên Windows | VB-Audio Virtual Cable (cài đặt thủ công, không tự code) |
| Giao tiếp cấu hình (tùy chọn, phase sau) | Đơn giản: nhập IP bằng tay; nâng cao: mDNS/UDP broadcast để tự discovery |

---

## 4. Cấu trúc thư mục đề xuất

```
android-mic-bridge/
├── android-app/              # Project Android Studio (Kotlin)
│   ├── app/src/main/java/.../
│   │   ├── MainActivity.kt
│   │   ├── AudioCaptureService.kt   # Foreground service ghi âm
│   │   ├── UdpStreamer.kt           # Gửi gói tin UDP
│   │   └── OpusEncoderWrapper.kt    # (phase 2, optional)
│   └── app/src/main/AndroidManifest.xml
│
├── windows-app/               # Project C# (.NET, WinForms hoặc WPF đơn giản)
│   ├── Program.cs
│   ├── UdpReceiver.cs
│   ├── AudioPlayer.cs          # Dùng NAudio ghi ra device ảo
│   └── MainForm.cs             # UI: nhập IP, chọn output device, start/stop, hiển thị mức âm lượng
│
└── docs/
    └── README.md               # Hướng dẫn cài VB-Cable, build, chạy
```

---

## 5. Các giai đoạn triển khai (phases)

### Phase 0 — Khởi tạo project
- Tạo Android project Kotlin trống, target SDK mới nhất, min SDK 24+.
- Tạo Windows project C# .NET console hoặc WinForms.
- Thêm NAudio qua NuGet.
- Viết README hướng dẫn cài **VB-Audio Virtual Cable** trên Windows (link: https://vb-audio.com/Cable/), vì đây là bước cài đặt tay, không code được.

**Tiêu chí hoàn thành**: 2 project build chạy được, in "Hello World".

---

### Phase 1 — Android: Ghi âm cơ bản
- Khai báo quyền `RECORD_AUDIO` trong Manifest, xin runtime permission.
- Tạo `Foreground Service` (bắt buộc từ Android 9+ để ghi âm khi app ở background) với notification "Đang stream microphone".
- Dùng `AudioRecord`:
  - Sample rate: 48000 Hz (hoặc 16000 Hz nếu ưu tiên băng thông thấp)
  - Format: 16-bit PCM, mono
  - Source: `MediaRecorder.AudioSource.MIC` hoặc `VOICE_COMMUNICATION` (có lọc echo/noise tốt hơn cho mục đích này)
- Test: ghi ra file `.pcm`/`.wav` trên máy để xác nhận audio sạch, không méo.

**Tiêu chí hoàn thành**: ghi được file âm thanh nghe rõ, không nhiễu, không bị crash khi tắt/mở màn hình.

---

### Phase 2 — Windows: Nhận UDP cơ bản
- Viết `UdpReceiver` lắng nghe trên một port cố định (ví dụ 5005).
- In ra console số byte nhận mỗi giây để kiểm tra kết nối.
- Test bằng cách gửi gói tin UDP thử từ máy khác (hoặc từ chính Android) để xác nhận firewall không chặn.

**Tiêu chí hoàn thành**: Windows nhận đúng số byte gửi từ Android, không mất kết nối khi để chạy vài phút.

---

### Phase 3 — Ghép nối: Android gửi → Windows phát ra VB-Cable
- Android: thay việc ghi file bằng gửi từng buffer PCM qua UDP socket tới IP:port của máy Windows (đặt IP/port trong UI đơn giản, hoặc hard-code để test).
- Windows: dùng NAudio (`WaveOutEvent` hoặc `DirectSoundOut`) để phát buffer PCM nhận được, với `DeviceNumber` chỉ định tới thiết bị **"CABLE Input"**.
- Cấu hình Windows: vào Sound Settings, kiểm tra "CABLE Output" có lên mức âm lượng khi nói vào điện thoại không.
- Test thực tế: mở Discord/Zoom trên Windows, chọn Microphone = "CABLE Output", thử nói vào điện thoại và xác nhận âm thanh xuất hiện.

**Tiêu chí hoàn thành**: Voice chat app trên Windows nhận được âm thanh từ điện thoại Android qua mic ảo, độ trễ chấp nhận được bằng cảm nhận tai (chưa cần đo chính xác).

---

### Phase 4 — Ổn định hóa & UX
- Android: thêm UI start/stop service, hiển thị trạng thái kết nối, nút nhập IP Windows.
- Windows: UI hiển thị IP/port đang nghe (để người dùng gõ vào điện thoại), dropdown chọn output device, hiển thị VU meter (mức âm lượng) để debug.
- Xử lý mất gói UDP: nếu thiếu dữ liệu, chèn silence (im lặng) ngắn thay vì làm app crash hoặc bị treo.
- Xử lý reconnect khi đổi mạng Wi-Fi hoặc app Android bị Android OS kill service.
- Thêm buffer/jitter buffer nhỏ (vài chục ms) ở Windows để giảm giật do mạng dao động.

**Tiêu chí hoàn thành**: hệ thống chạy ổn định liên tục >30 phút, tự phục hồi khi mất gói tạm thời.

---

### Phase 5 (tùy chọn, nâng cao) — Tối ưu chất lượng & độ trễ
- Thêm mã hóa Opus (thư viện `concentus` bên Android, decode bên Windows bằng `Concentus` .NET hoặc `OpusDotNet`) để giảm băng thông, vẫn giữ chất lượng tốt.
- Thêm tùy chọn kết nối qua **USB Tethering + ADB reverse/forward** để giảm trễ và tránh phụ thuộc Wi-Fi (độ trễ qua USB thường ổn định hơn Wi-Fi).
- Thêm noise suppression / automatic gain control (Android có `NoiseSuppressor`, `AutomaticGainControl` API có sẵn).
- Thêm tự động discovery (Android broadcast UDP tìm Windows app trong cùng mạng) để không phải gõ IP tay.

**Tiêu chí hoàn thành**: độ trễ đo được < 150ms, chất lượng âm thanh ổn định khi nói liên tục, kết nối USB hoạt động như phương án dự phòng.

---

## 6. Rủi ro & lưu ý cần biết trước

- **Android background restriction**: Android 12+ giới hạn khá chặt việc chạy service nền lâu — phải dùng đúng `Foreground Service` với type `microphone` (khai báo `foregroundServiceType="microphone"` trong Manifest) để không bị OS kill.
- **Không thể tự tạo driver mic ảo trên Windows** mà không ký số kernel driver — đây là lý do bắt buộc phải dùng VB-Audio Cable có sẵn, không nên cố tự viết driver.
- **UDP có thể mất gói** trên Wi-Fi yếu — cần buffer chống giật, không cần 100% chính xác từng gói vì âm thanh người nghe tha thứ được mất gói nhỏ.
- **Firewall Windows** có thể chặn cổng UDP lần đầu chạy — cần hướng dẫn người dùng allow trong Windows Defender Firewall.
- **Phải cài thủ công** VB-Audio Cable trên Windows trước khi chạy app — không thể tự động hóa bước cài driver bên thứ 3 bằng code thông thường (cần installer riêng).

---

## 7. Định nghĩa "Done" cho MVP (Phase 0–3)

- [ ] Android ghi âm và gửi PCM qua UDP tới IP nhập tay
- [ ] Windows nhận UDP và phát ra "CABLE Input" qua NAudio
- [ ] Một app voice chat (Discord/Zoom) trên Windows chọn được "CABLE Output" làm mic và nhận đúng âm thanh nói từ điện thoại
- [ ] Có README hướng dẫn cài VB-Cable + cách build/run 2 app

---

## 8. Gợi ý cho opencode khi triển khai

- Bắt đầu code theo đúng thứ tự Phase 0 → 5, không nhảy cóc, vì mỗi phase đều có tiêu chí test rõ ràng trước khi qua phase sau.
- Giữ code đơn giản ở các phase đầu (PCM thô, không nén) để dễ debug audio có chạy được hay không trước khi thêm độ phức tạp (Opus, discovery...).
- Khi viết phần NAudio bên Windows, log rõ danh sách output devices ra console để dễ chọn đúng index của "CABLE Input".
- Khi test, luôn kiểm tra Windows Volume Mixer / Sound Settings để xác nhận có tín hiệu chạy vào CABLE trước khi nghi code Windows sai.