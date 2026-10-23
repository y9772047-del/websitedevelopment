package com.example.studyplan.security;

import com.example.studyplan.entity.User;
import com.example.studyplan.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PasswordMigration implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PasswordMigration.class);
    private final UserMapper users;
    private final PasswordEncoder encoder;

    public PasswordMigration(UserMapper users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrate();
    }

    public void migrate() {
        int changed = 0;
        for (User user : users.selectList(null)) {
            String stored = user.getPassword();
            if (stored != null && stored.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$")) {
                continue;
            }
            if (stored == null) {
                throw new IllegalStateException("User " + user.getId() + " has no password");
            }
            User update = new User();
            update.setId(user.getId());
            update.setPassword(encoder.encode(stored));
            if (users.updateById(update) != 1) {
                throw new IllegalStateException("Could not migrate password for user " + user.getId());
            }
            changed++;
        }
        if (changed > 0) {
            log.info("Converted {} legacy passwords to BCrypt", changed);
        }
    }
}
