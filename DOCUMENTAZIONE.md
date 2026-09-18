# Documentazione EConTab - Applicazione Android

## Informazioni Generali

### Nome Applicazione
**EConTab** (RV.App)

### Package
`pfa.app.econtab`

### Versione
- **Version Code**: 108
- **Version Name**: 2.10

### Descrizione
Applicazione Android per il progetto RV.Web dedicata alla gestione di preventivi, cantieri e impianti elettrici per installatori e professionisti del settore.

---

## Requisiti Tecnici

### Ambiente di Sviluppo
- **Linguaggio**: Java
- **Build System**: Gradle
- **Android Gradle Plugin**: 8.5.2
- **Compile SDK**: 33
- **Build Tools**: 33.0.0
- **Min SDK**: 26 (Android 8.0 Oreo)

### Permessi Richiesti
L'applicazione richiede i seguenti permessi Android:
- `INTERNET` - Connessione a internet per sincronizzazione dati
- `ACCESS_NETWORK_STATE` - Verifica stato connessione di rete
- `ACCESS_WIFI_STATE` - Verifica stato WiFi
- `WRITE_EXTERNAL_STORAGE` - Scrittura file su storage
- `READ_PHONE_STATE` - Lettura stato del telefono
- `GET_ACCOUNTS` - Accesso agli account Google
- `READ_CONTACTS` - Lettura contatti
- `com.android.vending.BILLING` - Gestione acquisti in-app

### Dipendenze Principali
```gradle
- com.google.android.material:material:1.0.0
- androidx.legacy:legacy-support-v4:1.0.0
- androidx.appcompat:appcompat:1.0.2
- com.android.billingclient:billing:6.0.1
- com.github.ok2c.hc4.android:httpclient-android:0.1.0
- org.jetbrains:annotations:15.0
```

---

## Architettura dell'Applicazione

### Struttura del Progetto

#### Package Principale: `pfa.app.econtab`
L'applicazione è organizzata in diversi package:

1. **Package principale** (`pfa.app.econtab`)
   - Contiene 173 file Java
   - Activity principali e logica di business

2. **Package `db`** - Gestione Database
   - `DbInterno.java` - Classe principale per operazioni database SQLite
   - `Join.java` - Gestione join tra tabelle
   - **Sottopacchetto `table`** - Classi entity per tabelle database

3. **Package `utils`** - Utility
   - Classi helper e utility per operazioni comuni
   - Gestione sessione, licenze, download

4. **Package `servizi`** - Servizi
   - `EConTabService` - Servizio in background per sincronizzazione

5. **Package `server`**
   - Gestione comunicazione con server remoto
   - `ConfigurazioneGenActivity` - Configurazione server

6. **Package Esterni**:
   - `com.android.vending.billing.util` - Gestione billing Google Play
   - `com.polites.android` - Libreria per zoom/gesture su immagini
   - `pfa.app.general.simplecropimage` - Crop immagini

---

## Funzionalità Principali

### 1. Gestione Clienti
**Activity**: `ClientiActivity`, `ClientiDettaglioActivity`, `ClientiDettaglioModActivity`

Gestione anagrafica clienti con possibilità di:
- Visualizzazione lista clienti
- Dettaglio cliente
- Modifica/inserimento dati cliente

### 2. Gestione Cantieri
**Activity**: `CantieriActivity`, `CantieriDettaglioModActivity`, `CantiereSplitActivity`

Sistema completo per la gestione dei cantieri:
- Elenco cantieri
- Dettaglio cantiere con struttura gerarchica
- Split cantieri
- Gestione unità, aree e locali

**Struttura Gerarchica Cantiere**:
- **Cantiere** → Unità → Aree → Locali
- `UnitaDettaglioModActivity` - Gestione unità
- `AreaDettaglioModActivity` - Gestione aree
- `LocaleDettaglioModActivity` - Gestione locali
- `PiantinaLocaleModActivity` - Piantina del locale

### 3. Gestione Preventivi
**Activity**: `PreventiviActivity`, `PreventiviDettaglioModActivity`

Sistema di preventivazione con:
- Creazione preventivi
- Associazione a cantieri e clienti
- Stati preventivo (Aperto, Accettato, Rifiutato, Chiuso)
- Trasformazione preventivi in ordini
- Aggiornamento prezzi automatico
- Esportazione

### 4. Gestione Elementi e Componenti
**Activity**: `ElementoModActivity`, `ElementoDettaglioActivity`, `ComponenteModActivity`

