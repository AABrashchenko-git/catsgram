package ru.yandex.practicum.catsgram.controller;

import com.sun.jdi.request.DuplicateRequestException;
import org.springframework.web.bind.annotation.*;

import ru.yandex.practicum.catsgram.exception.ConditionsNotMetException;
import ru.yandex.practicum.catsgram.exception.DuplicatedDataException;
import ru.yandex.practicum.catsgram.exception.NotFoundException;
import ru.yandex.practicum.catsgram.model.User;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Long, User> users = new HashMap<>();

    @GetMapping
    public Collection<User> findAll() {
        return users.values();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ConditionsNotMetException("Имейл должен быть указан");
        }
        if (emailExists(user)) {
            throw new DuplicatedDataException("Этот имейл уже используется");
        }
        user.setId(getNextId());
        user.setRegistrationDate(Instant.now());
        users.put(user.getId(), user);
        return user;
    }

    @PutMapping
    public User update(@RequestBody User newUser) {
        if (newUser.getId() == null) {
            throw new ConditionsNotMetException("Id должен быть указан");
        }
        if (users.containsKey(newUser.getId())) {
            if (emailExists(newUser)) {
                throw new DuplicatedDataException("Этот имейл уже используется");
            }
            User oldUser = users.get(newUser.getId());
            if (newUser.getEmail() == null || newUser.getUsername() == null || newUser.getPassword() == null) {
                //throw new ConditionsNotMetException("Адрес электронной почты, имя или пароль не указаны");
                return oldUser;
            }
            oldUser.setEmail(newUser.getEmail());
            oldUser.setUsername(newUser.getUsername());
            oldUser.setPassword(newUser.getPassword());
            return oldUser;
        }
        throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден");
    }

    // вспомогательный метод для генерации идентификатора нового поста
    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    private boolean emailExists(User user) {
        return users.values().stream().anyMatch((u -> u.getEmail().equals(user.getEmail())));
    }


}
