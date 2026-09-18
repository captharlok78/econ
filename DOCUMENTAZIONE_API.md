# Documentazione API - EConTab Android App

## Informazioni Generali

### Base URL Server
```
http://147.93.127.131:5151
```

Definito in: `Globals.LICENSE_URL_SERVER`

### Porta Server Cloud
```
80
```

Definito in: `Globals.CLOUD_URL_SERVER_PORT`

### Web Service
```
sincronizzatore
```

Definito in: `Globals.DL_WEB_SERVICE`

---

## Architettura API

### Protocolli Supportati
1. **HTTP POST con JSON** (Predefinito)
   - Content-Type: `application/json`
   - Formato dati: JSON
   - Timeout: 60 secondi (connect + read)

2. **HTTP GET con parametri URL** (Opzionale)
   - Parametri passati via query string
   - Separatore: `;`

### Controllo Protocollo
```java
Globals.INVIA_RICHIESTA_USE_POST = true; // Usa POST (default)
```

### Formato Comunicazione
- **Richiesta**: JSON compresso (GZIP)
- **Risposta**: JSON compresso (GZIP)
- **Encoding**: UTF-8

---

## Endpoint API

### 1. Sincronizzazione Dati

#### **POST** `/sincronizzatore/syncDownloadPOST`

Sincronizzazione bidirezionale dei dati tra app e server.

**Classe**: `Sincronizzatore.java`
**Metodo**: `syncDownload`

##### Request Body (JSON)
```json
{
  "dataSitesQuotes": "20150101000000",
  "login": "CODICE_CLIENTE|ID_OPERATORE|PASSWORD",
  "dataUltimaSincronizzazione": "20240101000000",
  "parametri": "<DATI_COMPRESSI_BASE64>"
}
```

**Parametri**:
- `dataSitesQuotes` (string): Data sincronizzazione cantieri/preventivi (formato: yyyyMMddHHmmss)
- `login` (string): Credenziali formato `CODICE_CLIENTE|ID_OPERATORE|PASSWORD`
- `dataUltimaSincronizzazione` (string): Data ultima sincronizzazione (formato: yyyyMMddHHmmss)
- `parametri` (string): Dati da inviare compressi e codificati in Base64

##### Struttura Parametri (decompresso)
```json
{
  "INSERT": {
    "cantieri": [...],
    "elementi_cantiere": [...],
    "preventivi": [...]
  },
  "UPDATE": {
    "cantieri": [...],
    "clienti": [...]
  },
  "DELETE": {
    "cantieri": [...]
  }
}
```

##### Response (JSON compresso)
```json
{
  "DATASYNC": "20240803120000",
  "cantieri": [...],
  "clienti": [...],
  "elementi": [...],
  "componenti": [...],
  "record_eliminati": [...]
}
```

**Codici di Risposta**:
- `200 OK`: Sincronizzazione riuscita
- `500 Internal Server Error`: Errore server

**Note**:
- Data sincronizzazione viene aggiornata con timestamp server + tempo operazioni locali
- Supporta sincronizzazione totale (tabelle specificate) o incrementale (solo modifiche)

---

### 2. Upload Immagini

#### **POST** `/sincronizzatore/uploadFile`

Upload di icone elementi e componenti modificati.

**Classe**: `Sincronizzatore.java`
**Metodo**: `sincronizzaImmagini()`

##### Request Body (JSON)
```json
{
  "login": "CODICE_CLIENTE|ID_OPERATORE|PASSWORD",
  "dataUltimaSincronizzazione": "20240101000000",
  "images": [
    {
      "fileName": "immagini/icons/elemento_123.png",
      "fileContent": "<BASE64_ENCODED_IMAGE>"
    },
    {
      "fileName": "immagini/icons/componente_456.png",
      "fileContent": "<BASE64_ENCODED_IMAGE>"
    }
  ]
}
```

**Parametri**:
- `login` (string): Credenziali autenticazione
- `dataUltimaSincronizzazione` (string): Data ultima sincronizzazione immagini
- `images` (array): Array di oggetti immagine
  - `fileName` (string): Percorso relativo file
  - `fileContent` (string): Contenuto file codificato Base64

##### Response (JSON compresso)
```json
{
  "DATASYNC": "20240803120000",
  "categorie_generali": ["icona1.png", "icona2.png"],
  "categorie_componenti": ["icona3.png"],
  "elementi": ["elemento_789.png"],
  "componenti": ["componente_012.png"]
}
```

