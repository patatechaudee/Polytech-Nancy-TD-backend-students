package com.example.todoapp.business.service;

import com.example.todoapp.business.model.Task;
import com.example.todoapp.dao.TaskDao;

import java.util.List;
import java.util.Optional;

/**
 * Service Layer (Business Logic) orchestrating transactions and domain logic.
 */
public class TaskService {

    private final TaskDao dao;

    public TaskService(TaskDao dao) {
        this.dao = dao;
    }

    public List<Task> getTasks(boolean todoOnly) {
        return todoOnly ? dao.findAllTodo() : dao.findAll();
    }

    public Optional<Task> getTaskById(int id) {
        return dao.findById(id);
    }

    public Task createTask(Task task) {
        // C'est ici qu'on ajouterait d'éventuelles validations ou règles métiers
        return dao.save(task);
    }

    public boolean deleteTask(int id) {
        return dao.deleteById(id);
    }

    public boolean updateTask(int id, Task task) {
        return dao.update(id, task);
    }
}
