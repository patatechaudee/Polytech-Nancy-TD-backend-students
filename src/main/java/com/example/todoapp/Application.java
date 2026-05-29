package com.example.todoapp;

import com.example.todoapp.dao.TaskDao;
import com.example.todoapp.business.service.TaskService;
import com.example.todoapp.presentation.TaskController;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class Application {

    // 1. On crée d'abord la persistance (DAO)
    private static final TaskDao dao = new TaskDao();

    // 2. On injecte le DAO dans le Service
    private static final TaskService service = new TaskService(dao);

    // 3. On injecte le Service dans le Contrôleur (C'est ici qu'était votre erreur !)
    private static final TaskController controller = new TaskController(service);

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // On redirige la route vers la méthode handle de notre contrôleur instancié
        server.createContext("/tasks", controller::handle);

        server.setExecutor(null);
        server.start();
        System.out.println("HTTP server started on http://localhost:8080");
    }
}