**Response Fields**:
- `DATASYNC`: Timestamp sincronizzazione
- `<tabella>`: Array di nomi file da scaricare per ogni tabella

**Note**:
- Solo le icone modificate vengono caricate (tracciate in tabella `icone_modificate`)
- Le thumbnail vengono sincronizzate separatamente

---

### 3. Download Immagini

#### **GET** `/images/icons/{filename}`

Download singola icona.

**Classe**: `DownloadUtil.java`
**Metodo**: `scaricaIcona()`

##### Request
```
GET http://147.93.127.131:80/images/icons/elemento_123.png
```

##### Response
- **Content-Type**: `image/png` o `image/jpeg`
- **Body**: File binario immagine

**Thumbnail**:
```
GET http://147.93.127.131:80/images/icons/thumb/elemento_123.png
```

**Note**:
- Configurabile se thumb folder = icons folder (`Globals.THUMB_FOLDER_IS_ICONS_FOLDER`)

---

### 4. Verifica Licenza Server

#### **GET** `/license/attivazioneServer`

Verifica validità licenza Business (cloud).

**Classe**: `Sincronizzatore.java`
**Metodo**: `controllaAttivazione()`

##### Request
```
GET /license/attivazioneServer?action=C&ID=<CODICE_ENCRYPTED>
```

**Parametri**:
- `action` (string): `C` = Check
- `ID` (string): Codice attivazione criptato e URL-encoded

**Formato ID**:
```
URLEncode(Encrypt(CODICE_ATTIVAZIONE))
```

##### Response (Plain Text)
```
ATTIVA
```
o
```
NON_ATTIVA
```

**Codici di Risposta**:
- `200 OK`: Licenza valida
- `200 OK` con "NON_ATTIVA": Licenza non valida

**Note**:
- Verifica eseguita max 1 volta al giorno
- Data ultima verifica salvata in SharedPreferences: `DATA_VERIFICA`

---

### 5. Verifica Licenza Full

#### **GET** `/license/attivazioneFull`

Verifica validità licenza Full o abbonamento.

**Classe**: `Licenza.java`
**Metodo**: `controllaAbbonamentoOnLine()`

##### Request
```
GET /license/attivazioneFull?action=C&ID=<ID_ENCRYPTED>
```

**Parametri**:
- `action` (string): `C` = Check
- `ID` (string): ID composto criptato

**Formato ID**:
```
URLEncode(Encrypt("APP_NAME|ECONTAB_REG|DEVICE_ID|CODICE_FULL"))
```

Dove:
- `APP_NAME`: Nome applicazione
- `ECONTAB_REG`: Codice registrazione utente
- `DEVICE_ID`: Android ID dispositivo
- `CODICE_FULL`: Codice licenza Full

##### Response (Plain Text)
```
ATTIVA
```
o
```
SCADUTA
```
o
```
NON_ATTIVA
```

**Codici di Risposta**:
- `200 OK`: Check eseguito

---

### 6. Verifica Versione App

#### **GET** `/mobileapp/version`

Verifica disponibilità aggiornamenti app.

**Classe**: `MenuActivity.java`
**Metodo**: `onResume()`

##### Request
```
GET /mobileapp/version?type=Android
```

**Parametri**:
- `type` (string): Piattaforma (`Android`)

##### Response (Plain Text)
```
108
```

**Response**: Version code ultima versione disponibile (numero intero)

**Note**:
- Verifica eseguita max 1 volta al giorno
- Confronto con `PackageInfo.versionCode` locale
- Se nuova versione disponibile, mostra dialog con opzione aggiornamento

---

### 7. Download Database Iniziale (Deprecato)

#### **POST** `/TOBECHANGEDdownloadEConTabDb.asmx/downloadDati`

Download listino base iniziale (NON PIÙ USATO).

**Classe**: `DownloadUtil.java`
**Metodo**: `downloadDati()` (commentato)

**Note**:
- Dal 14/02/2015 il database iniziale viene caricato da asset locali
- File asset: `dbiniziale_it.txt`, `dbiniziale_en.txt`, `dbiniziale_lk.txt`
- Selezione automatica in base a lingua dispositivo

---

## Autenticazione

### Sistema di Login

**Formato Credenziali**:
```
CODICE_CLIENTE|ID_OPERATORE|PASSWORD
```

Esempio:
```
PFA|1|Filippo
```

