# ifs25045-p5-pbo2026-springboot-cashflow

Aplikasi web pencatat keuangan pribadi (Cashflow) — Praktikum PBO 2026.
Clean Architecture · Java Generics · Java Exceptions · Spring Boot 4 · Vaadin 25 · JDBC PostgreSQL.

Package: `org.delcom.ifs25045springbootcashflow`

## Fitur
- Register, login, logout; sesi bertahan setelah refresh (cookie HttpOnly bertanda tangan HMAC + `LOGGED_OUT_KEY`).
- Role `ADMIN` / `USER`; admin di-seed dari `ROOT_USERNAME` / `ROOT_PASSWORD`; password di-hash SHA-256.
- Tambah pemasukan / pengeluaran dengan bukti gambar opsional (klik gambar untuk zoom), cari, urut (header grid + `SortOption`), hapus dengan konfirmasi, kartu ringkasan & saldo (`Rp 1,000,000.00`).
- Profil: ubah nama, password, foto.
- Admin: daftar pengguna dan peninjauan transaksi pengguna lain (read-only); admin juga punya cashflow sendiri.

## Rute
| Path | Akses |
|---|---|
| `/login`, `/register` | Publik (sudah login → diarahkan ke `/`) |
| `/uploads/**`, `/icons/**`, `styles.css` | Publik (statik) |
| `/`, `/profile` | USER & ADMIN |
| `/users`, `/user-transactions` | ADMIN (USER → diarahkan ke `/`) |

## Prasyarat
- JDK 21, PostgreSQL. Node.js diunduh otomatis oleh Vaadin saat pertama kali dijalankan (butuh internet).

## Konfigurasi
1. Buat database:
   ```sql
   CREATE DATABASE pbo2026_cashflow;
   ```
2. Salin `.env.example` menjadi `.env`, lalu isi kredensial Anda (jangan di-commit):
   ```
   APP_PORT, DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD,
   ROOT_USERNAME, ROOT_PASSWORD, UPLOAD_DIR
   ```
   Opsional: `SESSION_SECRET` agar login bertahan setelah server di-restart
   (kosong = kunci acak tiap start, sehingga cookie lama tidak berlaku setelah restart).
3. Tabel `users` dan `transactions` dibuat otomatis saat aplikasi start (`SchemaInitializer`).

## Menjalankan
```bash
./mvnw spring-boot:run          # Linux/macOS
.\mvnw.cmd spring-boot:run      # Windows
```
Buka `http://localhost:<APP_PORT>` (default 8080).

## Pengujian
```bash
./mvnw clean test      # unit test + AppTest (profil "test", H2 in-memory)
./mvnw clean verify    # + JaCoCo: instruction/branch/line/method/class coverage 100%
```
Laporan JaCoCo: `target/site/jacoco/index.html` (framework/view, framework/auth,
adapter/repository, dan entry point `App` dikecualikan).

## Struktur
```
domain/      entity, exception, repository (port), PasswordHasher       — tanpa Spring/Vaadin
usecase/     AuthUseCase, FinanceUseCase, UserUseCase                   — tanpa I/O UI
adapter/     repository (InMemory, Jdbc, SchemaInitializer), storage, presenter
framework/   auth (AuthSession), view (Vaadin)
config/      AppConfig (Composition Root)
```
Dependency mengarah ke dalam: `framework/adapter → usecase → domain`.

## Catatan
- Upload: JPG/PNG/GIF/WEBP, maks. 5 MB; nama file dibuat server (UUID) dan isi file dicek terhadap signature gambar.
- `uploads/`, `.env`, `target/` tidak masuk Git.
- Pipeline CI/CD ada di `Jenkinsfile` (Build & Test, Jacoco 100%, Trivy, Package, Publish, Deploy).
