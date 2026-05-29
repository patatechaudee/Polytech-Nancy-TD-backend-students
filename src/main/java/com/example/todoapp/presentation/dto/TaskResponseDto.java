package com.example.todoapp.presentation.dto;

/**
 * DTO retourné par l'ensemble des endpoints de consultation.
 */
public record TaskResponseDto(Integer id, String title, String description, boolean done) {
}
