package ru.yandex.practicum.catsgram.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.catsgram.enums.SortOrder;
import ru.yandex.practicum.catsgram.exception.ConditionsNotMetException;
import ru.yandex.practicum.catsgram.exception.NotFoundException;
import ru.yandex.practicum.catsgram.exception.ParameterNotValidException;
import ru.yandex.practicum.catsgram.model.Post;
import ru.yandex.practicum.catsgram.model.User;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostService {
    private final Map<Long, Post> posts = new HashMap<>();
    private final UserService userService;

    @Autowired
    public PostService(UserService userService) {
        this.userService = userService;
    }

    public Collection<Post> findAll(String sort, Long size, Long from) {
        SortOrder order = SortOrder.from(sort);
        if (order == null) {
            throw new ParameterNotValidException("sort", "Получено: " + sort + " должно быть: asc или desc");
        }
        if (size < 0) {
            throw new ParameterNotValidException(size.toString(), "Некорректный размер выборки. Размер должен быть больше нуля");
        }

        if (from < 0)
            throw new ParameterNotValidException(from.toString(), "Некорректный размер выборки. Размер должен быть больше нуля");

        return posts.values().stream()
                .sorted((p1, p2) -> {
                    int comp = p1.getPostDate().compareTo(p2.getPostDate());
                    if (order.equals(SortOrder.DESCENDING)) {
                        comp = -1 * comp;
                    }
                    return comp;
                })
                .skip(from).limit(size).collect(Collectors.toList());
    }

    public Post getPostById(String id) {
        Optional<Post> postOptional;
        try {
            postOptional = Optional.ofNullable(posts.get(Long.parseLong(id)));
        } catch (NumberFormatException ex) {
            throw new ConditionsNotMetException("Некорректный идентификатор поста");
        }
        return postOptional
                .orElseThrow(() -> new NotFoundException(String.format("Пост с id = %s не найден", id)));
    }

    public Post create(Post post) {
        Optional<User> userOptional = userService.findUserById(post.getAuthorId());
        if (userOptional.isEmpty()) {
            throw new ConditionsNotMetException(String.format("Автор с id = %d не найден", post.getAuthorId()));
        }
        if (post.getDescription() == null || post.getDescription().isBlank()) {
            throw new ConditionsNotMetException("Описание не может быть пустым");
        }
        post.setId(getNextId());
        post.setPostDate(Instant.now());
        posts.put(post.getId(), post);
        return post;
    }

    public Post update(Post newPost) {
        if (newPost.getId() == null) {
            throw new ConditionsNotMetException("Id должен быть указан");
        }
        if (posts.containsKey(newPost.getId())) {
            Post oldPost = posts.get(newPost.getId());
            if (newPost.getDescription() == null || newPost.getDescription().isBlank()) {
                throw new ConditionsNotMetException("Описание не может быть пустым");
            }
            oldPost.setDescription(newPost.getDescription());
            return oldPost;
        }
        throw new NotFoundException("Пост с id = " + newPost.getId() + " не найден");
    }

    private long getNextId() {
        long currentMaxId = posts.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}