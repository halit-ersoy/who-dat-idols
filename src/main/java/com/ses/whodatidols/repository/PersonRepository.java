package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.Person;
import com.ses.whodatidols.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public class PersonRepository {

    private final JdbcTemplate jdbcTemplate;
    private final EmailService emailService;
    private static volatile boolean schemaEnsured = false;

    @Autowired
    public PersonRepository(JdbcTemplate jdbcTemplate, EmailService emailService) {
        this.jdbcTemplate = jdbcTemplate;
        this.emailService = emailService;
        ensureSchema();
    }

    // SQL sorguları
    private static final String CALL_VERIFY_GENERATE_CODE = "{call VerifyOrGenerateCode(?, ?, ?, ?)}";

    private static final String VALIDATE_BY_NICKNAME = "SELECT COUNT(*) FROM [WhoDatIdols].[dbo].[Person] WHERE nickname = ? AND password = ?";

    private static final String VALIDATE_BY_EMAIL = "SELECT COUNT(*) FROM [WhoDatIdols].[dbo].[Person] WHERE email = ? AND password = ?";

    /**
     * Verifies if a user exists and generates a verification code
     */
    @SuppressWarnings("unchecked")
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
            }, new ArrayList<SqlParameter>());

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
                                emailFailure.put("Message",
                                        "E-posta gönderimi başarısız oldu: " + e.getMessage());
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
    @SuppressWarnings("unchecked")
    public Map<String, Object> verifyResetCode(String nicknameOrEmail, String code) {
        try {
            Map<String, Object> result = jdbcTemplate.call(connection -> {
                var callableStatement = connection.prepareCall(CALL_VERIFY_GENERATE_CODE);
                callableStatement.setString(1, nicknameOrEmail);
                callableStatement.setString(2, code);
                callableStatement.setString(3, null);
                callableStatement.setString(4, null);
                return callableStatement;
            }, new ArrayList<SqlParameter>());

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
    @SuppressWarnings("unchecked")
    public Map<String, Object> resetPassword(String nicknameOrEmail, String code, String newPassword) {
        try {
            Map<String, Object> result = jdbcTemplate.call(connection -> {
                var callableStatement = connection.prepareCall(CALL_VERIFY_GENERATE_CODE);
                callableStatement.setString(1, nicknameOrEmail);
                callableStatement.setString(2, code);
                callableStatement.setString(3, newPassword);
                callableStatement.setString(4, null);
                return callableStatement;
            }, new ArrayList<SqlParameter>());

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

    /**
     * Generates a code specifically for email verification
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateEmailVerificationCode(String nickname) {
        try {
            // Generate a random 6-digit code
            String code = String.format("%06d", new java.util.Random().nextInt(1000000));

            Map<String, Object> result = jdbcTemplate.call(connection -> {
                var callableStatement = connection.prepareCall(CALL_VERIFY_GENERATE_CODE);
                callableStatement.setString(1, nickname);
                callableStatement.setString(2, null);
                callableStatement.setString(3, null);
                callableStatement.setString(4, code);
                return callableStatement;
            }, new ArrayList<SqlParameter>());

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
                                emailFailure.put("Message", "E-posta gönderimi başarısız oldu: " + e.getMessage());
                                return emailFailure;
                            }
                        }
                    }

                    // Create a clean response map without exposing the code
                    Map<String, Object> cleanResponse = new HashMap<>();
                    cleanResponse.put("Result", responseMap.get("Result"));
                    cleanResponse.put("Message", responseMap.get("Message"));
                    cleanResponse.put("success", responseMap.get("Result")); // For UI
                    return cleanResponse;
                }
            }

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Bir hata oluştu");
            return error;

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return error;
        }
    }

    /**
     * Verifies the email verification code
     */
    public Map<String, Object> verifyEmailCode(String nickname, String code) {
        try {
            Map<String, Object> verifyResult = verifyResetCode(nickname, code);

            boolean isSuccess = false;
            if (verifyResult.containsKey("Result") && verifyResult.get("Result") instanceof Boolean) {
                isSuccess = (boolean) verifyResult.get("Result");
            }

            if (isSuccess) {
                // If code is valid, update the user to be verified
                jdbcTemplate.update("UPDATE [WhoDatIdols].[dbo].[Person] SET isVerified = 1 WHERE nickname = ?",
                        nickname);

                // Clear the verification code
                jdbcTemplate.update(
                        "DELETE FROM [WhoDatIdols].[dbo].[VerificationCode] WHERE ID = (SELECT ID FROM [WhoDatIdols].[dbo].[Person] WHERE nickname = ?)",
                        nickname);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Hesabınız başarıyla doğrulandı!");
                return response;
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Geçersiz veya süresi dolmuş kod.");
                return response;
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
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
        return loginUser(usernameOrEmail, password, "whodatidols");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> loginUser(String usernameOrEmail, String password, String platform) {
        Map<String, Object> result;
        try {
            result = jdbcTemplate.call(connection -> {
                var callableStatement = connection.prepareCall("{call LoginAndSetCookie(?, ?, ?)}");
                callableStatement.setString(1, usernameOrEmail);
                callableStatement.setString(2, password);
                callableStatement.setString(3, platform != null ? platform : "whodatidols");
                return callableStatement;
            }, new ArrayList<SqlParameter>());
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("too many arguments") || e.getMessage().contains("8144"))) {
                result = jdbcTemplate.call(connection -> {
                    var callableStatement = connection.prepareCall("{call LoginAndSetCookie(?, ?)}");
                    callableStatement.setString(1, usernameOrEmail);
                    callableStatement.setString(2, password);
                    return callableStatement;
                }, new ArrayList<SqlParameter>());
            } else {
                throw e;
            }
        }

        Map<String, Object> response = new HashMap<>();
        if (result.containsKey("#result-set-1")) {
            List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");
            if (!resultSet.isEmpty()) {
                Map<String, Object> row = resultSet.get(0);
                for (String key : row.keySet()) {
                    String lowerKey = key.toLowerCase();
                    Object value = row.get(key);
                    if (lowerKey.equals("result"))
                        response.put("success", value);
                    else if (lowerKey.equals("message"))
                        response.put("message", value);
                    else if (lowerKey.equals("nickname"))
                        response.put("nickname", value);
                    else if (lowerKey.equals("cookie"))
                        response.put("cookie", value);
                    else if (lowerKey.equals("isbanned"))
                        response.put("isBanned", value);
                    else if (lowerKey.equals("banreason"))
                        response.put("banReason", value);
                    response.put(key, value);
                }

                Object successVal = response.get("success");
                boolean isSuccess = Boolean.TRUE.equals(successVal) ||
                        (successVal instanceof Number && ((Number) successVal).intValue() == 1) ||
                        "true".equalsIgnoreCase(String.valueOf(successVal));
                Object cookieVal = response.get("cookie");
                if (isSuccess && cookieVal != null) {
                    try {
                        UUID token = UUID.fromString(cookieVal.toString());
                        UUID userId = findUserIdByCookie(cookieVal.toString());
                        if (userId != null) {
                            jdbcTemplate.update("""
                                IF NOT EXISTS (SELECT 1 FROM [WhoDatIdols].[dbo].[UserSession] WHERE Token = ?)
                                BEGIN
                                    INSERT INTO [WhoDatIdols].[dbo].[UserSession] ([UserId], [Token], [Platform], [CreatedAt], [LastActiveAt])
                                    VALUES (?, ?, ?, GETDATE(), GETDATE())
                                END
                            """, token, userId, token, platform != null ? platform : "whodatidols");
                        }
                    } catch (Exception ex) {
                        System.err.println("UserSession register error: " + ex.getMessage());
                    }
                }
            }
        }

        return response;
    }

    public Optional<Person> findByNicknameOrEmail(String usernameOrEmail) {
        ensureSchema();
        String sql = "SELECT ID, nickname, name, surname, email, password, isBanned, banReason, role, allowMessages, isVerified FROM [WhoDatIdols].[dbo].[Person] WHERE nickname = ? OR email = ?";
        return jdbcTemplate.query(sql, (rs) -> {
            if (rs.next()) {
                Person p = new Person();
                p.setId(UUID.fromString(rs.getString("ID")));
                p.setNickname(rs.getString("nickname"));
                p.setName(rs.getString("name"));
                p.setSurname(rs.getString("surname"));
                p.setEmail(rs.getString("email"));
                p.setPassword(rs.getString("password"));
                p.setBanned(rs.getBoolean("isBanned"));
                p.setBanReason(rs.getString("banReason"));
                p.setRole(rs.getString("role"));
                p.setAllowMessages(rs.getBoolean("allowMessages"));
                p.setVerified(rs.getBoolean("isVerified"));
                return Optional.of(p);
            }
            return Optional.empty();
        }, usernameOrEmail, usernameOrEmail);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> registerUser(Person person) {
        return registerUser(person, "whodatidols");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> registerUser(Person person, String platform) {
        Map<String, Object> result;
        try {
            result = jdbcTemplate.call(connection -> {
                var callableStatement = connection.prepareCall("{call RegisterUser(?, ?, ?, ?, ?, ?)}");
                callableStatement.setString(1, person.getNickname());
                callableStatement.setString(2, person.getName());
                callableStatement.setString(3, person.getSurname());
                callableStatement.setString(4, person.getEmail());
                callableStatement.setString(5, person.getPassword());
                callableStatement.setString(6, platform != null ? platform : "whodatidols");
                return callableStatement;
            }, new ArrayList<SqlParameter>());
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("too many arguments") || e.getMessage().contains("8144"))) {
                result = jdbcTemplate.call(connection -> {
                    var callableStatement = connection.prepareCall("{call RegisterUser(?, ?, ?, ?, ?)}");
                    callableStatement.setString(1, person.getNickname());
                    callableStatement.setString(2, person.getName());
                    callableStatement.setString(3, person.getSurname());
                    callableStatement.setString(4, person.getEmail());
                    callableStatement.setString(5, person.getPassword());
                    return callableStatement;
                }, new ArrayList<SqlParameter>());
            } else {
                throw e;
            }
        }

        Map<String, Object> response = new HashMap<>();
        if (result.containsKey("#result-set-1")) {
            List<Map<String, Object>> resultSet = (List<Map<String, Object>>) result.get("#result-set-1");
            if (!resultSet.isEmpty()) {
                Map<String, Object> row = resultSet.get(0);
                response.put("success", row.get("Result"));
                response.put("message", row.get("Message"));
                response.put("nickname", row.get("Nickname"));
                response.put("cookie", row.get("Cookie"));

                Object successVal = response.get("success");
                boolean isSuccess = Boolean.TRUE.equals(successVal) ||
                        (successVal instanceof Number && ((Number) successVal).intValue() == 1) ||
                        "true".equalsIgnoreCase(String.valueOf(successVal));
                Object cookieVal = response.get("cookie");
                if (isSuccess && cookieVal != null) {
                    try {
                        UUID token = UUID.fromString(cookieVal.toString());
                        UUID userId = findUserIdByCookie(cookieVal.toString());
                        if (userId != null) {
                            jdbcTemplate.update("""
                                IF NOT EXISTS (SELECT 1 FROM [WhoDatIdols].[dbo].[UserSession] WHERE Token = ?)
                                BEGIN
                                    INSERT INTO [WhoDatIdols].[dbo].[UserSession] ([UserId], [Token], [Platform], [CreatedAt], [LastActiveAt])
                                    VALUES (?, ?, ?, GETDATE(), GETDATE())
                                END
                            """, token, userId, token, platform != null ? platform : "whodatidols");
                        }
                    } catch (Exception ex) {
                        System.err.println("UserSession register error during registration: " + ex.getMessage());
                    }
                }
            }
        }

        return response;
    }

    public Map<String, Object> getUserInfoByCookie(String cookie) {
        ensureSchema();
        UUID cookieUuid = UUID.fromString(cookie);
        String sql = """
            SELECT TOP 1 P.ID, P.nickname, P.name, P.surname, P.email, P.isBanned, P.banReason, P.role, P.allowMessages, P.isVerified 
            FROM [WhoDatIdols].[dbo].[Person] P 
            WHERE P.cookie = ? OR EXISTS (
                SELECT 1 FROM [WhoDatIdols].[dbo].[UserSession] US 
                WHERE US.UserId = P.ID AND US.Token = ?
            )
        """;
        return jdbcTemplate.queryForMap(sql, cookieUuid, cookieUuid);
    }

    public UUID findUserIdByCookie(String cookie) {
        if (cookie == null || cookie.isBlank()) return null;
        try {
            UUID cookieUuid = UUID.fromString(cookie.trim());
            String sql = "SELECT [dbo].[ResolveUserIdByCookie](?)";
            String userIdStr = jdbcTemplate.queryForObject(sql, String.class, cookieUuid);
            return userIdStr != null ? UUID.fromString(userIdStr) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> updatePasswordByCookie(String cookie, String newPassword) {
        String sql = "EXEC UpdatePasswordByCookie @cookie = ?, @password = ?";
        return jdbcTemplate.queryForMap(sql, UUID.fromString(cookie), newPassword);
    }

    public Map<String, Object> updateProfileByCookie(String cookie, String nickname, String name, String surname,
            String email) {
        String sql = "EXEC UpdateUserProfile @cookie = ?, @nickname = ?, @name = ?, @surname = ?, @email = ?";

        // Before updating, check if email changed
        try {
            Map<String, Object> currentUser = getUserInfoByCookie(cookie);
            String currentEmail = (String) currentUser.get("email");
            if (currentEmail != null && !currentEmail.equals(email)) {
                jdbcTemplate.update("UPDATE [WhoDatIdols].[dbo].[Person] SET isVerified = 0 WHERE ID = [dbo].[ResolveUserIdByCookie](?)",
                        UUID.fromString(cookie));
            }
        } catch (Exception e) {
            // Ignore error
        }

        return jdbcTemplate.queryForMap(sql, UUID.fromString(cookie), nickname, name, surname, email);
    }

    /**
     * Creates a short-lived SSO Ticket for cross-domain single sign-on.
     */
    @SuppressWarnings("unchecked")
    public String createSsoTicket(String cookie, String targetPlatform) {
        try {
            Map<String, Object> result = jdbcTemplate.call(connection -> {
                var callableStatement = connection.prepareCall("{call CreateSsoTicket(?, ?)}");
                callableStatement.setObject(1, UUID.fromString(cookie));
                callableStatement.setString(2, targetPlatform);
                return callableStatement;
            }, new ArrayList<SqlParameter>());

            if (result.containsKey("#result-set-1")) {
                List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("#result-set-1");
                if (!rows.isEmpty()) {
                    Map<String, Object> row = rows.get(0);
                    Object res = row.get("Result");
                    boolean success = Boolean.TRUE.equals(res) || (res instanceof Number && ((Number) res).intValue() == 1);
                    if (success && row.get("Ticket") != null) {
                        return row.get("Ticket").toString();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("createSsoTicket error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Consumes an SSO Ticket, creates a platform session and returns user info + new cookie.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> consumeSsoTicket(String ticket, String platform) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = jdbcTemplate.call(connection -> {
                var callableStatement = connection.prepareCall("{call ConsumeSsoTicket(?, ?)}");
                callableStatement.setObject(1, UUID.fromString(ticket));
                callableStatement.setString(2, platform != null ? platform : "whodatidols");
                return callableStatement;
            }, new ArrayList<SqlParameter>());

            if (result.containsKey("#result-set-1")) {
                List<Map<String, Object>> rows = (List<Map<String, Object>>) result.get("#result-set-1");
                if (!rows.isEmpty()) {
                    Map<String, Object> row = rows.get(0);
                    for (String key : row.keySet()) {
                        String lower = key.toLowerCase();
                        if (lower.equals("result")) response.put("success", row.get(key));
                        else if (lower.equals("message")) response.put("message", row.get(key));
                        else if (lower.equals("cookie")) response.put("cookie", row.get(key));
                        else if (lower.equals("nickname")) response.put("nickname", row.get(key));
                        else if (lower.equals("userid")) response.put("userId", row.get(key));
                        response.put(key, row.get(key));
                    }
                    return response;
                }
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "SSO bilet işleme hatası: " + e.getMessage());
            return response;
        }
        response.put("success", false);
        response.put("message", "Geçersiz veya süresi dolmuş bilet");
        return response;
    }

    public List<Person> findAllUsers() {
        ensureSchema();
        String sql = "SELECT ID, nickname, name, surname, email, isBanned, banReason, role, allowMessages, isVerified FROM [WhoDatIdols].[dbo].[Person] ORDER BY nickname ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Person p = new Person();
            p.setId(UUID.fromString(rs.getString("ID")));
            p.setNickname(rs.getString("nickname"));
            p.setName(rs.getString("name"));
            p.setSurname(rs.getString("surname"));
            p.setEmail(rs.getString("email"));
            p.setBanned(rs.getBoolean("isBanned"));
            p.setBanReason(rs.getString("banReason"));
            p.setRole(rs.getString("role"));
            p.setAllowMessages(rs.getBoolean("allowMessages"));
            p.setVerified(rs.getBoolean("isVerified"));
            return p;
        });
    }

    public void updateUserRole(UUID userId, String role) {
        String sql = "UPDATE [WhoDatIdols].[dbo].[Person] SET role = ? WHERE ID = ?";
        jdbcTemplate.update(sql, role, userId.toString());
    }

    public void deleteUser(UUID userId) {
        if (userId == null) return;
        ensureSchema();

        // 1. UserSession & SsoTicket
        try { jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[UserSession] WHERE UserId = ?", userId); } catch (Exception ignored) {}
        try { jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[SsoTicket] WHERE UserId = ?", userId); } catch (Exception ignored) {}

        // 2. Feedback
        try { jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[Feedback] WHERE UserID = ?", userId); } catch (Exception ignored) {}

        // 3. UserMessages, UserBlocks, UserReports
        try { jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[UserMessages] WHERE SenderID = ? OR ReceiverID = ?", userId, userId); } catch (Exception ignored) {}
        try { jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[UserBlocks] WHERE BlockerID = ? OR BlockedID = ?", userId, userId); } catch (Exception ignored) {}
        try { jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[UserReports] WHERE ReporterID = ? OR ReportedID = ?", userId, userId); } catch (Exception ignored) {}

        // 4. VerificationCode
        try { jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[VerificationCode] WHERE ID = ?", userId); } catch (Exception ignored) {}

        // 5. UserLists & UserListItems
        try { jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[UserListItems] WHERE ListID IN (SELECT ID FROM [WhoDatIdols].[dbo].[UserLists] WHERE UserID = ?)", userId); } catch (Exception ignored) {}
        try { jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[UserLists] WHERE UserID = ?", userId); } catch (Exception ignored) {}

        // 6. UserWatchHistory
        try { jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[UserWatchHistory] WHERE UserID = ?", userId); } catch (Exception ignored) {}

        // 7. Comments & CommentLikes
        try { jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[CommentLikes] WHERE UserId = ?", userId); } catch (Exception ignored) {}
        try { jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[CommentLikes] WHERE CommentId IN (SELECT ID FROM [WhoDatIdols].[dbo].[Comments] WHERE UserId = ?)", userId); } catch (Exception ignored) {}
        try { jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[Comments] WHERE UserId = ?", userId); } catch (Exception ignored) {}

        // 8. Delete user from Person
        String sql = "DELETE FROM [WhoDatIdols].[dbo].[Person] WHERE ID = ?";
        jdbcTemplate.update(sql, userId.toString());
    }

    public void toggleUserBanStatus(UUID userId, boolean ban, String reason) {
        String sql = "UPDATE [WhoDatIdols].[dbo].[Person] SET isBanned = ?, banReason = ? " +
                (ban ? ", cookie = NULL " : "") +
                "WHERE ID = ?";
        jdbcTemplate.update(sql, ban ? 1 : 0, reason, userId.toString());
        if (ban) {
            jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[UserSession] WHERE UserId = ?", userId);
        }
    }

    public void ensureSchema() {
        if (schemaEnsured) return;
        synchronized (PersonRepository.class) {
            if (schemaEnsured) return;
            try {
            // Columns on Person
            executeSqlWithRetry(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('[WhoDatIdols].[dbo].[Person]') AND name = 'role') ALTER TABLE [WhoDatIdols].[dbo].[Person] ADD role NVARCHAR(50) DEFAULT 'USER'");
            executeSqlWithRetry(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('[WhoDatIdols].[dbo].[Person]') AND name = 'allowMessages') ALTER TABLE [WhoDatIdols].[dbo].[Person] ADD allowMessages BIT DEFAULT 1");
            executeSqlWithRetry(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('[WhoDatIdols].[dbo].[Person]') AND name = 'isVerified') ALTER TABLE [WhoDatIdols].[dbo].[Person] ADD isVerified BIT DEFAULT 0");

            // UserSession table
            executeSqlWithRetry("""
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='UserSession' AND xtype='U')
                BEGIN
                    CREATE TABLE [dbo].[UserSession] (
                        [ID] UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID() PRIMARY KEY,
                        [UserId] UNIQUEIDENTIFIER NOT NULL,
                        [Token] UNIQUEIDENTIFIER NOT NULL,
                        [Platform] NVARCHAR(50) NULL DEFAULT 'whodatidols',
                        [CreatedAt] DATETIME NOT NULL DEFAULT GETDATE(),
                        [LastActiveAt] DATETIME NOT NULL DEFAULT GETDATE(),
                        CONSTRAINT [FK_UserSession_Person] FOREIGN KEY ([UserId]) REFERENCES [dbo].[Person]([ID]) ON DELETE CASCADE
                    );
                    CREATE UNIQUE NONCLUSTERED INDEX [IX_UserSession_Token] ON [dbo].[UserSession]([Token]);
                    CREATE NONCLUSTERED INDEX [IX_UserSession_UserId] ON [dbo].[UserSession]([UserId]);
                END
            """);

            // SsoTicket table
            executeSqlWithRetry("""
                IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='SsoTicket' AND xtype='U')
                BEGIN
                    CREATE TABLE [dbo].[SsoTicket] (
                        [Ticket] UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
                        [UserId] UNIQUEIDENTIFIER NOT NULL,
                        [TargetPlatform] NVARCHAR(50) NULL,
                        [ExpiresAt] DATETIME NOT NULL,
                        [CreatedAt] DATETIME NOT NULL DEFAULT GETDATE(),
                        CONSTRAINT [FK_SsoTicket_Person] FOREIGN KEY ([UserId]) REFERENCES [dbo].[Person]([ID]) ON DELETE CASCADE
                    );
                    CREATE NONCLUSTERED INDEX [IX_SsoTicket_ExpiresAt] ON [dbo].[SsoTicket]([ExpiresAt]);
                END
            """);

            // ResolveUserIdByCookie function
            executeSqlWithRetry("""
                CREATE OR ALTER FUNCTION [dbo].[ResolveUserIdByCookie](@cookie UNIQUEIDENTIFIER)
                RETURNS UNIQUEIDENTIFIER
                AS
                BEGIN
                    IF @cookie IS NULL RETURN NULL;
                    DECLARE @UserId UNIQUEIDENTIFIER;

                    SELECT TOP 1 @UserId = ID FROM [dbo].[Person] WHERE cookie = @cookie;

                    IF @UserId IS NULL
                    BEGIN
                        SELECT TOP 1 @UserId = UserId FROM [dbo].[UserSession] WHERE Token = @cookie;
                    END

                    RETURN @UserId;
                END;
            """);

            // CreateSsoTicket Procedure
            executeSqlWithRetry("""
                CREATE OR ALTER PROCEDURE [dbo].[CreateSsoTicket]
                    @Cookie UNIQUEIDENTIFIER,
                    @TargetPlatform NVARCHAR(50) = NULL
                AS
                BEGIN
                    SET NOCOUNT ON;
                    DECLARE @UserId UNIQUEIDENTIFIER = [dbo].[ResolveUserIdByCookie](@Cookie);

                    IF @UserId IS NULL
                    BEGIN
                        SELECT CAST(0 AS BIT) AS Result, 'Gecersiz oturum' AS Message, NULL AS Ticket;
                        RETURN;
                    END

                    DECLARE @Ticket UNIQUEIDENTIFIER = NEWID();
                    INSERT INTO [dbo].[SsoTicket] ([Ticket], [UserId], [TargetPlatform], [ExpiresAt], [CreatedAt])
                    VALUES (@Ticket, @UserId, @TargetPlatform, DATEADD(SECOND, 120, GETDATE()), GETDATE());

                    DELETE FROM [dbo].[SsoTicket] WHERE ExpiresAt < GETDATE();

                    SELECT CAST(1 AS BIT) AS Result, 'Bilet olusturuldu' AS Message, @Ticket AS Ticket;
                END;
            """);

            // ConsumeSsoTicket Procedure
            executeSqlWithRetry("""
                CREATE OR ALTER PROCEDURE [dbo].[ConsumeSsoTicket]
                    @Ticket UNIQUEIDENTIFIER,
                    @Platform NVARCHAR(50) = 'whodatidols'
                AS
                BEGIN
                    SET NOCOUNT ON;
                    DECLARE @UserId UNIQUEIDENTIFIER, @TargetPlatform NVARCHAR(50);

                    SELECT @UserId = UserId, @TargetPlatform = TargetPlatform
                    FROM [dbo].[SsoTicket]
                    WHERE Ticket = @Ticket AND ExpiresAt >= GETDATE();

                    IF @UserId IS NULL
                    BEGIN
                        SELECT CAST(0 AS BIT) AS Result, 'Gecersiz veya suresi dolmus SSO bileti' AS Message, NULL AS Nickname, NULL AS Cookie;
                        RETURN;
                    END

                    DELETE FROM [dbo].[SsoTicket] WHERE Ticket = @Ticket;

                    DECLARE @Nickname NVARCHAR(50), @IsBanned BIT, @BanReason NVARCHAR(500);
                    SELECT @Nickname = Nickname, @IsBanned = ISNULL(isBanned, 0), @BanReason = banReason
                    FROM [dbo].[Person] WHERE ID = @UserId;

                    IF @IsBanned = 1
                    BEGIN
                        SELECT CAST(0 AS BIT) AS Result, ISNULL(@BanReason, 'Hesabiniz askiya alinmistir') AS Message, NULL AS Nickname, NULL AS Cookie;
                        RETURN;
                    END

                    DECLARE @NewToken UNIQUEIDENTIFIER = NEWID();
                    INSERT INTO [dbo].[UserSession] ([UserId], [Token], [Platform], [CreatedAt], [LastActiveAt])
                    VALUES (@UserId, @NewToken, ISNULL(@Platform, 'whodatidols'), GETDATE(), GETDATE());

                    SELECT CAST(1 AS BIT) AS Result, 'SSO giris basarili' AS Message, @Nickname AS Nickname, @NewToken AS Cookie, @UserId AS UserId;
                END;
            """);

            // LoginAndSetCookie Procedure (Multi-Session & Multi-Platform Support)
            executeSqlWithRetry("""
                CREATE OR ALTER PROCEDURE [dbo].[LoginAndSetCookie]
                    @UsernameOrEmail NVARCHAR(255),
                    @Password NVARCHAR(255),
                    @Platform NVARCHAR(50) = 'whodatidols'
                AS
                BEGIN
                    SET NOCOUNT ON;
                    DECLARE @UserId UNIQUEIDENTIFIER, @Nickname NVARCHAR(50), @NewCookie UNIQUEIDENTIFIER, @IsBanned BIT, @BanReason NVARCHAR(500);

                    SELECT @UserId = ID, @Nickname = Nickname, @IsBanned = ISNULL(isBanned, 0), @BanReason = banReason
                    FROM [dbo].[Person]
                    WHERE (Email = @UsernameOrEmail OR Nickname = @UsernameOrEmail) AND Password = @Password;

                    IF @UserId IS NOT NULL
                    BEGIN
                        IF @IsBanned = 1
                        BEGIN
                            SELECT CAST(0 AS BIT) as Result, ISNULL(@BanReason, 'Hesabiniz askiya alinmistir') as Message;
                            RETURN;
                        END

                        SET @NewCookie = NEWID();

                        INSERT INTO [dbo].[UserSession] ([UserId], [Token], [Platform], [CreatedAt], [LastActiveAt])
                        VALUES (@UserId, @NewCookie, ISNULL(@Platform, 'whodatidols'), GETDATE(), GETDATE());

                        UPDATE [dbo].[Person] SET cookie = @NewCookie WHERE ID = @UserId;

                        DELETE FROM [dbo].[UserSession]
                        WHERE UserId = @UserId AND LastActiveAt < DATEADD(DAY, -30, GETDATE());

                        SELECT CAST(1 AS BIT) as Result, 'Giris basarili' as Message, @Nickname as Nickname, @NewCookie as Cookie;
                    END
                    ELSE
                        SELECT CAST(0 AS BIT) as Result, 'Hatali kullanici adi veya sifre' as Message;
                END;
            """);

            // RegisterUser Procedure (Multi-Platform Support)
            executeSqlWithRetry("""
                CREATE OR ALTER PROCEDURE [dbo].[RegisterUser]
                    @Nickname NVARCHAR(50),
                    @Name NVARCHAR(100),
                    @Surname NVARCHAR(100),
                    @Email NVARCHAR(255),
                    @Password NVARCHAR(255),
                    @Platform NVARCHAR(50) = 'whodatidols'
                AS
                BEGIN
                    SET NOCOUNT ON;
                    IF EXISTS (SELECT 1 FROM [dbo].[Person] WHERE Nickname = @Nickname OR Email = @Email)
                    BEGIN
                        SELECT CAST(0 AS BIT) as Result, 'Kullanici adi veya E-posta zaten kullanimda.' as Message;
                        RETURN;
                    END
                    
                    DECLARE @UserId UNIQUEIDENTIFIER = NEWID();
                    DECLARE @NewCookie UNIQUEIDENTIFIER = NEWID();

                    INSERT INTO [dbo].[Person] (ID, Nickname, Name, Surname, Email, Password, cookie, role, isVerified, isBanned, allowMessages)
                    VALUES (@UserId, @Nickname, @Name, @Surname, @Email, @Password, @NewCookie, 'USER', 0, 0, 1);

                    INSERT INTO [dbo].[UserSession] ([UserId], [Token], [Platform], [CreatedAt], [LastActiveAt])
                    VALUES (@UserId, @NewCookie, ISNULL(@Platform, 'whodatidols'), GETDATE(), GETDATE());
                    
                    SELECT CAST(1 AS BIT) as Result, 'Kayit basarili.' as Message, @Nickname as Nickname, @NewCookie as Cookie;
                END;
            """);

            // Ensure existing users have defaults set correctly
            executeSqlWithRetry("UPDATE [WhoDatIdols].[dbo].[Person] SET allowMessages = 1 WHERE allowMessages IS NULL");
            executeSqlWithRetry("UPDATE [WhoDatIdols].[dbo].[Person] SET isBanned = 0 WHERE isBanned IS NULL");
            executeSqlWithRetry("UPDATE [WhoDatIdols].[dbo].[Person] SET role = 'USER' WHERE role IS NULL");
            executeSqlWithRetry("UPDATE [WhoDatIdols].[dbo].[Person] SET isVerified = 0 WHERE isVerified IS NULL");
            schemaEnsured = true;
        } catch (Exception e) {
            System.err.println("PersonRepository ensureSchema error: " + e.getMessage());
        }
        }
    }

    private void executeSqlWithRetry(String sql) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                jdbcTemplate.execute(sql);
                return;
            } catch (Exception e) {
                if (i == maxRetries - 1) {
                    throw e;
                }
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    public void logoutUser(String cookie) {
        if (cookie == null || cookie.isBlank()) return;
        try {
            ensureSchema();
            UUID token = UUID.fromString(cookie.trim());
            jdbcTemplate.update("DELETE FROM [WhoDatIdols].[dbo].[UserSession] WHERE Token = ?", token);
            jdbcTemplate.update("UPDATE [WhoDatIdols].[dbo].[Person] SET cookie = NULL WHERE cookie = ?", token);
        } catch (Exception e) {
            System.err.println("Logout user error: " + e.getMessage());
        }
    }

    public void updateAllowMessages(UUID userId, boolean allow) {
        ensureSchema();
        String sql = "UPDATE [WhoDatIdols].[dbo].[Person] SET allowMessages = ? WHERE ID = ?";
        jdbcTemplate.update(sql, allow ? 1 : 0, userId.toString());
    }

    public void updateAllowMessagesByCookie(String cookie, boolean allow) {
        ensureSchema();
        String sql = "UPDATE [WhoDatIdols].[dbo].[Person] SET allowMessages = ? WHERE ID = [dbo].[ResolveUserIdByCookie](?)";
        jdbcTemplate.update(sql, allow ? 1 : 0, UUID.fromString(cookie));
    }

    public List<Person> searchUsersForMessaging(String query, String excludeNickname) {
        ensureSchema();
        String sql = "SELECT ID, nickname, name, surname, role, isVerified FROM [WhoDatIdols].[dbo].[Person] " +
                "WHERE (nickname LIKE ? OR name LIKE ? OR surname LIKE ?) " +
                "AND (allowMessages = 1 OR allowMessages IS NULL) " +
                "AND (isBanned = 0 OR isBanned IS NULL) " +
                "AND nickname != ? " +
                "ORDER BY nickname ASC";
        String searchPattern = "%" + query + "%";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Person p = new Person();
            p.setId(UUID.fromString(rs.getString("ID")));
            p.setNickname(rs.getString("nickname"));
            p.setName(rs.getString("name"));
            p.setSurname(rs.getString("surname"));
            p.setRole(rs.getString("role"));
            p.setVerified(rs.getBoolean("isVerified"));
            return p;
        }, searchPattern, searchPattern, searchPattern, excludeNickname);
    }
}
