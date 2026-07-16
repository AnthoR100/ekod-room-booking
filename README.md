# EKOD Room Booking

Application web de gestion et réservation de salles pour le campus EKOD. Développée avec Spring Boot, elle expose une API REST complète et une interface web Thymeleaf avec gestion des rôles, authentification JWT et stockage de fichiers cloud.

---

## Table des matières

- [Fonctionnalités](#fonctionnalités)
- [Stack technique](#stack-technique)
- [Architecture](#architecture)
- [Prérequis](#prérequis)
- [Installation et démarrage](#installation-et-démarrage)
- [Configuration](#configuration)
- [API REST](#api-rest)
- [Interface web](#interface-web)
- [Tests](#tests)

Pour le détail du fonctionnement interne (sécurité, cycle de vie des réservations, chatbot IA...), voir [FONCTIONNEMENT.md](FONCTIONNEMENT.md).

---

## Fonctionnalités

- **Authentification JWT** avec refresh tokens (access token 15 min, refresh token 7 jours)
- **Connexion OAuth2 Google** en plus du couple email/mot de passe (compte auto-créé au premier login)
- **Assistant chatbot IA** capable de chercher des salles disponibles, réserver et annuler une réservation en langage naturel (function calling OpenAI)
- **Gestion des salles** : création, modification, suppression, recherche et filtrage par disponibilité
- **Système de réservation** avec détection des conflits de créneaux
- **Gestion des équipements** associés aux salles (projecteurs, tableaux blancs, etc.)
- **Upload de fichiers** vers Scaleway S3 (JPEG, PNG, GIF, WebP, PDF)
- **Dashboard administrateur** pour la gestion des réservations et des salles
- **Contrôle d'accès par rôle** : `ROLE_USER` / `ROLE_ADMIN`
- **Documentation API** via Swagger UI
- **Migrations BDD** gérées par Flyway

---

## Stack technique

| Couche | Technologie |
|---|---|
| Langage | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Sécurité | Spring Security + JJWT 0.12.6 |
| Persistance | Spring Data JPA + PostgreSQL |
| Migrations | Flyway |
| Mapping | MapStruct 1.6.3 |
| Templates | Thymeleaf |
| Stockage fichiers | AWS S3 SDK (Scaleway) |
| IA / Chatbot | OpenAI Java SDK (function calling, `gpt-4.1-mini`) |
| OAuth2 | Spring Security OAuth2 Client (Google) |
| Documentation | Springdoc OpenAPI (Swagger UI) |
| Build | Maven |
| Conteneurisation | Docker / Docker Compose |

---

## Architecture

```
src/main/java/fr/ekod/cda/ja/ekod_room_booking/
├── config/         # SecurityConfig, S3Config, OpenAIConfig
├── controller/     # Contrôleurs REST et vues Thymeleaf
├── dto/            # DTOs (auth, chatbot, equipment, file, reservation, room, user)
├── exception/      # Exceptions métier personnalisées
├── mapper/         # Mappers MapStruct
├── model/          # Entités JPA et enums
├── repository/     # Repositories Spring Data JPA
├── scheduler/      # Tâches planifiées (disponibilité des salles)
├── security/       # Filtre JWT, JwtService, UserDetailsService, OAuth2SuccessHandler
├── service/        # Logique métier (dont ChatService — assistant IA)
└── validation/     # Validateurs personnalisés

src/main/resources/
├── application.properties
├── db/migration/   # Scripts Flyway (V1 à V4)
├── static/css/     # Styles
└── templates/      # Vues Thymeleaf (dont fragments/chatbot.html)

bruno/              # Collection de tests API (par feature)
```

**Modèle de données principal :**

- `User` — compte utilisateur avec rôle (mot de passe nullable pour les comptes créés via OAuth2)
- `Room` — salle avec capacité, localisation et équipements
- `Equipment` — équipement associable à une salle
- `Reservation` — réservation avec statut (`PENDING`, `CONFIRMED`, `CANCELLED`, `REJECTED`)
- `RoomFile` — fichier/image lié à une salle (stocké sur S3)
- `RefreshToken` — token de renouvellement de session
- `Conversation` — fil de discussion du chatbot, un par utilisateur
- `ChatMessage` — message d'une conversation (`USER` / `ASSISTANT`)

---

## Prérequis

- Java 21
- Maven
- Docker & Docker Compose

---

## Installation et démarrage

**1. Cloner le dépôt**

```bash
git clone <url-du-repo>
cd ekod-room-booking
```

**2. Configurer les variables d'environnement**

Copier le fichier exemple et renseigner les valeurs :

```bash
cp .env.exemple .env
```

Voir la section [Configuration](#configuration) pour le détail des variables.

**3. Démarrer la base de données PostgreSQL**

```bash
docker-compose up -d
```

**4. Lancer l'application**

```bash
./mvnw clean spring-boot:run
```

L'application est disponible sur `http://localhost:8080`.

La documentation Swagger est accessible sur `http://localhost:8080/swagger-ui.html`.

---

## Configuration

Variables d'environnement à définir dans le fichier `.env` :

| Variable | Description |
|---|---|
| `DB_URL` | URL JDBC de la base PostgreSQL |
| `DB_USERNAME` | Nom d'utilisateur PostgreSQL |
| `DB_PASSWORD` | Mot de passe PostgreSQL |
| `JWT_SECRET` | Clé secrète pour la signature JWT |
| `S3_ACCESS_KEY` (`SCW_ACCESS_KEY`) | Clé d'accès Scaleway S3 |
| `S3_SECRET_KEY` (`SCW_SECRET_KEY`) | Clé secrète Scaleway S3 |
| `S3_BUCKET_NAME` (`SCW_BUCKET`) | Nom du bucket S3 |
| `S3_REGION` (`SCW_REGION`) | Région Scaleway |
| `S3_ENDPOINT` (`SCW_ENDPOINT`) | URL d'endpoint S3 |
| `ADMIN_SECRET` | Secret requis pour créer un compte administrateur |
| `GOOGLE_CLIENT_ID` | Identifiant client OAuth2 Google |
| `GOOGLE_CLIENT_SECRET` | Secret client OAuth2 Google |
| `OPENAI_API_KEY` | Clé API OpenAI utilisée par le chatbot |

La base de données Docker est exposée sur le port `5440`. Voir `.env.exemple` pour le détail exact des noms de variables.

---

## API REST

### Authentification — `/api/auth`

| Méthode | Endpoint | Description | Accès |
|---|---|---|---|
| POST | `/register` | Inscription | Public |
| POST | `/login` | Connexion | Public |
| POST | `/refresh` | Renouveler l'access token | Public |
| POST | `/logout` | Déconnexion | Authentifié |

La connexion via Google (`/oauth2/authorization/google`) est également disponible depuis la page `/login` : elle crée automatiquement un compte `ROLE_USER` au premier login et pose un cookie `accessToken` (JWT) comme pour l'authentification classique.

### Chatbot IA — `/api/chat`

| Méthode | Endpoint | Description | Accès |
|---|---|---|---|
| POST | `/` | Envoyer un message à l'assistant | Authentifié |
| GET | `/history` | Récupérer l'historique de la conversation | Authentifié |

L'assistant peut chercher des salles disponibles, créer une réservation, lister ou annuler les réservations de l'utilisateur, via function calling OpenAI (voir [FONCTIONNEMENT.md](FONCTIONNEMENT.md#chatbot-ia)).

### Salles — `/api/rooms`

| Méthode | Endpoint | Description | Accès |
|---|---|---|---|
| GET | `/` | Lister toutes les salles | Authentifié |
| GET | `/available` | Salles disponibles | Authentifié |
| GET | `/search` | Recherche par nom/description | Authentifié |
| GET | `/{id}` | Détail d'une salle | Authentifié |
| POST | `/` | Créer une salle | Admin |
| PUT | `/{id}` | Modifier une salle | Admin |
| DELETE | `/{id}` | Supprimer une salle | Admin |
| POST | `/upload` | Uploader un fichier | Admin |
| GET | `/files` | Lister les fichiers | Authentifié |
| GET | `/files/{key}` | Télécharger un fichier | Authentifié |
| DELETE | `/files/{key}` | Supprimer un fichier | Admin |

### Réservations — `/api/reservations`

| Méthode | Endpoint | Description | Accès |
|---|---|---|---|
| GET | `/` | Toutes les réservations | Admin |
| GET | `/pending` | Réservations en attente | Admin |
| GET | `/me` | Mes réservations | Authentifié |
| GET | `/room/{roomId}` | Réservations d'une salle | Admin |
| GET | `/user/{userId}` | Réservations d'un utilisateur | Admin |
| POST | `/` | Créer une réservation | Authentifié |
| PATCH | `/{id}/confirm` | Confirmer | Admin |
| PATCH | `/{id}/reject` | Rejeter | Admin |
| PATCH | `/{id}/cancel` | Annuler | Authentifié |
| DELETE | `/{id}` | Supprimer | Admin |

### Équipements — `/api/equipment`

| Méthode | Endpoint | Description | Accès |
|---|---|---|---|
| GET | `/` | Lister | Authentifié |
| GET | `/{id}` | Détail | Authentifié |
| POST | `/` | Créer | Admin |
| PUT | `/{id}` | Modifier | Admin |
| DELETE | `/{id}` | Supprimer | Admin |

### Utilisateurs — `/api/users`

| Méthode | Endpoint | Description | Accès |
|---|---|---|---|
| GET | `/me` | Mon profil | Authentifié |
| PATCH | `/me` | Modifier mon profil | Authentifié |
| DELETE | `/{id}` | Supprimer un utilisateur | Admin |

---

## Interface web

| Route | Description | Accès |
|---|---|---|
| `/` | Page d'accueil avec statistiques | Public |
| `/rooms` | Liste des salles | Public |
| `/rooms/{id}` | Détail d'une salle | Public |
| `/login` | Connexion (email/mot de passe + bouton Google) | Public |
| `/oauth2/success` | Redirection après connexion Google réussie | Public |
| `/register` | Inscription | Public |
| `/profile` | Mon profil | Authentifié |
| `/reservations/me` | Mes réservations | Authentifié |
| `/admin/dashboard` | Gestion des réservations | Admin |
| `/admin/rooms` | Gestion des salles | Admin |

Un widget de chat flottant (fragment `fragments/chatbot.html`, inclus dans le footer) est visible sur toutes les pages pour un utilisateur connecté, quelle que soit la route.

---

## Stratégie de test

### Couverture par grandes catégories

| Niveau | Ce qu'on vérifie | Outil | Emplacement |
|---|---|---|---|
| **Unitaire** | Logique métier isolée (services, mappers, validateurs) sans dépendance externe | JUnit 5 + Mockito | `src/test/java/.../service/` |
| **Intégration base de données** | Requêtes JPA, contraintes, migrations Flyway sur une vraie base | Spring Boot Test + H2 (mode PostgreSQL) | `src/test/java/.../repository/` |
| **Intégration API** | Endpoints REST : codes HTTP, sérialisation JSON, sécurité (auth/rôles) | MockMvc + Spring Security Test | `src/test/java/.../controller/` |
| **Système** | Parcours utilisateur complets (inscription → réservation → validation admin) | Collection Bruno (`/bruno`) exécutée manuellement | `bruno/` |

### Ce qui n'est PAS couvert, et pourquoi

- **Les vues Thymeleaf** ne sont pas testées automatiquement : le coût d'automatisation IHM (Selenium, Playwright) est disproportionné pour ce projet, elles sont vérifiées manuellement.
- **L'intégration Scaleway S3** n'est pas testée : appeler un service cloud payant en CI introduit des dépendances externes, des coûts et de la fragilité. Le service est mocké en test unitaire.
- **Les cas de charge et de performance** ne sont pas couverts : hors périmètre pour une application de gestion interne à faible trafic.
- **Les tests exhaustifs sont impossibles** : on couvre les chemins critiques (happy path + principaux cas d'erreur), pas toutes les combinaisons d'entrées possibles.

---

## Tests

### Tests unitaires et d'intégration

```bash
./mvnw test
```

Les tests utilisent une base H2 en mémoire configurée indépendamment de l'environnement de développement.

### Tests E2E — Scénario Bruno

La collection Bruno est versionnée dans `/bruno`, organisée par fonctionnalité (auth, rooms, equipment, reservations, users, files) et par scénario de bout en bout (`bruno/scenario/`).

**Prérequis** : l'application doit être démarrée sur `http://localhost:8080`.

```bash
docker-compose up -d
```

**Lancer le scénario complet en ligne de commande :**

```bash
cd bruno
npx @usebruno/cli run scenario --env Local
```

**Ou depuis l'UI Bruno :** clic droit sur le dossier *Scenario E2E* → *Run* → environnement *Local*.

Le scénario couvre 12 requêtes avec assertions, du début à la fin du cycle de vie d'un utilisateur :

| # | Requête | Code attendu |
|---|---------|-------------|
| 01 | Inscription (email dynamique) | 201 |
| 02 | Connexion, récupération du token | 200 |
| 03 | Accès sans token → refusé | 401 |
| 04 | Inscription email invalide → refusé | 400 |
| 05 | Création compte admin | 201 |
| 06 | Création salle avec `ROLE_USER` → refusé | 403 |
| 07 | Création salle (admin) | 201 |
| 08 | Réservation avec dates futures dynamiques | 201 PENDING |
| 09 | Dépassement de capacité → refusé | 400 |
| 10 | Confirmation de la réservation (admin) | 200 CONFIRMED |
| 11 | Double réservation (salle indisponible) → refusé | 409 |
| 12 | Annulation par le propriétaire | 200 CANCELLED |
