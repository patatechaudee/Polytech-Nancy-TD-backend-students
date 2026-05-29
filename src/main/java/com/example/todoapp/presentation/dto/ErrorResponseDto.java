package com.example.todoapp.presentation.dto;

/**
 * DTO renvoyé en cas d'erreur fonctionnelle ou de validation (HTTP 400).
 */
public record ErrorResponseDto(String field, String message) {
}