# Dernière AMI Ubuntu 22.04 publiée par Canonical.
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical
  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
}

# Serveur de production : installe Docker, se connecte à ECR via le rôle IAM,
# puis lance la stack DemoBank au premier démarrage (user_data).
resource "aws_instance" "serveur_web" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = "t3.micro"
  vpc_security_group_ids = [aws_security_group.web_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name
  # Forcer IMDSv2 (tokens de session obligatoires) - protection SSRF
  metadata_options {
    http_tokens   = "required"
    http_endpoint = "enabled"
  }
    # Chiffrement du disque au repos
  root_block_device {
    encrypted = true
  }
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

              # 5. Récupérer les secrets depuis AWS Secrets Manager et écrire le .env
              SECRET_JSON=$(aws secretsmanager get-secret-value \
                --secret-id demobank/app-secrets \
                --region us-east-1 \
                --query SecretString \
                --output text)

              POSTGRES_PASSWORD=$(echo "$SECRET_JSON" | grep -o '"POSTGRES_PASSWORD":"[^"]*"' | cut -d'"' -f4)
              JWT_SECRET=$(echo "$SECRET_JSON" | grep -o '"JWT_SECRET":"[^"]*"' | cut -d'"' -f4)

              cat > .env <<ENVFILE
              POSTGRES_DB=${var.postgres_db}
              POSTGRES_USER=${var.postgres_user}
              POSTGRES_PASSWORD=$POSTGRES_PASSWORD
              JWT_SECRET=$JWT_SECRET
              JWT_EXPIRATION=${var.jwt_expiration}
              ENVFILE

              # 6. Écrire le docker-compose.prod.yml
              cat > docker-compose.prod.yml <<'COMPOSEFILE'
              services:
                backend:
                  image: 342677170112.dkr.ecr.us-east-1.amazonaws.com/demo-backend:latest
                  container_name: demobank-backend
                  environment:
                    DB_URL: jdbc:postgresql://${aws_db_instance.demobank.address}:5432/$${POSTGRES_DB}
                    DB_USERNAME: $${POSTGRES_USER}
                    DB_PASSWORD: $${POSTGRES_PASSWORD}
                    JWT_SECRET: $${JWT_SECRET}
                    JWT_EXPIRATION: $${JWT_EXPIRATION:-86400000}
                    SPRING_JPA_HIBERNATE_DDL_AUTO: update
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
              COMPOSEFILE

              # 7. Lancer DemoBank
              docker compose -f docker-compose.prod.yml up -d
              EOF

  user_data_replace_on_change = true

  tags = {
    Name = "demobank-prod"
  }
}
