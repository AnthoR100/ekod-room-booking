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

---

## Fonctionnalités

- **Authentification JWT** avec refresh tokens (access token 15 min, refresh token 7 jours)
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
| Documentation | Springdoc OpenAPI (Swagger UI) |
| Build | Maven |
| Conteneurisation | Docker / Docker Compose |

---

## Architecture

```
src/main/java/fr/ekod/cda/ja/ekod_room_booking/
├── config/         # SecurityConfig, S3Config
├── controller/     # Contrôleurs REST et vues Thymeleaf
├── dto/            # DTOs (auth, equipment, file, reservation, room, user)
├── exception/      # Exceptions métier personnalisées
├── mapper/         # Mappers MapStruct
├── model/          # Entités JPA et enums
├── repository/     # Repositories Spring Data JPA
├── scheduler/      # Tâches planifiées (disponibilité des salles)
├── security/       # Filtre JWT, JwtService, UserDetailsService
├── service/        # Logique métier
└── validation/     # Validateurs personnalisés

src/main/resources/
├── application.properties
├── db/migration/   # Scripts Flyway (V1, V2)
├── static/css/     # Styles
└── templates/      # Vues Thymeleaf

bruno/              # Collection de tests API (par feature)
```

**Modèle de données principal :**

- `User` — compte utilisateur avec rôle
- `Room` — salle avec capacité, localisation et équipements
- `Equipment` — équipement associable à une salle
- `Reservation` — réservation avec statut (`PENDING`, `CONFIRMED`, `CANCELLED`, `REJECTED`)
- `RoomFile` — fichier/image lié à une salle (stocké sur S3)
- `RefreshToken` — token de renouvellement de session

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
| `S3_ACCESS_KEY` | Clé d'accès Scaleway S3 |
| `S3_SECRET_KEY` | Clé secrète Scaleway S3 |
| `S3_BUCKET_NAME` | Nom du bucket S3 |
| `S3_REGION` | Région Scaleway |
| `S3_ENDPOINT` | URL d'endpoint S3 |

La base de données Docker est exposée sur le port `5440`.

---

## API REST

### Authentification — `/api/auth`

| Méthode | Endpoint | Description | Accès |
|---|---|---|---|
| POST | `/register` | Inscription | Public |
| POST | `/login` | Connexion | Public |
| POST | `/refresh` | Renouveler l'access token | Public |
| POST | `/logout` | Déconnexion | Authentifié |

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
| `/login` | Connexion | Public |
| `/register` | Inscription | Public |
| `/profile` | Mon profil | Authentifié |
| `/reservations/me` | Mes réservations | Authentifié |
| `/admin/dashboard` | Gestion des réservations | Admin |
| `/admin/rooms` | Gestion des salles | Admin |

---

## Tests

Le projet inclut une collection **Bruno** dans le dossier `/bruno`, organisée par fonctionnalité (auth, rooms, equipment, reservations, users, files), pour tester l'API REST manuellement.

Pour les tests automatisés :

```bash
./mvnw test
```

Les tests utilisent une base H2 en mémoire configurée indépendamment de l'environnement de développement.
