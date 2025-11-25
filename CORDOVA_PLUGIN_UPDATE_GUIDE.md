# 🔧 Guia de Atualização do Plugin Cordova - Fatiamento Automático Gertec

## 📋 Resumo das Mudanças

Este guia descreve as alterações necessárias no plugin Cordova `nxtqs-thermal-printer-cordova-plugin` para habilitar o fatiamento automático de imagens/QR codes em impressoras Gertec usando a biblioteca versão **7.0.2**.

---

## 🎯 Objetivo

Permitir que o app Angular/Ionic passe o parâmetro `printerModel: 'gertec'` para o plugin, que então habilitará o fatiamento automático nativo de imagens, resolvendo o problema de travamento da impressora Gertec ao imprimir logos e QR codes grandes.

---

## 📦 Pré-requisitos

- ✅ Biblioteca `nextqs-ESCPOS-ThermalPrinter-Android` versão **7.0.2** ou superior
- ✅ Plugin Cordova `nxtqs-thermal-printer-cordova-plugin` instalado
- ✅ Projeto Android com Gradle configurado

## 📢 Versão 7.0.2 - Correção Importante

A versão 7.0.2 corrige um problema crítico onde o fatiamento inseria quebras de linha entre os strips da imagem, causando **linhas brancas visíveis** na impressão. Agora as imagens permanecem **completamente contínuas** durante o fatiamento.

---

## 🔧 Alterações Necessárias

### 1️⃣ Atualizar `build.gradle` do Plugin (Android)

**Arquivo:** `nxtqs-thermal-printer-cordova-plugin/src/android/build.gradle` ou similar

**Localizar:**
```gradle
dependencies {
    implementation 'com.github.taylorlacort:nextqs-ESCPOS-ThermalPrinter-Android:6.x.x'
}
```

**Alterar para:**
```gradle
dependencies {
    implementation 'com.github.taylorlacort:nextqs-ESCPOS-ThermalPrinter-Android:7.0.2'
}
```

---

### 2️⃣ Atualizar Código Java do Plugin

**Arquivo:** `nxtqs-thermal-printer-cordova-plugin/src/android/ThermalPrinter.java` (ou nome similar)

#### 📍 Localizar o método `printFormattedTextAndCut`:

```java
@Override
public boolean execute(String action, JSONArray args, CallbackContext callbackContext) 
        throws JSONException {
    
    if (action.equals("printFormattedTextAndCut")) {
        JSONObject options = args.getJSONObject(0);
        
        String type = options.getString("type");
        String id = options.optString("id", "");
        String text = options.getString("text");
        double mmFeedPaper = options.optDouble("mmFeedPaper", 0);
        int printerWidthMM = options.optInt("printerWidthMM", 48);
        int printerNbrCharactersPerLine = options.optInt("printerNbrCharactersPerLine", 32);
        
        // ... código existente de charset encoding
        
        // Criar conexão e impressora
        DeviceConnection connection = getConnection(type, id);
        EscPosPrinter printer = new EscPosPrinter(
            connection, 
            203, 
            printerWidthMM, 
            printerNbrCharactersPerLine
        );
        
        // Imprimir
        printer.printFormattedTextAndCut(text, (float) mmFeedPaper);
        
        callbackContext.success("Impressão realizada com sucesso");
        return true;
    }
    
    return false;
}
```

#### ✅ Adicionar após a leitura dos parâmetros:

```java
@Override
public boolean execute(String action, JSONArray args, CallbackContext callbackContext) 
        throws JSONException {
    
    if (action.equals("printFormattedTextAndCut")) {
        JSONObject options = args.getJSONObject(0);
        
        String type = options.getString("type");
        String id = options.optString("id", "");
        String text = options.getString("text");
        double mmFeedPaper = options.optDouble("mmFeedPaper", 0);
        int printerWidthMM = options.optInt("printerWidthMM", 48);
        int printerNbrCharactersPerLine = options.optInt("printerNbrCharactersPerLine", 32);
        
        // ✅ NOVO: Ler parâmetro printerModel
        String printerModel = options.optString("printerModel", "");
        
        // ... código existente de charset encoding
        
        // Criar conexão e impressora
        DeviceConnection connection = getConnection(type, id);
        EscPosPrinter printer = new EscPosPrinter(
            connection, 
            203, 
            printerWidthMM, 
            printerNbrCharactersPerLine
        );
        
        // ✅ NOVO: Habilitar fatiamento automático para Gertec
        if ("gertec".equalsIgnoreCase(printerModel)) {
            printer.setImageSlicing(true);
            printer.setImageSliceLinesPerStrip(20);
        }
        
        // Imprimir
        printer.printFormattedTextAndCut(text, (float) mmFeedPaper);
        
        callbackContext.success("Impressão realizada com sucesso");
        return true;
    }
    
    return false;
}
```

