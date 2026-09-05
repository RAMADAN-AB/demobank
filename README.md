# DemoBank

Application bancaire de démonstration : API Spring Boot, interface Angular, PostgreSQL.
Conteneurisée avec Docker, déployée sur AWS via Terraform, livrée par un pipeline
GitHub Actions authentifié en OIDC (aucune clé AWS stockée dans le dépôt).

---

## Stack

| Couche | Technologie |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Security, JWT (jjwt 0.11.5) |
| Frontend | Angular 16, TypeScript 5.1, servi par NGINX |
| Base de données | PostgreSQL 15 |
| Infrastructure | Terraform, AWS EC2 + ECR + IAM |
| CI/CD | GitHub Actions, authentification OIDC |

---

## Organisation du dépôt

```
.
├── backend/              API Spring Boot (Maven)
├── frontend/             Application Angular + config NGINX
├── infra/                Infrastructure Terraform
│   ├── providers.tf        provider AWS et versions requises
│   ├── variables.tf        variables d'entrée (dont les secrets)
│   ├── network.tf          groupe de sécurité
│   ├── compute.tf          instance EC2 et son user_data
│   ├── ecr.tf              dépôts d'images Docker
│   ├── iam_ec2.tf          rôle et profil portés par l'instance
│   ├── iam_github_oidc.tf  provider OIDC et rôle GitHub Actions
│   └── outputs.tf          sorties (IP publique, URLs ECR, ARN du rôle)
├── .github/workflows/    pipeline de build et push vers ECR
├── docker-compose.yml    stack complète en local
└── .env.example          modèle de configuration
```

---

## Lancer en local

Prérequis : Docker et Docker Compose.

```bash
cp .env.example .env
```

Renseigne `POSTGRES_PASSWORD` et `JWT_SECRET` dans le `.env`. Pour générer une clé
de signature valide :

```bash
openssl rand -hex 32
```

Puis :

```bash
docker compose up --build -d
```

L'interface est sur http://localhost, l'API derrière `/api/` via le reverse proxy NGINX.

Pour repartir d'une base vierge (supprime les données) :

```bash
docker compose down -v && docker compose up --build -d
```

---

## API

| Méthode | Route | Rôle |
|---|---|---|
| `POST` | `/api/auth/register` | Créer un compte, retourne un JWT |
| `POST` | `/api/auth/login` | S'authentifier, retourne un JWT |
| `POST` | `/api/accounts` | Ouvrir un compte bancaire |
| `GET` | `/api/accounts/me` | Comptes de l'utilisateur authentifié |
| `POST` | `/api/transactions/transfer` | Virement entre comptes |
| `GET` | `/api/transactions/history/{accountNumber}` | Historique d'un compte |

Les routes hors `/api/auth/**` exigent l'en-tête `Authorization: Bearer <token>`.
Les mots de passe sont hachés en BCrypt.

---

## Infrastructure

Toutes les commandes Terraform se lancent depuis `infra/`.

```bash
cd infra && terraform init
```

Les valeurs sensibles ne sont pas versionnées. Crée `infra/terraform.tfvars` :

```hcl
postgres_password = "..."
jwt_secret        = "..."
```

Puis :

```bash
cd infra && terraform apply
```

Terraform provisionne un groupe de sécurité, deux dépôts ECR, les rôles IAM, et une
instance EC2 dont le `user_data` installe Docker, s'authentifie à ECR via son rôle,
puis démarre la stack. L'IP publique est retournée par `terraform output public_ip`.

---

## Pipeline CI/CD

À chaque push sur `main`, `.github/workflows/deploy.yml` construit les deux images
et les pousse sur ECR.

L'authentification à AWS se fait par OIDC : GitHub présente un jeton signé, AWS
vérifie que le `sub` correspond à ce dépôt puis délivre des identifiants temporaires.
Aucune clé d'accès n'est stockée dans les secrets du dépôt.

Le `sub` émis par GitHub embarque les identifiants numériques immuables du compte et
du dépôt, ce qui empêche un tiers de récupérer le nom du dépôt pour usurper le rôle.
La politique de confiance dans `infra/iam_github_oidc.tf` accepte ce format ainsi que
le format historique.

---

## Sécurité

- Aucun secret dans le dépôt : `.env` et `infra/terraform.tfvars` sont ignorés par git
- Authentification AWS sans clé, par OIDC à durée de vie courte
- L'instance EC2 tire ses images via un rôle IAM, sans identifiants sur disque
- Mots de passe hachés en BCrypt, sessions sans état via JWT

---

## Licence

Projet personnel de démonstration. Aucun lien avec un établissement bancaire réel.
