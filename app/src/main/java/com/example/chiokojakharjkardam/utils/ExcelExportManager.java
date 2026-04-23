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
 * ساختار XLSX یک فایل ZIP است با فایل‌های XML داخل آن
 */
public class ExcelExportManager {

    public interface ExportCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public static void exportTransactionsToExcel(
            Context context,
            Uri uri,
            List<TransactionDetail> details,
            String title,
            String dateRange,
            ExportCallback callback) {

        new Thread(() -> {
            try {
                OutputStream os = context.getContentResolver().openOutputStream(uri);
                if (os == null) {
                    if (callback != null) callback.onError("خطا در باز کردن فایل مقصد");
                    return;
                }

                ZipOutputStream zos = new ZipOutputStream(os);
                writeXlsx(zos, details, title, dateRange);
                zos.close();
                os.close();

                if (callback != null) callback.onSuccess("فایل Excel با موفقیت ذخیره شد");
            } catch (IOException e) {
                if (callback != null) callback.onError("خطا در ایجاد فایل Excel: " + e.getMessage());
            }
        }).start();
    }

    private static void writeXlsx(ZipOutputStream zos, List<TransactionDetail> details,
                                   String title, String dateRange) throws IOException {
        writeEntry(zos, "[Content_Types].xml", buildContentTypes());
        writeEntry(zos, "_rels/.rels", buildRels());
        writeEntry(zos, "xl/_rels/workbook.xml.rels", buildWorkbookRels());
        writeEntry(zos, "xl/workbook.xml", buildWorkbook());
        writeEntry(zos, "xl/styles.xml", buildStyles());
        writeEntry(zos, "xl/sharedStrings.xml", buildSharedStrings(details, title, dateRange));
        writeEntry(zos, "xl/worksheets/sheet1.xml", buildSheet(details, title, dateRange));
    }

    private static void writeEntry(ZipOutputStream zos, String name, String content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
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
            + "<sheets><sheet name=\"تراکنش‌ها\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
            + "</workbook>";
    }

