# Analisi Compatibilità Cross-Platform — EConTab Android App

**Data analisi**: 2026-05-09  
**App attuale**: Android nativa in Java  
**Versione**: 2.10 (versionCode 108)  
**Obiettivo**: Portare su iOS mantenendo tutte le funzionalità

---

## 1. Stato Attuale

| Metrica | Valore |
|--------|--------|
| Linguaggio | Java (Android nativo) |
| File Java | 173 |
| Linee di codice | ~54.000 LOC |
| Activities | 40+ |
| Fragment | 15+ |
| Custom Views | 20+ |
| Tabelle DB | 40+ (SQLite) |
| Layout XML | 80+ |
| Drawable | 180+ |
| SDK minimo | 26 (Android 8.0 Oreo) |
| SDK target | 33 (Android 13) |

---

## 2. Funzionalità dell'App

### 2.1 Moduli Principali

| Modulo | Descrizione |
|--------|-------------|
| Anagrafica | Gestione clienti, aziende, costruttori |
| Cantieri | Struttura gerarchica: Cantiere → Unità → Aree → Locali → Elementi |
| Elementi & Componenti | Gestione elementi elettrici con foto, icone, categorie |
| Preventivi | Creazione, modifica, stati (Aperto/Accettato/Rifiutato/Chiuso), export XLS |
| Rapportini | Gestione rapportini di lavoro, export XLS |
| Composizione Quadri | Wizard composizione quadri elettrici con calcolo automatico materiali |
| Listini & Fornitori | Gestione listini, placche, codici articoli, associazioni |
| Piantina Interattiva | Editor planimetrico con disegno su canvas, zoom/pan gestuale |
| Sincronizzazione | Sync SOAP con server remoto, gestione eliminazioni |
| Licenze | Sistema licenze LIGHT/FULL/BUSINESS con Google Play Billing |
| Export | Export XLS per preventivi, rapportini, consuntivi, composizioni |
| Multi-lingua | Italiano (IT), altro (SI) |

### 2.2 Infrastruttura

| Componente | Dettaglio |
|-----------|-----------|
| Database | SQLite locale (`ECONTAB.db`, schema version 20) |
| Sincronizzazione | SOAP web service (ksoap2) |
| Billing | Google Play Billing v6.0.1 + legacy IabHelper |
| Export | Apache POI 3.7 (formato XLS) |
| HTTP | Apache HttpClient legacy + HttpURLConnection |
| Foto/Immagini | Camera Intent, FileProvider, crop image |

---

## 3. Blockers Android-Specifici

### 3.1 Bloccanti Critici (rewrite totale)

| Componente Android | Equivalente iOS | Note |
|-------------------|-----------------|------|
| Activity system (40+) | UIViewController | Navigazione completamente diversa |
| Fragment + ViewPager (15+) | Container VC / UIPageVC | Paradigma UI diverso |
| XML Layouts (80+) | Storyboard / SwiftUI / Auto Layout | Formato incompatibile |
| Intent navigation | Segue / NavigationStack | Meccanismo diverso |
| SQLite (DbInterno.java) | Core Data / Realm / SQLite-swift | API diverse |
| Google Play Billing | StoreKit 2 | Piattaforme diverse |
| android.app.Service | URLSession / Background Modes | Paradigmi background diversi |

### 3.2 Refactoring Significativo

| Componente Android | Equivalente iOS |
|-------------------|-----------------|
| SharedPreferences | UserDefaults / Keychain |
| Environment / FileProvider | NSDocumentDirectory / NSFileManager |
| Runtime Permissions | Info.plist + authorization requests |
| READ_PHONE_STATE (IMEI) | UIDevice.identifierForVendor |
| Camera Intent + MediaStore | AVFoundation / UIImagePickerController |
| READ_CONTACTS | CNContactStore / Contacts framework |

### 3.3 Librerie da Sostituire

| Libreria Android | Alternativa Cross-Platform |
|-----------------|---------------------------|
| ksoap2 (SOAP client) | URLSession + XML parsing nativo |
| Apache POI 3.7 (XLS) | Libreria alternativa o export lato server |
| IabHelper (legacy billing) | (già da sostituire anche su Android) |
| com.polites GestureImageView | Gesture recognizers nativi |
| SimpleCropImage | Libreria crop cross-platform |

---

## 4. Strategie di Migrazione

### Opzione A — Flutter (CONSIGLIATA)

