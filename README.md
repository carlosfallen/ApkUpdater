# APK Auto-Updater

Sistema automatizado de atualização de APKs via servidor local, projetado para manter aplicativos Android sempre atualizados sem intervenção manual.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Características](#características)
- [Arquitetura](#arquitetura)
- [Requisitos](#requisitos)
- [Instalação](#instalação)
- [Configuração](#configuração)
- [Uso](#uso)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [API Endpoints](#api-endpoints)
- [Troubleshooting](#troubleshooting)

## 🎯 Visão Geral

O APK Auto-Updater é uma solução completa para distribuição automática de atualizações de aplicativos Android em ambientes controlados (empresas, dispositivos dedicados, etc.). O sistema monitora continuamente um servidor local e notifica/instala automaticamente novas versões quando disponíveis.

### Componentes

1. **Servidor Node.js**: Serve os APKs via API REST
2. **App Android**: Cliente que monitora e instala atualizações automaticamente
3. **Sistema de Notificações**: Alertas inteligentes para atualizações disponíveis

## ✨ Características

### Servidor
- ✅ API REST simples e eficiente
- ✅ Suporte para múltiplos APKs simultâneos
- ✅ Listagem de todos os APKs disponíveis
- ✅ Informações detalhadas (tamanho, data de modificação)
- ✅ Download direto via HTTP

### App Android
- ✅ Monitoramento automático a cada 15 segundos
- ✅ Serviço em foreground (continua executando em background)
- ✅ Notificações inteligentes com prioridades diferentes
- ✅ Instalação com um toque
- ✅ Suporte para Android 5.0+ (API 21+)
- ✅ Detecção inteligente de mudanças (timestamp-based)
- ✅ Otimizado para baixo consumo de bateria
- ✅ Interface minimalista em Jetpack Compose

## 🏗️ Arquitetura
```
┌─────────────────┐         HTTP/REST        ┌──────────────────┐
│   Servidor      │◄─────────────────────────┤   App Android    │
│   Node.js       │                          │   (Cliente)      │
│                 │                          │                  │
│  ┌───────────┐  │    GET /api/bonus/info   │  ┌────────────┐  │
│  │  Express  │  │◄─────────────────────────┤  │  Service   │  │
│  │           │  │                          │  │ (Monitoring)│  │
│  │  Serve    │  │    GET /api/bonus        │  │            │  │
│  │   APKs    │  │◄─────────────────────────┤  │ Download & │  │
│  │           │  │                          │  │  Install   │  │
│  └───────────┘  │                          │  └────────────┘  │
│                 │                          │                  │
│  /srv/samba/    │                          │  Notifications   │
│  local/Apk/     │                          │  + UI            │
└─────────────────┘                          └──────────────────┘
```

## 💻 Requisitos

### Servidor
- Node.js 16+
- Acesso a diretório de APKs
- Porta 5176 disponível

### Cliente Android
- Android 5.0+ (API 21+)
- Permissões:
  - `INTERNET`
  - `REQUEST_INSTALL_PACKAGES`
  - `FOREGROUND_SERVICE`
  - `POST_NOTIFICATIONS` (Android 13+)

## 🚀 Instalação

### 1. Configurar o Servidor
```bash
# Clone o repositório
git clone <repo-url>
cd apk-updater/server

# Instale as dependências
npm install

# Configure o diretório dos APKs (edite server.js)
const APK_DIR = '/srv/samba/local/Apk';

# Inicie o servidor
node server.js
```

O servidor estará disponível em `http://0.0.0.0:5176`

### 2. Compilar o App Android
```bash
# No Android Studio
1. Abra o projeto em app/
2. Aguarde o Gradle sync
3. Build > Build Bundle(s) / APK(s) > Build APK(s)
4. O APK estará em app/build/outputs/apk/release/
```

### 3. Instalar o App no Dispositivo
```bash
adb install app-release.apk
```

Ou transfira o APK manualmente e instale.

## ⚙️ Configuração

### Servidor - Adicionar Novos APKs

Basta colocar os arquivos `.apk` no diretório configurado:
```bash
# Para monitoramento específico (Bonus e Updater)
/srv/samba/local/Apk/Bonus.apk
/srv/samba/local/Apk/Updater.apk

# Outros APKs ficam disponíveis via /api/apps
```

### App Android - Alterar IP do Servidor

Edite `UpdateService.kt`:
```kotlin
private val baseUrl = "http://10.0.11.150:5176"  // Altere para seu IP
```

### Ajustar Intervalo de Verificação

Edite `UpdateService.kt`:
```kotlin
delay(15000)  // 15 segundos (valor em milissegundos)
```

## 📱 Uso

### Primeira Execução

1. **Instale o app no dispositivo**
2. **Abra o aplicativo**
3. **Conceda as permissões solicitadas:**
   - Instalar aplicativos de fontes desconhecidas
   - Permitir notificações (Android 13+)
4. **O serviço inicia automaticamente**

### Fluxo de Atualização
```
1. App verifica servidor a cada 15s
2. Servidor retorna timestamp do APK
3. Se timestamp > último conhecido:
   ├─ Notificação: "Atualização encontrada"
   ├─ Download automático do APK
   ├─ Notificação: "Toque para instalar"
   └─ Abre tela de instalação automaticamente
4. Usuário confirma instalação
5. App atualiza e continua monitorando
```

### Notificações

**Notificação Persistente (Baixa Prioridade)**
- Sempre visível na barra de status
- Mostra status atual: "Aguardando atualizações..."
- Não emite som/vibração

**Notificação de Instalação (Alta Prioridade)**
- Aparece quando atualização está pronta
- Emite som e vibração
- Botão "Instalar Agora"
- Auto-dismiss após instalação

## 📁 Estrutura do Projeto
```
apk-updater/
├── server/
│   ├── server.js              # Servidor Express
│   ├── package.json
│   └── dist/                  # Frontend (opcional)
│
└── app/                       # Projeto Android
    ├── src/main/
    │   ├── java/com/example/apkupdater/
    │   │   ├── MainActivity.kt        # Activity principal
    │   │   └── UpdateService.kt       # Serviço de monitoramento
    │   │
    │   ├── res/
    │   │   ├── xml/
    │   │   │   └── file_paths.xml    # FileProvider config
    │   │   └── values/
    │   │       └── strings.xml
    │   │
    │   └── AndroidManifest.xml
    │
    ├── build.gradle.kts
    └── proguard-rules.pro
```

## 🔌 API Endpoints

### GET /api/apps
Lista todos os APKs disponíveis no diretório.

**Response:**
```json
[
  {
    "name": "Bonus.apk",
    "size": "25.43 MB",
    "sizeBytes": 26671104,
    "modified": "2025-10-23T10:30:00.000Z",
    "modifiedTimestamp": 1729676400000
  }
]
```

### GET /api/bonus/info
Retorna informações do APK Bonus.

**Response:**
```json
{
  "available": true,
  "name": "Bonus.apk",
  "size": "25.43 MB",
  "sizeBytes": 26671104,
  "modifiedTimestamp": 1729676400000,
  "modified": "2025-10-23T10:30:00.000Z"
}
```

### GET /api/bonus
Download direto do APK Bonus.

**Headers:**
```
Content-Type: application/vnd.android.package-archive
Content-Disposition: attachment; filename="Bonus.apk"
Content-Length: 26671104
```

### GET /api/updater/info
Retorna informações do APK Updater (idêntico ao /api/bonus/info).

### GET /api/updater
Download direto do APK Updater (idêntico ao /api/bonus).

### GET /api/download/:filename
Download de qualquer APK no diretório.

**Exemplo:**
```
GET /api/download/MeuApp.apk
```

## 🔧 Troubleshooting

### Servidor não inicia
```bash
# Verifique se a porta está em uso
lsof -i :5176

# Teste acesso ao diretório
ls -la /srv/samba/local/Apk/
```

### App não detecta atualizações

1. **Verifique conectividade:**
```bash
   # Do dispositivo Android
   curl http://10.0.11.150:5176/api/bonus/info
```

2. **Verifique IP no código:**
   - Certifique-se que `baseUrl` em `UpdateService.kt` está correto

3. **Verifique logs:**
```bash
   adb logcat | grep APKUpdater
```

### Instalação falha

1. **Permissão de instalar apps:**
   - Configurações > Segurança > Fontes desconhecidas
   - Android 8+: Configurações > Apps > Acesso especial > Instalar apps desconhecidos

2. **Espaço insuficiente:**
   - Libere espaço no dispositivo

3. **Assinatura incompatível:**
   - Se já existe uma versão instalada com assinatura diferente, desinstale primeiro

### Serviço para de funcionar
```bash
# Verifique se o serviço está rodando
adb shell dumpsys activity services | grep UpdateService

# Reinicie o app
adb shell am force-stop com.example.apkupdater
adb shell am start -n com.example.apkupdater/.MainActivity
```

### Consumo alto de bateria

- Aumente o intervalo de verificação (padrão: 15s)
- Considere usar WorkManager para verificações menos frequentes

## 📝 Notas Importantes

### Segurança

⚠️ **Este sistema foi projetado para redes locais/privadas**

- Não há autenticação
- Tráfego HTTP sem criptografia
- Para produção, considere:
  - HTTPS/TLS
  - Autenticação por token
  - Verificação de assinatura do APK

### Otimizações para Produção

1. **MinSDK mais alto**: Se todos os dispositivos forem modernos, aumente `minSdk` para reduzir tamanho do APK
2. **ProGuard/R8**: Já habilitado em release builds
3. **Versionamento**: Implemente verificação de `versionCode` além de timestamp
4. **Retry logic**: Adicione tentativas em caso de falha de rede