Gestione completa di elementi elettrici e componenti:
- **Elementi**: Elementi dell'impianto elettrico
- **Componenti**: Componenti specifici da associare agli elementi
- Categorie e sottocategorie
- Icone personalizzate
- Foto elementi (`FotoElementi`)

### 5. Gestione Fornitori e Listini
**Activity**: `FornitoriLineeActivity`, `LineeDettaglioModActivity`, `PlaccheDettaglioModActivity`

Sistema di gestione listini:
- Fornitori
- Linee prodotto
- Placche e accessori
- Codici articoli (`CodiciArticoliActivity`, `CodiceArticoloModActivity`)
- Associazione codici articoli a linee (`AssCodiciLineeActivity`)

### 6. Composizione Quadri Elettrici
**Activity**: `ComposizioneQuadroActivity`, `ComposizioneActivity`, `ComposizioneLiberaActivity`

Funzionalità avanzate per:
- Composizione quadri elettrici
- Selezione componenti
- Calcolo automatico materiali
- Visualizzazione composizioni

### 7. Collegamenti Elettrici
**Activity**: `CollegamentoActivity`, `CollegamentiElementoActivity`, `RelazioneActivity`

Gestione collegamenti tra elementi:
- Collegamenti tra elementi
- Relazioni tra componenti
- Cavi e tubi
- Calcolo metri cavo da tubo

### 8. Rapportini
**Activity**: `RapportiniActivity`, `RapportinoDettaglioModActivity`

Sistema per gestione rapportini di lavoro giornalieri.

### 9. Configurazione e Impostazioni
**Activity**: `ConfigurazioneActivity`, `AziendaDettaglioActivity`, `CostruttoriDettaglioActivity`

Configurazione completa:
- Dati azienda
- Dati costruttori
- IVA (`IvaDettaglioActivity`)
- Unità di misura (`UnitaMisuraDettaglioActivity`)
- Manodopera (`ManodoperaDettaglioActivity`)
- Configurazione server (`ConfigurazioneGenActivity`)

### 10. Sistema di Licenze e Abbonamenti
**Activity**: `GestAbbonamentoActivity`, `RegistrazioneActivity`, `LoginActivity`

Gestione licenze:
- **Licenza Gratis** (Light) - Funzionalità limitate
- **Licenza Full** - Funzionalità complete
- **Licenza Business** - Con sincronizzazione cloud
- Sistema billing Google Play integrato
- Registrazione utente obbligatoria

### 11. Sincronizzazione Cloud
**Service**: `EConTabService`

Sistema di sincronizzazione con server cloud:
- Download dati iniziali (listino base)
- Sincronizzazione bidirezionale
- Upload/download immagini
- Gestione conflitti

### 12. Gestione Immagini
**Activity**: `ImmagineActivity`, `CropImage`

Funzionalità immagini:
- Visualizzazione immagini con zoom/gesture
- Crop immagini
- Upload/download immagini da server
- Icone elementi e componenti

### 13. Utilità Aggiuntive
- **File Chooser**: `EConTabFileChooserActivity`
- **SQL Manager**: `SqlManagerActivity` - Gestione database per debug
- **Crash Handler**: `CrashActivity` - Gestione errori
- **Finestre Popup**:
  - `FinestraElementiActivity`
  - `FinestraComponentiActivity`
  - `FinestraListinoBaseActivity`
  - `FinestraLocaliPreferitiActivity`

---

## Database

### Gestione Database
- **Classe Principale**: `DbInterno.java`
- **Tipo**: SQLite locale
- **Sincronizzazione**: Con server remoto tramite JSON

### Tabelle Principali (Package `db.table`)
- `Anagrafica` - Dati anagrafici generici
- `Cantieri` - Cantieri
- `Clienti` - Clienti (estende Anagrafica)
- `Ditte` - Ditte/Fornitori
- `Aree` - Aree dei cantieri
- `Locali` - Locali delle aree
- `Elementi` - Elementi elettrici
- `ElementiCantiere` - Elementi associati ai cantieri
- `ElementiCodici` - Codici articolo per elementi
- `Componenti` - Componenti elettrici
- `ComponentiCantiere` - Componenti in cantiere
- `ComponentiComposti` - Composizione componenti
- `Composizioni` - Composizioni elementi
- `ComposizioniCantiere` - Composizioni in cantiere
- `CategorieGenerali` - Categorie elementi
- `CategorieComponenti` - Categorie componenti
- `Collegamenti` - Collegamenti elettrici
- `Costruttori` - Costruttori/produttori
- `FotoElementi` - Foto associate agli elementi
- `IVA` - Aliquote IVA
- `Linee` - Linee prodotto
- `Manodopera` - Tariffe manodopera
- `Placche` - Placche e accessori
- `Preventivi` - Preventivi
- `Rapportini` - Rapportini di lavoro
- `UnitaMisura` - Unità di misura