**Componenti**:
- `CODICE_CLIENTE`: Codice univoco cliente (da registrazione)
- `ID_OPERATORE`: ID operatore/utente nel sistema (0 se non autenticato)
- `PASSWORD`: Password operatore (vuota se ID_OPERATORE = 0)

### Identificazione Dispositivo

**Device ID**:
```java
String deviceId = Settings.Secure.getString(context.getContentResolver(),
                                            Settings.Secure.ANDROID_ID);
```

### Crittografia

**Algoritmo**: AES/CBC/PKCS5Padding (opzionale) o Base64 semplice

**Configurazione**:
```java
Globals.LICENSE_DATA_IS_ENCRYPTED = false; // Solo Base64
```

**Parametri Encryption** (quando attiva):
- Password: `apppfa14`
- Salt: `pfasas`
- IV: `e675f725e675f725`
- Key derivation: PBKDF2WithHmacSHA1
- Iterations: 65536
- Key size: 128 bit

**Funzione**:
```java
String encrypted = Licenza.encrypt(plaintext);
```

---

## Gestione Dati

### Compressione/Decompressione

**Classe**: `Utility.java`

#### Compressione
```java
String compressed = Utility.comprimi(jsonString);
```
- Input: JSON string
- Output: Base64 encoded GZIP compressed data

#### Decompressione
```java
String decompressed = Utility.decomprimi(compressedString);
```
- Input: Base64 encoded GZIP compressed data
- Output: JSON string

**Decompressione File**:
```java
String filePath = Utility.decomprimiFile(compressedData);
```
- Salva dati decompressi in file temporaneo
- Ritorna percorso file

---

### Formati Data/Ora

**Formato Standard**: `yyyyMMddHHmmss`

Esempi:
- `19700101000000` - Sync globale (epoch)
- `20240101000000` - 1 gennaio 2024, 00:00:00
- `20240803120000` - 3 agosto 2024, 12:00:00

**Conversione**:
```java
SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
String dataSync = sdf.format(new Date());
```

**Costanti Predefinite**:
- `DATA_SYNC_GLOBALE`: "19700101000000"
- `DATA_SYNC_GLOBALE_2024`: "20240101000000"

---

### Struttura Record Database

#### Record Nuovi (INSERT)
- Hanno `id < 0` (ID negativi temporanei)
- Vengono sostituiti con ID positivi dal server
- Dopo sincronizzazione, record con ID < 0 vengono eliminati localmente

#### Record Modificati (UPDATE)
- Tracciati tramite campo `data_ultima_modifica`
- Confronto con `dataUltimaSincronizzazione`

#### Record Eliminati (DELETE)
- Salvati in tabella speciale `record_eliminati`
- Formato chiave: `id_campo1|id_campo2|...`
- Vengono inviati al server e poi cancellati localmente

#### Tabella `record_eliminati`
```json
{
  "tabella": "cantieri",
  "chiave_record": "123|456"
}
```

---

## Tabelle Sincronizzate

### Tabelle Sistema
- `utenti`
- `utenti_ditta`
- `ditte`
- `livelli_utente`

### Tabelle Anagrafiche
- `clienti`
- `fornitori`
- `costruttori`

### Tabelle Cantieri
- `cantieri`
- `aree`
- `locali`
- `unita`

### Tabelle Elementi
- `elementi`
- `elementi_cantiere`
- `elementi_codici`
- `categorie_generali`
- `foto_elementi`

### Tabelle Componenti
- `componenti`
- `componenti_cantiere`
- `componenti_composti`
- `categorie_componenti`

### Tabelle Composizioni
- `composizioni`
- `composizioni_cantiere`

### Tabelle Listini
- `linee`
- `placche`
- `codici_articoli`
- `ass_codici_linee`
- `unita_misura`
- `manodopera`

### Tabelle Preventivi/Ordini
- `preventivi`
- `ordini`

### Tabelle Collegamenti
- `collegamenti`
- `relazioni`

### Tabelle Varie
- `rapportini`
- `iva`

---

## Flussi di Sincronizzazione

### 1. Prima Sincronizzazione (Nuova Installazione)

#### Sequenza:
1. App installa → Carica DB iniziale da asset (categorie + elementi base)
2. Utente si registra → Ottiene `CODICE_CLIENTE`
3. Utente fa login → Ottiene `ID_OPERATORE`
4. Prima sync → Download totale dati utente

**Parametri Prima Sync**:
- `tabelle`: "utenti|utenti_ditta|ditte|livelli_utente"
- `dataUltimaSincronizzazione`: "19700101000000"
- `primaSincronizzazione`: true

