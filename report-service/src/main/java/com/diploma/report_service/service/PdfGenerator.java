package com.diploma.report_service.service;

import com.diploma.report_service.client.ReviewClient;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class PdfGenerator {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMMM yyyy 'в' HH:mm", new Locale("ru"))
                    .withZone(ZoneId.systemDefault());

    @Value("${app.report.font-path}")
    private String fontPath;

    @Value("${app.report.font-bold-path}")
    private String fontBoldPath;

    @Value("${app.report.font-mono-path}")
    private String fontMonoPath;

    private Font titleFont;
    private Font sectionFont;
    private Font normalFont;
    private Font smallFont;
    private Font monoFont;
    private Font boldFont;

    @PostConstruct
    void init() {
        BaseFont regular = loadFont(fontPath, "DejaVuSans");
        BaseFont bold = loadFont(fontBoldPath, "DejaVuSans-Bold");
        BaseFont mono = loadFont(fontMonoPath, "DejaVuSansMono");

        BaseFont regularEffective = regular != null ? regular : fallback();
        BaseFont boldEffective = bold != null ? bold : regularEffective;
        BaseFont monoEffective = mono != null ? mono : regularEffective;

        titleFont = new Font(boldEffective, 20, Font.BOLD);
        sectionFont = new Font(boldEffective, 13, Font.BOLD);
        normalFont = new Font(regularEffective, 11);
        smallFont = new Font(regularEffective, 9, Font.NORMAL, Color.DARK_GRAY);
        monoFont = new Font(monoEffective, 9);
        boldFont = new Font(boldEffective, 11, Font.BOLD);
    }

    private BaseFont loadFont(String path, String name) {
        if (path == null || path.isBlank()) return null;
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            log.warn("Font file not found at {} (for {}) — Cyrillic text may render as boxes", path, name);
            return null;
        }
        try {
            return BaseFont.createFont(p.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            log.warn("Failed to load font {} from {}: {}", name, path, e.getMessage());
            return null;
        }
    }

    private BaseFont fallback() {
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create fallback Helvetica font", e);
        }
    }

    public byte[] render(ReportContext ctx) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 48, 48);
        PdfWriter.getInstance(doc, out);
        doc.open();

        writeTitle(doc, ctx);
        writeSessionMeta(doc, ctx);
        writeScores(doc, ctx);
        writeVerdict(doc, ctx);
        writeSummary(doc, ctx);
        writeComments(doc, ctx);
        writeCode(doc, ctx);

        doc.close();
        return out.toByteArray();
    }

    private void writeTitle(Document doc, ReportContext ctx) {
        Paragraph p = new Paragraph("Отчёт по техническому интервью", titleFont);
        p.setSpacingAfter(8f);
        doc.add(p);

        Paragraph sub = new Paragraph(
                "Сгенерирован " + DATE_FORMAT.format(java.time.Instant.now()),
                smallFont
        );
        sub.setSpacingAfter(18f);
        doc.add(sub);
    }

    private void writeSessionMeta(Document doc, ReportContext ctx) {
        doc.add(section("Сессия"));
        PdfPTable t = newTable(new float[]{1f, 3f});
        addRow(t, "Название",     str(ctx.session().getTitle()));
        addRow(t, "Язык",         str(ctx.session().getLanguage()));
        addRow(t, "Задача",       str(ctx.session().getTaskTitle()));
        addRow(t, "Запланирована", formatInstant(ctx.session().getScheduledAt()));
        addRow(t, "Начата",        formatInstant(ctx.session().getStartedAt()));
        addRow(t, "Завершена",     formatInstant(ctx.session().getCompletedAt()));
        addRow(t, "Интервьюер",    displayPerson(ctx.interviewer(), ctx.session().getInterviewerId()));
        addRow(t, "Кандидат",      displayPerson(ctx.candidate(), ctx.session().getCandidateId()));
        t.setSpacingAfter(14f);
        doc.add(t);
    }

    private void writeScores(Document doc, ReportContext ctx) {
        doc.add(section("Оценки"));
        PdfPTable t = newTable(new float[]{3f, 1f});
        ReviewClient.ReviewInfo r = ctx.review();
        addRow(t, "Корректность",      formatScore(r.getScoreCorrectness()));
        addRow(t, "Чистота кода",      formatScore(r.getScoreCodeQuality()));
        addRow(t, "Производительность", formatScore(r.getScorePerformance()));
        addRow(t, "Коммуникация",      formatScore(r.getScoreCommunication()));
        if (r.getScoreAverage() != null) {
            addRow(t, "Средняя",           String.format(Locale.US, "%.1f / 5", r.getScoreAverage()));
        }
        t.setSpacingAfter(14f);
        doc.add(t);
    }

    private void writeVerdict(Document doc, ReportContext ctx) {
        doc.add(section("Вердикт"));
        String verdict = mapVerdict(ctx.review().getVerdict());
        Paragraph p = new Paragraph(verdict, new Font(boldFont.getBaseFont(), 16, Font.BOLD));
        p.setSpacingAfter(14f);
        doc.add(p);
    }

    private void writeSummary(Document doc, ReportContext ctx) {
        String summary = ctx.review().getSummary();
        if (summary == null || summary.isBlank()) return;
        doc.add(section("Итоговый комментарий"));
        for (String line : summary.split("\\R")) {
            Paragraph p = new Paragraph(line, normalFont);
            p.setSpacingAfter(2f);
            doc.add(p);
        }
        doc.add(new Paragraph(" ", smallFont));
    }

    private void writeComments(Document doc, ReportContext ctx) {
        List<ReviewClient.ReviewInfo.Comment> comments = ctx.review().getComments();
        if (comments == null || comments.isEmpty()) return;
        doc.add(section("Комментарии к коду"));
        PdfPTable t = newTable(new float[]{0.7f, 5f});
        // header
        PdfPCell h1 = new PdfPCell(new Phrase("Стр.", boldFont));
        h1.setBackgroundColor(new Color(0xF1, 0xF5, 0xF9));
        PdfPCell h2 = new PdfPCell(new Phrase("Комментарий", boldFont));
        h2.setBackgroundColor(new Color(0xF1, 0xF5, 0xF9));
        t.addCell(h1);
        t.addCell(h2);
        for (ReviewClient.ReviewInfo.Comment c : comments) {
            t.addCell(new Phrase(c.getLineNumber() == null ? "—" : String.valueOf(c.getLineNumber()), normalFont));
            t.addCell(new Phrase(c.getText() == null ? "" : c.getText(), normalFont));
        }
        t.setSpacingAfter(14f);
        doc.add(t);
    }

    private void writeCode(Document doc, ReportContext ctx) {
        if (ctx.snapshot() == null || ctx.snapshot().getCode() == null || ctx.snapshot().getCode().isBlank()) {
            return;
        }
        doc.add(section("Код"));
        String code = ctx.snapshot().getCode();
        String[] lines = code.split("\\R", -1);
        int width = String.valueOf(lines.length).length();
        Paragraph block = new Paragraph();
        block.setLeading(11.5f);
        for (int i = 0; i < lines.length; i++) {
            String num = String.format("%" + width + "d  ", i + 1);
            Chunk nu = new Chunk(num, new Font(monoFont.getBaseFont(), 9, Font.NORMAL, Color.GRAY));
            Chunk ln = new Chunk(lines[i] + "\n", monoFont);
            block.add(nu);
            block.add(ln);
        }
        doc.add(block);
    }

    // ---------- helpers ----------

    private Paragraph section(String title) {
        Paragraph p = new Paragraph(title, sectionFont);
        p.setSpacingBefore(4f);
        p.setSpacingAfter(6f);
        return p;
    }

    private PdfPTable newTable(float[] widths) {
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        return t;
    }

    private void addRow(PdfPTable t, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, boldFont));
        labelCell.setBackgroundColor(new Color(0xF8, 0xFA, 0xFC));
        labelCell.setBorderColor(new Color(0xE2, 0xE8, 0xF0));
        labelCell.setPadding(4f);
        t.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, normalFont));
        valueCell.setBorderColor(new Color(0xE2, 0xE8, 0xF0));
        valueCell.setPadding(4f);
        t.addCell(valueCell);
    }

    private String str(String v) {
        return v == null || v.isBlank() ? "—" : v;
    }

    private String displayPerson(com.diploma.report_service.client.UserClient.UserInfo u, java.util.UUID fallbackId) {
        if (u == null) return fallbackId == null ? "—" : fallbackId.toString();
        String name = u.displayName();
        String email = u.getEmail();
        if (email != null && !email.isBlank() && !email.equals(name)) {
            return name + " (" + email + ")";
        }
        return name;
    }

    private String formatInstant(java.time.Instant instant) {
        return instant == null ? "—" : DATE_FORMAT.format(instant);
    }

    private String formatScore(Integer s) {
        return s == null ? "—" : (s + " / 5");
    }

    private String mapVerdict(String verdict) {
        if (verdict == null) return "Не выставлен";
        return switch (verdict) {
            case "HIRE" -> "Нанимать";
            case "NO_HIRE" -> "Не нанимать";
            case "MAYBE" -> "Сомневаюсь";
            default -> verdict;
        };
    }
}
