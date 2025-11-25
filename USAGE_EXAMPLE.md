# Exemplo de Uso - Fatiamento Automático para Gertec

## ✅ CONFIRMADO: O fatiamento funciona automaticamente para:

1. **QR Codes** (tag `<qrcode>`)
2. **Logos** (tag `<img>` com hex)
3. **Qualquer imagem** convertida via `bitmapToHexadecimalString`

## Como Funciona o Fluxo

### 1. QR Code (`<qrcode>`)

```java
// Usuário chama:
printer.printFormattedText("[C]<qrcode size='40'>PEDIDO-123</qrcode>\n");

// Internamente:
PrinterTextParserQRCode.initConstructor()
  → EscPosPrinterCommands.QRCodeDataToBytes()  // Gera byte[] da imagem
  → PrinterTextParserImg.print()
    → printerSocket.printImage(this.image)     // ✅ Aplica fatiamento se habilitado!
```

### 2. Logo/Imagem (`<img>`)

```java
// Usuário chama:
String logoHex = PrinterTextParserImg.bitmapToHexadecimalString(printer, logoBitmap);
printer.printFormattedText("[C]<img>" + logoHex + "</img>\n");

// Internamente:
PrinterTextParserImg constructor recebe hex
  → Converte hex para byte[]
  → PrinterTextParserImg.print()
    → printerSocket.printImage(this.image)     // ✅ Aplica fatiamento se habilitado!
```

### 3. Método `printImage()` (Core)

```java
public EscPosPrinterCommands printImage(byte[] image) {
    byte[][] bytesToPrint;

    if (this.useEscAsteriskCommand) {
        // Usa comando ESC * alternativo
        bytesToPrint = convertGSv0ToEscAsterisk(image);
    } else if (this.enableImageSlicing) {
        // ✅ FATIAMENTO ATIVADO
        byte[][] strips = sliceGSv0Image(image, this.imageSliceLinesPerStrip);
        bytesToPrint = strips;
    } else {
        // Modo padrão: envia imagem completa
        bytesToPrint = new byte[][]{image};
    }

    // Envia cada tira com delay entre elas
    for (int i = 0; i < bytesToPrint.length; i++) {
        this.printerConnection.write(bytesToPrint[i]);
        this.printerConnection.send();
        
        if (this.enableImageSlicing && i < bytesToPrint.length - 1) {
            this.printerConnection.write(new byte[]{LF});  // Line feed
            this.printerConnection.send(50);                // 50ms delay
        }
    }

    return this;
}
```

## Exemplo Completo em Java

```java
public class PrinterExample {
    
    public void printReceiptWithGertec(DeviceConnection connection, Bitmap logo) 
            throws Exception {
        
        // 1. Criar impressora
        EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);
        
        // 2. ✅ HABILITAR FATIAMENTO PARA GERTEC
        printer
            .setImageSlicing(true)              // Liga fatiamento
            .setImageSliceLinesPerStrip(20);    // 20 linhas por tira
        
        // 3. Converter logo para hex
        String logoHex = PrinterTextParserImg.bitmapToHexadecimalString(
            printer, 
            logo
        );
        
        // 4. Montar layout com logo e QR code
        String text = 
            "[C]<img>" + logoHex + "</img>\n" +
            "[L]\n" +
            "[C]<u><font size='big'>PEDIDO N°12345</font></u>\n" +
            "[L]\n" +
            "[L]Cliente: João Silva\n" +
            "[L]Total: R$ 150,00\n" +
            "[L]\n" +
            "[C]<qrcode size='40'>PEDIDO-12345</qrcode>\n";
        
        // 5. ✅ IMPRIMIR - Logo e QR serão automaticamente fatiados!
        printer.printFormattedTextAndCut(text);
    }
    
    // Versão com detecção automática de Gertec
    public void printReceiptAuto(DeviceConnection connection, Bitmap logo, 
                                 String printerModel) throws Exception {
        
        EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);
        
        // ✅ Ativar fatiamento apenas para Gertec
        if ("gertec".equalsIgnoreCase(printerModel)) {
            printer
                .setImageSlicing(true)
                .setImageSliceLinesPerStrip(20);
        }
        
        String logoHex = PrinterTextParserImg.bitmapToHexadecimalString(printer, logo);
        
        String text = 
            "[C]<img>" + logoHex + "</img>\n" +
            "[C]<qrcode size='40'>PEDIDO-12345</qrcode>\n" +
            "[L]Texto após imagem imprime normalmente!\n";
        
        printer.printFormattedTextAndCut(text);
    }
}
```