### 2. Sincronizzazione Incrementale

#### Upload (Client → Server):
```json
{
  "INSERT": {
    "cantieri": [<nuovi_cantieri_id_negativo>],
    "clienti": [<nuovi_clienti>]
  },
  "UPDATE": {
    "cantieri": [<cantieri_modificati>],
    "preventivi": [<preventivi_aggiornati>]
  },
  "DELETE": {
    "cantieri": [
      {"tabella": "cantieri", "chiave_record": "123"}
    ]
  }
}
```

#### Download (Server → Client):
```json
{
  "DATASYNC": "20240803153045",
  "cantieri": [<cantieri_nuovi_o_modificati>],
  "clienti": [<clienti_aggiornati>],
  "record_eliminati": [
    {"tabella": "cantieri", "chiave_record": "456"}
  ]
}
```

#### Processo:
1. **Pre-sync**: Raccolta modifiche locali
2. **Upload**: Invio modifiche al server
3. **Download**: Ricezione aggiornamenti server
4. **Apply**:
   - UPDATE/INSERT record ricevuti
   - DELETE record in `record_eliminati`
   - DELETE record locali con ID < 0
5. **Post-sync**: Aggiornamento timestamp + sync immagini

### 3. Sincronizzazione Immagini

**Eseguita separatamente dopo sync dati**

#### Upload Immagini:
- Cerca in `icone_modificate` per file da caricare
- Codifica in Base64
- Invia batch a `/sincronizzatore/uploadFile`
- Pulisce tabella `icone_modificate` se successo

#### Download Immagini:
- Server risponde con lista file da scaricare
- App scarica ogni file da `/images/icons/{filename}`
- Crea thumbnail (100x100)
- Salva in directory private app

---

## Gestione Errori

### Timeout
- **Connect Timeout**: 60000ms (60 secondi)
- **Read Timeout**: 60000ms (60 secondi)

### Retry Logic
- Verifica connessione prima di chiamata API
- Se offline, attende 4 secondi e riprova
- Se ancora offline, restituisce errore

### Error Handling

#### Errori HTTP
```java
if (HttpResult != HttpURLConnection.HTTP_OK) {
    // Gestione errore
}
```

#### Errori Sincronizzazione
- **IOException**: Errore connessione → Mostra `R.string.errore_connessione`
- **JSONException**: Errore parsing → Log stack trace
- **Exception generica**: Log e rollback transazione DB

#### Transazioni Database
```java
db.beginTransaction();
try {
    // Operazioni DB
    db.setTransactionSuccessful();
} finally {
    db.endTransaction();
}
```

### Response Error Formats

**Errore Upload Immagini**:
```
ERR: <messaggio_errore>
```

**Errore HTML** (server down):
```html
<html><title>Error</title>...</html>
```

---

## Progress Tracking

### Progress Dialog

**Classe**: `ProgressDialog`

#### Stati Progress:
1. **Indeterminate** (0-2%): Download risposta server
2. **Determinate** (2-5%): Decompressione file
3. **Per Tabella** (5-100%): Elaborazione tabelle
   - Formula: `progressAttuale + (currentTable / totalTables) * 100`

#### Titoli Progress:
- Aggiornamento dati: `"Aggiornamento dati X/Y"`
- Aggiornamento file: `"Aggiornamento files X/Y"`

### Callback Progress
```java
protected void onProgressUpdate(Integer... values) {
    pd.setProgress(values[0]);
    if (values.length > 1) {
        if (values[1] == -1) {
            // Aggiornamento immagini
        } else {
            // Aggiornamento dati
        }
    }
}
```

---

## Configurazione URL Server

### Metodo Configurazione
**Classe**: `Utility.java`
**Metodo**: `getURLServer(Context)`

### Sorgenti URL (in ordine):
1. **SharedPreferences**: `"URL_SERVER"` (se licenza Business)
2. **Globals.LICENSE_URL_SERVER**: Default hardcoded

### Formato URL
```
http://147.93.127.131:5151
```

**Senza trailing slash**

### URL Completi
- **Sincronizzatore**: `{URL_SERVER}/sincronizzatore`
- **Upload File**: `{URL_SERVER}/sincronizzatore/uploadFile`
- **Licenza**: `{URL_SERVER}/license/attivazioneServer`
- **Immagini**: `{URL_SERVER}:80/images`

---

