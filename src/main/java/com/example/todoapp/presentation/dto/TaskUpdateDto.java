package com.example.todoapp.presentation.dto;

/**
 * DTO pour la modification complète d'une tâche (PUT /tasks/{id}).
 */
public record TaskUpdateDto(String title, String description, boolean done) {
}