#### 📝 Diff Visual:

```diff
@Override
public boolean execute(String action, JSONArray args, CallbackContext callbackContext) 
        throws JSONException {
    
    if (action.equals("printFormattedTextAndCut")) {
        JSONObject options = args.getJSONObject(0);
        
        String type = options.getString("type");
        String id = options.optString("id", "");
        String text = options.getString("text");
        double mmFeedPaper = options.optDouble("mmFeedPaper", 0);
        int printerWidthMM = options.optInt("printerWidthMM", 48);
        int printerNbrCharactersPerLine = options.optInt("printerNbrCharactersPerLine", 32);
        
+       // Ler parâmetro printerModel (opcional)
+       String printerModel = options.optString("printerModel", "");
        
        // ... código de charset encoding
        
        DeviceConnection connection = getConnection(type, id);
        EscPosPrinter printer = new EscPosPrinter(
            connection, 
            203, 
            printerWidthMM, 
            printerNbrCharactersPerLine
        );
        
+       // Habilitar fatiamento para impressoras Gertec
+       if ("gertec".equalsIgnoreCase(printerModel)) {
+           printer.setImageSlicing(true);
+           printer.setImageSliceLinesPerStrip(20);
+       }
        
        printer.printFormattedTextAndCut(text, (float) mmFeedPaper);
        
        callbackContext.success("Impressão realizada com sucesso");
        return true;
    }
    
    return false;
}
```

---

### 3️⃣ Adicionar Imports (se necessário)

Verifique se os imports estão corretos no topo do arquivo Java:

```java
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.DeviceConnection;
// ... outros imports existentes
```

---

### 4️⃣ Atualizar Interface TypeScript (Opcional mas Recomendado)

**Arquivo:** `nxtqs-thermal-printer-cordova-plugin/src/index.d.ts` (ou similar)

#### 📍 Localizar a interface:

```typescript
export interface PrintFormattedTextOptions {
  type: 'bluetooth' | 'tcp' | 'usb';
  id: string | number;
  text: string;
  mmFeedPaper?: number;
  printerWidthMM?: number;
  printerNbrCharactersPerLine?: number;
  charsetEncoding?: {
    charsetName: string;
    charsetId: number;
  };
}
```

#### ✅ Adicionar parâmetro `printerModel`:

```typescript
export interface PrintFormattedTextOptions {
  type: 'bluetooth' | 'tcp' | 'usb';
  id: string | number;
  text: string;
  mmFeedPaper?: number;
  printerWidthMM?: number;
  printerNbrCharactersPerLine?: number;
  printerModel?: string; // ✅ NOVO: 'gertec' para habilitar fatiamento automático
  charsetEncoding?: {
    charsetName: string;
    charsetId: number;
  };
}
```

#### 📝 Diff Visual:

```diff
export interface PrintFormattedTextOptions {
  type: 'bluetooth' | 'tcp' | 'usb';
  id: string | number;
  text: string;
  mmFeedPaper?: number;
  printerWidthMM?: number;
  printerNbrCharactersPerLine?: number;
+ printerModel?: string; // 'gertec' para habilitar fatiamento automático
  charsetEncoding?: {
    charsetName: string;
    charsetId: number;
  };
}
```

---

## 🧪 Testando as Mudanças

### Teste 1: Verificar se plugin compila

```bash
cd nxtqs-thermal-printer-cordova-plugin
cordova build android
```

**Resultado esperado:** Build sem erros

---

### Teste 2: Adicionar logs de debug (opcional)

Adicione logs para verificar se o parâmetro está sendo recebido:

```java
String printerModel = options.optString("printerModel", "");
Log.d("ThermalPrinter", "Received printerModel: '" + printerModel + "'");

if ("gertec".equalsIgnoreCase(printerModel)) {
    Log.d("ThermalPrinter", "✅ Enabling image slicing for Gertec");
    printer.setImageSlicing(true);
    printer.setImageSliceLinesPerStrip(20);
} else {
    Log.d("ThermalPrinter", "ℹ️ Image slicing disabled (printerModel: '" + printerModel + "')");
}
```

