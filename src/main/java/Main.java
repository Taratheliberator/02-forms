import java.io.BufferedOutputStream;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        final var server = new Server();

        // добавление handler'ов (обработчиков)
        server.addHandler("GET", "/messages", (request, out) -> {
            sendResponse(out,"Hello from GET /message");
        });
        server.addHandler("POST", "/messages", (request, out) -> {
            sendResponse(out, "Hello from POST /messages");
        });

        server.listen(9999);
    }
    private static void sendResponse(BufferedOutputStream out, String response) throws IOException {
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Length: " + response.length() + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(response.getBytes());
        out.flush();
    }
}