# Comportamento da Cor Padrão de Texto

## Versão 7.0.7 - Mudança de Comportamento

### Contexto

Nas versões anteriores (até 7.0.6), a cor padrão do texto era **sempre preta** (`TEXT_COLOR_BLACK`), independentemente do tipo de impressora.

```java
// Código antigo (até v7.0.6)
private byte[][] textColor = {EscPosPrinterCommands.TEXT_COLOR_BLACK};
```

### Problema Identificado

As impressoras **Gertec** apresentam comportamento diferente quando comandos de cor são enviados mesmo sem especificação explícita de cor no texto formatado. Isso pode causar problemas de compatibilidade.

### Solução Implementada (v7.0.7+)

A cor padrão agora é **condicional** baseada no tipo de equipamento:

#### 1. Equipamentos Gertec (com `imageSlicing` habilitado)
- **Sem cor padrão** - comandos de cor só são enviados quando explicitamente definidos no texto formatado
- Exemplo: `<font color='black'>Texto</font>`

#### 2. Outros Equipamentos (sem `imageSlicing`)
- **Cor preta padrão** - mantém compatibilidade com código legado
- Comportamento idêntico às versões anteriores

### Implementação

```java
public PrinterTextParser(EscPosPrinter printer) {
    this.printer = printer;
    // Para equipamentos Gertec (com imageSlicing habilitado), não definir cor padrão
    // Para outros equipamentos, usar cor preta como padrão (compatibilidade com código antigo)
    if (printer != null && printer.getPrinter() != null && printer.getPrinter().isImageSlicingEnabled()) {
        this.textColor = new byte[0][];  // Sem cor padrão para Gertec
    } else {
        this.textColor = new byte[][]{EscPosPrinterCommands.TEXT_COLOR_BLACK};  // Cor preta padrão para outros
    }
}
```

### Detecção Automática

A detecção é feita automaticamente através do status do `imageSlicing`:

```java
EscPosPrinter printer = new EscPosPrinter(conexao, 203, 48f, 32);

// Para Gertec - sem cor padrão
printer.setImageSlicing(true);  // Ativa modo Gertec

// Para outros - cor preta padrão
// (não chamar setImageSlicing ou chamar com false)
```

### Impacto na Aplicação

**Nenhuma mudança necessária** no código da aplicação. A lógica é transparente e automática:

```java
// Este código funciona para ambos os casos
printer
    .setImageSlicing(isGertec)  // true para Gertec, false para outros
    .printFormattedText(
        "[C]Cabeçalho\n" +
        "[L]Item 1\n" +
        "[L]<font color='red'>Erro!</font>\n"  // Cor explícita sempre funciona
    );
```

### Referências

- **Arquivo modificado**: `escposprinter/src/main/java/com/dantsu/escposprinter/textparser/PrinterTextParser.java`
- **Método afetado**: Constructor `PrinterTextParser(EscPosPrinter printer)`
- **Guia Gertec**: `GERTEC_GUIDE_PT-BR.md`
- **Notas de Release**: `RELEASE_NOTES_v7.0.3.md`

### Testes Recomendados

1. ✅ Imprimir texto simples em Gertec (sem cores explícitas)
2. ✅ Imprimir texto com cores explícitas em Gertec
3. ✅ Imprimir texto simples em impressoras não-Gertec
4. ✅ Imprimir texto com cores explícitas em impressoras não-Gertec

### Compatibilidade com Código Legado

✅ **100% compatível** - Aplicações existentes continuam funcionando sem modificações:
- Impressoras não-Gertec mantêm comportamento idêntico
- Impressoras Gertec funcionam corretamente com a nova lógica
- Código com cores explícitas funciona em ambos os casos
