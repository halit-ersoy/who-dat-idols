package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.Person;
import com.ses.whodatidols.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class PersonRepository {

    private final JdbcTemplate jdbcTemplate;
    private final EmailService emailService;

    @Autowired
    public PersonRepository(JdbcTemplate jdbcTemplate, EmailService emailService) {
        this.jdbcTemplate = jdbcTemplate;
        this.emailService = emailService;
    }

    // SQL sorguları
    private static final String CALL_VERIFY_GENERATE_CODE =
            "{call VerifyOrGenerateCode(?, ?, ?, ?)}";

    private static final String INSERT_PERSON =
            "INSERT INTO [WhoDatIdols].[dbo].[Person] (nickname, name, surname, email, password) VALUES (?, ?, ?, ?, ?)";

    private static final String CHECK_NICKNAME_EXISTS =
            "SELECT COUNT(*) FROM [WhoDatIdols].[dbo].[Person] WHERE nickname = ?";

    private static final String CHECK_EMAIL_EXISTS =
            "SELECT COUNT(*) FROM [WhoDatIdols].[dbo].[Person] WHERE email = ?";

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

    private static final String CHECK_COOKIE_EXISTS =
            "SELECT COUNT(*) FROM [WhoDatIdols].[dbo].[Cookies] WHERE nickname = ?";

    /**
     * Verifies if a user exists and generates a verification code
     */
    public Map<String, Object> generateResetCode(String nicknameOrEmail) {
        try {
            // Generate a random 6-digit code
            String code = String.format("%06d", new java.util.Random().nextInt(1000000));

            Map<String, Object> result = jdbcTemplate.call(connection -> {
                var callableStatement = connection.prepareCall(CALL_VERIFY_GENERATE_CODE);
                callableStatement.setString(1, nicknameOrEmail);
                callableStatement.setString(2, null);
                callableStatement.setString(3, null);
                callableStatement.setString(4, code);
                return callableStatement;
            }, Collections.emptyList());

            if (result.containsKey("#result-set-1")) {
                List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");
                if (!resultSet.isEmpty()) {
                    Map<String, Object> responseMap = resultSet.get(0);
                    boolean isSuccess = (boolean) responseMap.get("Result");

                    // If successful and email exists, send the verification code
                    if (isSuccess && responseMap.containsKey("Email")) {
                        String email = (String) responseMap.get("Email");
                        if (email != null && !email.isEmpty()) {
                            try {
                                // Send verification code via email
                                emailService.sendVerificationCode(email, code);
                            } catch (RuntimeException e) {
                                // Email sending failed, update response
                                Map<String, Object> emailFailure = new HashMap<>();
                                emailFailure.put("Result", false);
                                emailFailure.put("Message", "E-posta gönderimi başarısız oldu. Lütfen daha sonra tekrar deneyin.");
                                return emailFailure;
                            }
                        }
                    }

                    // Create a clean response map without exposing the code
                    Map<String, Object> cleanResponse = new HashMap<>();
                    cleanResponse.put("Result", responseMap.get("Result"));
                    cleanResponse.put("Message", responseMap.get("Message"));
                    return cleanResponse;
                }
            }

            Map<String, Object> error = new HashMap<>();
            error.put("Result", false);
            error.put("Message", "Bir hata oluştu");
            return error;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("Result", false);
            error.put("Message", e.getMessage());
            return error;
        }
    }
    /**
     * Verifies the reset code
     */
    public Map<String, Object> verifyResetCode(String nicknameOrEmail, String code) {
        try {
            Map<String, Object> result = jdbcTemplate.call(connection -> {
                var callableStatement = connection.prepareCall(CALL_VERIFY_GENERATE_CODE);
                callableStatement.setString(1, nicknameOrEmail);
                callableStatement.setString(2, code);
                callableStatement.setString(3, null);
                callableStatement.setString(4, null);
                return callableStatement;
            }, Collections.emptyList());

            if (result.containsKey("#result-set-1")) {
                List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");
                if (!resultSet.isEmpty()) {
                    return resultSet.get(0);
                }
            }

            Map<String, Object> error = new HashMap<>();
            error.put("Result", false);
            error.put("Message", "Doğrulama başarısız");
            return error;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("Result", false);
            error.put("Message", e.getMessage());
            return error;
        }
    }

    /**
     * Resets the password using verification code
     */
    public Map<String, Object> resetPassword(String nicknameOrEmail, String code, String newPassword) {
        try {
            Map<String, Object> result = jdbcTemplate.call(connection -> {
                var callableStatement = connection.prepareCall(CALL_VERIFY_GENERATE_CODE);
                callableStatement.setString(1, nicknameOrEmail);
                callableStatement.setString(2, code);
                callableStatement.setString(3, newPassword);
                callableStatement.setString(4, null);
                return callableStatement;
            }, Collections.emptyList());

            if (result.containsKey("#result-set-1")) {
                List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");
                if (!resultSet.isEmpty()) {
                    return resultSet.get(0);
                }
            }

            Map<String, Object> error = new HashMap<>();
            error.put("Result", false);
            error.put("Message", "Şifre sıfırlama başarısız");
            return error;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("Result", false);
            error.put("Message", e.getMessage());
            return error;
        }
    }

    public void registerPerson(Person person) {
        // Check if nickname already exists
        Integer nicknameCount = jdbcTemplate.queryForObject(
                CHECK_NICKNAME_EXISTS, Integer.class, person.getNickname());

        Integer emailCount = jdbcTemplate.queryForObject(
                CHECK_EMAIL_EXISTS, Integer.class, person.getEmail());

        if (nicknameCount != null && nicknameCount > 0) {
            throw new RuntimeException("Bu kullanıcı adı zaten kullanılmaktadır.");
        }

        if (emailCount != null && emailCount > 0) {
            throw new RuntimeException("Bu e-posta adresi zaten kullanılmaktadır.");
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

        // Check if cookie record already exists
        Integer count = jdbcTemplate.queryForObject(
                CHECK_COOKIE_EXISTS,
                Integer.class, nickname);

        if (count != null && count > 0) {
            // Update existing record
            jdbcTemplate.update(UPDATE_COOKIE, cookieValue, nickname);
        } else {
            // Insert new record
            jdbcTemplate.update(INSERT_COOKIE, nickname, cookieValue);
        }

        return cookieValue;
    }

}