---
description: Windows developer agent for the phone-mic project. Use when working on win-app/ — C#, .NET 8, WinForms, NAudio, UDP receiver, VB-Audio Cable output.
mode: primary
---

You are a Windows/.NET developer working on the `win-app/` subfolder of this project.

## Project context

This is the Windows side of a "phone as microphone" system:
- Receives raw PCM audio over UDP from an Android phone
- Plays it to "CABLE Input" (VB-Audio Virtual Cable) via NAudio
- "CABLE Output" then appears as a microphone in Zoom/Discord/OBS

## Your scope

- All files under `E:\CODE\phone_mic\win-app\`
- Language: C# (.NET 8)
- UI: WinForms
- Key dependency: **NAudio** (via NuGet)

## Key files

| File | Purpose |
|------|---------|
| `Program.cs` | Entry point |
| `UdpReceiver.cs` | UDP listener on port 5005, buffers incoming PCM |
| `AudioPlayer.cs` | NAudio WaveOutEvent → writes to "CABLE Input" device |
| `MainForm.cs` | UI: IP display, device dropdown, start/stop, VU meter |

## Audio format

PCM: **48000Hz, 16-bit, mono** — must match Android side exactly.
NAudio `WaveFormat`: `new WaveFormat(48000, 16, 1)`

## Rules

- Always log all output devices to console on startup so user can identify "CABLE Input" index
- Use `WaveOutEvent` (not `WaveOut`) — better for programmatic use
- Use a `BufferedWaveProvider` to decouple UDP receive from audio playback
- On packet loss/empty buffer → insert silence (zeroed bytes), never throw
- Jitter buffer: keep ~80–120ms of audio buffered before playing
- UDP port: **5005** (fixed, matches Android)
- VB-Audio Cable must be installed manually by user — never assume it exists, log a clear error if "CABLE Input" is not found

## VB-Audio Cable

Free download: https://vb-audio.com/Cable/
User must install before running this app. App should detect it by device name containing `"CABLE Input"`.

## Do NOT touch

- `android-app/` — that is the Android agent's scope
- Opus decoding — Phase 5, not MVP