## Integração com Cordova Plugin

### Modificar Plugin Java

```java
// No plugin Cordova (ex: ThermalPrinter.java)
@Override
public boolean execute(String action, JSONArray args, CallbackContext callback) {
    
    if (action.equals("printFormattedText")) {
        String text = args.getString(0);
        String printerModel = args.optString(1, "");  // Novo parâmetro opcional
        
        try {
            EscPosPrinter printer = new EscPosPrinter(
                getConnection(), 
                203, 
                48f, 
                32
            );
            
            // ✅ Habilitar fatiamento se Gertec
            if ("gertec".equalsIgnoreCase(printerModel)) {
                printer
                    .setImageSlicing(true)
                    .setImageSliceLinesPerStrip(20);
            }
            
            printer.printFormattedTextAndCut(text);
            callback.success("Printed successfully");
            return true;
            
        } catch (Exception e) {
            callback.error("Print failed: " + e.getMessage());
            return false;
        }
    }
    
    return false;
}
```

### Chamar do Angular/TypeScript

```typescript
// thermal-printer.service.ts

interface ThermalPrinterPlugin {
    printFormattedText(text: string, printerModel?: string): Promise<void>;
}

@Injectable()
export class ThermalPrinterService {
    
    private get plugin(): ThermalPrinterPlugin {
        return (window as any).ThermalPrinter;
    }
    
    isGertec(printer: any): boolean {
        // Detectar Gertec por MAC, nome ou ID armazenado
        if (printer.address?.startsWith('00:11:22:33:44:55')) return true;
        if (printer.name?.toLowerCase().includes('gertec')) return true;
        return false;
    }
    
    async printReceipt(ticket: any, printer: any): Promise<void> {
        const layout = this.buildTicketLayout(ticket);
        const printerModel = this.isGertec(printer) ? 'gertec' : '';
        
        // ✅ Passar printerModel - plugin aplicará fatiamento automaticamente
        await this.plugin.printFormattedText(layout, printerModel);
    }
    
    buildTicketLayout(ticket: any): string {
        // ✅ NÃO precisa mais fatiar manualmente!
        // O fatiamento será feito automaticamente pelo Java se printerModel='gertec'
        
        let layout = '';
        
        // Logo (será fatiado automaticamente se Gertec)
        if (this.printableLogo) {
            layout += `<img>${this.printableLogo}</img>`;
        }
        
        layout += `
            <u><font size='big'>PEDIDO N°${ticket.id}</font></u>
            
            Cliente: ${ticket.customer}
            Total: R$ ${ticket.total}
            
            <qrcode size='40'>${ticket.id}</qrcode>
        `;
        
        return layout;
    }
    
    // ✅ OPCIONAL: Remover função de fatiamento manual (não é mais necessária)
    // private splitGsV0HexIntoStrips(gsHex: string, stripLines: number = 20) { ... }
}
```

## Resultado da Análise

### ✅ CONFIRMADO:

1. **O fatiamento FUNCIONA para QR codes:**
   - `<qrcode>` → `QRCodeDataToBytes()` → `printImage()` → fatiamento aplicado ✅

2. **O fatiamento FUNCIONA para logos:**
   - `<img>hexString</img>` → `PrinterTextParserImg` → `printImage()` → fatiamento aplicado ✅

3. **O fatiamento é AUTOMÁTICO:**
   - Basta chamar `setImageSlicing(true)` antes de `printFormattedText()`
   - Todas as imagens (logos, QR, barcodes renderizados) serão fatiadas

