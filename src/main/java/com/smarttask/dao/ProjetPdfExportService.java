package com.smarttask.dao;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import com.smarttask.AppContext;
import com.smarttask.model.Projet;
import com.smarttask.model.Tache;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ProjetPdfExportService {
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN = 36f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);
    private static final DateTimeFormatter GENERATED_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final PDFont FONT_HELVETICA = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_HELVETICA_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont FONT_HELVETICA_OBLIQUE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    public Path exportProjects(List<Projet> projets, Path outputFile) {
        List<Projet> sortedProjects = new ArrayList<>(projets);
        sortedProjects.sort(Comparator.comparing(Projet::getNom, String.CASE_INSENSITIVE_ORDER));

        try (PDDocument document = new PDDocument()) {
            PdfCursor cursor = openPage(document, "Export PDF des projets", "Rapport detaille avec taches et indicateurs");
            drawSummarySection(cursor, sortedProjects);

            boolean firstProject = true;
            for (Projet projet : sortedProjects) {
                List<Tache> taches = new ArrayList<>(AppContext.tacheRepository().findByProjetId(projet.getId()));
                taches.sort(Comparator.comparing(Tache::getDateLimite).thenComparing(Tache::getLibelle, String.CASE_INSENSITIVE_ORDER));

                if (!firstProject) {
                    cursor.close();
                    cursor = openPage(document, "Export PDF des projets", "Suite du rapport");
                }
                firstProject = false;

                drawProjectSection(cursor, projet, taches);
            }

            cursor.close();
            document.save(outputFile.toFile());
            return outputFile;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export projects to PDF", e);
        }
    }

    private PdfCursor openPage(PDDocument document, String title, String subtitle) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        PDPageContentStream content = new PDPageContentStream(document, page);
        PdfCursor cursor = new PdfCursor(document, page, content, PAGE_HEIGHT - MARGIN);
        drawHeader(cursor, title, subtitle);
        return cursor;
    }

    private void drawHeader(PdfCursor cursor, String title, String subtitle) throws IOException {
        cursor.content.setNonStrokingColor(new Color(6, 106, 201));
        cursor.content.addRect(0, PAGE_HEIGHT - 96f, PAGE_WIDTH, 96f);
        cursor.content.fill();

        cursor.content.setNonStrokingColor(Color.WHITE);
        cursor.content.beginText();
        cursor.content.setFont(FONT_HELVETICA_BOLD, 22);
        cursor.content.newLineAtOffset(MARGIN, PAGE_HEIGHT - 58f);
        cursor.content.showText(title);
        cursor.content.endText();

        cursor.content.beginText();
        cursor.content.setFont(FONT_HELVETICA, 11);
        cursor.content.newLineAtOffset(MARGIN, PAGE_HEIGHT - 76f);
        cursor.content.showText(subtitle + "  |  Genere le " + LocalDateTime.now().format(GENERATED_FORMATTER));
        cursor.content.endText();

        cursor.y = PAGE_HEIGHT - 120f;
    }

    private void drawSummarySection(PdfCursor cursor, List<Projet> projets) throws IOException {
        float cardWidth = (CONTENT_WIDTH - 18f) / 4f;
        float cardHeight = 70f;
        float startY = cursor.y;
        float[] positions = {MARGIN, MARGIN + cardWidth + 6f, MARGIN + (cardWidth + 6f) * 2f, MARGIN + (cardWidth + 6f) * 3f};

        SummaryMetric[] metrics = {
                new SummaryMetric("Projets", String.valueOf(projets.size()), new Color(232, 240, 254)),
                new SummaryMetric("En cours", String.valueOf(countProjectsByStatus(projets, "actif")), new Color(219, 234, 254)),
                new SummaryMetric("Termines", String.valueOf(countProjectsByStatus(projets, "termine")), new Color(220, 252, 231)),
                new SummaryMetric("Taches", String.valueOf(countAllTasks(projets)), new Color(254, 243, 199))
        };

        for (int i = 0; i < metrics.length; i++) {
            drawMetricCard(cursor, positions[i], startY - cardHeight, cardWidth, cardHeight, metrics[i]);
        }

        cursor.y = startY - cardHeight - 26f;
    }

    private void drawMetricCard(PdfCursor cursor, float x, float y, float width, float height, SummaryMetric metric) throws IOException {
        cursor.content.setNonStrokingColor(metric.background());
        cursor.content.addRect(x, y, width, height);
        cursor.content.fill();

        cursor.content.setStrokingColor(new Color(210, 218, 228));
        cursor.content.addRect(x, y, width, height);
        cursor.content.stroke();

        cursor.content.setNonStrokingColor(new Color(31, 41, 55));
        cursor.content.beginText();
        cursor.content.setFont(FONT_HELVETICA_BOLD, 10);
        cursor.content.newLineAtOffset(x + 12f, y + height - 18f);
        cursor.content.showText(metric.label().toUpperCase(Locale.ROOT));
        cursor.content.endText();

        cursor.content.beginText();
        cursor.content.setFont(FONT_HELVETICA_BOLD, 22);
        cursor.content.newLineAtOffset(x + 12f, y + 18f);
        cursor.content.showText(metric.value());
        cursor.content.endText();
    }

    private void drawProjectSection(PdfCursor cursor, Projet projet, List<Tache> taches) throws IOException {
        ensureSpace(cursor, 170f + (taches.size() * 16f));

        float sectionTop = cursor.y;
        float sectionHeight = 130f;
        Color accent = statusColor(projet.getStatut());

        cursor.content.setNonStrokingColor(new Color(248, 250, 252));
        cursor.content.addRect(MARGIN, sectionTop - sectionHeight, CONTENT_WIDTH, sectionHeight);
        cursor.content.fill();

        cursor.content.setStrokingColor(new Color(221, 224, 227));
        cursor.content.addRect(MARGIN, sectionTop - sectionHeight, CONTENT_WIDTH, sectionHeight);
        cursor.content.stroke();

        cursor.content.setNonStrokingColor(accent);
        cursor.content.addRect(MARGIN, sectionTop - 18f, CONTENT_WIDTH, 18f);
        cursor.content.fill();

        writeText(cursor, projet.getNom(), FONT_HELVETICA_BOLD, 18, MARGIN + 16f, sectionTop - 44f, new Color(29, 59, 83));
        writeText(cursor, formatStatus(projet.getStatut()), FONT_HELVETICA_BOLD, 10, PAGE_WIDTH - 120f, sectionTop - 40f, accent);
        writeWrappedText(cursor, projet.getDescription(), FONT_HELVETICA, 11, MARGIN + 16f, sectionTop - 62f, CONTENT_WIDTH - 32f, 13f, new Color(55, 65, 81));

        drawBadge(cursor, MARGIN + 16f, sectionTop - 100f, 112f, 20f, "Debut: " + projet.getDateDebut().format(DATE_FORMATTER), new Color(219, 234, 254), new Color(30, 64, 175));
        drawBadge(cursor, MARGIN + 136f, sectionTop - 100f, 138f, 20f, "Echeance: " + projet.getDateEcheance().format(DATE_FORMATTER), new Color(254, 243, 199), new Color(180, 83, 9));
        drawBadge(cursor, MARGIN + 282f, sectionTop - 100f, 122f, 20f, taches.isEmpty() ? "0 tache" : taches.size() + " taches", new Color(220, 252, 231), new Color(22, 101, 52));

        float tasksStart = sectionTop - 130f;
        writeText(cursor, "Taches associees", FONT_HELVETICA_BOLD, 13, MARGIN + 16f, tasksStart, new Color(29, 59, 83));
        float taskY = tasksStart - 18f;

        if (taches.isEmpty()) {
            writeText(cursor, "Aucune tache associee a ce projet.", FONT_HELVETICA_OBLIQUE, 11, MARGIN + 20f, taskY, new Color(107, 114, 128));
            cursor.y = taskY - 22f;
            return;
        }

        for (Tache tache : taches) {
            ensureSpace(cursor, 18f);
            String line = "- " + tache.getLibelle() + "  |  Priorite: " + capitalize(tache.getPriorite()) + "  |  Etat: "
                    + formatTaskState(tache.getEtat()) + "  |  Limite: " + tache.getDateLimite().format(DATE_FORMATTER);
            writeWrappedText(cursor, line, FONT_HELVETICA, 10, MARGIN + 24f, taskY, CONTENT_WIDTH - 40f, 12f, new Color(55, 65, 81));
            taskY -= 14f;
        }

        cursor.y = taskY - 10f;
    }

    private void ensureSpace(PdfCursor cursor, float requiredHeight) throws IOException {
        if (cursor.y - requiredHeight >= MARGIN) {
            return;
        }

        cursor.content.close();
        PDPage page = new PDPage(PDRectangle.A4);
        cursor.document.addPage(page);
        cursor.page = page;
        cursor.content = new PDPageContentStream(cursor.document, page);
        cursor.y = PAGE_HEIGHT - MARGIN;
        drawHeader(cursor, "Export PDF des projets", "Suite du rapport");
    }

    private void drawBadge(PdfCursor cursor, float x, float y, float width, float height, String text, Color background, Color foreground) throws IOException {
        cursor.content.setNonStrokingColor(background);
        cursor.content.addRect(x, y, width, height);
        cursor.content.fill();

        cursor.content.setStrokingColor(background.darker());
        cursor.content.addRect(x, y, width, height);
        cursor.content.stroke();

        cursor.content.setNonStrokingColor(foreground);
        cursor.content.beginText();
        cursor.content.setFont(FONT_HELVETICA_BOLD, 9);
        cursor.content.newLineAtOffset(x + 7f, y + 6f);
        cursor.content.showText(text);
        cursor.content.endText();
    }

    private void writeText(PdfCursor cursor, String text, PDFont font, float size, float x, float y, Color color) throws IOException {
        if (text == null || text.isBlank()) {
            return;
        }

        cursor.content.setNonStrokingColor(color);
        cursor.content.beginText();
        cursor.content.setFont(font, size);
        cursor.content.newLineAtOffset(x, y);
        cursor.content.showText(sanitize(text));
        cursor.content.endText();
    }

    private void writeWrappedText(PdfCursor cursor, String text, PDFont font, float size, float x, float y, float maxWidth, float lineSpacing, Color color) throws IOException {
        if (text == null || text.isBlank()) {
            return;
        }

        cursor.content.setNonStrokingColor(color);
        List<String> lines = wrapText(text, font, size, maxWidth);
        float currentY = y;
        for (String line : lines) {
            cursor.content.beginText();
            cursor.content.setFont(font, size);
            cursor.content.newLineAtOffset(x, currentY);
            cursor.content.showText(sanitize(line));
            cursor.content.endText();
            currentY -= lineSpacing;
        }
    }

    private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (stringWidth(font, fontSize, candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (current.length() > 0) {
                    lines.add(current.toString());
                }
                current.setLength(0);
                current.append(word);
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        return lines;
    }

    private float stringWidth(PDFont font, float fontSize, String text) throws IOException {
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    private String sanitize(String text) {
        return text.replace("\n", " ").replace("\r", " ");
    }

    private String formatStatus(String status) {
        return switch (normalize(status)) {
            case "actif" -> "Statut: Actif";
            case "termine" -> "Statut: Termine";
            default -> "Statut: En attente";
        };
    }

    private String formatTaskState(String state) {
        return switch (normalize(state)) {
            case "en_cours" -> "En cours";
            case "termine" -> "Termine";
            default -> "A faire";
        };
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Color statusColor(String status) {
        return switch (normalize(status)) {
            case "actif" -> new Color(37, 99, 235);
            case "termine" -> new Color(16, 185, 129);
            default -> new Color(245, 158, 11);
        };
    }

    private long countProjectsByStatus(List<Projet> projets, String status) {
        return projets.stream().filter(projet -> normalize(projet.getStatut()).equals(status)).count();
    }

    private long countAllTasks(List<Projet> projets) {
        return projets.stream()
                .mapToLong(projet -> AppContext.tacheRepository().findByProjetId(projet.getId()).size())
                .sum();
    }

    private static final class PdfCursor {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream content;
        private float y;

        private PdfCursor(PDDocument document, PDPage page, PDPageContentStream content, float y) {
            this.document = document;
            this.page = page;
            this.content = content;
            this.y = y;
        }

        private void close() throws IOException {
            content.close();
        }
    }

    private record SummaryMetric(String label, String value, Color background) {
    }
}
