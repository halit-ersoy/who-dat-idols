package com.ses.whodatidols.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/filters")
public class FilterController {

    private final JdbcTemplate jdbcTemplate;

    public FilterController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/options")
    public ResponseEntity<Map<String, Object>> getFilterOptions(
            @RequestParam(value = "type", required = true) String type) {
        Map<String, Object> options = new HashMap<>();

        // 1. Categories
        String categorySql = "SELECT ID, Name FROM Categories ORDER BY Name ASC";
        List<Map<String, Object>> categories = jdbcTemplate.query(categorySql, (rs, rowNum) -> {
            Map<String, Object> cat = new HashMap<>();
            cat.put("id", rs.getInt("ID"));
            cat.put("name", rs.getString("Name"));
            return cat;
        });
        options.put("categories", categories);

        // 2. Years
        String yearSql = "";
        if ("movie".equalsIgnoreCase(type)) {
            yearSql = "SELECT DISTINCT ReleaseYear as Year FROM Movie WHERE ReleaseYear IS NOT NULL ORDER BY ReleaseYear DESC";
        } else {
            // For series (Dizi, BL, Program), we get distinct years from their episodes or
            // assume they uploaded recently
            // Given the schema, we can look at Series.uploadDate year or
            // Episode.ReleaseYear.
            // A simpler approach: distinct year from max episode release year per series,
            // or just distinct ReleaseYear from Episode.
            yearSql = "SELECT DISTINCT ReleaseYear as Year FROM Episode WHERE ReleaseYear IS NOT NULL ORDER BY ReleaseYear DESC";
        }

        List<Integer> years = jdbcTemplate.queryForList(yearSql, Integer.class);
        options.put("years", years);

        // 3. Countries
        String countrySql = "";
        if ("movie".equalsIgnoreCase(type)) {
            countrySql = "SELECT DISTINCT Country FROM Movie WHERE Country IS NOT NULL AND Country != '' ORDER BY Country ASC";
        } else {
            countrySql = "SELECT DISTINCT Country FROM Series WHERE Country IS NOT NULL AND Country != '' ORDER BY Country ASC";
        }

        List<String> rawCountries = jdbcTemplate.queryForList(countrySql, String.class);
        // Clean up common country mappings
        List<Map<String, String>> countries = rawCountries.stream()
                .map(code -> {
                    Map<String, String> c = new HashMap<>();
                    c.put("code", code);
                    c.put("name", mapCountryCodeToName(code));
                    return c;
                })
                .collect(Collectors.toList());
        options.put("countries", countries);

        return ResponseEntity.ok(options);
    }

    private String mapCountryCodeToName(String code) {
        if (code == null)
            return "Bilinmiyor";
        String lower = code.trim().toLowerCase();
        switch (lower) {
            case "kr":
            case "kor":
                return "Güney Kore";
            case "jp":
            case "jpn":
                return "Japonya";
            case "cn":
            case "chn":
                return "Çin";
            case "th":
            case "tha":
                return "Tayland";
            case "tw":
            case "twn":
                return "Tayvan";
            case "ph":
            case "phl":
                return "Filipinler";
            case "id":
            case "idn":
                return "Endonezya";
            case "my":
            case "mys":
                return "Malezya";
            case "sg":
            case "sgp":
                return "Singapur";
            case "pl":
            case "pol":
                return "Polonya";
            case "us":
            case "usa":
                return "ABD";
            case "ca":
            case "can":
                return "Kanada";
            case "hk":
            case "hkg":
                return "Hong Kong";
            case "vn":
            case "vnm":
                return "Vietnam";
            case "tr":
            case "tur":
                return "Türkiye";
            case "gb":
            case "uk":
            case "gbr":
                return "Birleşik Krallık";
            case "de":
            case "deu":
                return "Almanya";
            case "fr":
            case "fra":
                return "Fransa";
            case "it":
            case "ita":
                return "İtalya";
            case "es":
            case "esp":
                return "İspanya";
            case "ru":
            case "rus":
                return "Rusya";
            case "at":
            case "aut":
                return "Avusturya";
            case "be":
            case "bel":
                return "Belçika";
            case "ch":
            case "che":
                return "İsviçre";
            case "ee":
            case "est":
                return "Estonya";
            case "in":
            case "ind":
                return "Hindistan";
            case "kh":
            case "khm":
                return "Kamboçya";
            case "nl":
            case "nld":
                return "Hollanda";
            default:
                return code.toUpperCase();
        }
    }
}
