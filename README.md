# ChioKojaKharjKardam — چیو کجا خرج کردم

[![GitHub repo](https://img.shields.io/badge/github-chioKojaKharjKardam-blue)](https://github.com/navidAbbasian/chioKojaKharjKardam)
[![Android](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com)

یک اپلیکیشن مدیریت هزینه و خانوادگی برای ثبت هزینه‌ها، کارت‌ها، اعضای خانواده و گزارش‌ها. این مخزن حاوی پروژهٔ اندروید (Gradle/Kotlin/Java) است.

---

## شمای کلی (سریع)

- زبان: Java/Kotlin (بسته به ماژول‌ها)
- ابزار ساخت: Gradle (Kotlin DSL)
- ساختار: یک ماژول `app/` حاوی کد اپلیکیشن

سازمان‌دهی اصلی:

- `app/src/main/java/...` — کد منبع
- `app/src/main/res/` — منابع (layout, drawable, values)
- `app/src/main/res/navigation/` — NavGraph
- `gradle/` و `build.gradle.kts` — تنظیمات ساخت

---

## شروع سریع

پیش‌نیازها:

- Android Studio (آخرین نسخه توصیه می‌شود)
- JDK 11 یا بالاتر
- Android SDK (نسخه‌های مورد نیاز در Android Studio)

گام‌ها:

1. کلون کردن مخزن:

```bash
git clone https://github.com/navidAbbasian/chioKojaKharjKardam.git
cd chioKojaKharjKardam
```

2. باز کردن پروژه در Android Studio: File → Open → پوشه‌ی پروژه

3. ساخت و اجرای برنامه (از داخل Android Studio یا خط فرمان):

```bash
# برای ساخت
./gradlew assembleDebug

# برای اجرای روی دستگاه/emulator
./gradlew installDebug
```

نکته: قبل از اجرای روی دستگاه واقعی، `google-services.json` یا سایر کلیدهای محلی را اضافه کنید اگر پروژه به سرویس‌های خارجی متکی است.

---

## معماری — شماتیک ساده

این اپ با الگوی مرسوم MVVM (ViewModel + Repository) و Room برای DB طراحی شده است.

+ UI (Fragments/Activities)
  + ViewModels
    + Repositories
      + Room (DAO, Entities)

نمونهٔ سادهٔ جریان داده:

UI (Fragment) -> ViewModel -> Repository -> DAO (Room)

---

## فایل‌های مهم

- `app/src/main/java/.../data/database/` — Entities, DAOs, Database
- `app/src/main/java/.../ui/` — Fragments, Activities و adapters
- `app/src/main/res/navigation/nav_graph.xml` — ناوبری اپ
- `build.gradle.kts` (در ریشه و `app/`) — وابستگی‌ها و تنظیمات ساخت

---

## توسعه و تست

- اجرای تست‌های واحد:

```bash
./gradlew test
```

- اجرای تست‌های اندروید (instrumented):

```bash
./gradlew connectedAndroidTest
```

---

## نکات امنیتی

- فایل‌های حساس مثل `google-services.json` یا keystore را در مخزن عمومی قرار ندهید.
- از `.gitignore` برای جلوگیری از اضافه شدن فایل‌های محلی/باینری استفاده شده است.

---

## مشارکت

اگر می‌خواهید کمک کنید:

1. یک issue باز کنید یا تغییرات پیشنهادی را در یک branch مجزا ایجاد کنید.
2. یک Pull Request با توضیحات روشن ارسال کنید.

قوانین ساده:

- پیام‌های کامیت معنادار بنویسید.
- تغییرات بزرگ را قبل ازمرج در یک PR مستقل و با توضیحات ارسال کنید.

---