---

## Flusso Applicativo

### 1. Avvio Applicazione
**Activity**: `StartActivity` (LAUNCHER)

Sequenza di avvio:
1. Verifica permessi storage (READ/WRITE_EXTERNAL_STORAGE)
2. Mostra splash screen (3 secondi)
3. Verifica accettazione Termini e Condizioni (TAC)
4. Se prima apertura: download listino base da internet
5. Redirect a `MenuActivity`

### 2. Menu Principale
**Activity**: `MenuActivity`

Menu principale con pulsanti per:
- Clienti
- Cantieri
- Listini
- Preventivi
- Rapportini
- Impostazioni
- Guida
- Sincronizzazione/Upgrade (in base alla licenza)

Funzionalità automatiche:
- Verifica aggiornamenti app (una volta al giorno)
- Mostra dialog guida al primo accesso
- Controllo stato licenza

### 3. Workflow Tipico

#### Scenario 1: Creazione Preventivo
1. Menu → Clienti → Seleziona/Crea cliente
2. Menu → Cantieri → Seleziona/Crea cantiere
3. Aggiungi Unità → Aggiungi Aree → Aggiungi Locali
4. In ogni locale: aggiungi elementi elettrici
5. Per ogni elemento: scegli componenti
6. Menu → Preventivi → Crea nuovo preventivo
7. Associa cliente e cantiere
8. Associa codici articoli automaticamente
9. Esporta preventivo

#### Scenario 2: Gestione Cantiere
1. Menu → Cantieri → Seleziona cantiere
2. Visualizza struttura gerarchica
3. Aggiungi locali e piantine
4. Posiziona elementi sulla piantina
5. Crea collegamenti tra elementi
6. Genera report materiali

---

## Configurazione Server

### URL Server
Definito in `Globals.java`:
```java
public static final String LICENSE_URL_SERVER = "http://147.93.127.131:5151";
public static final String CLOUD_URL_SERVER_PORT = "80";
public static final String DL_WEB_SERVICE = "sincronizzatore";
```

### Comunicazione Server
- Metodo: POST (configurabile)
- Formato dati: JSON
- Autenticazione: Account Google
- Sincronizzazione: Bidirezionale con timestamp

---

## Sistema di Licenze

### Tipi di Licenza
1. **GRATIS (Light)**
   - Funzionalità base
   - Inserimenti limitati
   - No esportazioni Excel
   - No sincronizzazione

2. **FULL**
   - Tutte le funzionalità
   - Inserimenti illimitati
   - Esportazioni Excel
   - No sincronizzazione cloud

3. **BUSINESS**
   - Tutte le funzionalità Full
   - Sincronizzazione cloud
   - Backup automatico
   - Multi-dispositivo

### Gestione Licenza
- Account Google obbligatorio
- Verifica licenza tramite API server
- Billing Google Play integrato
- Periodo di prova: 15 giorni

---

## File e Configurazioni

### Percorsi File
- Database: Interno all'app (SQLite)
- Immagini elementi: Directory privata app
- Icone componenti: Directory privata app
- Backup: ECONTAB/backup (storage esterno)
- Accettazione TAC: File privato `tac.dat`

### SharedPreferences
Nome: `APP_NAME` (definito in `Utility`)

Preferenze salvate:
- `ECONTAB_REG` - Stato registrazione
- `PERINIZIARE` - Flag prima apertura
- `DATA_VERIFICA_AGGIORNAMERNTI` - Ultima verifica aggiornamenti

---

## Caratteristiche Tecniche

### Gestione Orientamento
Tutte le Activity hanno:
```xml
android:configChanges="orientation|screenSize"
```
Per gestire manualmente i cambiamenti di orientamento.

### Heap Memory
```xml
android:largeHeap="true"
```
Richiesto per gestione immagini e database di grandi dimensioni.

### Cleartext Traffic
```xml
android:usesCleartextTraffic="true"
```
Permette connessioni HTTP non cifrate (per retrocompatibilità).

### Debug Mode
Controllato da `Globals.DEBUG_MODE`:
- `false` in produzione
- Disabilita output console quando false

---

## Activity Popup

