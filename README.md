# Inventário Gatil — App Android

App de gestão de inventário para associações que fazem CED (Captura,
Esterilização e Devolução) de animais errantes: doações, material CED
(com controlo de empréstimos/devoluções), parceiros com vantagens, gatos
disponíveis para adoção, e consultas/faturas de veterinário.

## Stack

- Kotlin + Jetpack Compose (Material 3)
- Firebase Authentication (login por email/palavra-passe)
- Cloud Firestore (base de dados, sincronizada entre utilizadores)
- Navigation Compose
- CameraX + Google ML Kit (leitura de código de barras/QR)
- WorkManager (verificação periódica para alertas de consultas de veterinário)
- Coil (carregamento de imagens locais escolhidas/tiradas antes de guardar)

## Estrutura do projeto

```
app/src/main/java/com/gatil/inventario/
  data/
    model/          -> DonationItem, CEDItem, LoanRecord, Partner, Advantage, UserProfile,
                        Cat, VetAppointment, VetInvoice
    repository/      -> acesso ao Firestore (CRUD + lógica de empréstimos/consultas/faturas)
  notifications/    -> NotificationHelper + VetReminderWorker (alertas de consultas)
  ui/
    login/          -> ecrã de login/registo
    session/        -> perfil/role do utilizador autenticado, partilhado por toda a app
    dashboard/       -> menu principal (com selos de stock baixo e consultas próximas)
    donations/       -> lista + adicionar doações
    ced/             -> lista + empréstimo/devolução de material CED + histórico
    partners/         -> lista de parceiros + vantagens
    cats/            -> ficha de gatos para adoção (com foto)
    vet/             -> consultas de veterinário + faturas
    scanner/         -> leitura de código de barras/QR (CameraX + ML Kit)
    navigation/       -> grafo de navegação
    theme/           -> cores e tema Material 3
  util/             -> utilitários (datas, valores, compressão de imagem, câmara, QR)
```

## Como configurar (passos necessários antes de correr)

1. **Criar o projeto no Firebase**
   - Vai a https://console.firebase.google.com e cria um novo projeto.
   - Adiciona uma app Android com o package name `com.gatil.inventario`.
   - Ativa **Authentication** → método "Email/Palavra-passe".
   - Ativa **Cloud Firestore** (modo produção, escolhe a região mais próxima, ex: `eur3`).

2. **Descarregar `google-services.json`**
   - No Firebase Console, descarrega o ficheiro `google-services.json`
   - Coloca-o em `app/google-services.json` (mesma pasta do `build.gradle.kts` do módulo app)
   - **Este ficheiro não está incluído neste projeto** — tem de ser gerado por ti no Firebase Console.

3. **Abrir no Android Studio**
   - Abre a pasta `InventarioApp` no Android Studio (versão recente, ex: Koala ou superior).
   - Deixa o Gradle sincronizar (vai descarregar as dependências).
   - Corre num emulador ou dispositivo físico (Android 8.0 / API 26 ou superior).
   - **Para testar a leitura de código de barras/QR**, se usares um emulador,
     cria-o com uma "Webcam0" configurada em Camera (no AVD Manager → Edit →
     Camera → Back: Webcam0) para usar a câmara do computador; num dispositivo
     físico funciona diretamente com a câmara normal.

