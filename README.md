Introduction to Software Architecture Project

## Description

Application REST API avec :
- **Systeme d'authentification** (JWT)
- **API CRUD pour la gestion des utilisateurs**
- **Gestion des roles** (ADMIN / USER)
- **Vérification d'e-mail** via RabbitMQ + MailHog
- **Tests via Postman**

## Fonctionnalités

### Authentification
- **Register** : Inscription d'un nouvel utilisateur (verified=false)
- **Login** : Connexion et génération de token JWT
- **Verify** : Vérification d'e-mail via lien (token BCrypt, one-shot)

### Vérification d'e-mail (RabbitMQ)
- Auth publie `UserRegistered` sur RabbitMQ à l'inscription
- Notification consomme l'événement et envoie un e-mail via MailHog
- L'utilisateur clique sur le lien → `GET /api/auth/verify` → verified=true
- Auth publie `EmailVerified` après vérification réussie
- Consumer Analytics compte les vérifications

### Gestion des Utilisateurs (ADMIN uniquement)
- **GET** `/api/admin/users` : Liste tous les utilisateurs (pagination)
- **GET** `/api/admin/users/{id}` : Récupère un utilisateur par ID
- **POST** `/api/admin/users` : Crée un nouvel utilisateur
- **PUT** `/api/admin/users/{id}` : Modifie un utilisateur
- **DELETE** `/api/admin/users/{id}` : Supprime un utilisateur

## Modèle de données (3 classes)

### Relations JPA
```
User (1) ←→ (1) Credentials     [OneToOne avec cascade]
User (*) ←→ (1) Role            [ManyToOne]
```

### Entités
| Entité | Description |
|--------|-------------|
| **User** | Utilisateur avec username et relation vers Role et Credentials |
| **Credentials** | Identifiants sensibles (email, phoneNumber, password) |
| **Role** | Rôle avec enum RoleType (ADMIN, USER) |

## Technologies
- **Java 17**
- **Spring Boot 3.2**
- **Spring Security** (JWT)
- **Spring Data JPA**
- **Spring AMQP** (RabbitMQ)
- **Spring Mail** (MailHog)
- **PostgreSQL**
- **RabbitMQ 3.13** (messagerie asynchrone)
- **MailHog** (SMTP de test)
- **Docker** (conteneurisation)
- **Lombok**
- **Swagger/OpenAPI 3**
- **Postman** (tests API)

## Structure du Projet
```
src/main/java/com/bhak/project/
├── configuration/          # Config (Security, JWT, OpenAPI, DataInit)
│   ├── DataInitializer.java
│   ├── JwtUtils.java
│   ├── OpenApiConfig.java
│   └── SecurityConfig.java
├── controller/             # REST Controllers
│   ├── AuthController.java
│   ├── HomeController.java
│   └── UserController.java
├── dto/                    # Data Transfer Objects
│   ├── EmailVerifiedEvent.java
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   └── UserRegisteredEvent.java
├── entity/                 # Entités JPA
│   ├── Credentials.java
│   ├── Role.java
│   ├── RoleType.java
│   ├── User.java
│   └── VerificationToken.java
├── exception/              # Exceptions métier
│   ├── DuplicateResourceException.java
│   ├── GlobalExceptionHandler.java
│   ├── InvalidRequestException.java
│   └── ResourceNotFoundException.java
├── filter/                 # Filtres (JWT)
│   └── JwtFilter.java
├── repository/             # Repositories JPA
│   ├── CredentialsRepository.java
│   ├── RoleRepository.java
│   ├── UserRepository.java
│   └── VerificationTokenRepository.java
└── service/                # Services métier
    ├── CustomUserDetailsService.java
    ├── EventPublisher.java
    ├── UserService.java
    └── VerificationService.java
```

## Prérequis
1. Java 17+
2. Maven (ou utiliser le wrapper `mvnw` inclus)
3. Docker et Docker Compose
4. Postman

## Installation et Lancement

### Option 1 : avec Docker (recommande)
Une seule commande, rien d'autre a installer :
```bash
docker-compose up --build
```
Cela lance automatiquement :
- PostgreSQL (port 5432)
- RabbitMQ (port 5672 / UI 15672)
- MailHog (SMTP 1025 / UI 8025)
- L'application Spring Boot (port 8080)

