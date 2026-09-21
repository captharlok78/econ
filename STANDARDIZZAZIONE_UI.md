# Standardizzazione UI dei moduli EConTab

Log di lavoro e stato architetturale dell'iniziativa "elimina la duplicazione grafica/di
codice tra i moduli, estraendo dei padri condivisi". Aggiornato al 2026-09-18. Leggere
questo file prima di continuare il lavoro in una nuova sessione.

## Obiettivo generale

I moduli dell'app (Clienti, Cantieri, Preventivi, Ordini, Rapportini, Listini) erano stati
scritti ciascuno con la propria grafica/logica duplicata per: liste con ricerca/paginazione,
pagine di modifica/inserimento, pagine di sola visualizzazione. L'obiettivo, richiesto
esplicitamente dall'utente, è avere dei "padri" condivisi (Java + layout `<include>`) così
che una modifica al padre si propaghi automaticamente a tutti i moduli senza dover
ricopiare la modifica ovunque, evitando le inconsistenze già capitate (es. il pulsante
"Ultimi N" dimenticato in 4 moduli su 6 durante la prima migrazione).

Ci sono **due iniziative distinte**, entrambe nello stesso spirito:

1. **Liste/ricerca** — COMPLETATA su tutti i 6 moduli.
2. **Dettaglio (modifica/inserimento e visualizzazione)** — PROTOTIPO completato solo su
   Clienti, da estendere agli altri moduli. **Punto in cui riprendere il lavoro.**

---

## 1. Liste/ricerca — COMPLETATA

Architettura in `app/src/main/java/pfa/app/econtab/lista/`:

- `EConTabListaStandardController` — stato e logica condivisi (paginazione, ordinamento,
  ricerca, "ultimi N"), comunica con l'Activity/Fragment ospitante tramite l'interfaccia
  `Host` (così la stessa logica funziona sia per Activity a schermo intero sia per Fragment
  incorporati, es. Cantieri dentro `ClientiDettaglioActivity`).
- `EConTabListaStandardDefinition` — classe astratta, una per modulo: query (`costruisciQuery`),
  colonne, azione click riga, titolo, `getOrdineRecenti()` (ordine per "Ultimi N": `null` =
  pulsante nascosto).
- `EConTabListaStandardActivity` / `EConTabListaStandardFragment` — host sottili che
  delegano al controller.
- `EConTabListaStandardAdapter`, `ColonnaLista`, `QueryPagina`, `FiltriHelper`.
- `DbInterno.eseguiSelectPaginato(...)` — helper di paginazione SQL condiviso (nuovo).

Layout condivisi via `<include>` (uno per tutti i moduli, MAI ricopiati):

- `header_lista_standard.xml` — barra blu `indietro | titolo | filtro | nuovo`.
- `filtri_azioni_standard.xml` — riga pulsanti `Cerca | Reset filtri | Ultimi N`. "Ultimi N"
  si mostra/nasconde SOLO tramite `getOrdineRecenti()` del modulo, mai toccando questo XML.

Moduli migrati e verificati dal vivo su tablet: Clienti, Cantieri, Preventivi, Ordini
(eredita da `PreventiviListaFragment`), Rapportini, Listini (CodiciArticoli +
FornitoriLinee, quest'ultimo resta su `ExpandableListView` proprio ma con paginazione e
pulsanti condivisi).

Commit principali (branch `master`): `2ef47a8` (migrazione base 6 moduli), `8cd49d7`
(testata unificata), `ee74166` (estrazione `filtri_azioni_standard.xml` + `getOrdineRecenti()`
sui moduli che lo avevano dimenticato).

**Questa parte è considerata conclusa.** Eventuali richieste future qui sono manutenzione,
non nuova migrazione.

---

## 2. Dettaglio (modifica/inserimento + visualizzazione) — IN CORSO

### Cosa c'era prima

