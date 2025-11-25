# Guia de Compatibilidade com Impressoras Gertec

## Problema Identificado

As impressoras Gertec possuem uma limitação de firmware que causa travamento ao processar imagens raster grandes (logos, QR codes) enviadas através do comando ESC/POS `GS v 0`. 

Quando uma imagem grande é impressa, a impressora:
1. Imprime a imagem corretamente
2. Trava ou congela após a impressão
3. Ignora todo o texto subsequente
4. Requer reinicialização manual

Este problema ocorre especialmente com:
- Logos com dimensões 200x200px ou maiores
- QR codes com `size='40'` ou maior
- Qualquer imagem que gere um comando `GS v 0` com mais de ~5000 bytes

## Solução Implementada (Versão 7.0.0)

A biblioteca agora inclui um recurso de **fatiamento automático de imagens** que divide imagens grandes em tiras verticais menores, prevenindo o travamento da Gertec.

### Como Funciona o Fatiamento

1. A imagem completa é gerada normalmente como comando `GS v 0`
2. O comando é analisado e dividido em múltiplas tiras horizontais
3. Cada tira é enviada como um comando `GS v 0` separado com altura recalculada
4. Entre cada tira, é inserido:
   - Um line feed (`LF` / `0x0A`)
   - Um delay de 50ms para recuperação da impressora
5. O resultado visual é idêntico—as divisões são invisíveis para o usuário final

### Estrutura do Comando GS v 0

```
Byte    Descrição
----    ---------
0       1D (GS - Group Separator)
1       76 (v - comando de imagem raster)
2       30 (0 - modo normal)
3       m (modo de impressão: 0=normal, 1=dupla largura, 2=dupla altura, 3=quadruplo tamanho)
4       xL (largura em bytes, byte baixo)
5       xH (largura em bytes, byte alto)
6       yL (altura em pixels, byte baixo)
7       yH (altura em pixels, byte alto)
8+      payload (dados da imagem em formato raster)

Largura em bytes = xL + (xH × 256)
Altura em pixels = yL + (yH × 256)
Tamanho do payload = largura × altura
```

### Exemplo de Fatiamento

**Imagem original:**
- Dimensões: 200x200px
- Largura em bytes: 25 (200 ÷ 8)
- Payload total: 5000 bytes (25 × 200)

**Após fatiamento (20 linhas por tira):**
- 10 tiras de 20 linhas cada
- Cada tira: 500 bytes (25 × 20)
- Comando completo: 8 bytes de header + 500 bytes de payload por tira

## Uso na Biblioteca Java (Android)

### Habilitando o Fatiamento

```java
// Criar instância da impressora
EscPosPrinter printer = new EscPosPrinter(
    BluetoothPrintersConnections.selectFirstPaired(), 
    203, 
    48f, 
    32
);

// Habilitar fatiamento para Gertec
printer
    .setImageSlicing(true)              // Ativar fatiamento
    .setImageSliceLinesPerStrip(20)     // Configurar linhas por tira
    .printFormattedText(
        "[C]<img>" + logoHex + "</img>\n" +
        "[C]<qrcode size='40'>PEDIDO-12345</qrcode>\n" +
        "[L]Detalhes do pedido aqui...\n"
    );
```

### Detecção Automática da Gertec

```java
public void imprimirRecibo(DeviceConnection conexao, String macAddress) {
    EscPosPrinter printer = new EscPosPrinter(conexao, 203, 48f, 32);
    
    // Detectar Gertec pelo MAC address (exemplo)
    boolean isGertec = macAddress.startsWith("00:11:22:33:44:55");
    
    if (isGertec) {
        printer
            .setImageSlicing(true)
            .setImageSliceLinesPerStrip(20);
    }
    
    printer.printFormattedText(
        "[C]<img>" + logoHex + "</img>\n" +
        "[C]<qrcode size='40'>PEDIDO-12345</qrcode>\n" +
        "[L]Cliente: João Silva\n" +
        "[L]Total: R$ 150,00\n"
    );
}
```

## Uso no Cordova Plugin (Angular/TypeScript)

### Opção 1: Fatiamento no JavaScript (Solução Atual)

Você já implementou a função `splitGsV0HexIntoStrips` no seu serviço Angular:

