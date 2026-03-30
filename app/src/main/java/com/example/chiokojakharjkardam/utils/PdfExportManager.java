package com.example.chiokojakharjkardam.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;

import com.example.chiokojakharjkardam.data.database.entity.Transaction;
import com.example.chiokojakharjkardam.data.database.entity.TransactionDetail;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.List;

/**
 * کلاس کمکی برای صادر کردن گزارش به فرمت PDF
 * صفحه A4 افقی (Landscape) برای نمایش بهتر جدول
 */
public class PdfExportManager {

    // A4 Landscape
    private static final int PAGE_WIDTH  = 842;
    private static final int PAGE_HEIGHT = 595;
    private static final int MARGIN = 36;

    // ارتفاع هر سطر
    private static final int ROW_H      = 26;
    private static final int HEADER_ROW = 30;

    // عرض ستون‌ها (مجموع = PAGE_WIDTH - 2*MARGIN = 770)
    // ردیف | تاریخ | توضیحات | دسته‌بندی | تگ‌ها | نوع | مبلغ
    private static final int[] COL_W = {30, 90, 175, 130, 155, 60, 130};
    // مجموع = 30+90+175+130+155+60+130 = 770 ✓

    public interface ExportCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public static void exportTransactionsToPdf(
            Context context,
            Uri uri,
            List<TransactionDetail> details,
            String title,
            String dateRange,
            ExportCallback callback) {

        new Thread(() -> {
            PdfDocument pdf = new PdfDocument();
            try {
                DecimalFormat fmt = new DecimalFormat("#,###");

                // محاسبه فضای موجود برای سطرهای داده در هر صفحه
                // هدر صفحه: عنوان(40) + خط(4) + بازه(24) + خلاصه(36) + فاصله(8) + هدر ستون(HEADER_ROW) + خط(4)
                int headerSpace = 40 + 4 + 24 + 36 + 8 + HEADER_ROW + 4;
                int footerSpace = 30;
                int availH = PAGE_HEIGHT - MARGIN * 2 - headerSpace - footerSpace;
                int rowsPerPage = Math.max(1, availH / ROW_H);

                long totalExpense = 0, totalIncome = 0;
                for (TransactionDetail d : details) {
                    if (d.getTransaction().getType() == Transaction.TYPE_EXPENSE)
                        totalExpense += d.getTransaction().getAmount();
                    else
                        totalIncome += d.getTransaction().getAmount();
                }

                int totalPages = (int) Math.ceil((double) details.size() / rowsPerPage);
                if (totalPages == 0) totalPages = 1;

                for (int p = 0; p < totalPages; p++) {
                    PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(
                            PAGE_WIDTH, PAGE_HEIGHT, p + 1).create();
                    PdfDocument.Page page = pdf.startPage(info);
                    drawPage(page.getCanvas(), details, fmt, title, dateRange,
                            p, totalPages, rowsPerPage, totalExpense, totalIncome);
                    pdf.finishPage(page);
                }

                OutputStream os = context.getContentResolver().openOutputStream(uri);
                if (os != null) {
                    pdf.writeTo(os);
                    os.flush();
                    os.close();
                    if (callback != null) callback.onSuccess("فایل PDF با موفقیت ذخیره شد");
                } else {
                    if (callback != null) callback.onError("خطا در باز کردن فایل مقصد");
                }
            } catch (IOException e) {
                if (callback != null) callback.onError("خطا در ایجاد فایل PDF: " + e.getMessage());
            } finally {
                pdf.close();
            }
        }).start();
    }

    private static void drawPage(Canvas cv, List<TransactionDetail> details,
                                  DecimalFormat fmt, String title, String dateRange,
                                  int pageNum, int totalPages, int rowsPerPage,
                                  long totalExpense, long totalIncome) {

        // ---- Paints ----
        Paint pTitle = makePaint("#1A237E", 18f, true, Paint.Align.CENTER);
        Paint pSub   = makePaint("#546E7A", 10f, false, Paint.Align.CENTER);
        Paint pSum   = makePaint("#1A237E", 10f, true,  Paint.Align.CENTER);
        Paint pColHd = makePaint("#FFFFFF", 10f, true,  Paint.Align.CENTER);
        Paint pRow   = makePaint("#212121", 9f,  false, Paint.Align.CENTER);
        Paint pExp   = makePaint("#C62828", 9f,  false, Paint.Align.CENTER);
        Paint pInc   = makePaint("#2E7D32", 9f,  false, Paint.Align.CENTER);
        Paint pFooter= makePaint("#90A4AE", 8f,  false, Paint.Align.CENTER);

        Paint pBgColHd = solidPaint("#1565C0");
        Paint pBgSum   = solidPaint("#E8EAF6");
        Paint pBgAlt   = solidPaint("#F5F5F5");
        Paint pBgExp   = solidPaint("#FFEBEE");
        Paint pBgInc   = solidPaint("#E8F5E9");
        Paint pLine    = strokePaint("#CFD8DC", 0.8f);
        Paint pBorder  = strokePaint("#90A4AE", 0.5f);

        int x0 = MARGIN; // x شروع جدول

        // ---- عنوان ----
        int y = MARGIN + 28;
        cv.drawText(title, PAGE_WIDTH / 2f, y, pTitle);
        y += 6;
        cv.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, pLine);
        y += 18;
        cv.drawText(dateRange, PAGE_WIDTH / 2f, y, pSub);
        y += 10;

