import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocket serverSocket;
    private ExecutorService service;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers = new ConcurrentHashMap<>();


    public void addHandler(String method, String path, Handler handler) {
        if (!handlers.containsKey(method)){
            handlers.put(method, new ConcurrentHashMap<>());
        }
        handlers.get(method).put(path, handler);

    }

    public void listen(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started");

        } catch (IOException e) {
            e.printStackTrace();
        }
        int THREADS_QUANTITY = 64;
        service = Executors.newFixedThreadPool(THREADS_QUANTITY);
        listenConnection();
    }

    public void listenConnection() {
        while(true) {

            try {
                final var socket = serverSocket.accept();

                service.submit(() -> new ConnectionHandler(socket, handlers).handle());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