4. **Regras de segurança do Firestore** (importante antes de ir para produção)
   Estas regras aplicam a mesma política que a app: qualquer utilizador autenticado
   pode ler, criar e editar; só quem tem `role: "ADMIN"` no seu perfil pode eliminar.
   Também impedem que um voluntário se autopromova a admin ao editar o próprio perfil.

   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {

       function isSignedIn() {
         return request.auth != null;
       }

       function isAdmin() {
         return isSignedIn() &&
           get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'ADMIN';
       }

       match /users/{userId} {
         allow read: if isSignedIn();
         // Ao criar a própria conta, o role tem de ser VOLUNTARIO (evita autopromoção no registo)
         allow create: if isSignedIn() && request.auth.uid == userId
                         && request.resource.data.role == 'VOLUNTARIO';
         // Pode editar o próprio perfil desde que não mude o role; admins podem mudar o de qualquer um
         allow update: if isAdmin() ||
                         (isSignedIn() && request.auth.uid == userId
                           && request.resource.data.role == resource.data.role);
         allow delete: if isAdmin();
       }

       match /donationItems/{itemId} {
         allow read, create, update: if isSignedIn();
         allow delete: if isAdmin();
       }

       match /cedItems/{itemId} {
         allow read, create, update: if isSignedIn();
         allow delete: if isAdmin();
       }

       match /loanRecords/{loanId} {
         allow read, create, update: if isSignedIn();
         allow delete: if isAdmin();
       }

       match /partners/{partnerId} {
         allow read, create, update: if isSignedIn();
         allow delete: if isAdmin();

         match /advantages/{advantageId} {
           allow read, create, update: if isSignedIn();
           allow delete: if isAdmin();
         }
       }

       match /cats/{catId} {
         allow read, create, update: if isSignedIn();
         allow delete: if isAdmin();
       }

       match /vetAppointments/{appointmentId} {
         allow read, create, update: if isSignedIn();
         allow delete: if isAdmin();
       }

       match /vetInvoices/{invoiceId} {
         allow read, create, update: if isSignedIn();
         allow delete: if isAdmin();
       }
     }
   }
   ```

   **Como criar o primeiro admin:** como ninguém começa como admin, vai ao Firebase
   Console → Firestore Database → coleção `users` → o teu documento de utilizador →
   muda o campo `role` de `"VOLUNTARIO"` para `"ADMIN"` manualmente. A partir daí,
   esse admin já pode gerir os restantes (se um dia adicionares um ecrã para isso).

   **Nota sobre o Firebase Storage:** a app não usa o Firebase Storage — as fotos
   dos gatos e das faturas são comprimidas no telemóvel e guardadas diretamente
   como texto dentro do próprio documento do Firestore. Isto significa que **não
   precisas de ativar o Storage nem de associar cartão de crédito ao projeto**;
   Firestore e Auth têm nível gratuito sem cartão associado.

## O que já está feito

- Login e registo de conta (Firebase Auth)
- Doações: listar, pesquisar, adicionar, editar, eliminar (eliminar só admin)
- Material CED: listar, pesquisar, adicionar, editar, eliminar (eliminar só admin),
  **registar empréstimo**, **registar devolução** (a quantidade disponível é atualizada
  automaticamente numa transação atómica, para evitar problemas se dois voluntários
  usarem a app ao mesmo tempo)
- Parceiros: listar, adicionar, editar, eliminar (eliminar só admin), e gerir vantagens
  associadas a cada parceiro (adicionar/eliminar; eliminar só admin)
- **Permissões admin vs voluntário:** voluntários podem ver, adicionar e editar;
  só admins podem eliminar itens, parceiros e vantagens. Aplicado tanto na app
  (botões escondidos/desativados) como nas regras do Firestore (servidor)
- **Histórico de empréstimos:** ecrã acessível pelo ícone de relógio no topo do
  ecrã de Material CED, com pesquisa por material/pessoa e filtro por estado
  (Todos / Emprestados / Devolvidos)
- **Leitura de código de barras/QR:** botão de câmara junto à pesquisa em Doações
  e Material CED — digitaliza um código e usa-o para encontrar o item na lista.
  Em Material CED também podes gerar uma etiqueta QR própria para cada item
  (ícone de QR no cartão) e imprimi-la para colar no material físico, mesmo que
  ele não tenha um código de fábrica.
- **Alertas de stock baixo:** em Doações (quantidade) e Material CED (quantidade
  disponível), podes definir por item um limite a partir do qual queres ser
  avisado. Itens em stock baixo ficam destacados a vermelho na lista, sobem para
  o topo automaticamente, e há um filtro "Só stock baixo" para os ver isolados.
  O ecrã principal (dashboard) mostra logo à entrada quantos itens precisam de
  atenção, com um selo numerado em cada secção afetada.
- **Gatos para Adoção:** nova secção com ficha completa por gato — foto (galeria
  ou câmara), nome, sexo, data de nascimento (ou idade aproximada, quando não há
  data exata), ninhada, local de captura, cor/pelagem, esterilização,
  desparasitação externa e interna, vacinação, teste FIV/FeLV, microchip,
  notas de saúde, temperamento, e estado de adoção (Disponível / Reservado /
  Adotado). A lista mostra fotos em grelha com filtro por estado de adoção e
  pesquisa por nome/ninhada/local de captura; eliminar um gato continua
  reservado a admins.
- **Consultas de Veterinário:** calendário de consultas por gato (motivo, local,
  veterinário, notas), com **alertas locais automáticos** — uma verificação em
  segundo plano (WorkManager, a cada 6h) avisa com uma notificação alguns dias
  antes da consulta (configurável por consulta), sem precisar de nenhum
  servidor externo.
- **Faturas de Veterinário:** registo de despesas por clínica (valor, data,
  gato associado, descrição, foto/scan do recibo), com separadores por clínica
  e total calculado automaticamente por separador.
- **Fotos sem custos de Storage:** as fotos dos gatos e das faturas são
  comprimidas e reduzidas no telemóvel (~640px, JPEG) e guardadas como texto
  diretamente dentro do documento do Firestore — a app não usa o Firebase
  Storage (que passou a exigir plano pago), fica tudo no nível gratuito.

## Próximos passos sugeridos

- Ecrã de gestão de utilizadores (promover/despromover admins a partir da app, sem ir ao Firebase Console)
- Exportação de relatórios (PDF/Excel)
