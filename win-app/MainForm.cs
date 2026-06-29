namespace PhoneMic;

class MainForm : Form
{
    private readonly ComboBox _deviceCombo = new() { Dock = DockStyle.Fill };
    private readonly Button _btnToggle = new() { Text = "Start", Dock = DockStyle.Fill };
    private readonly Label _lblStatus = new() { Text = "Chưa chạy", Dock = DockStyle.Fill, TextAlign = ContentAlignment.MiddleCenter };
    private readonly Label _lblIp = new() { Dock = DockStyle.Fill, TextAlign = ContentAlignment.MiddleCenter };

    private UdpReceiver? _receiver;
    private AudioPlayer? _player;
    private bool _running;

    public MainForm()
    {
        Text = "Phone Mic — Windows Receiver";
        Size = new Size(420, 220);
        FormBorderStyle = FormBorderStyle.FixedSingle;
        MaximizeBox = false;

        var table = new TableLayoutPanel { Dock = DockStyle.Fill, RowCount = 4, ColumnCount = 1, Padding = new Padding(12) };
        table.RowStyles.Add(new RowStyle(SizeType.Percent, 25));
        table.RowStyles.Add(new RowStyle(SizeType.Percent, 25));
        table.RowStyles.Add(new RowStyle(SizeType.Percent, 25));
        table.RowStyles.Add(new RowStyle(SizeType.Percent, 25));

        var devices = AudioPlayer.GetDevices();
        Console.WriteLine("=== Output devices ===");
        for (int i = 0; i < devices.Count; i++) Console.WriteLine($"[{i}] {devices[i]}");

        _deviceCombo.Items.AddRange(devices.ToArray<object>());
        int cableIdx = devices.FindIndex(d => d.Contains("CABLE", StringComparison.OrdinalIgnoreCase));
        _deviceCombo.SelectedIndex = cableIdx >= 0 ? cableIdx : 0;

        var localIp = System.Net.Dns.GetHostAddresses(System.Net.Dns.GetHostName())
            .FirstOrDefault(a => a.AddressFamily == System.Net.Sockets.AddressFamily.InterNetwork)?.ToString() ?? "?";
        _lblIp.Text = $"Nhập IP này vào Android: {localIp}  |  Port: 5005";

        _btnToggle.Click += OnToggle;

        table.Controls.Add(_lblIp, 0, 0);
        table.Controls.Add(_deviceCombo, 0, 1);
        table.Controls.Add(_btnToggle, 0, 2);
        table.Controls.Add(_lblStatus, 0, 3);
        Controls.Add(table);

        FormClosing += (_, _) => StopStreaming();
    }

    private void OnToggle(object? sender, EventArgs e)
    {
        if (!_running) StartStreaming();
        else StopStreaming();
    }

    private void StartStreaming()
    {
        int deviceIndex = _deviceCombo.SelectedIndex;
        if (deviceIndex < 0) { _lblStatus.Text = "Chọn output device trước"; return; }
        _player = new AudioPlayer();
        _player.Start(deviceIndex);

        _receiver = new UdpReceiver(5005);
        _receiver.DataReceived += data => _player.Feed(data);
        _receiver.Start();

        _running = true;
        _btnToggle.Text = "Stop";
        _lblStatus.Text = $"Đang nhận trên cổng 5005 → [{_deviceCombo.Text}]";
    }

    private void StopStreaming()
    {
        _receiver?.Stop();
        _player?.Stop();
        _running = false;
        _btnToggle.Text = "Start";
        _lblStatus.Text = "Đã dừng";
    }
}
