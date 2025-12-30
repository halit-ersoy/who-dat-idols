package com.ses.whodatidols.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class SoapOpera {
    private UUID id;
    private String name;      // Dizi Adı
    private String category;
    private String content;   // Özet (Ana Tablo: _content)
    private String language;  // Dil (Ana Tablo: country)
    private int finalStatus;  // Final yaptı mı? (1: Evet, 0: Hayır)
    private String xmlData;   // XML Yapısı (soapOperaSeries)

    // Bölüm Detayları (Child Table)
    private int time;         // Süre
    private int year;         // Yıl
    private LocalDateTime uploadDate;

    // Formdan Gelen Ekstra Veriler (Veritabanında sütunu yok, işlem için lazım)
    private int seasonNumber;
    private int episodeNumber;

    // Getter ve Setter'lar
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(int finalStatus) {
        this.finalStatus = finalStatus;
    }

    public String getXmlData() {
        return xmlData;
    }

    public void setXmlData(String xmlData) {
        this.xmlData = xmlData;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(int seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(int episodeNumber) {
        this.episodeNumber = episodeNumber;
    }
}