package com.example.todoapp.presentation;

import com.example.todoapp.JsonUtils;
import com.example.todoapp.business.model.Task;
import com.example.todoapp.business.service.TaskService;
import com.example.todoapp.presentation.dto.*;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;

public class TaskController {

    private static final Pattern ID_PATH = Pattern.compile("^/tasks/([0-9]+)$");
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        try {
            if ("GET".equals(method) && "/tasks".equals(path)) {
                boolean todoOnly = nonNull(query) && query.contains("todo-only=true");

                List<Task> domainTasks = service.getTasks(todoOnly);

                if (domainTasks.isEmpty()) {
                    sendResponse(exchange, 204, null);
                } else {
                    List<TaskResponseDto> responseDtos = domainTasks.stream()
                            .map(t -> new TaskResponseDto(t.id(), t.title(), t.description(), t.done()))
                            .collect(Collectors.toList());
                    sendResponse(exchange, 200, JsonUtils.serialize(responseDtos));
                }
                return;
            }

            if ("POST".equals(method) && "/tasks".equals(path)) {
                String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
                TaskCreateDto inputDto = JsonUtils.deserialize(body, TaskCreateDto.class);

                validateCreateDto(inputDto);

                Task domainTask = new Task(null, inputDto.title(), inputDto.description(), false);
                Task createdTask = service.createTask(domainTask);

                TaskResponseDto responseDto = new TaskResponseDto(
                        createdTask.id(), createdTask.title(), createdTask.description(), createdTask.done()
                );

                exchange.getResponseHeaders().add("Location", "/tasks/" + createdTask.id());
                sendResponse(exchange, 201, JsonUtils.serialize(responseDto));
                return;
            }

            // Routes avec identifiant dynamique /tasks/{id}
            Matcher m = ID_PATH.matcher(path);
            if (m.matches()) {
                int id = Integer.parseInt(m.group(1));

                switch (method) {
                    case "GET":
                        service.getTaskById(id).ifPresentOrElse(
                                task -> {
                                    try {
                                        TaskResponseDto dto = new TaskResponseDto(task.id(), task.title(), task.description(), task.done());
                                        sendResponse(exchange, 200, JsonUtils.serialize(dto));
                                    } catch (IOException ignored) {}
                                },
                                () -> { try { sendResponse(exchange, 404, null); } catch (IOException ignored) {} }
                        );
                        break;

                    case "DELETE":
                        boolean deleted = service.deleteTask(id);
                        sendResponse(exchange, deleted ? 204 : 404, null);
                        break;

                    case "PUT":
                        String body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);
                        TaskUpdateDto updateDto = JsonUtils.deserialize(body, TaskUpdateDto.class);

                        validateUpdateDto(updateDto);

                        Task domainTask = new Task(id, updateDto.title(), updateDto.description(), updateDto.done());
                        boolean updated = service.updateTask(id, domainTask);
                        sendResponse(exchange, updated ? 204 : 404, null);
                        break;

                    default:
                        sendResponse(exchange, 405, null);
                        break;
                }
                return;
            }

            sendResponse(exchange, 404, null);

        } catch (ValidationException ex) {
            ErrorResponseDto errorDto = new ErrorResponseDto(ex.getField(), ex.getMessage());
            sendResponse(exchange, 400, JsonUtils.serialize(errorDto));
        } catch (Exception ex) {
            System.err.println("Erreur critique serveur : " + ex.getMessage());
            ex.printStackTrace();
            sendResponse(exchange, 500, null);
        }
    }


    private void validateCreateDto(TaskCreateDto dto) {
        if (dto == null) {
            throw new ValidationException("body", "Le corps de la requête ne peut pas être vide.");
        }
        if (dto.title() == null || dto.title().strip().isEmpty()) {
            throw new ValidationException("title", "Le titre est obligatoire.");
        }
        if (dto.title().length() > 50) {
            throw new ValidationException("title", "La taille maximale du titre est de 50 caractères.");
        }
        if (dto.description() != null && dto.description().length() > 255) {
            throw new ValidationException("description", "La taille maximale de la description est de 255 caractères.");
        }
    }

    private void validateUpdateDto(TaskUpdateDto dto) {
        if (dto == null) {
            throw new ValidationException("body", "Le corps de la requête ne peut pas être vide.");
        }
        if (dto.title() == null || dto.title().strip().isEmpty()) {
            throw new ValidationException("title", "Le titre est obligatoire.");
        }
        if (dto.title().length() > 50) {
            throw new ValidationException("title", "La taille maximale du titre est de 50 caractères.");
        }
        if (dto.description() != null && dto.description().length() > 255) {
            throw new ValidationException("description", "La taille maximale de la description est de 255 caractères.");
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String json) throws IOException {
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