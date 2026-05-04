package com.smarttask.util;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class CertificatePdfWriter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private CertificatePdfWriter() {
    }

    /**
     * Creates a simple one-page completion certificate.
     *
     * @param outputPath     destination file (parent directories must exist or be creatable)
     * @param userName       trainee name
     * @param formationTitle course title
     * @param completionDate date shown on the certificate
     */
    public static void write(java.nio.file.Path outputPath, String userName, String formationTitle,
                             LocalDate completionDate) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath");
        if (userName == null || userName.isBlank()) {
            userName = "Participant";
        }
        if (formationTitle == null || formationTitle.isBlank()) {
            formationTitle = "Formation";
        }
        LocalDate date = completionDate != null ? completionDate : LocalDate.now();

        java.nio.file.Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (PdfWriter writer = new PdfWriter(Files.newOutputStream(outputPath));
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            Paragraph title = new Paragraph("Certificate of Completion")
                    .setFontSize(22)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(24);
            document.add(title);

            document.add(new Paragraph("This certifies that").setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(userName)
                    .setBold()
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(8)
                    .setMarginBottom(8));
            document.add(new Paragraph("has successfully completed").setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(formationTitle)
                    .setBold()
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(8)
                    .setMarginBottom(24));

            String dateLine = "Completion date: " + date.format(DATE_FORMAT);
            document.add(new Paragraph(dateLine).setTextAlignment(TextAlignment.CENTER));
        }
    }
}