        // ---- خلاصه (فقط صفحه اول) ----
        if (pageNum == 0) {
            RectF sumRect = new RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 32);
            cv.drawRoundRect(sumRect, 6, 6, pBgSum);
            y += 20;
            String sumText =
                "جمع خرج: " + persian(fmt.format(totalExpense / 10)) + " تومان" +
                "     |     " +
                "جمع درآمد: " + persian(fmt.format(totalIncome / 10)) + " تومان" +
                "     |     " +
                "تعداد: " + persian(String.valueOf(details.size())) + " تراکنش";
            cv.drawText(sumText, PAGE_WIDTH / 2f, y, pSum);
            y += 16;
        } else {
            y += 8;
        }

        // ---- هدر ستون‌ها ----
        // عناوین ستون‌ها (از راست به چپ: ردیف، تاریخ، توضیحات، دسته‌بندی، تگ‌ها، نوع، مبلغ)
        String[] headers = {"ردیف", "تاریخ", "توضیحات", "دسته‌بندی", "تگ‌ها", "نوع", "مبلغ (تومان)"};

        // رسم پس‌زمینه هدر
        cv.drawRect(x0, y, x0 + totalColW(), y + HEADER_ROW, pBgColHd);

        // رسم متن هدر
        int colX = x0;
        for (int c = 0; c < COL_W.length; c++) {
            float cx = colX + COL_W[c] / 2f;
            cv.drawText(headers[c], cx, y + HEADER_ROW - 9, pColHd);
            colX += COL_W[c];
        }
        y += HEADER_ROW;

        // خط زیر هدر
        cv.drawLine(x0, y, x0 + totalColW(), y, pLine);

        // ---- سطرهای داده ----
        int startIdx = pageNum * rowsPerPage;
        int endIdx   = Math.min(startIdx + rowsPerPage, details.size());

        for (int i = startIdx; i < endIdx; i++) {
            TransactionDetail d = details.get(i);
            Transaction t = d.getTransaction();
            boolean isExp = t.getType() == Transaction.TYPE_EXPENSE;

            // پس‌زمینه سطر
            Paint bgRow = isExp ? pBgExp : pBgInc;
            if ((i - startIdx) % 2 == 1) bgRow = pBgAlt;
            cv.drawRect(x0, y, x0 + totalColW(), y + ROW_H, bgRow);

            // داده‌های هر سطر
            String[] cells = {
                persian(String.valueOf(i + 1)),
                PersianDateUtils.formatDate(t.getDate()),
                truncate(t.getDescription(), 22),
                truncate(d.getCategoryName(), 16),
                truncate(d.getTagNames(), 20),
                isExp ? "خرج" : "درآمد",
                persian(fmt.format(t.getAmount() / 10))
            };

            Paint textPaint = isExp ? pExp : pInc;
            if ((i - startIdx) % 2 == 1) textPaint = pRow;

            colX = x0;
            float textY = y + ROW_H - 8f;
            for (int c = 0; c < COL_W.length; c++) {
                float cx = colX + COL_W[c] / 2f;
                // ستون مبلغ با رنگ خرج/درآمد
                Paint cp = (c == COL_W.length - 1 || c == 5) ? (isExp ? pExp : pInc) : pRow;
                cv.drawText(cells[c], cx, textY, cp);
                colX += COL_W[c];
            }

            // خط جدا بین سطرها
            cv.drawLine(x0, y + ROW_H, x0 + totalColW(), y + ROW_H, pBorder);
            y += ROW_H;
        }

        // خط انتهای جدول
        cv.drawLine(x0, y, x0 + totalColW(), y, pLine);

        // خطوط عمودی ستون‌ها
        colX = x0;
        int tableTop = MARGIN;
        for (int w : COL_W) {
            cv.drawLine(colX, tableTop + 70, colX, y, pBorder);
            colX += w;
        }
        cv.drawLine(colX, tableTop + 70, colX, y, pBorder);
        // خط بیرونی جدول
        cv.drawRect(x0, tableTop + 70, x0 + totalColW(), y, pBorder);

        // ---- فوتر ----
        cv.drawLine(MARGIN, PAGE_HEIGHT - MARGIN - 12,
                PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN - 12, pLine);
        cv.drawText(
            "صفحه " + persian(String.valueOf(pageNum + 1)) + " از " + persian(String.valueOf(totalPages)),
            PAGE_WIDTH / 2f, PAGE_HEIGHT - MARGIN, pFooter);
    }

    // ---- کمکی ----

    private static int totalColW() {
        int s = 0;
        for (int w : COL_W) s += w;
        return s;
    }

    private static Paint makePaint(String hex, float size, boolean bold, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor(hex));
        p.setTextSize(size);
        if (bold) p.setFakeBoldText(true);
        p.setTextAlign(align);
        return p;
    }

    private static Paint solidPaint(String hex) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor(hex));
        p.setStyle(Paint.Style.FILL);
        return p;
    }

    private static Paint strokePaint(String hex, float width) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.parseColor(hex));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(width);
        return p;
    }

    /** تبدیل اعداد انگلیسی به فارسی */
    private static String persian(String s) {
        return PersianDateUtils.toPersianDigits(s);
    }

    /** کوتاه‌کردن متن طولانی */
    private static String truncate(String s, int max) {
        if (s == null || s.isEmpty()) return "-";
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }

    public static String generatePdfFileName(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + ".pdf";
    }
}
