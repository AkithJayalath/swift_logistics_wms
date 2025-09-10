package lk.swiftlogistics.wms.tcp;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public class TcpMessageParser {

    // Shared secret known by both client and server
    private static final String SHARED_SECRET = "WMS-TCP-SECRET";

    public static TcpMessage parse(String raw) {
        String[] parts = raw.trim().split("\\|");
        if (parts.length < 6) {
            throw new IllegalArgumentException("Invalid frame: " + raw);
        }

        String type = parts[0];
        String pkgRef = parts[1];
        long timestamp = Long.parseLong(parts[2]);
        String data = parts[3];
        String secret = parts[4];
        long crc = Long.parseLong(parts[5]);

        // check secret
        if (!SHARED_SECRET.equals(secret)) {
            throw new SecurityException("Invalid shared secret");
        }

        // verify CRC
        long calcCrc = crc32(type + "|" + pkgRef + "|" + timestamp + "|" + data + "|" + secret);
        if (crc != calcCrc) {
            throw new IllegalArgumentException("CRC mismatch");
        }

        return new TcpMessage(type, pkgRef, timestamp, data, secret);
    }

    public static long crc32(String input) {
        CRC32 crc = new CRC32();
        crc.update(input.getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }
}
