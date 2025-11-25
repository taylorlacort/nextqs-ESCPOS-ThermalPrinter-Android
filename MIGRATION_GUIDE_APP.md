# 🚀 Atualização Necessária - Plugin de Impressão Térmica

## 📋 Resumo

A biblioteca de impressão térmica foi atualizada para a **versão 7.0.0**, que inclui suporte nativo para impressoras Gertec. Esta atualização elimina a necessidade de fatiamento manual de imagens no código TypeScript.

## ⚠️ Problema Atual

O código TypeScript atual está fatiando manualmente os logos para impressoras Gertec usando a função `splitGsV0HexIntoStrips()`. Porém:

1. ❌ **QR codes não são fatiados** → impressora Gertec trava após imprimir QR codes
2. ❌ **Código duplicado** → lógica de fatiamento existe em TS e Java
3. ❌ **Layout poluído** → múltiplas tags `<img>` para um único logo
4. ❌ **Performance ruim** → processamento em JavaScript ao invés de nativo

## ✅ Solução

A versão 7.0.0 da biblioteca já possui **fatiamento automático nativo** que funciona para:
- ✅ Logos
- ✅ QR codes
- ✅ Qualquer imagem

## 🔧 O Que Precisa Ser Ajustado

### 1. Atualizar Plugin Cordova Java

**Arquivo:** `src/android/ThermalPrinter.java` (ou similar)

**Adicionar parâmetro `printerModel` ao método `printFormattedText`:**

```java
@Override
public boolean execute(String action, JSONArray args, CallbackContext callbackContext) 
        throws JSONException {
    
    if (action.equals("printFormattedText")) {
        String text = args.getString(0);
        String printerModel = args.optString(1, "");  // ← NOVO: parâmetro opcional
        
        try {
            // Criar impressora
            EscPosPrinter printer = new EscPosPrinter(
                this.getConnection(),  // seu método de obter conexão
                203, 
                48f, 
                32
            );
            
            // ← NOVO: Habilitar fatiamento se for Gertec
            if ("gertec".equalsIgnoreCase(printerModel)) {
                printer
                    .setImageSlicing(true)
                    .setImageSliceLinesPerStrip(20);
            }
            
            // Imprimir
            printer.printFormattedTextAndCut(text);
            
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

### 2. Atualizar Interface TypeScript do Plugin

**Arquivo:** `src/app/plugins/thermal-printer.d.ts` (ou similar)

```typescript
// ANTES:
interface ThermalPrinter {
    printFormattedText(text: string): Promise<void>;
}

// DEPOIS:
interface ThermalPrinter {
    printFormattedText(text: string, printerModel?: string): Promise<void>;
}
```

### 3. Simplificar Serviço Angular

**Arquivo:** `src/app/services/thermal-printer.service.ts`

#### 3.1. Manter método `isGertec()`

```typescript
isGertec(printer: any): boolean {
    // Detectar por MAC address
    if (printer.address?.startsWith('00:11:22:33:44:55')) {
        return true;
    }
    
    // Detectar por nome
    if (printer.name?.toLowerCase().includes('gertec')) {
        return true;
    }
    
    // Detectar por lista de IDs conhecidos
    const gertecIds = this.storage.get('gertec_printer_ids') || [];
    return gertecIds.includes(printer.id);
}
```

#### 3.2. **REMOVER** função `splitGsV0HexIntoStrips()`

```typescript
// ❌ REMOVER ESTA FUNÇÃO COMPLETA (não é mais necessária)
private splitGsV0HexIntoStrips(gsHex: string, stripLines: number = 20): string[] {
    // ... todo o código desta função pode ser deletado
}
```

#### 3.3. **SIMPLIFICAR** `buildTicketLayout()`

```typescript
// ❌ ANTES (com fatiamento manual):
buildTicketLayout(ticket: any, printer: any): string {
    let layout = '';
    
    const isGertec = this.isGertec(printer);
    
    // Logo
    if (this.printableLogo) {
        if (isGertec) {
            // Fatiamento manual
            const strips = this.splitGsV0HexIntoStrips(this.printableLogo, 20);
            for (const strip of strips) {
                layout += `<img>${strip}</img>`;
            }
        } else {
            layout += `<img>${this.printableLogo}</img>`;
        }
    }
    
    layout += `<qrcode size='40'>${ticket.id}</qrcode>`;
    
    return layout;
}

