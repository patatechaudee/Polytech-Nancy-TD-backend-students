package com.example.todoapp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private static final Pattern ID_PATH = Pattern.compile("^/tasks/([0-9]+)$");
    private static final TaskDao dao = new TaskDao();

    public static void main(String[] args) throws Exception {
        log.info("In-memory repository initialised");

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/tasks", Application::handleTasks);
        server.setExecutor(null);
        server.start();
        log.info("HTTP server started on http://localhost:8080");
    }

    private static void handleTasks(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();


        if ("GET".equals(method) && "/tasks".equals(path)) {
            boolean todoOnly = nonNull(query) && query.contains("todo-only=true");
            List<Task> tasks = todoOnly ? dao.findAllTodo() : dao.findAll();

            if (tasks.isEmpty()) {
                sendResponse(exchange, 204, null);
            } else {
                sendResponse(exchange, 200, JsonUtils.serialize(tasks));
            }
            return;
        }


        if ("POST".equals(method) && "/tasks".equals(path)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
            Task input = JsonUtils.deserialize(body, Task.class);
            Task createdTask = dao.save(input);

            exchange.getResponseHeaders().add("Location", "/tasks/" + createdTask.id());
            sendResponse(exchange, 201, JsonUtils.serialize(createdTask));
            return;
        }


        Matcher m = ID_PATH.matcher(path);
        if (m.matches()) {
            int id = Integer.parseInt(m.group(1));

            switch (method) {
                case "GET":
                    dao.findById(id).ifPresentOrElse(
                            task -> { try { sendResponse(exchange, 200, JsonUtils.serialize(task)); } catch (IOException ignored) {} },
                            () -> { try { sendResponse(exchange, 404, null); } catch (IOException ignored) {} }
                    );
                    break;

                case "DELETE":
                    boolean deleted = dao.deleteById(id);
                    sendResponse(exchange, deleted ? 204 : 404, null);
                    break;

                case "PUT":
                    String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
                    Task input = JsonUtils.deserialize(body, Task.class);
                    boolean updated = dao.update(id, input);
                    sendResponse(exchange, updated ? 204 : 404, null);
                    break;

                default:
                    sendResponse(exchange, 405, null); // Method Not Allowed
                    break;
            }
            return;
        }

        // Sinon 404
        sendResponse(exchange, 404, null);
    }

    private static void sendResponse(HttpExchange exchange, int status, String json) throws IOException {
        if (nonNull(json)) {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = json.getBytes(UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.sendResponseHeaders(status, -1);
        }
    }
}