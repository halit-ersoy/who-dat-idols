package com.ses.whodatidols.repository;

import com.ses.whodatidols.model.Person;
import com.ses.whodatidols.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
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
                                        "E-posta gönderimi başarısız oldu. Lütfen daha sonra tekrar deneyin.");
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

    @SuppressWarnings("unchecked")
    public Map<String, Object> loginUser(String usernameOrEmail, String password) {
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            var callableStatement = connection.prepareCall("{call LoginAndSetCookie(?, ?)}");
            callableStatement.setString(1, usernameOrEmail);
            callableStatement.setString(2, password);
            return callableStatement;
        }, new ArrayList<SqlParameter>());

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
            }
        }

        return response;
    }

    public Optional<Person> findByNicknameOrEmail(String usernameOrEmail) {
        ensureAllowMessagesColumnExists();
        String sql = "SELECT ID, nickname, name, surname, email, password, isBanned, banReason, role, allowMessages FROM [WhoDatIdols].[dbo].[Person] WHERE nickname = ? OR email = ?";
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
                return Optional.of(p);
            }
            return Optional.empty();
        }, usernameOrEmail, usernameOrEmail);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> registerUser(Person person) {
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            var callableStatement = connection.prepareCall("{call RegisterUser(?, ?, ?, ?, ?)}");
            callableStatement.setString(1, person.getNickname());
            callableStatement.setString(2, person.getName());
            callableStatement.setString(3, person.getSurname());
            callableStatement.setString(4, person.getEmail());
            callableStatement.setString(5, person.getPassword());
            return callableStatement;
        }, new ArrayList<SqlParameter>());

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

    public Map<String, Object> getUserInfoByCookie(String cookie) {
        ensureAllowMessagesColumnExists();
        String sql = "SELECT ID, nickname, name, surname, email, isBanned, banReason, role, allowMessages FROM [WhoDatIdols].[dbo].[Person] WHERE cookie = ?";
        return jdbcTemplate.queryForMap(sql, UUID.fromString(cookie));
    }

    public Map<String, Object> updatePasswordByCookie(String cookie, String newPassword) {
        String sql = "EXEC UpdatePasswordByCookie @cookie = ?, @password = ?";
        return jdbcTemplate.queryForMap(sql, UUID.fromString(cookie), newPassword);
    }

    public Map<String, Object> updateProfileByCookie(String cookie, String nickname, String name, String surname,
            String email) {
        String sql = "EXEC UpdateUserProfile @cookie = ?, @nickname = ?, @name = ?, @surname = ?, @email = ?";
        return jdbcTemplate.queryForMap(sql, UUID.fromString(cookie), nickname, name, surname, email);
    }

    public List<Person> findAllUsers() {
        ensureAllowMessagesColumnExists();
        String sql = "SELECT ID, nickname, name, surname, email, isBanned, banReason, role, allowMessages FROM [WhoDatIdols].[dbo].[Person] ORDER BY nickname ASC";
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
            return p;
        });
    }

    public void updateUserRole(UUID userId, String role) {
        String sql = "UPDATE [WhoDatIdols].[dbo].[Person] SET role = ? WHERE ID = ?";
        jdbcTemplate.update(sql, role, userId.toString());
    }

    public void deleteUser(UUID userId) {
        String sql = "DELETE FROM [WhoDatIdols].[dbo].[Person] WHERE ID = ?";
        jdbcTemplate.update(sql, userId.toString());
    }

    public void toggleUserBanStatus(UUID userId, boolean ban, String reason) {
        String sql = "UPDATE [WhoDatIdols].[dbo].[Person] SET isBanned = ?, banReason = ? " +
                (ban ? ", cookie = NULL " : "") +
                "WHERE ID = ?";
        jdbcTemplate.update(sql, ban ? 1 : 0, reason, userId.toString());
    }

    private void ensureAllowMessagesColumnExists() {
        try {
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('[WhoDatIdols].[dbo].[Person]') AND name = 'role') ALTER TABLE [WhoDatIdols].[dbo].[Person] ADD role NVARCHAR(50) DEFAULT 'USER'");
            jdbcTemplate.execute(
                    "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('[WhoDatIdols].[dbo].[Person]') AND name = 'allowMessages') ALTER TABLE [WhoDatIdols].[dbo].[Person] ADD allowMessages BIT DEFAULT 1");
            // Ensure existing users have it set correctly (SQL Server fix)
            jdbcTemplate
                    .execute("UPDATE [WhoDatIdols].[dbo].[Person] SET allowMessages = 1 WHERE allowMessages IS NULL");
            jdbcTemplate.execute("UPDATE [WhoDatIdols].[dbo].[Person] SET isBanned = 0 WHERE isBanned IS NULL");
            jdbcTemplate.execute("UPDATE [WhoDatIdols].[dbo].[Person] SET role = 'USER' WHERE role IS NULL");
        } catch (Exception e) {
            // Log or handle error if needed, but suppressed for auto-migration
        }
    }

    public void updateAllowMessages(UUID userId, boolean allow) {
        ensureAllowMessagesColumnExists();
        String sql = "UPDATE [WhoDatIdols].[dbo].[Person] SET allowMessages = ? WHERE ID = ?";
        jdbcTemplate.update(sql, allow ? 1 : 0, userId.toString());
    }

    public void updateAllowMessagesByCookie(String cookie, boolean allow) {
        ensureAllowMessagesColumnExists();
        String sql = "UPDATE [WhoDatIdols].[dbo].[Person] SET allowMessages = ? WHERE cookie = ?";
        jdbcTemplate.update(sql, allow ? 1 : 0, UUID.fromString(cookie));
    }

    public List<Person> searchUsersForMessaging(String query, String excludeNickname) {
        ensureAllowMessagesColumnExists();
        String sql = "SELECT ID, nickname, name, surname, profilePhoto, role FROM [WhoDatIdols].[dbo].[Person] " +
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
            p.setProfilePhoto(rs.getString("profilePhoto"));
            p.setRole(rs.getString("role"));
            return p;
        }, searchPattern, searchPattern, searchPattern, excludeNickname);
    }
}