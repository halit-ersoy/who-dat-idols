package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.Person;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class PersonRepository {
    private final JdbcTemplate jdbcTemplate;
    private static final String INSERT_PERSON =
            "INSERT INTO [WhoDatIdols].[dbo].[Person] (nickname, name, surname, email, password) VALUES (?, ?, ?, ?, ?)";

    public PersonRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void registerPerson(Person person) {
        jdbcTemplate.update(INSERT_PERSON,
                person.getNickname(),
                person.getName(),
                person.getSurname(),
                person.getEmail(),
                person.getPassword());
    }

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
    private static final String GET_COOKIE =
            "SELECT cookie FROM [WhoDatIdols].[dbo].[Cookies] WHERE nickname = ?";

    public boolean validateCredentials(String usernameOrEmail, String password) {
        // First check if input is nickname
        Integer nicknameMatch = jdbcTemplate.queryForObject(VALIDATE_BY_NICKNAME, Integer.class,
                usernameOrEmail, password);

        if (nicknameMatch != null && nicknameMatch > 0) {
            return true;
        }

        // If not found by nickname, try email
        Integer emailMatch = jdbcTemplate.queryForObject(VALIDATE_BY_EMAIL, Integer.class,
                usernameOrEmail, password);

        return emailMatch != null && emailMatch > 0;
    }

    // Update the createCookie method in PersonRepository.java
    public String createCookie(String usernameOrEmail) {
        String nickname = usernameOrEmail;

        // If the input is an email, get the associated nickname
        if (usernameOrEmail.contains("@")) {
            nickname = jdbcTemplate.queryForObject(GET_NICKNAME_BY_EMAIL, String.class, usernameOrEmail);
        }

        // Generate a unique cookie value
        String cookieValue = UUID.randomUUID().toString();

        // First check if there's an existing cookie for this user
        Integer cookieCount = jdbcTemplate.queryForObject(
                GET_COOKIE,
                Integer.class,
                nickname);

        if (cookieCount != null && cookieCount > 0) {
            // Update existing cookie
            jdbcTemplate.update(
                    UPDATE_COOKIE,
                    cookieValue, nickname);
        } else {
            // Insert new cookie
            jdbcTemplate.update(INSERT_COOKIE, nickname, cookieValue);
        }

        return cookieValue;
    }

}