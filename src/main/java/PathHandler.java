import java.io.File;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PathHandler {
    private File resource;
    private List<String> validPaths = new CopyOnWriteArrayList<>();
    private String directory;

    public PathHandler(String directory) {
        this.directory = directory;
    }

    public boolean requestProcessor(String requestLine) {
        if (validPaths.isEmpty()) {
            readPaths();
        }
        String[] requestParts = requestLine.split(" ");

        final var path = requestParts[1];
        return !validPaths.contains(path);
    }

    private void readPaths() {
        try {
            Files.lines(Path.of(directory), StandardCharsets.UTF_8)
                    .forEach(s -> {
                        String[] parts = s.split(", ");
                        validPaths.addAll(Arrays.asList(parts));
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
