# UDP Echo Server cho bandwidth test
# Lắng nghe port 5006, echo lại mọi packet về đúng sender
# Chạy: .\udp-echo-server.ps1

$port = 5006
$udp = [System.Net.Sockets.UdpClient]::new($port)
$endpoint = [System.Net.IPEndPoint]::new([System.Net.IPAddress]::Any, $port)

Write-Host "UDP Echo Server dang lang nghe tren port $port..."
Write-Host "Nhan Ctrl+C de dung`n"

$packets = 0
$sw = [System.Diagnostics.Stopwatch]::StartNew()

try {
    while ($true) {
        # Nhan packet
        $data = $udp.Receive([ref]$endpoint)

        # Echo nguyen ban ve dung sender
        $udp.Send($data, $data.Length, $endpoint) | Out-Null
        $packets++

        # Log moi giay
        if ($sw.ElapsedMilliseconds -ge 1000) {
            Write-Host "[$([datetime]::Now.ToString('HH:mm:ss'))] $packets packets/s tu $($endpoint.Address):$($endpoint.Port)"
            $packets = 0
            $sw.Restart()
        }
    }
}
finally {
    $udp.Close()
    Write-Host "Server da dung."
}