**Linguaggio**: Dart  
**UI**: Widget tree cross-platform  
**DB**: sqflite (SQLite su entrambe le piattaforme)  
**Billing**: in_app_purchase package  
**HTTP/SOAP**: http package + xml parsing  
**Export XLS**: excel package  

**Vantaggi**:
- Un solo codebase per Android e iOS
- Ottimo supporto SQLite (sqflite), molto simile all'attuale struttura
- Widget personalizzati: ottimo supporto per custom views e canvas (CustomPainter per piantina)
- Performance vicina al nativo con Impeller
- Dart è tipizzato, più simile a Java

**Svantaggi**:
- Rewrite completo dell'UI (80+ layout → Widget)
- Curva di apprendimento Dart
- Dipendenza da Flutter engine (~10MB overhead)

**Stima**: 5-8 mesi, 1-2 sviluppatori

---

### Opzione B — React Native

**Linguaggio**: TypeScript/JavaScript  
**UI**: Componenti React cross-platform  
**DB**: react-native-sqlite-storage / expo-sqlite  
**Billing**: react-native-iap  
**HTTP/SOAP**: axios + fast-xml-parser  
**Export XLS**: xlsx package  

**Vantaggi**:
- Grande ecosistema npm
- Hot reload veloce
- Buona comunità

**Svantaggi**:
- Bridge JS/native può causare problemi di performance con liste grandi
- Meno adatto per la piantina interattiva (canvas drawing)
- Debugging più complesso con bridge
- Librerie XLS limitate rispetto a POI

**Stima**: 6-9 mesi, 1-2 sviluppatori

---

### Opzione C — Kotlin Multiplatform Mobile (KMM)

**Linguaggio**: Kotlin (condiviso) + Swift (iOS UI)  
**Logica condivisa**: Business logic, DB, network  
**UI Android**: Jetpack Compose  
**UI iOS**: SwiftUI  

**Vantaggi**:
- Riuso del codice business logic (DB, sync, calcoli) — ~60-70% del codice riusabile
- UI nativa su entrambe le piattaforme
- Java → Kotlin conversione automatica disponibile
- Database: SQLDelight (SQLite cross-platform, simile al DbInterno attuale)

**Svantaggi**:
- Serve scrivere UI due volte (Compose + SwiftUI)
- Più complesso da gestire
- KMM ancora maturo ma meno diffuso di Flutter/RN

**Stima**: 7-10 mesi, 2 sviluppatori (uno Android, uno iOS)

---

### Opzione D — Approccio ibrido Web (Capacitor/Ionic)

**Linguaggio**: HTML/CSS/TypeScript  
**Rendering**: WebView  

**Non consigliata** per questa app: la piantina interattiva, le custom views e la gestione database locale ne farebbero risentire le performance in modo significativo.

---

## 5. Raccomandazione

### Flutter è la scelta ottimale per EConTab

**Motivazioni specifiche per questa app**:

1. **Database SQLite** — `sqflite` in Flutter usa lo stesso SQLite, la struttura di `DbInterno.java` con 40+ tabelle si porta quasi 1:1 in Dart
2. **Canvas drawing** — `CustomPainter` in Flutter è equivalente diretto di `Canvas onDraw()` usato nella piantina interattiva
3. **Gestione stato complessa** — Riverpod o BLoC gestiscono bene la complessità dell'app
4. **Tipizzazione** — Dart tipizzato è più simile a Java rispetto a JS
5. **Liste performanti** — `ListView.builder` / `GridView` ottimizzati meglio del bridge RN
6. **Custom views** — Tutti gli elementi personalizzati (EConTabElemento*, piantina) sono riproducibili con Widget + CustomPainter

---

## 6. Piano di Migrazione Flutter

### Fase 1 — Database & Business Logic (8-10 settimane)

- [ ] Setup progetto Flutter con struttura moduli
- [ ] Migrazione DbInterno.java → database layer con `sqflite`
- [ ] Migrazione classi entity (50+ table) → modelli Dart
- [ ] Migrazione Sincronizzatore.java → SOAP client Dart
- [ ] Migrazione logica export XLS (`excel` package)
- [ ] Migrazione sistema licenze
- [ ] Test unitari su business logic

### Fase 2 — UI Core (10-12 settimane)

- [ ] Struttura navigazione (GoRouter o Navigator 2.0)
- [ ] Schermata menu principale
- [ ] Modulo Clienti/Anagrafica
- [ ] Modulo Cantieri (struttura gerarchica)
- [ ] Modulo Elementi & Componenti
- [ ] Gestione foto (image_picker package)
- [ ] Adattamento assets (PNG → Flutter assets)
- [ ] Tema e stili

