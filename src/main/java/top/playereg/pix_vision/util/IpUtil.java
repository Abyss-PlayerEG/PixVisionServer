package top.playereg.pix_vision.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class IpUtil {

    /**
     * 获取所有本地IP地址
     *
     * @return 所有本地IP地址
     * @author PlayerEG
     */
    public static List<String> getAllLocalIpAddresses() {
        List<String> ipAddresses = new ArrayList<>();
        try {
            // 尝试获取所有网络接口
            Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();

                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses =
                        networkInterface.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();

                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        String ip = address.getHostAddress();
                        // 排除 docker 等虚拟网卡
                        if (!ip.startsWith("172.") && !ip.startsWith("192.168.")) {
                            continue;
                        }
                        if (!ipAddresses.contains(ip)) {  // 避免重复添加
                            ipAddresses.add(ip);
                        }
                    }
                }
            }

            // 获取公网IP
            String publicIp = getPublicIpAddress();
            if (publicIp != null && !publicIp.isEmpty() && !ipAddresses.contains(publicIp)) {
                ipAddresses.add(publicIp);
            }

            // 如果没有找到合适的IP，尝试连接外部地址来获取本地IP
            if (ipAddresses.isEmpty()) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("8.8.8.8", 53));
                    String ip = socket.getLocalAddress().getHostAddress();
                    if (!ipAddresses.contains(ip)) {
                        ipAddresses.add(ip);
                    }
                } catch (Exception ignored) {
                    ipAddresses.add("127.0.0.1");
                }
            }

            // 如果仍然没有找到任何IP，则添加回环地址
            if (ipAddresses.isEmpty()) {
                ipAddresses.add("127.0.0.1");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 发生异常时至少添加回环地址
            ipAddresses.add("127.0.0.1");
        }

        return ipAddresses;
    }

    /**
     * 获取公网IP地址
     *
     * @return 公网IP地址，如果没有找到，则返回null
     * @author PlayerEG
     */
    public static String getPublicIpAddress() {
        // 常用的获取公网IP的API列表
        String[] ipApiUrls = {
                "https://checkip.amazonaws.com",  // AWS提供的服务，通常更可靠
                "https://api.ipify.org",          // 可靠性较高
                "https://icanhazip.com",          // 可靠性中等
                "https://ident.me",               // 可靠性中等
                "https://ipecho.net/plain"        // 可能不稳定
        };

        for (String apiUrl : ipApiUrls) {
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000); // 3秒超时
                connection.setReadTimeout(3000); // 3秒超时

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                String ip = reader.readLine();
                reader.close();

                if (ip != null && !ip.trim().isEmpty()) {
                    ip = ip.trim();
                    // 验证是否为有效的IP地址格式
                    if (isValidIpAddress(ip)) {
                        return ip;
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }

        return null; // 所有API都失败
    }

    /**
     * 验证IP地址格式
     *
     * @param ipAddress IP地址
     * @return true表示格式正确，false表示格式错误
     * @author PlayerEG
     */
    public static boolean isValidIpAddress(String ipAddress) {
        // 验证IP地址格式的正则表达式
        String ipPattern = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ipAddress != null && ipAddress.matches(ipPattern);
    }
}