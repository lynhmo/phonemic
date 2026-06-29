---
name: phone-mic-plan
description: Use when working on the phone-mic project. Contains full architecture, phase-by-phase todos, tech stack, and important constraints for the Android→Windows microphone bridge project.
---

# Phone Mic — Project Plan & Todos

Biến điện thoại Android thành Microphone cho Windows qua UDP/LAN.

## Architecture

```
[Android: AudioRecord] → [PCM 48kHz 16-bit mono] → [UDP :5005]
    → [Windows: UdpReceiver] → [BufferedWaveProvider] → [NAudio WaveOutEvent]
    → [VB-Audio CABLE Input] → [CABLE Output = Mic in Zoom/Discord/OBS]
```

## Tech Stack

| Component | Tech |
|-----------|------|
| Android | Kotlin, AudioRecord, Foreground Service (type: microphone) |
| Transport | UDP, raw PCM, 20ms packets (1920 bytes each) |
| Windows | C# .NET 8, WinForms, NAudio |
| Audio routing | VB-Audio Virtual Cable (free, manual install) |

## Audio Parameters (fixed across both sides)

- Sample rate: **48000 Hz**
- Bit depth: **16-bit PCM**
- Channels: **mono**
- Packet size: **20ms = 1920 bytes**
- UDP port: **5005**

---

## Phase Todos

### Phase 0 — Project Init
- [ ] Android: Kotlin project builds (Hello World)
- [ ] Windows: C# .NET 8 WinForms builds (Hello World)
- [ ] Windows: NAudio added via NuGet
- [ ] README with VB-Audio Cable install guide written

### Phase 1 — Android Audio Capture
- [ ] RECORD_AUDIO declared in Manifest + runtime permission
- [ ] Foreground Service with `foregroundServiceType="microphone"`
- [ ] AudioRecord loop: 48kHz, 16-bit, mono, VOICE_COMMUNICATION source
- [ ] Record to .wav file — verify audio quality before network

### Phase 2 — Windows UDP Receiver
- [ ] UdpReceiver listens on port 5005
- [ ] Logs bytes/sec received to console
- [ ] Windows Firewall allows port 5005 UDP (documented in README)

### Phase 3 — End-to-End (MVP)
- [ ] Android sends PCM buffers via UDP to user-entered IP:5005
- [ ] Windows plays to "CABLE Input" via NAudio WaveOutEvent
- [ ] Discord/Zoom detects "CABLE Output" as microphone
- [ ] Voice heard end-to-end with acceptable latency

### Phase 4 — Stability & UX
- [ ] Android: IP input UI, start/stop button, status indicator
- [ ] Windows: device dropdown, VU meter, listening IP display
- [ ] Packet loss → insert silence (no crash)
- [ ] Reconnect on network change or OS service kill
- [ ] Jitter buffer 80–120ms

### Phase 5 — Advanced (Optional)
- [ ] Opus encoding/decoding (concentus)
- [ ] USB Tethering via ADB reverse
- [ ] NoiseSuppressor / AutomaticGainControl
- [ ] Auto-discovery via UDP broadcast

---

## MVP Done Definition (Phase 0–3)

- [ ] Android records & streams PCM over UDP
- [ ] Windows receives & plays to CABLE Input
- [ ] Voice app on Windows uses CABLE Output as mic and hears phone audio
- [ ] README covers: VB-Cable install, build steps, firewall config

---

## Constraints & Risks

| Risk | Mitigation |
|------|-----------|
| Android 12+ kills background service | `foregroundServiceType="microphone"` mandatory |
| Cannot create Windows virtual mic driver | Use VB-Audio Cable (pre-installed by user) |
| UDP packet loss on weak WiFi | Jitter buffer + silence insertion |
| Windows Firewall blocks UDP 5005 | Document in README, show error in UI |
| VB-Audio Cable not installed | Detect by device name, log clear error |

## Debug Checklist

1. Check Windows Volume Mixer — is CABLE Input receiving signal?
2. Check Sound Settings — is CABLE Output set as default mic?
3. Log all NAudio output devices on startup to find correct index
4. Test UDP connection with a simple sender before audio