## Best Practices

### 1. Chiamate API

#### Sempre verificare connessione
```java
if (!Utility.isOnline(context)) {
    // Gestisci offline
    return;
}
```

#### Usare transazioni per sync
```java
db.beginTransaction();
try {
    // Multiple DB operations
    db.setTransactionSuccessful();
} finally {
    db.endTransaction();
}
```

### 2. Gestione Memoria

#### Compressione Obbligatoria
- Tutti i payload JSON devono essere compressi
- Dimensione tipica: 90% riduzione

#### Streaming per File Grandi
```java
// Decomprimi in file invece che in memoria
String filePath = Utility.decomprimiFile(data);
```

### 3. Sicurezza

#### Non loggare dati sensibili
```java
if (Globals.DEBUG_MODE == false) {
    // Disabilita System.out
}
```

#### Validare risposte server
```java
if (result.startsWith("ERR:") ||
    (result.contains("<title>") && result.contains("<html>"))) {
    throw new Exception(result);
}
```

---

## Flags di Debug

### Globals.java

```java
// Modalità debug (disabilita log in produzione)
public static final boolean DEBUG_MODE = false;

// Usa POST invece di GET
public static final boolean INVIA_RICHIESTA_USE_POST = true;

// Forza upload di tutte le immagini
public static final boolean FORCE_IMMAGINI_TO_BE_UPLOADED = false;

// Sincronizza solo immagini (skip dati)
public static final boolean SINCRONIZZA_SOLO_IMMAGINI = false;

// Salta sincronizzazione immagini
public static final boolean SKIP_SYNCING_IMMAGINI = false;

// Forza uso account Google specifico
public static final boolean FORCE_GOOGLE_ACCOUNT = false;
public static final String FORCE_GOOGLE_ACCOUNT_EMAIL_ADDRESS = "...";

// Crittografia dati licenza
public static final boolean LICENSE_DATA_IS_ENCRYPTED = false;

// Thumb folder = icons folder
public static final boolean THUMB_FOLDER_IS_ICONS_FOLDER = true;

// Extra cavi/tubi in preventivo
public static final boolean EXTRA_CAVI_TUBI_IN_PREVENTIVO = true;
public static final int EXTRA_CAVI_TUBI_IN_PREVENTIVO_SINCE_DATE = 20240803;
```

---

## Rate Limiting & Throttling

### Verifica Licenza
- **Frequenza**: Max 1 volta al giorno
- **Storage**: SharedPreferences `DATA_VERIFICA`

### Verifica Aggiornamenti
- **Frequenza**: Max 1 volta al giorno
- **Storage**: SharedPreferences `DATA_VERIFICA_AGGIORNAMERNTI`

### Sincronizzazione
- **Frequenza**: A richiesta utente o automatica
- **No rate limiting** lato client

---

## Codici di Stato Licenza

### Tipi Licenza
```java
public static final int LIGHT = 0;     // Gratis
public static final int FULL = 1;      // A pagamento
public static final int BUSINESS = 2;  // Cloud + Sync
```

### Stati Abbonamento
```java
public static final int ABBONAMENTO_ATTIVO = 0;
public static final int ABBONAMENTO_SCADUTO = 1;
public static final int ABBONAMENTO_RIMBORSATO = 2;
```

### SKU Billing
```java
public static String ABBONAMENTO_MENSILE = "abb_mese_2";
public static String ABBONAMENTO_ANNUALE = "abb_anno";
```

---

## Esempi Chiamate API

### Esempio 1: Sincronizzazione Completa

```java
// Preparazione parametri
HashMap<String, Object> parametri = new HashMap<>();
parametri.put("login", "PFA|1|Filippo");
parametri.put("dataUltimaSincronizzazione", "20240101000000");

JSONObject datiUpload = getDatiNuoviModificati();
String parametriCompressi = Utility.comprimi(datiUpload.toString());
parametri.put("parametri", parametriCompressi);
parametri.put("dataSincroSitesQuotes", "20240101000000");

// Chiamata API
String url = "http://147.93.127.131:5151/sincronizzatore";
Object result = inviaRichiesta("syncDownload", "pfa.app.econtab/syncDownload",
                                url, parametri);

// Elaborazione risposta
String resultJSON = (String) result;
String filePath = Utility.decomprimiFile(resultJSON);
JSONObject response = new JSONObject(new FileReader(filePath));
```

### Esempio 2: Upload Immagine