Pour arreter :
```bash
docker-compose down
```

### Option 2 : sans Docker (developpement local)
1. Installer PostgreSQL et creer la base :
```sql
CREATE DATABASE software_architecture_db;
```
2. Démarrer l'infrastructure :
```bash
docker compose up -d postgres rabbitmq mailhog
```
3. Lancer le service Auth :
```bash
./mvnw spring-boot:run
```
4. Lancer le service Notification (dans un autre terminal) :
```bash
cd ../notification-service && mvn spring-boot:run
```

### Option 3 : avec H2 en memoire (tests rapides)
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=test
```

## Endpoints API

### Authentification (Public)
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/auth/register` | Inscription d'un nouvel utilisateur |
| POST | `/api/auth/login` | Connexion (retourne un token JWT) |
| GET | `/api/auth/verify?tokenId=...&t=...` | Vérification d'e-mail via lien |

### Gestion des Utilisateurs (ADMIN uniquement)
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/admin/users?page=0&size=10` | Liste paginée des utilisateurs |
| GET | `/api/admin/users/{id}` | Détails d'un utilisateur |
| POST | `/api/admin/users` | Créer un utilisateur |
| PUT | `/api/admin/users/{id}` | Modifier un utilisateur |
| DELETE | `/api/admin/users/{id}` | Supprimer un utilisateur |

## Documentation Swagger
Accédez à la documentation interactive :
```
http://localhost:8080/swagger-ui.html
```
## Tester avec Postman

Une collection Postman est fournie dans le dossier `postman/`.

### Importer la collection
1. Ouvrir Postman
2. Cliquer sur **Import**
3. Selectionner le fichier `postman/Software_Architecture_BHAK.postman_collection.json`

### Workflow de test
1. **Register ADMIN** : creer un compte admin
2. **Login ADMIN** : se connecter (le token JWT est sauvegarde automatiquement)
3. **CRUD Users** : toutes les requetes admin utilisent le token automatiquement

La collection contient :
- **Auth** : Register ADMIN, Register USER, Login ADMIN, Login USER
- **Users CRUD** : GET all (pagine), GET by ID, POST, PUT, DELETE
- **Tests securite** : acces sans token (403), endpoint public
## Exemples d'utilisation

### 1. Inscription
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "password123",
    "phoneNumber": "+33612345678",
    "roleType": "USER"
  }'
```

### 2. Connexion
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "password123"
  }'
```

### 3. Liste des utilisateurs (ADMIN uniquement)
```bash
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer VOTRE_TOKEN_JWT"
```

### 4. Créer un utilisateur (ADMIN uniquement)
```bash
curl -X POST http://localhost:8080/api/admin/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer VOTRE_TOKEN_JWT" \
  -d '{
    "username": "new_user",
    "email": "newuser@example.com",
    "phoneNumber": "+33698765432",
    "password": "password123",
    "roleType": "USER"
  }'
```

## Concepts du cours appliqués

### Architecture en couches
- **Controller** : Reçoit les requêtes HTTP, valide les entrées
- **Service** : Contient la logique métier
- **Repository** : Accès aux données (JPA)
- **Entity** : Modèles de données persistants

### Relations JPA (TD JPA 2)
- **@OneToOne** avec cascade : User ↔ Credentials
- **@ManyToOne** : User → Role
- **orphanRemoval** : Suppression automatique des credentials orphelins
- **FetchType.EAGER/LAZY** : Stratégies de chargement

### Sécurité
- **JWT** : Tokens stateless pour l'authentification
- **BCrypt** : Hachage sécurisé des mots de passe et des tokens de vérification
- **@PreAuthorize** : Contrôle d'accès basé sur les rôles

### Messagerie asynchrone (TP Vérification d'e-mail)
- **RabbitMQ** : Exchange topic `auth.events`, queues durables, DLX/DLQ
- **Événements** : `UserRegistered` (inscription) et `EmailVerified` (vérification)
- **Découplage** : Auth ne connaît pas Notification, communication via événements
- **Résilience** : DLQ pour les messages en erreur, retries automatiques
- **MailHog** : Serveur SMTP local pour tester les e-mails sans envoi réel
- Voir [messagerie.md](messagerie.md) pour la documentation complète

## Auteur
BHAK - Introduction to Software Architecture
