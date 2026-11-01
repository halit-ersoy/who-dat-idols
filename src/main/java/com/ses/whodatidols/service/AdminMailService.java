package com.ses.whodatidols.service;

import com.ses.whodatidols.viewmodel.MailAccountDto;
import com.ses.whodatidols.viewmodel.MailDto;
import com.ses.whodatidols.viewmodel.MailFolderDto;
import com.ses.whodatidols.viewmodel.SendMailRequest;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class AdminMailService {

    private static final Logger logger = LoggerFactory.getLogger(AdminMailService.class);

    private final JavaMailSender defaultMailSender;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${mail.imap.port:993}")
    private int imapPort;

    @Value("${mail.imap.ssl.enable:true}")
    private boolean imapSsl;

    @Value("${mail.imap.connectiontimeout:8000}")
    private int connectionTimeout;

    @Value("${mail.imap.timeout:12000}")
    private int readTimeout;

    public static class AccountInfo {
        public final String id;
        public final String email;
        public final String displayName;
        public final String imapHost;
        public final String smtpHost;

        public AccountInfo(String id, String email, String displayName, String imapHost, String smtpHost) {
            this.id = id;
            this.email = email;
            this.displayName = displayName;
            this.imapHost = imapHost;
            this.smtpHost = smtpHost;
        }
    }

    private final AccountInfo ACC_WHODATIDOLS = new AccountInfo(
            "whodatidols",
            "info@whodatidols.com",
            "Who Dat Idols",
            "mail.whodatidols.com",
            "mail.whodatidols.com"
    );

    private final AccountInfo ACC_BLDIZI = new AccountInfo(
            "bldizi",
            "info@bldizi.com",
            "BL Dizi",
            "mail.whodatidols.com",
            "mail.whodatidols.com"
    );

    public AdminMailService(JavaMailSender defaultMailSender) {
        this.defaultMailSender = defaultMailSender;
    }

    /**
     * İstek yapılan domain veya seçilen hesaba göre doğru hesap konfigürasyonunu çözümler
     */
    public AccountInfo resolveAccount(String accountKey, HttpServletRequest request) {
        if (accountKey != null && !accountKey.isBlank()) {
            if ("bldizi".equalsIgnoreCase(accountKey.trim()) || accountKey.toLowerCase().contains("bldizi")) {
                return ACC_BLDIZI;
            }
            return ACC_WHODATIDOLS;
        }

        // Domain üzerinden otomatik algılama
        if (request != null) {
            String serverName = request.getServerName();
            if (serverName != null && serverName.toLowerCase(Locale.ROOT).contains("bldizi")) {
                return ACC_BLDIZI;
            }
        }

        return ACC_WHODATIDOLS;
    }

    /**
     * Kullanılabilir hesapları ve aktif olanı döner
     */
    public List<MailAccountDto> getAccounts(String currentAccount, HttpServletRequest request) {
        AccountInfo active = resolveAccount(currentAccount, request);
        List<MailAccountDto> list = new ArrayList<>();
        list.add(new MailAccountDto(ACC_WHODATIDOLS.id, ACC_WHODATIDOLS.email, ACC_WHODATIDOLS.displayName,
                ACC_WHODATIDOLS.imapHost, ACC_WHODATIDOLS.smtpHost, ACC_WHODATIDOLS.id.equals(active.id)));
        list.add(new MailAccountDto(ACC_BLDIZI.id, ACC_BLDIZI.email, ACC_BLDIZI.displayName,
                ACC_BLDIZI.imapHost, ACC_BLDIZI.smtpHost, ACC_BLDIZI.id.equals(active.id)));
        return list;
    }

    private Properties createImapProperties(AccountInfo acc) {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", acc.imapHost);
        props.put("mail.imaps.port", String.valueOf(imapPort));
        props.put("mail.imaps.ssl.enable", String.valueOf(imapSsl));
        props.put("mail.imaps.ssl.trust", "*");
        props.put("mail.imaps.connectiontimeout", String.valueOf(connectionTimeout));
        props.put("mail.imaps.timeout", String.valueOf(readTimeout));
        props.put("mail.imaps.partialfetch", "false");
        return props;
    }

    private Store openStore(AccountInfo acc) throws MessagingException {
        if (mailPassword == null || mailPassword.isBlank()) {
            throw new IllegalStateException("E-posta şifresi (MAIL_PASSWORD) ayarlanmamış.");
        }
        Session session = Session.getInstance(createImapProperties(acc), null);
        Store store = session.getStore("imaps");
        store.connect(acc.imapHost, imapPort, acc.email, mailPassword);
        return store;
    }

    /**
     * Standart mail klasörlerini listeler ve okunmamış sayılarını döner
     */
    public List<MailFolderDto> getFolders(String accountKey, HttpServletRequest request) {
        AccountInfo acc = resolveAccount(accountKey, request);
        List<MailFolderDto> folderDtos = new ArrayList<>();
        Store store = null;
        try {
            store = openStore(acc);
            Folder defaultFolder = store.getDefaultFolder();
            Folder[] serverFolders = defaultFolder.list("*");

            Map<String, Folder> folderMap = new HashMap<>();
            for (Folder f : serverFolders) {
                folderMap.put(f.getFullName().toLowerCase(Locale.ROOT), f);
            }

            // Standart klasör eşlemeleri
            addFolderIfExists(folderDtos, folderMap, "inbox", "INBOX", "Gelen Kutusu", "fa-inbox");
            addFolderIfExists(folderDtos, folderMap, "sent", "Sent", "Gönderilenler", "fa-paper-plane");
            addFolderIfExists(folderDtos, folderMap, "junk", "Spam", "Spam / İstenmeyen", "fa-shield-halflf");
            addFolderIfExists(folderDtos, folderMap, "trash", "Trash", "Çöp Kutusu", "fa-trash");

            if (folderDtos.isEmpty()) {
                Folder inbox = store.getFolder("INBOX");
                if (inbox.exists()) {
                    inbox.open(Folder.READ_ONLY);
                    folderDtos.add(new MailFolderDto("INBOX", "INBOX", "Gelen Kutusu", "fa-inbox",
                            inbox.getUnreadMessageCount(), inbox.getMessageCount()));
                    inbox.close(false);
                }
            }

        } catch (Exception e) {
            logger.warn("[{}] IMAP klasörleri listelenirken hata: {}", acc.id, e.getMessage());
            folderDtos.add(new MailFolderDto("INBOX", "INBOX", "Gelen Kutusu", "fa-inbox", 0, 0));
            folderDtos.add(new MailFolderDto("Sent", "Sent", "Gönderilenler", "fa-paper-plane", 0, 0));
            folderDtos.add(new MailFolderDto("Spam", "Spam", "Spam / İstenmeyen", "fa-shield-halflf", 0, 0));
            folderDtos.add(new MailFolderDto("Trash", "Trash", "Çöp Kutusu", "fa-trash", 0, 0));
        } finally {
            closeStoreSilently(store);
        }
        return folderDtos;
    }

    private void addFolderIfExists(List<MailFolderDto> list, Map<String, Folder> map,
                                   String searchKey, String folderId, String displayName, String icon) {
        Folder matched = null;
        for (Map.Entry<String, Folder> entry : map.entrySet()) {
            String name = entry.getKey();
            if (name.equals(searchKey) || name.endsWith("." + searchKey) || name.contains(searchKey)) {
                matched = entry.getValue();
                break;
            }
        }
        if (matched != null) {
            try {
                int unread = 0;
                int total = 0;
                if ((matched.getType() & Folder.HOLDS_MESSAGES) != 0) {
                    matched.open(Folder.READ_ONLY);
                    unread = matched.getUnreadMessageCount();
                    total = matched.getMessageCount();
                    matched.close(false);
                }
                list.add(new MailFolderDto(matched.getFullName(), matched.getName(), displayName, icon, unread, total));
            } catch (Exception ignored) {
                list.add(new MailFolderDto(matched.getFullName(), matched.getName(), displayName, icon, 0, 0));
            }
        } else if (folderId.equalsIgnoreCase("INBOX")) {
            list.add(new MailFolderDto("INBOX", "INBOX", "Gelen Kutusu", icon, 0, 0));
        }
    }

    /**
     * Belirtilen klasördeki mailleri sayfalı ve optimize şekilde çeker
     */
    public Map<String, Object> getMessages(String accountKey, String folderName, int page, int size, HttpServletRequest request) {
        AccountInfo acc = resolveAccount(accountKey, request);
        Map<String, Object> result = new HashMap<>();
        List<MailDto> mailDtos = new ArrayList<>();
        Store store = null;
        Folder folder = null;

        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;

        try {
            store = openStore(acc);
            folder = resolveFolder(store, folderName);
            folder.open(Folder.READ_ONLY);

            int totalMessages = folder.getMessageCount();
            int unreadCount = folder.getUnreadMessageCount();

            result.put("account", acc.id);
            result.put("accountEmail", acc.email);
            result.put("total", totalMessages);
            result.put("unread", unreadCount);
            result.put("page", page);
            result.put("size", size);
            result.put("totalPages", (int) Math.ceil((double) totalMessages / size));

            if (totalMessages > 0) {
                int end = totalMessages - (page * size);
                int start = Math.max(1, end - size + 1);

                if (end >= 1 && start <= totalMessages) {
                    Message[] messages = folder.getMessages(start, end);

                    FetchProfile fp = new FetchProfile();
                    fp.add(FetchProfile.Item.ENVELOPE);
                    fp.add(FetchProfile.Item.FLAGS);
                    fp.add(FetchProfile.Item.CONTENT_INFO);
                    if (folder instanceof UIDFolder) {
                        fp.add(UIDFolder.FetchProfileItem.UID);
                    }
                    folder.fetch(messages, fp);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

                    for (int i = messages.length - 1; i >= 0; i--) {
                        Message msg = messages[i];
                        MailDto dto = new MailDto();
                        if (folder instanceof UIDFolder uidFolder) {
                            dto.setUid(uidFolder.getUID(msg));
                        } else {
                            dto.setUid(msg.getMessageNumber());
                        }
                        dto.setMessageNumber(msg.getMessageNumber());
                        dto.setSubject(msg.getSubject() != null ? msg.getSubject() : "(Konusuz)");
                        dto.setFrom(formatAddresses(msg.getFrom()));
                        dto.setTo(formatAddresses(msg.getRecipients(Message.RecipientType.TO)));

                        Date date = msg.getSentDate() != null ? msg.getSentDate() : msg.getReceivedDate();
                        dto.setSentDate(date);
                        dto.setFormattedDate(date != null ? sdf.format(date) : "-");
                        dto.setRead(msg.isSet(Flags.Flag.SEEN));
                        dto.setFolder(folder.getFullName());

                        dto.setSnippet(extractSnippet(msg));
                        mailDtos.add(dto);
                    }
                }
            }

            result.put("messages", mailDtos);
            result.put("success", true);

        } catch (Exception e) {
            logger.error("[{}] E-postalar çekilirken hata: {}", acc.id, e.getMessage());
            result.put("success", false);
            result.put("message", "E-postalar alınamadı: " + e.getMessage());
            result.put("messages", Collections.emptyList());
            result.put("total", 0);
            result.put("totalPages", 0);
            result.put("account", acc.id);
            result.put("accountEmail", acc.email);
        } finally {
            closeFolderSilently(folder);
            closeStoreSilently(store);
        }

        return result;
    }

    /**
     * E-posta detayını ve sanitize edilmiş gövdesini getirir
     */
    public MailDto getMessageDetail(String accountKey, String folderName, long uid, HttpServletRequest request) {
        AccountInfo acc = resolveAccount(accountKey, request);
        Store store = null;
        Folder folder = null;
        try {
            store = openStore(acc);
            folder = resolveFolder(store, folderName);
            folder.open(Folder.READ_WRITE);

            Message msg = null;
            if (folder instanceof UIDFolder uidFolder) {
                msg = uidFolder.getMessageByUID(uid);
            }
            if (msg == null && uid <= Integer.MAX_VALUE) {
                msg = folder.getMessage((int) uid);
            }

            if (msg == null) {
                throw new IllegalArgumentException("E-posta bulunamadı (UID: " + uid + ")");
            }

            if (!msg.isSet(Flags.Flag.SEEN)) {
                msg.setFlag(Flags.Flag.SEEN, true);
            }

            MailDto dto = new MailDto();
            dto.setUid(uid);
            dto.setMessageNumber(msg.getMessageNumber());
            dto.setSubject(msg.getSubject() != null ? msg.getSubject() : "(Konusuz)");
            dto.setFrom(formatAddresses(msg.getFrom()));
            dto.setTo(formatAddresses(msg.getRecipients(Message.RecipientType.TO)));

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            Date date = msg.getSentDate() != null ? msg.getSentDate() : msg.getReceivedDate();
            dto.setSentDate(date);
            dto.setFormattedDate(date != null ? sdf.format(date) : "-");
            dto.setRead(true);
            dto.setFolder(folder.getFullName());

            BodyContent bodyContent = extractFullBody(msg);
            if (bodyContent.html != null && !bodyContent.html.isBlank()) {
                dto.setHtmlContent(sanitizeHtml(bodyContent.html));
            } else if (bodyContent.plainText != null) {
                dto.setHtmlContent("<div style='white-space: pre-wrap; font-family: sans-serif;'>" +
                        escapeHtml(bodyContent.plainText) + "</div>");
            } else {
                dto.setHtmlContent("<em>(E-posta içeriği boş)</em>");
            }
            dto.setPlainTextContent(bodyContent.plainText);
            dto.setHasAttachments(!bodyContent.attachmentNames.isEmpty());
            dto.setAttachmentNames(bodyContent.attachmentNames);

            return dto;

        } catch (Exception e) {
            logger.error("[{}] E-posta detayı alınırken hata: {}", acc.id, e.getMessage());
            throw new RuntimeException("E-posta detayı alınamadı: " + e.getMessage(), e);
        } finally {
            closeFolderSilently(folder);
            closeStoreSilently(store);
        }
    }

    /**
     * Seçilen hesap üzerinden e-posta gönderir ve IMAP Sent klasörüne kaydeder
     */
    public void sendMail(SendMailRequest request, HttpServletRequest httpRequest) {
        if (request.getTo() == null || request.getTo().isBlank()) {
            throw new IllegalArgumentException("Alıcı e-posta adresi boş olamaz.");
        }
        if (request.getSubject() == null || request.getSubject().isBlank()) {
            throw new IllegalArgumentException("E-posta konusu boş olamaz.");
        }

        AccountInfo acc = resolveAccount(request.getAccount(), httpRequest);

        try {
            JavaMailSender sender = createMailSenderForAccount(acc);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(acc.email, acc.displayName));

            String[] toAddresses = request.getTo().split("[,;]");
            for (int i = 0; i < toAddresses.length; i++) {
                toAddresses[i] = toAddresses[i].trim();
            }
            helper.setTo(toAddresses);

            if (request.getCc() != null && !request.getCc().isBlank()) {
                helper.setCc(request.getCc().split("[,;]"));
            }
            if (request.getBcc() != null && !request.getBcc().isBlank()) {
                helper.setBcc(request.getBcc().split("[,;]"));
            }

            helper.setSubject(request.getSubject());
            helper.setText(request.getBody(), request.isHtml());
            helper.setSentDate(new Date());

            // 1. SMTP ile gönder
            sender.send(message);
            logger.info("[{}] E-posta başarıyla gönderildi: Kime={}", acc.id, request.getTo());

            // 2. Sent klasörüne kaydet
            saveToSentFolderSilently(acc, message);

        } catch (Exception e) {
            logger.error("[{}] E-posta gönderim hatası: {}", acc.id, e.getMessage(), e);
            throw new RuntimeException("E-posta gönderilemedi: " + e.getMessage(), e);
        }
    }

    private JavaMailSender createMailSenderForAccount(AccountInfo acc) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(acc.smtpHost);
        sender.setPort(587);
        sender.setUsername(acc.email);
        sender.setPassword(mailPassword);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.checkserveridentity", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        return sender;
    }

    /**
     * E-postayı siler / çöpe taşır
     */
    public void deleteMessage(String accountKey, String folderName, long uid, HttpServletRequest request) {
        AccountInfo acc = resolveAccount(accountKey, request);
        Store store = null;
        Folder folder = null;
        try {
            store = openStore(acc);
            folder = resolveFolder(store, folderName);
            folder.open(Folder.READ_WRITE);

            Message msg = getMessageByUid(folder, uid);
            if (msg == null) throw new IllegalArgumentException("E-posta bulunamadı.");

            if (isTrashFolder(folderName)) {
                msg.setFlag(Flags.Flag.DELETED, true);
            } else {
                Folder trashFolder = findTrashFolder(store);
                if (trashFolder != null && trashFolder.exists()) {
                    trashFolder.open(Folder.READ_WRITE);
                    folder.copyMessages(new Message[]{msg}, trashFolder);
                    trashFolder.close(false);
                    msg.setFlag(Flags.Flag.DELETED, true);
                } else {
                    msg.setFlag(Flags.Flag.DELETED, true);
                }
            }
        } catch (Exception e) {
            logger.error("[{}] Mesaj silinemedi: {}", acc.id, e.getMessage());
            throw new RuntimeException("Silme işlemi başarısız: " + e.getMessage(), e);
        } finally {
            closeFolderSilently(folder, true);
            closeStoreSilently(store);
        }
    }

    /**
     * E-postayı spama taşır
     */
    public void moveToSpam(String accountKey, String folderName, long uid, HttpServletRequest request) {
        AccountInfo acc = resolveAccount(accountKey, request);
        Store store = null;
        Folder folder = null;
        try {
            store = openStore(acc);
            folder = resolveFolder(store, folderName);
            folder.open(Folder.READ_WRITE);

            Message msg = getMessageByUid(folder, uid);
            if (msg == null) throw new IllegalArgumentException("E-posta bulunamadı.");

            Folder spamFolder = findSpamFolder(store);
            if (spamFolder != null && spamFolder.exists()) {
                spamFolder.open(Folder.READ_WRITE);
                folder.copyMessages(new Message[]{msg}, spamFolder);
                spamFolder.close(false);
                msg.setFlag(Flags.Flag.DELETED, true);
            } else {
                throw new IllegalStateException("Spam/Junk klasörü bulunamadı.");
            }
        } catch (Exception e) {
            logger.error("[{}] Mesaj spama taşınamadı: {}", acc.id, e.getMessage());
            throw new RuntimeException("Spam işlemi başarısız: " + e.getMessage(), e);
        } finally {
            closeFolderSilently(folder, true);
            closeStoreSilently(store);
        }
    }

    /**
     * E-posta okundu durumunu günceller
     */
    public void markAsRead(String accountKey, String folderName, long uid, boolean read, HttpServletRequest request) {
        AccountInfo acc = resolveAccount(accountKey, request);
        Store store = null;
        Folder folder = null;
        try {
            store = openStore(acc);
            folder = resolveFolder(store, folderName);
            folder.open(Folder.READ_WRITE);

            Message msg = getMessageByUid(folder, uid);
            if (msg != null) {
                msg.setFlag(Flags.Flag.SEEN, read);
            }
        } catch (Exception e) {
            logger.error("[{}] Okundu durumu güncellenemedi: {}", acc.id, e.getMessage());
            throw new RuntimeException("Durum güncellenemedi: " + e.getMessage(), e);
        } finally {
            closeFolderSilently(folder);
            closeStoreSilently(store);
        }
    }

    // --- YARDIMCI METODLAR ---

    private Message getMessageByUid(Folder folder, long uid) throws MessagingException {
        if (folder instanceof UIDFolder uidFolder) {
            return uidFolder.getMessageByUID(uid);
        }
        if (uid <= Integer.MAX_VALUE) {
            return folder.getMessage((int) uid);
        }
        return null;
    }

    private Folder resolveFolder(Store store, String folderName) throws MessagingException {
        if (folderName == null || folderName.isBlank() || folderName.equalsIgnoreCase("inbox")) {
            return store.getFolder("INBOX");
        }
        Folder f = store.getFolder(folderName);
        if (!f.exists()) {
            Folder[] all = store.getDefaultFolder().list("*");
            for (Folder item : all) {
                if (item.getName().equalsIgnoreCase(folderName) || item.getFullName().equalsIgnoreCase(folderName)) {
                    return item;
                }
            }
        }
        return f;
    }

    private Folder findTrashFolder(Store store) throws MessagingException {
        Folder[] all = store.getDefaultFolder().list("*");
        for (Folder f : all) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (n.contains("trash") || n.contains("silinen") || n.contains("deleted")) {
                return f;
            }
        }
        return null;
    }

    private Folder findSpamFolder(Store store) throws MessagingException {
        Folder[] all = store.getDefaultFolder().list("*");
        for (Folder f : all) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (n.contains("spam") || n.contains("junk") || n.contains("önemsiz")) {
                return f;
            }
        }
        return null;
    }

    private boolean isTrashFolder(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("trash") || lower.contains("deleted") || lower.contains("silinen");
    }

    private void saveToSentFolderSilently(AccountInfo acc, MimeMessage message) {
        Store store = null;
        Folder sentFolder = null;
        try {
            store = openStore(acc);
            Folder[] all = store.getDefaultFolder().list("*");
            for (Folder f : all) {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.contains("sent") || n.contains("gönderil")) {
                    sentFolder = f;
                    break;
                }
            }
            if (sentFolder != null && sentFolder.exists()) {
                sentFolder.open(Folder.READ_WRITE);
                message.setFlag(Flags.Flag.SEEN, true);
                sentFolder.appendMessages(new Message[]{message});
            }
        } catch (Exception ex) {
            logger.debug("[{}] Gönderilen e-posta Sent klasörüne kopyalanamadı: {}", acc.id, ex.getMessage());
        } finally {
            closeFolderSilently(sentFolder);
            closeStoreSilently(store);
        }
    }

    private String formatAddresses(Address[] addresses) {
        if (addresses == null || addresses.length == 0) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < addresses.length; i++) {
            if (i > 0) sb.append(", ");
            Address addr = addresses[i];
            if (addr instanceof InternetAddress ia) {
                if (ia.getPersonal() != null && !ia.getPersonal().isBlank()) {
                    sb.append(ia.getPersonal()).append(" <").append(ia.getAddress()).append(">");
                } else {
                    sb.append(ia.getAddress());
                }
            } else {
                sb.append(addr.toString());
            }
        }
        return sb.toString();
    }

    private String extractSnippet(Message msg) {
        try {
            BodyContent bc = extractFullBody(msg);
            String text = bc.plainText;
            if (text == null && bc.html != null) {
                text = stripHtmlTags(bc.html);
            }
            if (text != null) {
                text = text.replaceAll("\\s+", " ").trim();
                return text.length() > 120 ? text.substring(0, 117) + "..." : text;
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static class BodyContent {
        String plainText;
        String html;
        List<String> attachmentNames = new ArrayList<>();
    }

    private BodyContent extractFullBody(Message msg) throws IOException, MessagingException {
        BodyContent bc = new BodyContent();
        extractPart(msg, bc);
        return bc;
    }

    private void extractPart(Part part, BodyContent bc) throws IOException, MessagingException {
        String disposition = part.getDisposition();

        if (Part.ATTACHMENT.equalsIgnoreCase(disposition) || part.getFileName() != null) {
            bc.attachmentNames.add(part.getFileName() != null ? part.getFileName() : "ek-dosya");
            return;
        }

        if (part.isMimeType("text/plain")) {
            if (bc.plainText == null) {
                bc.plainText = part.getContent().toString();
            }
        } else if (part.isMimeType("text/html")) {
            if (bc.html == null) {
                bc.html = part.getContent().toString();
            }
        } else if (part.isMimeType("multipart/*")) {
            MimeMultipart mp = (MimeMultipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                extractPart(mp.getBodyPart(i), bc);
            }
        }
    }

    /**
     * HTML etiketlerini regex ile ayıklar (Snippet ve düz metin görünümü için)
     */
    private String stripHtmlTags(String html) {
        if (html == null) return "";
        String cleaned = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(html).replaceAll("");
        cleaned = Pattern.compile("<style[^>]*>.*?</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(cleaned).replaceAll("");
        cleaned = cleaned.replaceAll("<[^>]+>", " ");
        return cleaned.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    /**
     * Güvenlik: Gelen e-postadaki HTML'i saf Java ile sanitize eder
     */
    private String sanitizeHtml(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) return "";

        String html = rawHtml;

        Pattern dangerousTags = Pattern.compile(
                "<(script|iframe|object|embed|applet|meta|link|base|form)[^>]*>.*?</\\1>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        html = dangerousTags.matcher(html).replaceAll("");

        Pattern singleDangerous = Pattern.compile(
                "</?(script|iframe|object|embed|applet|meta|link|base|form)[^>]*>",
                Pattern.CASE_INSENSITIVE
        );
        html = singleDangerous.matcher(html).replaceAll("");

        Pattern onEvents = Pattern.compile("\\s+on[a-zA-Z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", Pattern.CASE_INSENSITIVE);
        html = onEvents.matcher(html).replaceAll("");

        Pattern jsProtocols = Pattern.compile("(href|src)\\s*=\\s*([\"']?)(javascript|vbscript|data:text/html):", Pattern.CASE_INSENSITIVE);
        html = jsProtocols.matcher(html).replaceAll("$1=$2unsafe-blocked:");

        Pattern linkPattern = Pattern.compile("<a\\s+(?![^>]*target=)([^>]+)>", Pattern.CASE_INSENSITIVE);
        html = linkPattern.matcher(html).replaceAll("<a target=\"_blank\" rel=\"noopener noreferrer\" $1>");

        return html;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private void closeFolderSilently(Folder folder) {
        closeFolderSilently(folder, false);
    }

    private void closeFolderSilently(Folder folder, boolean expunge) {
        if (folder != null && folder.isOpen()) {
            try {
                folder.close(expunge);
            } catch (Exception ignored) {}
        }
    }

    private void closeStoreSilently(Store store) {
        if (store != null && store.isConnected()) {
            try {
                store.close();
            } catch (Exception ignored) {}
        }
    }
}
