# Guide d'utilisation — Projet BHAK (Architecture Logicielle)

> Guide complet de A à Z : démarrage, authentification, vérification d'e-mail, gestion des utilisateurs, outils et dépannage.

---

## Table des matières

1. [Prérequis](#1-prérequis)
2. [Démarrage du stack avec Docker](#2-démarrage-du-stack-avec-docker)
3. [Services et URLs](#3-services-et-urls)
4. [Comptes par défaut](#4-comptes-par-défaut)
5. [Endpoint racine (Health Check)](#5-endpoint-racine-health-check)
6. [Authentification](#6-authentification)
   - [Inscription](#61-inscription)
   - [Connexion](#62-connexion)
   - [Utiliser le token JWT](#63-utiliser-le-token-jwt)
7. [Vérification d'e-mail](#7-vérification-de-mail)
8. [Gestion des utilisateurs (ADMIN)](#8-gestion-des-utilisateurs-admin)
   - [Lister tous les utilisateurs](#81-lister-tous-les-utilisateurs-paginé)
   - [Récupérer un utilisateur par ID](#82-récupérer-un-utilisateur-par-id)
   - [Créer un utilisateur](#83-créer-un-utilisateur)
   - [Modifier un utilisateur](#84-modifier-un-utilisateur)
   - [Supprimer un utilisateur](#85-supprimer-un-utilisateur)
9. [Swagger / OpenAPI](#9-swagger--openapi)
10. [RabbitMQ Management](#10-rabbitmq-management)
11. [MailHog (e-mails de test)](#11-mailhog-e-mails-de-test)
12. [Erreurs possibles](#12-erreurs-possibles)
13. [Collection Postman](#13-collection-postman)
14. [Lancement local (sans Docker)](#14-lancement-local-sans-docker)

---

## 1. Prérequis

### Avec Docker (recommandé)

- **Docker** ≥ 20.x
- **Docker Compose** ≥ 2.x

Vérifier l'installation :

```bash
docker --version
docker compose version
```

### Sans Docker (développement local)

- **Java** 17 (JDK)
- **Maven** 3.9+ (ou utiliser le wrapper `./mvnw` inclus)
- **PostgreSQL** 16
- **RabbitMQ** 3.13 avec le plugin management activé
- **MailHog** (optionnel, pour tester les e-mails)

---

## 2. Démarrage du stack avec Docker

Depuis la racine du projet :

```bash
docker compose up --build
```

Cela démarre **5 services** :

| Service          | Image                                | Rôle                                  |
|------------------|--------------------------------------|---------------------------------------|
| `postgres`       | `postgres:16-alpine`                 | Base de données PostgreSQL            |
| `rabbitmq`       | `rabbitmq:3.13-management-alpine`    | Broker de messages (événements)       |
| `mailhog`        | `mailhog/mailhog:latest`             | Serveur SMTP local (capture d'e-mails)|
| `app`            | Build depuis `Dockerfile`            | Service Auth (API principale)         |
| `notification`   | Build depuis `../notification-service`| Service Notification (e-mails)        |

> **Attendre** que tous les services soient `healthy` avant d'utiliser l'API.
> Les logs affichent `Started ProjectApplication` quand le service est prêt.

Pour arrêter :

```bash
docker compose down
```

Pour tout supprimer (y compris les données PostgreSQL) :

```bash
docker compose down -v
```

---

## 3. Services et URLs

| Service                | URL                                        | Identifiants          |
|------------------------|--------------------------------------------|-----------------------|
| **API principale**     | `http://localhost:8080`                     | —                     |
| **Swagger UI**         | `http://localhost:8080/swagger-ui.html`     | —                     |
| **OpenAPI JSON**       | `http://localhost:8080/v3/api-docs`         | —                     |
| **RabbitMQ Management**| `http://localhost:15672`                    | `guest` / `guest`     |
| **MailHog UI**         | `http://localhost:8025`                     | —                     |
| **Service Notification**| `http://localhost:8081`                    | —                     |

Ports réseau :

| Port   | Utilisation                        |
|--------|------------------------------------|
| `8080` | API REST (Auth + Admin)            |
| `8081` | Service Notification               |
| `5432` | PostgreSQL                         |
| `5672` | RabbitMQ (AMQP)                    |
| `15672`| RabbitMQ (interface web)           |
| `1025` | MailHog (SMTP)                     |
| `8025` | MailHog (interface web)            |

---

## 4. Comptes par défaut

Au démarrage, `DataInitializer` crée automatiquement deux comptes :

| Pseudo   | Mot de passe | Rôle    | E-mail            | Téléphone       |
|----------|--------------|---------|--------------------|-----------------|
| `admin`  | `admin123`   | `ADMIN` | `admin@bhak.com`   | `+33600000001`  |
| `user1`  | `user123`    | `USER`  | `user1@bhak.com`   | `+33600000002`  |

> Ces comptes sont créés uniquement si la base est vide. Ils ne sont **pas vérifiés** par défaut (champ `verified = false`).

---

## 5. Endpoint racine (Health Check)

**Vérifier que le serveur est en ligne :**

```bash
curl http://localhost:8080/
```

**Réponse** (200 OK) :

```json
{
  "application": "Architecture Logicielle - Projet BHAK",
  "version": "2.0.0",
  "status": "UP",
  "timestamp": "2026-03-14T10:30:00.123",
  "routes": {
    "inscription": "POST /api/auth/register",
    "connexion": "POST /api/auth/login",
    "admin_users": "GET  /api/admin/users"
  }
}
```

---

## 6. Authentification

Tous les endpoints d'authentification sont **publics** (pas besoin de token JWT).

### 6.1 Inscription

Créer un nouveau compte utilisateur.

**Requête :**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "monpseudo",
    "email": "mon@email.com",
    "phoneNumber": "+33612345678",
    "password": "monmotdepasse",
    "roleType": "USER"
  }'
```

**Champs du corps de la requête :**

| Champ         | Type     | Obligatoire | Description                                      |
|---------------|----------|-------------|--------------------------------------------------|
| `username`    | `string` | ✅ Oui      | Pseudo unique (max 50 caractères)                |
| `email`       | `string` | ✅ Oui      | Adresse e-mail unique (max 100 caractères)       |
| `password`    | `string` | ✅ Oui      | Mot de passe (haché en BCrypt côté serveur)      |
| `phoneNumber` | `string` | ❌ Non      | Numéro de téléphone unique (max 20 caractères)   |
| `roleType`    | `string` | ❌ Non      | Rôle : `USER` (défaut), `ADMIN` ou `MODERATOR`  |

**Réponse** (201 Created) :

```json
{
  "id": 3,
  "username": "monpseudo",
  "verified": false,
  "role": {
    "id": 2,
    "name": "USER",
    "description": "Utilisateur"
  },
  "credentials": {
    "id": 3,
    "email": "mon@email.com",
    "phoneNumber": "+33612345678"
  },
  "createdAt": "2026-03-14T10:35:00"
}
```

> **Note :** Le mot de passe n'est **jamais** retourné dans les réponses JSON.

**Ce qui se passe en arrière-plan :**
1. L'utilisateur est créé en base avec `verified = false`
2. Un token de vérification est généré (UUID hashé en BCrypt)
3. Un événement `UserRegistered` est publié sur RabbitMQ
4. Le service Notification consomme l'événement et envoie un e-mail de vérification

**Erreurs possibles :**
- `400` — Champs obligatoires manquants
- `409` — Pseudo, e-mail ou téléphone déjà utilisé

---

### 6.2 Connexion

S'authentifier et obtenir un token JWT.

**Requête :**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Champs du corps de la requête :**

| Champ      | Type     | Obligatoire | Description        |
|------------|----------|-------------|--------------------|
| `username` | `string` | ✅ Oui      | Pseudo du compte   |
| `password` | `string` | ✅ Oui      | Mot de passe       |

**Réponse** (200 OK) :

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MTcxMDQwMDAwMCwiZXhwIjoxNzEwNDg2NDAwfQ.xxxxx",
  "type": "Bearer"
}
```

**Erreurs possibles :**
- `400` — Pseudo ou mot de passe manquant
- `401` — Identifiants invalides

---

### 6.3 Utiliser le token JWT

Après la connexion, il faut inclure le token dans le header `Authorization` de chaque requête protégée :

```
Authorization: Bearer <token>
```

**Exemple avec curl :**

```bash
# 1. Se connecter et récupérer le token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# 2. Utiliser le token pour une requête protégée
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/users
```

> **Durée de validité :** Le token JWT expire après **24 heures** (86 400 000 ms).  
> Après expiration, il faut se reconnecter via `POST /api/auth/login`.

---

## 7. Vérification d'e-mail

Après l'inscription, un e-mail de vérification est envoyé automatiquement via RabbitMQ et le Service Notification.

### Flux complet étape par étape

```
1. POST /api/auth/register        → Création du compte (verified=false)
2. Auth Service                    → Génère un token de vérification
3. Auth Service → RabbitMQ         → Publie l'événement "UserRegistered"
4. Notification Service ← RabbitMQ → Consomme l'événement
5. Notification Service → MailHog  → Envoie l'e-mail avec le lien de vérification
6. Utilisateur                     → Ouvre MailHog et clique sur le lien
7. GET /api/auth/verify            → Le compte passe à verified=true
```

### Étape 1 : Inscrivez-vous

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "pass123"
  }'
```

### Étape 2 : Ouvrir MailHog

Allez sur **http://localhost:8025** dans votre navigateur.

Vous y verrez l'e-mail de vérification envoyé à `test@example.com`. L'e-mail contient un lien de la forme :

```
http://localhost:8080/api/auth/verify?tokenId=tok_abcd1234ef56&t=550e8400-e29b-41d4-a716-446655440000
```

### Étape 3 : Cliquer sur le lien (ou l'appeler via curl)

```bash
curl "http://localhost:8080/api/auth/verify?tokenId=tok_abcd1234ef56&t=550e8400-e29b-41d4-a716-446655440000"
```

**Réponse** (200 OK) :

```json
{
  "message": "Compte vérifié avec succès"
}
```

### Détails techniques du token

| Élément       | Format                            | Stockage                                |
|---------------|-----------------------------------|-----------------------------------------|
| `tokenId`     | `tok_` + 12 caractères hex       | Stocké en clair en base (clé primaire)  |
| `t` (token)   | UUID complet                      | **Jamais stocké** — seul le hash BCrypt est en base |
| Expiration    | 30 minutes après la création      | Champ `expiresAt` en base               |

> Le token est **à usage unique** : il est supprimé de la base après vérification réussie.

**Erreurs possibles :**
- `400` — Token expiré (plus de 30 minutes)
- `400` — Token invalide (ne correspond pas au hash)

---

## 8. Gestion des utilisateurs (ADMIN)

Tous les endpoints de cette section nécessitent :
1. Un **token JWT** valide dans le header `Authorization`
2. Un compte avec le rôle **ADMIN**

> Si vous utilisez un compte `USER`, vous recevrez une erreur **403 Forbidden**.

**Récupérer le token admin :**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')
```

---

### 8.1 Lister tous les utilisateurs (paginé)

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/admin/users?page=0&size=10&sort=username"
```

**Paramètres de requête :**

| Paramètre | Type   | Défaut     | Description                                        |
|-----------|--------|------------|----------------------------------------------------|
| `page`    | `int`  | `0`        | Numéro de la page (commence à 0)                  |
| `size`    | `int`  | `10`       | Nombre d'éléments par page                         |
| `sort`    | `string`| `username`| Champ de tri (ex: `username`, `id`, `createdAt`)   |

**Réponse** (200 OK) :

```json
{
  "content": [
    {
      "id": 1,
      "username": "admin",
      "verified": false,
      "role": {
        "id": 1,
        "name": "ADMIN",
        "description": "Administrateur"
      },
      "credentials": {
        "id": 1,
        "email": "admin@bhak.com",
        "phoneNumber": "+33600000001"
      },
      "createdAt": "2026-03-14T10:00:00"
    },
    {
      "id": 2,
      "username": "user1",
      "verified": false,
      "role": {
        "id": 2,
        "name": "USER",
        "description": "Utilisateur"
      },
      "credentials": {
        "id": 2,
        "email": "user1@bhak.com",
        "phoneNumber": "+33600000002"
      },
      "createdAt": "2026-03-14T10:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": { "sorted": true, "unsorted": false, "empty": false }
  },
  "totalElements": 2,
  "totalPages": 1,
  "last": true,
  "first": true,
  "number": 0,
  "size": 10,
  "numberOfElements": 2,
  "empty": false
}
```

---

### 8.2 Récupérer un utilisateur par ID

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/users/1
```

**Réponse** (200 OK) :

```json
{
  "id": 1,
  "username": "admin",
  "verified": false,
  "role": {
    "id": 1,
    "name": "ADMIN",
    "description": "Administrateur"
  },
  "credentials": {
    "id": 1,
    "email": "admin@bhak.com",
    "phoneNumber": "+33600000001"
  },
  "createdAt": "2026-03-14T10:00:00"
}
```

**Erreurs possibles :**
- `404` — `"Utilisateur introuvable avec l'identifiant: 999"`

---

### 8.3 Créer un utilisateur

```bash
curl -X POST http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "nouveauuser",
    "email": "nouveau@email.com",
    "phoneNumber": "+33698765432",
    "password": "motdepasse123",
    "roleType": "USER"
  }'
```

**Corps de la requête :** Identique à l'inscription (voir [section 6.1](#61-inscription)).

**Réponse** (201 Created) :

```json
{
  "id": 4,
  "username": "nouveauuser",
  "verified": false,
  "role": {
    "id": 2,
    "name": "USER",
    "description": "Utilisateur"
  },
  "credentials": {
    "id": 4,
    "email": "nouveau@email.com",
    "phoneNumber": "+33698765432"
  },
  "createdAt": "2026-03-14T11:00:00"
}
```

**Erreurs possibles :**
- `400` — Données manquantes ou rôle inconnu
- `409` — Pseudo, e-mail ou téléphone déjà utilisé

---

### 8.4 Modifier un utilisateur

La modification est **partielle** : seuls les champs non-nuls sont mis à jour.

```bash
curl -X PUT http://localhost:8080/api/admin/users/2 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user1_modifie",
    "email": "user1_new@bhak.com"
  }'
```

> **Seuls les champs fournis sont modifiés.** Par exemple, si vous n'envoyez que `username`, seul le pseudo change — l'e-mail, le téléphone, le mot de passe et le rôle restent inchangés.

**Réponse** (200 OK) :

```json
{
  "id": 2,
  "username": "user1_modifie",
  "verified": false,
  "role": {
    "id": 2,
    "name": "USER",
    "description": "Utilisateur"
  },
  "credentials": {
    "id": 2,
    "email": "user1_new@bhak.com",
    "phoneNumber": "+33600000002"
  },
  "createdAt": "2026-03-14T10:00:00"
}
```

**Exemples de modifications partielles :**

Changer uniquement le mot de passe :

```bash
curl -X PUT http://localhost:8080/api/admin/users/2 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"password": "nouveaumotdepasse"}'
```

Changer le rôle :

```bash
curl -X PUT http://localhost:8080/api/admin/users/2 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"roleType": "ADMIN"}'
```

**Erreurs possibles :**
- `404` — Utilisateur introuvable
- `409` — Pseudo, e-mail ou téléphone déjà utilisé par un autre compte

---

### 8.5 Supprimer un utilisateur

```bash
curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/admin/users/2
```

**Réponse** (204 No Content) : Pas de corps de réponse.

> La suppression cascade sur les `Credentials` associés (OneToOne avec `cascade = ALL` et `orphanRemoval = true`).

**Erreurs possibles :**
- `404` — Utilisateur introuvable

---

## 9. Swagger / OpenAPI

L'API est documentée automatiquement via Swagger UI.

### Accéder à Swagger

Ouvrir dans le navigateur : **http://localhost:8080/swagger-ui.html**

### S'authentifier dans Swagger

1. Se connecter via `POST /api/auth/login` (directement dans Swagger ou via curl)
2. Copier le token JWT reçu dans la réponse
3. Cliquer sur le bouton **"Authorize"** 🔒 en haut à droite de Swagger
4. Coller le token dans le champ `Value` (sans le préfixe `Bearer`)
5. Cliquer sur **"Authorize"** puis **"Close"**

Tous les endpoints protégés sont maintenant accessibles depuis Swagger.

### OpenAPI JSON

La spécification OpenAPI brute est disponible à : **http://localhost:8080/v3/api-docs**

---

## 10. RabbitMQ Management

### Accéder à l'interface

Ouvrir dans le navigateur : **http://localhost:15672**

**Identifiants :** `guest` / `guest`

### Éléments créés automatiquement

**Exchanges :**

| Nom                | Type    | Rôle                                    |
|--------------------|---------|-----------------------------------------|
| `auth.events`      | `topic` | Exchange principal pour les événements  |
| `auth.events.dlx`  | `topic` | Dead Letter Exchange (messages échoués) |

**Queues :**

| Nom                                | Routing Key                          | Rôle                                   |
|------------------------------------|--------------------------------------|----------------------------------------|
| `notification.user-registered`     | `auth.user-registered`               | Reçoit les inscriptions → envoie e-mail|
| `analytics.email-verified`         | `auth.email-verified`                | Reçoit les vérifications d'e-mail      |
| `notification.user-registered.dlq` | `dlq.notification.user-registered`   | Messages échoués (Dead Letter Queue)   |

### Événements publiés

**À l'inscription** (`auth.user-registered`) :

```json
{
  "type": "UserRegistered",
  "eventId": "uuid-...",
  "occurredAt": "2026-03-14T10:35:00Z",
  "userId": "3",
  "email": "mon@email.com",
  "tokenId": "tok_abcd1234ef56",
  "tokenClear": "550e8400-e29b-41d4-a716-446655440000",
  "correlationId": "uuid-..."
}
```

**À la vérification** (`auth.email-verified`) :

```json
{
  "type": "EmailVerified",
  "eventId": "uuid-...",
  "occurredAt": "2026-03-14T10:36:00Z",
  "userId": "3",
  "email": "mon@email.com",
  "correlationId": "uuid-..."
}
```

---

## 11. MailHog (e-mails de test)

MailHog capture tous les e-mails envoyés par le Service Notification sans les transmettre réellement.

### Accéder à l'interface

Ouvrir dans le navigateur : **http://localhost:8025**

### Comment l'utiliser

1. Inscrivez un nouvel utilisateur (voir [section 6.1](#61-inscription))
2. Ouvrez MailHog à `http://localhost:8025`
3. L'e-mail de vérification apparaît dans la boîte de réception
4. Cliquez sur l'e-mail pour voir son contenu
5. Cliquez sur le lien de vérification dans l'e-mail
6. Le compte passe à `verified = true`

> MailHog expose le port SMTP `1025` (utilisé par le Service Notification) et l'interface web sur le port `8025`.

---

## 12. Erreurs possibles

Toutes les erreurs sont retournées au format JSON uniforme :

```json
{
  "timestamp": "2026-03-14T10:30:00.123",
  "status": <code>,
  "error": "<libellé>",
  "message": "<détail>"
}
```

### Codes d'erreur

| Code | Libellé                | Causes                                                                 |
|------|------------------------|------------------------------------------------------------------------|
| 400  | `Bad Request`          | Champs obligatoires manquants, token de vérification expiré ou invalide|
| 401  | `Unauthorized`         | Identifiants de connexion incorrects, token JWT absent ou expiré       |
| 403  | `Forbidden`            | Rôle insuffisant (ex: USER essaie d'accéder aux endpoints ADMIN)       |
| 404  | `Not Found`            | Utilisateur ou ressource introuvable                                   |
| 409  | `Conflict`             | Pseudo, e-mail ou téléphone déjà utilisé par un autre compte           |
| 500  | `Internal Server Error`| Erreur interne inattendue                                              |

### Exemples

**400 — Champs manquants :**

```json
{
  "timestamp": "2026-03-14T10:30:00.123",
  "status": 400,
  "error": "Bad Request",
  "message": "Les champs pseudo, mail et mot de passe sont obligatoires"
}
```

**401 — Identifiants incorrects :**

```
Identifiants invalides
```

**403 — Accès refusé (USER vers endpoint ADMIN) :**

```bash
# Connexion en tant que USER
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"user123"}' | jq -r '.token')

# Tentative d'accès aux endpoints admin → 403
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/admin/users
```

**404 — Utilisateur introuvable :**

```json
{
  "timestamp": "2026-03-14T10:30:00.123",
  "status": 404,
  "error": "Not Found",
  "message": "Utilisateur introuvable avec l'identifiant: 999"
}
```

**409 — Doublon :**

```json
{
  "timestamp": "2026-03-14T10:30:00.123",
  "status": 409,
  "error": "Conflict",
  "message": "La valeur 'admin' est déjà attribuée au champ: pseudo"
}
```

---

## 13. Collection Postman

Une collection Postman prête à l'emploi est fournie dans le dossier `postman/`.

### Importer la collection

1. Ouvrir **Postman**
2. Cliquer sur **Import** (en haut à gauche)
3. Glisser-déposer le fichier `postman/Software_Architecture_BHAK.postman_collection.json`
4. La collection apparaît dans la barre latérale

### Contenu de la collection

**Variables de collection :**

| Variable    | Valeur par défaut          | Description                |
|-------------|----------------------------|----------------------------|
| `base_url`  | `http://localhost:8080`    | URL de base de l'API       |
| `jwt_token` | *(vide)*                   | Rempli automatiquement après login |

**Requêtes incluses :**

#### Auth (public)
| # | Nom               | Méthode | URL                          |
|---|-------------------|---------|------------------------------|
| 1 | Register ADMIN    | POST    | `/api/auth/register`         |
| 2 | Register USER     | POST    | `/api/auth/register`         |
| 3 | Login ADMIN       | POST    | `/api/auth/login`            |
| 4 | Login USER        | POST    | `/api/auth/login`            |

> **Login ADMIN** contient un script de test qui sauvegarde automatiquement le token JWT dans la variable `jwt_token`.

#### Users CRUD (admin, token requis)
| # | Nom                          | Méthode | URL                                          |
|---|------------------------------|---------|----------------------------------------------|
| 1 | GET tous les utilisateurs    | GET     | `/api/admin/users?page=0&size=10&sort=username` |
| 2 | GET utilisateur par ID       | GET     | `/api/admin/users/1`                         |
| 3 | POST créer utilisateur       | POST    | `/api/admin/users`                           |
| 4 | PUT modifier utilisateur     | PUT     | `/api/admin/users/2`                         |
| 5 | DELETE utilisateur            | DELETE  | `/api/admin/users/2`                         |

#### Tests de sécurité
| # | Nom                          | Méthode | URL                    | Résultat attendu |
|---|------------------------------|---------|------------------------|------------------|
| 1 | GET users SANS token         | GET     | `/api/admin/users`     | 403 Forbidden    |
| 2 | GET endpoint racine (public) | GET     | `/`                    | 200 OK           |

### Workflow recommandé dans Postman

1. Exécuter **Login ADMIN** → le token est sauvegardé automatiquement
2. Exécuter n'importe quelle requête CRUD → le token est injecté via `{{jwt_token}}`
3. Tester **GET users SANS token** pour vérifier que la sécurité fonctionne

---

## 14. Lancement local (sans Docker)

Si vous préférez lancer le service Auth directement sur votre machine.

### Prérequis

1. **Java 17** installé (`java -version`)
2. **PostgreSQL 16** en cours d'exécution sur `localhost:5432`
3. **RabbitMQ** en cours d'exécution sur `localhost:5672`
4. *(Optionnel)* **MailHog** sur `localhost:1025` / `localhost:8025`

### Créer la base de données

```bash
psql -U postgres -c "CREATE DATABASE software_architecture_db;"
```

### Lancer l'application

Avec le wrapper Maven inclus :

```bash
./mvnw spring-boot:run
```

Ou avec Maven installé globalement :

```bash
mvn spring-boot:run
```

> L'application démarre sur **http://localhost:8080**.  
> Les tables sont créées automatiquement grâce à `spring.jpa.hibernate.ddl-auto=update`.  
> Les comptes par défaut (`admin`, `user1`) sont créés au premier démarrage.

### Lancer avec Docker uniquement pour les dépendances

Si vous voulez lancer PostgreSQL, RabbitMQ et MailHog via Docker mais l'application en local :

```bash
# Démarrer uniquement les dépendances
docker compose up postgres rabbitmq mailhog -d

# Lancer l'application en local
./mvnw spring-boot:run
```

---

## Récapitulatif des endpoints

| Méthode  | URL                                           | Auth requise | Rôle requis | Description                    |
|----------|-----------------------------------------------|-------------|-------------|--------------------------------|
| `GET`    | `/`                                           | ❌ Non       | —           | Health check                   |
| `POST`   | `/api/auth/register`                          | ❌ Non       | —           | Inscription                    |
| `POST`   | `/api/auth/login`                             | ❌ Non       | —           | Connexion (retourne JWT)       |
| `GET`    | `/api/auth/verify?tokenId=...&t=...`          | ❌ Non       | —           | Vérification d'e-mail          |
| `GET`    | `/api/admin/users?page=0&size=10&sort=...`    | ✅ Oui       | `ADMIN`     | Lister les utilisateurs        |
| `GET`    | `/api/admin/users/{id}`                       | ✅ Oui       | `ADMIN`     | Détail d'un utilisateur        |
| `POST`   | `/api/admin/users`                            | ✅ Oui       | `ADMIN`     | Créer un utilisateur           |
| `PUT`    | `/api/admin/users/{id}`                       | ✅ Oui       | `ADMIN`     | Modifier un utilisateur        |
| `DELETE` | `/api/admin/users/{id}`                       | ✅ Oui       | `ADMIN`     | Supprimer un utilisateur       |
| `GET`    | `/swagger-ui.html`                            | ❌ Non       | —           | Documentation Swagger          |
| `GET`    | `/v3/api-docs`                                | ❌ Non       | —           | Spécification OpenAPI (JSON)   |