```typescript
private splitGsV0HexIntoStrips(gsHex: string, stripLines: number = 20): string[] {
    const header = gsHex.substring(0, 16); // 8 bytes = 16 chars hex
    
    if (header.substring(0, 6) !== '1d7630') {
        return [gsHex]; // Não é GS v 0, retornar original
    }
    
    // Parsear header
    const xL = parseInt(gsHex.substring(8, 10), 16);
    const xH = parseInt(gsHex.substring(10, 12), 16);
    const yL = parseInt(gsHex.substring(12, 14), 16);
    const yH = parseInt(gsHex.substring(14, 16), 16);
    
    const bytesPerLine = xL + (xH * 256);
    const totalLines = yL + (yH * 256);
    const payload = gsHex.substring(16);
    
    const numStrips = Math.ceil(totalLines / stripLines);
    const strips: string[] = [];
    
    for (let i = 0; i < numStrips; i++) {
        const startLine = i * stripLines;
        const endLine = Math.min(startLine + stripLines, totalLines);
        const currentStripLines = endLine - startLine;
        
        // Recalcular altura
        const newYL = currentStripLines % 256;
        const newYH = Math.floor(currentStripLines / 256);
        
        // Construir novo header
        const newHeader = header.substring(0, 12) + 
            newYL.toString(16).padStart(2, '0') + 
            newYH.toString(16).padStart(2, '0');
        
        // Extrair payload desta tira
        const startByte = startLine * bytesPerLine * 2; // *2 porque hex
        const stripPayload = payload.substring(
            startByte, 
            startByte + (currentStripLines * bytesPerLine * 2)
        );
        
        strips.push(newHeader + stripPayload);
    }
    
    return strips;
}

// Uso no buildTicketLayout
buildTicketLayout(ticket: any, printer: any): string {
    let layout = '';
    
    const isGertec = this.isGertec(printer);
    
    // Logo
    if (this.printableLogo) {
        if (isGertec) {
            // Fatiar logo para Gertec
            const strips = this.splitGsV0HexIntoStrips(this.printableLogo, 20);
            for (const strip of strips) {
                layout += `<img>${strip}</img>`;
            }
        } else {
            layout += `<img>${this.printableLogo}</img>`;
        }
    }
    
    // QR Code (também pode ser fatiado se necessário)
    layout += `<qrcode size='40'>${ticket.id}</qrcode>`;
    
    return layout;
}
```

### Opção 2: Fatiamento no Plugin Java (Recomendado)

**Vantagens:**
- Transparente para o código JavaScript
- Não requer modificação no layout
- Funciona automaticamente com logos e QR codes
- Melhor performance (processamento nativo)

**Como usar:**

1. **Atualizar para versão 7.0.0:**

```gradle
dependencies {
    implementation 'com.github.taylorlacort:nextqs-ESCPOS-ThermalPrinter-Android:7.0.0'
}
```

2. **Modificar o plugin Cordova para aceitar parâmetro `printerModel`:**

```java
// No plugin Cordova (ThermalPrinter.java ou similar)
@Override
public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
    if (action.equals("printFormattedText")) {
        String text = args.getString(0);
        String printerModel = args.optString(1, ""); // Novo parâmetro opcional
        
        // Configurar impressora
        EscPosPrinter printer = new EscPosPrinter(connection, 203, 48f, 32);
        
        // Habilitar fatiamento se Gertec
        if ("gertec".equalsIgnoreCase(printerModel)) {
            printer
                .setImageSlicing(true)
                .setImageSliceLinesPerStrip(20);
        }
        
        printer.printFormattedText(text);
        callbackContext.success();
        return true;
    }
    return false;
}
```

3. **Chamar do Angular com parâmetro:**

```typescript
printByUsb(ticket: any, printer: any): Observable<any> {
    const layout = this.buildTicketLayout(ticket, printer);
    const printerModel = this.isGertec(printer) ? 'gertec' : '';
    
    return from(
        (window as any).ThermalPrinter.printFormattedText(
            layout,
            printerModel  // Novo parâmetro
        )
    );
}
```

## Configuração Recomendada

### Linhas por Tira (Lines Per Strip)

| Valor | Segurança | Velocidade | Uso Recomendado |
|-------|-----------|------------|-----------------|
| 10    | Máxima    | Lenta      | Gertec com problemas graves |
| 20    | Alta      | Moderada   | **Padrão recomendado para Gertec** |
| 30    | Média     | Rápida     | Gertec sem problemas graves |
| 50    | Baixa     | Muito rápida | Teste com cautela |
| 100+  | Nenhuma   | Máxima     | Não recomendado (anula o fatiamento) |

### Quando Desabilitar o Fatiamento

O fatiamento deve ser **desabilitado** para:
- Impressoras que funcionam perfeitamente com imagens grandes
- Impressoras modernas (Epson TM-T20, etc.)
- Quando a velocidade de impressão é crítica