Alcune Activity utilizzano tema popup (`@style/EConTabPopup`):
- `FinestraElementiActivity`
- `FinestraComponentiActivity`
- `FinestraListinoBaseActivity`
- `ImmagineActivity`
- `ComposizioneActivity`
- `ComposizioneQuadroActivity`
- `PlaccheActivity`
- `CollegamentoActivity`
- `RelazioneActivity`
- `CollegamentiElementoActivity`
- `FinestraLocaliPreferitiActivity`
- `ElementoDettaglioActivity`
- `EConTabFileChooserActivity`
- `ConfigurazioneGenActivity`
- `ComposizioneLiberaActivity`
- `AssCodiciLineeActivity`
- `SqlManagerActivity`

---

## Librerie Terze Parti

### Billing Google Play
Package: `com.android.vending.billing.util`
- `IabHelper` - Helper per in-app billing
- `IabResult` - Risultati operazioni
- `Purchase` - Acquisti
- `Inventory` - Inventario prodotti
- `SkuDetails` - Dettagli SKU
- `Security` - Validazione sicurezza

### Gesture Image View
Package: `com.polites.android`
- `GestureImageView` - ImageView con zoom/pan/gesture
- Animazioni zoom e fling
- Touch listener personalizzato

### Simple Crop Image
Package: `pfa.app.general.simplecropimage`
- `CropImage` - Activity per crop immagini
- `CropImageView` - View personalizzata
- Rotazione immagini

---

## Note di Sviluppo

### Packaging
```gradle
packagingOptions {
    exclude("META-INF/DEPENDENCIES")
    exclude("META-INF/LICENSE")
    exclude("META-INF/LICENSE.txt")
    exclude("META-INF/license.txt")
    exclude("META-INF/NOTICE")
    exclude("META-INF/NOTICE.txt")
    exclude("META-INF/notice.txt")
    exclude("META-INF/ASL2.0")
    exclude("META-INF/*.kotlin_module")
}
```

### ProGuard
```gradle
minifyEnabled false
```
Offuscamento disabilitato per facilitare debug.

### Build Type Release
```gradle
debuggable true
```
Debug abilitato anche in release per troubleshooting.

---

## Risorse UI

### Layout
- 121 file XML di layout
- Layout responsive per diverse dimensioni schermo
- Supporto orientamento portrait/landscape

### Stringhe
File: `res/values/strings.xml`
- Supporto multilingua (IT/EN)
- Messaggi errore localizzati
- Testi UI configurabili

### Temi
- `@style/AppTheme` - Tema principale
- `@style/EConTabPopup` - Tema finestre popup
- `@style/EConTabPopupNoAnimation` - Popup senza animazioni

---

## Sicurezza

### File Provider
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.provider"
    android:exported="false"
    android:grantUriPermissions="true">
```
Per condivisione sicura file tra app.

### Validazione Licenza
- Controllo firma digitale acquisti Google Play
- Verifica ID dispositivo
- Crittografia dati licenza (opzionale)
- Validazione server-side

---

## Supporto e Documentazione

### Email Supporto
`pfaimpianti@gmail.com` (definito in `Globals.PUBLISHER_EMAIL_ADDRESS`)

### Risorse Guida
- Video guide online
- FAQ
- Manuale PDF (asset: `manuale.pdf`)
- Richiesta assistenza integrata

---

## Version History

### Versione Corrente: 2.10 (108)
Features:
- Sistema completo gestione cantieri
- Preventivi e ordini
- Sincronizzazione cloud
- Gestione rapportini
- Sistema licenze multi-tier

### Note di Versione
- SDK Target: Android 13 (API 33)
- Min SDK: Android 8.0 (API 26)
- Build Tools: 33.0.0

---

## Deployment

### Build
```bash
./gradlew assembleRelease
```

### Output
- APK: `app/build/outputs/apk/release/app-release.apk`
- Package: `pfa.app.econtab`

### Distribuzione
- Google Play Store
- In-app billing configurato
- Update checker integrato

---

## Troubleshooting

### Crash Activity
`CrashActivity` gestisce gli errori non previsti e fornisce:
- Log dettagliati
- Opzione per inviare report
- Recovery graceful

### SQL Manager
`SqlManagerActivity` permette:
- Ispezione database
- Query SQL personalizzate
- Export/import dati
- Debug struttura dati

### Backup/Restore
- Backup manuale database in `ECONTAB/backup`
- Restore da file backup
- Sincronizzazione cloud come backup

---

## Contatti Sviluppo

Per informazioni tecniche o supporto allo sviluppo:
- Email: pfaimpianti@gmail.com
- Server API: http://147.93.127.131:5151

---

*Documentazione generata il: 21 Febbraio 2026*
*Versione App Documentata: 2.10 (108)*
