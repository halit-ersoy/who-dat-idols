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

    public Map<String, Object> loginUser(String usernameOrEmail, String password) {
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            var callableStatement = connection.prepareCall("{call LoginAndSetCookie(?, ?)}");
            callableStatement.setString(1, usernameOrEmail);
            callableStatement.setString(2, password);
            return callableStatement;
        }, Collections.emptyList());

        Map<String, Object> response = new HashMap<>();
        if (result.containsKey("#result-set-1")) {
            List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");
            if (!resultSet.isEmpty()) {
                Map<String, Object> row = resultSet.get(0);
                response.put("success", row.get("Result"));
                response.put("message", row.get("Message"));
                response.put("nickname", row.get("Nickname"));
                response.put("cookie", row.get("Cookie"));
            }
        }

        return response;
    }

    public Map<String, Object> registerUser(Person person) {
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            var callableStatement = connection.prepareCall("{call RegisterUser(?, ?, ?, ?, ?)}");
            callableStatement.setString(1, person.getNickname());
            callableStatement.setString(2, person.getName());
            callableStatement.setString(3, person.getSurname());
            callableStatement.setString(4, person.getEmail());
            callableStatement.setString(5, person.getPassword());
            return callableStatement;
        }, Collections.emptyList());

        Map<String, Object> response = new HashMap<>();
        if (result.containsKey("#result-set-1")) {
            List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");
            if (!resultSet.isEmpty()) {
                Map<String, Object> row = resultSet.get(0);
                response.put("success", row.get("Result"));
                response.put("message", row.get("Message"));
                response.put("nickname", row.get("Nickname"));
                response.put("cookie", row.get("Cookie"));
            }
        }

        return response;
    }

}