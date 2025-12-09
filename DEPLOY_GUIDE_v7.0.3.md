# 🚀 Guia de Publicação v7.0.3

## Status Atual
- ✅ **Branch criada:** `release/v7.0.3-gertec-centered`
- ✅ **Tag criada:** `v7.0.3`
- ✅ **Commits:** 2 commits prontos para publicação
- ✅ **Documentação:** Release notes criadas
- ✅ **Autoria:** Taylor Lacort adicionado em todas as funções Gertec

---

## 📋 Próximos Passos para Publicação

### 1️⃣ Testar a Build (RECOMENDADO)
```bash
cd /home/taylor-lacort/Documents/nextqs-ESCPOS-ThermalPrinter-Android
./gradlew clean build --no-daemon
```

### 2️⃣ Fazer Push da Branch e Tag
```bash
# Push da branch de release
git push origin release/v7.0.3-gertec-centered

# Push da tag
git push origin v7.0.3
```

### 3️⃣ Merge para Branch Principal (Opcional)
```bash
# Voltar para branch principal
git checkout v.7.0.x  # ou main/master

# Fazer merge da release
git merge release/v7.0.3-gertec-centered

# Push do merge
git push origin v.7.0.x  # ou main/master
```

### 4️⃣ Atualizar App Angular/TypeScript
No seu projeto que usa a biblioteca, atualize para:
```json
{
  "dependencies": {
    "nxtqs-thermal-printer-cordova-plugin": "^7.0.3"
  }
}
```

### 5️⃣ Testar em Produção
1. Instalar a nova versão no app
2. Testar impressão com impressora Gertec
3. Verificar centralização do logo
4. Validar com múltiplas impressões

---

## 🧪 Checklist de Testes

Antes de publicar em produção, teste:

- [ ] Logo aparece centralizado em impressoras Gertec
- [ ] Texto do ticket está correto
- [ ] QR Code está centralizado (se aplicável)
- [ ] Impressão funciona com múltiplas cópias
- [ ] Não houve regressão em outras impressoras ESC/POS
- [ ] Build compila sem erros
- [ ] Testes unitários passam (se disponíveis)

---

## 📊 Alterações Técnicas

### Arquivos Modificados
1. ✅ `EscPosPrinterCommands.java` - Lógica de fatiamento e centralização
2. ✅ `EscPosPrinter.java` - API pública
3. ✅ `PrinterTextParserImg.java` - Parser de imagens

### Funções com Autoria Taylor Lacort
- `setImageSlicing()`
- `isImageSlicingEnabled()`
- `setImageSliceLinesPerStrip()`
- `setImagePrintableWidthPx()`
- `setImagePrintableWidthBytes()`
- `sliceGSv0Image()`
- `centerAndSliceGSv0Image()`
- `PrinterTextParserImg` constructor
- `getPrinter()`

---

## 🐛 Rollback (Se Necessário)

Se houver problemas, você pode reverter:

```bash
# Voltar para versão anterior
git checkout v.7.0.2-centered

# Ou deletar a tag localmente (antes do push)
git tag -d v7.0.3

# Deletar a branch (antes do push)
git branch -D release/v7.0.3-gertec-centered
```

---

## 📞 Contato

**Desenvolvedor:** Taylor Lacort  
**Versão:** 7.0.3  
**Data:** 09/12/2025

---

## ✅ Comando Rápido para Publicar

```bash
# Teste a build primeiro
./gradlew clean build --no-daemon

# Se tudo estiver OK, publique
git push origin release/v7.0.3-gertec-centered
git push origin v7.0.3

echo "✅ Release v7.0.3 publicada com sucesso!"
```

---

**Boa sorte com a publicação! 🎉**