**Verificar no logcat:**
```bash
adb logcat | grep ThermalPrinter
```

**Resultado esperado para Gertec:**
```
D/ThermalPrinter: Received printerModel: 'gertec'
D/ThermalPrinter: ✅ Enabling image slicing for Gertec
```

**Resultado esperado para outras impressoras:**
```
D/ThermalPrinter: Received printerModel: ''
D/ThermalPrinter: ℹ️ Image slicing disabled (printerModel: '')
```

---

### Teste 3: Teste funcional na Gertec

**Cenário:** Imprimir ticket com logo 200x200px + QR code size='40'

**Antes da atualização:**
- ❌ Logo imprime
- ❌ QR code imprime
- ❌ **Impressora trava**
- ❌ Texto após QR não aparece

**Depois da atualização (com `printerModel: 'gertec'`):**
- ✅ Logo imprime corretamente
- ✅ QR code imprime corretamente
- ✅ **Impressora NÃO trava**
- ✅ Texto após QR aparece normalmente
- ⏱️ Impressão pode ser ligeiramente mais lenta (devido aos strips)

---

### Teste 4: Teste com impressora não-Gertec (MTP-II)

**Cenário:** Imprimir mesmo ticket na MTP-II

**Resultado esperado:**
- ✅ Logo imprime rapidamente (sem fatiamento)
- ✅ QR code imprime rapidamente (sem fatiamento)
- ✅ Texto aparece normalmente
- ⚡ Velocidade de impressão normal (sem delays)

---

## 📊 Resumo das Alterações

| Arquivo | Alteração | Obrigatório? |
|---------|-----------|--------------|
| `build.gradle` | Atualizar versão para 7.0.2 | ✅ Sim |
| `ThermalPrinter.java` | Adicionar leitura de `printerModel` | ✅ Sim |
| `ThermalPrinter.java` | Adicionar `if ("gertec")` + `setImageSlicing` | ✅ Sim |
| `index.d.ts` | Adicionar `printerModel?: string` | ⚠️ Recomendado |
| Logs de debug | Adicionar `Log.d()` | ℹ️ Opcional |

---

## 🔍 Verificação Final

Após implementar as mudanças, verifique:

- [ ] `build.gradle` tem versão 7.0.2
- [ ] Código Java lê `printerModel` do JSONObject
- [ ] Código Java verifica `if ("gertec".equalsIgnoreCase(printerModel))`
- [ ] Código Java chama `setImageSlicing(true)` para Gertec
- [ ] Interface TypeScript atualizada (opcional)
- [ ] Build do plugin compila sem erros
- [ ] Teste na Gertec: logo + QR imprimem sem travar
- [ ] Teste em outra impressora: funciona normalmente

---

## 🚀 Código Completo de Referência

### Java (ThermalPrinter.java)

```java
@Override
public boolean execute(String action, JSONArray args, CallbackContext callbackContext) 
        throws JSONException {
    
    if (action.equals("printFormattedTextAndCut")) {
        try {
            JSONObject options = args.getJSONObject(0);
            
            // Parâmetros obrigatórios
            String type = options.getString("type");
            String id = options.optString("id", "");
            String text = options.getString("text");
            
            // Parâmetros opcionais
            double mmFeedPaper = options.optDouble("mmFeedPaper", 0);
            int printerWidthMM = options.optInt("printerWidthMM", 48);
            int printerNbrCharactersPerLine = options.optInt("printerNbrCharactersPerLine", 32);
            String printerModel = options.optString("printerModel", "");
            
            // Charset encoding
            EscPosCharsetEncoding encoding = null;
            if (options.has("charsetEncoding")) {
                JSONObject charsetObj = options.getJSONObject("charsetEncoding");
                String charsetName = charsetObj.getString("charsetName");
                int charsetId = charsetObj.getInt("charsetId");
                encoding = new EscPosCharsetEncoding(charsetName, charsetId);
            }
            
            // Criar conexão
            DeviceConnection connection = getConnection(type, id);
            
            // Criar impressora
            EscPosPrinter printer;
            if (encoding != null) {
                printer = new EscPosPrinter(
                    connection, 
                    203, 
                    printerWidthMM, 
                    printerNbrCharactersPerLine,
                    encoding
                );
            } else {
                printer = new EscPosPrinter(
                    connection, 
                    203, 
                    printerWidthMM, 
                    printerNbrCharactersPerLine
                );
            }
            
            // Habilitar fatiamento para Gertec
            if ("gertec".equalsIgnoreCase(printerModel)) {
                printer.setImageSlicing(true);
                printer.setImageSliceLinesPerStrip(20);
            }
            
            // Imprimir
            printer.printFormattedTextAndCut(text, (float) mmFeedPaper);
            
            callbackContext.success("Impressão realizada com sucesso");
            return true;
            
        } catch (Exception e) {
            callbackContext.error("Erro ao imprimir: " + e.getMessage());
            return false;
        }
    }
    
    return false;
}
```