Le pagine di modifica/inserimento (le `*Mod*Activity`, 19+ moduli) erano tutte costruite
sullo stesso schema (`EConTabDettaglioActivity` + layout con `ScrollView` + `footer_dett.xml`
in fondo con Annulla/Salva) ma:
- nessun header/titolo visibile (action bar nascosta ovunque nell'app);
- `EditText` senza bordo, sfondo grigio uniforme, spaziatura minima (`@dimen/margine` = 5dp);
- pulsanti Annulla/Salva identici (entrambi blu piatto), nessuna gerarchia visiva.

La pagina di **sola visualizzazione** di Clienti (`ClientiDettaglioActivity`, tab
"ANAGRAFICA / CANTIERI") non aveva **nessun pulsante indietro visibile** (solo tasto/gesture
hardware) — unico modulo con questo pattern a tab, nessun altro modulo lo replica.

### Cosa è stato fatto (prototipo su Clienti)

Nuovi elementi condivisi, pronti per essere adottati da altri moduli senza altre modifiche
al file stesso:

- **`header_dettaglio_standard.xml`** (nuovo) — stessa barra blu `#1565C0` degli altri
  header, ma solo `indietro | titolo` (niente filtro/nuovo, non serve in un dettaglio). Il
  pulsante indietro usa `onClick="annulla"`.
- **`EConTabActivity.annulla(View)`** (nuovo, alias di `indietro()`) — permette al pulsante
  indietro dell'header di funzionare su QUALSIASI Activity dell'app: nelle pagine di
  modifica (`EConTabDettaglioActivity`) è già sovrascritto per impostare
  `RESULT_CANCELED`; nelle pagine di sola visualizzazione (che non estendono
  `EConTabDettaglioActivity`) usa semplicemente `finish()`. Questo è ciò che rende l'header
  riusabile in entrambi i contesti senza duplicare il layout.
- **`EConTabDettaglioActivity.getTitoloDettaglio()`** (nuovo hook, default `null`) — il
  modulo lo sovrascrive per impostare il titolo (es. `"Nuovo cliente"` / `"Modifica
  cliente"` in base a `getModalita()`). Se `null` o se il layout non include
  `headerTitolo`, non succede nulla: **l'adozione è incrementale, un modulo alla volta,
  senza rischio per gli altri**.
- **`footer_dett.xml`** (ristilizzato, condiviso da TUTTI i moduli di dettaglio) — pulsante
  "Annulla" ora secondario (grigio `#E0E0E0`, testo blu), "Salva" primario (blu `#1565C0`,
  testo bianco), stessa gerarchia visiva di "Reset filtri"/"Cerca" nelle liste. **Essendo
  condiviso, questo miglioramento è già visibile in tutti i 19+ moduli di dettaglio senza
  bisogno di toccarli uno per uno.**
- **`EConTabDettaglioActivity`** — sfondo dello `scroll` cambiato da `#eeeded` a `#F5F5F5`
  (uniforme con le liste), sempre condiviso da tutti i moduli.

Applicato per intero (header + card + input bordati + titolo dinamico) solo su:

- `activity_clienti_dettaglio_mod.xml` + `ClientiDettaglioModActivity` (modifica/inserimento)
- `activity_clienti_dettaglio.xml` + `fragment_clienti_dettaglio.xml` +
  `ClientiDettaglioFragment` (sola visualizzazione, tab Anagrafica/Cantieri — **unico
  modulo con questo pattern a tab**, nessun altro lo replica)

Stile dei form (card + input), stessa identica ricetta usata nelle liste:
- contenitore campi: `android:background="@drawable/bg_card_filtri"` +
  `layout_margin="8dp"` + `padding="10dp"` (invece di `@dimen/margine`=5dp piatto)
- ogni `EditText`: `android:background="@drawable/bg_input_outline"` +
  `minHeight="40dp"` + `paddingStart/End="8dp"`
- spaziatura tra campi: `10dp` fisso (invece di `@dimen/margine`)
- etichette: stile `TestoIntestazione` esistente, non cambiato (già coerente in tutta l'app)

Verificato dal vivo su tablet (serial `R92X3031PNW`): apertura Nuovo/Modifica cliente,
titolo dinamico corretto, Annulla, validazione IVA non toccata, nessuna regressione sugli
altri moduli (Cantieri controllato: sfondo e pulsanti footer aggiornati correttamente senza
header, comportamento invariato). Vista a tab: titolo dinamico (ragione sociale), pulsante
indietro funzionante (prima assente), tab Cantieri imbottita corretta.

Commit: `7ca13ce` (modifica/inserimento), `a8b3857` (visualizzazione).

### Cosa NON è stato fatto (prossimi passi)

**Serve estendere lo stesso trattamento agli altri moduli**, uno alla volta, con verifica
dal vivo e commit/push ad ogni passo — stessa disciplina usata per la migrazione delle
liste. Elenco dei layout `*_dettaglio_mod.xml`/equivalenti ancora da fare (tutti già
beneficiano automaticamente del nuovo `footer_dett.xml` e dello sfondo `#F5F5F5`, manca solo
header + card + input style):

```
activity_area_dettaglio_mod.xml
activity_azienda_dettaglio.xml
activity_cantieri_dettaglio_mod.xml
activity_codice_articolo_dettaglio_mod.xml
activity_collegamento.xml
activity_componente_mod.xml
activity_configurazione_generale.xml
activity_costruttori_dettaglio.xml
activity_elemento_mod.xml
activity_iva_dettaglio.xml
activity_linee_dettaglio_mod.xml
activity_locale_dettaglio_mod.xml
activity_manodopera_dettaglio.xml
activity_piantina_locale_mod.xml
activity_placche_dettaglio_mod.xml
activity_preventivi_dettaglio_mod.xml
activity_rapportino_dettaglio_mod.xml
activity_relazione.xml
activity_unita_dettaglio_mod.xml
activity_unita_misura_dettaglio.xml
```

Per ciascuno, la ricetta (già rodata su Clienti) è:

1. Nel layout: aggiungere `<include layout="@layout/header_dettaglio_standard" />` sopra lo
   `ScrollView` (allineato in cima), e mettere lo `ScrollView` in `layout_below="@+id/header"`
   invece di `layout_alignParentTop`.
2. Avvolgere il contenitore campi in `bg_card_filtri` (margin 8dp, padding 10dp) e ogni
   `EditText`/`Spinner` in `bg_input_outline` (minHeight 40dp, padding 8dp), spaziatura 10dp.
3. Nella Activity Java corrispondente: `@Override protected String getTitoloDettaglio() {
   return getModalita() == INSERIMENTO ? "Nuovo X" : "Modifica X"; }`.
4. Build (`./gradlew installDebug --offline`), verifica dal vivo su tablet, commit+push.

**Nessun modulo oltre Clienti ha una vista di sola visualizzazione a tab** (verificato con
`grep -rl "extends EConTabFragmentActivity"` → solo `ClientiDettaglioActivity`), quindi il
punto 2 dell'elenco iniziale ("visualizzazione") riguarda SOLO Clienti ed è già concluso.
Gli altri moduli vanno dalla lista diretti al Mod (modifica), quindi per loro serve solo il
trattamento "modifica/inserimento" sopra descritto.

### Domanda architetturale aperta (da decidere, non ancora presa)

Per le liste è stato creato un vero "padre" Java (`EConTabListaStandardController` +
`Definition`) perché la logica (query, paginazione, ordinamento) era genuinamente comune.
Per il dettaglio, invece, i form sono molto eterogenei (join, autocomplete, spinner,
logica di business per riga) — **il valore comune è quasi solo la GRAFICA** (header,
footer, stile input/card), non la logica. Gli hook attuali (`getTitoloDettaglio()` +
header/footer condivisi via `<include>`) potrebbero essere sufficienti, senza bisogno di
un controller Java dedicato come per le liste. Da confermare con l'utente prima di iniziare
la migrazione degli altri moduli, per non costruire un'astrazione più pesante del
necessario.

---

## 3. Pannello laterale (menu ad albero delle maschere con split) — PROTOTIPO su Cantiere/Preventivo

Il menu a sinistra di `CantiereSplitActivity` (Preventivo / Cantiere / Unita' / Area / Locale, usato
da cantieri, preventivi e ordini) era grigio scuro con testo chiaro e con logica grafica nel suo
adapter. Ora e' un componente standard:

- `pannello_menu_laterale.xml` — contenitore bianco + `ListView` (`@+id/lista`); si inserisce con
  `<include android:id="@+id/sinistra" layout="@layout/pannello_menu_laterale" .../>`.
- `list_item_menu_laterale.xml` — riga: icona, tipo (piccolo), nome; testi scuri su bianco.
- `bg_menu_laterale_sel.xml` — riga selezionata: sfondo azzurrino `#E3F2FD` (`#BBDEFB` da premuta).
- `MenuLateraleAdapter` (astratto, `adapters/`) — grafica, indentazione per livello e selezione
  (sfondo + grassetto, impostati in entrambi i sensi perche' le view sono riciclate). Un modulo lo
  estende implementando solo `getTipoTesto / getNomeTesto / getIconaRes / getLivello /
  getPosizioneSelezionata`.
- `CantiereMenuAdapter` ora estende `MenuLateraleAdapter` (solo mappatura dei dati).
- Icone: Font Awesome nel colore di default (`FaIcone`, `R.color.colore_icona` = `#757374`, lo stesso delle
  icone `ic_azione_*`), niente piu' PNG colorati per livello.

Verificato dal vivo su tablet: apertura preventivo, pannello bianco, cambio selezione. Rimosso il
vecchio `list_item_menucantiere.xml`. Nessun altro modulo usa oggi un pannello laterale
(`EConTabSplitPaneLayout` e' usato solo da `activity_cantiere_split.xml`): un futuro modulo con menu
laterale deve riusare questi file, non ricopiarli.

---

## 4. Dettaglio a tabella (righe di preventivo/ordine) — PROTOTIPO su Preventivi/Ordini

La lista righe del dettaglio preventivo (`PreventivoDettaglioFragment`) era una lista di card con
molti campi in ordine sparso. Ora e' una tabella standard, riusabile:

- `DettaglioTabellaAdapter` (astratto, `adapters/`) + `ColonnaDettaglio` — il modulo dichiara le colonne
  (`getColonne`: icona stretta / testo flessibile o fisso / custom) e il contenuto delle celle
  (`bindCella`); riga di gruppo opzionale (`isRigaGruppo/bindGruppo`, es. il locale). Testata e righe
  usano la stessa definizione, quindi restano allineate. `collegaTestata(contenitore)` costruisce la
  testata, FISSA (fuori dalla lista).
- Layout: `dettaglio_tabella_standard.xml` (testata `@+id/testata_dettaglio` + `@+id/lista_dett`, scorre in
  orizzontale se le colonne non stanno, es. con il menu laterale aperto), `list_item_riga_gruppo.xml`,
  `barra_totali_standard.xml` (totali/IVA STATICI in fondo, stessi id di prima), drawable
  `bg_cella_dettaglio` / `bg_cella_testata` (bordo grigetto solo a destra/sotto per non raddoppiare).
- `PreventiviDettaglioAdapter` estende il padre: colonne Tipo (icona con tooltip materiale / manodopera /
  collegamenti / placche / note) | Descrizione | Codice | Prz. acq. | Prz. lis. | Q.ta' (- valore +) |
  Prezzo | Importo | Rapportini (solo ordini) | Elimina (icona cestino, tooltip). Popup, opzioni riga,
  eliminazione con conferma: invariati (restano nell'adapter del modulo).
- `FaIcone` (`utils/`): Font Awesome 5.15 Free solid (SIL OFL) in `res/font/fa_solid_900.ttf`; per una
  nuova icona si aggiunge la costante del glifo.
- Rimossi i vecchi `list_item_preventivo_dettaglio.xml` e `list_item_locale_preventivo_dettaglio.xml`.

Verificato dal vivo su tablet: testata fissa, celle bordate, tooltip pressione lunga, totali statici,
menu opzioni riga dal tap sulla descrizione. NON ancora provati dal vivo: colonna Rapportini (ordini),
+/- quantita', popup codice/prezzo, elimina riga. Un futuro dettaglio a righe (es. rapportini) deve
estendere `DettaglioTabellaAdapter`, non ricopiare la grafica.

---

## File chiave (riferimento rapido)

**Liste** (`pfa.app.econtab.lista`): `EConTabListaStandardController.java`,
`EConTabListaStandardDefinition.java`, `EConTabListaStandardActivity.java`,
`EConTabListaStandardFragment.java`, `EConTabListaStandardAdapter.java`,
`ColonnaLista.java`, `QueryPagina.java`, `FiltriHelper.java`.
Layout: `header_lista_standard.xml`, `filtri_azioni_standard.xml`.

**Dettaglio**: `EConTabDettaglioActivity.java` (hook `getTitoloDettaglio()`),
`EConTabActivity.java` (metodo `annulla(View)`).
Layout: `header_dettaglio_standard.xml`, `footer_dett.xml`.
Prototipo completo: `ClientiDettaglioModActivity.java` +
`activity_clienti_dettaglio_mod.xml` (modifica/inserimento);
`ClientiDettaglioActivity.java` + `ClientiDettaglioFragment.java` +
`activity_clienti_dettaglio.xml` + `fragment_clienti_dettaglio.xml` (visualizzazione).

## Ambiente di test dal vivo

```bash
export PATH=$PATH:/home/acoppo/Android/Sdk/platform-tools   # adb non è nel PATH di default
cd /home/acoppo/wsncfphp2_0/econtab/RVAndroidApp
./gradlew installDebug --offline
adb devices   # tablet: R92X3031PNW
```

Ogni commit triggera l'auto-versioning via `.githooks/post-commit` (versione app corrente:
**1.0.21**). Commit solo dopo verifica dal vivo sul tablet, poi push.
