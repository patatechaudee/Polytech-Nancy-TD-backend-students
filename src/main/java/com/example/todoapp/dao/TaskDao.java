package com.example.todoapp.dao;

import com.example.todoapp.business.model.Task;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link Task} model using SQLite.
 */
public class TaskDao {

    // URL de connexion vers le fichier de base de données local (app.db)
    private static final String DB_URL = "jdbc:sqlite:app.db";

    public TaskDao() {
        // Initialisation : création de la table si elle n'existe pas encore
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS task (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                done INTEGER NOT NULL
            );
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);

            // Initialisation des données de test si la base est vide
            if (findAll().isEmpty()) {
                save(new Task(null, "Réviser DS de maths", "Séries numériques et probabilités.", false));
                save(new Task(null, "Valider mon PIVE", "PIVE Club Poker.", true));
                save(new Task(null, "Choisir mon parcours de 4A", "SIR ou SIA ?", false));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Impossible d'initialiser la base de données SQLite", e);
        }
    }

    public Task save(Task task) {
        String sql = "INSERT INTO task (title, description, done) VALUES (?, ?, ?)";

        // Utilisation du bloc try-with-resources pour libérer les ressources automatiquement
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) { // Récupère l'ID auto-généré

            ps.setString(1, task.title()); //
            ps.setString(2, task.description()); //
            ps.setInt(3, task.done() ? 1 : 0);   // SQLite n'a pas de type BOOLEAN, on utilise 0 ou 1
            ps.executeUpdate(); //

            // Récupération de la clé générée par SQLite
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new Task(generatedKeys.getInt(1), task.title(), task.description(), task.done());
                }
            }
            return task;
        } catch (SQLException e) {
            throw new RuntimeException("Échec de l'insertion de la tâche", e); //
        }
    }

    public Optional<Task> findById(int id) {
        String sql = "SELECT id, title, description, done FROM task WHERE id = ?"; //

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) { //

            ps.setInt(1, id); //
            try (ResultSet rs = ps.executeQuery()) { //
                if (rs.next()) { //
                    return Optional.of(mapRowToTask(rs)); //
                }
            }
            return Optional.empty(); //
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération de la tâche " + id, e); //
        }
    }

    // Récupérer toutes les tâches
    public List<Task> findAll() {
        String sql = "SELECT id, title, description, done FROM task";
        List<Task> tasks = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tasks.add(mapRowToTask(rs));
            }
            return tasks;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des tâches", e);
        }
    }

    // Récupérer seulement les tâches à faire (done = false / 0)
    public List<Task> findAllTodo() {
        String sql = "SELECT id, title, description, done FROM task WHERE done = 0";
        List<Task> tasks = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tasks.add(mapRowToTask(rs));
            }
            return tasks;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des tâches à faire", e);
        }
    }

    // Supprimer une tâche
    public boolean deleteById(int id) {
        String sql = "DELETE FROM task WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0; // Renvoie true si une ligne a été supprimée
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de la tâche " + id, e);
        }
    }

    // Mettre à jour une tâche existante
    public boolean update(int id, Task task) {
        String sql = "UPDATE task SET title = ?, description = ?, done = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, task.title());
            ps.setString(2, task.description());
            ps.setInt(3, task.done() ? 1 : 0);
            ps.setInt(4, id);

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0; // Renvoie true si la tâche existait et a été mise à jour
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la tâche " + id, e);
        }
    }

    // Méthode d'aide privée pour transformer un tuple de la base de données en objet Java Task
    private Task mapRowToTask(ResultSet rs) throws SQLException {
        return new Task(
                rs.getInt("id"),         //
                rs.getString("title"),   //
                rs.getString("description"), //
                rs.getInt("done") == 1   // Traduit la valeur 1 en true, sinon false
        );
    }
}