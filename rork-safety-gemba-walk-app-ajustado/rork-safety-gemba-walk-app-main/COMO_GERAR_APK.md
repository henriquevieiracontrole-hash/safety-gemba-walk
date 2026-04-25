# Como gerar o APK

1. Suba estes arquivos no GitHub.
2. Abra o repositório no GitHub.
3. Vá em **Actions**.
4. Clique em **Build Android APK**.
5. Clique em **Run workflow**.
6. Ao terminar, baixe o artefato **Safety-Gemba-Walk-debug-apk**.

Observação: se quiser manter a IA do Rork funcionando, cadastre o segredo do repositório:
Settings > Secrets and variables > Actions > New repository secret
Nome: EXPO_PUBLIC_RORK_TOOLKIT_SECRET_KEY
Valor: a chave do Rork, se você conseguir pegar em Variáveis ambientais.