### Detecção de Impressoras Gertec

```typescript
// No seu serviço Angular
isGertec(printer: any): boolean {
    // Opção 1: Detecção por MAC address
    if (printer.address && printer.address.startsWith('00:11:22:33:44:55')) {
        return true;
    }
    
    // Opção 2: Detecção por nome
    if (printer.name && printer.name.toLowerCase().includes('gertec')) {
        return true;
    }
    
    // Opção 3: Detecção por ID armazenado
    const gertecIds = this.storage.get('gertec_printer_ids') || [];
    return gertecIds.includes(printer.id);
}
```

## Comparação de Abordagens

### Fatiamento no TypeScript (Atual)

**Prós:**
- Já implementado e funcionando
- Não requer atualização do plugin
- Controle fino sobre o processo

**Contras:**
- Código duplicado (TypeScript + Java)
- Não funciona para QR codes automaticamente
- Requer modificação do layout para cada imagem
- Processamento em JavaScript (mais lento)

### Fatiamento no Java (Versão 7.0.0)

**Prós:**
- Transparente—funciona automaticamente
- Aplica-se a logos, QR codes e barcodes
- Processamento nativo (mais rápido)
- Código centralizado na biblioteca
- Não requer modificação do layout

**Contras:**
- Requer atualização do plugin Cordova
- Requer adicionar parâmetro `printerModel`

## Migração Sugerida

### Passo 1: Atualizar Dependência

```gradle
// app/build.gradle
dependencies {
    implementation 'com.github.taylorlacort:nextqs-ESCPOS-ThermalPrinter-Android:7.0.0'
}
```

### Passo 2: Modificar Plugin Cordova

Adicionar suporte ao parâmetro `printerModel` no plugin Java.

### Passo 3: Atualizar Interface JavaScript

```typescript
// plugin-interface.ts
export interface ThermalPrinter {
    printFormattedText(text: string, printerModel?: string): Promise<void>;
}
```

### Passo 4: Simplificar buildTicketLayout

```typescript
// Remover fatiamento manual
buildTicketLayout(ticket: any, printer: any): string {
    let layout = '';
    
    // Logo (sem fatiamento manual)
    if (this.printableLogo) {
        layout += `<img>${this.printableLogo}</img>`;
    }
    
    // QR Code (também será fatiado automaticamente)
    layout += `<qrcode size='40'>${ticket.id}</qrcode>`;
    
    return layout;
}

// Passar printerModel ao imprimir
printByUsb(ticket: any, printer: any): Observable<any> {
    const layout = this.buildTicketLayout(ticket, printer);
    const printerModel = this.isGertec(printer) ? 'gertec' : '';
    
    return from(
        (window as any).ThermalPrinter.printFormattedText(layout, printerModel)
    );
}
```

### Passo 5: Remover Código Obsoleto

```typescript
// Pode remover ou manter como fallback
// private splitGsV0HexIntoStrips(...) { ... }
```

## Testes Recomendados

1. **Teste com Gertec:**
   - Imprimir logo 200x200px
   - Imprimir QR code size='40'
   - Verificar que o texto após a imagem é impresso corretamente

2. **Teste com outras impressoras:**
   - Garantir que impressoras normais continuam funcionando
   - Verificar velocidade de impressão (não deve ser afetada)

3. **Teste de stress:**
   - Imprimir múltiplos logos consecutivos
   - Imprimir logo + QR code + barcode na mesma página
   - Imprimir em diferentes resoluções (203 DPI, 300 DPI)

## Suporte e Problemas

Se você encontrar problemas:

1. **Verificar versão da biblioteca:** Deve ser 7.0.0 ou superior
2. **Confirmar que o fatiamento está habilitado:** `setImageSlicing(true)`
3. **Ajustar linhas por tira:** Tentar valores entre 10 e 30
4. **Verificar logs:** Procurar por erros de conexão ou timeout
5. **Testar com imagem menor:** Reduzir logo para 150x150px para verificar se é problema de tamanho

## Conclusão

A versão 7.0.0 da biblioteca já inclui toda a funcionalidade de fatiamento que você implementou em TypeScript. 

**Próximos passos recomendados:**
1. Atualizar o plugin Cordova para aceitar parâmetro `printerModel`
2. Passar `"gertec"` quando detectar impressora Gertec
3. Remover o fatiamento manual do código TypeScript
4. Testar com impressoras Gertec e outras

Isso tornará o código mais limpo, rápido e fácil de manter! 🚀
