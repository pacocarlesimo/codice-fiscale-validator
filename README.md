# CodiceFiscaleValidator

**CodiceFiscaleValidator** è una libreria Java che permette la validazione completa del Codice Fiscale italiano. Oltre al semplice controllo del formato, verifica la coerenza del comune di nascita tramite DB LightSQL integrato e il carattere di controllo, garantendo un'accuratezza superiore rispetto ai controlli formali.

## Caratteristiche principali
- **Validazione completa**: controlla formato, omocodia, carattere di controllo e comune di nascita.
- **Database integrato**: include tutti i codici catastali ufficiali dei comuni italiani e stati esteri.
- **Generazione del codice fiscale**: genera un codice fiscale partendo dai dati anagrafici.
- **Gestione omocodia**: normalizza e riconosce codici fiscali omocodici.

---

## 📦 Installazione

Aggiungi la dipendenza Maven al tuo progetto:

```xml
<dependency>
    <groupId>io.github.pacocarlesimo</groupId>
    <artifactId>codice-fiscale-validator</artifactId>
    <version>1.0.0</version>
</dependency>
```

Se usi Gradle:

```gradle
dependencies {
    implementation 'io.github.pacocarlesimo:codice-fiscale-validator:1.0.0'
}
```

---

## 🔍 Utilizzo

### 1️⃣ Validazione del Codice Fiscale

```java
import it.codicefiscale.CodiceFiscaleValidator;
import it.codicefiscale.CodiceFiscaleValidator.Risultato;

CodiceFiscaleValidator validator = new CodiceFiscaleValidator();
Risultato risultato = validator.validaFormato("RSSMRA85M01H501Z");

if (risultato.isValido()) {
    System.out.println("Codice Fiscale valido!");
} else {
    System.out.println("Errore: " + risultato.getMessaggio());
}
```

### 2️⃣ Validazione completa con dati anagrafici

```java
Risultato risultatoCompleto = validator.valida(
    "RSSMRA85M01H501Z", // Codice fiscale
    "Mario",             // Nome
    "Rossi",             // Cognome
    LocalDate.of(1985, 8, 1), // Data di nascita
    'M',                 // Sesso
    "Roma",             // Comune di nascita
    "RM"                // Sigla provincia
);

System.out.println(risultatoCompleto.getMessaggio());
```

### 3️⃣ Generazione di un Codice Fiscale

```java
String codiceFiscaleGenerato = validator.generaCodiceFiscale(
    "Mario", "Rossi", LocalDate.of(1985, 8, 1), 'M', "Roma", "RM"
);
System.out.println("Codice Fiscale generato: " + codiceFiscaleGenerato);
```

---

## 🛠️ Contributi & Supporto

Hai trovato un bug o vuoi suggerire una feature? Apri un'**issue** su GitHub!

---

## 📜 Licenza

Questo progetto è rilasciato sotto licenza MIT. Sentiti libero di utilizzarlo e migliorarlo! 😊

