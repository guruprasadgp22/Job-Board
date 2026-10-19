package com.guru.jobboard.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import com.guru.jobboard.dto.ResumeDto;
import com.guru.jobboard.entity.Preference;

/** Stores a user's resume, extracts skills from it, and re-scores their jobs. */
@Service
public class ResumeService {

    private static final int MAX_TEXT = 60_000;
    /** How many top resume skills become search keywords when the user has none yet. */
    private static final int AUTO_KEYWORDS = 6;

    private final PreferenceService preferences;
    private final SkillCatalog catalog;
    private final JobSyncService syncService;

    public ResumeService(PreferenceService preferences, SkillCatalog catalog, JobSyncService syncService) {
        this.preferences = preferences;
        this.catalog = catalog;
        this.syncService = syncService;
    }

    public ResumeDto get(long userId) {
        return toDto(preferences.get(userId));
    }

    public ResumeDto upload(long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a file to upload");
        }
        String name = file.getOriginalFilename() == null ? "resume" : file.getOriginalFilename();
        String text;
        try {
            text = extractText(name, file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read this file: " + e.getMessage());
        }
        if (text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No text found in this file. If it is a scanned PDF, paste your resume text instead.");
        }
        return saveText(userId, text, name);
    }

    public ResumeDto saveText(long userId, String text, String fileName) {
        String clean = text.replace("\r", "").replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
        if (clean.length() > MAX_TEXT) {
            clean = clean.substring(0, MAX_TEXT);
        }
        List<String> skills = catalog.extractRanked(clean);
        Preference p = preferences.get(userId);
        p.setResumeText(clean);
        p.setResumeFileName(fileName);
        p.setResumeSkills(String.join(", ", skills));
        p.setResumeUpdatedAt(Instant.now());
        if (p.getKeywords() == null || p.getKeywords().isBlank()) {
            p.setKeywords(String.join(", ", skills.stream().limit(AUTO_KEYWORDS).toList()));
        }
        preferences.save(p);
        syncService.rescore(userId);
        return toDto(p);
    }

    /** User edited the extracted skill list. */
    public ResumeDto updateSkills(long userId, List<String> skills) {
        Preference p = preferences.get(userId);
        p.setResumeSkills(String.join(", ", catalog.normalize(skills)));
        preferences.save(p);
        syncService.rescore(userId);
        return toDto(p);
    }

    /** Replace search keywords with the top resume skills. */
    public Preference useSkillsAsKeywords(long userId, int count) {
        Preference p = preferences.get(userId);
        List<String> skills = split(p.getResumeSkills());
        p.setKeywords(String.join(", ", skills.stream().limit(Math.clamp(count, 1, 15)).toList()));
        preferences.save(p);
        syncService.rescore(userId);
        return p;
    }

    public void delete(long userId) {
        Preference p = preferences.get(userId);
        p.setResumeText(null);
        p.setResumeFileName(null);
        p.setResumeSkills(null);
        p.setResumeUpdatedAt(null);
        preferences.save(p);
        syncService.rescore(userId);
    }

    static String extractText(String fileName, byte[] bytes) throws IOException {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            try (PDDocument doc = Loader.loadPDF(bytes)) {
                return new PDFTextStripper().getText(doc);
            }
        }
        if (lower.endsWith(".docx")) {
            return docxText(bytes);
        }
        if (lower.endsWith(".txt") || lower.endsWith(".md")) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload a PDF, DOCX or TXT file");
    }

    /** A .docx is a zip; the text lives in word/document.xml. */
    private static String docxText(byte[] bytes) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    String text = xml.replaceAll("</w:p>", "\n")
                            .replaceAll("<w:tab/>", "\t")
                            .replaceAll("<w:br/>", "\n")
                            .replaceAll("<[^>]+>", "");
                    return HtmlUtils.htmlUnescape(text);
                }
            }
        }
        throw new IOException("not a valid .docx file");
    }

    private static List<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static ResumeDto toDto(Preference p) {
        return new ResumeDto(p.getResumeFileName(), p.getResumeText(), split(p.getResumeSkills()),
                p.getResumeUpdatedAt(), p.getKeywords());
    }
}