// ✅ DEPOIS (sem fatiamento manual):
buildTicketLayout(ticket: any): string {  // ← Remover parâmetro 'printer'
    let layout = '';
    
    // Logo (será fatiado automaticamente pelo Java se for Gertec)
    if (this.printableLogo) {
        layout += `<img>${this.printableLogo}</img>`;
    }
    
    // QR Code (será fatiado automaticamente pelo Java se for Gertec)
    layout += `<qrcode size='40'>${ticket.id}</qrcode>`;
    
    return layout;
}
```

#### 3.4. **ATUALIZAR** método de impressão

```typescript
// ❌ ANTES:
printByUsb(ticket: any, printer: any): Observable<any> {
    const layout = this.buildTicketLayout(ticket, printer);
    
    return from(
        (window as any).ThermalPrinter.printFormattedText(layout)
    );
}

// ✅ DEPOIS:
printByUsb(ticket: any, printer: any): Observable<any> {
    const layout = this.buildTicketLayout(ticket);  // ← Sem passar 'printer'
    const printerModel = this.isGertec(printer) ? 'gertec' : '';  // ← Detectar modelo
    
    return from(
        (window as any).ThermalPrinter.printFormattedText(
            layout,
            printerModel  // ← NOVO: passar modelo da impressora
        )
    );
}
```

## 📊 Comparação Antes vs Depois

### Layout Gerado

**❌ ANTES (múltiplas tags `<img>` para logo fatiado):**
```
<img>1d763000190000140000[dados tira 1]</img>
<img>1d763000190000140000[dados tira 2]</img>
<img>1d763000190000140000[dados tira 3]</img>
<img>1d763000190000140000[dados tira 4]</img>
<img>1d763000190000140000[dados tira 5]</img>
<qrcode size='40'>PEDIDO-123</qrcode>
```

**✅ DEPOIS (tag única, fatiamento automático no Java):**
```
<img>1d763000190000c80000[dados completos do logo]</img>
<qrcode size='40'>PEDIDO-123</qrcode>
```

### Processamento

| Aspecto | Antes (TypeScript) | Depois (Java 7.0.0) |
|---------|-------------------|---------------------|
| **Logo** | Fatiado em TS | Fatiado nativamente |
| **QR Code** | ❌ NÃO fatiado | ✅ Fatiado nativamente |
| **Performance** | Lenta (JS) | Rápida (nativo) |
| **Código TS** | ~100 linhas | ~10 linhas |
| **Manutenção** | Difícil (2 lugares) | Fácil (1 lugar) |

## ✅ Checklist de Implementação

- [ ] 1. Atualizar `build.gradle` para versão 7.0.0:
  ```gradle
  implementation 'com.github.taylorlacort:nextqs-ESCPOS-ThermalPrinter-Android:7.0.0'
  ```

- [ ] 2. Modificar plugin Java para aceitar parâmetro `printerModel`

- [ ] 3. Adicionar lógica `if ("gertec".equals(printerModel)) { setImageSlicing(true); }`

- [ ] 4. Atualizar interface TypeScript do plugin

- [ ] 5. Remover função `splitGsV0HexIntoStrips()` do serviço

- [ ] 6. Simplificar `buildTicketLayout()` (remover lógica de fatiamento manual)

- [ ] 7. Atualizar métodos de impressão para passar `printerModel`

- [ ] 8. Testar com impressora Gertec:
  - [ ] Logo 200x200px imprime corretamente
  - [ ] QR code size='40' imprime corretamente
  - [ ] Texto após imagem/QR é impresso (não trava mais)

- [ ] 9. Testar com outras impressoras (garantir que não quebraram)

- [ ] 10. Remover código comentado/obsoleto

## 🧪 Como Testar

### Teste 1: Gertec com Logo + QR

```typescript
const ticket = {
    id: 'PEDIDO-12345',
    customer: 'João Silva',
    total: 150.00
};

