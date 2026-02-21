# Baby Tracker

Prosta aplikacja Android do śledzenia zachowań noworodka — karmienie i pieluszki — w jak najmniejszej liczbie kliknięć.

---

## Funkcje

- **Karmienie** — butelka (opcjonalnie z liczbą ml) lub naturalne
- **Pieluszka** — siku, kupka lub mieszane
- **Widget ekranu głównego** — zapis zdarzenia bez otwierania aplikacji
- **Dashboard** — statystyki dobowe z nawigacją po dniach
- **Eksport CSV** — dane do Excela przez dowolną aplikację (e-mail, Drive itd.)

---

## Jak wgrać na telefon

### Wymagania

| Co | Gdzie pobrać |
|---|---|
| Android Studio (bezpłatne) | https://developer.android.com/studio |
| Telefon z Androidem 8.0+ | — |
| Kabel USB | — |

---

### Krok 1 — Pobierz i zainstaluj Android Studio

Wejdź na https://developer.android.com/studio, pobierz instalator dla swojego systemu (Windows / Mac / Linux) i uruchom go. Instalacja zajmuje ok. 10–15 minut.

---

### Krok 2 — Otwórz projekt

1. Uruchom Android Studio
2. Na ekranie startowym kliknij **Open**
3. Wskaż folder z tym projektem (`baby-tracker/`)
4. Poczekaj aż Android Studio zsynchronizuje projekt — postęp widać na dole okna. Przy pierwszym otwarciu może to potrwać kilka minut (pobiera zależności).

---

### Krok 3 — Włącz tryb dewelopera na telefonie

> Musisz to zrobić tylko raz na danym telefonie.

1. Otwórz **Ustawienia** telefonu
2. Wejdź w **O telefonie** (lub **Informacje o urządzeniu**)
3. Znajdź pozycję **Numer kompilacji** i dotknij jej **7 razy z rzędu**
4. Pojawi się komunikat: *"Jesteś teraz deweloperem"*
5. Wróć do Ustawień — pojawi się nowa pozycja **Opcje deweloperskie**
6. Wejdź w **Opcje deweloperskie** i włącz **Debugowanie USB**

> **Samsung:** Ustawienia → Informacje o telefonie → Informacje o oprogramowaniu → Numer kompilacji
>
> **Xiaomi / MIUI:** Ustawienia → O telefonie → Wersja MIUI (7 razy)

---

### Krok 4 — Podłącz telefon

1. Podłącz telefon kablem USB do komputera
2. Na ekranie telefonu pojawi się okienko — wybierz **Zezwól na debugowanie USB**
3. W Android Studio, u góry ekranu, powinno pojawić się imię/model Twojego telefonu

---

### Krok 5 — Zainstaluj aplikację

1. Kliknij zielony przycisk **▶ Run** (lub wciśnij **Shift + F10**)
2. Android Studio skompiluje aplikację i zainstaluje ją na telefonie (1–2 minuty)
3. Aplikacja uruchomi się automatycznie

> Aplikacja zostaje na telefonie — możesz odłączyć kabel. Działa w pełni offline.

---

## Jak dodać widget na ekran główny

Po zainstalowaniu aplikacji:

1. Przytrzymaj palec na **pustym miejscu** na ekranie głównym
2. Wybierz **Widżety** (lub Widget)
3. Znajdź na liście **Baby Tracker**
4. Przeciągnij widget w wybrane miejsce
5. Od teraz możesz zapisywać karmienie i pieluszki **jednym dotknięciem** bez otwierania aplikacji

---

## Eksport danych

W zakładce **Dashboard** (ikona wykresu w prawym górnym rogu ekranu głównego):

1. Kliknij ikonę pobierania ↓ w prawym górnym rogu
2. Wybierz aplikację do udostępnienia (e-mail, Dysk Google, WhatsApp itd.)
3. Plik CSV możesz otworzyć w Excelu lub Arkuszach Google

---

## Publikacja w Google Play Store

Jeśli chcesz udostępnić aplikację innym przez sklep — szczegółowa instrukcja krok po kroku jest w pliku [`DEPLOYMENT.md`](DEPLOYMENT.md).

Skrócony przegląd procesu:
1. Rejestracja konta Google Play Developer (jednorazowa opłata 25 USD)
2. Wygenerowanie podpisanego pliku `.aab` w Android Studio
3. Utworzenie wpisu aplikacji w Google Play Console
4. Przesłanie pliku `.aab` i wypełnienie wymaganych informacji
5. Oczekiwanie na weryfikację Google (1–3 dni robocze)

---

## Wymagania techniczne

- Android **8.0 (Oreo)** lub nowszy
- Większość telefonów z 2017 roku lub nowszych spełnia ten wymóg
- Aplikacja nie wymaga internetu ani żadnych uprawnień poza wibracją
