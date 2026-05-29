package com.example.todoapp.presentation.dto;

/**
 * DTO pour la création d'une tâche (POST /tasks).
 */
public record TaskCreateDto(String title, String description) {
}
