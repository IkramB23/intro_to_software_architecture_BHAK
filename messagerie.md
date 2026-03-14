# TP — Vérification d'e‑mail avec messagerie (Spring Boot + RabbitMQ)

## Objectifs pédagogiques

- Comprendre le découplage entre services via la messagerie (RabbitMQ).
- Mettre en place un flux d'inscription avec envoi d'e‑mail asynchrone et vérification par lien.
- Implémenter la génération et la validation d'un token de vérification stocké sous forme de hash.
- Configurer un échange, une file et une DLQ RabbitMQ.
- Manipuler Spring Boot (Web, Data JPA, AMQP) et un serveur SMTP local (MailHog).

---

## Architecture

```
Client
  │
  ▼
Auth Service (port 8080)
  ├── POST /api/auth/register  →  crée le user (verified=false)
  │                                génère un token UUID, stocke le hash BCrypt
  │                                publie "UserRegistered" sur RabbitMQ
  │
  └── GET /api/auth/verify?tokenId=...&t=...  →  valide le token
                                                   marque verified=true
                                                   supprime le token (one-shot)

RabbitMQ (port 5672 / UI 15672)
  ├── Exchange: auth.events (topic)
  ├── Queue: notification.user-registered
  ├── DLX: auth.events.dlx
  └── DLQ: notification.user-registered.dlq

Notification Service (port 8081)
  └── @RabbitListener sur "notification.user-registered"
        → construit le lien de vérification
        → envoie l'e-mail via MailHog

MailHog (SMTP 1025 / UI 8025)
  └── Boîte de réception web pour visualiser les e-mails
```

---

## Architecture cible (vue simple)

Le TP suit une architecture **event-driven** composée de deux microservices découplés par un broker de messages (RabbitMQ). Voici le flux complet pas à pas :

```
┌────────┐    POST /register    ┌──────────────┐
│ Client │ ──────────────────►  │ Auth Service  │
└────────┘                      │  (port 8080)  │
                                └──────┬───────┘
                                       │
                          1. Crée le user (verified=false)
                          2. Génère un token UUID, stocke le hash BCrypt
                          3. Publie "UserRegistered" sur RabbitMQ
                                       │
                                       ▼
                          ┌─────────────────────────┐
                          │      RabbitMQ            │
                          │  Exchange: auth.events   │
                          │       (topic)            │
                          └──────────┬──────────────┘
                                     │ routing key: auth.user-registered
                                     ▼
                          ┌──────────────────────────────┐
                          │ Queue: notification.          │
                          │        user-registered        │
                          └──────────┬───────────────────┘
                                     │
                                     ▼
                          ┌──────────────────────────┐
                          │ Notification Service     │
                          │  (port 8081)             │
                          │  @RabbitListener          │
                          │  → construit le lien      │
                          │  → envoie l'e-mail SMTP   │
                          └──────────┬───────────────┘
                                     │
                                     ▼
                          ┌──────────────────────────┐
                          │  MailHog (SMTP 1025)     │
                          │  UI : http://localhost:8025 │
                          └──────────────────────────┘

         ┌────────┐  GET /verify?tokenId=...&t=...  ┌──────────────┐
         │ Client │ ──────────────────────────────► │ Auth Service  │
         │ (clic  │                                 │  vérifie le   │
         │  lien) │                                 │  token, marque│
         └────────┘                                 │  verified=true│
                                                    └──────┬───────┘
                                                           │ (optionnel)
                                                           ▼
                                              Publie "EmailVerified"
                                              sur auth.events
```

### Explication détaillée du flux

1. **Le client envoie `POST /api/auth/register`** avec username, email et password. C'est le point d'entrée unique pour l'inscription.

