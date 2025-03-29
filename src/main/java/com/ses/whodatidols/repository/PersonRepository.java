package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.Person;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class PersonRepository {

    private final JdbcTemplate jdbcTemplate;

    // SQL sorguları
    private static final String INSERT_PERSON =
            "INSERT INTO [WhoDatIdols].[dbo].[Person] (nickname, name, surname, email, password) VALUES (?, ?, ?, ?, ?)";

    private static final String CHECK_NICKNAME_EXISTS =
            "SELECT COUNT(*) FROM [WhoDatIdols].[dbo].[Person] WHERE nickname = ?";

    private static final String VALIDATE_BY_NICKNAME =
            "SELECT COUNT(*) FROM [WhoDatIdols].[dbo].[Person] WHERE nickname = ? AND password = ?";

    private static final String VALIDATE_BY_EMAIL =
            "SELECT COUNT(*) FROM [WhoDatIdols].[dbo].[Person] WHERE email = ? AND password = ?";

    private static final String GET_NICKNAME_BY_EMAIL =
            "SELECT nickname FROM [WhoDatIdols].[dbo].[Person] WHERE email = ?";

    private static final String INSERT_COOKIE =
            "INSERT INTO [WhoDatIdols].[dbo].[Cookies] (nickname, cookie) VALUES (?, ?)";

    private static final String UPDATE_COOKIE =
            "UPDATE [WhoDatIdols].[dbo].[Cookies] SET cookie = ? WHERE nickname = ?";

    public PersonRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void registerPerson(Person person) {
        // Check if nickname already exists
        Integer count = jdbcTemplate.queryForObject(
                CHECK_NICKNAME_EXISTS, Integer.class, person.getNickname());

        if (count != null && count > 0) {
            throw new RuntimeException("Bu kullanıcı adı zaten kullanılmaktadır.");
        }

        jdbcTemplate.update(INSERT_PERSON,
                person.getNickname(),
                person.getName(),
                person.getSurname(),
                person.getEmail(),
                person.getPassword());
    }

    public boolean validateCredentials(String usernameOrEmail, String password) {
        // Öncelikle nickname üzerinden kontrol et
        Integer nicknameMatch = jdbcTemplate.queryForObject(
                VALIDATE_BY_NICKNAME, Integer.class, usernameOrEmail, password);

        if (nicknameMatch != null && nicknameMatch > 0) {
            return true;
        }

        Integer emailMatch = jdbcTemplate.queryForObject(
                VALIDATE_BY_EMAIL, Integer.class, usernameOrEmail, password);

        return emailMatch != null && emailMatch > 0;
    }

    public String createCookie(String usernameOrEmail) {
        String nickname = usernameOrEmail;

        if (usernameOrEmail.contains("@")) {
            nickname = jdbcTemplate.queryForObject(
                    GET_NICKNAME_BY_EMAIL, String.class, usernameOrEmail);
        }

        String cookieValue = UUID.randomUUID().toString();

        try {
            jdbcTemplate.update(UPDATE_COOKIE, cookieValue, nickname);
        } catch (EmptyResultDataAccessException e) {
            jdbcTemplate.update(INSERT_COOKIE, nickname, cookieValue);
        }

        return cookieValue;
    }
}