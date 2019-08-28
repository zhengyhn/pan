package io.github.zhengyhn.pan;

import picocli.CommandLine;

@CommandLine.Command(
        name = "pan-cli",
        description = "Command line client for pan server"
)
public class PanClientCommand implements Runnable {
    @CommandLine.Option(names = { "-h", "--host" }, description = "Server host", required = true)
    private String host;
    @CommandLine.Option(names = { "-p", "--port" }, description = "Server port", required = true)
    private Integer port;

    @Override
    public void run() {
        ClientOption option = ClientOption.builder()
                .host(host).port(port).build();
        new PanClient(option).execute();
    }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new PanClientCommand());
        int exitCode = cmd.execute(args);
        System.exit(exitCode);
    }
}
