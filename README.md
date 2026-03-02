Introduction to Software Architecture Project

## Description

Application REST API avec :
- **Systeme d'authentification** (JWT)
- **API CRUD pour la gestion des utilisateurs**
- **Gestion des roles** (ADMIN / USER)
- **Tests via Postman**

## Fonctionnalités

### Authentification
- **Register** : Inscription d'un nouvel utilisateur
- **Login** : Connexion et génération de token JWT

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
- **PostgreSQL**
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
│   ├── LoginRequest.java
│   └── RegisterRequest.java
├── entity/                 # Entités JPA
│   ├── Credentials.java
│   ├── Role.java
│   ├── RoleType.java
│   └── User.java
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
│   └── UserRepository.java
└── service/                # Services métier
    ├── CustomUserDetailsService.java
    └── UserService.java
```

## Prérequis
1. Java 17+
2. Maven
3. PostgreSQL
4. Postman

## Installation

### 1. Créer la base de données
```sql
CREATE DATABASE software_architecture_db;
```

### 2. Configurer la connexion
Modifier `src/main/resources/application.properties` :
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/software_architecture_db
spring.datasource.username=votre_username
spring.datasource.password=votre_password
```

### 3. Lancer l'application
```bash
mvn spring-boot:run
```

## Endpoints API

### Authentification (Public)
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/auth/register` | Inscription d'un nouvel utilisateur |
| POST | `/api/auth/login` | Connexion (retourne un token JWT) |

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
- **BCrypt** : Hachage sécurisé des mots de passe
- **@PreAuthorize** : Contrôle d'accès basé sur les rôles

## Auteur
BHAK - Introduction to Software Architecture
