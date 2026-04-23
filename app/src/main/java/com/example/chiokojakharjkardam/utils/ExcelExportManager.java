package com.example.chiokojakharjkardam.utils;

import android.content.Context;
import android.net.Uri;

import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.data.database.entity.TransactionDetail;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * خروجی اکسل (XLSX) بدون نیاز به کتابخانه خارجی
 * ساختار XLSX یک فایل ZIP است با فایل های XML داخل آن
 */
public class ExcelExportManager {

    public interface ExportCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    // Per-instance shared-strings state
    private int ssIdx;
    private final StringBuilder ssList = new StringBuilder();

    private String si(String text) {
        ssList.append("<si><t xml:space=\"preserve\">")
              .append(xmlEscape(text))
              .append("</t></si>");
        return String.valueOf(ssIdx++);
    }

    public static void exportTransactionsToExcel(
            Context context, Uri uri, List<TransactionDetail> details,
            String title, String dateRange, ExportCallback callback) {
        new Thread(() -> {
            try {
                OutputStream os = context.getContentResolver().openOutputStream(uri);
                if (os == null) {
                    if (callback != null) callback.onError("خطا در باز کردن فایل مقصد");
                    return;
                }
                ZipOutputStream zos = new ZipOutputStream(os);
                new ExcelExportManager().writeXlsx(zos, details, title, dateRange);
                zos.close();
                os.close();
                if (callback != null) callback.onSuccess("فایل Excel با موفقیت ذخیره شد");
            } catch (IOException e) {
                if (callback != null) callback.onError("خطا در ایجاد فایل Excel: " + e.getMessage());
            }
        }).start();
    }

    public static String generateExcelFileName(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + ".xlsx";
    }

    private void writeXlsx(ZipOutputStream zos, List<TransactionDetail> details,
                            String title, String dateRange) throws IOException {
        String sheetXml = buildSheet(details, title, dateRange);
        String ssXml = buildSsXml();
        writeEntry(zos, "[Content_Types].xml", buildContentTypes());
        writeEntry(zos, "_rels/.rels", buildRels());
        writeEntry(zos, "xl/_rels/workbook.xml.rels", buildWorkbookRels());
        writeEntry(zos, "xl/workbook.xml", buildWorkbook());
        writeEntry(zos, "xl/styles.xml", buildStyles());
        writeEntry(zos, "xl/sharedStrings.xml", ssXml);
        writeEntry(zos, "xl/worksheets/sheet1.xml", sheetXml);
    }

