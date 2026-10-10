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
