# 📦 Guia de Publicação JitPack - v7.0.3

## ✅ Status
- ✅ **Versão atualizada:** 7.0.3 (versionCode: 7000003)
- ✅ **Tag criada:** v7.0.3
- ✅ **Branch:** release/v7.0.3-gertec-centered
- ✅ **jitpack.yml configurado**
- ✅ **README atualizado**
- ✅ **Build.gradle configurado**

---

## 🚀 Passos para Publicar no JitPack

### 1️⃣ Push para GitHub
```bash
# Push da branch
git push origin release/v7.0.3-gertec-centered

# Push da tag (IMPORTANTE!)
git push origin v7.0.3

# Ou push de todas as tags
git push origin --tags
```

### 2️⃣ Acesse o JitPack
Abra o navegador e acesse:
```
https://jitpack.io/#taylorlacort/nextqs-ESCPOS-ThermalPrinter-Android
```

### 3️⃣ Construa a Release
1. No campo "Look up", digite: **v7.0.3**
2. Clique em **"Get it"**
3. Aguarde a build ser concluída (pode levar alguns minutos)
4. Status deve mostrar: ✅ **"Build: Success"**

### 4️⃣ Verifique a Build
Após a build, você verá:
```
📦 Version: 7.0.3
✅ Status: SUCCESS
📥 Downloads: Available
```

---

## 📱 Como Usar no Seu Projeto

### Gradle (Project level - settings.gradle ou build.gradle)
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }  // ← Adicione esta linha
    }
}
```

**OU** no build.gradle antigo:
```gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }  // ← Adicione esta linha
    }
}
```

### Gradle (Module level - app/build.gradle)
```gradle
dependencies {
    implementation 'com.github.taylorlacort:nextqs-ESCPOS-ThermalPrinter-Android:7.0.3'
}
```

### Sync Gradle
```bash
# No Android Studio
File → Sync Project with Gradle Files
```

---

## 🔍 Verificação de Build

### Verificar se a versão foi publicada:
```
https://jitpack.io/api/builds/com.github.taylorlacort/nextqs-ESCPOS-ThermalPrinter-Android/v7.0.3
```

### Ver logs da build:
```
https://jitpack.io/com/github/taylorlacort/nextqs-ESCPOS-ThermalPrinter-Android/7.0.3/build.log
```

### Badge do JitPack (já no README):
[![](https://jitpack.io/v/taylorlacort/nextqs-ESCPOS-ThermalPrinter-Android.svg)](https://jitpack.io/#taylorlacort/nextqs-ESCPOS-ThermalPrinter-Android)

---

## 🐛 Troubleshooting

### Se a build falhar:

1. **Verifique o log:**
   ```
   https://jitpack.io/com/github/taylorlacort/nextqs-ESCPOS-ThermalPrinter-Android/7.0.3/build.log
   ```

2. **Delete e recrie a tag:**
   ```bash
   # Local
   git tag -d v7.0.3
   git tag -a v7.0.3 -m "Release v7.0.3"
   
   # Remote
   git push origin :refs/tags/v7.0.3
   git push origin v7.0.3
   ```

3. **Force rebuild no JitPack:**
   - Acesse: https://jitpack.io/#taylorlacort/nextqs-ESCPOS-ThermalPrinter-Android
   - Clique no ícone 🗑️ ao lado da versão
   - Clique em "Get it" novamente

### Se não aparecer no JitPack:

1. Certifique-se que o repositório é público
2. Verifique se a tag foi enviada: `git ls-remote --tags origin`
3. Aguarde 5-10 minutos e tente novamente

---

## 📊 Informações da Release

**Repository:** https://github.com/taylorlacort/nextqs-ESCPOS-ThermalPrinter-Android  
**GroupId:** com.github.taylorlacort  
**ArtifactId:** nextqs-ESCPOS-ThermalPrinter-Android  
**Version:** 7.0.3  
**VersionCode:** 7000003  
**Tag:** v7.0.3  
**Branch:** release/v7.0.3-gertec-centered  

**Compatibilidade:**
- Min SDK: 16 (Android 4.1)
- Target SDK: 33 (Android 13)
- Java: 11+

---

## 🎯 Changelog v7.0.3

### 🐛 Bug Fixes
- Fixed logo centering for Gertec printers
- Prevented double centering issue

### ✨ New Features
- Added `isImageSlicingEnabled()` method
- Added `getPrinter()` method for low-level access

### 📝 Documentation
- Added Taylor Lacort authorship to Gertec functions
- Updated release notes and deployment guide

---

## 📞 Suporte

**Desenvolvedor:** Taylor Lacort  
**Email:** (adicione se necessário)  
**GitHub Issues:** https://github.com/taylorlacort/nextqs-ESCPOS-ThermalPrinter-Android/issues

---

## ✅ Comandos Completos para Publicação

```bash
# Navegar para o projeto
cd /home/taylor-lacort/Documents/nextqs-ESCPOS-ThermalPrinter-Android

# Verificar status
git status
git log --oneline -5

# Push da branch e tag
git push origin release/v7.0.3-gertec-centered
git push origin v7.0.3

# Confirmar push
git ls-remote --tags origin | grep v7.0.3

# Aguardar e verificar JitPack
echo "Acesse: https://jitpack.io/#taylorlacort/nextqs-ESCPOS-ThermalPrinter-Android"
echo "Digite 'v7.0.3' e clique em 'Get it'"
```

---

## 🎉 Após Publicação

1. ✅ Verificar badge no README está atualizado
2. ✅ Testar instalação em um projeto de teste
3. ✅ Documentar no changelog do projeto principal
4. ✅ Notificar equipe sobre nova versão disponível

---

**Boa sorte com a publicação no JitPack! 🚀**

_Última atualização: 09/12/2025 - Taylor Lacort_
