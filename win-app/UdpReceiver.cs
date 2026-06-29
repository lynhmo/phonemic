using System.Net;
using System.Net.Sockets;

namespace PhoneMic;

class UdpReceiver
{
    private UdpClient? _client;
    private readonly int _port;
    public event Action<byte[]>? DataReceived;

    public UdpReceiver(int port) => _port = port;

    public void Start()
    {
        _client = new UdpClient(_port);
        var endpoint = new IPEndPoint(IPAddress.Any, _port);
        Task.Run(() =>
        {
            while (_client != null)
            {
                try
                {
                    var data = _client.Receive(ref endpoint);
                    DataReceived?.Invoke(data);
                }
                catch { break; }
            }
        });
    }

    public void Stop()
    {
        _client?.Close();
        _client = null;
    }
}
