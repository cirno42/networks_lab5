import socks5proxy.Proxy;

public class Main {
    private static final int DEFAULT_PROXY_PORT = 1080;
    public static void main(String[] args) throws Exception {
        int port;
        if (args.length == 0) {
            System.out.println("Use port as an argument, otherwise it will start on " + DEFAULT_PROXY_PORT);
            port = DEFAULT_PROXY_PORT;
        } else {
            port = Integer.parseInt(args[0]);
        }
        System.out.println("Proxy's starting on port " + port);
        var proxy = new Proxy(port);
        proxy.start();
    }
}