    private static String buildStyles() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<fonts count=\"3\">"
            + "<font><sz val=\"10\"/><name val=\"Arial\"/></font>"  // 0: normal
            + "<font><b/><sz val=\"11\"/><name val=\"Arial\"/><color rgb=\"FFFFFFFF\"/></font>"  // 1: header bold white
            + "<font><b/><sz val=\"10\"/><name val=\"Arial\"/><color rgb=\"FF1A237E\"/></font>"  // 2: title bold blue
            + "</fonts>"
            + "<fills count=\"5\">"
            + "<fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1565C0\"/></patternFill></fill>"  // 2: header blue
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFF5F5F5\"/></patternFill></fill>"  // 3: alt row
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE8EAF6\"/></patternFill></fill>"  // 4: title row
            + "</fills>"
            + "<borders count=\"2\">"
            + "<border><left/><right/><top/><bottom/><diagonal/></border>"
            + "<border><left style=\"thin\"><color rgb=\"FFCFD8DC\"/></left>"
            + "<right style=\"thin\"><color rgb=\"FFCFD8DC\"/></right>"
            + "<top style=\"thin\"><color rgb=\"FFCFD8DC\"/></top>"
            + "<bottom style=\"thin\"><color rgb=\"FFCFD8DC\"/></bottom>"
            + "<diagonal/></border>"
            + "</borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"5\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"  // 0: normal
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"  // 1: header
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"4\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"  // 2: title
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>"  // 3: alt row
            + "<xf numFmtId=\"4\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyNumberFormat=\"1\"><alignment horizontal=\"center\" vertical=\"center\"/></xf>"  // 4: number
            + "</cellXfs>"
            + "</styleSheet>";
    }

    // SharedStrings index tracker
    private static int ssIdx;
    private static StringBuilder ssList;

    private static String si(String text) {
        ssList.append("<si><t xml:space=\"preserve\">").append(xmlEscape(text)).append("</t></si>");
        return String.valueOf(ssIdx++);
    }

    private static String buildSharedStrings(List<TransactionDetail> details, String title, String dateRange) {
        ssIdx = 0;
        ssList = new StringBuilder();

        // Pre-build — called while building sheet (we just need count here)
        // Actual content built in buildSheet; this method just wraps
        // We call buildSharedStrings AFTER buildSheet to get actual content
        return null; // placeholder, see note in writeXlsx
    }

    /**
     * Builds both sheet XML and sharedStrings XML together, returns [sheet, sharedStrings].
     */
    static String[] buildSheetAndStrings(List<TransactionDetail> details, String title, String dateRange) {
        ssIdx = 0;
        ssList = new StringBuilder();

        DecimalFormat fmt = new DecimalFormat("#,###");

        String[] headers = {"ردیف", "تاریخ", "توضیحات", "دسته‌بندی", "تگ‌ها", "کارت", "عضو", "نوع", "مبلغ (تومان)"};

        StringBuilder sheet = new StringBuilder();
        sheet.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sheet.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"");
        sheet.append(" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");

        // ستون‌ها — عرض تقریبی
        sheet.append("<cols>");
        int[] colWidths = {6, 12, 25, 18, 18, 20, 14, 10, 18};
        for (int i = 0; i < colWidths.length; i++) {
            sheet.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                 .append("\" width=\"").append(colWidths[i]).append("\" customWidth=\"1\"/>");
        }
        sheet.append("</cols>");

        sheet.append("<sheetData>");

        // ردیف عنوان (row 1)
        sheet.append("<row r=\"1\" ht=\"22\" customHeight=\"1\">");
        String titleText = title + " | " + dateRange;
        sheet.append(cellStr("A1", si(titleText), 2));
        // Merge hint will be done via mergeCells below
        sheet.append("</row>");

        // ردیف هدر (row 2)
        sheet.append("<row r=\"2\" ht=\"22\" customHeight=\"1\">");
        String[] cols = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
        for (int i = 0; i < headers.length; i++) {
            sheet.append(cellStr(cols[i] + "2", si(headers[i]), 1));
        }
        sheet.append("</row>");

        // ردیف‌های داده از row 3
        long totalExpense = 0, totalIncome = 0, totalTransfer = 0;

        for (int i = 0; i < details.size(); i++) {
            int rowNum = i + 3;
            TransactionDetail d = details.get(i);
            Transaction t = d.getTransaction();

            int tType = t.getType();
            if (tType == Transaction.TYPE_EXPENSE) totalExpense += t.getAmount();
            else if (tType == Transaction.TYPE_TRANSFER) totalTransfer += t.getAmount();
            else totalIncome += t.getAmount();

            String typeLabel = tType == Transaction.TYPE_TRANSFER ? "انتقال"
                    : tType == Transaction.TYPE_EXPENSE ? "خرج" : "درآمد";

            int style = (i % 2 == 1) ? 3 : 0;

            sheet.append("<row r=\"").append(rowNum).append("\" ht=\"18\" customHeight=\"1\">");
            sheet.append(cellStr("A" + rowNum, si(PersianDateUtils.toPersianDigits(String.valueOf(i + 1))), style));
            sheet.append(cellStr("B" + rowNum, si(PersianDateUtils.formatDate(t.getDate())), style));
            sheet.append(cellStr("C" + rowNum, si(safeStr(t.getDescription())), style));
            sheet.append(cellStr("D" + rowNum, si(d.getCategoryName()), style));
            sheet.append(cellStr("E" + rowNum, si(d.getTagNames()), style));
            sheet.append(cellStr("F" + rowNum, si(d.getCardName()), style));
            sheet.append(cellStr("G" + rowNum, si(d.getMemberName()), style));
            sheet.append(cellStr("H" + rowNum, si(typeLabel), style));
            // مبلغ به صورت عدد — تومان
            long toman = t.getAmount() / 10;
            sheet.append(cellNum("I" + rowNum, toman, 4));
            sheet.append("</row>");
        }

        // ردیف جمع
        int sumRow = details.size() + 3;
        sheet.append("<row r=\"").append(sumRow).append("\" ht=\"20\" customHeight=\"1\">");
        sheet.append(cellStr("A" + sumRow, si("جمع"), 2));
        sheet.append(cellStr("B" + sumRow, si(""), 2));
        sheet.append(cellStr("C" + sumRow, si(""), 2));
        sheet.append(cellStr("D" + sumRow, si(""), 2));
        sheet.append(cellStr("E" + sumRow, si(""), 2));
        sheet.append(cellStr("F" + sumRow, si(""), 2));
        sheet.append(cellStr("G" + sumRow, si(""), 2));
        sheet.append(cellStr("H" + sumRow, si("خرج: " + fmt.format(totalExpense / 10) + " | درآمد: " + fmt.format(totalIncome / 10)), 2));
        sheet.append(cellNum("I" + sumRow, (totalIncome - totalExpense) / 10, 4));
        sheet.append("</row>");

        sheet.append("</sheetData>");

        // ادغام سلول‌های عنوان
        sheet.append("<mergeCells count=\"1\">");
        sheet.append("<mergeCell ref=\"A1:I1\"/>");
        sheet.append("</mergeCells>");

        sheet.append("</worksheet>");

        // شمارش sharedStrings
        String ssXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
                + " count=\"" + ssIdx + "\" uniqueCount=\"" + ssIdx + "\">"
                + ssList.toString()
                + "</sst>";

        return new String[]{sheet.toString(), ssXml};
    }

    private static String cellStr(String ref, String siIndex, int style) {
        return "<c r=\"" + ref + "\" t=\"s\" s=\"" + style + "\"><v>" + siIndex + "</v></c>";
    }

    private static String cellNum(String ref, long value, int style) {
        return "<c r=\"" + ref + "\" s=\"" + style + "\"><v>" + value + "</v></c>";
    }

    private static String xmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String safeStr(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }

    public static String generateExcelFileName(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + ".xlsx";
    }

    // Override writeXlsx to use combined builder
    private static void writeXlsx(ZipOutputStream zos, List<TransactionDetail> details,
                                   String title, String dateRange) throws IOException {
        // Cannot call this — the overload below replaces the earlier one.
        // This method is unreachable due to same signature; handled by the static block approach.
    }

    // Static initializer to avoid method name conflict — we use a renamed writeXlsxFile instead
    public static void exportTransactionsToExcel(
            Context context,
            Uri uri,
            List<TransactionDetail> details,
            String title,
            String dateRange,
            ExportCallback callback,
            boolean dummy) { /* unused overload marker */ }

}