4. **O fatiamento é CONFIGURÁVEL:**
   - `setImageSlicing(true/false)` - ativa/desativa
   - `setImageSliceLinesPerStrip(20)` - ajusta tamanho das tiras

### ❌ O que NÃO precisa fazer:

1. ~~Fatiar manualmente no TypeScript~~
2. ~~Modificar o hex do logo antes de enviar~~
3. ~~Criar múltiplas tags `<img>` para cada tira~~
4. ~~Tratar QR code diferente de logo~~

### 🎯 O que PRECISA fazer:

1. ✅ Atualizar plugin Cordova para aceitar parâmetro `printerModel`
2. ✅ Chamar `setImageSlicing(true)` quando `printerModel === "gertec"`
3. ✅ Passar `"gertec"` do Angular para o plugin Java
4. ✅ Simplificar código TypeScript (remover fatiamento manual)

## Comparação: Antes vs Depois

### ❌ ANTES (TypeScript - Solução Temporária)

```typescript
buildTicketLayout(ticket: any, printer: any): string {
    let layout = '';
    
    // ❌ Fatiamento manual no TypeScript
    if (this.printableLogo) {
        if (this.isGertec(printer)) {
            const strips = this.splitGsV0HexIntoStrips(this.printableLogo, 20);
            for (const strip of strips) {
                layout += `<img>${strip}</img>`;  // Múltiplas tags <img>
            }
        } else {
            layout += `<img>${this.printableLogo}</img>`;
        }
    }
    
    // ❌ QR code NÃO é fatiado (problema!)
    layout += `<qrcode size='40'>${ticket.id}</qrcode>`;
    
    return layout;
}
```

### ✅ DEPOIS (Java - Solução Nativa)

```typescript
buildTicketLayout(ticket: any): string {
    // ✅ Código limpo - fatiamento automático no Java
    return `
        <img>${this.printableLogo}</img>
        <qrcode size='40'>${ticket.id}</qrcode>
    `;
}

async printReceipt(ticket: any, printer: any): Promise<void> {
    const layout = this.buildTicketLayout(ticket);
    const printerModel = this.isGertec(printer) ? 'gertec' : '';
    
    // ✅ Java aplica fatiamento automaticamente
    await this.plugin.printFormattedText(layout, printerModel);
}
```

## Vantagens da Solução Nativa (7.0.0)

| Aspecto | TypeScript (Antigo) | Java 7.0.0 (Novo) |
|---------|---------------------|-------------------|
| **QR Code** | ❌ Não fatiado (trava Gertec) | ✅ Fatiado automaticamente |
| **Logo** | ⚠️ Fatiado manualmente | ✅ Fatiado automaticamente |
| **Código** | ⚠️ Duplicado (TS + Java) | ✅ Centralizado no Java |
| **Performance** | ⚠️ Processamento em JS | ✅ Processamento nativo |
| **Manutenção** | ❌ Difícil (2 lugares) | ✅ Fácil (1 lugar) |
| **Layout** | ❌ Múltiplas tags `<img>` | ✅ Tag única normal |
| **Transparência** | ❌ Visível no código | ✅ Invisível (automático) |

## Conclusão

🎉 **A biblioteca versão 7.0.0 já resolve completamente o problema da Gertec!**

Quando você passar `printerModel: "gertec"` no plugin Cordova, TODAS as imagens (logos, QR codes, barcodes) serão automaticamente fatiadas em tiras de 20 linhas, prevenindo o travamento da impressora.

Você só precisa:
1. Modificar o plugin Cordova para aceitar o parâmetro
2. Ativar `setImageSlicing(true)` quando detectar Gertec
3. Simplificar o código Angular (remover fatiamento manual)

✅ **Logo será fatiado automaticamente**  
✅ **QR code será fatiado automaticamente**  
✅ **Texto após imagens será impresso corretamente**  
✅ **Não requer modificação do layout**
