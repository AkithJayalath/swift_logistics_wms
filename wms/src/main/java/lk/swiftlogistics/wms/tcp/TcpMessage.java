package lk.swiftlogistics.wms.tcp;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TcpMessage {
    private String type;
    private String pkgRef;
    private long timestamp;
    private String data;
    private String secret;   
}
