package io.github.zhengyhn.pan;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientOption {
    private String host;
    private Integer port;

    public String getPromptPrefix() {
        return new StringBuilder().append(host).append(":").append(port).append("> ").toString();
    }
}