### Fase 3 — UI Avanzata (8-10 settimane)

- [ ] Modulo Preventivi con export XLS
- [ ] Modulo Rapportini con export XLS
- [ ] Piantina interattiva (CustomPainter + GestureDetector)
- [ ] Composizione Quadri (wizard multi-step)
- [ ] Modulo Listini & Fornitori
- [ ] Split view per tablet

### Fase 4 — Platform Integration (4-6 settimane)

- [ ] In-app purchases (Google Play + Apple StoreKit via `in_app_purchase`)
- [ ] Gestione permessi cross-platform
- [ ] File sharing e export su entrambe le piattaforme
- [ ] Gestione device ID per licenze
- [ ] Background sync service

### Fase 5 — QA & Store (3-4 settimane)

- [ ] Test su dispositivi Android reali
- [ ] Test su dispositivi iOS reali
- [ ] Beta testing
- [ ] Submission Play Store (aggiornamento)
- [ ] Submission App Store (nuovo)

**Totale stimato**: 33-42 settimane (8-10 mesi con buffer)

---

## 7. Stima Economica

| Componente | Settimane | Costo Stimato (senior dev) |
|-----------|-----------|--------------------------|
| Database & Backend | 8-10 | 8.000-10.000 € |
| UI Core | 10-12 | 10.000-12.000 € |
| UI Avanzata | 8-10 | 8.000-10.000 € |
| Platform Integration | 4-6 | 4.000-6.000 € |
| QA & Deploy | 3-4 | 3.000-4.000 € |
| **Totale** | **33-42** | **33.000-42.000 €** |

---

## 8. Rischi e Mitigazioni

| Rischio | Probabilità | Impatto | Mitigazione |
|---------|------------|---------|-------------|
| Complessità piantina interattiva | Alta | Medio | Prototipo iniziale con CustomPainter |
| Performance con 40+ tabelle SQLite | Media | Alto | sqflite + ottimizzazione query |
| SOAP sync complesso | Media | Medio | Test unitari, refactor verso REST se possibile |
| Export XLS (POI 3.7 → Flutter) | Bassa | Medio | Package excel, o export server-side |
| Licenze/Billing cross-platform | Media | Alto | in_app_purchase package, test su entrambe le piattaforme |
| App Store review (Apple) | Alta | Alto | Revisione HIG, validazione anticipata |

---

## 9. Note Tecniche Importanti

### Database Migration
Il `DbInterno.java` usa `SQLiteOpenHelper` con `onUpgrade()` per le migrazioni. In Flutter con `sqflite` il pattern è identico (parametro `version` + callback `onUpgrade`). Lo schema attuale alla version 20 va migrato con la stessa logica.

### Piantina Interattiva
`EConTabPiantinaImageView` usa `Canvas.drawBitmap()`, `Canvas.drawRect()`, gestione touch per posizionare elementi sulla planimetria. In Flutter si replica con `CustomPainter` + `GestureDetector`. La libreria `com.polites.android` per zoom/pan si sostituisce con `InteractiveViewer` built-in in Flutter.

### SOAP Calls
`Sincronizzatore.java` usa ksoap2. In Flutter non esiste un client SOAP diretto maturo; la soluzione è usare `http` package con XML building/parsing manuale oppure valutare una migrazione verso REST API sul server (se fattibile su Mercury server).

### Export XLS
Apache POI 3.7 non è portabile. In Flutter il package `excel` supporta formato `.xlsx` (OOXML). Se si devono generare `.xls` (formato vecchio), l'alternativa è generarli lato server e scaricarli.

### Billing
`IabHelper` è legacy anche su Android (va sostituito con BillingClient v6 già incluso). Su Flutter, `in_app_purchase` gestisce entrambe le piattaforme con una sola API.

---

## 10. Quick Win — Android Prima, iOS Dopo

Se si vuole procedere per gradi senza bloccare lo sviluppo corrente:

1. **Prima mossa**: Aggiornare l'app Android esistente:
   - Sostituire `IabHelper` legacy con `BillingClient` v6 moderno
   - Aggiornare dipendenze AndroidX (appcompat 1.0.2 → latest)
   - Material Components 1.0.0 → Material 3
   - Risolvere `debuggable: true` in release build

2. **Secondo passo**: Avviare il progetto Flutter in parallelo, partendo da database layer e logica di business

3. **Terzo passo**: UI Flutter Android, poi iOS

---

*Documento generato da analisi automatica del sorgente — 2026-05-09*