### TypeScript (index.d.ts)

```typescript
export interface PrintFormattedTextOptions {
  /** Tipo de conexão: bluetooth, tcp ou usb */
  type: 'bluetooth' | 'tcp' | 'usb';
  
  /** ID da impressora (MAC address para Bluetooth, IP para TCP, deviceId para USB) */
  id: string | number;
  
  /** Texto formatado para impressão (suporta tags ESC/POS) */
  text: string;
  
  /** Distância de feed do papel em milímetros (padrão: 0) */
  mmFeedPaper?: number;
  
  /** Largura da impressora em milímetros (padrão: 48) */
  printerWidthMM?: number;
  
  /** Número de caracteres por linha (padrão: 32) */
  printerNbrCharactersPerLine?: number;
  
  /** 
   * Modelo da impressora. Use 'gertec' para habilitar fatiamento automático
   * de imagens/QR codes, prevenindo travamentos em impressoras Gertec.
   * Para outras impressoras, deixe vazio ou omita.
   */
  printerModel?: string;
  
  /** Configuração de charset encoding */
  charsetEncoding?: {
    charsetName: string;
    charsetId: number;
  };
}

export interface ThermalPrinterPlugin {
  printFormattedTextAndCut(
    options: PrintFormattedTextOptions,
    successCallback: () => void,
    errorCallback: (error: any) => void
  ): void;
  
  // ... outros métodos
}
```

---

## 📚 Documentação de Referência

- **Biblioteca nextqs-ESCPOS-ThermalPrinter-Android v7.0.2:**
  - GitHub: https://github.com/taylorlacort/nextqs-ESCPOS-ThermalPrinter-Android
  - Tag: https://github.com/taylorlacort/nextqs-ESCPOS-ThermalPrinter-Android/releases/tag/7.0.2
  - JitPack: https://jitpack.io/#taylorlacort/nextqs-ESCPOS-ThermalPrinter-Android/7.0.2

- **Guias da Biblioteca:**
  - `README.md`: Documentação geral e API
  - `GERTEC_GUIDE_PT-BR.md`: Guia completo em Português sobre o problema Gertec
  - `MIGRATION_GUIDE_APP.md`: Guia de migração para apps
  - `USAGE_EXAMPLE.md`: Exemplos práticos de uso

---

## ❓ FAQ

### 1. O que acontece se não passar `printerModel`?

O parâmetro é opcional. Se não for passado ou for string vazia, a impressora funcionará no modo padrão (sem fatiamento), igual ao comportamento anterior.

### 2. Posso usar fatiamento em impressoras não-Gertec?

Sim, mas **não é recomendado**. O fatiamento adiciona delays entre strips, deixando a impressão mais lenta. Use apenas para impressoras que realmente precisam (Gertec).

### 3. Quantas linhas por strip devo usar?

O padrão é **20 linhas**, que funciona bem para Gertec. Valores menores (10-15) são mais seguros mas mais lentos. Valores maiores (30-50) são mais rápidos mas podem não resolver o problema.

### 4. Como debugar se o fatiamento está ativo?

Adicione logs no código Java conforme mostrado na seção "Teste 2" e monitore o logcat do Android.

### 5. Preciso atualizar o app Angular?

Se você já tem o código TypeScript correto (passando `printerModel`), não precisa alterar nada. Basta atualizar o plugin Cordova e recompilar o app.

---

## 🆘 Suporte

Se encontrar problemas durante a implementação:

1. **Verifique a versão da biblioteca** no `build.gradle` (deve ser 7.0.1)
2. **Verifique os logs** do Android Studio/logcat
3. **Teste com hardcode** primeiro: `printer.setImageSlicing(true);` direto no Java
4. **Consulte os guias** da biblioteca (GERTEC_GUIDE_PT-BR.md, etc.)

---

**Data de criação:** 25/11/2025  
**Última atualização:** 25/11/2025  
**Versão da biblioteca:** 7.0.2  
**Compatibilidade:** Cordova 9+, Android 4.1+