2. **Auth Service traite la requête** :
   - Crée l'utilisateur en base avec `verified = false`.
   - Génère un identifiant court (`tokenId`, ex. `tok_abc123`) et un UUID aléatoire comme token clair.
   - Stocke **uniquement le hash BCrypt** du token en table `tbl_verification_tokens` (le token clair n'est jamais persisté, par sécurité).
   - Publie un événement `UserRegistered` (JSON) sur l'exchange RabbitMQ `auth.events` avec la routing key `auth.user-registered`.

3. **RabbitMQ route le message** :
   - L'exchange `auth.events` (type **topic**) reçoit le message.
   - Grâce au binding `auth.user-registered`, le message est délivré dans la queue `notification.user-registered`.
   - Si le consommateur échoue, le message est redirigé vers la **DLQ** `notification.user-registered.dlq` via le DLX `auth.events.dlx` (mécanisme de dead-letter).

4. **Notification Service consomme le message** :
   - Le `@RabbitListener` sur la queue `notification.user-registered` désérialise l'événement JSON en `UserRegisteredEvent`.
   - `EmailService` construit un lien de vérification : `http://localhost:8080/api/auth/verify?tokenId=tok_abc123&t=<uuid-clair>`.
   - Envoie un e-mail via SMTP vers **MailHog** (port 1025).

5. **L'utilisateur clique sur le lien dans l'e-mail** → `GET /api/auth/verify?tokenId=...&t=...` :
   - Auth Service récupère le token par son `tokenId`.
   - Vérifie que le token n'a pas expiré (TTL de 30 minutes).
   - Compare le token clair reçu avec le hash BCrypt stocké.
   - Si la vérification réussit : marque `verified = true` et supprime le token (usage unique, one-shot).
   - L'opération est **idempotente** : si le compte est déjà vérifié, aucune erreur n'est levée.

6. **(Optionnel — Séance 2)** Auth publie un événement `EmailVerified` sur le même exchange, qui pourra être consommé par un service Analytics pour compter les vérifications.

### Pourquoi cette architecture ?

| Principe | Explication |
|----------|-------------|
| **Découplage** | Auth ne connaît pas Notification. Il publie un événement, c'est tout. On peut ajouter d'autres consommateurs (Analytics, CRM…) sans toucher à Auth. |
| **Asynchrone** | L'envoi d'e-mail se fait en arrière-plan. Le client reçoit sa réponse immédiatement, sans attendre le SMTP. |
| **Résilience** | Si Notification est en panne, le message reste dans la queue RabbitMQ et sera traité dès son redémarrage. En cas d'erreur, la DLQ capture le message pour analyse. |
| **Sécurité** | Le token clair transite uniquement dans le message RabbitMQ et dans le lien e-mail. En base, seul le hash BCrypt est stocké. Un accès en lecture à la BDD ne compromet aucun token. |

---

## Pourquoi un hash pour le token ?

### Principe

Le token de vérification est un **secret à usage unique**. Comme pour les mots de passe, on ne stocke **jamais** le token en clair en base de données : on stocke uniquement un **hash BCrypt**. Lors de la vérification, le token reçu dans l'URL est comparé au hash stocké via `BCryptPasswordEncoder.matches()`. Ainsi, une fuite de la base de données ne permet pas d'utiliser les tokens.

### Implémentation dans le projet

Dans `VerificationService.java` :

```java
// À la création (register) — on stocke UNIQUEMENT le hash
String rawToken = UUID.randomUUID().toString();           // token clair (envoyé par e-mail)
String tokenHash = passwordEncoder.encode(rawToken);      // hash BCrypt (stocké en BDD)
verificationToken.setTokenHash(tokenHash);                // seul le hash est persisté
```

```java
// À la vérification (GET /verify) — on compare sans jamais stocker le clair
if (!passwordEncoder.matches(rawToken, token.getTokenHash())) {
    throw new InvalidRequestException("Token invalide");  // comparaison BCrypt
}
// Si ok → verified=true, puis suppression du token (one-shot)
```

Le token clair (`rawToken`) n'existe que :
1. En mémoire lors de la création (le temps de publier l'événement RabbitMQ)
2. Dans le message RabbitMQ transmis à Notification
3. Dans le lien e-mail envoyé à l'utilisateur

Il n'est **jamais écrit en base de données**.

### Bénéfices

| Bénéfice | Détail |
|----------|--------|
| **Confidentialité** | Aucun secret en clair en base. Une fuite SQL ne compromet rien. |
| **Intégrité** | Le hash garantit que le token n'a pas été modifié. Seul le détenteur du token clair peut passer la vérification BCrypt. |
| **Révocation simplifiée** | Pour invalider un token, il suffit de supprimer l'entrée en base. Pas besoin de blacklist ou de mécanisme complexe. |
| **Conformité** | Réduit les données sensibles stockées, conforme aux bonnes pratiques OWASP et aux exigences RGPD de minimisation des données. |
| **Usage unique (one-shot)** | Le token est supprimé après vérification réussie. Impossible de le réutiliser. |

---

## Pourquoi passer par une messagerie ?

### Découplage

Auth Service ne connaît pas Notification Service et n'attend pas la fin de l'envoi d'e-mail. Il se contente de **publier un fait métier** (`UserRegistered`) sur RabbitMQ, puis rend immédiatement la réponse HTTP au client.

Concrètement dans notre code, `EventPublisher` envoie un message JSON sur l'exchange `auth.events` — il ne sait même pas qui le consomme. Cela signifie qu'on peut :
- Ajouter de nouveaux consommateurs (Analytics, CRM, logs…) **sans modifier une seule ligne dans Auth**.
- Remplacer ou redéployer Notification indépendamment d'Auth.
- Faire évoluer chaque service à son propre rythme (technologie, version, équipe).

### Résilience & scalabilité

En cas de **pic d'inscriptions** ou d'**indisponibilité temporaire du SMTP** (MailHog en panne, timeout réseau…), les messages s'accumulent dans la queue RabbitMQ au lieu d'être perdus :

```
Pic d'inscriptions             Notification en panne
       │                              │
       ▼                              ▼
  100 messages/s               Messages en attente
  → RabbitMQ absorbe           dans la queue
  → Notification traite        → Notification redémarre
    à son rythme               → Messages traités
```

Les mécanismes de résilience mis en place dans le projet :

| Mécanisme | Rôle | Implémentation |
|-----------|------|----------------|
| **Queue durable** | Les messages survivent à un redémarrage de RabbitMQ | `durable = true` dans `RabbitMQConfig` |
| **DLX / DLQ** | Les messages qui échouent (exception côté Notification) sont redirigés vers `notification.user-registered.dlq` pour analyse, au lieu d'être perdus | Exchange `auth.events.dlx` + queue DLQ déclarés dans `RabbitMQConfig` |
| **Retries** | RabbitMQ peut re-délivrer un message non acquitté | Comportement par défaut de Spring AMQP |
| **Scalabilité horizontale** | On peut lancer N instances de Notification qui consomment la même queue en parallèle | Chaque instance se connecte à la même queue |

### Traçabilité

Chaque événement publié est **structuré** et contient des métadonnées permettant l'audit :

```json
{
  "type": "UserRegistered",
  "eventId": "evt_a1b2c3d4",
  "occurredAt": "2026-03-14T10:30:00Z",
  "userId": 42,
  "email": "test@example.com",
  "tokenId": "tok_abc123",
  "correlationId": "corr_xyz789"
}
```

| Champ | Utilité |
|-------|---------|
| `eventId` | Identifie de manière unique chaque événement. Permet de détecter les doublons. |
| `occurredAt` | Horodatage précis pour l'audit et le debugging. |
| `correlationId` | Permet de tracer une requête de bout en bout, du client jusqu'à l'e-mail envoyé. Transmis en header RabbitMQ (`x-correlation-id`). |
| `x-schema-version` | Header RabbitMQ indiquant la version du schéma JSON, pour assurer la compatibilité lors des évolutions. |

### Comparaison : avec vs sans messagerie

| | Appel synchrone (sans RabbitMQ) | Messagerie (avec RabbitMQ) |
|---|---|---|
| **Couplage** | Auth dépend de Notification (appel HTTP direct) | Aucune dépendance directe |
| **Latence** | Le client attend l'envoi de l'e-mail | Réponse immédiate, e-mail en arrière-plan |
| **Panne SMTP** | Inscription échoue ou timeout | Message en attente dans la queue |
| **Scalabilité** | Limitée au throughput SMTP | N consommateurs en parallèle |
| **Nouveaux consommateurs** | Modifier Auth pour chaque ajout | Ajouter un binding, zéro changement dans Auth |

---

## Rôles des composants / services

### Service Auth (port 8080)

Le service principal de l'application. Il gère l'identité des utilisateurs et orchestre le flux de vérification.

| Responsabilité | Détail | Fichier(s) |
|----------------|--------|------------|
| **POST /api/auth/register** | Crée l'utilisateur (`verified=false`), génère le token, publie l'événement | `AuthController.java`, `UserService.java` |
| **GET /api/auth/verify** | Valide le token (BCrypt), marque `verified=true`, supprime le token | `AuthController.java`, `VerificationService.java` |
| **Persistance utilisateurs** | Table `tbl_users` avec champ `verified` | `User.java`, `UserRepository.java` |
| **Persistance tokens** | Table `tbl_verification_tokens` — stocke uniquement le hash BCrypt, jamais le token clair | `VerificationToken.java`, `VerificationTokenRepository.java` |
| **Publication d'événements** | Publie `UserRegistered` sur l'exchange `auth.events` (routing key `auth.user-registered`) | `EventPublisher.java`, `UserRegisteredEvent.java` |
| **Publication EmailVerified** | Publie `EmailVerified` après vérification réussie (routing key `auth.email-verified`) | `EventPublisher.java`, `EmailVerifiedEvent.java` |

### Service Notification (port 8081)

Microservice dédié à l'envoi de notifications. Il ne connaît pas Auth directement — il consomme uniquement les événements RabbitMQ.

| Responsabilité | Détail | Fichier(s) |
|----------------|--------|------------|
| **Consommation des événements** | `@RabbitListener` sur la queue `notification.user-registered`, désérialise le JSON en `UserRegisteredEvent` | `UserRegisteredListener.java` |
| **Construction du lien** | Assemble l'URL de vérification : `{authBaseUrl}/api/auth/verify?tokenId=...&t=...` | `EmailService.java` |
| **Envoi d'e-mail** | Envoie un `SimpleMailMessage` via `JavaMailSender` vers le SMTP MailHog | `EmailService.java` |
| **Gestion des erreurs** | Si une exception est levée, le message est rejeté et redirigé vers la DLQ par RabbitMQ (retry automatique, puis dead-letter) | Configuration Spring AMQP + `RabbitMQConfig.java` |

### RabbitMQ (ports 5672 / 15672)

Le broker de messages qui assure le découplage entre Auth et Notification.

| Composant | Type | Rôle |
|-----------|------|------|
| `auth.events` | Exchange (topic) | Reçoit tous les événements émis par Auth. Le type **topic** permet un routage flexible par pattern de routing key. |
| `notification.user-registered` | Queue (durable) | Stocke les messages destinés à Notification. Bound à l'exchange via la routing key `auth.user-registered`. |
| `auth.events.dlx` | Exchange (dead-letter) | Reçoit les messages rejetés ou échoués depuis la queue principale. |
| `notification.user-registered.dlq` | Queue (dead-letter) | Stocke les messages en erreur pour analyse manuelle. Accessible via la console RabbitMQ (http://localhost:15672). |
| `analytics.email-verified` | Queue (durable) | Stocke les événements `EmailVerified` pour le consumer Analytics. Bound via la routing key `auth.email-verified`. |

```
auth.events (topic exchange)
    │
    ├── routing key: auth.user-registered
    │       → notification.user-registered (queue)
    │               │ (en cas d'échec)
    │               └── → auth.events.dlx (DLX)
    │                         └── → notification.user-registered.dlq (DLQ)
    │
    └── routing key: auth.email-verified
            → analytics.email-verified (queue)
                    → EmailVerifiedListener (compteur)
```

### MailHog (ports 1025 / 8025)

Serveur SMTP de **développement uniquement** qui capture tous les e-mails sans jamais les envoyer à l'extérieur.

| Port | Usage |
|------|-------|
| **1025** | Port SMTP — le Notification Service y envoie les e-mails (`spring.mail.host=mailhog`, `spring.mail.port=1025`) |
| **8025** | Interface web — boîte de réception pour visualiser les e-mails reçus (http://localhost:8025) |

Avantages :
- **Zéro configuration** : pas besoin de compte Gmail ou SendGrid pour les tests.
- **Pas d'envoi réel** : aucun risque d'envoyer des e-mails de test à de vrais utilisateurs.
- **API REST** : `GET http://localhost:8025/api/v2/messages` permet de vérifier les e-mails programmatiquement (utile pour les tests automatisés).

---

## Spécifications des événements

### UserRegistered

Publié par Auth Service lors de chaque inscription (`POST /api/auth/register`).

```json
{
  "type": "UserRegistered",
  "eventId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "occurredAt": "2026-03-14T10:30:00Z",
  "userId": "42",
  "email": "test@example.com",
  "tokenId": "tok_abc123def456",
  "tokenClear": "854d4b66-d59b-456a-b6ad-cec7cb85c860",
  "correlationId": "7c9e6679-7425-40de-944b-e07fc1f90ae7"
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `type` | String | Toujours `"UserRegistered"` |
| `eventId` | UUID | Identifiant unique de l'événement (détection de doublons) |
| `occurredAt` | Instant (ISO 8601) | Horodatage de l'événement |
| `userId` | String | ID de l'utilisateur créé |
| `email` | String | Adresse e-mail de l'utilisateur |
| `tokenId` | String | Identifiant court du token (préfixé `tok_`), sert de clé primaire en BDD |
| `tokenClear` | String | Token clair (UUID) — inclus pour que Notification puisse construire le lien de vérification. **Ne jamais persister ce champ.** |
| `correlationId` | UUID | Identifiant de corrélation pour tracer la requête de bout en bout |

**Fichier DTO** : `com.bhak.project.dto.UserRegisteredEvent` (Auth) / `com.bhak.notification.dto.UserRegisteredEvent` (Notification)

### EmailVerified

Publié par Auth Service après une vérification réussie du token (`GET /api/auth/verify`).

```json
{
  "type": "EmailVerified",
  "eventId": "9a1b2c3d-4e5f-6789-abcd-ef0123456789",
  "occurredAt": "2026-03-14T10:35:00Z",
  "userId": "42",
  "email": "test@example.com",
  "correlationId": "d4e5f678-9abc-def0-1234-567890abcdef"
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `type` | String | Toujours `"EmailVerified"` |
| `eventId` | UUID | Identifiant unique de l'événement |
| `occurredAt` | Instant (ISO 8601) | Horodatage de la vérification |
| `userId` | String | ID de l'utilisateur vérifié |
| `email` | String | Adresse e-mail vérifiée |
| `correlationId` | UUID | Identifiant de corrélation |

**Fichier DTO** : `com.bhak.project.dto.EmailVerifiedEvent` (Auth) / `com.bhak.notification.dto.EmailVerifiedEvent` (Notification)

### En-têtes RabbitMQ

Chaque message publié inclut des en-têtes AMQP définis dans `EventPublisher.java` :

| En-tête | Valeur | Rôle |
|---------|--------|------|
| `x-correlation-id` | UUID (ex. `7c9e6679-...`) | Permet de corréler un message avec la requête HTTP d'origine. Utile pour le debugging et l'audit distribué. |
| `x-schema-version` | `1` (entier) | Indique la version du schéma JSON du message. Permet aux consommateurs de gérer la compatibilité ascendante lors d'évolutions du format. |

```java
// EventPublisher.java — mise en place des en-têtes
rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> {
    message.getMessageProperties().setHeader("x-correlation-id", event.getCorrelationId());
    message.getMessageProperties().setHeader("x-schema-version", 1);
    return message;
});
```

### Routing keys

| Événement | Exchange | Routing key | Queue cible |
|-----------|----------|-------------|-------------|
| `UserRegistered` | `auth.events` | `auth.user-registered` | `notification.user-registered` |
| `EmailVerified` | `auth.events` | `auth.email-verified` | `analytics.email-verified` |

---

## Contrats d'API (Auth)

### POST /api/auth/register

Inscription d'un nouveau compte. Crée l'utilisateur, génère un token de vérification et publie l'événement `UserRegistered`.

**Requête :**

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "pass123",
  "phoneNumber": "+33612345678",   // optionnel
  "roleType": "USER"               // optionnel, défaut: USER
}
```

| Champ | Obligatoire | Description |
|-------|:-----------:|-------------|
| `username` | oui | Pseudo unique |
| `email` | oui | Adresse e-mail unique |
| `password` | oui | Mot de passe (hashé BCrypt avant stockage) |
| `phoneNumber` | non | Numéro de téléphone unique (si fourni) |
| `roleType` | non | `USER` (défaut) ou `ADMIN` |

**Réponses :**

| Code | Statut | Description |
|------|--------|-------------|
| **201** | Created | Compte créé. Corps = objet User (avec `verified: false`) |
| **400** | Bad Request | Champs obligatoires manquants |
| **409** | Conflict | Pseudo, e-mail ou téléphone déjà utilisé |

**Effets de bord :**

1. Crée l'utilisateur en base (`verified = false`)
2. Crée un `VerificationToken` (hash BCrypt stocké, expire dans 30 min)
3. Publie `UserRegistered` sur RabbitMQ → déclenche l'envoi d'e-mail

**Exemple de réponse 201 :**

```json
{
  "id": 1,
  "username": "testuser",
  "verified": false,
  "role": { "id": 1, "name": "USER" },
  "credentials": {
    "email": "test@example.com",
    "phoneNumber": null
  }
}
```

**Test curl :**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"pass123"}'
```

---

### GET /api/auth/verify

Vérifie l'adresse e-mail via le lien reçu par l'utilisateur. Token à usage unique (one-shot).

**Requête :**

```http
GET /api/auth/verify?tokenId=tok_abc123def456&t=854d4b66-d59b-456a-b6ad-cec7cb85c860
```

| Paramètre | Type | Description |
|-----------|------|-------------|
| `tokenId` | query string | Identifiant du token (préfixé `tok_`) |
| `t` | query string | Token clair (UUID) reçu dans le lien e-mail |

**Réponses :**

| Code | Statut | Description |
|------|--------|-------------|
| **200** | OK | Compte vérifié avec succès |
| **400** | Bad Request | Token invalide, expiré ou déjà utilisé |

**Effets de bord :**

1. Compare le token clair avec le hash BCrypt stocké
2. Vérifie que le token n'a pas expiré (TTL 30 min)
3. Marque `verified = true` sur l'utilisateur
4. Supprime le token (one-shot, usage unique)
5. Publie `EmailVerified` sur RabbitMQ
6. **Idempotent** : si le compte est déjà vérifié, aucune erreur

**Exemple de réponse 200 :**

```json
{
  "message": "Compte vérifié avec succès"
}
```

**Exemple de réponse 400 (token expiré) :**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Le token de verification a expire"
}
```

---

## Modèle de données

### User (`tbl_users`)

```
┌─────────────────────────────────────┐
│              tbl_users              │
├──────────────┬──────────────────────┤
│ id           │ BIGINT (PK, auto)    │
│ username     │ VARCHAR (unique)     │
│ verified     │ BOOLEAN (def. false) │
│ role_id      │ BIGINT (FK → roles)  │
└──────────────┴──────────────────────┘
         │ 1:1
         ▼
┌─────────────────────────────────────┐
│          tbl_credentials            │
├──────────────┬──────────────────────┤
│ id           │ BIGINT (PK, auto)    │
│ email        │ VARCHAR (unique)     │
│ phone_number │ VARCHAR (unique)     │
│ password     │ VARCHAR (BCrypt)     │
│ user_id      │ BIGINT (FK → users)  │
└──────────────┴──────────────────────┘
```

Le champ `verified` (ajouté pour le TP) est initialisé à `false` lors de l'inscription. Il passe à `true` uniquement après validation du token via `GET /api/auth/verify`.

**Fichiers** : `User.java`, `Credentials.java`, `UserRepository.java`, `CredentialsRepository.java`

### VerificationToken (`tbl_verification_tokens`)

```
┌──────────────────────────────────────────┐
│       tbl_verification_tokens            │
├──────────────┬───────────────────────────┤
│ token_id     │ VARCHAR(64) (PK)          │
│ user_id      │ BIGINT                    │
│ token_hash   │ VARCHAR (BCrypt)          │
│ expires_at   │ TIMESTAMP                 │
└──────────────┴───────────────────────────┘
```

| Colonne | Description |
|---------|-------------|
| `token_id` | Identifiant unique préfixé `tok_` (ex. `tok_abc123def456`). Sert de clé primaire et est transmis dans le lien de vérification. |
| `user_id` | Référence vers l'utilisateur. Pas de FK JPA (simple Long) pour découpler du cycle de vie de l'entité User. |
| `token_hash` | Hash BCrypt du token clair. **Le token clair n'est jamais stocké en base.** |
| `expires_at` | Date d'expiration (30 min après création, configurable via `app.verification.token-ttl-minutes`). |

**Cycle de vie** : créé à l'inscription → utilisé une seule fois lors de la vérification → supprimé (one-shot).

**Fichiers** : `VerificationToken.java`, `VerificationTokenRepository.java`

---

## Informations de configuration

### Docker Compose

Le fichier `docker-compose.yml` orchestre toute l'infrastructure locale :

| Service | Image | Ports | Rôle |
|---------|-------|-------|------|
| **postgres** | `postgres:16-alpine` | `5432:5432` | Base de données Auth |
| **rabbitmq** | `rabbitmq:3.13-management-alpine` | `5672:5672` (AMQP) / `15672:15672` (UI) | Broker de messages |
| **mailhog** | `mailhog/mailhog:latest` | `1025:1025` (SMTP) / `8025:8025` (UI) | Serveur SMTP de test |
| **notification** | Build `../notification-service` | `8081:8081` | Microservice Notification |

```bash
# Démarrer toute l'infra
docker compose up -d postgres rabbitmq mailhog
```

**Fichier** : `docker-compose.yml`

### Dépendances Spring Boot

#### Auth Service (`pom.xml`)

| Dépendance | Usage |
|------------|-------|
| `spring-boot-starter-web` | REST controllers, serveur Tomcat |
| `spring-boot-starter-data-jpa` | Persistance JPA/Hibernate |
| `spring-boot-starter-amqp` | Client RabbitMQ (publication d'événements) |
| `spring-boot-starter-security` | JWT, BCrypt, filtres de sécurité |
| `postgresql` (driver) | Connexion PostgreSQL (pas H2 — on utilise une vraie BDD) |
| `lombok` | Réduction du boilerplate |
| `springdoc-openapi` | Documentation Swagger UI |

#### Notification Service (`pom.xml`)

| Dépendance | Usage |
|------------|-------|
| `spring-boot-starter-web` | Serveur sur port 8081 |
| `spring-boot-starter-amqp` | Client RabbitMQ (consommation d'événements) |
| `spring-boot-starter-mail` | Envoi d'e-mails via `JavaMailSender` |
| `lombok` | Réduction du boilerplate |

### Configuration RabbitMQ (`application.properties`)

#### Auth Service

```properties
# Connexion
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Messaging
app.mq.exchange=auth.events
app.mq.rk.userRegistered=auth.user-registered
app.mq.rk.emailVerified=auth.email-verified
```

#### Topologie déclarée (`RabbitMQConfig.java` — Auth & Notification)

| Composant | Nom | Type |
|-----------|-----|------|
| Exchange principal | `auth.events` | Topic |
| Queue inscription | `notification.user-registered` | Durable, DLX configuré |
| Queue analytics | `analytics.email-verified` | Durable |
| DLX | `auth.events.dlx` | Topic |
| DLQ | `notification.user-registered.dlq` | Durable |
| Binding 1 | `auth.user-registered` → queue inscription | Routing key |
| Binding 2 | `auth.email-verified` → queue analytics | Routing key |
| Binding DLQ | `dlq.notification.user-registered` → DLQ | Routing key |

### Configuration SMTP (`application.properties` — Notification)

```properties
# SMTP vers MailHog (pas de TLS, pas d'auth en local)
spring.mail.host=localhost
spring.mail.port=1025
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false
```

> **Note** : en production, on remplacerait MailHog par un vrai serveur SMTP (SendGrid, SES…) avec TLS et authentification.

### Autres configurations (Auth)

```properties
# Token de vérification
app.verification.token-ttl-minutes=30
app.verification.base-url=http://localhost:8080

# JWT
app.jwt.secret-key=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
app.jwt.expiration-time=86400000
```

---

## Vérification de conformité — Séance 1

Les 7 étapes demandées par le TP sont toutes implémentées :

| # | Étape TP | Statut | Fichiers | Notes |
|---|----------|:------:|----------|-------|
| 1 | POST /register : persister user (verified=false) | ✅ | `AuthController.register()`, `User.java` | `verified` défaut `false` via Lombok |
| 2 | Générer UUID token, stocker BCrypt(token) + expiresAt, conserver tokenId | ✅ | `VerificationService.createTokenAndPublishEvent()` | `tokenId` préfixé `tok_`, hash BCrypt, TTL 30 min |
| 3 | Publier UserRegistered sur `auth.events` avec key `auth.user-registered` | ✅ | `EventPublisher.publishUserRegistered()` | + headers `x-correlation-id`, `x-schema-version` |
| 4 | Notification : déclarer exchange, queue, DLQ ; consommer l'événement | ✅ | `RabbitMQConfig.java` (Notification), `UserRegisteredListener.java` | Exchange, queue, DLX, DLQ tous déclarés |
| 5 | Construire le lien de vérification, envoyer l'e-mail via MailHog | ✅ | `EmailService.sendVerificationEmail()` | URL : `/api/auth/verify` (au lieu de `/verify` dans le TP — cohérent avec le prefix `/api/auth`) |
| 6 | Tester : POST /register → vérifier e-mail dans MailHog | ✅ | — | Testé avec curl, e-mail reçu confirmé via API MailHog |
| 7 | GET /verify : expiration, BCrypt compare, verified=true, supprimer token | ✅ | `AuthController.verify()`, `VerificationService.verify()` | One-shot + idempotent + publie EmailVerified |

---

## Étapes réalisées — Séance 1

### 1. Infrastructure (docker-compose.yml)

Ajout de trois services à côté de PostgreSQL :

| Service    | Image                              | Ports          |
|------------|------------------------------------|----------------|
| RabbitMQ   | `rabbitmq:3.13-management-alpine`  | 5672 / 15672   |
| MailHog    | `mailhog/mailhog:latest`           | 1025 / 8025    |
| Notification | Build `../notification-service`  | 8081           |

Commande pour démarrer l'infra :
```bash
docker compose up -d postgres rabbitmq mailhog
```

### 2. Dépendance AMQP (Auth — pom.xml)

Ajout de `spring-boot-starter-amqp` pour permettre à l'Auth Service de publier des messages dans RabbitMQ.

### 3. Champ `verified` sur l'entité User

Ajout d'un booléen `verified` (défaut `false`) dans `tbl_users`. Le compte passe à `true` uniquement après validation du token.

### 4. Entité VerificationToken

Nouvelle table `tbl_verification_tokens` :

| Colonne     | Type            | Description                        |
|-------------|-----------------|------------------------------------|
| token_id    | VARCHAR(64) PK  | Identifiant unique du token        |
| user_id     | BIGINT          | Référence vers le compte           |
| token_hash  | VARCHAR         | Hash BCrypt du token clair         |
| expires_at  | TIMESTAMP       | Date d'expiration (30 min par défaut) |

**Principe de sécurité** : le token clair n'est jamais stocké en base. Seul le hash BCrypt est persisté. Lors de la vérification, on compare `BCrypt(tokenReçu)` avec le hash stocké.

### 5. Configuration RabbitMQ (Auth)

Fichier `RabbitMQConfig.java` — déclare :
- **Exchange** `auth.events` (topic)
- **Queue** `notification.user-registered` avec redirection vers DLX en cas d'échec
- **DLX** `auth.events.dlx` + **DLQ** `notification.user-registered.dlq`
- Convertisseur JSON (Jackson) pour sérialiser les messages

### 6. Événement UserRegistered + Publisher

- DTO `UserRegisteredEvent` : type, eventId, occurredAt, userId, email, tokenId, tokenClear, correlationId
- Service `EventPublisher` : publie l'événement sur l'exchange `auth.events` avec la routing key `auth.user-registered`
- En-têtes ajoutés : `x-correlation-id`, `x-schema-version`

### 7. Modification du flux d'inscription

Dans `AuthController.register()` :
1. Création du user (verified=false) — comme avant
2. **Nouveau** : appel à `VerificationService.createTokenAndPublishEvent(user)`
   - Génère un UUID comme token clair
   - Stocke le hash BCrypt + expiration en base
   - Publie `UserRegistered` dans RabbitMQ

### 8. Endpoint GET /api/auth/verify

Nouveau endpoint accessible sans authentification :
- Reçoit `tokenId` et `t` (token clair) en paramètres
- Vérifie l'expiration du token
- Compare le token clair avec le hash BCrypt stocké
- Marque le compte `verified=true`
- Supprime le token (one-shot, usage unique)
- Idempotent : si le compte est déjà vérifié, aucune erreur

### 9. Notification Service (nouveau microservice)

Projet Spring Boot séparé (`notification-service/`) avec :

| Composant                  | Rôle                                                    |
|----------------------------|---------------------------------------------------------|
| `RabbitMQConfig`           | Déclare les mêmes files/exchanges (miroir du Auth)       |
| `UserRegisteredListener`   | `@RabbitListener` qui consomme les événements            |
| `EmailService`             | Construit le lien de vérification, envoie l'e-mail       |

L'e-mail envoyé contient un lien du type :
```
http://localhost:8080/api/auth/verify?tokenId=tok_abc123&t=uuid-clair
```

---

## Comment tester

```bash
# 1. Démarrer l'infra
docker compose up -d postgres rabbitmq mailhog

# 2. Lancer le service Auth (port 8080)
cd intro_to_software_architecture_BHAK
./mvnw spring-boot:run

# 3. Lancer le service Notification (port 8081)
cd notification-service
mvn spring-boot:run

# 4. Inscrire un utilisateur
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"pass123"}'

# 5. Vérifier l'e-mail reçu dans MailHog
#    → http://localhost:8025

# 6. Cliquer sur le lien de vérification dans l'e-mail
#    → Le compte passe à verified=true

# 7. (Optionnel) Vérifier la console RabbitMQ
#    → http://localhost:15672 (guest / guest)
```

---

## Vérification de conformité — Séance 2 (Futur)

### Étape 8 : Idempotence — ✅ Implémenté

Le TP demande que la vérification **ne pas échouer** si le compte est déjà vérifié.

**Implémentation** dans `VerificationService.verify()` :

```java
if (user.isVerified()) {
    // idempotent : le compte est deja verifie
    tokenRepository.delete(vt);
    log.info("Compte #{} deja verifie (idempotent)", user.getId());
    return;
}
```

Si le compte est déjà `verified=true`, le token est supprimé et la méthode retourne sans erreur. Le client reçoit un 200 OK dans les deux cas.

### Étape 9 : DLQ — ✅ Configuré, test documenté ci-dessous

La DLQ est déjà configurée dans `RabbitMQConfig.java` (Auth + Notification) :
- Queue principale `notification.user-registered` avec argument `x-dead-letter-exchange = auth.events.dlx`
- DLX `auth.events.dlx` (topic exchange)
- DLQ `notification.user-registered.dlq` bound au DLX

**Comment simuler une erreur pour observer le message en DLQ :**

**Méthode 1 — Ajouter une exception temporaire dans le listener :**

```java
// UserRegisteredListener.java — ajouter temporairement cette ligne
@RabbitListener(queues = "notification.user-registered")
public void onUserRegistered(UserRegisteredEvent event) {
    throw new RuntimeException("Erreur simulée pour tester la DLQ");
}
```

**Méthode 2 — Stopper MailHog avant de s'inscrire :**

```bash
# 1. Arrêter MailHog
docker compose stop mailhog

# 2. S'inscrire (l'envoi d'e-mail échouera)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"dlqtest","email":"dlq@test.com","password":"pass123"}'

# 3. Observer le message dans la DLQ via la console RabbitMQ
#    → http://localhost:15672 → Queues → notification.user-registered.dlq
#    Le message rejeté apparaît dans cette queue

# 4. Relancer MailHog
docker compose start mailhog
```

**Vérification dans la console RabbitMQ (http://localhost:15672) :**
1. Aller dans l'onglet **Queues**
2. La queue `notification.user-registered.dlq` doit contenir 1 message
3. Cliquer sur la queue → **Get messages** pour voir le contenu JSON du message échoué

### Étape 10 : EmailVerified + Analytics — ✅ Implémenté

Après vérification réussie, Auth publie `EmailVerified` sur `auth.events` (routing key `auth.email-verified`). Le consumer Analytics dans Notification incrémente un compteur :

```java
// EmailVerifiedListener.java
@RabbitListener(queues = "analytics.email-verified")
public void onEmailVerified(EmailVerifiedEvent event) {
    long count = verifiedCount.incrementAndGet();
    log.info("[Analytics] EmailVerified recu — Total verifications: {}", count);
}
```

**Fichiers** : `EmailVerifiedEvent.java` (Auth + Notification), `EventPublisher.publishEmailVerified()`, `EmailVerifiedListener.java`

### Résumé de conformité

| # | Étape TP | Statut | Notes |
|---|----------|:------:|-------|
| 8 | Idempotence | ✅ | `VerificationService.verify()` — retourne sans erreur si déjà vérifié |
| 9 | DLQ + simulation erreur | ✅ config / 📋 test | DLQ configurée, procédure de test documentée |
| 10 | EmailVerified + compteur Analytics | ✅ | `EmailVerifiedListener` avec `AtomicLong` |

---

## Événement EmailVerified (implémenté)

Après une vérification réussie du token (`GET /api/auth/verify`), Auth Service publie un événement `EmailVerified` sur le même exchange `auth.events` avec la routing key `auth.email-verified`.

### Flux

```
GET /verify (succès)
    │
    ▼
VerificationService.verify()
    ├── marque verified=true
    ├── supprime le token
    └── publie EmailVerified
            │
            ▼
    auth.events (exchange)
            │ routing key: auth.email-verified
            ▼
    analytics.email-verified (queue)
            │
            ▼
    EmailVerifiedListener (Notification Service)
            └── incrémente un compteur AtomicLong
                log: "[Analytics] EmailVerified recu — Total verifications: N"
```

### Fichiers créés / modifiés

| Fichier | Service | Changement |
|---------|---------|------------|
| `EmailVerifiedEvent.java` | Auth (dto) | Nouveau DTO : type, eventId, occurredAt, userId, email, correlationId |
| `EventPublisher.java` | Auth (service) | Ajout de `publishEmailVerified()` |
| `VerificationService.java` | Auth (service) | Publie `EmailVerified` après vérification réussie |
| `RabbitMQConfig.java` | Auth + Notification | Ajout queue `analytics.email-verified` + binding |
| `EmailVerifiedEvent.java` | Notification (dto) | DTO miroir pour désérialisation |
| `EmailVerifiedListener.java` | Notification (listener) | `@RabbitListener` avec compteur `AtomicLong` |

---

## Critères d'évaluation

### 1. Flux fonctionnel complet (40%) — ✅

| Étape | Statut | Preuve |
|-------|:------:|--------|
| POST /register crée user (verified=false) | ✅ | curl testé, réponse 201 avec `verified: false` |
| Token généré, hash stocké, événement publié | ✅ | `VerificationService.createTokenAndPublishEvent()` |
| RabbitMQ route vers Notification | ✅ | Message reçu dans queue `notification.user-registered` |
| E-mail envoyé via MailHog | ✅ | Vérifié via API MailHog (`/api/v2/messages`) |
| Lien de vérification fonctionnel | ✅ | URL correcte dans l'e-mail |
| GET /verify marque verified=true | ✅ | `VerificationService.verify()` |

### 2. Sécurité minimale (25%) — ✅

| Exigence | Statut | Implémentation |
|----------|:------:|----------------|
| Token non stocké en clair | ✅ | `passwordEncoder.encode(tokenClear)` — seul le hash BCrypt est en BDD |
| Expiration respectée | ✅ | TTL 30 min, vérifié dans `verify()` avec `vt.getExpiresAt().isBefore(LocalDateTime.now())` |
| Token one-shot (supprimé après usage) | ✅ | `tokenRepository.delete(vt)` après vérification réussie |

### 3. Messagerie (20%) — ✅

| Exigence | Statut | Implémentation |
|----------|:------:|----------------|
| Exchange topic `auth.events` | ✅ | `RabbitMQConfig.java` |
| Queue `notification.user-registered` | ✅ | Durable, bound via `auth.user-registered` |
| DLX `auth.events.dlx` | ✅ | Topic exchange pour messages rejetés |
| DLQ `notification.user-registered.dlq` | ✅ | Bound au DLX |
| DLQ opérationnelle | ✅ | Configurée, procédure de test documentée (arrêt MailHog ou exception simulée) |
| Convertisseur JSON | ✅ | `Jackson2JsonMessageConverter` |
| Headers (`x-correlation-id`, `x-schema-version`) | ✅ | `EventPublisher` |

### 4. Qualité (15%) — ✅

| Exigence | Statut | Détail |
|----------|:------:|--------|
| Logs clairs | ✅ | `@Slf4j` sur tous les composants clés, messages avec eventId, userId, correlationId |
| README succinct | ✅ | `README.md` mis à jour avec email verification, RabbitMQ, Notification, structure, commandes |
| Tests manuels reproductibles | ✅ | Commandes curl + vérification MailHog documentées dans `messagerie.md` et `README.md` |

---

## 14. Conseils & bonnes pratiques

Cette section résume les bonnes pratiques recommandées par le TP et leur application dans notre projet.

### Tableau de conformité

| Bonne pratique | Statut | Implémentation |
|----------------|:------:|----------------|
| **Minimiser les PII** dans les événements | ✅ | L'email est inclus car nécessaire au service Notification pour envoyer le mail. Le `tokenClear` est transmis uniquement dans l'événement (jamais persisté en clair). Aucune donnée inutile n'est ajoutée. |
| **Identifiants stables** (`userId`) comme clés de corrélation | ✅ | `userId` présent dans tous les événements (`UserRegisteredEvent`, `EmailVerifiedEvent`). Routing keys thématiques (`auth.user-registered`, `auth.email-verified`) sur topic exchange. |
| **eventId (UUID)** pour ignorer les doublons côté consommateurs | ✅ | `UUID.randomUUID()` attribué dans `VerificationService` pour chaque événement publié. Permet la déduplication côté consumer. |
| **TTL pour les tokens** (15–30 min) | ✅ | `app.verification.token-ttl-minutes=30` — vérifié à la validation dans `VerificationService.verify()`. Token expiré → supprimé + exception. |
| **Journaliser les correlationId** pour traçabilité bout en bout | ✅ | `correlationId` loggé dans `VerificationService` (création + vérification), `EventPublisher` (publication), `UserRegisteredListener` (réception), `EmailVerifiedListener` (analytics). |

### Détail des pratiques

#### Minimisation des PII

- Les événements ne contiennent que les champs strictement nécessaires : `userId`, `email`, `tokenId`, `tokenClear`
- Le `tokenClear` est marqué comme **optionnel** par le TP (flux simple) — il est transmis dans l'événement mais **jamais stocké en clair** (seul le hash BCrypt est persisté en base)
- L'Annexe B du TP montre un email masqué (`a***@ex.com`) à titre d'exemple ; dans notre implémentation l'email complet est nécessaire pour l'envoi du mail de vérification

#### Identifiants stables

```
Routing keys (topic exchange) :
  auth.user-registered  →  queue notification.user-registered
  auth.email-verified   →  queue analytics.email-verified

Corrélation dans chaque événement :
  userId        → identifie l'utilisateur
  correlationId → trace le flux de bout en bout
  eventId       → identifie l'événement de façon unique
```

#### Déduplication par eventId

Chaque événement publié reçoit un `eventId` UUID unique. Un consommateur peut stocker les `eventId` traités pour ignorer les doublons en cas de redelivery.

#### TTL des tokens

```properties
# application.properties
app.verification.token-ttl-minutes=30
```

Vérification dans `VerificationService.verify()` :
```java
if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
    tokenRepository.delete(vt);
    throw new InvalidRequestException("Le token de verification a expire");
}
```

#### Traçabilité correlationId

Flux de logs pour une inscription complète :
```
[Auth]         Token de verification cree [tokenId=tok_xxx, userId=1, correlationId=abc-123]
[Auth]         Publication UserRegistered [eventId=evt-1, userId=1, correlationId=abc-123]
[Notification] Evenement recu: UserRegistered [eventId=evt-1, userId=1, correlationId=abc-123]
[Notification] Email de verification envoye a user@example.com
...
[Auth]         Compte #1 verifie avec succes [tokenId=tok_xxx, correlationId=def-456]
[Auth]         Publication EmailVerified [eventId=evt-2, userId=1, correlationId=def-456]
[Notification] [Analytics] EmailVerified recu [eventId=evt-2, userId=1, correlationId=def-456]
```

---

## Annexe A — Configuration (comparaison YAML du TP vs notre implémentation)

Le TP fournit un exemple en YAML. Notre projet utilise `application.properties` (équivalent).

### Auth Service (`application.properties`)

| Propriété TP (YAML) | Notre configuration | Statut |
|----------------------|---------------------|:------:|
| `spring.rabbitmq.host: localhost` | `spring.rabbitmq.host=localhost` | ✅ |
| `spring.rabbitmq.port: 5672` | `spring.rabbitmq.port=5672` | ✅ |
| `spring.rabbitmq.username: guest` | `spring.rabbitmq.username=guest` | ✅ |
| `spring.rabbitmq.password: guest` | `spring.rabbitmq.password=guest` | ✅ |
| `app.mq.exchange: auth.events` | `app.mq.exchange=auth.events` | ✅ |
| `app.mq.rk.userRegistered: auth.user-registered` | `app.mq.rk.userRegistered=auth.user-registered` | ✅ |
| `app.mq.rk.emailVerified: auth.email-verified` | `app.mq.rk.emailVerified=auth.email-verified` | ✅ |

### Notification Service (`application.properties`)

| Propriété TP (YAML) | Notre configuration | Statut |
|----------------------|---------------------|:------:|
| `spring.mail.host: localhost` | `spring.mail.host=localhost` | ✅ |
| `spring.mail.port: 1025` | `spring.mail.port=1025` | ✅ |
| `spring.mail.properties.mail.smtp.auth: false` | `spring.mail.properties.mail.smtp.auth=false` | ✅ |
| `spring.mail.properties.mail.smtp.starttls.enable: false` | `spring.mail.properties.mail.smtp.starttls.enable=false` | ✅ |

---

## Annexe B — Schéma des événements (comparaison JSON)

### Structure du TP (Annexe B)

```json
{
  "type": "UserRegistered",
  "eventId": "uuid",
  "occurredAt": "2026-02-15T10:00:00Z",
  "data": {
    "userId": "u_123",
    "email": "a***@ex.com",
    "tokenId": "tok_abc",
    "tokenClear": "OPTIONNEL (flux simple TP)"
  },
  "headers": {
    "x-correlation-id": "uuid",
    "x-schema-version": 1
  }
}
```

### Notre implémentation

```json
{
  "type": "UserRegistered",
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "occurredAt": "2026-02-15T10:00:00Z",
  "userId": "1",
  "email": "user@example.com",
  "tokenId": "tok_a1b2c3d4e5f6",
  "tokenClear": "a1b2c3d4-e5f6-...",
  "correlationId": "660e8400-e29b-41d4-a716-446655440001"
}
```

**Headers AMQP** (séparés du body JSON) :
```
x-correlation-id: 660e8400-e29b-41d4-a716-446655440001
x-schema-version: 1
```

### Différences et justification

| Aspect | TP (Annexe B) | Notre implémentation | Justification |
|--------|---------------|----------------------|---------------|
| Structure | Imbriquée (`data: {}`) | Plate (champs au même niveau) | Sérialisation plus simple, compatible Jackson |
| Headers | Dans le JSON (`headers: {}`) | AMQP message properties | **Meilleure pratique** : les headers de routage/corrélation sont dans les propriétés du message AMQP, pas dans le payload |
| Email | Masqué (`a***@ex.com`) | Complet | Nécessaire pour l'envoi du mail par le service Notification |
| correlationId | Dans `headers` | Champ du body + header AMQP | Double disponibilité : dans le body pour la sérialisation, dans les headers AMQP pour le routage/traçage |
