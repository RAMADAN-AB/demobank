provider "aws" {
  region = "us-east-1"
}

data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical
  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
}

resource "aws_security_group" "web_sg" {
  name = "aws-ha-webapp-sg"

  # Entrée : autoriser le trafic web (port 80) depuis Internet
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
    ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Sortie : autoriser tout le trafic sortant (pour télécharger Docker, les images ECR, etc.)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_instance" "serveur_web" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = "t3.micro"
  vpc_security_group_ids = [aws_security_group.web_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  user_data = <<-EOF
              #!/bin/bash
              set -e

              # 1. Installer Docker
              apt-get update -y
              apt-get install -y docker.io
              systemctl start docker
              systemctl enable docker

              # 2. Installer Docker Compose (plugin v2)
              mkdir -p /usr/local/lib/docker/cli-plugins
              curl -SL https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-x86_64 \
                -o /usr/local/lib/docker/cli-plugins/docker-compose
              chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

              # 3. Se connecter à ECR (grâce au rôle IAM, pas de mot de passe)
              apt-get install -y awscli
              aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 342677170112.dkr.ecr.us-east-1.amazonaws.com

              # 4. Créer le dossier de travail
              mkdir -p /opt/demobank
              cd /opt/demobank

              # 5. Écrire le fichier .env
              cat > .env <<ENVFILE
              POSTGRES_DB=${var.postgres_db}
              POSTGRES_USER=${var.postgres_user}
              POSTGRES_PASSWORD=${var.postgres_password}
              JWT_SECRET=${var.jwt_secret}
              JWT_EXPIRATION=${var.jwt_expiration}
              ENVFILE

              # 6. Écrire le docker-compose.prod.yml
              cat > docker-compose.prod.yml <<'COMPOSEFILE'
              services:
                postgres:
                  image: postgres:15-alpine
                  container_name: demobank-db
                  environment:
                    POSTGRES_DB: $${POSTGRES_DB}
                    POSTGRES_USER: $${POSTGRES_USER}
                    POSTGRES_PASSWORD: $${POSTGRES_PASSWORD}
                  volumes:
                    - postgres_data:/var/lib/postgresql/data
                  networks:
                    - demobank-net
                  healthcheck:
                    test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
                    interval: 5s
                    timeout: 5s
                    retries: 5
                backend:
                  image: 342677170112.dkr.ecr.us-east-1.amazonaws.com/demo-backend:latest
                  container_name: demobank-backend
                  environment:
                    DB_URL: jdbc:postgresql://postgres:5432/$${POSTGRES_DB}
                    DB_USERNAME: $${POSTGRES_USER}
                    DB_PASSWORD: $${POSTGRES_PASSWORD}
                    JWT_SECRET: $${JWT_SECRET}
                    JWT_EXPIRATION: $${JWT_EXPIRATION:-86400000}
                    SPRING_JPA_HIBERNATE_DDL_AUTO: update
                  depends_on:
                    postgres:
                      condition: service_healthy
                  networks:
                    - demobank-net
                frontend:
                  image: 342677170112.dkr.ecr.us-east-1.amazonaws.com/demo-frontend:latest
                  container_name: demobank-frontend
                  ports:
                    - "80:80"
                  depends_on:
                    - backend
                  networks:
                    - demobank-net
              networks:
                demobank-net:
                  driver: bridge
              volumes:
                postgres_data:
              COMPOSEFILE

              # 7. Lancer DemoBank
              docker compose -f docker-compose.prod.yml up -d
              EOF

  user_data_replace_on_change = true

  tags = {
    Name = "demobank-prod"
  }
}


output "public_ip" {
  value       = aws_instance.serveur_web.public_ip
  description = "IP publique du serveur web"
}


resource "aws_ecr_repository" "backend" {
  name = "demo-backend"
  # Permet a Terraform de supprimer le depot meme s'il contient encore des images
  #force_delete = true
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_repository" "frontend" {
  name = "demo-frontend"
  # Permet a Terraform de supprimer le depot meme s'il contient encore des images
  #force_delete = true
  image_scanning_configuration {
    scan_on_push = true
  }
}

output "ecr_backend_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "ecr_frontend_url" {
  value = aws_ecr_repository.frontend.repository_url
}

resource "aws_iam_role" "ec2_ecr_role" {
  name = "demobank-ec2-ecr-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecr_read" {
  role       = aws_iam_role.ec2_ecr_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "demobank-ec2-profile"
  role = aws_iam_role.ec2_ecr_role.name
}

# =========================================================
# OIDC : permettre à GitHub Actions d'agir sur AWS
# =========================================================

# A. Le provider OIDC GitHub
resource "aws_iam_openid_connect_provider" "github" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

# B. Le rôle que GitHub Actions va endosser
resource "aws_iam_role" "github_actions" {
  name = "demobank-github-actions-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = aws_iam_openid_connect_provider.github.arn
      }
      Action = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
        }
        # GitHub emet desormais un "sub" immuable, avec les ID numeriques du
        # compte (@200751835) et du depot (@1350193891). On accepte les deux
        # formats : la liste vaut un OU logique.
        StringLike = {
          "token.actions.githubusercontent.com:sub" = [
            "repo:RAMADAN-AB@200751835/demobank@1350193891:*",
            "repo:RAMADAN-AB/demobank:*"
          ]
        }
      }
    }]
  })
}

# C. Autoriser ce rôle à pousser/tirer sur ECR
resource "aws_iam_role_policy_attachment" "github_ecr" {
  role       = aws_iam_role.github_actions.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPowerUser"
}

# Output : l'ARN du rôle (on en aura besoin dans le pipeline)
output "github_actions_role_arn" {
  value = aws_iam_role.github_actions.arn
}