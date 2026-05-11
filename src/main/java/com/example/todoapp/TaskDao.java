package com.example.todoapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Data Access Object for {@link Task} model.
 */
public class TaskDao {

    private final Map<Integer, Task> storage = new HashMap<>();

    {
        save(new Task(1, "Réviser DS de maths", "Séries numériques et probabilités.", false));
        save(new Task(2, "Valider mon PIVE", "PIVE Club Poker.", true));
        save(new Task(3, "Choisir mon parcours de 4A", "SIR ou SIA ?", false));
    }

    public Task save(Task task) {
        storage.put(task.id(), task);
        return task;
    }

    public Optional<Task> findById(int id) {
        return Optional.ofNullable(storage.get(id));
    }

    // Récupérer toutes les tâches
    public List<Task> findAll() {
        return new ArrayList<>(storage.values());
    }

    // Récupérer seulement les tâches à faire (done = false)
    public List<Task> findAllTodo() {
        return storage.values().stream()
                .filter(task -> !task.done())
                .collect(Collectors.toList());
    }

    // Supprimer une tâche
    public boolean deleteById(int id) {
        return storage.remove(id) != null;
    }

    // Mettre à jour une tâche existante
    public boolean update(int id, Task task) {
        if (storage.containsKey(id)) {
            // On crée une nouvelle instance pour s'assurer que l'ID est celui du path
            Task updatedTask = new Task(id, task.title(), task.description(), task.done());
            storage.put(id, updatedTask);
            return true;
        }
        return false;
    }
}