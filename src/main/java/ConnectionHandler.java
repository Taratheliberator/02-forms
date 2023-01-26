import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionHandler {
    private final Socket socket;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers;
    private final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
    private final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
    private final int limit = 4096;
    private final Request request ;

    public ConnectionHandler(Socket socket,
                             ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers) {
        this.socket = socket;
        this.handlers = handlers;
        request = new Request();
    }

    public void handle() {

            try {
                final var in = new BufferedInputStream(socket.getInputStream());
                final var out = new BufferedOutputStream(socket.getOutputStream());

                handleRequest(in, out);

            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    private void handleRequest(BufferedInputStream in, BufferedOutputStream out) throws Exception {

        // лимит на request line + заголовки
        in.mark(limit);
        byte[] buffer = new byte[limit];
        int read = in.read(buffer);

        // ищем request line
        int headersStart = handleRequestLine(buffer, requestLineDelimiter, read, out);
        if (headersStart == -1) return;
        if (!request.getMethod().equals("GET")) {
            handleRequestBody(in);
        }

        handleHeaders(buffer, read, headersStart, in, out);

        if (!handleHeaders(buffer, read, headersStart, in, out)) return;

        if (!handlers.containsKey(request.getMethod())) {
            send404NotFound(out);
            return;
        }

        var pathHandlers = handlers.get(request.getMethod());

        if (!pathHandlers.containsKey(request.getPath())) {
            send404NotFound(out);
            return;
        }
        var handler = pathHandlers.get(request.getPath());
        try {
            handler.handle(request, out);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            send500ServerError(out);
        }
    }

    private int handleRequestLine(byte[] buffer, byte[] requestLineDelimiter, int read,
                                  BufferedOutputStream out) throws Exception {

        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            badRequest(out);
        }

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            badRequest(out);
        }

        String method = requestLine[0];
        if (!handlers.containsKey(method)) {
            badRequest(out);
            return -1;
        }

        String uri = requestLine[1];
        if (!uri.startsWith("/")) {
            badRequest(out);
            return -1;
        }

        String path;
        if (uri.contains("?")) {
            path = uri.substring(0, uri.indexOf('?'));
            List<NameValuePair> queryParams = URLEncodedUtils.parse(new URI(uri), Charset.defaultCharset());
            request.setQueryParams(queryParams);
            request.getQueryParams();
        } else {
            path = uri;
        }

        request.setMethod(method);
        System.out.println("Method:  " + method);
        request.setPath(path);
        System.out.println("Path: " + path);

        return requestLineEnd + requestLineDelimiter.length;
    }

    private boolean handleHeaders(byte[]buffer, int read, int headersStart,
                               BufferedInputStream in, BufferedOutputStream out) throws IOException {

        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            badRequest(out);
            return false;
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        request.setHeaders(headers);
        return true;
    }

    private void handleRequestBody(BufferedInputStream in) throws IOException {
        in.skip(headersDelimiter.length);

        // вычитываем Content-Length, чтобы прочитать body
        final var contentLength = extractHeader(request.getHeaders(), "Content-Length");
        if (contentLength.isPresent()) {
            final var length = Integer.parseInt(contentLength.get());
            final var bodyBytes = in.readNBytes(length);
            String body = new String(bodyBytes);
            request.setBody(body);
            System.out.println("Body: " + body);
        }
    }

    private void send500ServerError(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 500 Server Error\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private void send404NotFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
}