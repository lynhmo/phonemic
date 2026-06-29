# Phone Mic — Biến điện thoại Android thành Microphone cho Windows

Hệ thống gồm 2 app kết hợp: app Android ghi âm từ mic điện thoại và truyền qua mạng LAN/Wi-Fi tới máy Windows, nơi âm thanh được phát ra thiết bị audio ảo để các app như Zoom, Discord, OBS nhận diện như một microphone bình thường.

---

## Kiến trúc

```
[Android: AudioRecord] → [PCM 48kHz 16-bit mono] → [UDP :5005]
        ↓ LAN / Wi-Fi
[Windows: UdpReceiver] → [NAudio BufferedWaveProvider] → [WaveOutEvent]
        ↓
[VB-Audio CABLE Input] → [CABLE Output = Microphone trong Zoom/Discord/OBS]
```

---

## Yêu cầu

### Android
- Android 16 (API 36) trở lên
- Cấp quyền **Microphone** khi app yêu cầu

### Windows
- Windows 10/11
- [.NET 8 Runtime](https://aka.ms/dotnet/download)
- [VB-Audio Virtual Cable](https://vb-audio.com/Cable/) — **cài thủ công trước khi chạy app**

---

## Cài đặt

### 1. Cài VB-Audio Virtual Cable trên Windows

1. Tải tại https://vb-audio.com/Cable/
2. Giải nén và chạy `VBCABLE_Setup_x64.exe` với quyền Administrator
3. Khởi động lại máy
4. Vào **Sound Settings** → đảm bảo thấy thiết bị **CABLE Input** và **CABLE Output**

### 2. Cấu hình Firewall Windows

Cho phép UDP port **5005** (audio stream) và **5006** (bandwidth test):

```powershell
New-NetFirewallRule -DisplayName "PhoneMic Audio" -Direction Inbound -Protocol UDP -LocalPort 5005 -Action Allow
New-NetFirewallRule -DisplayName "PhoneMic Bandwidth" -Direction Inbound -Protocol UDP -LocalPort 5006 -Action Allow
```

### 3. Build Android app

1. Mở thư mục `android-app/` trong Android Studio
2. Kết nối điện thoại Android 16+ qua USB
3. Nhấn **Run**

### 4. Build Windows app

```powershell
cd win-app
dotnet build
dotnet run
```

---

## Sử dụng

### Stream microphone

1. Mở **Phone Mic** trên Windows → ghi lại địa chỉ IP hiển thị (VD: `192.168.1.100`)
2. Mở app trên điện thoại → nhập IP Windows → nhấn **Start Stream**
3. Windows app tự chọn **CABLE Input** làm output device
4. Vào Zoom/Discord/OBS → chọn **CABLE Output** làm microphone

### Test băng thông

Dùng để kiểm tra chất lượng mạng LAN trước khi stream:

**Bước 1** — Chạy echo server trên Windows:
```powershell
cd win-app
.\udp-echo-server.ps1
```

**Bước 2** — Trên điện thoại: nhập IP → nhấn **Test Băng Thông** → nhấn **Bắt đầu đo**

Kết quả hiển thị real-time:

| Metric | Mô tả | Xuất sắc | Tốt | Trung bình | Kém |
|--------|--------|----------|-----|------------|-----|
| **Ping** | Round-trip time | < 5ms | < 20ms | < 50ms | ≥ 50ms |
| **Jitter** | Độ dao động RTT | < 2ms | < 5ms | < 15ms | ≥ 15ms |
| **Loss** | Tỉ lệ mất gói | < 0.5% | < 1% | < 5% | ≥ 5% |
| **TX** | Throughput gửi | > 5 Mbps | > 2 Mbps | > 0.5 Mbps | < 0.5 Mbps |

### Ghi test âm thanh

Nhấn **Record WAV (10s)** → nói vào mic → file lưu tại:
```
/sdcard/Android/data/com.project.phonemic/files/test_audio.wav
```

Pull về PC để kiểm tra:
```bash
adb pull /sdcard/Android/data/com.project.phonemic/files/test_audio.wav .
```

---

## Cấu trúc dự án

```
phone_mic/
├── android-app/                    # Android Studio project (Kotlin)
│   └── app/src/main/java/.../
│       ├── MainActivity.kt         # UI chính, lịch sử IP
│       ├── AudioCaptureService.kt  # Foreground Service ghi âm
│       ├── UdpStreamer.kt          # Gửi PCM qua UDP
│       ├── WavRecorder.kt          # Ghi file WAV để test
│       ├── BandwidthTester.kt      # Đo RTT/loss/jitter/throughput
│       ├── BandwidthProbe.kt       # Packet format cho bandwidth test
│       ├── BandwidthGraphView.kt   # Canvas graph real-time
│       ├── BandwidthActivity.kt    # Màn hình test băng thông
│       └── IpHistoryStore.kt       # Lưu lịch sử IP đã kết nối
│
├── win-app/                        # C# .NET 8 WinForms
│   ├── Program.cs
│   ├── MainForm.cs                 # UI: chọn device, start/stop, VU meter
│   ├── UdpReceiver.cs              # Nhận UDP port 5005
│   ├── AudioPlayer.cs              # NAudio → CABLE Input
│   └── udp-echo-server.ps1         # Echo server cho bandwidth test
│
└── docs/
    └── README.md
```

---

## Thông số kỹ thuật

| Thông số | Giá trị |
|----------|---------|
| Sample rate | 48000 Hz |
| Bit depth | 16-bit PCM |
| Channels | Mono |
| Packet size | 20ms = 1920 bytes |
| Audio port | UDP 5005 |
| Bandwidth test port | UDP 5006 |
| Buffer jitter | ~80–120ms |

---

## Lưu ý

- **VB-Audio Cable phải được cài trước** khi chạy Windows app — app sẽ báo lỗi nếu không tìm thấy thiết bị "CABLE Input"
- **Firewall Windows** có thể chặn UDP lần đầu — chạy lệnh PowerShell ở trên để mở port
- **Android OS** có thể kill Foreground Service nếu pin yếu — app dùng đúng `foregroundServiceType="microphone"` để tránh bị kill
- **Wi-Fi yếu** gây mất gói → âm thanh bị ngắt quãng ngắn, đây là bình thường; jitter buffer 80–120ms giúp giảm thiểu
- Kết nối **USB Tethering** cho độ trễ thấp hơn và ổn định hơn Wi-Fi (Phase 5)
