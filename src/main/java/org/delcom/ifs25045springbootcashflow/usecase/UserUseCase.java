package org.delcom.ifs25045springbootcashflow.usecase;

import org.delcom.ifs25045springbootcashflow.domain.PasswordHasher;
import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25045springbootcashflow.domain.exception.ForbiddenException;
import org.delcom.ifs25045springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25045springbootcashflow.domain.repository.IUserRepository;
import java.util.Comparator;
import java.util.List;

public class UserUseCase {
    private final IUserRepository users;
    private final PasswordHasher hasher;

    public UserUseCase(IUserRepository users, PasswordHasher hasher) {
        this.users = users;
        this.hasher = hasher;
    }

    /** Hanya ADMIN yang boleh melihat seluruh pengguna. */
    public List<User> listUsers(User actor) {
        if (actor == null || !actor.isAdmin()) {
            throw new ForbiddenException("Hanya admin yang dapat melihat daftar pengguna");
        }
        return users.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    public User getUser(Integer id) {
        InputRules.requireValidId(id);
        return users.findById(id).orElseThrow(() -> new EntityNotFoundException("Pengguna", id));
    }

    public User updateProfile(Integer userId, String name) {
        User user = getUser(userId);
        user.setName(InputRules.requireName(name));
        return users.save(user);
    }

    public User updatePassword(Integer userId, String currentPassword, String newPassword) {
        User user = getUser(userId);
        if (!hasher.matches(currentPassword, user.getPasswordHash())) {
            throw new ValidationException("Password saat ini salah");
        }
        InputRules.requirePassword(newPassword);
        user.setPasswordHash(hasher.hash(newPassword));
        return users.save(user);
    }

    public User updatePhoto(Integer userId, String photoPath) {
        if (photoPath == null || photoPath.isBlank()) {
            throw new ValidationException("Path foto tidak boleh kosong");
        }
        User user = getUser(userId);
        user.setPhotoPath(photoPath);
        return users.save(user);
    }
}
