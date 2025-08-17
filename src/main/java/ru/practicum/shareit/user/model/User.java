package ru.practicum.shareit.user.model;

import lombok.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Имя не может быть пустым")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат email")
    @Column(unique = true, nullable = false)
    private String email;
}