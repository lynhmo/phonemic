using NAudio.Wave;

namespace PhoneMic;

class AudioPlayer : IDisposable
{
    private WaveOutEvent? _waveOut;
    private BufferedWaveProvider? _buffer;
    private readonly WaveFormat _format = new(48000, 16, 1);

    public static List<string> GetDevices()
    {
        var list = new List<string>();
        for (int i = 0; i < WaveOut.DeviceCount; i++)
            list.Add(WaveOut.GetCapabilities(i).ProductName);
        return list;
    }

    public void Start(int deviceIndex)
    {
        _buffer = new BufferedWaveProvider(_format)
        {
            BufferDuration = TimeSpan.FromMilliseconds(500),
            DiscardOnBufferOverflow = true
        };
        _waveOut = new WaveOutEvent { DeviceNumber = deviceIndex, DesiredLatency = 80 };
        _waveOut.Init(_buffer);
        _waveOut.Play();
    }

    public void Feed(byte[] data) => _buffer?.AddSamples(data, 0, data.Length);

    public void Stop()
    {
        _waveOut?.Stop();
        _waveOut?.Dispose();
        _waveOut = null;
        _buffer = null;
    }

    public void Dispose() => Stop();
}
