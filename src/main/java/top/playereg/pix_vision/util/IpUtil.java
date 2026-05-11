package top.playereg.pix_vision.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * IP 地址工具类
 * <p>
 * 提供本地 IP 地址获取、公网 IP 地址查询、IP 格式验证等功能。
 * 支持多网卡环境下的 IP 地址筛选，优先返回局域网 IP。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ol>
 *   <li>应用启动时显示访问地址列表</li>
 *   <li>记录用户操作日志中的 IP 信息</li>
 *   <li>网络诊断和连接测试</li>
 * </ol>
 *
 * <h3>注意事项</h3>
 * <ul>
 *   <li>仅返回 IPv4 地址，过滤回环地址和虚拟网卡</li>
 *   <li>优先返回 172.x 和 192.168.x 开头的局域网 IP</li>
 *   <li>公网 IP 通过外部 API 获取，可能受网络环境影响</li>
 *   <li>多个 API 失败时会返回 null，调用方需处理空值</li>
 * </ul>
 *
 * @author PlayerEG
 * @since DEV-2.0.0
 */
public class IpUtil {

    /**
     * 获取所有本地 IP 地址
     * <p>
     * 遍历所有网络接口，过滤回环地址、虚拟网卡和 Docker 网卡，
     * 优先返回 172.x 和 192.168.x 开头的局域网 IP。
     * 如果未找到合适的 IP，会尝试连接外部地址获取本地 IP。
     * </p>
     *
     * @return 本地 IP 地址列表，至少包含 127.0.0.1
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
     * 获取公网 IP 地址
     * <p>
     * 通过多个外部 API 服务查询公网 IP，按优先级依次尝试：
     * AWS、ipify、icanhazip、ident.me、ipecho.net。
     * 每个 API 超时时间为 3 秒，所有 API 失败时返回 null。
     * </p>
     *
     * @return 公网 IP 地址字符串，如果所有 API 都失败则返回 null
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
     * 验证 IP 地址格式
     * <p>
     * 使用正则表达式验证 IPv4 地址格式的合法性。
     * 支持的标准格式：xxx.xxx.xxx.xxx，其中 xxx 为 0-255 的数字。
     * </p>
     *
     * @param ipAddress IP 地址字符串
     * @return true 表示格式正确，false 表示格式错误
     * @author PlayerEG
     */
    public static boolean isValidIpAddress(String ipAddress) {
        // 验证IP地址格式的正则表达式
        String ipPattern = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ipAddress != null && ipAddress.matches(ipPattern);
    }
}