```java
// Preparazione dati
JSONObject obj = new JSONObject();
obj.put("login", "PFA|1|Filippo");
obj.put("dataUltimaSincronizzazione", "20240101000000");

JSONArray images = new JSONArray();
JSONObject img = new JSONObject();
img.put("fileName", "immagini/icons/elemento_123.png");

File imageFile = new File(dirElementi, "elemento_123.png");
byte[] imageBytes = Utility.getBytes(imageFile);
String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
img.put("fileContent", base64Image);

images.put(img);
obj.put("images", images);

// Invio POST
String url = "http://147.93.127.131:5151/sincronizzatore/uploadFile";
HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
conn.setRequestMethod("POST");
conn.setRequestProperty("Content-Type", "application/json");
conn.setDoOutput(true);

DataOutputStream out = new DataOutputStream(conn.getOutputStream());
out.writeBytes(obj.toString());
out.flush();
out.close();

// Lettura risposta
BufferedReader br = new BufferedReader(
    new InputStreamReader(conn.getInputStream(), "utf-8"));
String response = br.readLine();
```

### Esempio 3: Download Immagine

```java
String icona = "elemento_123.png";
String urlIcona = "http://147.93.127.131:80/images/icons/" + icona;

URL url = new URL(urlIcona);
InputStream in = url.openStream();
FileOutputStream fos = new FileOutputStream(new File(dirElementi, icona));

byte[] buffer = new byte[1024];
int len;
while ((len = in.read(buffer)) > 0) {
    fos.write(buffer, 0, len);
}

fos.close();
in.close();
```

### Esempio 4: Verifica Licenza

```java
String codice = sharedPrefs.getString(Sessione.CODICE_ATTIVAZIONE, "");
String codiceEncrypted = URLEncoder.encode(Licenza.encrypt(codice), "UTF-8");

String url = "http://147.93.127.131:5151/license/attivazioneServer" +
             "?action=C&ID=" + codiceEncrypted;

String result = Utility.getStringaDaPaginaWeb(url);

if (result != null && !result.equals("NON_ATTIVA")) {
    // Licenza valida
} else {
    // Licenza non valida
}
```

---

## Riferimenti Codice

### Classi Principali API

| Classe | Percorso | Descrizione |
|--------|----------|-------------|
| `Sincronizzatore` | `server/Sincronizzatore.java` | Sincronizzazione bidirezionale dati |
| `DownloadUtil` | `utils/DownloadUtil.java` | Download database iniziale e utilità |
| `Licenza` | `utils/Licenza.java` | Gestione licenze e verifica abbonamenti |
| `Utility` | `utils/Utility.java` | Metodi utility (compress, web fetch, etc.) |
| `Globals` | `Globals.java` | Costanti configurazione globali |

### Metodi Chiave

| Metodo | Classe | Riga | Descrizione |
|--------|--------|------|-------------|
| `sincronizza()` | Sincronizzatore | 103 | Main sync logic |
| `inviaRichiesta()` | DownloadUtil | 335 | HTTP request wrapper |
| `sincronizzaImmagini()` | Sincronizzatore | 596 | Image sync |
| `controllaAttivazione()` | Sincronizzatore | 348 | License check |
| `comprimi()` | Utility | - | GZIP compression |
| `decomprimi()` | Utility | - | GZIP decompression |

---

## Changelog API

### Version 2.10 (108)
- Implementato POST con JSON invece di SOAP
- Aggiunto supporto compressione GZIP
- Ottimizzato sync immagini (solo modificate)
- Aggiunto tracciamento `icone_modificate`

### Version Precedenti
- SOAP Web Service (deprecato)
- Download database da internet (ora da asset)
- Sync completa ogni volta (ora incrementale)

---

## Note Tecniche

### Limitazioni
- **Max Record per Tabella**: 16384 (continuaTabella per tabelle più grandi)
- **Timeout**: 60 secondi per chiamata
- **Max Image Size**: Nessun limite esplicito (ma compressione consigliata)

### Performance
- Compressione GZIP: ~90% riduzione dimensione
- Transazioni DB: Batch insert per performance
- Progress tracking per UX migliore

### Compatibilità
- **Min API**: 26 (Android 8.0)
- **Target API**: 33 (Android 13)
- **HTTP Client**: HttpURLConnection (standard Android)

---

*Documentazione API generata il: 21 Febbraio 2026*
*Versione App Documentata: 2.10 (108)*
*Base URL: http://147.93.127.131:5151*