const gertecPrinter = {
    name: 'GERTEC PRINTER',
    address: '00:11:22:33:44:55'
};

// Deve imprimir: logo + QR code + texto sem travar
await this.printerService.printByUsb(ticket, gertecPrinter);
```

**Resultado esperado:**
- ✅ Logo impresso corretamente
- ✅ QR code impresso corretamente
- ✅ Texto "João Silva" e "R$ 150,00" impresso após o QR
- ✅ Impressora NÃO trava

### Teste 2: Outra Impressora (não-Gertec)

```typescript
const otherPrinter = {
    name: 'EPSON TM-T20',
    address: 'AA:BB:CC:DD:EE:FF'
};

// Deve imprimir normalmente (sem fatiamento)
await this.printerService.printByUsb(ticket, otherPrinter);
```

**Resultado esperado:**
- ✅ Imprime normalmente
- ✅ Velocidade de impressão não afetada

## 📚 Documentação de Referência

A biblioteca 7.0.0 já inclui documentação completa:

- **README.md** → Seção "Gertec Printer Compatibility"
- **GERTEC_GUIDE_PT-BR.md** → Guia completo em Português
- **USAGE_EXAMPLE.md** → Exemplos de código Java e TypeScript

## ❓ Dúvidas Frequentes

### 1. Por que remover o fatiamento do TypeScript?

O fatiamento nativo (Java) é:
- ✅ Mais rápido (processamento nativo)
- ✅ Funciona para logos E QR codes
- ✅ Transparente (não polui o layout)
- ✅ Mais fácil de manter (um único lugar)

### 2. O que acontece se não passar `printerModel`?

Se o segundo parâmetro for omitido ou vazio, a impressora funciona no modo padrão (sem fatiamento), igual ao comportamento atual para impressoras não-Gertec.

### 3. Vai quebrar algo?

Não! As mudanças são **retrocompatíveis**:
- Impressoras não-Gertec continuam funcionando igual
- O parâmetro `printerModel` é **opcional**
- Se não passar, comportamento é o mesmo de antes

### 4. Preciso atualizar o Cordova plugin agora?

Não é obrigatório imediatamente, mas **altamente recomendado** porque:
- ✅ Corrige o problema de QR codes travando Gertec
- ✅ Simplifica muito o código
- ✅ Melhora performance
- ✅ Facilita manutenção futura

## 🆘 Suporte

Se tiver dúvidas durante a implementação:

1. Consulte `GERTEC_GUIDE_PT-BR.md` (guia completo em Português)
2. Consulte `USAGE_EXAMPLE.md` (exemplos práticos)
3. Veja o código atual da biblioteca em `EscPosPrinterCommands.java` (linhas 594-625)

## 📝 Resumo das Mudanças

**Arquivos a modificar:**
1. ✏️ `ThermalPrinter.java` (plugin) - adicionar parâmetro `printerModel`
2. ✏️ `thermal-printer.d.ts` - atualizar interface
3. ✏️ `thermal-printer.service.ts`:
   - ❌ Remover `splitGsV0HexIntoStrips()`
   - ✏️ Simplificar `buildTicketLayout()`
   - ✏️ Atualizar `printByUsb()` para passar `printerModel`
4. 📦 `build.gradle` - atualizar para versão 7.0.0

**Linhas de código:**
- ❌ Remover: ~100 linhas (função de fatiamento TS)
- ✏️ Modificar: ~20 linhas (plugin Java + serviço TS)
- ➕ Adicionar: ~10 linhas (detecção e passagem de modelo)

**Total:** Redução de ~70 linhas de código complexo! 🎉

---

**Data de criação:** 25/11/2025  
**Versão da biblioteca:** 7.0.0  
**Compatibilidade:** Todas as impressoras ESC/POS (especialmente Gertec)
