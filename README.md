# Visual Realism Suite

**Proyek Praktikum Grafika Komputer – Minggu ke-11**  
**Rendering Faces pada Visual Realism Berbasis Java OOP dan Modern OpenGL/GLSL**

## Identitas Mahasiswa

- **Nama:** Sekar Danutri Hamdala
- **NIM:** 25050974077
- **Program Studi:** Pendidikan Teknologi Informasi
- **Institusi:** Universitas Negeri Surabaya
- **Mata Kuliah:** Grafika Komputer
- **Dosen Pengampu:** Drs. Bambang Sujatmiko, M.T.

## Deskripsi Proyek

Visual Realism Suite merupakan aplikasi grafika komputer tiga dimensi yang dikembangkan menggunakan Java OOP dan Modern OpenGL/GLSL.

Proyek ini bertujuan menerapkan beberapa teknik rendering untuk menghasilkan objek 3D yang lebih realistis, meliputi pencahayaan, pengaturan permukaan objek, pengujian kedalaman, pemetaan tekstur, dan bayangan secara real-time.

## Fitur Utama

1. **Phong dan Blinn-Phong Shading**  
   Menerapkan dua model pencahayaan untuk menghasilkan efek cahaya dan pantulan pada objek 3D.

2. **Flat dan Smooth Shading**  
   Membandingkan tampilan permukaan objek menggunakan normal per bidang dan normal per vertex.

3. **Hidden Surface Removal (HSR)**  
   Menggunakan Z-Buffer dan Back-Face Culling untuk mengatur visibilitas permukaan objek.

4. **Texture Mapping**  
   Menerapkan tekstur gambar pada permukaan objek menggunakan koordinat UV, serta membandingkan metode filtering.

5. **Real-Time Shadow Mapping**  
   Menghasilkan bayangan objek menggunakan teknik Shadow Mapping dengan dua tahap rendering.

## Teknologi yang Digunakan

- Java
- Java OOP
- Maven
- LWJGL (Lightweight Java Game Library)
- OpenGL
- GLSL Shader
- JOML

## Struktur Proyek

```text
VisualRealismSuite/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       └── resources/
└── textures/
```

## Cara Menjalankan Program

1. Pastikan Java Development Kit (JDK) dan NetBeans IDE sudah terpasang.
2. Pastikan library dan dependensi Maven dapat diunduh.
3. Buka NetBeans IDE.
4. Pilih **File → Open Project**.
5. Pilih folder `VisualRealismSuite`.
6. Tunggu proses pemuatan dependensi Maven selesai.
7. Jalankan proyek dengan memilih **Run Project**.

Pastikan konfigurasi Main Class mengarah ke:

```text
com.grafkom.viewing3d.VisualRealismSuite
```

## Pengujian

Pengujian dilakukan dengan membandingkan hasil rendering melalui beberapa indikator:

- Perbandingan Phong dan Blinn-Phong.
- Perbandingan Flat dan Smooth Shading.
- Pengujian Z-Buffer dan Back-Face Culling.
- Pengujian Texture Mapping dan Texture Filtering.
- Pengujian Real-Time Shadow Mapping.

## Tujuan

Proyek ini dibuat sebagai pemenuhan tugas praktikum Grafika Komputer Minggu ke-11 sekaligus sebagai penerapan konsep rendering tiga dimensi menggunakan Java OOP dan Modern OpenGL/GLSL.

---

**Universitas Negeri Surabaya**  
**Program Studi Pendidikan Teknologi Informasi**  
**2026**