    private String buildSsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
                + " count=\"" + ssIdx + "\" uniqueCount=\"" + ssIdx + "\">"
                + ssList.toString() + "</sst>";
    }

    private String buildSheet(List<TransactionDetail> details, String title, String dateRange) {
        DecimalFormat fmt = new DecimalFormat("#,###");
        String[] headers = {
            "\u0631\u062f\u06cc\u0641",
            "\u062a\u0627\u0631\u06cc\u062e",
            "\u062a\u0648\u0632\u06cc\u062d\u0627\u062a",
            "\u062f\u0633\u062a\u0647\u200c\u0628\u0646\u062f\u06cc",
            "\u062a\u06af\u200c\u0647\u0627",
            "\u06a9\u0627\u0631\u062a",
            "\u0639\u0636\u0648",
            "\u0646\u0648\u0639",
            "\u0645\u0628\u0644\u063a (\u062a\u0648\u0645\u0627\u0646)"
        };
        String[] cols = {"A","B","C","D","E","F","G","H","I"};
        int[] colWidths = {6, 12, 25, 18, 18, 20, 14, 10, 18};

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"");
        sb.append(" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
        sb.append("<cols>");
        for (int i = 0; i < colWidths.length; i++) {
            sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
              .append("\" width=\"").append(colWidths[i]).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");

        // Title row
        sb.append("<row r=\"1\" ht=\"22\" customHeight=\"1\">");
        sb.append(cs("A1", si(title + " | " + dateRange), 2));
        sb.append("</row>");

        // Header row
        sb.append("<row r=\"2\" ht=\"22\" customHeight=\"1\">");
        for (int i = 0; i < headers.length; i++) {
            sb.append(cs(cols[i] + "2", si(headers[i]), 1));
        }
        sb.append("</row>");

        // Data rows
        long totalExpense = 0, totalIncome = 0;
        for (int i = 0; i < details.size(); i++) {
            int rowNum = i + 3;
            TransactionDetail d = details.get(i);
            Transaction t = d.getTransaction();
            int tType = t.getType();
            if (tType == Transaction.TYPE_EXPENSE) totalExpense += t.getAmount();
            else if (tType == Transaction.TYPE_INCOME) totalIncome += t.getAmount();
            String lbl = tType == Transaction.TYPE_TRANSFER
                    ? "\u0627\u0646\u062a\u0642\u0627\u0644"
                    : tType == Transaction.TYPE_EXPENSE ? "\u062e\u0631\u062c" : "\u062f\u0631\u0622\u0645\u062f";
            int style = (i % 2 == 1) ? 3 : 0;
            String rn = String.valueOf(rowNum);
            sb.append("<row r=\"").append(rn).append("\" ht=\"18\" customHeight=\"1\">");
            sb.append(cs("A" + rn, si(PersianDateUtils.toPersianDigits(String.valueOf(i + 1))), style));
            sb.append(cs("B" + rn, si(PersianDateUtils.formatDate(t.getDate())), style));
            sb.append(cs("C" + rn, si(safe(t.getDescription())), style));
            sb.append(cs("D" + rn, si(d.getCategoryName()), style));
            sb.append(cs("E" + rn, si(d.getTagNames()), style));
            sb.append(cs("F" + rn, si(d.getCardName()), style));
            sb.append(cs("G" + rn, si(d.getMemberName()), style));
            sb.append(cs("H" + rn, si(lbl), style));
            sb.append(cn("I" + rn, t.getAmount() / 10, 4));
            sb.append("</row>");
        }

        // Summary row
        int sumRow = details.size() + 3;
        String sr = String.valueOf(sumRow);
        String sumLbl = "\u062e\u0631\u062c: " + fmt.format(totalExpense / 10)
                + " | \u062f\u0631\u0622\u0645\u062f: " + fmt.format(totalIncome / 10);
        sb.append("<row r=\"").append(sr).append("\" ht=\"20\" customHeight=\"1\">");
        for (String c : new String[]{"A","B","C","D","E","F","G"}) {
            sb.append(cs(c + sr, si(""), 2));
        }
        sb.append(cs("H" + sr, si(sumLbl), 2));
        sb.append(cn("I" + sr, (totalIncome - totalExpense) / 10, 4));
        sb.append("</row>");
        sb.append("</sheetData>");
        sb.append("<mergeCells count=\"1\"><mergeCell ref=\"A1:I1\"/></mergeCells>");
        sb.append("</worksheet>");
        return sb.toString();
    }

    // cell string (shared string index)
    private static String cs(String ref, String idx, int style) {
        return "<c r=\"" + ref + "\" t=\"s\" s=\"" + style + "\"><v>" + idx + "</v></c>";
    }

    // cell number
    private static String cn(String ref, long val, int style) {
        return "<c r=\"" + ref + "\" s=\"" + style + "\"><v>" + val + "</v></c>";
    }

    private static void writeEntry(ZipOutputStream zos, String name, String content) throws IOException {
        ZipEntry e = new ZipEntry(name);
        zos.putNextEntry(e);
        zos.write(content.getBytes("UTF-8"));
        zos.closeEntry();
    }

    private static String buildContentTypes() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>"
            + "</Types>";
    }

    private static String buildRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";
    }

    private static String buildWorkbookRels() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
            + "<Relationship Id=\"rId3\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>"
            + "</Relationships>";
    }

    private static String buildWorkbook() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
            + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<sheets><sheet name=\"transactions\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
            + "</workbook>";
    }

    private static String buildStyles() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<fonts count=\"3\">"
            + "<font><sz val=\"10\"/><name val=\"Arial\"/></font>"
            + "<font><b/><sz val=\"11\"/><name val=\"Arial\"/><color rgb=\"FFFFFFFF\"/></font>"
            + "<font><b/><sz val=\"10\"/><name val=\"Arial\"/><color rgb=\"FF1A237E\"/></font>"
            + "</fonts>"
            + "<fills count=\"5\">"
            + "<fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1565C0\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF5F5F5\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE8EAF6\"/></patternFill></fill>"
            + "</fills>"
            + "<borders count=\"2\">"
            + "<border><left/><right/><top/><bottom/><diagonal/></border>"
            + "<border>"
            + "<left style=\"thin\"><color rgb=\"FFCFD8DC\"/></left>"
            + "<right style=\"thin\"><color rgb=\"FFCFD8DC\"/></right>"
            + "<top style=\"thin\"><color rgb=\"FFCFD8DC\"/></top>"
            + "<bottom style=\"thin\"><color rgb=\"FFCFD8DC\"/></bottom>"
            + "<diagonal/></border>"
            + "</borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"5\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"4\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"
            + "<xf numFmtId=\"4\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyNumberFormat=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"
            + "</cellXfs>"
            + "</styleSheet>";
    }

    private static String xmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String safe(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }
}

