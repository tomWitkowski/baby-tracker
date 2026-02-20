# Jak wgrać Baby Tracker na telefon i opublikować w Google Play

## Opcja A: Bezpośrednio na telefon (bez sklepu) — najszybsza

### Co potrzebujesz
- Komputer z systemem Windows, Mac lub Linux
- Android Studio (bezpłatne): https://developer.android.com/studio
- Kabel USB lub połączenie przez Wi-Fi
- Telefon z Androidem

### Kroki

#### 1. Zainstaluj Android Studio
Pobierz i zainstaluj Android Studio ze strony podanej wyżej. Instalacja trwa ok. 10-15 minut.

#### 2. Otwórz projekt
1. Uruchom Android Studio
2. Wybierz **"Open"**
3. Wskaż folder z tym projektem (`baby-tracker/`)
4. Poczekaj aż Android Studio pobierze wszystkie zależności (może trwać kilka minut przy pierwszym otwarciu)

#### 3. Przygotuj telefon
1. Wejdź w **Ustawienia** telefonu
2. Znajdź **"O telefonie"** lub **"Informacje o urządzeniu"**
3. Kliknij **7 razy** w pole **"Numer kompilacji"** — pojawi się komunikat "Jesteś teraz deweloperem"
4. Wróć do Ustawień → znajdź **"Opcje deweloperskie"**
5. Włącz **"Debugowanie USB"**

#### 4. Podłącz telefon
1. Podłącz telefon kablem USB do komputera
2. Na ekranie telefonu pojawi się pytanie — wybierz **"Zezwól na debugowanie USB"**

#### 5. Uruchom aplikację
1. W Android Studio na górze zobaczysz nazwę swojego telefonu w pasku
2. Kliknij zielony przycisk **▶ Run** (lub Shift+F10)
3. Aplikacja zostanie zainstalowana i uruchomiona na telefonie

> Gotowe! Aplikacja będzie na Twoim telefonie i będzie działać bez internetu.

---

## Opcja B: Publikacja w Google Play Store

### Czego potrzebujesz
- Konto Google Play Developer — jednorazowa opłata **25 USD**
  Rejestracja: https://play.google.com/console
- Android Studio z projektem (jak wyżej)

### Krok 1: Utwórz klucz podpisywania (Keystore)

W Android Studio:
1. Menu górne → **Build** → **Generate Signed Bundle / APK**
2. Wybierz **Android App Bundle** → Dalej
3. Kliknij **"Create new..."** przy polu Keystore path
4. Wypełnij formularz:
   - **Key store path**: wybierz bezpieczne miejsce na dysku (np. `~/baby-tracker-key.jks`)
   - **Password**: wymyśl silne hasło (ZAPISZ JE — bez niego nie zaktualizujesz aplikacji nigdy!)
   - **Key alias**: np. `baby-tracker-key`
   - **Key password**: może być takie samo jak powyżej
   - **Validity**: 25 lat
   - **First and last name**: Twoje imię i nazwisko
5. Kliknij OK → Dalej → **Release** → **Finish**
6. Plik `.aab` (Android App Bundle) zostanie wygenerowany w folderze `app/release/`

### Krok 2: Skonfiguruj podpisywanie (opcjonalne ale wygodne)

W pliku `app/build.gradle.kts` możesz dodać konfigurację podpisywania żeby nie wpisywać haseł za każdym razem.

### Krok 3: Utwórz aplikację w Google Play Console

1. Wejdź na https://play.google.com/console
2. Kliknij **"Utwórz aplikację"**
3. Wypełnij:
   - Nazwa: **Baby Tracker**
   - Język domyślny: Polski
   - Typ: Aplikacja
   - Bezpłatna/Płatna: Bezpłatna
4. Zaakceptuj zasady → **Utwórz aplikację**

### Krok 4: Wypełnij wymagane informacje

W Google Play Console musisz wypełnić kilka sekcji:

#### Informacje o aplikacji
- Krótki opis (maks. 80 znaków): `Śledzenie karmienia i pieluszek noworodka`
- Pełny opis: opisz co aplikacja robi
- Ikona (512x512 px PNG)
- Grafika promująca (1024x500 px)
- Zrzuty ekranu (min. 2, telefon: 320-3840 px szerokości)

#### Klasyfikacja treści
- Wypełnij kwestionariusz → aplikacja otrzyma klasyfikację "Dla wszystkich"

#### Ustawienia aplikacji
- Kategoria: **Zdrowie i fitness** lub **Styl życia**
- Polityka prywatności: potrzebujesz strony z polityką prywatności
  (możesz użyć generatora np. https://www.privacypolicygenerator.info)

### Krok 5: Prześlij plik AAB

1. W lewym menu → **Produkcja** → **Wersje**
2. Kliknij **"Utwórz nową wersję"**
3. Prześlij plik `.aab` z folderu `app/release/`
4. Dodaj informacje o wersji (np. "Pierwsza wersja")
5. Kliknij **"Zapisz"** → **"Sprawdź wersję"** → **"Opublikuj"**

### Krok 6: Oczekiwanie na weryfikację

Google weryfikuje aplikacje zazwyczaj w ciągu **1-3 dni roboczych**.
Po weryfikacji aplikacja pojawi się w sklepie Play.

---

## Opcja C: Udostępnij plik APK bezpośrednio (bez sklepu)

Jeśli chcesz tylko dać aplikację kilku osobom bez publikacji w sklepie:

1. W Android Studio → **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Plik APK zostanie wygenerowany w `app/build/outputs/apk/debug/`
3. Wyślij plik APK przez e-mail, WhatsApp lub Dysk Google
4. Odbiorca musi włączyć w telefonie:
   - Ustawienia → Bezpieczeństwo → **"Instaluj aplikacje z nieznanych źródeł"** (lub podobne)
5. Po pobraniu APK — wystarczy go kliknąć żeby zainstalować

---

## Wymagania minimalne telefonu

- System Android w wersji **8.0 (Oreo)** lub nowszy
- Większość telefonów wyprodukowanych po 2017 roku spełnia ten wymóg

---

## Widget na ekranie głównym

Po zainstalowaniu aplikacji możesz dodać widget:
1. Przytrzymaj palec na pustym miejscu na ekranie głównym
2. Wybierz **"Widżety"** lub **"Widget"**
3. Znajdź **"Baby Tracker"**
4. Przeciągnij widget na ekran
5. Możesz teraz zapisywać zdarzenia jednym dotknięciem bez otwierania aplikacji!

---

## Wskazówki

- **Backup danych**: Aplikacja przechowuje dane lokalnie. Włącz backup Google w ustawieniach telefonu żeby dane były bezpieczne.
- **Eksport**: W zakładce Dashboard użyj ikony pobierania aby wyeksportować dane do pliku CSV (Excel).
- **Aktualizacja**: Po opublikowaniu aktualizacji w Play Store użytkownicy otrzymają ją automatycznie.
